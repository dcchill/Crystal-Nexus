package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;

import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;

public class PipeStraightOnTickUpdateProcedure {

    private static final int MAX_TRANSFER = 100;
    private static final int PARTICLE_CHANCE = 20;

    public static void execute(LevelAccessor world, BlockPos pos, BlockState state) {
        if (world == null || pos == null || state == null) return;

        // Reschedule tick
        world.scheduleTick(pos, state.getBlock(), 1);

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof PipeStraightBlockEntity pipeBE)) return;
        IFluidHandler internalTank = pipeBE.getFluidTank();
        if (internalTank == null) return;

        Direction facing = state.getValue(PipeStraightBlock.FACING);
        Direction inputDir = facing.getOpposite(); // pull from opposite of facing
        Direction outputDir = facing; // push toward facing

        BlockPos inputPos = pos.relative(inputDir);
        BlockPos outputPos = pos.relative(outputDir);

        // Pull from input if valid
        if (isValidFluidSource(world, inputPos, inputDir)) {
            handlePull(world, inputPos, inputDir.getOpposite(), internalTank);
        }

        // Push to output if tank has fluid
        handlePush(world, outputPos, outputDir.getOpposite(), internalTank);

        // Drip particles
        if (world instanceof ServerLevel server && server.getRandom().nextInt(PARTICLE_CHANCE) == 0) {
            FluidStack fluidSim = internalTank.drain(1, IFluidHandler.FluidAction.SIMULATE);
            if (fluidSim != null && !fluidSim.isEmpty()) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.35;
                double z = pos.getZ() + 0.5;
                server.sendParticles(ParticleTypes.DRIPPING_WATER, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    // Pull only if the neighbor is a valid fluid source
    private static boolean isValidFluidSource(LevelAccessor world, BlockPos pos, Direction side) {
        BlockState neighborState = world.getBlockState(pos);

        // Disallow pulling from another straight pipe’s side
        if (neighborState.getBlock() instanceof PipeStraightBlock) {
            Direction neighborFacing = neighborState.getValue(PipeStraightBlock.FACING);
            // Only allow pull if the neighbor’s output faces this pipe
            return neighborFacing == side.getOpposite();
        }

        // Everything else is allowed (tanks, fluid blocks, etc.)
        return true;
    }

    private static void handlePull(LevelAccessor world, BlockPos neighborPos, Direction side, IFluidHandler tank) {
        IFluidHandler neighbor = BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK,
                (world instanceof ServerLevel s) ? s : null,
                neighborPos, side
        ).getCapability();

        if (neighbor != null) {
            FluidStack pulled = neighbor.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
            if (!pulled.isEmpty()) {
                int accepted = tank.fill(pulled, IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    int drained = neighbor.drain(accepted, IFluidHandler.FluidAction.EXECUTE).getAmount();
                    tank.fill(new FluidStack(pulled.getFluid(), drained), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    private static void handlePush(LevelAccessor world, BlockPos neighborPos, Direction side, IFluidHandler tank) {
        IFluidHandler neighbor = BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK,
                (world instanceof ServerLevel s) ? s : null,
                neighborPos, side
        ).getCapability();

        if (neighbor != null) {
            FluidStack toPush = tank.drain(MAX_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
            if (!toPush.isEmpty()) {
                int accepted = neighbor.fill(toPush, IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    int drained = tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE).getAmount();
                    neighbor.fill(new FluidStack(toPush.getFluid(), drained), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }
}
