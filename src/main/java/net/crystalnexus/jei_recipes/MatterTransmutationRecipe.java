package net.crystalnexus.jei_recipes;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;

public class MatterTransmutationRecipe implements Recipe<RecipeInput> {

	private final ItemStack output;
	private final NonNullList<Ingredient> ingredients;
	private final List<Integer> integers;

	/* ------------------------------------------------------------ */
	/* Constructor + normalization                                 */
	/* ------------------------------------------------------------ */

	public MatterTransmutationRecipe(ItemStack output,
	                                 NonNullList<Ingredient> ingredients,
	                                 List<Integer> integers) {
		this.output = output;
		this.ingredients = ingredients;

		// Normalize integers -> same length as ingredients, default 1
		int size = ingredients.size();
		List<Integer> normalized = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			int v = (integers != null && i < integers.size())
				? Math.max(1, integers.get(i))
				: 1;
			normalized.add(v);
		}
		this.integers = normalized;
	}

	/* ------------------------------------------------------------ */
	/* Helpers                                                      */
	/* ------------------------------------------------------------ */

	public int getInputCount(int index) {
		if (index < 0 || index >= integers.size()) return 1;
		return Math.max(1, integers.get(index));
	}

	public List<Integer> integers() {
		return integers;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	/* ------------------------------------------------------------ */
	/* Vanilla Recipe stuff                                         */
	/* ------------------------------------------------------------ */

	@Override
	public boolean matches(RecipeInput input, Level level) {
		return false;
	}

	@Override
	public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
		return output.copy();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
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

	/* ------------------------------------------------------------ */
	/* Type                                                         */
	/* ------------------------------------------------------------ */

	public static class Type implements RecipeType<MatterTransmutationRecipe> {
		public static final Type INSTANCE = new Type();
	}

	/* ------------------------------------------------------------ */
	/* Serializer                                                   */
	/* ------------------------------------------------------------ */

	public static class Serializer implements RecipeSerializer<MatterTransmutationRecipe> {

		public static final Serializer INSTANCE = new Serializer();

		private static final MapCodec<MatterTransmutationRecipe> CODEC =
			RecordCodecBuilder.mapCodec(builder -> builder.group(

				ItemStack.STRICT_CODEC.fieldOf("output")
					.forGetter(r -> r.output),

				Ingredient.CODEC.listOf().fieldOf("ingredients")
					.flatXmap(list -> {
						if (list.isEmpty())
							return DataResult.error(() -> "No ingredients");
						return DataResult.success(
							NonNullList.of(Ingredient.EMPTY,
								list.toArray(Ingredient[]::new))
						);
					}, DataResult::success)
					.forGetter(r -> r.ingredients),

				Codec.INT.listOf()
					.optionalFieldOf("integers", List.of())
					.forGetter(r -> r.integers)

			).apply(builder, MatterTransmutationRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, MatterTransmutationRecipe>
			STREAM_CODEC = StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

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

			NonNullList<Ingredient> inputs =
				NonNullList.withSize(size, Ingredient.EMPTY);
			for (int i = 0; i < size; i++) {
				inputs.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
			}

			List<Integer> ints = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				ints.add(Math.max(1, buf.readVarInt()));
			}

			ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
			return new MatterTransmutationRecipe(output, inputs, ints);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buf, MatterTransmutationRecipe recipe) {
			buf.writeVarInt(recipe.ingredients.size());

			for (Ingredient ing : recipe.ingredients) {
				ItemStack[] items = ing.getItems();
				if (ing.isEmpty() || items.length == 0 || items[0].getItem() == Items.AIR) {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, Ingredient.EMPTY);
				} else {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
				}
			}

			for (int i = 0; i < recipe.ingredients.size(); i++) {
				buf.writeVarInt(recipe.getInputCount(i));
			}

			ItemStack.STREAM_CODEC.encode(buf, recipe.output);
		}
	}
}
