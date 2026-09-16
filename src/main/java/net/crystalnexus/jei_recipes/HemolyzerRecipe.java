package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Datapack-defined item-to-Blood conversion used by the Hemolyzer. */
public final class HemolyzerRecipe implements CrystalNexusRecipe {
    public record BloodOutput(ResourceLocation fluid, int amount) {
        private static final Codec<BloodOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(BloodOutput::fluid),
            Codec.INT.fieldOf("amount").forGetter(BloodOutput::amount)
        ).apply(instance, BloodOutput::new));
        public FluidStack stack() { return new FluidStack(BuiltInRegistries.FLUID.get(fluid), amount); }
    }

    private final Ingredient input;
    private final BloodOutput blood;
    public HemolyzerRecipe(Ingredient input, BloodOutput blood) { this.input = input; this.blood = blood; }
    public Ingredient input() { return input; }
    public BloodOutput blood() { return blood; }
    public boolean matches(ItemStack stack) { return input.test(stack); }
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return ItemStack.EMPTY; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<HemolyzerRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() { }
    }

    public static final class Serializer implements RecipeSerializer<HemolyzerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<HemolyzerRecipe> CODEC = RecordCodecBuilder.<HemolyzerRecipe>mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(HemolyzerRecipe::input),
            BloodOutput.CODEC.fieldOf("blood").forGetter(HemolyzerRecipe::blood)
        ).apply(instance, HemolyzerRecipe::new)).flatXmap(recipe -> recipe.blood.amount() > 0
            ? DataResult.success(recipe) : DataResult.error(() -> "Hemolyzer Blood output must be positive"), DataResult::success);
        private static final StreamCodec<RegistryFriendlyByteBuf, HemolyzerRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                buffer.writeResourceLocation(recipe.blood.fluid());
                buffer.writeVarInt(recipe.blood.amount());
            }, buffer -> new HemolyzerRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                new BloodOutput(buffer.readResourceLocation(), buffer.readVarInt())));
        @Override public MapCodec<HemolyzerRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, HemolyzerRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
