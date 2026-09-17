package net.crystalnexus.network;

import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyLineItemConfigure(int menu, int node, int socket, String item) implements CustomPacketPayload {
    public static final Type<AssemblyLineItemConfigure> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:assembly_item_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineItemConfigure> STREAM_CODEC = StreamCodec.of(
        (b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.node);b.writeVarInt(p.socket);b.writeUtf(p.item,256);},
        b->new AssemblyLineItemConfigure(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(256)));
    @Override public Type<AssemblyLineItemConfigure> type(){return TYPE;}
    public static void handle(AssemblyLineItemConfigure p, IPayloadContext context){context.enqueueWork(()->{
        if (!(context.player().containerMenu instanceof AssemblyLineMenu menu) || menu.containerId != p.menu || !menu.stillValid(context.player())) return;
        menu.controller.configureGraphItem(p.node,p.socket,p.item);
    });}
}