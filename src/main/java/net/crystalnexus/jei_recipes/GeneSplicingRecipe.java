package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crystalnexus.item.PrisonCubeItem;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.SpawnEggItem;
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
	public static final int DEFAULT_BLOOD_AMOUNT = 500;
	private final Ingredient prisonCube;
	private final ResourceLocation mob;
	private final ItemStack result;
	private final int bloodAmount;
	private final int prisonCubeCount;

	public GeneSplicingRecipe(Ingredient prisonCube, ResourceLocation mob, ItemStack result, int bloodAmount, int prisonCubeCount) {
		this.prisonCube = prisonCube;
		this.mob = mob;
		this.result = result.copy();
		this.bloodAmount = Math.max(1, bloodAmount);
		this.prisonCubeCount = Math.max(1, prisonCubeCount);
	}

	public ItemStack spawnEgg() {
		SpawnEggItem egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(mob));
		return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
	}

	public int bloodAmount() { return bloodAmount; }
	public Ingredient prisonCube() { return prisonCube; }
	public ResourceLocation mob() { return mob; }
	public int prisonCubeCount() { return prisonCubeCount; }

	@Override
	public boolean matches(RecipeInput input, Level level) {
		return input.size() > 0 && this.prisonCube.test(input.getItem(0))
				&& input.getItem(0).getCount() >= prisonCubeCount
				&& PrisonCubeItem.hasStoredEntity(input.getItem(0), mob);
	}

	public boolean matches(FluidStack blood, ItemStack prisonCube) {
		return blood.is(CrystalnexusModFluids.BLOOD.get()) && blood.getAmount() >= bloodAmount
				&& prisonCube.getCount() >= prisonCubeCount && this.prisonCube.test(prisonCube)
				&& PrisonCubeItem.hasStoredEntity(prisonCube, mob);
	}

	@Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return result.copy(); }
	@Override public boolean canCraftInDimensions(int width, int height) { return true; }
	@Override public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }
	@Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, prisonCube); }
	@Override public RecipeType<?> getType() { return Type.INSTANCE; }
	@Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

	public static final class Type implements RecipeType<GeneSplicingRecipe> {
		public static final Type INSTANCE = new Type();
		private Type() {}
	}

	public static final class Serializer implements RecipeSerializer<GeneSplicingRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<GeneSplicingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("prison_cube").forGetter(recipe -> recipe.prisonCube),
				ResourceLocation.CODEC.fieldOf("mob").forGetter(recipe -> recipe.mob),
				ItemStack.STRICT_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(recipe -> recipe.result),
				Codec.intRange(1, 4000).optionalFieldOf("blood_amount", DEFAULT_BLOOD_AMOUNT).forGetter(recipe -> recipe.bloodAmount),
				Codec.INT.optionalFieldOf("prison_cube_count", 1).forGetter(recipe -> recipe.prisonCubeCount)
			).apply(instance, GeneSplicingRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> STREAM_CODEC = StreamCodec.of(
				(buffer, recipe) -> {
					Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.prisonCube);
					buffer.writeResourceLocation(recipe.mob);
					ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.result);
					buffer.writeVarInt(recipe.bloodAmount);
					buffer.writeVarInt(recipe.prisonCubeCount);
				},
				buffer -> new GeneSplicingRecipe(
						Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
						buffer.readResourceLocation(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt()));

		@Override public MapCodec<GeneSplicingRecipe> codec() { return CODEC; }
		@Override public StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> streamCodec() { return STREAM_CODEC; }
	}
}
