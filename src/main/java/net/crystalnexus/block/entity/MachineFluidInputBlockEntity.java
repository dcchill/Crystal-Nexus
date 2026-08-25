package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public final class MachineFluidInputBlockEntity extends BlockEntity {
    private final IFluidHandler input = new IFluidHandler() {
        @Override public int getTanks() { IFluidHandler target = target(); return target == null ? 0 : target.getTanks(); }
        @Override public FluidStack getFluidInTank(int tankIndex) { IFluidHandler target = target(); return target == null ? FluidStack.EMPTY : target.getFluidInTank(tankIndex); }
        @Override public int getTankCapacity(int tankIndex) { IFluidHandler target = target(); return target == null ? 0 : target.getTankCapacity(tankIndex); }
        @Override public boolean isFluidValid(int tankIndex, FluidStack stack) { IFluidHandler target = target(); return target != null && target.isFluidValid(tankIndex, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { IFluidHandler target = target(); return target == null ? 0 : target.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    @Nullable private BlockPos machineController;

    public MachineFluidInputBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.MACHINE_FLUID_INPUT.get(), pos, state);
    }

    public IFluidHandler getFluidInput() { return input; }

    public void bindGravitationalController(BlockPos controller) {
        bindController(controller);
    }

    public void unbindGravitationalController(BlockPos controller) {
        unbindController(controller);
    }

    public void bindController(BlockPos controller) {
        if (controller.equals(machineController)) return;
        machineController = controller.immutable();
        sync();
    }

    public void unbindController(BlockPos controller) {
        if (!controller.equals(machineController)) return;
        machineController = null;
        sync();
    }

    public boolean isBoundTo(BlockPos controller) { return controller.equals(machineController); }

    @Nullable private IFluidHandler target() {
        if (level == null || machineController == null
            || !(level.getBlockEntity(machineController) instanceof MultiblockPortTarget target)) return null;
        return target.multiblockFluidInput();
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String key = tag.contains("machineController", Tag.TAG_LONG) ? "machineController" : "gravitationalController";
        machineController = tag.contains(key, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(key)) : null;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (machineController != null) tag.putLong("machineController", machineController.asLong());
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
