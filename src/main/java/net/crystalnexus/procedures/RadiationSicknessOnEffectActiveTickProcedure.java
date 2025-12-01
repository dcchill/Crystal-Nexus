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

import net.crystalnexus.network.CrystalnexusModVariables;

public class RadiationSicknessOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY)
				.getEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse("crystalnexus:hazmat")))) != 0)) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 1, false, false));
			CrystalnexusModVariables.MapVariables.get(world).timeSick = CrystalnexusModVariables.MapVariables.get(world).timeSick + 1;
			CrystalnexusModVariables.MapVariables.get(world).syncData(world);
			if (1000 <= CrystalnexusModVariables.MapVariables.get(world).timeSick + 1) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1, false, false));
				CrystalnexusModVariables.MapVariables.get(world).timeSick = CrystalnexusModVariables.MapVariables.get(world).timeSick + 1;
				CrystalnexusModVariables.MapVariables.get(world).syncData(world);
			}
			if (1500 <= CrystalnexusModVariables.MapVariables.get(world).timeSick + 1) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 20)) {
					entity.hurt(new DamageSource(world.holderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("crystalnexus:rad_sickness")))), 2);
				}
			}
		}
	}
}