package net.crystalnexus.gametest;

import net.crystalnexus.block.GasolineGeneratorControllerBlock;
import net.crystalnexus.block.GasolineGeneratorDriveshaftBlock;
import net.crystalnexus.block.entity.GasolineGeneratorControllerBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class GasolineGeneratorGameTests {
    @GameTest(template = "zero_point")
    public static void generatorScalesFromThreeToSixteenShafts(GameTestHelper helper) {
        GasolineGeneratorControllerBlockEntity minimum = build(helper, new BlockPos(5, 5, 5), 3, 0, 0);
        helper.assertTrue(minimum.validateStructureNow(), "The 3-shaft diesel_generator.nbt layout must form");
        helper.assertTrue(minimum.getShaftCount() == 3, "Minimum generator must count three driveshafts");
        minimum.getFuelTank().fill(new FluidStack(CrystalnexusModFluids.GASOLINE.get(), 750), IFluidHandler.FluidAction.EXECUTE);
        minimum.serverTick();
        helper.assertTrue(minimum.getOutputPerTick() == GasolineGeneratorControllerBlockEntity.GASOLINE_FE_PER_TICK_PER_SHAFT * 3, "Gasoline output must scale by driveshaft count");
        for (int tick = 1; tick < GasolineGeneratorControllerBlockEntity.FUEL_CYCLE_TICKS; tick++) minimum.serverTick();
        helper.assertTrue(minimum.getFuelTank().getFluidAmount() == 0, "A three-shaft cycle must consume 750 mB");

        GasolineGeneratorControllerBlockEntity ported = build(helper, new BlockPos(20, 5, 5), 16, 1, 1);
        helper.assertTrue(ported.validateStructureNow(), "A 16-shaft generator must form with optional ports");
        MachineFluidInputBlockEntity input = helper.getBlockEntity(new BlockPos(20, 4, 4));
        MachineEnergyOutputBlockEntity output = helper.getBlockEntity(new BlockPos(20, 4, 5));
        helper.assertTrue(input.isBoundTo(helper.absolutePos(new BlockPos(20, 5, 5))) && output.isBoundTo(helper.absolutePos(new BlockPos(20, 5, 5))), "Configured ports must bind");
        ported.getFuelTank().fill(new FluidStack(CrystalnexusModFluids.OVERFUEL.get(), 4_000), IFluidHandler.FluidAction.EXECUTE);
        ported.serverTick();
        helper.assertTrue(ported.getOutputPerTick() == GasolineGeneratorControllerBlockEntity.GASOLINE_FE_PER_TICK_PER_SHAFT * 16 * 4, "Overfuel must generate exactly four times gasoline FE");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void rejectsInvalidLengthsAndPortCounts(GameTestHelper helper) {
        GasolineGeneratorControllerBlockEntity shortGenerator = build(helper, new BlockPos(5, 5, 5), 2, 1, 1);
        helper.assertTrue(!shortGenerator.validateStructureNow() && shortGenerator.getStatus().contains("3-16"), "Two driveshafts must be rejected");
        GasolineGeneratorControllerBlockEntity longGenerator = build(helper, new BlockPos(40, 5, 5), 17, 0, 0);
        helper.assertTrue(!longGenerator.validateStructureNow() && longGenerator.getStatus().contains("3-16"), "Seventeen driveshafts must be rejected");
        GasolineGeneratorControllerBlockEntity ports = build(helper, new BlockPos(20, 5, 5), 3, 5, 1);
        helper.assertTrue(!ports.validateStructureNow() && ports.getStatus().contains("at most 4 machine fluid"), "Five fluid inputs must be rejected");
        helper.succeed();
    }

    private static GasolineGeneratorControllerBlockEntity build(GameTestHelper helper, BlockPos controllerPos, int shafts, int fluidPorts, int energyPorts) {
        Direction front = Direction.NORTH;
        Direction rear = Direction.EAST;
        int fluidLeft = fluidPorts;
        int energyLeft = energyPorts;
        for (int depth = 0; depth <= shafts + 1; depth++) {
            BlockPos center = controllerPos.relative(rear, depth);
            for (int y = -1; y <= 1; y++) for (int side = -1; side <= 1; side++) {
                BlockPos pos = center.offset(rear.getClockWise().getStepX() * side, y, rear.getClockWise().getStepZ() * side);
                boolean isCenter = y == 0 && side == 0;
                if (depth == 0 && isCenter) helper.setBlock(pos, CrystalnexusModBlocks.DIESEL_GENERATOR_CONTROLLER.get().defaultBlockState().setValue(GasolineGeneratorControllerBlock.FACING, front));
                else if (isCenter && depth <= shafts) helper.setBlock(pos, CrystalnexusModBlocks.DIESEL_GENERATOR_DRIVESHAFT.get().defaultBlockState().setValue(GasolineGeneratorDriveshaftBlock.FACING, front));
                else if (fluidLeft-- > 0) helper.setBlock(pos, CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
                else if (energyLeft-- > 0) helper.setBlock(pos, CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
                else helper.setBlock(pos, CrystalnexusModBlocks.INSULATED_TITANIUM_CASING.get());
            }
        }
        return helper.getBlockEntity(controllerPos);
    }
}
