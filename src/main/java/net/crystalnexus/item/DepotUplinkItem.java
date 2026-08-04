package net.crystalnexus.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;

import net.crystalnexus.world.inventory.DepotMenu;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;

import io.netty.buffer.Unpooled;

public class DepotUplinkItem extends Item {
	public DepotUplinkItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		if (entity instanceof ServerPlayer serverPlayer) {
			if (!DepotSavedData.requirePoweredController(serverPlayer)) {
				return InteractionResultHolder.fail(entity.getItemInHand(hand));
			}
			serverPlayer.openMenu(new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Dimensional Depot Uplink");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
					packetBuffer.writeBlockPos(entity.blockPosition());
					packetBuffer.writeBoolean(true);
					packetBuffer.writeBoolean(DepotNetwork.hasCraftingUpgrade(serverPlayer));
					return new DepotMenu(id, inventory, packetBuffer);
				}
			}, buf -> {
				buf.writeBlockPos(entity.blockPosition());
				buf.writeBoolean(true);
				buf.writeBoolean(DepotNetwork.hasCraftingUpgrade(serverPlayer));
			});
		}
		return ar;
	}
}
