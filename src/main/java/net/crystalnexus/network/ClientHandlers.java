package net.crystalnexus.network;

import net.crystalnexus.network.payload.S2C_SendPage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientHandlers {

    public static void onSendPage(final S2C_SendPage msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                // Call: net.crystalnexus.client.gui.DepotScreenHooks.handle(msg)
                Class<?> hooks = Class.forName("net.crystalnexus.client.gui.DepotScreenHooks");
                hooks.getMethod("handle", S2C_SendPage.class).invoke(null, msg);
            } catch (Throwable ignored) {
                // If we're on server or screen not open, do nothing
            }
        });
    }
}
