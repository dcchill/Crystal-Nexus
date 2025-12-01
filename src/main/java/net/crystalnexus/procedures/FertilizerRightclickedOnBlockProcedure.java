package net.crystalnexus.procedures;

import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.core.BlockPos;

public class FertilizerRightclickedOnBlockProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double dx = 0;
		double dz = 0;
		double i = 0;
		double j = 0;
		String tag = "";
		i = -1;
		for (int index0 = 0; index0 < 3; index0++) {
			j = -1;
			for (int index1 = 0; index1 < 3; index1++) {
				if ((world.getBlockState(BlockPos.containing(x + i, y, z + j))).getBlock() instanceof BonemealableBlock) {
					if (world instanceof Level _level) {
						BlockPos _bp = BlockPos.containing(x + i, y, z + j);
						if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), _level, _bp) || BoneMealItem.growWaterPlant(new ItemStack(Items.BONE_MEAL), _level, _bp, null)) {
							if (!_level.isClientSide())
								_level.levelEvent(2005, _bp, 0);
						}
					}
				}
				j = j + 1;
			}
			i = i + 1;
		}
	}
}