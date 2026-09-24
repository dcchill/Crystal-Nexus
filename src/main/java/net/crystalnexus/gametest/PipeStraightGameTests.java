package net.crystalnexus.gametest;

import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.block.entity.PipeJunctionBlockEntity;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class PipeStraightGameTests {
    private PipeStraightGameTests() {}

    @GameTest(template = "zero_point")
    public static void cyclesDefaultInputOutputModes(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 4, 1);
        helper.setBlock(pipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        PipeStraightBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(pipe.cycleSideMode(Direction.EAST) == 1 && pipe.isInputSide(Direction.EAST),
            "The first wrench cycle must configure the clicked side as input");
        helper.assertTrue(pipe.cycleSideMode(Direction.EAST) == 2 && pipe.isOutputSide(Direction.EAST),
            "The second wrench cycle must configure the clicked side as output");
        helper.assertTrue(pipe.cycleSideMode(Direction.EAST) == 0
                && !pipe.isInputSide(Direction.EAST) && !pipe.isOutputSide(Direction.EAST),
            "The third wrench cycle must restore automatic/default behavior");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void selectsConnectionArmInsteadOfTouchedSurface(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 4, 1);
        helper.setBlock(pipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(pipePos.east(), CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        Direction selected = PipeStraightBlock.connectionAt(helper.getBlockState(pipePos), pipePos,
            new Vec3(pipePos.getX() + 0.85, pipePos.getY() + 0.69, pipePos.getZ() + 0.5), Direction.UP);
        helper.assertTrue(selected == Direction.EAST,
            "Clicking the top surface of an east pipe arm must configure the east connection, not the up face");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void standardPipeRoutesPointToPointWithoutStorage(GameTestHelper helper) {
        assertPointToPoint(helper, CrystalnexusModBlocks.PIPE_STRAIGHT.get(), 2_000);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void copperPipeRoutesPointToPointAtCopperRate(GameTestHelper helper) {
        assertPointToPoint(helper, CrystalnexusModBlocks.COPPER_FLUID_PIPE.get(),
            PipeStraightBlockEntity.COPPER_MAX_TRANSFER);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void fillsClosestEndpointFirst(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 4, 1);
        BlockPos sourcePos = pipePos.west();
        BlockPos nearSinkPos = pipePos.north();
        BlockPos secondPipePos = pipePos.east();
        BlockPos farSinkPos = secondPipePos.east();
        helper.setBlock(sourcePos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        helper.setBlock(pipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(nearSinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        helper.setBlock(secondPipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(farSinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        PipeJunctionBlockEntity source = helper.getBlockEntity(sourcePos);
        PipeJunctionBlockEntity nearSink = helper.getBlockEntity(nearSinkPos);
        PipeJunctionBlockEntity farSink = helper.getBlockEntity(farSinkPos);
        source.getFluidTank().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);

        ((PipeStraightBlockEntity) helper.getBlockEntity(pipePos)).serverTick();
        helper.assertTrue(source.getFluidTank().getFluidAmount() == 500
                && nearSink.getFluidTank().isEmpty(),
            "A point-to-point link must not deliver fluid on the routing tick");
        tick((PipeStraightBlockEntity) helper.getBlockEntity(pipePos), 2);

        helper.assertTrue(source.getFluidTank().isEmpty()
                && nearSink.getFluidTank().getFluidAmount() == 500
                && farSink.getFluidTank().isEmpty(),
            "A pipe network must satisfy the closest endpoint before a farther endpoint");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void keepsTheCompletedRouteVisibleWhileSourceHasFluid(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 4, 1);
        BlockPos firstPipePos = sourcePos.east();
        BlockPos secondPipePos = firstPipePos.east();
        BlockPos sinkPos = secondPipePos.east();
        helper.setBlock(sourcePos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        helper.setBlock(firstPipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(secondPipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        PipeJunctionBlockEntity source = helper.getBlockEntity(sourcePos);
        PipeStraightBlockEntity first = helper.getBlockEntity(firstPipePos);
        PipeStraightBlockEntity second = helper.getBlockEntity(secondPipePos);
        source.getFluidTank().fill(new FluidStack(Fluids.WATER, 6_000), IFluidHandler.FluidAction.EXECUTE);

        first.serverTick();
        for (int tick = 0; tick < PipeStraightBlockEntity.TRANSFER_TICKS_PER_PIPE * 2; tick++) {
            first.serverTick();
            second.serverTick();
        }
        first.serverTick();
        second.serverTick();

        helper.assertTrue(first.getDisplayFluid().is(Fluids.WATER)
                && second.getDisplayFluid().is(Fluids.WATER)
                && source.getFluidTank().getFluidAmount() > 0,
            "Every pipe on a completed route must stay visibly filled while its source still has fluid");
        helper.succeed();
    }

    private static void assertPointToPoint(GameTestHelper helper, Block pipeBlock, int expectedMoved) {
        BlockPos sourcePos = new BlockPos(0, 4, 1);
        BlockPos pipePos = sourcePos.east();
        BlockPos sinkPos = pipePos.east();
        helper.setBlock(sourcePos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        helper.setBlock(pipePos, pipeBlock);
        helper.setBlock(sinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        PipeJunctionBlockEntity source = helper.getBlockEntity(sourcePos);
        PipeJunctionBlockEntity sink = helper.getBlockEntity(sinkPos);
        PipeStraightBlockEntity pipe = helper.getBlockEntity(pipePos);
        source.getFluidTank().fill(new FluidStack(Fluids.WATER, 2_000), IFluidHandler.FluidAction.EXECUTE);

        pipe.serverTick();
        helper.assertTrue(source.getFluidTank().getFluidAmount() == 2_000 && sink.getFluidTank().isEmpty(),
            "A point-to-point link must take time to cross the pipe");
        tick(pipe, PipeStraightBlockEntity.TRANSFER_TICKS_PER_PIPE);
        IFluidHandler pipeHandler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
            helper.absolutePos(pipePos), Direction.WEST);

        helper.assertTrue(source.getFluidTank().getFluidAmount() == 2_000 - expectedMoved
                && sink.getFluidTank().getFluidAmount() == expectedMoved,
            "The pipe must transfer directly between endpoints at its tier rate");
        helper.assertTrue(pipeHandler != null && pipeHandler.getFluidInTank(0).isEmpty(),
            "A pipe capability must be a link, not a fluid buffer");
    }

    private static void tick(PipeStraightBlockEntity pipe, int ticks) {
        for (int i = 0; i < ticks; i++) pipe.serverTick();
    }
}
