package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.BuildGunItem;
import net.crystalnexus.schematic.BuildgunSchematicManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record BuildgunTogglePlacementModeMessage() implements CustomPacketPayload {
	public static final Type<BuildgunTogglePlacementModeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "buildgun_toggle_placement_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunTogglePlacementModeMessage> STREAM_CODEC = StreamCodec.unit(new BuildgunTogglePlacementModeMessage());

	@Override
	public Type<BuildgunTogglePlacementModeMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunTogglePlacementModeMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ItemStack stack = heldBuildgun(player);
					if (!stack.isEmpty()) {
						BuildgunSchematicManager.togglePlacementMode(player, stack);
					}
				}
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	private static ItemStack heldBuildgun(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof BuildGunItem) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, BuildgunTogglePlacementModeMessage::handleData);
	}
}
