package net.crystalnexus.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class OreTankThreeProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) >= 20000 && getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null) < 32000) {
			return true;
		}
		return false;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}
}