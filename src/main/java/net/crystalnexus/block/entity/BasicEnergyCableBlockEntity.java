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

public class BasicEnergyCableBlockEntity extends BlockEntity implements WorldlyContainer {

	// Tune these
	private static final int BUFFER_CAPACITY = 10240;
	private static final int MAX_IO_PER_TICK = 5120;


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

	public BasicEnergyCableBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.BASIC_ENERGY_CABLE.get(), pos, state);
	}


	public EnergyStorage getEnergyStorage() {
		return energyStorage;
	}
// Track neighbor energy over time to detect true generators/sources.
// Key is packed relative position offset (dx,dy,dz) into a small int.
private final int[] lastNeighborEnergy = new int[6]; // one per Direction.ordinal()

private int dirIndex(Direction d) {
    return d.ordinal(); // DOWN(0) UP(1) NORTH(2) SOUTH(3) WEST(4) EAST(5)
}

	// ----------------------------
	// Capability helpers
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
private boolean isLikelyProducer(ILevelExtension ext, Level lvl, BlockPos nPos, Direction dir) {
    IEnergyStorage nNull = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, null);
    IEnergyStorage nSide = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, dir.getOpposite());
    IEnergyStorage n = (nSide != null ? nSide : nNull);
    if (n == null) return false;

    int now = n.getEnergyStored();
    int prev = lastNeighborEnergy[dirIndex(dir)];

    // Producer heuristic:
    // If energy rose since last tick, it is producing (or being filled).
    // That makes it safe to pull from.
    return (now - prev) >= 32; // only treat as producer if it rose by at least 32 FE in a tick
}

	private @Nullable IEnergyStorage findExtractorBySim(ILevelExtension ext, BlockPos pos, Direction fromCableToNeighbor, int want) {
		// 1) Try face looking back at cable (common)
		Direction fromNeighbor = fromCableToNeighbor.getOpposite();
		IEnergyStorage s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, fromNeighbor);
		if (s != null && s.canExtract() && s.extractEnergy(want, true) > 0)
			return s;

		// 2) Try NULL (your blocks often expose here)
		s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (s != null && s.canExtract() && s.extractEnergy(want, true) > 0)
			return s;

		// 3) Try all sides
		for (Direction d : Direction.values()) {
			s = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
			if (s != null && s.canExtract() && s.extractEnergy(want, true) > 0)
				return s;
		}

		return null;
	}

	/**
	 * Call from your block ticker on the server.
	 *
	 * Order matters:
	 * 1) PULL from producers (reactors/generators)
	 * 2) EQUALIZE with adjacent cables (moves energy along the line)
	 * 3) PUSH into consumers (machines/cubes)
	 */
public void serverTick() {
    Level lvl = level;
    if (lvl == null || lvl.isClientSide) return;
    if (!(lvl instanceof ILevelExtension ext)) return;

    // ----------------------------
    // 1) PULL: only from likely producers (energy rising since LAST tick)
    // ----------------------------
    int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
    if (space > 0) {
        for (Direction dir : Direction.values()) {
            if (space <= 0) break;

            BlockPos nPos = worldPosition.relative(dir);

            // Never pull from other cables (prevents loops)
            BlockEntity be = lvl.getBlockEntity(nPos);
            if (be instanceof BasicEnergyCableBlockEntity) continue;

            // Producer heuristic uses last tick's snapshot
            if (!isLikelyProducer(ext, lvl, nPos, dir)) continue;

            // Prefer sided for mods like Mekanism, fallback to null for your blocks
            IEnergyStorage src = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, dir.getOpposite());
            if (src == null) src = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, null);
            if (src == null || !src.canExtract()) continue;

            int want = Math.min(MAX_IO_PER_TICK, space);
            int pulledSim = src.extractEnergy(want, true);
            if (pulledSim <= 0) continue;

            int accepted = energyStorage.receiveEnergy(pulledSim, false);
            if (accepted > 0) {
                src.extractEnergy(accepted, false);
                space -= accepted;
            }
        }
    }

    // ----------------------------
    // 2) EQUALIZE: cable <-> cable
    // ----------------------------
    for (Direction dir : Direction.values()) {
        int myStored = energyStorage.getEnergyStored();
        if (myStored <= 1) break;

        BlockPos nPos = worldPosition.relative(dir);
        BlockEntity be = lvl.getBlockEntity(nPos);
        if (!(be instanceof BasicEnergyCableBlockEntity otherCable)) continue;

        IEnergyStorage other = otherCable.getEnergyStorage();
        int otherStored = other.getEnergyStored();

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

    // ----------------------------
    // 3) PUSH: cable -> non-cable neighbors
    // ----------------------------
    if (energyStorage.getEnergyStored() > 0) {
        for (Direction dir : Direction.values()) {
            if (energyStorage.getEnergyStored() <= 0) break;

            BlockPos nPos = worldPosition.relative(dir);

            // Don't push into other cables here (handled by equalize)
            BlockEntity nBe = lvl.getBlockEntity(nPos);
            if (nBe instanceof BasicEnergyCableBlockEntity) continue;

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
    // 4) SNAPSHOT neighbors for NEXT tick (must be last!)
    // ----------------------------
    for (Direction dir : Direction.values()) {
        BlockPos nPos = worldPosition.relative(dir);

        IEnergyStorage nNull = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, null);
        IEnergyStorage nSide = ext.getCapability(Capabilities.EnergyStorage.BLOCK, nPos, dir.getOpposite());
        IEnergyStorage n = (nSide != null ? nSide : nNull);

        lastNeighborEnergy[dirIndex(dir)] = (n == null) ? 0 : n.getEnergyStored();
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
