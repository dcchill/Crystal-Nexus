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
import net.crystalnexus.program.DepotProgramIndex;
import net.crystalnexus.program.DepotProgramRunner;

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
            depot.recordCompletedJob(completed.id());
            Item item = BuiltInRegistries.ITEM.get(completed.targetId());
            String name = item == null || item == Items.AIR ? completed.targetId().toString()
                    : new ItemStack(item).getHoverName().getString();
            player.displayClientMessage(Component.literal(
                    "Crafting job #" + completed.id() + " complete: " + completed.amount() + " " + name), false);
        }
        for (var owner : DepotProgramIndex.get(event.getServer()).owners()) {
            DepotSavedData depot = DepotSavedData.get(event.getServer().overworld(), owner);
            if (event.getServer().getPlayerList().getPlayer(owner) == null && depot.getCraftingJob() != null
                    && (depot.getCraftingJob().currentStep() == null || !depot.getCraftingJob().currentStep().processing())) {
                DepotSavedData.CraftingJob completed = depot.advanceCraftingJob(
                        DepotNetwork.craftingProcessorCount(event.getServer().overworld(), owner));
                if (completed != null) depot.recordCompletedJob(completed.id());
            }
            DepotProgramRunner.tick(event.getServer(), owner, depot);
        }
    }
}
