package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.SolarisItem;
import net.crystalnexus.item.ToolEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public class CompoundPickaxeEnergyEvents {
    private static final ThreadLocal<Boolean> MINING_AREA = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!isPoweredPickaxe(tool)) return;

        if (!ToolEnergy.consume(player, tool, energyCost(), true)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.level().isClientSide()) return;

        ItemStack tool = player.getMainHandItem();
        if (!isPoweredPickaxe(tool)) return;

        if (!ToolEnergy.consume(player, tool, energyCost(), false)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("Out of power!").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        if (tool.getItem() instanceof SolarisItem && player instanceof ServerPlayer serverPlayer && !MINING_AREA.get())
            mineArea(serverPlayer, event.getPos(), SolarisItem.radius(tool));
    }

    private static boolean isPoweredPickaxe(ItemStack tool) {
        return tool.is(CrystalnexusModItems.COMPOUND_PICKAXE.get())
                || tool.is(CrystalnexusModItems.SOLARIS.get());
    }

    private static int energyCost() {
        return CrystalnexusConfig.ITEMS.COMPOUND_PICKAXE.energyCost();
    }

    private static void mineArea(ServerPlayer player, BlockPos center, int radius) {
        if (radius == 0) return;
        Level level = player.level();
        var look = player.getLookAngle();
        Direction.Axis axis = Direction.getNearest(look.x, look.y, look.z).getAxis();
        MINING_AREA.set(true);
        try {
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos pos = switch (axis) {
                        case X -> center.offset(0, a, b);
                        case Y -> center.offset(a, 0, b);
                        case Z -> center.offset(a, b, 0);
                    };
                    if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
                    var state = level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
                    if (!ToolEnergy.consume(player, player.getMainHandItem(), energyCost(), true)) return;
                    player.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            MINING_AREA.remove();
        }
    }

}
