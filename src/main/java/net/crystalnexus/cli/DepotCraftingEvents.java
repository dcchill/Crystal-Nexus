package net.crystalnexus.cli;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class DepotCraftingEvents {
    private DepotCraftingEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            DepotSavedData depot = DepotSavedData.get(player);
            if (depot.getCraftingJob() == null) continue;
            DepotSavedData.CraftingJob completed = DepotProcessingService.tick(player, depot);
            if (completed == null) {
                completed = depot.advanceCraftingJob(DepotNetwork.craftingProcessorCount(player));
            }
            if (completed == null) continue;
            Item item = BuiltInRegistries.ITEM.get(completed.targetId());
            String name = item == null || item == Items.AIR ? completed.targetId().toString()
                    : new ItemStack(item).getHoverName().getString();
            player.displayClientMessage(Component.literal(
                    "Crafting job #" + completed.id() + " complete: " + completed.amount() + " " + name), false);
        }
    }
}
