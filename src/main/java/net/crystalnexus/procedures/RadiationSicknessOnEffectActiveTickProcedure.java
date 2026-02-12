package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class RadiationSicknessOnEffectActiveTickProcedure {
public static void execute(LevelAccessor world, Entity entity) {
    if (entity == null)
        return;

    if (entity instanceof LivingEntity living) {

        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasHazmat = chest.getEnchantmentLevel(
                world.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ResourceKey.create(
                                Registries.ENCHANTMENT,
                                ResourceLocation.parse("crystalnexus:hazmat")
                        ))
        ) > 0;

        if (!hasHazmat) {

            // increment once per tick
            double time = entity.getPersistentData().getDouble("timeSick") + 1;
            entity.getPersistentData().putDouble("timeSick", time);

            if (!living.level().isClientSide())
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 1, false, false));

            if (time >= 1000 && !living.level().isClientSide())
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1, false, false));

            if (time >= 1500) {
                if (living.getRandom().nextInt(20) == 0) {
                    living.hurt(
                        new DamageSource(world.holderOrThrow(
                                ResourceKey.create(
                                        Registries.DAMAGE_TYPE,
                                        ResourceLocation.parse("crystalnexus:rad_sickness")
                                ))),
                        2
                    );
                }
            }
        }
    }
}
}