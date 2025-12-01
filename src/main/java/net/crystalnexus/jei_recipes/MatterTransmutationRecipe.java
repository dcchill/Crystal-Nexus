package net.crystalnexus.jei_recipes;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;

public class MatterTransmutationRecipe implements Recipe<RecipeInput> {
	private final ItemStack output;
	private final NonNullList<Ingredient> recipeItems;

	public MatterTransmutationRecipe(ItemStack output, NonNullList<Ingredient> recipeItems) {
		this.output = output;
		this.recipeItems = recipeItems;
	}

	@Override
	public boolean matches(RecipeInput pContainer, Level pLevel) {
		if (pLevel.isClientSide()) {
			return false;
		}
		return false;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return recipeItems;
	}

	@Override
	public ItemStack assemble(RecipeInput input, HolderLookup.Provider holder) {
		return output;
	}

	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider provider) {
		return output.copy();
	}

	@Override
	public RecipeType<?> getType() {
		return Type.INSTANCE;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Serializer.INSTANCE;
	}

	public static class Type implements RecipeType<MatterTransmutationRecipe> {
		private Type() {}
		public static final RecipeType<MatterTransmutationRecipe> INSTANCE = new Type();
	}

	public static class Serializer implements RecipeSerializer<MatterTransmutationRecipe> {
		public static final Serializer INSTANCE = new Serializer();

		private static final MapCodec<MatterTransmutationRecipe> CODEC = RecordCodecBuilder.mapCodec(builder ->
			builder.group(
				ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
				Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap(ingredients -> {
					Ingredient[] arr = ingredients.toArray(Ingredient[]::new);
					if (arr.length == 0) {
						return DataResult.error(() -> "No ingredients found in custom recipe");
					} else {
						// Allow explicit EMPTY slots
						return DataResult.success(NonNullList.of(Ingredient.EMPTY, arr));
					}
				}, DataResult::success).forGetter(recipe -> recipe.recipeItems)
			).apply(builder, MatterTransmutationRecipe::new)
		);

		public static final StreamCodec<RegistryFriendlyByteBuf, MatterTransmutationRecipe> STREAM_CODEC =
				StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

		@Override
		public MapCodec<MatterTransmutationRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, MatterTransmutationRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static MatterTransmutationRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			NonNullList<Ingredient> inputs = NonNullList.withSize(size, Ingredient.EMPTY);
			for (int i = 0; i < size; i++) {
				Ingredient ing = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
				inputs.set(i, ing);
			}
			ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
			return new MatterTransmutationRecipe(result, inputs);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buf, MatterTransmutationRecipe recipe) {
			buf.writeVarInt(recipe.getIngredients().size());
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.isEmpty() || (ing.getItems().length > 0 && ing.getItems()[0].getItem() == Items.AIR)) {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, Ingredient.EMPTY);
				} else {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
				}
			}
			ItemStack.STREAM_CODEC.encode(buf, recipe.getResultItem(null));
		}
	}
}
