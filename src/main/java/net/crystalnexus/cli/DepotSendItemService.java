package net.crystalnexus.cli;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DepotSendItemService {
    private DepotSendItemService() {}

    public record Result(ProgramActionResult status, int sent) {}

    public static Result send(ServerPlayer player, DepotSavedData depot, ResourceLocation itemId, int amount) {
        int sent = DepotNetwork.routeItemToMachine(player, depot, itemId, amount).movedCount();
        return new Result(sent > 0 ? ProgramActionResult.SUCCESS : ProgramActionResult.WAITING, sent);
    }
}
