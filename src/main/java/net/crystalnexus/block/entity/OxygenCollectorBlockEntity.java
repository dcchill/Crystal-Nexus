package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.procedures.OxygenCollectorOnTickUpdateProcedure;
import net.crystalnexus.world.inventory.NodeExtractorGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class OxygenCollectorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);

	public OxygenCollectorBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.OXYGEN_COLLECTOR.get(), position, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, OxygenCollectorBlockEntity blockEntity) {
		if (!(level instanceof ServerLevel serverLevel))
			return;
		OxygenCollectorOnTickUpdateProcedure.execute(serverLevel, pos);
	}

	@Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
		super.loadAdditional(tag, lookup);
		if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, lookup);
		if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(lookup, energy);
		if (tag.get("fluidTank") instanceof CompoundTag fluid) fluidTank.readFromNBT(lookup, fluid);
	}
	@Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
		super.saveAdditional(tag, lookup);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, lookup);
		tag.put("energyStorage", energyStorage.serializeNBT(lookup));
		tag.put("fluidTank", fluidTank.writeToNBT(lookup, new CompoundTag()));
	}
	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithFullMetadata(lookup); }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
	@Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.atmosphere_collector"); }
	@Override public Component getDisplayName() { return getDefaultName(); }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return new NodeExtractorGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition)); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override public boolean canPlaceItem(int index, ItemStack stack) { return true; }
	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) { return canPlaceItem(index, stack); }
	@Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) { return false; }

	private final EnergyStorage energyStorage = new EnergyStorage(CrystalnexusConfig.MACHINES.NODE_EXTRACTOR.capacity(), CrystalnexusConfig.MACHINES.NODE_EXTRACTOR.maxReceive(), CrystalnexusConfig.MACHINES.NODE_EXTRACTOR.maxExtract(), 0) {
		@Override public int receiveEnergy(int amount, boolean simulate) { int accepted = super.receiveEnergy(amount, simulate); if (!simulate && accepted > 0) sync(); return accepted; }
		@Override public int extractEnergy(int amount, boolean simulate) { int extracted = super.extractEnergy(amount, simulate); if (!simulate && extracted > 0) sync(); return extracted; }
	};
	private final FluidTank fluidTank = new FluidTank(8000) { @Override protected void onContentsChanged() { super.onContentsChanged(); sync(); } };
	private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
	public EnergyStorage getEnergyStorage() { return energyStorage; }
	public FluidTank getFluidTank() { return fluidTank; }
}
