package net.crystalnexus.procedures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.LevelAccessor;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class CompoundSwordOnHitProcedure {
    public static void execute(LevelAccessor world, Entity sourceentity) {
        if (sourceentity == null)
            return;

        IItemHandler handler = sourceentity.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);

                if (stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:energy_crystals")))) {
                    if (world instanceof ServerLevel serverLevel) {
                        stack.hurtAndBreak(1, serverLevel,
                            (sourceentity instanceof Player ? (Player) sourceentity : null),
                            e -> {}
                        );

                        if (handler instanceof IItemHandlerModifiable modifiable) {
                            modifiable.setStackInSlot(i, stack);
                        }
                    }
                    break; // only damage one crystal per hit
                }
            }
        }
    }
}
