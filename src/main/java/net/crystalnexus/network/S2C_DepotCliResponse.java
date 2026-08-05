package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record S2C_DepotCliResponse(int menuId, boolean connected, List<String> lines, List<String> suggestions)
        implements CustomPacketPayload {
    public static final Type<S2C_DepotCliResponse> TYPE = new Type<>(DepotNetIds.id("depot_cli_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DepotCliResponse> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2C_DepotCliResponse::menuId,
            ByteBufCodecs.BOOL, S2C_DepotCliResponse::connected,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), S2C_DepotCliResponse::lines,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), S2C_DepotCliResponse::suggestions,
            S2C_DepotCliResponse::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
