package net.crystalnexus.recipe;

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

import java.util.List;

public final class CelestialGearForgeRecipe implements CrystalNexusRecipe {
	public static final int INPUT_COUNT = 9;
	public static final int STAR_SLOT = 8;
	private final List<Ingredient> ingredients;
	private final ItemStack output;

	public CelestialGearForgeRecipe(List<Ingredient> ingredients, ItemStack output) {
		this.ingredients = List.copyOf(ingredients);
		this.output = output.copy();
	}

	public boolean matches(List<ItemStack> inputs) {
		if (inputs.size() < INPUT_COUNT) return false;
		for (int i = 0; i < INPUT_COUNT; i++)
			if (inputs.get(i).isEmpty() || !ingredients.get(i).test(inputs.get(i))) return false;
		return true;
	}

	public boolean matchesIngredient(int slot, ItemStack stack) {
		return slot >= 0 && slot < INPUT_COUNT && !stack.isEmpty() && ingredients.get(slot).test(stack);
	}
	public ItemStack output() { return output.copy(); }
	@Override public NonNullList<Ingredient> getIngredients() {
		NonNullList<Ingredient> result = NonNullList.create();
		result.addAll(ingredients);
		return result;
	}
	@Override public boolean matches(RecipeInput input, Level level) { return false; }
	@Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return output(); }
	@Override public boolean canCraftInDimensions(int width, int height) { return true; }
	@Override public ItemStack getResultItem(HolderLookup.Provider provider) { return output(); }
	@Override public RecipeType<?> getType() { return Type.INSTANCE; }
	@Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

	public static final class Type implements RecipeType<CelestialGearForgeRecipe> {
		public static final Type INSTANCE = new Type();
		private Type() { }
	}

	public static final class Serializer implements RecipeSerializer<CelestialGearForgeRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<CelestialGearForgeRecipe> CODEC = RecordCodecBuilder.<CelestialGearForgeRecipe>mapCodec(instance -> instance.group(
			Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(ingredients -> ingredients.size() == INPUT_COUNT
				? DataResult.success(ingredients) : DataResult.error(() -> "Celestial Gear Forge recipes require eight ingredients and one star ingredient"), DataResult::success)
				.forGetter(recipe -> recipe.ingredients),
			ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output)
		).apply(instance, CelestialGearForgeRecipe::new)).flatXmap(recipe -> recipe.output.isEmpty()
			? DataResult.error(() -> "Celestial Gear Forge output must not be empty") : DataResult.success(recipe), DataResult::success);
		private static final StreamCodec<RegistryFriendlyByteBuf, CelestialGearForgeRecipe> STREAM_CODEC = StreamCodec.of(
			(buffer, recipe) -> {
				for (Ingredient ingredient : recipe.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
				ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
			}, buffer -> {
				List<Ingredient> ingredients = java.util.stream.IntStream.range(0, INPUT_COUNT)
					.mapToObj(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)).toList();
				return new CelestialGearForgeRecipe(ingredients, ItemStack.STREAM_CODEC.decode(buffer));
			});
		@Override public MapCodec<CelestialGearForgeRecipe> codec() { return CODEC; }
		@Override public StreamCodec<RegistryFriendlyByteBuf, CelestialGearForgeRecipe> streamCodec() { return STREAM_CODEC; }
	}
}
