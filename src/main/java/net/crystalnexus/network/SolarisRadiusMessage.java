package net.crystalnexus.network;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.SolarisItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public record SolarisRadiusMessage(int steps) implements CustomPacketPayload {
    public static final Type<SolarisRadiusMessage> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "solaris_radius"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SolarisRadiusMessage> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, SolarisRadiusMessage::steps, SolarisRadiusMessage::new);

    @Override public Type<SolarisRadiusMessage> type() { return TYPE; }

    public static void handleData(SolarisRadiusMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isShiftKeyDown())
                SolarisItem.adjustRadius(player, message.steps());
        });
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CrystalnexusMod.addNetworkMessage(TYPE, STREAM_CODEC, SolarisRadiusMessage::handleData);
    }
}
