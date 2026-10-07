package net.crystalnexus.block.entity;

import net.crystalnexus.block.CelestialGearForgeBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.CelestialGearForgeRecipe;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import net.neoforged.neoforge.energy.EnergyStorage;
import java.util.stream.IntStream;

public final class CelestialGearForgeBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	public static final int INPUT_COUNT = CelestialGearForgeRecipe.INPUT_COUNT;
	public static final int STAR_SLOT = CelestialGearForgeRecipe.STAR_SLOT;
	public static final int OUTPUT_SLOT = INPUT_COUNT;
	public static final int DURATION = 200;
	public static final int CRAFT_ENERGY_PER_TICK = 10_000;
	public static final int DATA_COUNT = 8;
	public static Map<net.minecraft.world.level.block.Block, Set<net.minecraft.world.level.block.Block>> energySubstitutions() {
		Set<net.minecraft.world.level.block.Block> input = Set.of(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
		return Map.of(CrystalnexusModBlocks.METEORITE_ALLOY_BLOCK.get(), input,
			CrystalnexusModBlocks.GRAVITY_CONTROL_POINT.get(), input, CrystalnexusModBlocks.HYPER_MACHINE_FRAME.get(), input);
	}
	private static final int VALIDATION_INTERVAL = 20;
	private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge");
	private NonNullList<ItemStack> stacks = NonNullList.withSize(INPUT_COUNT + 2, ItemStack.EMPTY);
	private NonNullList<ItemStack> activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
	@Nullable private ResourceLocation activeRecipe;
	@Nullable private ResourceLocation selectedEnchantment;
	@Nullable private ResourceLocation activeEnchantment;
	private boolean activeCombining;
	private final List<BlockPos> energyInputs = new ArrayList<>();
	private final ForgeEnergyStorage energy = new ForgeEnergyStorage();
	private int energyPerTick;
	private class ForgeEnergyStorage extends EnergyStorage {
		ForgeEnergyStorage() { super(Integer.MAX_VALUE, Integer.MAX_VALUE, 0); }
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int received = super.receiveEnergy(Math.max(0, amount), simulate);
			if (received > 0 && !simulate) sync();
			return received;
		}
		void consume(int amount) { energy -= amount; }
		void setStored(int amount) { energy = amount; }
	}
	private int progress;
	private int validationDelay;
	private boolean formed;
	private final ContainerData data = new ContainerData() {
		@Override public int get(int index) {
			return switch (index) {
				case 0 -> progress;
				case 1 -> DURATION;
				case 2 -> formed ? 1 : 0;
				case 3 -> selectedIndex();
				case 4 -> energy.getEnergyStored() & 0xffff;
				case 5 -> energy.getEnergyStored() >>> 16;
				case 6 -> energyPerTick & 0xffff;
				case 7 -> energyPerTick >>> 16;
				default -> 0;
			};
		}
		@Override public void set(int index, int value) {
			if (index == 0) progress = value;
			else if (index == 2) formed = value != 0;
			else if (index == 3) {
				var choices = enchantmentChoices(getItem(0), getItem(STAR_SLOT));
				selectedEnchantment = value >= 0 && value < choices.size() ? choices.get(value).unwrapKey().orElseThrow().location() : null;
			}
			else if (index == 4) energy.setStored((energy.getEnergyStored() & 0xffff0000) | (value & 0xffff));
			else if (index == 5) energy.setStored((energy.getEnergyStored() & 0xffff) | (value & 0xffff) << 16);
			else if (index == 6) energyPerTick = (energyPerTick & 0xffff0000) | (value & 0xffff);
			else if (index == 7) energyPerTick = (energyPerTick & 0xffff) | (value & 0xffff) << 16;
		}
		@Override public int getCount() { return DATA_COUNT; }
	};

	public CelestialGearForgeBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.CELESTIAL_GEAR_FORGE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CelestialGearForgeBlockEntity forge) {
		if (!level.isClientSide) forge.serverTick();
	}

	public void serverTick() {
        // ponytail: serialized work steps; separate lanes only if simultaneous recipe selection is needed.
        int crafts = level instanceof ServerLevel server && findRecipe(server, inputStacks()) != null
            ? net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(getItem(OUTPUT_SLOT + 1)) : 1;
        for (int craft = 0; craft < crafts; craft++)
            processStep();
    }

    private void processStep() {
		if (!(level instanceof ServerLevel serverLevel)) return;
		var choices = enchantmentChoices(getItem(0), getItem(STAR_SLOT));
		if (selectedIndex() < 0 && !choices.isEmpty()) selectedEnchantment = choices.getFirst().unwrapKey().orElseThrow().location();
		if (isProcessing() && !hasSameActiveInputs()) { resetProgress(); return; }
		if (validationDelay-- <= 0) {
			validateStructure(serverLevel);
			validationDelay = VALIDATION_INTERVAL;
		}
		if (!formed) {
			if (isProcessing()) resetProgress();
			return;
		}
		var recipe = activeRecipe == null ? (activeCombining || activeEnchantment != null ? null : findRecipe(serverLevel, stacks))
			: serverLevel.getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
				.filter(holder -> holder.id().equals(activeRecipe)).findFirst().orElse(null);
		if (recipe == null && activeRecipe == null) { processEnchanting(serverLevel); return; }
		if (recipe == null || !recipe.value().matches(inputStacks()) || !hasSameActiveInputs()) {
			if (activeRecipe != null) resetProgress();
			return;
		}
		if (!canAcceptOutput((recipe.value().output()).copy())) {
			energyPerTick = 0;
			if (activeRecipe == null) return;
			resetProgress();
			return;
		}
		energyPerTick = CRAFT_ENERGY_PER_TICK;
		if (energy.getEnergyStored() < energyPerTick) return;
		if (activeRecipe == null) {
			activeRecipe = recipe.id();
			for (int slot = 0; slot < INPUT_COUNT; slot++) activeInputs.set(slot, getItem(slot).copyWithCount(1));
		}

		energy.consume(energyPerTick);
		progress++;
		setChanged();
		if (progress >= DURATION) {
			ItemStack result = (recipe.value().output()).copy();
			net.minecraft.world.item.Item star = getItem(STAR_SLOT).getItem();
			for (int slot = 0; slot < INPUT_COUNT; slot++) getItem(slot).shrink(1);
			ItemStack output = getItem(OUTPUT_SLOT);
			if (output.isEmpty()) stacks.set(OUTPUT_SLOT, result);
			else output.grow(result.getCount());
			burst(serverLevel, star);
			resetProgress();
		} else {
			sync();
		}
	}

	private static net.minecraft.world.item.crafting.RecipeHolder<CelestialGearForgeRecipe> findRecipe(ServerLevel level, List<ItemStack> inputs) {
		return level.getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
			.filter(holder -> holder.value().matches(inputs)).findFirst().orElse(null);
	}

	private List<ItemStack> inputStacks() { return stacks.subList(0, INPUT_COUNT); }

	private boolean hasSameActiveInputs() {
		if (!isProcessing()) return true;
		for (int slot = 0; slot < INPUT_COUNT; slot++)
			if (!ItemStack.isSameItemSameComponents(activeInputs.get(slot), getItem(slot))) return false;
		return true;
	}

	public static List<Holder<Enchantment>> enchantmentChoices(ItemStack stack, ItemStack star) {
		int cap = CelestialGearForgeEnchanting.levelCap(star);
		return EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().stream()
			.filter(entry -> entry.getIntValue() < cap && entry.getKey().unwrapKey().isPresent())
			.map(entry -> entry.getKey()).sorted(Comparator.comparing(holder -> holder.unwrapKey().orElseThrow().location().toString())).toList();
	}

	public static int starBoost(ItemStack stack) {
		return CelestialGearForgeEnchanting.starBoost(stack);
	}

	private int selectedIndex() {
		var choices = enchantmentChoices(getItem(0), getItem(STAR_SLOT));
		for (int i = 0; i < choices.size(); i++)
			if (choices.get(i).unwrapKey().orElseThrow().location().equals(selectedEnchantment)) return i;
		return -1;
	}

	public boolean cycleEnchantment(int direction) {
		var choices = enchantmentChoices(getItem(0), getItem(STAR_SLOT));
		if (!upgradeLayout() || (direction != -1 && direction != 1) || choices.size() < 2) return false;
		int index = selectedIndex();
		selectedEnchantment = choices.get(Math.floorMod((index < 0 ? 0 : index) + direction, choices.size())).unwrapKey().orElseThrow().location();
		resetProgress();
		return true;
	}

	private boolean upgradeLayout() {
		return stacks.subList(1, STAR_SLOT).stream().allMatch(ItemStack::isEmpty);
	}

	private void processEnchanting(ServerLevel serverLevel) {
		int index = selectedIndex();
		var selected = index < 0 ? null : enchantmentChoices(getItem(0), getItem(STAR_SLOT)).get(index);
		var operation = CelestialGearForgeEnchanting.calculate(inputStacks(), selected);
		if (operation == null || activeEnchantment != null && !activeEnchantment.equals(selectedEnchantment)) {
			energyPerTick = 0;
			if (isProcessing()) resetProgress();
			return;
		}
		ItemStack result = (operation.result()).copy();
		if (!canAcceptOutput(result)) { energyPerTick = 0; if (isProcessing()) resetProgress(); return; }
		energyPerTick = operation.energyPerTick();
		if (energy.getEnergyStored() < energyPerTick) return;
		if (!isProcessing()) {
			activeCombining = !upgradeLayout();
			activeEnchantment = activeCombining ? null : selectedEnchantment;
			for (int slot = 0; slot < INPUT_COUNT; slot++) activeInputs.set(slot, getItem(slot).copyWithCount(1));
		}
		energy.consume(energyPerTick);
		if (++progress >= DURATION) {
			var star = getItem(STAR_SLOT).getItem();
			for (int slot = 0; slot < STAR_SLOT; slot++) getItem(slot).shrink(1);
			if (getItem(OUTPUT_SLOT).isEmpty()) stacks.set(OUTPUT_SLOT, result);
			else getItem(OUTPUT_SLOT).grow(result.getCount());
			burst(serverLevel, star);
			resetProgress();
		} else sync();
	}

	private boolean canAcceptOutput(ItemStack result) {
		ItemStack output = getItem(OUTPUT_SLOT);
		return (output.isEmpty() || ItemStack.isSameItemSameComponents(output, result))
            && output.getCount() + result.getCount() <= result.getMaxStackSize();
	}

	private void validateStructure(ServerLevel serverLevel) {
		var match = StructureNbtValidator.validate(serverLevel, STRUCTURE, worldPosition,
			getBlockState().getValue(CelestialGearForgeBlock.FACING), CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get(),
			CelestialGearForgeBlock.FACING, energySubstitutions(), true, false);
		var nextInputs = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
		for (BlockPos old : energyInputs) {
			if (!nextInputs.contains(old) && serverLevel.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input)
				input.unbindController(worldPosition);
		}
		energyInputs.clear();
		for (BlockPos pos : nextInputs) {
			if (serverLevel.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
				input.bindController(worldPosition);
				energyInputs.add(pos);
			}
		}
		boolean nextFormed = match.isPresent() && !energyInputs.isEmpty();
		if (formed != nextFormed) { formed = nextFormed; sync(); }
		if (!formed) { energyPerTick = 0; if (isProcessing()) resetProgress(); }
	}

	@Override public EnergyStorage multiblockEnergyInput() { return energy; }
	@Override public boolean acceptsMultiblockPort(BlockPos pos) {
		return formed && energyInputs.contains(pos) && level != null
			&& level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
	}
	public int getEnergyPerTick() { return energyPerTick; }
	public void onControllerRemoved() {
		if (level != null) for (BlockPos pos : energyInputs) {
			if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
		}
		energyInputs.clear();
		formed = false;
		resetProgress();
	}

	public boolean validateStructureNow() {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		validateStructure(serverLevel);
		validationDelay = VALIDATION_INTERVAL;
		return formed;
	}

	private void burst(ServerLevel level, net.minecraft.world.item.Item star) {
		Vector3f color = star == CrystalnexusModItems.ZERO_STAR.get() ? new Vector3f(0.35F, 1.0F, 1.0F)
			: star == CrystalnexusModItems.BLUE_STAR.get() ? new Vector3f(0.35F, 0.75F, 1.0F)
			: star == CrystalnexusModItems.PINK_STAR.get() ? new Vector3f(1.0F, 0.35F, 0.85F)
			: star == CrystalnexusModItems.ORANGE_STAR.get() ? new Vector3f(1.0F, 0.5F, 0.2F)
			: new Vector3f(1.0F, 0.85F, 0.35F);
		level.sendParticles(new DustParticleOptions(color, 1.4F), worldPosition.getX() + 0.5D,
			worldPosition.getY() + 2.4D, worldPosition.getZ() + 0.5D, 48, 0.7D, 0.7D, 0.7D, 0.12D);
	}

	private void resetProgress() {
		progress = 0;
		activeRecipe = null;
		activeEnchantment = null;
		activeCombining = false;
		energyPerTick = 0;
		activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
		sync();
	}

	public int getProgress() { return progress; }
	public boolean isProcessing() { return activeRecipe != null || activeEnchantment != null || activeCombining; }
	public boolean isFormed() { return formed; }
	public ContainerData data() { return data; }

	@Override protected Component getDefaultName() { return Component.translatable("block.crystalnexus.celestial_gear_forge"); }
	@Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CelestialGearForgeMenu(id, inventory, this); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
	@Override public void setItem(int slot, ItemStack stack) {
		if (slot == 0 && !ItemStack.isSameItemSameComponents(getItem(0), stack)) selectedEnchantment = null;
		super.setItem(slot, stack);
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 10 && net.crystalnexus.util.MachineUpgradeHelper.isZeroChip(stack)
                && !getItem(slot).isEmpty()) return false;
        if (slot == OUTPUT_SLOT + 1) return net.crystalnexus.util.MachineUpgradeHelper.isOutputUpgrade(stack);
		if (slot < 0 || slot >= INPUT_COUNT || stack.isEmpty()) return false;
		if (slot == STAR_SLOT) return isActiveStar(stack);
		if (slot == 0 && (stack.is(Items.ENCHANTED_BOOK) || stack.getItem().getEnchantmentValue(stack) > 0 || !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty())) return true;
		if (slot > 0 && slot < STAR_SLOT && stack.is(Items.ENCHANTED_BOOK)) return true;
		return level != null && level.getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
			.anyMatch(holder -> holder.value().matchesIngredient(slot, stack));
	}

	private static boolean isActiveStar(ItemStack stack) {
		return stack.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get()) || stack.is(CrystalnexusModItems.ORANGE_STAR.get())
			|| stack.is(CrystalnexusModItems.BLUE_STAR.get()) || stack.is(CrystalnexusModItems.PINK_STAR.get())
			|| stack.is(CrystalnexusModItems.ZERO_STAR.get());
	}

	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, stacks.size()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		stacks = NonNullList.withSize(INPUT_COUNT + 2, ItemStack.EMPTY);
		loadInventory(tag, stacks, registries, tag.getInt("inventoryVersion") < 2);
		progress = tag.getInt("progress");
		activeRecipe = tag.contains("activeRecipe") ? ResourceLocation.tryParse(tag.getString("activeRecipe")) : null;
		selectedEnchantment = tag.contains("selectedEnchantment") ? ResourceLocation.tryParse(tag.getString("selectedEnchantment")) : null;
		activeEnchantment = tag.contains("activeEnchantment") ? ResourceLocation.tryParse(tag.getString("activeEnchantment")) : null;
		activeCombining = tag.getBoolean("activeCombining");
		energy.deserializeNBT(registries, net.minecraft.nbt.IntTag.valueOf(Math.max(0, tag.getInt("energy"))));
		energyPerTick = Math.max(0, tag.getInt("energyPerTick"));
		activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
		if (tag.contains("activeInputs", net.minecraft.nbt.Tag.TAG_COMPOUND))
			loadInventory(tag.getCompound("activeInputs"), activeInputs, registries, tag.getInt("inventoryVersion") < 2);
		if (!isProcessing() || activeRecipe != null && (activeEnchantment != null || activeCombining)
			|| activeCombining && activeEnchantment != null || progress < 0 || progress >= DURATION) resetProgress();
		validationDelay = 0;
		formed = tag.getBoolean("formed");
	}

	private static void loadInventory(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries, boolean legacy) {
		if (!legacy) { ContainerHelper.loadAllItems(tag, items, registries); return; }
		NonNullList<ItemStack> oldItems = NonNullList.withSize(6, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, oldItems, registries);
		int[] slots = {0, 6, STAR_SLOT, 2, 4, OUTPUT_SLOT};
		for (int i = 0; i < slots.length; i++) if (slots[i] < items.size()) items.set(slots[i], oldItems.get(i));
	}

	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.putInt("inventoryVersion", 2);
		tag.putInt("progress", progress);
		tag.putBoolean("formed", formed);
		tag.putBoolean("activeCombining", activeCombining);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putInt("energyPerTick", energyPerTick);
		if (activeRecipe != null) tag.putString("activeRecipe", activeRecipe.toString());
		if (selectedEnchantment != null) tag.putString("selectedEnchantment", selectedEnchantment.toString());
		if (activeEnchantment != null) tag.putString("activeEnchantment", activeEnchantment.toString());
		CompoundTag inputs = new CompoundTag();
		ContainerHelper.saveAllItems(inputs, activeInputs, registries);
		tag.put("activeInputs", inputs);
	}

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}

	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
