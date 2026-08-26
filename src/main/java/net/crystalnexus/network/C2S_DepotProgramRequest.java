package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record C2S_DepotProgramRequest(int menuId, Action action, UUID programId, CompoundTag program)
        implements CustomPacketPayload {
    public enum Action { LIST, LOAD, SAVE, DELETE, VALIDATE, RUN, CANCEL, STATUS }
    public static final UUID NONE = new UUID(0, 0);
    public static final Type<C2S_DepotProgramRequest> TYPE = new Type<>(DepotNetIds.id("depot_program_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotProgramRequest> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.menuId());
                buf.writeEnum(value.action());
                buf.writeUUID(value.programId());
                buf.writeNbt(value.program());
            }, buf -> new C2S_DepotProgramRequest(buf.readVarInt(), buf.readEnum(Action.class), buf.readUUID(), buf.readNbt()));

    public C2S_DepotProgramRequest(int menuId, Action action, UUID programId) {
        this(menuId, action, programId, new CompoundTag());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
