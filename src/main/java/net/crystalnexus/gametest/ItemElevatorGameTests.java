package net.crystalnexus.gametest;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.procedures.ItemElevatorOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_elevators")
@PrefixGameTestTemplate(false)
public final class ItemElevatorGameTests {
    @GameTest(template = "empty")
    public static void transfersUpTo32InTheCorrectDirection(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var level = helper.getLevel();
        for (boolean down : new boolean[]{false, true}) {
            var block = down ? CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN.get() : CrystalnexusModBlocks.ITEM_ELEVATOR.get();
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), block.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), block.defaultBlockState());
            Container source = (Container) level.getBlockEntity(pos);
            Container target = (Container) level.getBlockEntity(down ? pos.below() : pos.above());
            Container opposite = (Container) level.getBlockEntity(down ? pos.above() : pos.below());
            for (var item : new net.minecraft.world.item.Item[]{Items.IRON_INGOT, Items.ENDER_PEARL, Items.IRON_SWORD}) {
                int max = item.getDefaultInstance().getMaxStackSize();
                for (int[] counts : new int[][]{{max, 0}, {1, 0}, {max, max - 1}, {max, max}, {0, 0}}) {
                    source.setItem(0, new ItemStack(item, counts[0]));
                    target.setItem(0, new ItemStack(item, counts[1]));
                    opposite.clearContent();
                    ItemElevatorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
                    int moved = Math.min(32, Math.min(counts[0], max - counts[1]));
                    helper.assertTrue(source.getItem(0).getCount() == counts[0] - moved
                            && target.getItem(0).getCount() == counts[1] + moved && opposite.isEmpty(),
                            "Elevators must respect batch, source and stack limits and only move in their own direction");
                }
            }
            source.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
            target.setItem(0, new ItemStack(Items.GOLD_INGOT, 1));
            ItemElevatorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
            helper.assertTrue(source.getItem(0).getCount() == 64 && target.getItem(0).is(Items.GOLD_INGOT)
                    && target.getItem(0).getCount() == 1 && opposite.isEmpty(), "Different items must not merge");
        }
        helper.succeed();
    }
}
