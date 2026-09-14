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
import java.util.List;

public class AcceleratorJeiRecipe implements CrystalNexusRecipe {
	private final ItemStack output;
	private final List<ItemStack> additionalOutputs;
	private final NonNullList<Ingredient> recipeItems;

	public AcceleratorJeiRecipe(ItemStack output, NonNullList<Ingredient> recipeItems) {
		this(output, List.of(), recipeItems);
	}

	public AcceleratorJeiRecipe(ItemStack output, List<ItemStack> additionalOutputs, NonNullList<Ingredient> recipeItems) {
		this.output = output;
		this.additionalOutputs = additionalOutputs;
		this.recipeItems = recipeItems;
	}

	public List<ItemStack> getOutputs() {
		return additionalOutputs.isEmpty() ? List.of(output.copy()) : concatOutputs();
	}

	private List<ItemStack> concatOutputs() {
		List<ItemStack> outputs = new java.util.ArrayList<>();
		outputs.add(output.copy());
		additionalOutputs.forEach(stack -> outputs.add(stack.copy()));
		return outputs;
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

	public static class Type implements RecipeType<AcceleratorJeiRecipe> {
		private Type() {
		}

		public static final RecipeType<AcceleratorJeiRecipe> INSTANCE = new Type();
	}

	public static class Serializer implements RecipeSerializer<AcceleratorJeiRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<AcceleratorJeiRecipe> CODEC = RecordCodecBuilder
				.mapCodec(builder -> builder.group(ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output), ItemStack.STRICT_CODEC.listOf().optionalFieldOf("additional_outputs", List.of()).forGetter(recipe -> recipe.additionalOutputs), Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(ingredients -> {
					Ingredient[] aingredient = ingredients.toArray(Ingredient[]::new); // Skip the empty check and create the array.
					if (aingredient.length == 0) {
						return DataResult.error(() -> "No ingredients found in custom recipe");
					} else {
						return DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
					}
				}, DataResult::success).forGetter(recipe -> recipe.recipeItems)).apply(builder, AcceleratorJeiRecipe::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, AcceleratorJeiRecipe> STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

		@Override
		public MapCodec<AcceleratorJeiRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, AcceleratorJeiRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static AcceleratorJeiRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
			NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
			inputs.replaceAll(ingredients -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
			ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
			List<ItemStack> additional = new java.util.ArrayList<>();
			for (int i = buf.readVarInt(); i > 0; i--) additional.add(ItemStack.STREAM_CODEC.decode(buf));
			return new AcceleratorJeiRecipe(output, additional, inputs);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buf, AcceleratorJeiRecipe recipe) {
			buf.writeVarInt(recipe.getIngredients().size());
			for (Ingredient ing : recipe.getIngredients()) {
				if (ing.getItems()[0].getItem() == Items.AIR)
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, Ingredient.EMPTY);
				else
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
			}
			ItemStack.STREAM_CODEC.encode(buf, recipe.getResultItem(null));
			buf.writeVarInt(recipe.additionalOutputs.size());
			for (ItemStack output : recipe.additionalOutputs) ItemStack.STREAM_CODEC.encode(buf, output);
		}
	}
}
