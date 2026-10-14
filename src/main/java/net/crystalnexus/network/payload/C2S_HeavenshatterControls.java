package net.crystalnexus.network.payload;

import net.crystalnexus.world.inventory.HeavenshatterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_HeavenshatterControls(int menuId, int candidate, int frequency, int bearing, int elevation, int action) implements CustomPacketPayload {
    public static final Type<C2S_HeavenshatterControls> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:heavenshatter_controls_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_HeavenshatterControls> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.menuId()); buffer.writeVarInt(packet.candidate()); buffer.writeVarInt(packet.frequency());
            buffer.writeVarInt(packet.bearing()); buffer.writeVarInt(packet.elevation()); buffer.writeVarInt(packet.action());
        }, buffer -> new C2S_HeavenshatterControls(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(C2S_HeavenshatterControls packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof HeavenshatterMenu menu && menu.containerId == packet.menuId()
                && menu.stillValid(context.player())) menu.controller().controls(context.player(), packet.candidate(), packet.frequency(), packet.bearing(), packet.elevation(), packet.action());
        });
    }
}
