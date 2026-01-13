package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2S_RequestPage(String search, int page) implements CustomPacketPayload {
    public static final Type<C2S_RequestPage> TYPE = new Type<>(DepotNetIds.id("depot_request_page"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_RequestPage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, C2S_RequestPage::search,
                    ByteBufCodecs.VAR_INT,     C2S_RequestPage::page,
                    C2S_RequestPage::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
