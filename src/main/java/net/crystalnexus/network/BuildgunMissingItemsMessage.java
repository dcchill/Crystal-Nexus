package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.client.BuildgunMissingOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record BuildgunMissingItemsMessage(List<Entry> entries) implements CustomPacketPayload {
	public static final Type<BuildgunMissingItemsMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "buildgun_missing_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunMissingItemsMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, BuildgunMissingItemsMessage message) -> {
				buffer.writeInt(message.entries.size());
				for (Entry entry : message.entries) {
					buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.item));
					buffer.writeInt(entry.count);
				}
			},
			(RegistryFriendlyByteBuf buffer) -> {
				int size = buffer.readInt();
				List<Entry> entries = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					Item item = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
					entries.add(new Entry(item, buffer.readInt()));
				}
				return new BuildgunMissingItemsMessage(entries);
			});

	@Override
	public Type<BuildgunMissingItemsMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunMissingItemsMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.CLIENTBOUND) {
			context.enqueueWork(() -> BuildgunMissingOverlay.show(message.entries)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(BuildgunMissingItemsMessage.TYPE, BuildgunMissingItemsMessage.STREAM_CODEC, BuildgunMissingItemsMessage::handleData);
	}

	public record Entry(Item item, int count) {
	}
}
