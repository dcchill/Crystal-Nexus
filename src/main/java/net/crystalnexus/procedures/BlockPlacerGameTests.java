package net.crystalnexus.procedures;

import net.crystalnexus.block.BlockPlacerBlock;
import net.crystalnexus.block.entity.BlockPlacerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class BlockPlacerGameTests {
	private BlockPlacerGameTests() {
	}

	@GameTest(template = "zero_point")
	public static void usesSlottedItemOnTargetBlock(GameTestHelper helper) {
		BlockPos placerPos = new BlockPos(1, 2, 1);
		BlockPos targetPos = placerPos.east();
		helper.setBlock(placerPos, CrystalnexusModBlocks.BLOCK_PLACER.get().defaultBlockState()
				.setValue(BlockPlacerBlock.FACING, net.minecraft.core.Direction.EAST));
		helper.setBlock(targetPos, Blocks.DIRT);

		BlockPlacerBlockEntity placer = helper.getBlockEntity(placerPos);
		placer.setItem(0, new ItemStack(Items.WOODEN_HOE));
		placer.getEnergyStorage().receiveEnergy(256, false);
		BlockPos absolutePos = helper.absolutePos(placerPos);
		BlockPlacerOnTickUpdateProcedure.execute(helper.getLevel(), absolutePos.getX(), absolutePos.getY(), absolutePos.getZ());

		helper.assertTrue(helper.getBlockState(targetPos).is(Blocks.FARMLAND),
				"Block placer must invoke the hoe's right-click behavior");
		helper.assertTrue(placer.getItem(0).getDamageValue() == 1,
				"Block placer must retain item durability changes");
		helper.succeed();
	}
}
