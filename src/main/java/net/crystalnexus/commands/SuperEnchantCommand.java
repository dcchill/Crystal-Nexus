package net.crystalnexus.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class SuperEnchantCommand {
    private static final SimpleCommandExceptionType NO_ITEM = new SimpleCommandExceptionType(Component.literal("Hold an item to enchant."));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("superenchant")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("enchantment", ResourceArgument.resource(event.getBuildContext(), Registries.ENCHANTMENT))
                .then(Commands.argument("level", IntegerArgumentType.integer(0, CelestialGearForgeEnchanting.MAX_LEVEL))
                    .executes(context -> apply(context.getSource(), ResourceArgument.getEnchantment(context, "enchantment"),
                        IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static int apply(CommandSourceStack source, Holder<Enchantment> enchantment, int level) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var item = player.getMainHandItem();
        if (item.isEmpty()) throw NO_ITEM.create();
        EnchantmentHelper.updateEnchantments(item, mutable -> mutable.set(enchantment, level));
        source.sendSuccess(() -> Component.translatable("commands.enchant.success.single",
            Enchantment.getFullname(enchantment, level), player.getDisplayName()), true);
        return 1;
    }
}
