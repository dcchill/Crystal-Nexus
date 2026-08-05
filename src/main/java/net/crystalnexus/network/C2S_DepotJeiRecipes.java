package net.crystalnexus.network.payload;

import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.network.DepotNetIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record C2S_DepotJeiRecipes(int generation, boolean reset, List<DepotJeiRecipeCache.Recipe> recipes)
        implements CustomPacketPayload {
    public static final Type<C2S_DepotJeiRecipes> TYPE = new Type<>(DepotNetIds.id("depot_jei_recipes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DepotJeiRecipes> STREAM_CODEC =
            StreamCodec.of(C2S_DepotJeiRecipes::encode, C2S_DepotJeiRecipes::decode);

    private static void encode(RegistryFriendlyByteBuf buf, C2S_DepotJeiRecipes message) {
        buf.writeVarInt(message.generation());
        buf.writeBoolean(message.reset());
        buf.writeVarInt(message.recipes().size());
        for (DepotJeiRecipeCache.Recipe recipe : message.recipes()) {
            buf.writeResourceLocation(recipe.id());
            buf.writeResourceLocation(recipe.categoryId());
            buf.writeUtf(recipe.categoryName(), 128);
            buf.writeVarInt(recipe.inputs().size());
            for (DepotJeiRecipeCache.Slot slot : recipe.inputs()) {
                buf.writeVarInt(slot.alternatives().size());
                for (DepotJeiRecipeCache.StackRef stack : slot.alternatives()) writeStack(buf, stack);
            }
            buf.writeVarInt(recipe.outputs().size());
            for (DepotJeiRecipeCache.StackRef stack : recipe.outputs()) writeStack(buf, stack);
            buf.writeVarInt(recipe.machineTypes().size());
            recipe.machineTypes().forEach(buf::writeResourceLocation);
        }
    }

    private static C2S_DepotJeiRecipes decode(RegistryFriendlyByteBuf buf) {
        int generation = buf.readVarInt();
        boolean reset = buf.readBoolean();
        int size = bounded(buf.readVarInt(), DepotJeiRecipeCache.MAX_CHUNK);
        List<DepotJeiRecipeCache.Recipe> recipes = new ArrayList<>(size);
        for (int recipeIndex = 0; recipeIndex < size; recipeIndex++) {
            ResourceLocation id = buf.readResourceLocation();
            ResourceLocation category = buf.readResourceLocation();
            String name = buf.readUtf(128);
            int inputSize = bounded(buf.readVarInt(), DepotJeiRecipeCache.MAX_SLOTS);
            List<DepotJeiRecipeCache.Slot> inputs = new ArrayList<>(inputSize);
            for (int slotIndex = 0; slotIndex < inputSize; slotIndex++) {
                int alternatives = bounded(buf.readVarInt(), DepotJeiRecipeCache.MAX_ALTERNATIVES);
                List<DepotJeiRecipeCache.StackRef> stacks = new ArrayList<>(alternatives);
                for (int stackIndex = 0; stackIndex < alternatives; stackIndex++) stacks.add(readStack(buf));
                inputs.add(new DepotJeiRecipeCache.Slot(stacks));
            }
            int outputSize = bounded(buf.readVarInt(), DepotJeiRecipeCache.MAX_SLOTS);
            List<DepotJeiRecipeCache.StackRef> outputs = new ArrayList<>(outputSize);
            for (int output = 0; output < outputSize; output++) outputs.add(readStack(buf));
            int machineSize = bounded(buf.readVarInt(), DepotJeiRecipeCache.MAX_ALTERNATIVES);
            List<ResourceLocation> machines = new ArrayList<>(machineSize);
            for (int machine = 0; machine < machineSize; machine++) machines.add(buf.readResourceLocation());
            recipes.add(new DepotJeiRecipeCache.Recipe(id, category, name, inputs, outputs, machines));
        }
        return new C2S_DepotJeiRecipes(generation, reset, recipes);
    }

    private static void writeStack(RegistryFriendlyByteBuf buf, DepotJeiRecipeCache.StackRef stack) {
        buf.writeResourceLocation(stack.itemId());
        buf.writeVarInt(stack.count());
    }

    private static DepotJeiRecipeCache.StackRef readStack(RegistryFriendlyByteBuf buf) {
        return new DepotJeiRecipeCache.StackRef(buf.readResourceLocation(), buf.readVarInt());
    }

    private static int bounded(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Invalid depot JEI payload size");
        return value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
