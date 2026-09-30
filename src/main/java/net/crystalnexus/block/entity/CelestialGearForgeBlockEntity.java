package net.crystalnexus.block.entity;

import net.crystalnexus.block.CelestialGearForgeBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.CelestialGearForgeRecipe;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

public final class CelestialGearForgeBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int INPUT_COUNT = 5;
	public static final int OUTPUT_SLOT = 5;
	public static final int DURATION = 200;
	private static final int VALIDATION_INTERVAL = 20;
	private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge");
	private NonNullList<ItemStack> stacks = NonNullList.withSize(INPUT_COUNT + 1, ItemStack.EMPTY);
	private NonNullList<ItemStack> activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
	@Nullable private ResourceLocation activeRecipe;
	private int progress;
	private int validationDelay;
	private boolean formed;
	private final ContainerData data = new ContainerData() {
		@Override public int get(int index) { return index == 0 ? progress : index == 1 ? DURATION : formed ? 1 : 0; }
		@Override public void set(int index, int value) { if (index == 0) progress = value; else if (index == 2) formed = value != 0; }
		@Override public int getCount() { return 3; }
	};

	public CelestialGearForgeBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.CELESTIAL_GEAR_FORGE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CelestialGearForgeBlockEntity forge) {
		if (!level.isClientSide) forge.serverTick();
	}

	public void serverTick() {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (validationDelay-- <= 0) {
			validateStructure(serverLevel);
			validationDelay = VALIDATION_INTERVAL;
		}
		if (!formed) {
			if (activeRecipe != null) resetProgress();
			return;
		}

		var recipe = activeRecipe == null ? findRecipe(serverLevel, stacks)
			: serverLevel.getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
				.filter(holder -> holder.id().equals(activeRecipe)).findFirst().orElse(null);
		if (recipe == null || !recipe.value().matches(inputStacks()) || !hasSameActiveInputs()) {
			if (activeRecipe != null) resetProgress();
			return;
		}
		if (!canAcceptOutput(recipe.value().output())) {
			if (activeRecipe == null) return;
			resetProgress();
			return;
		}
		if (activeRecipe == null) {
			activeRecipe = recipe.id();
			for (int slot = 0; slot < INPUT_COUNT; slot++) activeInputs.set(slot, getItem(slot).copyWithCount(1));
		}

		progress++;
		setChanged();
		if (progress >= DURATION) {
			ItemStack result = recipe.value().output();
			net.minecraft.world.item.Item star = getItem(2).getItem();
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
		if (activeRecipe == null) return true;
		for (int slot = 0; slot < INPUT_COUNT; slot++)
			if (getItem(slot).isEmpty() || !ItemStack.isSameItemSameComponents(activeInputs.get(slot), getItem(slot))) return false;
		return true;
	}

	private boolean canAcceptOutput(ItemStack result) {
		ItemStack output = getItem(OUTPUT_SLOT);
		return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
			&& output.getCount() + result.getCount() <= output.getMaxStackSize();
	}

	private void validateStructure(ServerLevel serverLevel) {
		boolean nextFormed = StructureNbtValidator.validate(serverLevel, STRUCTURE, worldPosition,
			getBlockState().getValue(CelestialGearForgeBlock.FACING), CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get(),
			CelestialGearForgeBlock.FACING, java.util.Map.of(), false, false).isPresent();
		if (formed != nextFormed) { formed = nextFormed; sync(); }
	}

	public boolean validateStructureNow() {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		validateStructure(serverLevel);
		validationDelay = VALIDATION_INTERVAL;
		return formed;
	}

	private void burst(ServerLevel level, net.minecraft.world.item.Item star) {
		Vector3f color = star == CrystalnexusModItems.BLUE_STAR.get() ? new Vector3f(0.35F, 0.75F, 1.0F)
			: star == CrystalnexusModItems.PINK_STAR.get() ? new Vector3f(1.0F, 0.35F, 0.85F)
			: star == CrystalnexusModItems.ORANGE_STAR.get() ? new Vector3f(1.0F, 0.5F, 0.2F)
			: new Vector3f(1.0F, 0.85F, 0.35F);
		level.sendParticles(new DustParticleOptions(color, 1.4F), worldPosition.getX() + 0.5D,
			worldPosition.getY() + 2.4D, worldPosition.getZ() + 0.5D, 48, 0.7D, 0.7D, 0.7D, 0.12D);
	}

	private void resetProgress() {
		progress = 0;
		activeRecipe = null;
		activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
		sync();
	}

	public int getProgress() { return progress; }
	public boolean isProcessing() { return activeRecipe != null; }
	public boolean isFormed() { return formed; }
	public ContainerData data() { return data; }

	@Override protected Component getDefaultName() { return Component.translatable("block.crystalnexus.celestial_gear_forge"); }
	@Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CelestialGearForgeMenu(id, inventory, this); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot < 0 || slot >= INPUT_COUNT || stack.isEmpty()) return false;
		if (slot == 2) return isActiveStar(stack);
		return level != null && level.getRecipeManager().getAllRecipesFor(CelestialGearForgeRecipe.Type.INSTANCE).stream()
			.anyMatch(holder -> holder.value().matchesIngredient(slot, stack));
	}

	private static boolean isActiveStar(ItemStack stack) {
		return stack.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get()) || stack.is(CrystalnexusModItems.ORANGE_STAR.get())
			|| stack.is(CrystalnexusModItems.BLUE_STAR.get()) || stack.is(CrystalnexusModItems.PINK_STAR.get());
	}

	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, stacks.size()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		stacks = NonNullList.withSize(INPUT_COUNT + 1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
		progress = tag.getInt("progress");
		activeRecipe = tag.contains("activeRecipe") ? ResourceLocation.tryParse(tag.getString("activeRecipe")) : null;
		activeInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
		if (tag.contains("activeInputs", net.minecraft.nbt.Tag.TAG_COMPOUND))
			ContainerHelper.loadAllItems(tag.getCompound("activeInputs"), activeInputs, registries);
		if (activeRecipe == null || progress < 0 || progress >= DURATION) resetProgress();
	}

	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.putInt("progress", progress);
		if (activeRecipe != null) tag.putString("activeRecipe", activeRecipe.toString());
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
