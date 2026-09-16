package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.MawBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MawGameTests {
    @GameTest(template = "zero_point", timeoutTicks = 200)
    public static void passivelyEatsMobsAndCollectsLoot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.MAW.get());
        MawBlockEntity maw = helper.getBlockEntity(pos);
        maw.setItem(0, new ItemStack(Items.BEEF, 63));
        helper.assertTrue(maw.collect(new ItemStack(Items.BEEF, 3)).isEmpty()
                && maw.getItem(0).getCount() == 64 && maw.getItem(1).getCount() == 2,
                "Collection must merge stacks and use remaining slots");
        maw.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        maw.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        helper.assertTrue(maw.collect(new ItemStack(Items.DIAMOND)).getCount() == 1,
                "Full inventories must return overflow rather than lose it");
        maw.clearContent();
        helper.assertTrue(!maw.canPlaceItemThroughFace(0, new ItemStack(Items.BEEF), Direction.UP)
                && maw.canTakeItemThroughFace(0, new ItemStack(Items.BEEF), Direction.DOWN),
                "Automation must only extract outputs");
        var cow = helper.spawn(EntityType.COW, pos.above());
        cow.setNoAi(true);
        cow.setHealth(4);
        helper.succeedWhen(() -> {
            helper.assertTrue(!cow.isAlive(), "The Maw must kill the cow without energy");
            boolean biomass = false, beef = false;
            for (int i = 0; i < 3; i++) {
                biomass |= maw.getItem(i).is(CrystalnexusModItems.BIOMASS.get());
                beef |= maw.getItem(i).is(Items.BEEF);
            }
            helper.assertTrue(biomass && beef, "The Maw must collect real mob loot and biomass");
        });
    }
}
