package net.crystalnexus.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import net.crystalnexus.init.CrystalnexusModItems;

import javax.annotation.Nullable;

@EventBusSubscriber
public class JetPackOnCraftProcedure {
	@SubscribeEvent
	public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
		execute(event, event.getCrafting());
	}

	public static void execute(ItemStack itemstack) {
		execute(null, itemstack);
	}

	private static void execute(@Nullable Event event, ItemStack itemstack) {
		if (CrystalnexusModItems.JET_PACK_CHESTPLATE.get() == itemstack.getItem() || CrystalnexusModItems.CARBON_JETPACK_CHESTPLATE.get() == itemstack.getItem()) {
			{
				final String _tagName = "fuel";
				final double _tagValue = 4096;
				CustomData.update(DataComponents.CUSTOM_DATA, itemstack, tag -> tag.putDouble(_tagName, _tagValue));
			}
		}
	}
}