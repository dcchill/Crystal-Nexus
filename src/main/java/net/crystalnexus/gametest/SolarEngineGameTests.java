package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.block.entity.SolarEngineControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class SolarEngineGameTests {
	private SolarEngineGameTests() {}

	@GameTest(template = "solar_engine")
	public static void validatesPortsCoolingExtractionAndFailure(GameTestHelper helper) {
		BlockPos controllerPos = find(helper, CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get());
		List<BlockPos> tungsten = findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get());
		helper.assertTrue(tungsten.size() >= 2, "Solar Engine template must contain replaceable Tungsten casing blocks");

		BlockState controllerState = helper.getBlockState(controllerPos);
		helper.setBlock(controllerPos, Blocks.AIR);
		helper.setBlock(controllerPos, controllerState);
		helper.setBlock(tungsten.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
		helper.setBlock(tungsten.get(1), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());

		SolarEngineControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
		MachineEnergyOutputBlockEntity output = helper.getBlockEntity(tungsten.get(0));
		MachineFluidInputBlockEntity input = helper.getBlockEntity(tungsten.get(1));
		input.getFluidInput().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(controller.validateStructureNow(), "Solar Engine must form with one energy output and one water input replacing Tungsten");
		helper.assertTrue(output.isBoundTo(helper.absolutePos(controllerPos)), "Energy output must bind to the formed controller");
		helper.assertTrue(input.isBoundTo(helper.absolutePos(controllerPos)), "Fluid input must bind to the formed controller");

		controller.setItem(0, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		controller.setExtractionPercent(100);
		controller.serverTick();
		helper.assertTrue(controller.getOutputPerTick() == 250_000 && output.getEnergyStorage().getEnergyStored() == 250_000,
			"Yellow Dwarf at full extraction must produce 250,000 FE/t through the output port");
		helper.assertTrue(controller.getCoolantTank().getFluidAmount() == 992 && controller.getHeat() == 0,
			"Available water must transfer in, be consumed, and remove generated heat");

		output.getEnergyStorage().extractEnergy(output.getEnergyStorage().getEnergyStored(), false);
		controller.setExtractionPercent(50);
		controller.serverTick();
		helper.assertTrue(controller.getOutputPerTick() == 125_000,
			"Extraction control must proportionally change FE/t");
		helper.succeed();
	}

	@GameTest(template = "solar_engine", timeoutTicks = 400)
	public static void coolantLossDestroysCasingAndConsumesStar(GameTestHelper helper) {
		BlockPos controllerPos = find(helper, CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get());
		List<BlockPos> tungsten = findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get());
		int originalTungsten = tungsten.size();
		BlockState controllerState = helper.getBlockState(controllerPos);
		helper.setBlock(controllerPos, Blocks.AIR);
		helper.setBlock(controllerPos, controllerState);
		helper.setBlock(tungsten.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
		helper.setBlock(tungsten.get(1), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
		SolarEngineControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
		helper.assertTrue(controller.validateStructureNow(), "Solar Engine must form before failure testing");
		controller.setItem(0, new ItemStack(CrystalnexusModItems.PINK_STAR.get()));
		controller.setExtractionPercent(100);
		for (int tick = 0; tick < 500 && !controller.getItem(0).isEmpty(); tick++) controller.serverTick();
		helper.assertTrue(controller.getItem(0).isEmpty(), "Containment failure must consume the installed artificial star");
		helper.assertTrue(findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get()).size() < originalTungsten - 2,
			"Containment failure must destroy Solar Engine casing components");
		helper.succeed();
	}

	@GameTest(template = "solar_engine")
	public static void runningStructureBreakCausesContainmentCollapse(GameTestHelper helper) {
		BlockPos controllerPos = find(helper, CrystalnexusModBlocks.SOLAR_ENGINE_CONTROLLER.get());
		List<BlockPos> tungsten = findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get());
		helper.assertTrue(tungsten.size() >= 3, "Solar Engine template must expose casing for ports and a breach");
		BlockState controllerState = helper.getBlockState(controllerPos);
		helper.setBlock(controllerPos, Blocks.AIR);
		helper.setBlock(controllerPos, controllerState);
		helper.setBlock(tungsten.get(0), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
		helper.setBlock(tungsten.get(1), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
		SolarEngineControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
		MachineFluidInputBlockEntity input = helper.getBlockEntity(tungsten.get(1));
		input.getFluidInput().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(controller.validateStructureNow(), "Solar Engine must form before its casing is breached");
		controller.setItem(0, new ItemStack(CrystalnexusModItems.YELLOW_DWARF_STAR.get()));
		controller.setExtractionPercent(100);
		controller.serverTick();
		helper.assertTrue(controller.isOperating(), "Solar Engine must be running when its casing is breached");

		int casingBeforeBreach = findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get()).size();
		helper.setBlock(tungsten.get(2), Blocks.AIR);
		helper.assertTrue(!controller.validateStructureNow(), "A breached Solar Engine must collapse");
		helper.assertTrue(controller.getItem(0).isEmpty(), "The stellar collapse must consume the artificial star");
		helper.assertTrue(findAll(helper, CrystalnexusModBlocks.TUNGSTEN_BLOCK.get()).size() < casingBeforeBreach - 1,
			"The stellar collapse must destroy additional tungsten casing");
		helper.succeed();
	}

	private static BlockPos find(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
		return findAll(helper, block).stream().findFirst().orElseThrow(() -> new IllegalStateException("Template is missing " + block));
	}

	private static List<BlockPos> findAll(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
		List<BlockPos> found = new ArrayList<>();
		for (int y = 1; y <= 32; y++) for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (helper.getBlockState(pos).is(block)) found.add(pos);
		}
		return found;
	}
}
