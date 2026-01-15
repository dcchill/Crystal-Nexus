package net.crystalnexus.radiation;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;

@EventBusSubscriber(modid = CrystalnexusMod.MODID) // <- standalone annotation (works in your environment)
public class RadiationEvents {

    private static final int TICK_INTERVAL = 20; // 1 second
    private static final int SCAN_RADIUS = 24;
    private static final int SCAN_Y = 8;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        var server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) continue;

            for (var player : level.players()) {
                BlockPos center = player.blockPosition();

                BlockPos min = center.offset(-SCAN_RADIUS, -SCAN_Y, -SCAN_RADIUS);
                BlockPos max = center.offset(SCAN_RADIUS, SCAN_Y, SCAN_RADIUS);

                BlockPos.betweenClosed(min, max).forEach(pos -> {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null || be.isRemoved()) return;

                    IItemHandler handler = getAnySidedItemHandler(level, pos);
                    if (handler == null) return;

                    int amount = countWaste(handler);
                    if (amount <= 0) return;

                    RadiationLogic.radiateFrom(level, pos, amount);
                });
            }
        }
    }

    private static IItemHandler getAnySidedItemHandler(ServerLevel level, BlockPos pos) {
        // Some blocks expose their item handler only on a side, so try null + all 6 sides
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (h != null) return h;

        for (Direction dir : Direction.values()) {
            h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (h != null) return h;
        }
        return null;
    }

    private static int countWaste(IItemHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == CrystalnexusModItems.BLUTONIUM_WASTE.get()) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
