package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.CraftingFactoryBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.procedures.AutoCrafterOnTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class CraftingFactoryGameTests {
    private CraftingFactoryGameTests() {}

    @GameTest(template = "zero_point")
    public static void shapedRecipesCraftWithoutTheirShape(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CrystalnexusModBlocks.CRAFTING_FACTORY.get());
        CraftingFactoryBlockEntity factory = helper.getBlockEntity(pos);
        factory.getEnergyStorage().receiveEnergy(2048, false);

        craft(factory, helper, pos, CrystalnexusModItems.IRON_MACHINE_BOLT.get(), 4,
            stack(0, CrystalnexusModItems.IRON_ROD.get(), 1),
            stack(8, CrystalnexusModItems.IRON_SHEET.get(), 1));
        craft(factory, helper, pos, CrystalnexusModItems.IRON_BEARING.get(), 2,
            stack(1, CrystalnexusModItems.IRON_ROD.get(), 1),
            stack(7, CrystalnexusModItems.IRON_SHEET.get(), 4));
        craft(factory, helper, pos, CrystalnexusModItems.ELECTRIC_MOTOR.get(), 1,
            stack(0, Items.REDSTONE, 3),
            stack(1, CrystalnexusModItems.IRON_MACHINE_BOLT.get(), 2),
            stack(2, CrystalnexusModItems.STATOR.get(), 1),
            stack(6, CrystalnexusModItems.IRON_SHEET.get(), 1),
            stack(7, CrystalnexusModItems.IRON_BEARING.get(), 1),
            stack(8, CrystalnexusModItems.IRON_ROD.get(), 1));

        helper.succeed();
    }

    private static void craft(CraftingFactoryBlockEntity factory, GameTestHelper helper, BlockPos pos,
            net.minecraft.world.item.Item output, int count, SlottedStack... inputs) {
        factory.clearContent();
        for (SlottedStack input : inputs) factory.setItem(input.slot(), input.stack());
        factory.setItem(10, new ItemStack(output));
        BlockPos absolute = helper.absolutePos(pos);
        for (int tick = 0; tick < 50; tick++) {
            AutoCrafterOnTickProcedure.execute(helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ());
        }
        helper.assertTrue(factory.getItem(9).is(output) && factory.getItem(9).getCount() == count,
            "Expected shape-agnostic recipe output " + output + " x" + count + ", got " + factory.getItem(9));
    }

    private static SlottedStack stack(int slot, net.minecraft.world.item.Item item, int count) {
        return new SlottedStack(slot, new ItemStack(item, count));
    }

    private record SlottedStack(int slot, ItemStack stack) {}
}
