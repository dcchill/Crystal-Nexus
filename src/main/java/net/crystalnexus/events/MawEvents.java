package net.crystalnexus.events;

import net.crystalnexus.block.entity.MawBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = "crystalnexus")
public final class MawEvents {
    @SubscribeEvent
    public static void eat(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        BlockPos pos = BlockPos.containing(mob.getX(), mob.getY() - 0.05, mob.getZ());
        if (!(mob.level().getBlockEntity(pos) instanceof MawBlockEntity maw)) return;
        event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(CrystalnexusModItems.BIOMASS.get())));
        event.getDrops().removeIf(drop -> {
            ItemStack remainder = maw.collect(drop.getItem());
            drop.setItem(remainder);
            return remainder.isEmpty();
        });
    }
}
