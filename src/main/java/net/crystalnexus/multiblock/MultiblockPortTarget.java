package net.crystalnexus.multiblock;

import javax.annotation.Nullable;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** The controller-owned stores exposed by bufferless multiblock ports. */
public interface MultiblockPortTarget {
	@Nullable default IFluidHandler multiblockFluidInput() { return null; }
	@Nullable default IFluidHandler multiblockFluidOutput() { return null; }
	@Nullable default IEnergyStorage multiblockEnergyInput() { return null; }
	@Nullable default IEnergyStorage multiblockEnergyOutput() { return null; }
}
