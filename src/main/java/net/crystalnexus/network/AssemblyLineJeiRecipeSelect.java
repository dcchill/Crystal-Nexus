package net.crystalnexus.network;

import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.world.inventory.AssemblyLineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Selects from the server's synchronized JEI index, including generated and third-party recipes. */
public record AssemblyLineJeiRecipeSelect(int menu, int node, String output, String input) implements CustomPacketPayload {
    public static final Type<AssemblyLineJeiRecipeSelect> TYPE = new Type<>(ResourceLocation.parse("crystalnexus:assembly_jei_recipe_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyLineJeiRecipeSelect> STREAM_CODEC = StreamCodec.of(
        (b,p)->{b.writeVarInt(p.menu);b.writeVarInt(p.node);b.writeUtf(p.output,256);b.writeUtf(p.input,256);},
        b->new AssemblyLineJeiRecipeSelect(b.readVarInt(),b.readVarInt(),b.readUtf(256),b.readUtf(256)));
    @Override public Type<AssemblyLineJeiRecipeSelect> type(){return TYPE;}
    public static void handle(AssemblyLineJeiRecipeSelect p, IPayloadContext context){context.enqueueWork(()->{
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof AssemblyLineMenu menu)
            || menu.containerId != p.menu || !menu.stillValid(player)) return;
        boolean cycle=p.output.isEmpty();
        ResourceLocation outputId=ResourceLocation.tryParse(p.output); var node=menu.controller.graph().node(p.node);
        if(node==null || node.machine==0) return;
        ResourceLocation machineId=BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(BlockPos.of(node.machine)).getBlock());
        var matches = outputId == null
            ? DepotJeiRecipeCache.recipesForMachine(player, machineId)
            : matches(player, outputId, machineId);
        if(outputId==null && !cycle) {
            if(node.outputItems!=null && node.outputItems.length>0) outputId=ResourceLocation.tryParse(node.outputItems[0]);
            if(outputId==null) return;
            matches = matches(player, outputId, machineId);
        }
        ResourceLocation inputId=ResourceLocation.tryParse(p.input);
        if(inputId!=null) matches=matches.stream().filter(recipe->recipe.inputs().stream()
            .anyMatch(slot->slot.alternatives().stream().anyMatch(stack->stack.itemId().equals(inputId)))).toList();
        if(!matches.isEmpty()) {
            if(cycle) menu.controller.selectNextGraphJeiRecipe(node.id,matches);
            else menu.controller.selectGraphJeiRecipe(node.id,matches.getFirst());
        }
    });}

    private static List<DepotJeiRecipeCache.Recipe> matches(ServerPlayer player, ResourceLocation outputId, ResourceLocation machineId) {
        List<DepotJeiRecipeCache.Recipe> exact=DepotJeiRecipeCache.recipesFor(player,outputId,machineId);
        if(!exact.isEmpty()) return exact;
        List<DepotJeiRecipeCache.Recipe> all=DepotJeiRecipeCache.recipesFor(player,outputId);
        List<DepotJeiRecipeCache.Recipe> smelting=new ArrayList<>();
        if(isCrystalNexusSmelter(machineId)) for(var recipe:all)
            if(recipe.categoryId().toString().equals("minecraft:smelting") || recipe.machineTypes().contains(ResourceLocation.parse("minecraft:furnace")))
                smelting.add(recipe);
        return (smelting.isEmpty()?all:smelting).stream().sorted(Comparator.comparing(recipe->recipe.id().toString())).toList();
    }

    private static boolean isCrystalNexusSmelter(ResourceLocation machineId) {
        return machineId != null && machineId.getNamespace().equals("crystalnexus") && machineId.getPath().endsWith("_smelter");
    }
}
