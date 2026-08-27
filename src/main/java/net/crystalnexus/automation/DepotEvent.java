package net.crystalnexus.automation;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record DepotEvent(Type type, ItemStack stack, UUID transactionId, @Nullable UUID sourceProgramId) {
    public enum Type { ITEM_ADDED, ITEM_REMOVED, INVENTORY_CHANGED }

    public DepotEvent {
        stack = stack.copy();
    }
}
