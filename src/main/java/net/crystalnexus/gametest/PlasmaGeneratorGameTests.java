package net.crystalnexus.gametest;

import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class PlasmaGeneratorGameTests {
    private PlasmaGeneratorGameTests() {}

    @GameTest(template = "plasma_generator")
    public static void consumesArgonAndGeneratesThroughReplaceablePorts(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.assertTrue(casing.size() >= 2,
            "The Plasma Generator template needs two replaceable Titanium Carbide Block positions");

        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        helper.assertTrue(!controller.validateStructureNow(), "The generator must require a fluid input port");

        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        MachineFluidInputBlockEntity input = helper.getBlockEntity(casing.get(0));
        input.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);

        helper.assertTrue(controller.validateStructureNow(),
            "A fluid input port must replace a Titanium Carbide Block anywhere in the structure");
        controller.serverTick();
        helper.assertTrue(input.isBoundTo(helper.absolutePos(controllerPos))
                && controller.getArgonTank().getFluidAmount() == PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY,
            "A formed generator must bind its fluid input and accept Argon before an energy output is installed");
        helper.assertTrue(controller.getStatus().equals("Energy Output Full"),
            "A generator without an energy output must report that output is unavailable");

        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
        MachineEnergyOutputBlockEntity output = helper.getBlockEntity(casing.get(1));
        helper.assertTrue(controller.validateStructureNow(),
            "An energy output port must be accepted at any Titanium Carbide Block position");
        helper.assertTrue(output.isBoundTo(helper.absolutePos(controllerPos)),
            "The energy output must bind to the formed Plasma Generator controller");
        controller.serverTick();

        helper.assertTrue(controller.isOperating() && controller.getStatus().equals("Generating"),
            "A formed generator with Argon and output capacity must report Generating");
        helper.assertTrue(controller.getArgonTank().getFluidAmount() == PlasmaGeneratorControllerBlockEntity.TANK_CAPACITY
                - PlasmaGeneratorControllerBlockEntity.ARGON_PER_TICK,
            "The internal Argon buffer must cap at 15 mB and consume one mB per generating tick");
        helper.assertTrue(controller.getOutputPerTick() == PlasmaGeneratorControllerBlockEntity.GENERATION_PER_TICK
                && output.getEnergyStorage().getEnergyStored() == PlasmaGeneratorControllerBlockEntity.GENERATION_PER_TICK,
            "The generator must emit its reported FE/t through the multiblock energy output");
        List<BlockPos> heatingCores = find(helper, CrystalnexusModBlocks.HEATING_CORE.get());
        helper.assertTrue(!heatingCores.isEmpty() && heatingCores.stream()
                .allMatch(pos -> helper.getBlockState(pos).getValue(HeatingCoreBlock.LIT)),
            "Every heating core must use its lit texture and emit light while the Plasma Generator is working");

        helper.setBlock(casing.get(1), CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.assertTrue(controller.validateStructureNow(),
            "Removing the optional energy output must leave the fluid-fed structure formed");
        controller.serverTick();
        helper.assertTrue(heatingCores.stream()
                .noneMatch(pos -> helper.getBlockState(pos).getValue(HeatingCoreBlock.LIT)),
            "Heating cores must turn off when the Plasma Generator stops working");
        helper.succeed();
    }

    @GameTest(template = "plasma_generator")
    public static void runningStructureBreakCausesPlasmaArcRupture(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        MachineFluidInputBlockEntity input = helper.getBlockEntity(casing.get(0));
        input.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(controller.validateStructureNow(), "Plasma Generator must form before a core is breached");
        controller.serverTick();
        helper.assertTrue(controller.isOperating(), "Plasma Generator must be running when a core is breached");

        List<BlockPos> heatingCores = find(helper, CrystalnexusModBlocks.HEATING_CORE.get());
        helper.assertTrue(!heatingCores.isEmpty(), "Plasma Generator template must contain a heating core to breach");
        int carbideBeforeBreach = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get()).size();
        helper.setBlock(heatingCores.getFirst(), Blocks.AIR);
        helper.assertTrue(!controller.validateStructureNow(), "A breached Plasma Generator must rupture");
        helper.assertTrue(controller.getArgonTank().isEmpty(), "The plasma arc rupture must vent all buffered Argon");
        helper.assertTrue(controller.getStatus().equals("Plasma Arc Failure"), "The controller must report its arc failure");
        helper.assertTrue(find(helper, CrystalnexusModBlocks.HEATING_CORE.get()).isEmpty(),
            "The plasma arc rupture must destroy every remaining heating core");
        helper.assertTrue(find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get()).size() == carbideBeforeBreach,
            "The plasma arc rupture must leave its carbide shell intact, unlike a stellar containment collapse");
        helper.succeed();
    }

    private static List<BlockPos> find(GameTestHelper helper, Block block) {
        List<BlockPos> found = new ArrayList<>();
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(block)) found.add(pos);
        }
        return found;
    }
}
