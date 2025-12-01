package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;

import net.crystalnexus.network.CrystalnexusModVariables;
import net.crystalnexus.init.CrystalnexusModItems;

public class CompoundSwordToolInInventoryTickProcedure {
	public static void execute(LevelAccessor world, Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.GODLIKE_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 15);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 10);
			}
		} else if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.BLUTONIUM_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 10);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 7);
			}
		} else if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.ULTIMATE_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 8);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 6);
			}
		} else if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.REGULATED_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 7);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 5);
			}
		} else if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.CONTROLLED_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 6);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 4);
			}
		} else if (hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.STABLE_CRYSTAL.get()))) {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = true;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = true;
				_vars.syncPlayerVariables(entity);
			}
			if (itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
				itemstack.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE), 3);
			}
		} else {
			{
				final String _tagName = "crystalPowered";
				final boolean _tagValue = false;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putBoolean(_tagName, _tagValue));
			}
			{
				CrystalnexusModVariables.PlayerVariables _vars = entity.getData(CrystalnexusModVariables.PLAYER_VARIABLES);
				_vars.swordPower = false;
				_vars.syncPlayerVariables(entity);
			}
			EnchantmentHelper.updateEnchantments(itemstack, mutableEnchantments -> mutableEnchantments.removeIf(enchantment -> enchantment.is(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS))));
			EnchantmentHelper.updateEnchantments(itemstack, mutableEnchantments -> mutableEnchantments.removeIf(enchantment -> enchantment.is(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE))));
		}
		if (!(itemstack.getItem() == (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem())) {
			EnchantmentHelper.updateEnchantments(itemstack, mutableEnchantments -> mutableEnchantments.removeIf(enchantment -> enchantment.is(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS))));
			EnchantmentHelper.updateEnchantments(itemstack, mutableEnchantments -> mutableEnchantments.removeIf(enchantment -> enchantment.is(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SWEEPING_EDGE))));
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}