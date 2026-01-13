package net.crystalnexus.network;

import net.minecraft.resources.ResourceLocation;

public final class DepotNetIds {
    // ✅ CHANGE THIS to your real mod id (must match mods.toml)
    public static final String MOD_ID = "crystalnexus";

    // Network version string used by the payload registrar (bump when breaking protocol)
    public static final String NETWORK_VERSION = "1";

    private DepotNetIds() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
