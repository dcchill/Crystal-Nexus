package net.crystalnexus.jei_recipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;

public final class TitaniumCarbideCircuitPressRecipe extends FluidChemicalReactionRecipe {
	private TitaniumCarbideCircuitPressRecipe(FluidChemicalReactionRecipe recipe) {
		super(recipe.fluidInput(0), recipe.fluidInput(1), recipe.itemInput(0), recipe.itemInput(1),
			recipe.fluidOutput(), recipe.itemOutput(), Optional.empty(), recipe.itemInputCount(0), recipe.itemInputCount(1));
	}

	@Override public RecipeType<?> getType() { return Type.INSTANCE; }
	@Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

	public static final class Type implements RecipeType<TitaniumCarbideCircuitPressRecipe> {
		public static final Type INSTANCE = new Type();
		private Type() {}
	}

	public static final class Serializer implements RecipeSerializer<TitaniumCarbideCircuitPressRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<TitaniumCarbideCircuitPressRecipe> CODEC =
			FluidChemicalReactionRecipe.Serializer.CODEC.xmap(TitaniumCarbideCircuitPressRecipe::new, recipe -> recipe);
		private static final StreamCodec<RegistryFriendlyByteBuf, TitaniumCarbideCircuitPressRecipe> STREAM_CODEC =
			StreamCodec.of((buffer, recipe) -> FluidChemicalReactionRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe),
				buffer -> new TitaniumCarbideCircuitPressRecipe(FluidChemicalReactionRecipe.Serializer.STREAM_CODEC.decode(buffer)));

		@Override public MapCodec<TitaniumCarbideCircuitPressRecipe> codec() { return CODEC; }
		@Override public StreamCodec<RegistryFriendlyByteBuf, TitaniumCarbideCircuitPressRecipe> streamCodec() { return STREAM_CODEC; }
	}
}
