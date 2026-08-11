package net.crystalnexus.gametest;

import net.crystalnexus.block.PipeStraightBlock;
import net.crystalnexus.block.entity.PipeJunctionBlockEntity;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class PipeStraightGameTests {
    private PipeStraightGameTests() {
    }

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
    public static void connectsAndBalancesFluidBetweenPipes(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 4, 1);
        BlockPos secondPos = firstPos.east();
        helper.setBlock(firstPos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(secondPos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        PipeStraightBlockEntity first = helper.getBlockEntity(firstPos);
        PipeStraightBlockEntity second = helper.getBlockEntity(secondPos);
        first.getFluidTank().fill(new FluidStack(Fluids.WATER, 800), IFluidHandler.FluidAction.EXECUTE);

        first.serverTick();
        second.serverTick();

        helper.assertTrue(helper.getBlockState(firstPos).getValue(PipeStraightBlock.EAST)
                && helper.getBlockState(secondPos).getValue(PipeStraightBlock.WEST),
            "Adjacent fluid pipes must expose connected cable-style arms");
        helper.assertTrue(second.getFluidTank().getFluidAmount() > 0
                && second.getFluidTank().getFluid().is(Fluids.WATER),
            "Adjacent pipes must balance and transport their contained fluid");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void drainsTheLastFluidUnitFromThePipeNetwork(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 4, 1);
        BlockPos secondPos = firstPos.east();
        BlockPos sinkPos = secondPos.east();
        helper.setBlock(firstPos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(secondPos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        PipeStraightBlockEntity first = helper.getBlockEntity(firstPos);
        PipeStraightBlockEntity second = helper.getBlockEntity(secondPos);
        PipeJunctionBlockEntity sink = helper.getBlockEntity(sinkPos);
        first.getFluidTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
        second.cycleSideMode(Direction.EAST);
        second.cycleSideMode(Direction.EAST);

        second.serverTick();

        helper.assertTrue(first.getFluidTank().isEmpty()
                && second.getFluidTank().isEmpty()
                && sink.getFluidTank().getFluidAmount() == 1,
            "A configured output must drain the final fluid unit from its connected pipe network");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void movesFluidBetweenExternalHandlers(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 4, 1);
        BlockPos pipePos = sourcePos.east();
        BlockPos sinkPos = pipePos.east();
        helper.setBlock(sourcePos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        helper.setBlock(pipePos, CrystalnexusModBlocks.PIPE_STRAIGHT.get());
        helper.setBlock(sinkPos, CrystalnexusModBlocks.PIPE_JUNCTION.get());
        PipeJunctionBlockEntity source = helper.getBlockEntity(sourcePos);
        PipeJunctionBlockEntity sink = helper.getBlockEntity(sinkPos);
        PipeStraightBlockEntity pipe = helper.getBlockEntity(pipePos);
        source.getFluidTank().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);

        for (int tick = 0; tick < 4; tick++) pipe.serverTick();

        helper.assertTrue(source.getFluidTank().getFluidAmount() == 100
                && sink.getFluidTank().getFluidAmount() == 400
                && sink.getFluidTank().getFluid().is(Fluids.WATER),
            "A pipe must pull from one connected fluid handler and push into another");
        helper.succeed();
    }
}
