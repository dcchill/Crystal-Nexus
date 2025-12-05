package net.crystalnexus.item.inventory;

import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;

import net.crystalnexus.world.inventory.MiningLaserGuiMenu;
import net.crystalnexus.init.CrystalnexusModItems;

import javax.annotation.Nonnull;

@EventBusSubscriber
public class MiningLaserInventoryCapability extends ComponentItemHandler {
	@SubscribeEvent
	public static void onItemDropped(ItemTossEvent event) {
		if (event.getEntity().getItem().getItem() == CrystalnexusModItems.MINING_LASER.get()) {
			Player player = event.getPlayer();
			if (player.containerMenu instanceof MiningLaserGuiMenu)
				player.closeContainer();
		}
	}

	public MiningLaserInventoryCapability(MutableDataComponentHolder parent) {
		super(parent, DataComponents.CONTAINER, 1);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return stack.getItem() != CrystalnexusModItems.MINING_LASER.get();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return super.getStackInSlot(slot).copy();
	}
}