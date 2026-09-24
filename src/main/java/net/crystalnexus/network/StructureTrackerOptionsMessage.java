package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.client.StructureTrackerClient;
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
public record StructureTrackerOptionsMessage(List<String> structures, String selected) implements CustomPacketPayload {
	public static final Type<StructureTrackerOptionsMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "structure_tracker_options"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StructureTrackerOptionsMessage> STREAM_CODEC = StreamCodec.of(
			(buffer, message) -> {
				buffer.writeVarInt(message.structures.size());
				message.structures.forEach(buffer::writeUtf);
				buffer.writeUtf(message.selected);
			},
			buffer -> {
				int size = buffer.readVarInt();
				List<String> structures = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					structures.add(buffer.readUtf(32767));
				}
				return new StructureTrackerOptionsMessage(structures, buffer.readUtf(32767));
			});

	@Override
	public Type<StructureTrackerOptionsMessage> type() {
		return TYPE;
	}

	public static void handleData(StructureTrackerOptionsMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND) {
			context.enqueueWork(() -> StructureTrackerClient.openSelection(message.structures, message.selected)).exceptionally(exception -> {
				context.connection().disconnect(Component.literal(exception.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, StructureTrackerOptionsMessage::handleData);
	}
}
