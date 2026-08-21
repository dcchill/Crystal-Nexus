package net.crystalnexus.block.entity;

import net.crystalnexus.energy.GeneratorEnergyStorage;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public final class MachineEnergyOutputBlockEntity extends BlockEntity {
	public static final int CAPACITY = 100_000_000;
	public static final int MAX_TRANSFER = 10_000_000;
	private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(CAPACITY, MAX_TRANSFER, this::sync);
	@Nullable private BlockPos controller;

	public MachineEnergyOutputBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.MACHINE_ENERGY_OUTPUT.get(), pos, state);
	}

	public GeneratorEnergyStorage getEnergyStorage() { return energy; }
	public int generateEnergy(int amount) { return energy.generateEnergy(amount, false); }
	public void bindController(BlockPos pos) { if (!pos.equals(controller)) { controller = pos.immutable(); sync(); } }
	public void unbindController(BlockPos pos) { if (pos.equals(controller)) { controller = null; sync(); } }
	public boolean isBoundTo(BlockPos pos) { return pos.equals(controller); }

	public void pushEnergy() {
		if (level == null || level.isClientSide || energy.getEnergyStored() == 0) return;
		for (Direction direction : Direction.values()) {
			IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK,
				worldPosition.relative(direction), direction.getOpposite());
			if (target == null || !target.canReceive()) continue;
			int moved = target.receiveEnergy(Math.min(MAX_TRANSFER, energy.getEnergyStored()), false);
			if (moved > 0) energy.extractEnergy(moved, false);
			if (energy.getEnergyStored() == 0) break;
		}
	}

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
		controller = tag.contains("controller", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("controller")) : null;
	}
	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("energy", energy.serializeNBT(registries));
		if (controller != null) tag.putLong("controller", controller.asLong());
	}
	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}
	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
