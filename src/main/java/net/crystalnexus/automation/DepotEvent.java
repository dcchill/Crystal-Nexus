package net.crystalnexus.automation;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record DepotEvent(Type type, ResourceLocation resourceId, UUID transactionId, @Nullable UUID sourceProgramId) {
    public enum Type { ITEM_ADDED, ITEM_REMOVED, FLUID_ADDED, FLUID_REMOVED, INVENTORY_CHANGED }
}
