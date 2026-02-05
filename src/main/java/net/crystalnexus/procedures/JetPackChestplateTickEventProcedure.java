package net.crystalnexus.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;

import net.crystalnexus.network.CrystalnexusModVariables;
import net.crystalnexus.init.CrystalnexusModItems;

public class JetPackChestplateTickEventProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return "";
		double thrust = 0;
		double ythrust = 0;
		if (entity.isSprinting()) {
			thrust = 1;
			ythrust = 0.1;
		} else {
			thrust = 0.1;
			ythrust = 0.7;
		}
		if (!entity.onGround()) {
			if (CrystalnexusModItems.JET_PACK_CHESTPLATE.get() == (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem()
					&& entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES).jetpackFly) {
				if ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("fuel") > 1
						|| (entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
					if (!(entity instanceof Player _plr ? _plr.getAbilities().instabuild : false)) {
						{
							final String _tagName = "fuel";
							final double _tagValue = ((entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
									.getDouble("fuel") - 1);
							CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY), tag -> tag.putDouble(_tagName, _tagValue));
						}
					}
					entity.setDeltaMovement(new Vec3((entity.getLookAngle().x * thrust), (entity.getLookAngle().y * thrust + ythrust), (entity.getLookAngle().z * thrust)));
					for (int index0 = 0; index0 < 20; index0++) {
						entity.fallDistance = 0;
					}
					world.addParticle(ParticleTypes.SMOKE, x, y, z, (Math.random() / 10), (-1), (Math.random() / 10));
				} else {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("No Fuel"), true);
				}
			}
		}
		return new java.text.DecimalFormat("\u00A7aFuel: ##.##").format(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("fuel"));
	}
}