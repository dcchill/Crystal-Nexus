package net.crystalnexus.network;

import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyLineRequest(int menu, String output, int amount, boolean rescan, boolean teach) implements CustomPacketPayload {
    public static final Type<AssemblyLineRequest> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:assembly_line_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineRequest> STREAM_CODEC = StreamCodec.of(
        (b, p) -> { b.writeVarInt(p.menu); b.writeUtf(p.output, 256); b.writeVarInt(p.amount); b.writeBoolean(p.rescan); b.writeBoolean(p.teach); },
        b -> new AssemblyLineRequest(b.readVarInt(), b.readUtf(256), b.readVarInt(), b.readBoolean(), b.readBoolean()));
    @Override public Type<AssemblyLineRequest> type() { return TYPE; }
    public static void handle(AssemblyLineRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof AssemblyLineMenu menu) || menu.containerId != request.menu || !menu.stillValid(context.player())) return;
            if (request.rescan) { menu.controller.retry(); return; }
            if (request.teach) return;
            ResourceLocation id = ResourceLocation.tryParse(request.output);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || request.amount < 1 || request.amount > 4096) return;
            menu.controller.enqueue(new ItemStack(BuiltInRegistries.ITEM.get(id), request.amount));
        });
    }
}
