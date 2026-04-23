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
public record BuildgunAdjustPlacementMessage(int steps, boolean rotate) implements CustomPacketPayload {
	public static final Type<BuildgunAdjustPlacementMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "buildgun_adjust_placement"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunAdjustPlacementMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, BuildgunAdjustPlacementMessage message) -> {
				buffer.writeInt(message.steps);
				buffer.writeBoolean(message.rotate);
			},
			(RegistryFriendlyByteBuf buffer) -> new BuildgunAdjustPlacementMessage(buffer.readInt(), buffer.readBoolean()));

	@Override
	public Type<BuildgunAdjustPlacementMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunAdjustPlacementMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ItemStack stack = heldBuildgun(player);
					if (!stack.isEmpty()) {
						BuildgunSchematicManager.adjustPlacement(player, stack, MthClamp.clamp(message.steps, -8, 8), message.rotate);
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

	private static final class MthClamp {
		private static int clamp(int value, int min, int max) {
			return Math.max(min, Math.min(max, value));
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(BuildgunAdjustPlacementMessage.TYPE, BuildgunAdjustPlacementMessage.STREAM_CODEC, BuildgunAdjustPlacementMessage::handleData);
	}
}
