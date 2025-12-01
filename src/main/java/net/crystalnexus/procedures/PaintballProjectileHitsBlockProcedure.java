package net.crystalnexus.procedures;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.crystalnexus.procedures.ColorBlocksProcedure;

public class PaintballProjectileHitsBlockProcedure {

    public static void execute(Projectile projectile, BlockHitResult hitResult) {
        if (projectile == null || hitResult == null) return;

        ItemStack dyeStack = ItemStack.EMPTY;
        Entity shooter = projectile.getOwner();
        if (shooter instanceof LivingEntity le) {
            ItemStack held = le.getMainHandItem();
            if (!held.isEmpty()) {
                dyeStack = new ItemStack(held.getItem());
                // copy dye NBT using components
                CustomData.update(DataComponents.CUSTOM_DATA, dyeStack, tag -> 
                    tag.putString("dye", held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("dye"))
                );
            }
        }

        if (!dyeStack.isEmpty()) {
            ColorBlocksProcedure.execute(projectile.level(), hitResult.getBlockPos().getX(), hitResult.getBlockPos().getY(), hitResult.getBlockPos().getZ(), dyeStack);
        }
    }
}
