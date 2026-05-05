package net.crystalnexus.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_OrbitalStrikeBeam(double x, double y, double z, double skyY, int durationTicks, int impactDelayTicks) implements CustomPacketPayload {
	public static final Type<S2C_OrbitalStrikeBeam> TYPE =
			new Type<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", "orbital_strike_beam"));

	public static final StreamCodec<FriendlyByteBuf, S2C_OrbitalStrikeBeam> STREAM_CODEC = StreamCodec.of(
			(buf, msg) -> {
				buf.writeDouble(msg.x());
				buf.writeDouble(msg.y());
				buf.writeDouble(msg.z());
				buf.writeDouble(msg.skyY());
				buf.writeVarInt(msg.durationTicks());
				buf.writeVarInt(msg.impactDelayTicks());
			},
			buf -> new S2C_OrbitalStrikeBeam(
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble(),
					buf.readVarInt(),
					buf.readVarInt()
			)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
