package net.crystalnexus.block.entity;


import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.energy.EnergyStorage;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.procedures.MachineCoreOnTickUpdateProcedure;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

public class MachineEnergyInputBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(0, ItemStack.EMPTY);
	@Nullable private BlockPos machineController;

	public MachineEnergyInputBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.MACHINE_ENERGY_INPUT.get(), position, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MachineEnergyInputBlockEntity blockEntity) {
		if (level.isClientSide())
			return;
		blockEntity.pushBufferedEnergy();
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		String controllerKey = compound.contains("machineController", Tag.TAG_LONG) ? "machineController" : "gravitationalController";
		machineController = compound.contains(controllerKey, Tag.TAG_LONG) ? BlockPos.of(compound.getLong(controllerKey)) : null;
		if (compound.get("energyBuffer") instanceof IntTag stored) energyBuffer.deserializeNBT(lookupProvider, stored);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		if (machineController != null) compound.putLong("machineController", machineController.asLong());
		compound.put("energyBuffer", energyBuffer.serializeNBT(lookupProvider));
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
		return Component.literal("machine_energy_input");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Machine Energy Input");
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

	/**
	 * Local ingress buffer. Cables can fill the hatch before or during a structure
	 * rescan; energy is then pushed transactionally into the bound controller.
	 */
	private final EnergyStorage energyBuffer = new EnergyStorage(16_000_000, 1_000_000, 1_000_000) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int accepted = super.receiveEnergy(amount, simulate);
			if (!simulate && accepted > 0) { setChanged(); sync(); }
			return accepted;
		}
	};

	public IEnergyStorage getEnergyStorage() {
		return energyBuffer;
	}

	private void pushBufferedEnergy() {
		if (energyBuffer.getEnergyStored() <= 0) return;
		if (level != null && machineController != null && level.hasChunkAt(machineController)
			&& level.getBlockEntity(machineController) instanceof AssemblyLineControllerBlockEntity assembly) {
			int moved = assembly.distributeEnergyFrom(energyBuffer);
			if (moved > 0) { setChanged(); sync(); }
			// Always return after trying controller distribution - don't fall through to cable fallback
			return;
		}
		IEnergyStorage destination = target();
		if (destination == null || !destination.canReceive()) return;
		int offered = Math.min(1_000_000, energyBuffer.getEnergyStored());
		int accepted = destination.receiveEnergy(offered, false);
		if (accepted > 0) {
			energyBuffer.extractEnergy(accepted, false);
			setChanged(); sync();
		}

	}

	private void sync() {
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}

	@Nullable private IEnergyStorage target() {
		if (level == null || machineController == null
			|| !level.hasChunkAt(machineController)
			|| !(level.getBlockEntity(machineController) instanceof MultiblockPortTarget target)
			|| !target.acceptsMultiblockPort(worldPosition)) return null;
		return target.multiblockEnergyInput();
	}

	public void bindGravitationalController(BlockPos controller) {
		bindController(controller);
	}

	public void unbindGravitationalController(BlockPos controller) {
		unbindController(controller);
	}

	public boolean isBoundTo(BlockPos controller) {
		return controller.equals(machineController);
	}

	public void bindController(BlockPos controller) {
		if (controller.equals(machineController)) return;
		machineController = controller.immutable();
		setChanged();
	}

	public void unbindController(BlockPos controller) {
		if (!controller.equals(machineController)) return;
		machineController = null;
		setChanged();
	}
}
