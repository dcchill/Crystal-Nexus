package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.MatterTransmutationTableBlockEntity;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.procedures.MatterTransmutationTableOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder("crystalnexus_transmutation_pattern")
@PrefixGameTestTemplate(false)
public final class MatterTransmutationPatternGameTests {
    @GameTest(template = "zero_point")
    public static void pooledPatternInputsFillRecipeSlots(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE.get());
        BlockPos pos = helper.absolutePos(relative);
        var table = (MatterTransmutationTableBlockEntity) helper.getLevel().getBlockEntity(pos);
        var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(items != null, "Table must expose automation inventory");
        ItemStack matter = new ItemStack(CrystalnexusModItems.EE_MATTER.get(), 4);
        helper.assertTrue(items.insertItem(0, matter, true).isEmpty() && table.isEmpty(),
            "Simulation accepts the pooled stack without changing inventory");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(items, matter, false).isEmpty(), "Accept all four matter items");
        MatterTransmutationTableOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(table.getItem(0).getCount() == 4 && table.getItem(1).isEmpty(),
            "Incomplete input must remain intact");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.GOLD_INGOT, 4), false).isEmpty(),
            "Accept all four ingots");
        MatterTransmutationTableOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 8; i++)
            helper.assertTrue(table.getItem(i).getCount() == 1
                && table.getItem(i).is(i % 2 == 0 ? CrystalnexusModItems.EE_MATTER.get() : Items.GOLD_INGOT),
                "Pattern ingredients must occupy the correct ring position " + i);
        helper.assertTrue(!items.insertItem(8, new ItemStack(Items.GOLD_INGOT), false).isEmpty(), "Protect output slot");
        table.getEnergyStorage().receiveEnergy(CrystalnexusConfig.MACHINES.MATTER_TRANSMUTATION_PROCESS.energyPerCraft(), false);
        for (int tick = 0; tick <= CrystalnexusConfig.MACHINES.MATTER_TRANSMUTATION_PROCESS.ticksPerCraft(); tick++)
            MatterTransmutationTableOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(table.getItem(8).is(Items.DIAMOND), "The distributed batch must craft a diamond");
        for (int i = 0; i < 8; i++) helper.assertTrue(table.getItem(i).isEmpty(), "Consume one full batch");
        ItemHandlerHelper.insertItemStacked(items, matter.copyWithCount(8), false);
        ItemHandlerHelper.insertItemStacked(items, new ItemStack(Items.GOLD_INGOT, 8), false);
        MatterTransmutationTableOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 8; i++) helper.assertTrue(table.getItem(i).getCount() == 2, "Preserve queued batch quantities");
        helper.succeed();
    }
}
