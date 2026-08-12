package net.crystalnexus.fluid;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluidTypes;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;

public abstract class TemporalEssenceFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> CrystalnexusModFluidTypes.TEMPORAL_ESSENCE_TYPE.get(),
			() -> CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), () -> CrystalnexusModFluids.FLOWING_TEMPORAL_ESSENCE.get()).explosionResistance(100f).bucket(() -> CrystalnexusModItems.TEMPORAL_ESSENCE_BUCKET.get())
			.block(() -> (LiquidBlock) CrystalnexusModBlocks.TEMPORAL_ESSENCE.get());

	private TemporalEssenceFluid() {
		super(PROPERTIES);
	}

	@Override
	public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
		Vec3 flow = super.getFlow(level, pos, state);
		return state.getValue(FALLING) ? new Vec3(flow.x, Math.abs(flow.y), flow.z) : flow;
	}

	@Override
	protected void spread(Level level, BlockPos pos, FluidState state) {
		if (state.isEmpty())
			return;

		BlockState currentBlock = level.getBlockState(pos);
		BlockPos above = pos.above();
		BlockState aboveBlock = level.getBlockState(above);
		FluidState upwardState = getNewLiquid(level, above, aboveBlock);
		if (!upwardState.isEmpty() && canSpreadTo(level, pos, currentBlock, Direction.UP, above, aboveBlock, level.getFluidState(above), upwardState.getType())) {
			spreadTo(level, above, aboveBlock, Direction.UP, upwardState);
			if (sourceNeighborCount(level, pos) >= 3)
				spreadToSides(level, pos, state, currentBlock);
		} else {
			spreadToSides(level, pos, state, currentBlock);
		}
	}

	private void spreadToSides(Level level, BlockPos pos, FluidState state, BlockState currentBlock) {
		int amount = state.getAmount() - getDropOff(level);
		if (amount <= 0)
			return;

		for (Map.Entry<Direction, FluidState> entry : getSpread(level, pos, currentBlock).entrySet()) {
			BlockPos target = pos.relative(entry.getKey());
			BlockState targetBlock = level.getBlockState(target);
			if (canSpreadTo(level, pos, currentBlock, entry.getKey(), target, targetBlock, level.getFluidState(target), entry.getValue().getType()))
				spreadTo(level, target, targetBlock, entry.getKey(), entry.getValue());
		}
	}

	@Override
	protected Map<Direction, FluidState> getSpread(Level level, BlockPos pos, BlockState currentBlock) {
		Map<Direction, FluidState> spread = new EnumMap<>(Direction.class);
		int shortest = Integer.MAX_VALUE;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos target = pos.relative(direction);
			BlockState targetBlock = level.getBlockState(target);
			FluidState newState = getNewLiquid(level, target, targetBlock);
			if (newState.isEmpty() || !canSpreadTo(level, pos, currentBlock, direction, target, targetBlock, level.getFluidState(target), newState.getType()))
				continue;

			int distance = hasUpwardOpening(level, target, targetBlock) ? 0 : getInvertedSlopeDistance(level, target, targetBlock, direction.getOpposite(), 1);
			if (distance < shortest)
				spread.clear();
			if (distance <= shortest) {
				spread.put(direction, newState);
				shortest = distance;
			}
		}
		return spread;
	}

	private int getInvertedSlopeDistance(LevelReader level, BlockPos pos, BlockState currentBlock, Direction cameFrom, int distance) {
		int shortest = Integer.MAX_VALUE;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (direction == cameFrom)
				continue;
			BlockPos target = pos.relative(direction);
			BlockState targetBlock = level.getBlockState(target);
			FluidState targetFluid = level.getFluidState(target);
			if (!canSpreadTo(level, pos, currentBlock, direction, target, targetBlock, targetFluid, getFlowing()))
				continue;
			if (hasUpwardOpening(level, target, targetBlock))
				return distance;
			if (distance < getSlopeFindDistance(level))
				shortest = Math.min(shortest, getInvertedSlopeDistance(level, target, targetBlock, direction.getOpposite(), distance + 1));
		}
		return shortest;
	}

	private boolean hasUpwardOpening(BlockGetter level, BlockPos pos, BlockState currentBlock) {
		BlockPos above = pos.above();
		BlockState aboveBlock = level.getBlockState(above);
		return canSpreadTo(level, pos, currentBlock, Direction.UP, above, aboveBlock, level.getFluidState(above), getFlowing());
	}

	@Override
	protected FluidState getNewLiquid(Level level, BlockPos pos, BlockState blockState) {
		int highestAmount = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos neighborPos = pos.relative(direction);
			FluidState neighbor = level.getFluidState(neighborPos);
			if (neighbor.getType().isSame(this))
				highestAmount = Math.max(highestAmount, neighbor.getAmount());
		}

		BlockPos below = pos.below();
		BlockState belowBlock = level.getBlockState(below);
		FluidState belowFluid = belowBlock.getFluidState();
		if (!belowFluid.isEmpty() && belowFluid.getType().isSame(this)) {
			int amount = belowFluid.getAmount() - getDropOff(level);
			return amount <= 0 ? net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState() : getFlowing(amount, true);
		}

		int amount = highestAmount - getDropOff(level);
		return amount <= 0 ? net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState() : getFlowing(amount, false);
	}

	private int sourceNeighborCount(LevelReader level, BlockPos pos) {
		int count = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL)
			if (level.getFluidState(pos.relative(direction)).isSourceOfType(this))
				count++;
		return count;
	}

	@Override
	protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
		return direction == Direction.UP && !isSame(fluid);
	}

	public static class Source extends TemporalEssenceFluid {
		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends TemporalEssenceFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}
}
