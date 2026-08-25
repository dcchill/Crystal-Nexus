package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.CryogenicFlashFreezerBlockEntity;
import net.crystalnexus.block.entity.MultiblockFluidOutputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class CryogenicFlashFreezerGameTests {
	private CryogenicFlashFreezerGameTests() {}

	@GameTest(template = "zero_point")
	public static void validatesSpacedPillarsAndFreezesOxygen(GameTestHelper helper) {
		BlockPos min = new BlockPos(1, 1, 1);
		BlockPos max = new BlockPos(5, 3, 5);
		for (int x = min.getX(); x <= max.getX(); x++) for (int y = min.getY(); y <= max.getY(); y++)
			for (int z = min.getZ(); z <= max.getZ(); z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		for (int x = min.getX(); x <= max.getX(); x++) for (int y = min.getY(); y <= max.getY(); y++)
			for (int z = min.getZ(); z <= max.getZ(); z++)
				if (x == min.getX() || x == max.getX() || y == min.getY() || y == max.getY()
						|| z == min.getZ() || z == max.getZ())
					helper.setBlock(new BlockPos(x, y, z), CrystalnexusModBlocks.INSULATED_TITANIUM_CASING.get());

		BlockPos controllerPos = new BlockPos(1, 2, 3);
		helper.setBlock(controllerPos, CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get());
		BlockPos outputPos = new BlockPos(5, 2, 3);
		helper.setBlock(outputPos, CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get());
		helper.setBlock(new BlockPos(3, 2, 3), CrystalnexusModBlocks.COOLING_COIL.get());
		CryogenicFlashFreezerBlockEntity freezer = helper.getBlockEntity(controllerPos);
		helper.assertTrue(freezer.validateStructureNow(), "A 5x3x5 shell with one spaced coil pillar must form");

		freezer.getTank(0).fill(new FluidStack(CrystalnexusModFluids.OXYGEN.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		for (int tick = 0; tick < 200; tick++) freezer.serverTick();
		helper.assertTrue(freezer.getTank(0).isEmpty(), "The freezer must consume 1000 mB Oxygen");
		freezer.serverTick();
		MultiblockFluidOutputBlockEntity output = helper.getBlockEntity(outputPos);
		FluidStack argon = output.getFluidOutput().drain(5, IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(argon.getAmount() == 5 && argon.is(CrystalnexusModFluids.ARGON.get()),
			"The freezer must send 5 mB Argon to its fluid output port");

		helper.setBlock(new BlockPos(2, 2, 3), CrystalnexusModBlocks.COOLING_COIL.get());
		helper.assertFalse(freezer.validateStructureNow(), "Adjacent cooling-coil pillars must be rejected");
		helper.succeed();
	}
}
