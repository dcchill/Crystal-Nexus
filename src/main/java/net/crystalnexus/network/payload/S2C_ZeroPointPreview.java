package net.crystalnexus.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_ZeroPointPreview(BlockPos controllerPos, String templateId, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<S2C_ZeroPointPreview> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", "zeropoint_preview"));

    public static final StreamCodec<ByteBuf, S2C_ZeroPointPreview> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2C_ZeroPointPreview::controllerPos,
            ByteBufCodecs.STRING_UTF8, S2C_ZeroPointPreview::templateId,
            ByteBufCodecs.VAR_INT, S2C_ZeroPointPreview::durationTicks,
            S2C_ZeroPointPreview::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
