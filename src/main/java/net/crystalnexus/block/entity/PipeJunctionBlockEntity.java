package net.crystalnexus.block.entity;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.block.PipeStraightBlock;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.List;

public class PipeJunctionBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private static final int MAX_TRANSFER = 100;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(0, ItemStack.EMPTY);
	private int nextOutput;

	public PipeJunctionBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.PIPE_JUNCTION.get(), position, state);
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		if (compound.get("fluidTank") instanceof CompoundTag compoundTag)
			fluidTank.readFromNBT(lookupProvider, compoundTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("fluidTank", fluidTank.writeToNBT(lookupProvider, new CompoundTag()));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("pipe_junction");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Pipe Junction");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		return true;
	}

	private final FluidTank fluidTank = new FluidTank(6000) {
		@Override
		protected void onContentsChanged() {
			super.onContentsChanged();
			setChanged();
			level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
		}
	};

	public FluidTank getFluidTank() {
		return fluidTank;
	}

	public static void tick(Level level, BlockPos pos, PipeJunctionBlockEntity junction) {
		FluidStack available = junction.fluidTank.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
		if (available.isEmpty()) return;

		List<IFluidHandler> outputs = new ArrayList<>();
		List<Integer> capacities = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborState = level.getBlockState(neighborPos);
			if (!(neighborState.getBlock() instanceof PipeStraightBlock)
					|| neighborState.getValue(PipeStraightBlock.FACING) != direction) continue;

			IFluidHandler handler = level.getCapability(
					Capabilities.FluidHandler.BLOCK, neighborPos, direction.getOpposite());
			if (handler == null) continue;

			int capacity = handler.fill(available, IFluidHandler.FluidAction.SIMULATE);
			if (capacity > 0) {
				outputs.add(handler);
				capacities.add(capacity);
			}
		}

		if (outputs.isEmpty()) return;
		int[] capacityArray = capacities.stream().mapToInt(Integer::intValue).toArray();
		int[] shares = FluidSplitMath.fairShares(available.getAmount(), capacityArray, junction.nextOutput);
		junction.nextOutput = (junction.nextOutput + 1) % outputs.size();

		for (int i = 0; i < outputs.size(); i++) {
			if (shares[i] == 0) continue;
			FluidStack share = available.copy();
			share.setAmount(shares[i]);
			int moved = outputs.get(i).fill(share, IFluidHandler.FluidAction.EXECUTE);
			if (moved > 0) junction.fluidTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
		}
	}

}
