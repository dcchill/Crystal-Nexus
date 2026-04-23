package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.client.BuildgunClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record BuildgunSchematicListMessage(List<String> schematics) implements CustomPacketPayload {
	public static final Type<BuildgunSchematicListMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "buildgun_schematic_list"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunSchematicListMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, BuildgunSchematicListMessage message) -> {
				buffer.writeInt(message.schematics.size());
				for (String schematic : message.schematics) {
					buffer.writeUtf(schematic);
				}
			},
			(RegistryFriendlyByteBuf buffer) -> {
				int size = buffer.readInt();
				List<String> schematics = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					schematics.add(buffer.readUtf(32767));
				}
				return new BuildgunSchematicListMessage(schematics);
			});

	@Override
	public Type<BuildgunSchematicListMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunSchematicListMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND) {
			context.enqueueWork(() -> BuildgunClient.openSchematicMenu(message.schematics)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(BuildgunSchematicListMessage.TYPE, BuildgunSchematicListMessage.STREAM_CODEC, BuildgunSchematicListMessage::handleData);
	}
}
