package net.crystalnexus.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.crystalnexus.procedures.HoverpackLeftOnKeyReleasedProcedure;
import net.crystalnexus.procedures.HoverpackLeftOnKeyPressedProcedure;
import net.crystalnexus.CrystalnexusMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record HoverpackRightMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<HoverpackRightMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "key_hoverpack_right"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HoverpackRightMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, HoverpackRightMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new HoverpackRightMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<HoverpackRightMessage> type() {
		return TYPE;
	}

	public static void handleData(final HoverpackRightMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				pressAction(context.player(), message.eventType, message.pressedms);
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {

			HoverpackLeftOnKeyPressedProcedure.execute(entity);
		}
		if (type == 1) {

			HoverpackLeftOnKeyReleasedProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(HoverpackRightMessage.TYPE, HoverpackRightMessage.STREAM_CODEC, HoverpackRightMessage::handleData);
	}
}