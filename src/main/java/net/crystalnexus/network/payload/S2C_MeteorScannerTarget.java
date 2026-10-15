package net.crystalnexus.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_MeteorScannerTarget(BlockPos base, int diameter, int elapsed) implements CustomPacketPayload {
    public static final Type<S2C_MeteorScannerTarget> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:meteor_scanner_target"));
    public static final StreamCodec<ByteBuf, S2C_MeteorScannerTarget> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, S2C_MeteorScannerTarget::base,
        ByteBufCodecs.VAR_INT, S2C_MeteorScannerTarget::diameter,
        ByteBufCodecs.VAR_INT, S2C_MeteorScannerTarget::elapsed, S2C_MeteorScannerTarget::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
