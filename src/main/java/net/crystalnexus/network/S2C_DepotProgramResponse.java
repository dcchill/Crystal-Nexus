package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.crystalnexus.program.DepotProgramRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record S2C_DepotProgramResponse(int menuId, boolean success, String message, List<Summary> programs,
        CompoundTag selected, String runStatus, UUID currentNode, List<Problem> problems) implements CustomPacketPayload {
    public record Summary(UUID id, String name, int revision) {}
    public record Problem(UUID nodeId, String message) {}
    public static final UUID NONE = new UUID(0, 0);
    public static final Type<S2C_DepotProgramResponse> TYPE = new Type<>(DepotNetIds.id("depot_program_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_DepotProgramResponse> STREAM_CODEC = StreamCodec.of(
            S2C_DepotProgramResponse::write, S2C_DepotProgramResponse::read);

    private static void write(RegistryFriendlyByteBuf buf, S2C_DepotProgramResponse value) {
        buf.writeVarInt(value.menuId());
        buf.writeBoolean(value.success());
        buf.writeUtf(value.message(), 512);
        buf.writeVarInt(value.programs().size());
        for (Summary summary : value.programs()) {
            buf.writeUUID(summary.id()); buf.writeUtf(summary.name(), 32); buf.writeVarInt(summary.revision());
        }
        buf.writeNbt(value.selected());
        buf.writeUtf(value.runStatus(), 32);
        buf.writeUUID(value.currentNode());
        buf.writeVarInt(value.problems().size());
        for (Problem problem : value.problems()) {
            buf.writeUUID(problem.nodeId()); buf.writeUtf(problem.message(), 256);
        }
    }

    private static S2C_DepotProgramResponse read(RegistryFriendlyByteBuf buf) {
        int menu = buf.readVarInt();
        boolean success = buf.readBoolean();
        String message = buf.readUtf(512);
        int count = Math.min(32, Math.max(0, buf.readVarInt()));
        List<Summary> programs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) programs.add(new Summary(buf.readUUID(), buf.readUtf(32), buf.readVarInt()));
        CompoundTag selected = buf.readNbt();
        String status = buf.readUtf(32);
        UUID current = buf.readUUID();
        int errorCount = Math.min(256, Math.max(0, buf.readVarInt()));
        List<Problem> problems = new ArrayList<>(errorCount);
        for (int i = 0; i < errorCount; i++) problems.add(new Problem(buf.readUUID(), buf.readUtf(256)));
        return new S2C_DepotProgramResponse(menu, success, message, List.copyOf(programs),
                selected == null ? new CompoundTag() : selected, status, current, List.copyOf(problems));
    }

    public static S2C_DepotProgramResponse of(int menuId, boolean success, String message,
            List<Summary> programs, CompoundTag selected, DepotProgramRun run, List<Problem> problems) {
        return new S2C_DepotProgramResponse(menuId, success, message, programs, selected,
                run == null ? "IDLE" : run.status().name(), run == null || run.currentNode() == null ? NONE : run.currentNode(), problems);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
