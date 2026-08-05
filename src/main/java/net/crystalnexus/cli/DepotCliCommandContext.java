package net.crystalnexus.cli;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.minecraft.server.level.ServerPlayer;

public record DepotCliCommandContext(ServerPlayer player, DepotCliMenu menu, DepotSavedData depot) {
    public boolean connected() {
        return menu.isConnected(player);
    }

    public boolean hasPermission(DepotCliCommand.Permission permission) {
        return menu.hasPermission(player);
    }
}
