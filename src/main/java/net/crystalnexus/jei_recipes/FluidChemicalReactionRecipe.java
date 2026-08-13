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

import java.util.Optional;

public class FluidChemicalReactionRecipe implements CrystalNexusRecipe {
    public record FluidAmount(ResourceLocation fluid, int amount) {
        static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidAmount::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidAmount::amount)
        ).apply(instance, FluidAmount::new));

        public FluidStack stack() {
            return new FluidStack(BuiltInRegistries.FLUID.get(fluid), amount);
        }
    }

    private final Optional<FluidAmount> fluidInput1;
    private final Optional<FluidAmount> fluidInput2;
    private final Optional<Ingredient> itemInput1;
    private final Optional<Ingredient> itemInput2;
    private final Optional<FluidAmount> fluidOutput;
    private final Optional<ItemStack> itemOutput;

    public FluidChemicalReactionRecipe(Optional<FluidAmount> fluidInput1, Optional<FluidAmount> fluidInput2,
                                       Optional<Ingredient> itemInput1, Optional<Ingredient> itemInput2,
                                       Optional<FluidAmount> fluidOutput, Optional<ItemStack> itemOutput) {
        this.fluidInput1 = fluidInput1;
        this.fluidInput2 = fluidInput2;
        this.itemInput1 = itemInput1;
        this.itemInput2 = itemInput2;
        this.fluidOutput = fluidOutput;
        this.itemOutput = itemOutput.map(ItemStack::copy);
    }

    public Optional<FluidAmount> fluidInput(int tank) { return tank == 0 ? fluidInput1 : fluidInput2; }
    public Optional<Ingredient> itemInput(int slot) { return slot == 0 ? itemInput1 : itemInput2; }
    public Optional<FluidAmount> fluidOutput() { return fluidOutput; }
    public Optional<ItemStack> itemOutput() { return itemOutput.map(ItemStack::copy); }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return getResultItem(provider); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return itemOutput().orElse(ItemStack.EMPTY); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        itemInput1.ifPresent(ingredients::add);
        itemInput2.ifPresent(ingredients::add);
        return ingredients;
    }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<FluidChemicalReactionRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<FluidChemicalReactionRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<FluidChemicalReactionRecipe> CODEC = RecordCodecBuilder.<FluidChemicalReactionRecipe>mapCodec(instance -> instance.group(
            FluidAmount.CODEC.optionalFieldOf("fluid_input_1").forGetter(recipe -> recipe.fluidInput1),
            FluidAmount.CODEC.optionalFieldOf("fluid_input_2").forGetter(recipe -> recipe.fluidInput2),
            Ingredient.CODEC.optionalFieldOf("item_input_1").forGetter(recipe -> recipe.itemInput1),
            Ingredient.CODEC.optionalFieldOf("item_input_2").forGetter(recipe -> recipe.itemInput2),
            FluidAmount.CODEC.optionalFieldOf("output").forGetter(recipe -> recipe.fluidOutput),
            ItemStack.STRICT_CODEC.optionalFieldOf("item_output").forGetter(recipe -> recipe.itemOutput)
        ).apply(instance, FluidChemicalReactionRecipe::new)).flatXmap(Serializer::validate, DataResult::success);
        public static final StreamCodec<RegistryFriendlyByteBuf, FluidChemicalReactionRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override public MapCodec<FluidChemicalReactionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FluidChemicalReactionRecipe> streamCodec() { return STREAM_CODEC; }

        private static void encode(RegistryFriendlyByteBuf buffer, FluidChemicalReactionRecipe recipe) {
            writeFluid(buffer, recipe.fluidInput1);
            writeFluid(buffer, recipe.fluidInput2);
            writeIngredient(buffer, recipe.itemInput1);
            writeIngredient(buffer, recipe.itemInput2);
            writeFluid(buffer, recipe.fluidOutput);
            buffer.writeBoolean(recipe.itemOutput.isPresent());
            recipe.itemOutput.ifPresent(output -> ItemStack.STREAM_CODEC.encode(buffer, output));
        }

        private static FluidChemicalReactionRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new FluidChemicalReactionRecipe(readFluid(buffer), readFluid(buffer), readIngredient(buffer),
                readIngredient(buffer), readFluid(buffer),
                buffer.readBoolean() ? Optional.of(ItemStack.STREAM_CODEC.decode(buffer)) : Optional.empty());
        }

        private static DataResult<FluidChemicalReactionRecipe> validate(FluidChemicalReactionRecipe recipe) {
            return recipe.fluidOutput.isEmpty() && recipe.itemOutput.isEmpty()
                ? DataResult.error(() -> "Fluid chemical reaction recipes require output, item_output, or both")
                : DataResult.success(recipe);
        }

        private static void writeFluid(RegistryFriendlyByteBuf buffer, Optional<FluidAmount> value) {
            buffer.writeBoolean(value.isPresent());
            value.ifPresent(fluid -> { buffer.writeResourceLocation(fluid.fluid()); buffer.writeVarInt(fluid.amount()); });
        }
        private static Optional<FluidAmount> readFluid(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean() ? Optional.of(new FluidAmount(buffer.readResourceLocation(), buffer.readVarInt())) : Optional.empty();
        }
        private static void writeIngredient(RegistryFriendlyByteBuf buffer, Optional<Ingredient> value) {
            buffer.writeBoolean(value.isPresent());
            value.ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
        }
        private static Optional<Ingredient> readIngredient(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean() ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)) : Optional.empty();
        }
    }
}
