package net.crystalnexus.network.payload;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2C_OreScanResult(List<BlockPos> positions, int durationTicks) implements CustomPacketPayload {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "ore_scan_result");
	public static final Type<S2C_OreScanResult> TYPE = new Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, S2C_OreScanResult> STREAM_CODEC = StreamCodec.of(
		(buf, msg) -> {
			buf.writeVarInt(msg.durationTicks());
			buf.writeVarInt(msg.positions().size());
			for (BlockPos p : msg.positions()) buf.writeBlockPos(p);
		},
		buf -> {
			int duration = buf.readVarInt();
			int size = buf.readVarInt();
			List<BlockPos> list = new ArrayList<>(size);
			for (int i = 0; i < size; i++) list.add(buf.readBlockPos());
			return new S2C_OreScanResult(list, duration);
		}
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
