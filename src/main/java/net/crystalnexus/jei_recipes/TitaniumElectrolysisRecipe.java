package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
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
import net.crystalnexus.processing.MachineTier;

import java.util.Optional;

public final class TitaniumElectrolysisRecipe implements CrystalNexusRecipe {
    private final Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput;
    private final Optional<Ingredient> itemInput;
    private final int itemInputCount;
    private final FluidChemicalReactionRecipe.FluidAmount fluidOutput;
    private final int minimumMachineTier;

    public TitaniumElectrolysisRecipe(Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput,
            Optional<Ingredient> itemInput, int itemInputCount,
            FluidChemicalReactionRecipe.FluidAmount fluidOutput, int minimumMachineTier) {
        this.fluidInput = fluidInput;
        this.itemInput = itemInput;
        this.itemInputCount = itemInputCount;
        this.fluidOutput = fluidOutput;
        this.minimumMachineTier = minimumMachineTier;
    }

    public Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput() { return fluidInput; }
    public Optional<Ingredient> itemInput() { return itemInput; }
    public int itemInputCount() { return itemInputCount; }
    public FluidChemicalReactionRecipe.FluidAmount fluidOutput() { return fluidOutput; }
    public int minimumMachineTier() { return minimumMachineTier; }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return ItemStack.EMPTY; }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        itemInput.ifPresent(ingredients::add);
        return ingredients;
    }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<TitaniumElectrolysisRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<TitaniumElectrolysisRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<TitaniumElectrolysisRecipe> CODEC = RecordCodecBuilder.<TitaniumElectrolysisRecipe>mapCodec(instance -> instance.group(
            FluidChemicalReactionRecipe.FluidAmount.CODEC.optionalFieldOf("fluid_input").forGetter(recipe -> recipe.fluidInput),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("item_input").forGetter(recipe -> recipe.itemInput),
            Codec.INT.optionalFieldOf("item_input_count", 1).forGetter(recipe -> recipe.itemInputCount),
            FluidChemicalReactionRecipe.FluidAmount.CODEC.fieldOf("fluid_output").forGetter(recipe -> recipe.fluidOutput),
            Codec.INT.optionalFieldOf("minimum_machine_tier", MachineTier.CHLOROPHYTE.level())
                .forGetter(recipe -> recipe.minimumMachineTier)
        ).apply(instance, TitaniumElectrolysisRecipe::new)).flatXmap(Serializer::validate, DataResult::success);

        public static final StreamCodec<RegistryFriendlyByteBuf, TitaniumElectrolysisRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::encode, Serializer::decode);

        private static DataResult<TitaniumElectrolysisRecipe> validate(TitaniumElectrolysisRecipe recipe) {
            int inputAmount = recipe.fluidInput.map(FluidChemicalReactionRecipe.FluidAmount::amount)
                .orElse(recipe.itemInputCount);
            String error = TitaniumElectrolysisRecipeRules.validationError(recipe.fluidInput.isPresent(),
                recipe.itemInput.isPresent(), inputAmount, recipe.fluidOutput.amount());
            return error == null ? DataResult.success(recipe) : DataResult.error(() -> error);
        }

        private static void encode(RegistryFriendlyByteBuf buffer, TitaniumElectrolysisRecipe recipe) {
            writeFluid(buffer, recipe.fluidInput);
            buffer.writeBoolean(recipe.itemInput.isPresent());
            recipe.itemInput.ifPresent(input -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, input));
            buffer.writeVarInt(recipe.itemInputCount);
            writeFluid(buffer, Optional.of(recipe.fluidOutput));
            buffer.writeVarInt(recipe.minimumMachineTier);
        }

        private static TitaniumElectrolysisRecipe decode(RegistryFriendlyByteBuf buffer) {
            Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput = readFluid(buffer);
            Optional<Ingredient> itemInput = buffer.readBoolean()
                ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)) : Optional.empty();
            int count = buffer.readVarInt();
            FluidChemicalReactionRecipe.FluidAmount output = readFluid(buffer).orElseThrow();
            return new TitaniumElectrolysisRecipe(fluidInput, itemInput, count, output, buffer.readVarInt());
        }

        private static void writeFluid(RegistryFriendlyByteBuf buffer,
                Optional<FluidChemicalReactionRecipe.FluidAmount> value) {
            buffer.writeBoolean(value.isPresent());
            value.ifPresent(fluid -> {
                buffer.writeResourceLocation(fluid.fluid());
                buffer.writeVarInt(fluid.amount());
                buffer.writeBoolean(fluid.material().isPresent());
                fluid.material().ifPresent(buffer::writeResourceLocation);
                buffer.writeBoolean(fluid.tag());
            });
        }

        private static Optional<FluidChemicalReactionRecipe.FluidAmount> readFluid(RegistryFriendlyByteBuf buffer) {
            if (!buffer.readBoolean()) return Optional.empty();
            var fluid = buffer.readResourceLocation();
            int amount = buffer.readVarInt();
            Optional<net.minecraft.resources.ResourceLocation> material = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
            return Optional.of(new FluidChemicalReactionRecipe.FluidAmount(fluid, amount, material, buffer.readBoolean()));
        }

        @Override public MapCodec<TitaniumElectrolysisRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, TitaniumElectrolysisRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
