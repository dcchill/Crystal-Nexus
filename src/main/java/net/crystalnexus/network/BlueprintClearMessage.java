package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.schematic.BlueprintSchematicManager;
import net.minecraft.core.BlockPos;
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
public record BlueprintClearMessage(BlockPos controllerPos) implements CustomPacketPayload {
	public static final Type<BlueprintClearMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "blueprint_clear"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintClearMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, BlueprintClearMessage message) -> buffer.writeBlockPos(message.controllerPos),
			(RegistryFriendlyByteBuf buffer) -> new BlueprintClearMessage(buffer.readBlockPos()));

	@Override
	public Type<BlueprintClearMessage> type() {
		return TYPE;
	}

	public static void handleData(final BlueprintClearMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player && player.blockPosition().distSqr(message.controllerPos) <= 4096) {
					BlueprintSchematicManager.clearFromController(player, message.controllerPos);
				}
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(BlueprintClearMessage.TYPE, BlueprintClearMessage.STREAM_CODEC, BlueprintClearMessage::handleData);
	}
}
