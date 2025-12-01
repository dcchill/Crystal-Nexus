package net.crystalnexus.item;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.crystalnexus.procedures.BlutoniumIngotItemInInventoryTickProcedure;

import java.util.List;

public class BlutoniumCrystalItem extends Item {
	public BlutoniumCrystalItem() {
		super(new Item.Properties().durability(40960));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("item.crystalnexus.blutonium_crystal.description_0"));
		list.add(Component.translatable("item.crystalnexus.blutonium_crystal.description_1"));
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		BlutoniumIngotItemInInventoryTickProcedure.execute(world, entity);
	}
}