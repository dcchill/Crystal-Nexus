package net.crystalnexus.client.gui;

import net.crystalnexus.network.payload.S2C_SendPage;

import net.minecraft.client.Minecraft;

public class DepotScreenHooks {

    public static void handle(S2C_SendPage packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DepotScreen screen) {
            screen.setPage(packet);
        }
    }
}
