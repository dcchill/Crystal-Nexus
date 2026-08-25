package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import javax.annotation.Nullable;

public final class MultiblockFluidOutputBlockEntity extends BlockEntity {
	@Nullable private BlockPos controller;
	private final IFluidHandler output = new IFluidHandler() {
		@Override public int getTanks() { IFluidHandler target = target(); return target == null ? 0 : target.getTanks(); }
		@Override public FluidStack getFluidInTank(int index) { IFluidHandler target = target(); return target == null ? FluidStack.EMPTY : target.getFluidInTank(index); }
		@Override public int getTankCapacity(int index) { IFluidHandler target = target(); return target == null ? 0 : target.getTankCapacity(index); }
		@Override public boolean isFluidValid(int index, FluidStack stack) { return false; }
		@Override public int fill(FluidStack resource, FluidAction action) { return 0; }
		@Override public FluidStack drain(FluidStack resource, FluidAction action) { IFluidHandler target = target(); return target == null ? FluidStack.EMPTY : target.drain(resource, action); }
		@Override public FluidStack drain(int amount, FluidAction action) { IFluidHandler target = target(); return target == null ? FluidStack.EMPTY : target.drain(amount, action); }
	};

	public MultiblockFluidOutputBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.MULTIBLOCK_FLUID_OUTPUT.get(), pos, state);
	}

	public IFluidHandler getFluidOutput() { return output; }
	public void bindController(BlockPos pos) { if (!pos.equals(controller)) { controller = pos.immutable(); sync(); } }
	public void unbindController(BlockPos pos) { if (pos.equals(controller)) { controller = null; sync(); } }
	public boolean isBoundTo(BlockPos pos) { return pos.equals(controller); }

	@Nullable private IFluidHandler target() {
		if (level == null || controller == null
			|| !(level.getBlockEntity(controller) instanceof MultiblockPortTarget target)) return null;
		return target.multiblockFluidOutput();
	}

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.loadAdditional(tag, provider);
		controller = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
	}
	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.saveAdditional(tag, provider);
		if (controller != null) tag.putLong("controller", controller.asLong());
	}
	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}
}
