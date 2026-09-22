package net.crystalnexus.block.entity;

import net.crystalnexus.block.CometForgeControllerBlock;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.multiblock.StructureNbtValidator;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.item.ResourceCometItem;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.world.inventory.CometForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public final class CometForgeControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
    public static final int TANK_CAPACITY = 100_000;
    public static final int DURATION = 200, ENERGY = 500_000, FLUID = 25_000;
    private NonNullList<ItemStack> activeInputs = NonNullList.withSize(4, ItemStack.EMPTY);
    private static final int VALIDATION_INTERVAL = 20;
    private static final int OUTPUT_SLOT = 4;
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "comet_forge");
    private NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
    private final FluidTank temporalFluid = new FluidTank(TANK_CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.TEMPORAL_ESSENCE.get())) {
        @Override protected void onContentsChanged() { sync(); }
    };
	private final EnergyStorage energyStorage = new EnergyStorage(
		CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.capacity(),
		CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxReceive(),
		CrystalnexusConfig.MACHINES.MACHINE_ENERGY_INPUT.maxExtract()) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int moved = super.receiveEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int moved = super.extractEnergy(amount, simulate); if (!simulate && moved > 0) sync(); return moved;
		}
	};
    private final List<BlockPos> energyInputs = new ArrayList<>();
    private final List<BlockPos> fluidInputs = new ArrayList<>();
    @Nullable private StructureNbtValidator.Match structure;
    @Nullable private Vec3 formationCenter;
    private boolean formed;
    @Nullable private ResourceLocation activeRecipe;
    private int progress;
    private int activeDuration;
    private long consumedEnergy;
    private int consumedFluid;
    private int validationDelay;

    public CometForgeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.COMET_FORGE_CONTROLLER.get(), pos, state);
    }

    public FluidTank getTemporalFluidTank() { return temporalFluid; }
    public int getProgress() { return progress; }
    public int getActiveDuration() { return activeDuration; }
    public boolean isFormed() { return formed; }
    @Nullable public Vec3 getFormationCenter() { return formationCenter; }
    public boolean validateStructureNow() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        validateStructure(serverLevel);
        validationDelay = VALIDATION_INTERVAL;
        return structure != null;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (validationDelay-- <= 0) { validateStructure(serverLevel); validationDelay = VALIDATION_INTERVAL; }
        if (activeRecipe != null && !sameInputs()) resetProgress();
        if (!formed || !hasIngredients()) return;
        ItemStack result = ResourceCometItem.create(stacks.get(3));
        ItemStack output = stacks.get(OUTPUT_SLOT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
            || output.getCount() >= output.getMaxStackSize())) return;
        if (activeRecipe == null) {
            activeRecipe = result.get(net.crystalnexus.init.CrystalnexusModDataComponents.MATERIAL.get());
            for (int i = 0; i < 4; i++) activeInputs.set(i, stacks.get(i).copyWithCount(1));
            activeDuration = DURATION;
        }
        long nextEnergy = GravitationalArrayCostSchedule.cumulative(ENERGY, progress + 1, DURATION);
        int nextFluid = (int) GravitationalArrayCostSchedule.cumulative(FLUID, progress + 1, DURATION);
        int fluidCost = nextFluid - consumedFluid;
        if (temporalFluid.getFluidAmount() < fluidCost) return;
        consumedEnergy += energyStorage.extractEnergy((int) (nextEnergy - consumedEnergy), false);
        setChanged();
        if (consumedEnergy < nextEnergy) return;
        temporalFluid.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        consumedFluid = nextFluid;
        progress++;
        if (progress == DURATION) {
            int remaining = 3;
            for (int i = 0; i < 3; i++) {
                int used = Math.min(remaining, stacks.get(i).getCount());
                stacks.get(i).shrink(used); remaining -= used;
            }
            stacks.get(3).shrink(stacks.get(3).getMaxStackSize());
            if (output.isEmpty()) stacks.set(OUTPUT_SLOT, result); else output.grow(1);
            resetProgress();
        }
        sync();
    }

    private boolean hasIngredients() {
        if (!ResourceCometItem.isMaterial(stacks.get(3))
            || stacks.get(3).getCount() < stacks.get(3).getMaxStackSize()) return false;
        int count = 0;
        for (int i = 0; i < 3; i++) {
            if (!stacks.get(i).isEmpty() && !ResourceCometItem.isSingularity(stacks.get(i))) return false;
            count += stacks.get(i).getCount();
        }
        return count >= 3;
    }

    private boolean sameInputs() {
        for (int i = 0; i < 4; i++)
            if (!ItemStack.isSameItemSameComponents(stacks.get(i), activeInputs.get(i))) return false;
        return true;
    }

    private void resetProgress() {
        activeRecipe = null; progress = 0; activeDuration = 0; consumedEnergy = 0; consumedFluid = 0;
        activeInputs = NonNullList.withSize(4, ItemStack.EMPTY);
        sync();
    }

    private void validateStructure(ServerLevel level) {
        Direction facing = getBlockState().getValue(CometForgeControllerBlock.FACING);
        Optional<StructureNbtValidator.Match> match = StructureNbtValidator.validate(level, STRUCTURE,
            worldPosition, facing, CrystalnexusModBlocks.COMET_FORGE_CONTROLLER.get(),
            CometForgeControllerBlock.FACING,
            Map.of(CrystalnexusModBlocks.METEORITE_ALLOY_BLOCK.get(), Set.of(
                CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get(), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())),
            true, false, Map.of());
        List<BlockPos> substitutions = match.map(StructureNbtValidator.Match::substitutionPositions).orElse(List.of());
        List<BlockPos> nextEnergy = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())).toList();
        List<BlockPos> nextFluid = substitutions.stream()
            .filter(pos -> level.getBlockState(pos).is(CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get())).toList();
        for (BlockPos old : List.copyOf(energyInputs)) {
            if (!nextEnergy.contains(old) && level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        energyInputs.clear();
        for (BlockPos pos : nextEnergy) {
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
                input.bindController(worldPosition);
                energyInputs.add(pos);
            }
        }
        for (BlockPos old : List.copyOf(fluidInputs)) {
            if (!nextFluid.contains(old) && level.getBlockEntity(old) instanceof MachineFluidInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        fluidInputs.clear();
        for (BlockPos pos : nextFluid) {
            if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input) {
                input.bindController(worldPosition);
                fluidInputs.add(pos);
            }
        }
        boolean wasFormed = formed;
        Vec3 previousCenter = formationCenter;
        structure = !energyInputs.isEmpty() && !fluidInputs.isEmpty()
            && energyInputs.size() + fluidInputs.size() == substitutions.size() ? match.orElse(null) : null;
        formed = structure != null;
        formationCenter = structure == null ? null : structure.center();
        if (wasFormed != formed || !Objects.equals(previousCenter, formationCenter)) sync();
    }

	@Override public IFluidHandler multiblockFluidInput() { return temporalFluid; }
    @Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }
    @Override public boolean acceptsMultiblockPort(BlockPos pos) {
        return formed && (energyInputs.contains(pos) || fluidInputs.contains(pos));
    }

    public void onControllerRemoved() {
        if (level == null) return;
        for (BlockPos pos : energyInputs) {
            if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        for (BlockPos pos : fluidInputs) {
            if (level.getBlockEntity(pos) instanceof MachineFluidInputBlockEntity input)
                input.unbindController(worldPosition);
        }
        energyInputs.clear();
        fluidInputs.clear();
        structure = null;
        formationCenter = null;
        formed = false;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.comet_forge_controller"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 3 ? ResourceCometItem.isSingularity(stack)
        : slot == 3 && ResourceCometItem.isMaterial(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, 5).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
        if (tag.get("temporalFluid") instanceof CompoundTag fluid) temporalFluid.readFromNBT(registries, fluid);
		if (tag.get("energy") instanceof IntTag energy) energyStorage.deserializeNBT(registries, energy);
        activeInputs = NonNullList.withSize(4, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("activeInputs"), activeInputs, registries);
        activeRecipe = tag.contains("activeRecipe") ? ResourceLocation.tryParse(tag.getString("activeRecipe")) : null;
        formed = tag.getBoolean("formed");
        formationCenter = tag.contains("formationX", Tag.TAG_DOUBLE)
            ? new Vec3(tag.getDouble("formationX"), tag.getDouble("formationY"), tag.getDouble("formationZ")) : null;
        progress = tag.getInt("progress");
        activeDuration = tag.getInt("activeDuration");
        consumedEnergy = tag.getLong("consumedEnergy");
        consumedFluid = tag.getInt("consumedFluid");
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
        tag.put("temporalFluid", temporalFluid.writeToNBT(registries, new CompoundTag()));
		tag.put("energy", energyStorage.serializeNBT(registries));
        CompoundTag inputs = new CompoundTag();
        ContainerHelper.saveAllItems(inputs, activeInputs, registries);
        tag.put("activeInputs", inputs);
        if (activeRecipe != null) tag.putString("activeRecipe", activeRecipe.toString());
        tag.putBoolean("formed", formed);
        if (formationCenter != null) {
            tag.putDouble("formationX", formationCenter.x);
            tag.putDouble("formationY", formationCenter.y);
            tag.putDouble("formationZ", formationCenter.z);
        }
        tag.putInt("progress", progress);
        tag.putInt("activeDuration", activeDuration);
        tag.putLong("consumedEnergy", consumedEnergy);
        tag.putInt("consumedFluid", consumedFluid);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CometForgeMenu(id, inventory, this);
    }
}
