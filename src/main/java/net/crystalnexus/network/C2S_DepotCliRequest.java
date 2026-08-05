package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record C2S_DepotCliRequest(int menuId, String input, boolean suggestions) implements CustomPacketPayload {
    public static final Type<C2S_DepotCliRequest> TYPE = new Type<>(DepotNetIds.id("depot_cli_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotCliRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2S_DepotCliRequest::menuId,
            ByteBufCodecs.stringUtf8(256), C2S_DepotCliRequest::input,
            ByteBufCodecs.BOOL, C2S_DepotCliRequest::suggestions,
            C2S_DepotCliRequest::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
