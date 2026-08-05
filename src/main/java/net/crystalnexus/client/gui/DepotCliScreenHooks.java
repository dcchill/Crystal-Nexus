package net.crystalnexus.client.gui;

import net.crystalnexus.network.payload.S2C_DepotCliResponse;
import net.minecraft.client.Minecraft;

public final class DepotCliScreenHooks {
    private DepotCliScreenHooks() {
    }

    public static void handle(S2C_DepotCliResponse packet) {
        if (Minecraft.getInstance().screen instanceof DepotCliScreen screen) screen.handleResponse(packet);
    }
}
