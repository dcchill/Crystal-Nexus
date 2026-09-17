package net.crystalnexus.network;

import net.crystalnexus.assembly.AssemblyLineMachine;
import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyLineRecipeSelect(int menu, int node, String recipe) implements CustomPacketPayload {
    public static final Type<AssemblyLineRecipeSelect> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:assembly_recipe_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineRecipeSelect> STREAM_CODEC = StreamCodec.of(
        (b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.node);b.writeUtf(p.recipe,256);},
        b->new AssemblyLineRecipeSelect(b.readVarInt(),b.readVarInt(),b.readUtf(256)));
    @Override public Type<AssemblyLineRecipeSelect> type(){return TYPE;}
    public static void handle(AssemblyLineRecipeSelect p, IPayloadContext context){context.enqueueWork(()->{
        if (!(context.player().containerMenu instanceof AssemblyLineMenu menu) || menu.containerId != p.menu || !menu.stillValid(context.player())) return;
        var holder = context.player().level().getRecipeManager().byKey(ResourceLocation.tryParse(p.recipe));
        if (holder.isEmpty()) return;
        var node = menu.controller.graph().node(p.node);
        if (node == null || node.machine == 0) return;
        var machine = menu.controller.machineAt(net.minecraft.core.BlockPos.of(node.machine));
        if (machine == null || !machine.supports(holder.get())) return;
        menu.controller.selectGraphRecipe(p.node, holder.get().id());
    });}
}
