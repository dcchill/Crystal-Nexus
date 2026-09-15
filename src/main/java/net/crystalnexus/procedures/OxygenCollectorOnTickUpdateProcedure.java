package net.crystalnexus.procedures;

import net.crystalnexus.block.entity.OxygenCollectorBlockEntity;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class OxygenCollectorOnTickUpdateProcedure {
	private static final int HORIZONTAL_RADIUS = 5;
	private static final int VERTICAL_RADIUS = 5;
	private static final int ENERGY_PER_BATCH = 256;
	private static final int ATMOSPHERE_PER_BATCH = 10;
	private static final int BASE_TICKS_PER_BATCH = 40;

	public static void execute(ServerLevel level, BlockPos collectorPos) {
		if (!(level.getBlockEntity(collectorPos) instanceof OxygenCollectorBlockEntity collector)) return;
		List<BlockPos> leaves = findLeaves(level, collectorPos);
		int ticksPerBatch = Math.max(1, BASE_TICKS_PER_BATCH / Math.max(1, leaves.size()));
		boolean canCollect = !leaves.isEmpty() && collector.getEnergyStorage().getEnergyStored() >= ENERGY_PER_BATCH
			&& collector.getFluidTank().fill(new FluidStack(CrystalnexusModFluids.ATMOSPHERE.get(), ATMOSPHERE_PER_BATCH), IFluidHandler.FluidAction.SIMULATE) == ATMOSPHERE_PER_BATCH;
		double progress = collector.getPersistentData().getDouble("progress");
		collector.getPersistentData().putDouble("maxProgress", ticksPerBatch);
		if (!canCollect) { if (progress != 0) collector.getPersistentData().putDouble("progress", 0); return; }
		progress++;
		collector.getPersistentData().putDouble("progress", progress);
		if (level.getGameTime() % 10 == 0) spawnLeafParticles(level, leaves, collectorPos, progress / ticksPerBatch);
		if (progress >= ticksPerBatch) {
			collector.getFluidTank().fill(new FluidStack(CrystalnexusModFluids.ATMOSPHERE.get(), ATMOSPHERE_PER_BATCH), IFluidHandler.FluidAction.EXECUTE);
			collector.getEnergyStorage().extractEnergy(ENERGY_PER_BATCH, false);
			collector.getPersistentData().putDouble("progress", 0);
		}
		collector.setChanged();
		level.sendBlockUpdated(collectorPos, level.getBlockState(collectorPos), level.getBlockState(collectorPos), 2);
	}

	private static List<BlockPos> findLeaves(Level level, BlockPos origin) {
		List<BlockPos> leaves = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS), origin.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS)))
			if (level.getBlockState(pos).is(BlockTags.LEAVES)) leaves.add(pos.immutable());
		return leaves;
	}
	private static void spawnLeafParticles(ServerLevel level, List<BlockPos> leaves, BlockPos collector, double progress) {
		for (BlockPos leaf : leaves) {
			double x = leaf.getX() + 0.5 + (collector.getX() + 0.5 - (leaf.getX() + 0.5)) * progress;
			double y = leaf.getY() + 0.5 + (collector.getY() + 0.7 - (leaf.getY() + 0.5)) * progress;
			double z = leaf.getZ() + 0.5 + (collector.getZ() + 0.5 - (leaf.getZ() + 0.5)) * progress;
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0, 0, 0);
		}
	}
}
