package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.StructureTrackerItem;
import net.minecraft.core.registries.Registries;
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
public record StructureTrackerSelectMessage(String structure) implements CustomPacketPayload {
	public static final Type<StructureTrackerSelectMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "structure_tracker_select"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StructureTrackerSelectMessage> STREAM_CODEC = StreamCodec.of(
			(buffer, message) -> buffer.writeUtf(message.structure),
			buffer -> new StructureTrackerSelectMessage(buffer.readUtf(32767)));

	@Override
	public Type<StructureTrackerSelectMessage> type() {
		return TYPE;
	}

	public static void handleData(StructureTrackerSelectMessage message, IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (!(context.player() instanceof ServerPlayer player)) {
					return;
				}
				ResourceLocation id = ResourceLocation.tryParse(message.structure);
				if (id == null || !StructureTrackerItem.RESOURCE_METEOR.equals(id)
						&& !player.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(id)) {
					return;
				}
				for (InteractionHand hand : InteractionHand.values()) {
					ItemStack stack = player.getItemInHand(hand);
					if (stack.getItem() instanceof StructureTrackerItem) {
						StructureTrackerItem.select(stack, id);
						player.displayClientMessage(Component.translatable("message.crystalnexus.structure_tracker.selected", StructureTrackerItem.displayName(id)), true);
						return;
					}
				}
			}).exceptionally(exception -> {
				context.connection().disconnect(Component.literal(exception.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, StructureTrackerSelectMessage::handleData);
	}
}
