package net.crystalnexus.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.crystalnexus.network.CrystalnexusModVariables;
import net.crystalnexus.init.CrystalnexusModItems;

import javax.annotation.Nullable;

@EventBusSubscriber
public class NoRadProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!(hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get())) || hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.BLUTONIUM_CRYSTAL.get()))
				|| hasEntityInInventory(entity, new ItemStack(CrystalnexusModItems.RAW_BLUTONIUM.get())))) {
			CrystalnexusModVariables.MapVariables.get(world).timeSick = 0;
			CrystalnexusModVariables.MapVariables.get(world).syncData(world);
		}
	}

	private static boolean hasEntityInInventory(Entity entity, ItemStack itemstack) {
		if (entity instanceof Player player)
			return player.getInventory().contains(stack -> !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack));
		return false;
	}
}