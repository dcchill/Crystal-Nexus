package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class ParticleAcceleratorStatusTextProcedureProcedure {

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		BlockEntity be = world.getBlockEntity(pos);

		if (be == null) {
			return "No controller data";
		}

		double formed = be.getPersistentData().getDouble("formed");
		double reason = be.getPersistentData().getDouble("reason");
		double stalled = be.getPersistentData().getDouble("stalled");

		double magCount = be.getPersistentData().getDouble("magCount");
		double stallNeed = be.getPersistentData().getDouble("stallNeed");
		double stallStored = be.getPersistentData().getDouble("stallStored");

		// ---- Highest priority errors first ----

		if (formed == 0) {
			return "Structure incomplete";
		}

		if (reason == 2) {
			return "Not enough electromagnets";
		}

		if (stalled == 1) {
			// Show useful power diagnostics
			return "Insufficient magnet power (" +
				(int) stallStored + " / " +
				(int) stallNeed + " FE)";
		}

		// ---- Warnings / info ----
		if (magCount <= 0) {
			return "No electromagnets detected";
		}

		// ---- All good ----
		return "Accelerator online";
	}
}
