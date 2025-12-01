/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;

@EventBusSubscriber
public class CrystalnexusModFuels {
	@SubscribeEvent
	public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
		ItemStack itemstack = event.getItemStack();
		if (itemstack.getItem() == CrystalnexusModItems.COAL_SINGULARITY.get())
			event.setBurnTime(16588800);
		else if (itemstack.getItem() == CrystalnexusModItems.BLUTONIUM_INGOT.get())
			event.setBurnTime(3200);
		else if (itemstack.getItem() == CrystalnexusModItems.RAW_CARBON.get())
			event.setBurnTime(1000);
		else if (itemstack.getItem() == CrystalnexusModItems.FLORATHANE.get())
			event.setBurnTime(22000);
		else if (itemstack.getItem() == CrystalnexusModItems.BIOMASS.get())
			event.setBurnTime(1600);
		else if (itemstack.getItem() == CrystalnexusModItems.CRUDE_OIL_BUCKET.get())
			event.setBurnTime(3200);
		else if (itemstack.getItem() == CrystalnexusModItems.GASOLINE_BUCKET.get())
			event.setBurnTime(9600);
	}
}