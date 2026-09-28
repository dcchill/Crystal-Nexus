package net.crystalnexus.multiblock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;


public interface MultiblockPortTarget {
	
	default boolean acceptsMultiblockPort(BlockPos pos) { return true; }
	@Nullable default IFluidHandler multiblockFluidInput() { return null; }
	@Nullable default IFluidHandler multiblockFluidOutput() { return null; }
	@Nullable default IEnergyStorage multiblockEnergyInput() { return null; }
	@Nullable default IEnergyStorage multiblockEnergyOutput() { return null; }
}
