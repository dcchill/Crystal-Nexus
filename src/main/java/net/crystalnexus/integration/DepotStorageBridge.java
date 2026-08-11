package net.crystalnexus.integration;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Optional external storage used by a depot while a compatible storage network
 * is connected. Implementations must only expose plain item ids because depot
 * storage does not retain data components.
 */
public interface DepotStorageBridge {
    boolean isConnected();

    long getCount(ResourceLocation itemId);

    long insert(ResourceLocation itemId, long amount);

    long extract(ResourceLocation itemId, long amount);

    Map<ResourceLocation, Long> snapshot();
}
