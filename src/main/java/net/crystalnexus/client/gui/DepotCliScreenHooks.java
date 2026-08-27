package net.crystalnexus.client.gui;

import net.crystalnexus.network.payload.S2C_DepotCraftingResponse;
import net.crystalnexus.network.payload.S2C_DepotProgramsResponse;
import net.minecraft.client.Minecraft;

public final class DepotCliScreenHooks {
    private DepotCliScreenHooks() {
    }

    public static void handle(S2C_DepotCraftingResponse packet) {
        if (Minecraft.getInstance().screen instanceof DepotCliScreen screen) screen.handleCraftingResponse(packet);
    }

    public static void handle(S2C_DepotProgramsResponse packet) {
        if (Minecraft.getInstance().screen instanceof DepotCliScreen screen) screen.handleProgramsResponse(packet);
    }
}
