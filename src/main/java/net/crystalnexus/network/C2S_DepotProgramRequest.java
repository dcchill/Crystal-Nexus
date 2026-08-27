package net.crystalnexus.network.payload;

import net.crystalnexus.automation.DepotProgram;
import net.crystalnexus.network.DepotNetIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record C2S_DepotProgramRequest(int menuId, Action action, UUID programId, DepotProgram program)
        implements CustomPacketPayload {
    public enum Action { LIST, UPSERT, DELETE, TOGGLE }
    public static final Type<C2S_DepotProgramRequest> TYPE = new Type<>(DepotNetIds.id("depot_program_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotProgramRequest> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.menuId());
                buf.writeEnum(value.action());
                buf.writeUUID(value.programId() == null ? new UUID(0, 0) : value.programId());
                buf.writeNbt(value.program() == null ? new CompoundTag() : value.program().save());
            }, buf -> {
                int menuId = buf.readVarInt();
                Action action = buf.readEnum(Action.class);
                UUID id = buf.readUUID();
                CompoundTag tag = buf.readNbt();
                return new C2S_DepotProgramRequest(menuId, action, id.getLeastSignificantBits() == 0
                        && id.getMostSignificantBits() == 0 ? null : id,
                        tag == null || tag.isEmpty() ? null : DepotProgram.load(tag));
            });

    public static C2S_DepotProgramRequest list(int menuId) {
        return new C2S_DepotProgramRequest(menuId, Action.LIST, null, null);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
