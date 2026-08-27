package net.crystalnexus.cli;

import net.crystalnexus.data.DepotSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** V1 program action: the cable network, not the program, chooses the destination. */
public record SendItemAction(ResourceLocation itemId, int amount) {
    public SendItemAction {
        if (itemId == null) throw new IllegalArgumentException("itemId");
        amount = Math.max(1, amount);
    }

    public ProgramActionResult execute(ServerPlayer player, DepotSavedData depot) {
        return DepotSendItemService.send(player, depot, itemId, amount).status();
    }

    @Override public String toString() { return "Send " + itemId + " x" + amount; }
}
