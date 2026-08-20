package net.crystalnexus.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crystalnexus.jei_recipes.CrystalNexusRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GravitationalArrayRecipe implements CrystalNexusRecipe {
    public record ItemInput(Ingredient ingredient, int count) {
        private static final Codec<ItemInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemInput::ingredient),
            Codec.INT.fieldOf("count").forGetter(ItemInput::count)
        ).apply(instance, ItemInput::new));
    }

    public record Visuals(float red, float green, float blue, float scale) {
        private static final Codec<Visuals> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("red", 1.0F).forGetter(Visuals::red),
            Codec.FLOAT.optionalFieldOf("green", 0.85F).forGetter(Visuals::green),
            Codec.FLOAT.optionalFieldOf("blue", 0.25F).forGetter(Visuals::blue),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Visuals::scale)
        ).apply(instance, Visuals::new));
    }

    private final List<ItemInput> inputs;
    private final int temporalFluid;
    private final long energy;
    private final int duration;
    private final ItemStack output;
    private final Optional<Visuals> visuals;

    public GravitationalArrayRecipe(List<ItemInput> inputs, int temporalFluid, long energy, int duration,
                                    ItemStack output, Optional<Visuals> visuals) {
        this.inputs = List.copyOf(inputs);
        this.temporalFluid = temporalFluid;
        this.energy = energy;
        this.duration = duration;
        this.output = output.copy();
        this.visuals = visuals;
    }

    public List<ItemInput> inputs() { return inputs; }
    public int temporalFluid() { return temporalFluid; }
    public long energy() { return energy; }
    public int duration() { return duration; }
    public ItemStack output() { return output.copy(); }
    public Optional<Visuals> visuals() { return visuals; }

    /** Returns per-slot consumption, or an empty array when the four input slots do not satisfy this recipe. */
    public int[] consumptionPlan(List<ItemStack> stacks) {
        int[] remaining = new int[stacks.size()];
        for (int i = 0; i < stacks.size(); i++) remaining[i] = stacks.get(i).getCount();
        int[] plan = matchInputs(stacks, remaining, new int[stacks.size()], new boolean[inputs.size()], 0);
        return plan == null ? new int[0] : plan;
    }

    private int[] matchInputs(List<ItemStack> stacks, int[] remaining, int[] consumed, boolean[] used, int depth) {
        if (depth == inputs.size()) return consumed;
        for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
            if (used[inputIndex]) continue;
            ItemInput input = inputs.get(inputIndex);
            int[] nextRemaining = remaining.clone(), nextConsumed = consumed.clone();
            int needed = input.count();
            for (int slot = 0; slot < stacks.size() && needed > 0; slot++) {
                if (!input.ingredient().test(stacks.get(slot))) continue;
                int take = Math.min(needed, nextRemaining[slot]);
                nextRemaining[slot] -= take;
                nextConsumed[slot] += take;
                needed -= take;
            }
            if (needed > 0) continue;
            used[inputIndex] = true;
            int[] result = matchInputs(stacks, nextRemaining, nextConsumed, used, depth + 1);
            used[inputIndex] = false;
            if (result != null) return result;
        }
        return null;
    }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return output(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return output(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        inputs.forEach(input -> result.add(input.ingredient()));
        return result;
    }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<GravitationalArrayRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<GravitationalArrayRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<GravitationalArrayRecipe> CODEC = RecordCodecBuilder.<GravitationalArrayRecipe>mapCodec(instance -> instance.group(
            ItemInput.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.inputs),
            Codec.INT.fieldOf("temporal_fluid").forGetter(recipe -> recipe.temporalFluid),
            Codec.LONG.fieldOf("energy").forGetter(recipe -> recipe.energy),
            Codec.INT.fieldOf("duration").forGetter(recipe -> recipe.duration),
            ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
            Visuals.CODEC.optionalFieldOf("visuals").forGetter(recipe -> recipe.visuals)
        ).apply(instance, GravitationalArrayRecipe::new)).flatXmap(Serializer::validate, DataResult::success);
        private static final StreamCodec<RegistryFriendlyByteBuf, GravitationalArrayRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        private static DataResult<GravitationalArrayRecipe> validate(GravitationalArrayRecipe recipe) {
            if (recipe.inputs.isEmpty() || recipe.inputs.size() > 4 || recipe.inputs.stream().anyMatch(input -> input.count() <= 0))
                return DataResult.error(() -> "Gravitational array recipes require one to four positive item inputs");
            if (recipe.temporalFluid <= 0 || recipe.energy <= 0 || recipe.duration <= 0 || recipe.output.isEmpty())
                return DataResult.error(() -> "Gravitational array fluid, energy, duration, and output must be positive");
            return DataResult.success(recipe);
        }

        private static void encode(RegistryFriendlyByteBuf buffer, GravitationalArrayRecipe recipe) {
            buffer.writeVarInt(recipe.inputs.size());
            for (ItemInput input : recipe.inputs) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, input.ingredient());
                buffer.writeVarInt(input.count());
            }
            buffer.writeVarInt(recipe.temporalFluid);
            buffer.writeVarLong(recipe.energy);
            buffer.writeVarInt(recipe.duration);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
            buffer.writeBoolean(recipe.visuals.isPresent());
            recipe.visuals.ifPresent(visuals -> {
                buffer.writeFloat(visuals.red());
                buffer.writeFloat(visuals.green());
                buffer.writeFloat(visuals.blue());
                buffer.writeFloat(visuals.scale());
            });
        }

        private static GravitationalArrayRecipe decode(RegistryFriendlyByteBuf buffer) {
            List<ItemInput> inputs = new ArrayList<>();
            int count = buffer.readVarInt();
            for (int i = 0; i < count; i++) inputs.add(new ItemInput(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt()));
            int fluid = buffer.readVarInt();
            long energy = buffer.readVarLong();
            int duration = buffer.readVarInt();
            ItemStack output = ItemStack.STREAM_CODEC.decode(buffer);
            Optional<Visuals> visuals = buffer.readBoolean() ? Optional.of(new Visuals(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat())) : Optional.empty();
            return new GravitationalArrayRecipe(inputs, fluid, energy, duration, output, visuals);
        }

        @Override public MapCodec<GravitationalArrayRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GravitationalArrayRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
