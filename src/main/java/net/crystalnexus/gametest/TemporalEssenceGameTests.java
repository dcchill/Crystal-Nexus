package net.crystalnexus.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class TemporalEssenceGameTests {
	private TemporalEssenceGameTests() {
	}

	@GameTest(template = "zero_point", timeoutTicks = 70)
	public static void flowsUpInsteadOfDown(GameTestHelper helper) {
		BlockPos source = new BlockPos(1, 3, 1);
		helper.setBlock(source.below(), Blocks.AIR);
		for (int distance = 1; distance <= 8; distance++)
			helper.setBlock(source.above(distance), Blocks.AIR);
		helper.setBlock(source, CrystalnexusModBlocks.TEMPORAL_ESSENCE.get());
		helper.assertTrue(helper.getBlockState(source).getFluidState().is(CrystalnexusModFluids.TEMPORAL_ESSENCE.get()),
				"Temporal essence source block must expose its registered fluid state");
		helper.getLevel().scheduleTick(helper.absolutePos(source), CrystalnexusModFluids.TEMPORAL_ESSENCE.get(), 1);

		helper.runAfterDelay(50, () -> {
			var above = helper.getBlockState(source.above());
			helper.assertTrue(above.getFluidState().getType().isSame(CrystalnexusModFluids.TEMPORAL_ESSENCE.get()),
					"Temporal essence must flow upward; found " + above);
			helper.assertTrue(helper.getBlockState(source.below()).getFluidState().isEmpty(), "Temporal essence must not flow downward");
			helper.assertTrue(helper.getBlockState(source.above(7)).getFluidState().getType().isSame(CrystalnexusModFluids.TEMPORAL_ESSENCE.get()),
					"Temporal essence must reach its seven-block range");
			helper.assertTrue(helper.getBlockState(source.above(8)).getFluidState().isEmpty(), "Temporal essence must stop after seven blocks");
			helper.succeed();
		});
	}
}
