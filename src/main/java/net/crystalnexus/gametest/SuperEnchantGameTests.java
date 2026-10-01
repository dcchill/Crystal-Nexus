package net.crystalnexus.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class SuperEnchantGameTests {
    @GameTest(template = "zero_point")
    public static void superEnchantCommandAndLevelNames(GameTestHelper helper) throws CommandSyntaxException {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), "SuperEnchantTest"), ClientInformation.createDefault());
        var source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var fortune = registry.getOrThrow(Enchantments.FORTUNE);
        var unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(unbreaking, 3));
        for (int level : new int[]{1000, 0}) {
            helper.assertTrue(dispatcher.execute("superenchant minecraft:fortune " + level, source) == 1, "Command succeeds");
            helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(tool).getLevel(fortune) == level, "Command sets the exact level");
            helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(tool).getLevel(unbreaking) == 3, "Other enchantments remain intact");
        }
        for (int level : new int[]{1001, Integer.MAX_VALUE, -1}) {
            boolean rejectedLevel = false;
            try { dispatcher.execute("superenchant minecraft:fortune " + level, source); }
            catch (CommandSyntaxException expected) { rejectedLevel = true; }
            helper.assertTrue(rejectedLevel, "Command rejects levels outside 0–1000");
        }
        EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.set(fortune, Integer.MAX_VALUE));
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(tool).getLevel(fortune) == 1000, "Direct enchanting respects the cap");
        EnchantmentHelper.updateEnchantments(tool, mutable -> mutable.upgrade(fortune, Integer.MAX_VALUE));
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(tool).getLevel(fortune) == 1000, "Upgrading respects the cap");
        var registries = helper.getLevel().registryAccess();
        var saved = (CompoundTag) tool.save(registries);
        saved.getCompound("components").getCompound("minecraft:enchantments").getCompound("levels").putInt("minecraft:fortune", Integer.MAX_VALUE);
        var restored = ItemStack.parseOptional(registries, saved);
        helper.assertTrue(restored.is(Items.DIAMOND_PICKAXE) && EnchantmentHelper.getEnchantmentsForCrafting(restored).getLevel(fortune) == 1000,
            "Legacy items load safely with their enchantment levels capped");
        helper.assertTrue(Enchantment.getFullname(fortune, 1000).getString().endsWith(" M"), "Level 1000 uses Roman numerals");
        helper.assertTrue(Enchantment.getFullname(fortune, 1001).getString().endsWith(" 1,001"), "Larger levels use grouped numbers");
        boolean denied = false;
        try { dispatcher.execute("superenchant minecraft:fortune 1", source.withPermission(0)); }
        catch (CommandSyntaxException expected) { denied = true; }
        helper.assertTrue(denied, "Command requires operator permission");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        boolean rejected = false;
        try { dispatcher.execute("superenchant minecraft:fortune 1", source); }
        catch (CommandSyntaxException expected) { rejected = true; }
        helper.assertTrue(rejected, "An empty hand is rejected");
        helper.succeed();
    }
}
