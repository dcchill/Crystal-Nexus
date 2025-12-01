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
import net.minecraft.core.BlockPos;

import net.crystalnexus.procedures.ZeroPointMultiGuiOpenProcedure;
import net.crystalnexus.procedures.ReactorMultiGuiOpenProcedure;
import net.crystalnexus.procedures.ReactionMultiGuiOpenProcedure;
import net.crystalnexus.procedures.OreProMultiGuiOpenProcedure;
import net.crystalnexus.procedures.MultiblockResearchPrevPageProcedure;
import net.crystalnexus.procedures.MultiblockResearchNextPageProcedure;
import net.crystalnexus.CrystalnexusMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record MultiblockGuiPage3ButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {

	public static final Type<MultiblockGuiPage3ButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "multiblock_gui_page_3_buttons"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockGuiPage3ButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, MultiblockGuiPage3ButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new MultiblockGuiPage3ButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));
	@Override
	public Type<MultiblockGuiPage3ButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final MultiblockGuiPage3ButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> handleButtonAction(context.player(), message.buttonID, message.x, message.y, message.z)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			MultiblockResearchNextPageProcedure.execute(world, x, y, z);
		}
		if (buttonID == 1) {

			MultiblockResearchPrevPageProcedure.execute(world, x, y, z);
		}
		if (buttonID == 2) {

			ZeroPointMultiGuiOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			OreProMultiGuiOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			ReactionMultiGuiOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			ReactorMultiGuiOpenProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		CrystalnexusMod.addNetworkMessage(MultiblockGuiPage3ButtonMessage.TYPE, MultiblockGuiPage3ButtonMessage.STREAM_CODEC, MultiblockGuiPage3ButtonMessage::handleData);
	}
}