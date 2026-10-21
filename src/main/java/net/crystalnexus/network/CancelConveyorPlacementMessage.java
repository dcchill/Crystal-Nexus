package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.util.ConveyorPlacement;
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

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public record CancelConveyorPlacementMessage() implements CustomPacketPayload {
    public static final Type<CancelConveyorPlacementMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "cancel_conveyor_placement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelConveyorPlacementMessage> STREAM_CODEC = StreamCodec.unit(new CancelConveyorPlacementMessage());

    @Override
    public Type<CancelConveyorPlacementMessage> type() { return TYPE; }

    public static void handleData(CancelConveyorPlacementMessage message, IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> ConveyorPlacement.cancel(context.player())).exceptionally(error -> {
                context.connection().disconnect(Component.literal(error.getMessage()));
                return null;
            });
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, CancelConveyorPlacementMessage::handleData);
    }
}
