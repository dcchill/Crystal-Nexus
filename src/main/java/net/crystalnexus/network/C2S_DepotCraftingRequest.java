package net.crystalnexus.network.payload;

import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2S_DepotCraftingRequest(int menuId, Action action, String search, int page, boolean craftableOnly,
        ResourceLocation targetId, int amount, ResourceLocation subjectId, ResourceLocation choiceId, int jobId)
        implements CustomPacketPayload {
    public enum Action { CATALOG, PREVIEW, START, SET_ROUTE, CLEAR_ROUTE, SET_MACHINE, CLEAR_MACHINE, CANCEL }
    public static final ResourceLocation NONE = ResourceLocation.parse("minecraft:air");
    public static final Type<C2S_DepotCraftingRequest> TYPE = new Type<>(DepotNetIds.id("depot_crafting_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotCraftingRequest> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.menuId());
                buf.writeEnum(value.action());
                buf.writeUtf(value.search(), 64);
                buf.writeVarInt(value.page());
                buf.writeBoolean(value.craftableOnly());
                buf.writeResourceLocation(value.targetId());
                buf.writeVarInt(value.amount());
                buf.writeResourceLocation(value.subjectId());
                buf.writeResourceLocation(value.choiceId());
                buf.writeVarInt(value.jobId());
            }, buf -> new C2S_DepotCraftingRequest(buf.readVarInt(), buf.readEnum(Action.class), buf.readUtf(64),
                    buf.readVarInt(), buf.readBoolean(), buf.readResourceLocation(), buf.readVarInt(), buf.readResourceLocation(),
                    buf.readResourceLocation(), buf.readVarInt()));

    public C2S_DepotCraftingRequest(int menuId, Action action, ResourceLocation targetId, int amount) {
        this(menuId, action, "", 0, true, targetId, amount, NONE, NONE, 0);
    }

    public C2S_DepotCraftingRequest(int menuId, Action action, String search, int page,
            ResourceLocation targetId, int amount, ResourceLocation subjectId, ResourceLocation choiceId, int jobId) {
        this(menuId, action, search, page, true, targetId, amount, subjectId, choiceId, jobId);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
