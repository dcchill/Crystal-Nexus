package net.crystalnexus.block.entity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class EnergyCableBlockEntity extends BlockEntity implements WorldlyContainer {

	// Tune these
	private static final int BUFFER_CAPACITY = 100000;
	private static final int MAX_IO_PER_TICK = 50000;

	// MCreator-style EnergyStorage (serializeNBT/deserializeNBT uses IntTag)
	private final EnergyStorage energyStorage = new EnergyStorage(BUFFER_CAPACITY, MAX_IO_PER_TICK, MAX_IO_PER_TICK, 0) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int ret = super.receiveEnergy(maxReceive, simulate);
			if (!simulate && ret > 0) {
				setChanged();
				if (level != null)
					level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return ret;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			int ret = super.extractEnergy(maxExtract, simulate);
			if (!simulate && ret > 0) {
				setChanged();
				if (level != null)
					level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return ret;
		}
	};

	public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.ENERGY_CABLE.get(), pos, state);
	}

	// MCreator locked capability registration calls this
	public EnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	// ----------------------------
	// Receiver finding: TRUST SIMULATION (works for your blocks + Mekanism)
	// ----------------------------

	private @Nullable IEnergyStorage findReceiverBySim(ILevelExtension ext, BlockPos pos, Direction fromCableToNeighbor, int offer) {
		// 1) Try face looking back at cable (common for Mek and most mods)
		Direction intoNeighbor = fromCableToNeighbor.getOpposite();
		IEnergyStorage s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, intoNeighbor);
		if (s != null && s.canReceive() && s.receiveEnergy(offer, true) > 0)
			return s;

		// 2) Try NULL (your MCreator blocks often accept this way)
		s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (s != null && s.canReceive() && s.receiveEnergy(offer, true) > 0)
			return s;

		// 3) Try all sides (covers weird wrappers)
		for (Direction d : Direction.values()) {
			s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
			if (s != null && s.canReceive() && s.receiveEnergy(offer, true) > 0)
				return s;
		}

		return null;
	}

	/**
	 * Call from your block ticker on the server.
	 * - Pushes energy into neighboring machines/cubes.
	 * - Equalizes with neighboring cables so energy travels down lines.
	 */
	public void serverTick() {
		Level lvl = level;
		if (lvl == null || lvl.isClientSide) return;
		if (!(lvl instanceof ILevelExtension ext)) return;

		// ----------------------------
		// 1) PUSH: cable -> non-cable neighbors
		// ----------------------------
		if (energyStorage.getEnergyStored() > 0) {
			for (Direction dir : Direction.values()) {
				if (energyStorage.getEnergyStored() <= 0) break;

				BlockPos nPos = worldPosition.relative(dir);

				// Don't push into other cables here (handled by equalize section)
				BlockEntity nBe = lvl.getBlockEntity(nPos);
				if (nBe instanceof EnergyCableBlockEntity) continue;

				int offer = Math.min(MAX_IO_PER_TICK, energyStorage.getEnergyStored());
				if (offer <= 0) break;

				IEnergyStorage receiver = findReceiverBySim(ext, nPos, dir, offer);
				if (receiver == null) continue;

				int acceptedSim = receiver.receiveEnergy(offer, true);
				if (acceptedSim <= 0) continue;

				int extracted = energyStorage.extractEnergy(acceptedSim, false);
				if (extracted > 0) {
					receiver.receiveEnergy(extracted, false);
				}
			}
		}

		// ----------------------------
		// 2) EQUALIZE: cable <-> cable
		// Moves energy along cable lines without ping-pong.
		// ----------------------------
		for (Direction dir : Direction.values()) {
			int myStored = energyStorage.getEnergyStored();
			if (myStored <= 1) break;

			BlockPos nPos = worldPosition.relative(dir);
			BlockEntity be = lvl.getBlockEntity(nPos);
			if (!(be instanceof EnergyCableBlockEntity otherCable)) continue;

			IEnergyStorage other = otherCable.getEnergyStorage();
			int otherStored = other.getEnergyStored();

			// Only move from higher -> lower
			if (otherStored >= myStored) continue;

			int diff = myStored - otherStored;
			int move = diff / 2;
			if (move <= 0) continue;

			move = Math.min(move, MAX_IO_PER_TICK);

			int otherSpace = other.getMaxEnergyStored() - otherStored;
			move = Math.min(move, otherSpace);
			if (move <= 0) continue;

			int extracted = energyStorage.extractEnergy(move, false);
			if (extracted > 0) {
				other.receiveEnergy(extracted, false);
			}
		}
	}

	// ----------------------------
	// NBT (MCreator-style)
	// ----------------------------

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (compound.get("energyStorage") instanceof IntTag intTag) {
			energyStorage.deserializeNBT(lookupProvider, intTag);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));
	}

	// ----------------------------
	// WorldlyContainer (empty inventory)
	// Exists ONLY to satisfy MCreator's auto ItemHandler registration.
	// ----------------------------

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		// no-op
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		// no-op
	}
}
