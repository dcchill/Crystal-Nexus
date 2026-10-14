package net.crystalnexus.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record S2C_RailgunBeam(int shooterId, Vec3 start, Vec3 end, float charge) implements CustomPacketPayload {
    public static final Type<S2C_RailgunBeam> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", "railgun_beam"));
    public static final StreamCodec<FriendlyByteBuf, S2C_RailgunBeam> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeVarInt(msg.shooterId());
                buf.writeVec3(msg.start());
                buf.writeVec3(msg.end());
                buf.writeFloat(msg.charge());
            },
            buf -> new S2C_RailgunBeam(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readFloat()));

    @Override public Type<S2C_RailgunBeam> type() { return TYPE; }
}
