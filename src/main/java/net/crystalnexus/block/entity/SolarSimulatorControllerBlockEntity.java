package net.crystalnexus.block.entity;

import net.crystalnexus.block.SolarSimulatorControllerBlock;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.world.inventory.SolarSimulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public final class SolarSimulatorControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
    public static final int DURATION = 3;
    private static final int ENERGY_PER_ITEM = 200_000;
    private static final int STAR_SLOT = 4;
    private static final int REQUIRED_ENERGY_TRANSFER = GravitationalArrayCostSchedule.maximumStep(
        (long) STAR_SLOT * 8 * ENERGY_PER_ITEM, DURATION);
    private static final int VALIDATION_INTERVAL = 20;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "solar_sim");
    private static final List<TagKey<Item>> TERRA = tags("raw_materials/iron", "raw_materials/copper", "gems/coal", "raw_materials/tin", "raw_materials/silver");
    private static final List<TagKey<Item>> CAELUS = tags("raw_materials/gold", "raw_materials/lead", "dusts/redstone", "raw_materials/nickel");
    private static final List<TagKey<Item>> BOREAS = tags("gems/diamond", "gems/quartz", "gems/certus_quartz", "raw_materials/titanium");
    private static final List<TagKey<Item>> METEOR = tags("raw_materials/tungsten", "raw_materials/uranium", "raw_materials/platinum");

    private NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
	private final EnergyStorage energyStorage = new EnergyStorage(
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.capacity(), REQUIRED_ENERGY_TRANSFER),
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxReceive(), REQUIRED_ENERGY_TRANSFER),
		Math.max(CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxExtract(), REQUIRED_ENERGY_TRANSFER)) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int moved = super.receiveEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int moved = super.extractEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
	};
    private final List<BlockPos> energyInputs = new ArrayList<>();
    private final List<BlockPos> outputs = new ArrayList<>();
    private boolean formed;
    @Nullable private Vec3 formationCenter;
    private boolean renderActive;
    private int inactiveRenderTicks;
    private int progress;
    private long consumedEnergy;
    private int validationDelay;

    public SolarSimulatorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.SOLAR_SIMULATOR_CONTROLLER.get(), pos, state);
    }

    public boolean isFormed() { return formed; }
    public int getProgress() { return progress; }
    public int getDuration() { return DURATION; }
    public boolean isRenderActive() { return formed && renderActive; }
    @Nullable public Vec3 getFormationCenter() { return formationCenter; }

    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return formed;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) {
            validateStructure(serverLevel);
            validationDelay = VALIDATION_INTERVAL;
        }
        int multiplier = starMultiplier(stacks.get(STAR_SLOT));
        int planets = planetCount();
        if (!formed || multiplier == 0 || planets == 0) {
            if (progress != 0 || consumedEnergy != 0 || renderActive) {
                progress = 0; consumedEnergy = 0; renderActive = false; inactiveRenderTicks = 0; sync();
            }
            return;
        }

        // Older worlds may have saved the former false-complete value.
        progress = Math.min(progress, DURATION - 1);
        List<ItemStack> results = progress == DURATION - 1 ? createResults(serverLevel, multiplier) : List.of();
        if (progress == DURATION - 1 && (results.isEmpty() || !canFit(results))) { markRenderInactive(); return; }

        long totalEnergy = (long) planets * multiplier * ENERGY_PER_ITEM;
        long nextEnergy = GravitationalArrayCostSchedule.cumulative(totalEnergy, progress + 1, DURATION);
        int extracted = extractEnergy((int) (nextEnergy - consumedEnergy), false);
        consumedEnergy += extracted;
        if (extracted > 0) markRenderActive(); else markRenderInactive();
        if (consumedEnergy < nextEnergy) return;

        progress++;
        if (progress >= DURATION) {
            insert(results);
            progress = 0;
            consumedEnergy = 0;
            sync();
        } else if (progress % 20 == 0) sync();
    }

    private void validateStructure(ServerLevel level) {
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE, worldPosition,
            getBlockState().getValue(SolarSimulatorControllerBlock.FACING), CrystalnexusModBlocks.SOLAR_SIMULATOR_CONTROLLER.get(),
            SolarSimulatorControllerBlock.FACING, Map.of(
                CrystalnexusModBlocks.CARBON_BLOCK.get(), Set.of(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get()),
                CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get(), Set.of(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get()),
                CrystalnexusModBlocks.EE_BATTERY.get(), Set.of(Blocks.AIR, CrystalnexusModBlocks.CARBON_BLOCK.get(), CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get(),
                    CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get(), CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())),
            Set.of(), true, false);
        List<BlockPos> previousEnergyInputs = List.copyOf(energyInputs);
        energyInputs.clear();
        outputs.clear();
        match.ifPresent(found -> {
            energyInputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())).toList());
            outputs.addAll(found.substitutionPositions().stream()
                .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get())).toList());
        });
        for (BlockPos old : previousEnergyInputs) {
            if (!energyInputs.contains(old) && level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        energyInputs.removeIf(pos -> {
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
                input.bindController(worldPosition);
                return false;
            }
            return true;
        });
        boolean next = match.isPresent() && !energyInputs.isEmpty() && !outputs.isEmpty()
            && outputs.stream().allMatch(pos -> level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity);
        Vec3 nextCenter = next ? match.orElseThrow().center() : null;
        if (formed != next || !Objects.equals(formationCenter, nextCenter)) {
            formed = next; formationCenter = nextCenter; sync();
        } else formed = next;
    }

    private List<ItemStack> createResults(ServerLevel level, int multiplier) {
        List<ItemStack> results = new ArrayList<>();
        for (int slot = 0; slot < STAR_SLOT; slot++) {
            List<TagKey<Item>> pool = planetPool(stacks.get(slot));
            List<Item> available = pool.stream().map(this::firstItem).flatMap(Optional::stream).toList();
            if (!available.isEmpty()) results.add(new ItemStack(available.get(level.random.nextInt(available.size())), multiplier));
        }
        return results;
    }

    private Optional<Item> firstItem(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag).flatMap(items -> items.stream().findFirst()).map(holder -> holder.value());
    }

    private boolean canFit(List<ItemStack> results) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (BlockPos pos : outputs) if (level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)
            for (int slot = 0; slot < output.getContainerSize(); slot++) snapshot.add(output.getItem(slot).copy());
        for (ItemStack result : results) if (!insertInto(snapshot, result.copy())) return false;
        return true;
    }

    private void insert(List<ItemStack> results) {
        for (ItemStack result : results) {
            ItemStack remaining = result.copy();
            for (BlockPos pos : outputs) {
                if (!(level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)) continue;
                for (int slot = 0; slot < output.getContainerSize() && !remaining.isEmpty(); slot++) {
                    ItemStack current = output.getItem(slot);
                    if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) continue;
                    int room = current.isEmpty() ? remaining.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
                    int moved = Math.min(room, remaining.getCount());
                    if (current.isEmpty()) output.setItem(slot, remaining.copyWithCount(moved)); else current.grow(moved);
                    remaining.shrink(moved);
                }
                output.setChanged();
            }
        }
    }

    private static boolean insertInto(List<ItemStack> slots, ItemStack remaining) {
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            ItemStack current = slots.get(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) continue;
            int room = current.isEmpty() ? remaining.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
            int moved = Math.min(room, remaining.getCount());
            if (current.isEmpty()) slots.set(slot, remaining.copyWithCount(moved)); else current.grow(moved);
            remaining.shrink(moved);
        }
        return remaining.isEmpty();
    }

    private int extractEnergy(int amount, boolean simulate) {
		return energyStorage.extractEnergy(amount, simulate);
    }

	@Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }

    public void onControllerRemoved() {
        if (level != null) for (BlockPos pos : energyInputs)
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
        energyInputs.clear();
        outputs.clear();
        formed = false;
        formationCenter = null;
        renderActive = false;
    }

    private void markRenderActive() {
        inactiveRenderTicks = 0;
        if (!renderActive) { renderActive = true; sync(); }
    }

    private void markRenderInactive() {
        if (renderActive && ++inactiveRenderTicks >= 20) { renderActive = false; inactiveRenderTicks = 0; sync(); }
    }

    private int planetCount() { return (int) IntStream.range(0, STAR_SLOT).filter(slot -> !planetPool(stacks.get(slot)).isEmpty()).count(); }
    private static List<TagKey<Item>> planetPool(ItemStack stack) {
        if (stack.is(CrystalnexusModItems.METEOR.get())) return METEOR;
        if (stack.is(CrystalnexusModItems.TERRA.get())) return TERRA;
        if (stack.is(CrystalnexusModItems.CAELUS.get())) return CAELUS;
        if (stack.is(CrystalnexusModItems.BOREAS.get())) return BOREAS;
        return List.of();
    }
    private static int starMultiplier(ItemStack stack) {
        if (stack.is(CrystalnexusModItems.YELLOW_DWARF_STAR.get())) return 1;
        if (stack.is(CrystalnexusModItems.ORANGE_STAR.get())) return 2;
        if (stack.is(CrystalnexusModItems.BLUE_STAR.get())) return 4;
        if (stack.is(CrystalnexusModItems.PINK_STAR.get())) return 8;
        return 0;
    }
    private static List<TagKey<Item>> tags(String... paths) {
        return java.util.Arrays.stream(paths).map(path -> TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path))).toList();
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.solar_simulator_controller"); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == STAR_SLOT ? starMultiplier(stack) > 0 : !planetPool(stack).isEmpty(); }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, 5).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SolarSimulatorMenu(id, inventory, this); }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
		if (tag.get("energy") instanceof IntTag energy) energyStorage.deserializeNBT(registries, energy);
        formed = tag.getBoolean("formed"); progress = tag.getInt("progress"); consumedEnergy = tag.getLong("consumedEnergy");
        renderActive = tag.getBoolean("renderActive");
        formationCenter = tag.contains("formationX")
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.put("energy", energyStorage.serializeNBT(registries));
        tag.putBoolean("formed", formed); tag.putInt("progress", progress); tag.putLong("consumedEnergy", consumedEnergy);
        tag.putBoolean("renderActive", renderActive);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x); tag.putDouble("formationY", formationCenter.y); tag.putDouble("formationZ", formationCenter.z);
        }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
