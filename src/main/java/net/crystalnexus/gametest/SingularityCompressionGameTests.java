package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.SingularityCompressorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipe;
import net.crystalnexus.procedures.SingularityCompressorOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_singularity")
@PrefixGameTestTemplate(false)
public final class SingularityCompressionGameTests {
    @GameTest(template = "zero_point")
    public static void fixedRecipesAcceptRawMaterialsAndRejectProcessedInputs(GameTestHelper helper) {
        var level = helper.getLevel();
        var resources = new net.minecraft.world.item.Item[]{Items.DIAMOND, Items.EMERALD, Items.QUARTZ, Items.COAL, Items.REDSTONE};
        var outputs = new net.minecraft.world.item.Item[]{CrystalnexusModItems.DIAMOND_SINGULARITY.get(),
                CrystalnexusModItems.EMERALD_SINGULARITY.get(), CrystalnexusModItems.QUARTZ_SINGULARITY.get(),
                CrystalnexusModItems.COAL_SINGULARITY.get(), CrystalnexusModItems.REDSTONE_SINGULARITY.get()};
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.SINGULARITY_COMPRESSOR.get());
        var machine = (SingularityCompressorBlockEntity) helper.getBlockEntity(relative);
        BlockPos pos = helper.absolutePos(relative);
        var handler = new net.neoforged.neoforge.items.wrapper.InvWrapper(machine);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var menu = machine.createMenu(1, player.getInventory());
        helper.assertTrue(!menu.slots.getFirst().mayPlace(new ItemStack(Items.DIAMOND_SWORD))
                && !handler.insertItem(0, new ItemStack(Items.DIAMOND_SWORD), false).isEmpty(),
                "Unsupported items must remain blocked at the input boundary");
        for (int i = 0; i < resources.length; i++) {
            helper.assertTrue(SingularityCompressionRecipe.resultFor(level, new ItemStack(resources[i])).is(outputs[i]),
                    "Fixed resource must resolve to its singularity");
            machine.getPersistentData().putDouble("item", 0);
            machine.setItem(0, ItemStack.EMPTY);
            ItemStack input = new ItemStack(resources[i], 2);
            helper.assertTrue(menu.slots.getFirst().mayPlace(input)
                    && machine.canPlaceItemThroughFace(0, input, net.minecraft.core.Direction.UP)
                    && handler.insertItem(0, input.copy(), false).isEmpty(),
                    "GUI and automation must accept every fixed resource");
            machine.setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(9, input);
            menu.quickMoveStack(player, 3);
            helper.assertTrue(machine.getItem(0).is(resources[i]) && machine.getItem(0).getCount() == 2
                    && player.getInventory().getItem(9).isEmpty(),
                    "Shift-click must insert fixed resources into the input slot");
            SingularityCompressorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
            helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getPersistentData().getDouble("item") == 1,
                    "Compressor must accept fixed non-metal recipe ingredients");
        }
        var raw = new net.minecraft.world.item.Item[]{Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER};
        var ingots = new net.minecraft.world.item.Item[]{Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT};
        for (int i = 0; i < raw.length; i++) {
            machine.getPersistentData().putDouble("item", 0);
            machine.setItem(0, new ItemStack(raw[i]));
            SingularityCompressorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
            machine.setItem(0, new ItemStack(ingots[i]));
            SingularityCompressorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
            helper.assertTrue(machine.getItem(0).is(ingots[i]) && machine.getPersistentData().getDouble("item") == 1
                    && SingularityCompressionRecipe.resultFor(level, new ItemStack(ingots[i])).isEmpty()
                    && !menu.slots.getFirst().mayPlace(new ItemStack(ingots[i]))
                    && !machine.canPlaceItemThroughFace(0, new ItemStack(ingots[i]), net.minecraft.core.Direction.UP),
                    "Ingots must be rejected without consuming them or adding material credit");
        }
        machine.getPersistentData().putDouble("item", 10_239);
        machine.getPersistentData().putString("setItem", "minecraft:raw_iron");
        machine.getPersistentData().putDouble("progress", 0);
        machine.getEnergyStorage().receiveEnergy(1_024_000, false);
        machine.setItem(0, new ItemStack(Items.RAW_IRON));
        SingularityCompressorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        machine.getPersistentData().putDouble("progress", 300);
        SingularityCompressorOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(machine.getItem(1).is(CrystalnexusModItems.IRON_SINGULARITY.get())
                && machine.getPersistentData().getDouble("item") == 0,
                "A raw-material batch must complete at exactly 10240 items");
        for (var processed : new net.minecraft.world.item.Item[]{Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.IRON_BLOCK, Items.OAK_PLANKS})
            helper.assertTrue(SingularityCompressionRecipe.resultFor(level, new ItemStack(processed)).isEmpty()
                && net.crystalnexus.item.GeneratedSingularityItem.create(new ItemStack(processed)).isEmpty(),
                "Processed inputs must not produce singularities");
        for (var gem : new net.minecraft.world.item.Item[]{Items.AMETHYST_SHARD, CrystalnexusModItems.INVERTIUM_CRYSTAL.get()})
            helper.assertTrue(!SingularityCompressionRecipe.resultFor(level, new ItemStack(gem)).isEmpty(),
                "Tagged gems must support generated singularities");
        ItemStack named = new ItemStack(Items.RAW_IRON);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Custom"));
        helper.assertTrue(SingularityCompressionRecipe.resultFor(level, named).isEmpty(), "Custom components must remain rejected");
        helper.succeed();
    }
}
