package net.crystalnexus.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.crystalnexus.schematic.BuildgunSchematicManager;
import net.crystalnexus.CrystalnexusMod;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record BuildgunMenuMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<BuildgunMenuMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "key_buildgun_menu"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunMenuMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, BuildgunMenuMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new BuildgunMenuMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<BuildgunMenuMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunMenuMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
					PacketDistributor.sendToPlayer(player, new BuildgunSchematicListMessage(BuildgunSchematicManager.listSchematics()));
				}
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(BuildgunMenuMessage.TYPE, BuildgunMenuMessage.STREAM_CODEC, BuildgunMenuMessage::handleData);
	}
}
