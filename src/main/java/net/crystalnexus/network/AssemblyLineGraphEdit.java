package net.crystalnexus.network;

import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyLineGraphEdit(int menu, String action, int a, int b, int c, int d, float x, float y) implements CustomPacketPayload {
    public static final Type<AssemblyLineGraphEdit> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:assembly_graph_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineGraphEdit> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeVarInt(p.menu); buf.writeUtf(p.action, 32); buf.writeVarInt(p.a); buf.writeVarInt(p.b); buf.writeVarInt(p.c); buf.writeVarInt(p.d); buf.writeFloat(p.x); buf.writeFloat(p.y); },
        buf -> new AssemblyLineGraphEdit(buf.readVarInt(), buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat()));
    @Override public Type<AssemblyLineGraphEdit> type() { return TYPE; }
    public static void handle(AssemblyLineGraphEdit p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof AssemblyLineMenu menu) || menu.containerId != p.menu || !menu.stillValid(context.player())) return;
            if (!p.action.equals("toggle") && !p.action.equals("edge") && !p.action.equals("remove_edge") && !p.action.equals("move") && !p.action.equals("export")) return;
            menu.controller.editGraph(p.action, p.a, p.b, p.c, p.d, p.x, p.y);
        });
    }
}
