package net.crystalnexus.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class HotIngotItem extends Item {
	public HotIngotItem() {
		super(new Item.Properties().fireResistant());
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (world.isClientSide() || !(entity instanceof ServerPlayer player))
			return;
		GameType gamemode = player.gameMode.getGameModeForPlayer();
		if ((gamemode == GameType.SURVIVAL || gamemode == GameType.ADVENTURE) && player.getRemainingFireTicks() < 20)
			player.igniteForSeconds(3);
	}
}