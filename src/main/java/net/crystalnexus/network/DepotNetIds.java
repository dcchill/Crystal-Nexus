package net.crystalnexus.network;

import net.minecraft.resources.ResourceLocation;

public final class DepotNetIds {
    public static final String MOD_ID = "crystalnexus";

    
    public static final String NETWORK_VERSION = "1";

    private DepotNetIds() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
