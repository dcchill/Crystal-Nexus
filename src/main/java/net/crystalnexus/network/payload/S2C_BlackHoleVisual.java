package net.crystalnexus.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_BlackHoleVisual(double x, double y, double z, double radius, int durationTicks) implements CustomPacketPayload {
	public static final Type<S2C_BlackHoleVisual> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", "black_hole_visual"));

	public static final StreamCodec<FriendlyByteBuf, S2C_BlackHoleVisual> STREAM_CODEC = StreamCodec.of(
			(buf, msg) -> {
				buf.writeDouble(msg.x());
				buf.writeDouble(msg.y());
				buf.writeDouble(msg.z());
				buf.writeDouble(msg.radius());
				buf.writeVarInt(msg.durationTicks());
			},
			buf -> new S2C_BlackHoleVisual(
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble(),
					buf.readVarInt()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
