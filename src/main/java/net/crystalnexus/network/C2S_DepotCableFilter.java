package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_DepotCableFilter(int menuId, int slot, ResourceLocation itemId) implements CustomPacketPayload {
    public static final Type<C2S_DepotCableFilter> TYPE = new Type<>(DepotNetIds.id("depot_cable_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotCableFilter> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.menuId());
                buf.writeVarInt(value.slot());
                buf.writeResourceLocation(value.itemId());
            }, buf -> new C2S_DepotCableFilter(buf.readVarInt(), buf.readVarInt(), buf.readResourceLocation()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
