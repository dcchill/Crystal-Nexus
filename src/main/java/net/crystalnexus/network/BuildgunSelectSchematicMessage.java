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
public record BuildgunSelectSchematicMessage(String schematicName) implements CustomPacketPayload {
	public static final Type<BuildgunSelectSchematicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "buildgun_select_schematic"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuildgunSelectSchematicMessage> STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buffer, BuildgunSelectSchematicMessage message) -> buffer.writeUtf(message.schematicName),
			(RegistryFriendlyByteBuf buffer) -> new BuildgunSelectSchematicMessage(buffer.readUtf(32767)));

	@Override
	public Type<BuildgunSelectSchematicMessage> type() {
		return TYPE;
	}

	public static void handleData(final BuildgunSelectSchematicMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) {
					ItemStack stack = heldBuildgun(player);
					if (!stack.isEmpty()) {
						BuildgunSchematicManager.selectSchematic(player, stack, message.schematicName);
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
		CrystalnexusMod.addNetworkMessage(BuildgunSelectSchematicMessage.TYPE, BuildgunSelectSchematicMessage.STREAM_CODEC, BuildgunSelectSchematicMessage::handleData);
	}
}
