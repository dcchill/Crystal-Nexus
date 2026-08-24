package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public final class MachineFluidInputBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1_000_000;
    private final FluidTank tank = new FluidTank(CAPACITY,
        stack -> stack.is(CrystalnexusModFluids.TEMPORAL_ESSENCE.get())
            || stack.is(CrystalnexusModFluids.ARGON.get()) || stack.is(Fluids.WATER)) {
        @Override protected void onContentsChanged() { sync(); }
    };
    private final IFluidHandler input = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tankIndex) { return tank.getFluidInTank(tankIndex); }
        @Override public int getTankCapacity(int tankIndex) { return tank.getTankCapacity(tankIndex); }
        @Override public boolean isFluidValid(int tankIndex, FluidStack stack) { return tank.isFluidValid(tankIndex, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return tank.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    @Nullable private BlockPos machineController;

    public MachineFluidInputBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.MACHINE_FLUID_INPUT.get(), pos, state);
    }

    public IFluidHandler getFluidInput() { return input; }

    public int transferTo(FluidTank target, int limit, BlockPos controller) {
        if (!controller.equals(machineController) || tank.isEmpty() || limit <= 0) return 0;
        FluidStack offered = tank.getFluid().copyWithAmount(Math.min(limit, tank.getFluidAmount()));
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted == 0) return 0;
        return target.fill(tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
    }

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

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.get("fluidTank") instanceof CompoundTag fluid) tank.readFromNBT(registries, fluid);
        String key = tag.contains("machineController", Tag.TAG_LONG) ? "machineController" : "gravitationalController";
        machineController = tag.contains(key, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(key)) : null;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("fluidTank", tank.writeToNBT(registries, new CompoundTag()));
        if (machineController != null) tag.putLong("machineController", machineController.asLong());
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
