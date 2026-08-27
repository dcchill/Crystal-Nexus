package net.crystalnexus.network.payload;

import net.crystalnexus.automation.DepotProgram;
import net.crystalnexus.network.DepotNetIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record S2C_DepotProgramsResponse(int menuId, List<DepotProgram> programs) implements CustomPacketPayload {
    public static final Type<S2C_DepotProgramsResponse> TYPE = new Type<>(DepotNetIds.id("depot_programs_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DepotProgramsResponse> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.menuId());
                buf.writeVarInt(Math.min(128, value.programs().size()));
                value.programs().stream().limit(128).forEach(program -> buf.writeNbt(program.save()));
            }, buf -> {
                int menuId = buf.readVarInt();
                int size = Math.max(0, Math.min(128, buf.readVarInt()));
                List<DepotProgram> programs = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    CompoundTag tag = buf.readNbt();
                    DepotProgram program = tag == null ? null : DepotProgram.load(tag);
                    if (program != null) programs.add(program);
                }
                return new S2C_DepotProgramsResponse(menuId, List.copyOf(programs));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
