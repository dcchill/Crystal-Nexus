package net.crystalnexus.jei_recipes;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

public final class ArcFurnaceRecipe implements CrystalNexusRecipe {
	private final ItemStack output;
	private final NonNullList<Ingredient> ingredients;

	public ArcFurnaceRecipe(ItemStack output, NonNullList<Ingredient> ingredients) {
		this.output = output;
		this.ingredients = ingredients;
	}

	@Override public boolean matches(RecipeInput input, Level level) { return false; }
	@Override public NonNullList<Ingredient> getIngredients() { return ingredients; }
	@Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return output.copy(); }
	@Override public boolean canCraftInDimensions(int width, int height) { return true; }
	@Override public ItemStack getResultItem(HolderLookup.Provider provider) { return output.copy(); }
	@Override public RecipeType<?> getType() { return Type.INSTANCE; }
	@Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

	public static final class Type implements RecipeType<ArcFurnaceRecipe> {
		public static final Type INSTANCE = new Type();
		private Type() {}
	}

	public static final class Serializer implements RecipeSerializer<ArcFurnaceRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<ArcFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
			Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> list.size() >= 1 && list.size() <= 2
				? DataResult.success(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)))
				: DataResult.error(() -> "Arc furnace recipes require one or two ingredients"), DataResult::success)
				.forGetter(recipe -> recipe.ingredients)
		).apply(builder, ArcFurnaceRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, ArcFurnaceRecipe> STREAM_CODEC = StreamCodec.of(
			Serializer::toNetwork, Serializer::fromNetwork);

		@Override public MapCodec<ArcFurnaceRecipe> codec() { return CODEC; }
		@Override public StreamCodec<RegistryFriendlyByteBuf, ArcFurnaceRecipe> streamCodec() { return STREAM_CODEC; }

		private static ArcFurnaceRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
			ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
			return new ArcFurnaceRecipe(ItemStack.STREAM_CODEC.decode(buffer), ingredients);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, ArcFurnaceRecipe recipe) {
			buffer.writeVarInt(recipe.ingredients.size());
			for (Ingredient ingredient : recipe.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
			ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
		}
	}
}
