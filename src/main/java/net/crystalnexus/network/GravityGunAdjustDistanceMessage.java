package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.GravityGunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record GravityGunAdjustDistanceMessage(int steps) implements CustomPacketPayload {
	public static final Type<GravityGunAdjustDistanceMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "gravity_gun_adjust_distance"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GravityGunAdjustDistanceMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, GravityGunAdjustDistanceMessage message) -> buffer.writeInt(message.steps),
			(RegistryFriendlyByteBuf buffer) -> new GravityGunAdjustDistanceMessage(buffer.readInt()));

	@Override
	public Type<GravityGunAdjustDistanceMessage> type() {
		return TYPE;
	}

	public static void handleData(final GravityGunAdjustDistanceMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					GravityGunItem.adjustHoldDistance(player, Math.max(-8, Math.min(8, message.steps)));
				}
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, GravityGunAdjustDistanceMessage::handleData);
	}
}
