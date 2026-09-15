package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crystalnexus.item.PrisonCubeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class GeneSplicingRecipe implements CrystalNexusRecipe {
	private final Ingredient fuel;
	private final Ingredient prisonCube;
	private final ResourceLocation mob;
	private final ItemStack result;
	private final int fuelCount;
	private final int prisonCubeCount;

	public GeneSplicingRecipe(Ingredient fuel, Ingredient prisonCube, ResourceLocation mob, ItemStack result) {
		this(fuel, prisonCube, mob, result, 1, 1);
	}

	public GeneSplicingRecipe(Ingredient fuel, Ingredient prisonCube, ResourceLocation mob, ItemStack result, int fuelCount, int prisonCubeCount) {
		this.fuel = fuel;
		this.prisonCube = prisonCube;
		this.mob = mob;
		this.result = result.copy();
		this.fuelCount = Math.max(1, fuelCount);
		this.prisonCubeCount = Math.max(1, prisonCubeCount);
	}

	public Ingredient fuel() { return fuel; }
	public Ingredient prisonCube() { return prisonCube; }
	public ResourceLocation mob() { return mob; }
	public int fuelCount() { return fuelCount; }
	public int prisonCubeCount() { return prisonCubeCount; }

	@Override
	public boolean matches(RecipeInput input, Level level) {
		return input.size() > 1 && matches(input.getItem(0), input.getItem(1));
	}

	public boolean matches(ItemStack fuel, ItemStack prisonCube) {
		return fuel.getCount() >= fuelCount && prisonCube.getCount() >= prisonCubeCount
				&& this.fuel.test(fuel) && this.prisonCube.test(prisonCube)
				&& PrisonCubeItem.hasStoredEntity(prisonCube, mob);
	}

	@Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return result.copy(); }
	@Override public boolean canCraftInDimensions(int width, int height) { return true; }
	@Override public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }
	@Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, fuel, prisonCube); }
	@Override public RecipeType<?> getType() { return Type.INSTANCE; }
	@Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

	public static final class Type implements RecipeType<GeneSplicingRecipe> {
		public static final Type INSTANCE = new Type();
		private Type() {}
	}

	public static final class Serializer implements RecipeSerializer<GeneSplicingRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<GeneSplicingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("fuel").forGetter(recipe -> recipe.fuel),
				Ingredient.CODEC_NONEMPTY.fieldOf("prison_cube").forGetter(recipe -> recipe.prisonCube),
				ResourceLocation.CODEC.fieldOf("mob").forGetter(recipe -> recipe.mob),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
				Codec.INT.optionalFieldOf("fuel_count", 1).forGetter(recipe -> recipe.fuelCount),
				Codec.INT.optionalFieldOf("prison_cube_count", 1).forGetter(recipe -> recipe.prisonCubeCount)
			).apply(instance, GeneSplicingRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> STREAM_CODEC = StreamCodec.of(
				(buffer, recipe) -> {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.fuel);
					Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.prisonCube);
					buffer.writeResourceLocation(recipe.mob);
					ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
					buffer.writeVarInt(recipe.fuelCount);
					buffer.writeVarInt(recipe.prisonCubeCount);
				},
				buffer -> new GeneSplicingRecipe(
						Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
						Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
						buffer.readResourceLocation(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt()));

		@Override public MapCodec<GeneSplicingRecipe> codec() { return CODEC; }
		@Override public StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> streamCodec() { return STREAM_CODEC; }
	}
}
