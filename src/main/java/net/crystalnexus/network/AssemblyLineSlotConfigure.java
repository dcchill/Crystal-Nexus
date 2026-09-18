package net.crystalnexus.network;

import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyLineSlotConfigure(int menu, int node, int socket, boolean output, String item)
        implements CustomPacketPayload {
    public static final Type<AssemblyLineSlotConfigure> TYPE =
            new Type<>(ResourceLocation.parse("crystalnexus:assembly_slot_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineSlotConfigure> STREAM_CODEC =
            StreamCodec.of((b, p) -> {
                b.writeVarInt(p.menu); b.writeVarInt(p.node); b.writeVarInt(p.socket);
                b.writeBoolean(p.output); b.writeUtf(p.item, 256);
            }, b -> new AssemblyLineSlotConfigure(b.readVarInt(), b.readVarInt(), b.readVarInt(),
                    b.readBoolean(), b.readUtf(256)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AssemblyLineSlotConfigure packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof AssemblyLineMenu menu)
                    || menu.containerId != packet.menu || !menu.stillValid(context.player())) return;
            menu.controller.configureGraphSlot(packet.node, packet.socket, packet.output, packet.item);
        });
    }
}
