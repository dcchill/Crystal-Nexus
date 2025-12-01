package net.crystalnexus.jei_recipes;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;

import java.util.*;

// JEI-only recipe for displaying Extractinator processing
public class ExtractinatorJEIRecipe implements Recipe<RecipeInput> {
    private final NonNullList<Ingredient> recipeItems;
    private final List<ItemStack> results;

    public ExtractinatorJEIRecipe(List<ItemStack> results, NonNullList<Ingredient> recipeItems) {
        this.results = results;
        this.recipeItems = recipeItems;
    }

    @Override
    public boolean matches(RecipeInput pContainer, Level pLevel) {
        return false; // Not used for JEI display
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider holder) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    public List<ItemStack> getResults() {
        return results;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    // ===================== TYPE =====================
    public static class Type implements RecipeType<ExtractinatorJEIRecipe> {
        private Type() {}
        public static final RecipeType<ExtractinatorJEIRecipe> INSTANCE = new Type();
    }

    // ===================== SERIALIZER =====================
public static class Serializer implements RecipeSerializer<ExtractinatorJEIRecipe> {
    public static final Serializer INSTANCE = new Serializer();

    // --- CODEC for JSON/Datapack parsing ---
    public static final MapCodec<ExtractinatorJEIRecipe> CODEC = RecordCodecBuilder.mapCodec(builder ->
        builder.group(
            ItemStack.CODEC.listOf().fieldOf("results")
                .forGetter(ExtractinatorJEIRecipe::getResults),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                .flatXmap(ingredients -> {
                    if (ingredients.isEmpty()) {
                        return DataResult.error(() -> "No ingredients in Extractinator recipe!");
                    }
                    return DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new)));
                }, DataResult::success)
                .forGetter(r -> r.getIngredients())
        ).apply(builder, ExtractinatorJEIRecipe::new)
    );

    // --- STREAM_CODEC for Network Sync ---
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractinatorJEIRecipe> STREAM_CODEC =
        StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

    @Override
    public MapCodec<ExtractinatorJEIRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ExtractinatorJEIRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static ExtractinatorJEIRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        int resultsCount = buf.readVarInt();
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < resultsCount; i++) {
            results.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        int ingredientsCount = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientsCount, Ingredient.EMPTY);
        for (int i = 0; i < ingredientsCount; i++) {
            ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
        }

        return new ExtractinatorJEIRecipe(results, ingredients);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, ExtractinatorJEIRecipe recipe) {
        buf.writeVarInt(recipe.getResults().size());
        for (ItemStack result : recipe.getResults()) {
            ItemStack.STREAM_CODEC.encode(buf, result);
        }

        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
        }
    }
}

}
