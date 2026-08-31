package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crystalnexus.processing.MachineTier;
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

import java.util.List;
import java.util.Optional;

/** Backward-compatible separator recipe. The misspelled ID remains part of the datapack API. */
public class DustSeperationRecipe implements CrystalNexusRecipe {
    private final Optional<ItemStack> output;
    private final Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedOutput;
    private final NonNullList<Ingredient> recipeItems;
    private final int inputCount;
    private final Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput;
    private final Optional<ItemStack> secondaryOutput;
    private final Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedSecondaryOutput;
    private final float secondaryChance;
    private final int minimumMachineTier;

    public DustSeperationRecipe(ItemStack output, NonNullList<Ingredient> recipeItems) {
        this(Optional.of(output), Optional.empty(), recipeItems, 1, Optional.empty(), Optional.empty(), Optional.empty(), 0f, 1);
    }

    public DustSeperationRecipe(Optional<ItemStack> output,
                                Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedOutput,
                                NonNullList<Ingredient> recipeItems, int inputCount,
                                Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput,
                                Optional<ItemStack> secondaryOutput,
                                Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedSecondaryOutput,
                                float secondaryChance, int minimumMachineTier) {
        this.output = output.map(ItemStack::copy);
        this.taggedOutput = taggedOutput;
        this.recipeItems = recipeItems;
        this.inputCount = Math.max(1, inputCount);
        this.fluidInput = fluidInput;
        this.secondaryOutput = secondaryOutput.map(ItemStack::copy);
        this.taggedSecondaryOutput = taggedSecondaryOutput;
        this.secondaryChance = Math.max(0f, Math.min(1f, secondaryChance));
        this.minimumMachineTier = Math.max(1, Math.min(MachineTier.TUNGSTEN.level(), minimumMachineTier));
    }

    public int inputCount() { return inputCount; }
    public Optional<FluidChemicalReactionRecipe.FluidAmount> fluidInput() { return fluidInput; }
    public ItemStack secondaryOutput() {
        return secondaryOutput.map(ItemStack::copy)
            .or(() -> taggedSecondaryOutput.map(FluidChemicalReactionRecipe.TaggedItemOutput::stack))
            .orElse(ItemStack.EMPTY);
    }
    public float secondaryChance() { return secondaryChance; }
    public int minimumMachineTier() { return minimumMachineTier; }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() { return recipeItems; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider holder) { return getResultItem(holder); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output.map(ItemStack::copy)
            .or(() -> taggedOutput.map(FluidChemicalReactionRecipe.TaggedItemOutput::stack))
            .orElse(ItemStack.EMPTY);
    }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<DustSeperationRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<DustSeperationRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<DustSeperationRecipe> CODEC = RecordCodecBuilder.<DustSeperationRecipe>mapCodec(builder -> builder.group(
            ItemStack.STRICT_CODEC.optionalFieldOf("output").forGetter(recipe -> recipe.output),
            FluidChemicalReactionRecipe.TaggedItemOutput.CODEC.optionalFieldOf("output_tag").forGetter(recipe -> recipe.taggedOutput),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients", List.of()).xmap(Serializer::ingredients, List::copyOf)
                .forGetter(recipe -> recipe.recipeItems),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(recipe -> recipe.inputCount),
            FluidChemicalReactionRecipe.FluidAmount.CODEC.optionalFieldOf("fluid_input").forGetter(recipe -> recipe.fluidInput),
            ItemStack.STRICT_CODEC.optionalFieldOf("secondary_output").forGetter(recipe -> recipe.secondaryOutput),
            FluidChemicalReactionRecipe.TaggedItemOutput.CODEC.optionalFieldOf("secondary_output_tag").forGetter(recipe -> recipe.taggedSecondaryOutput),
            Codec.FLOAT.optionalFieldOf("secondary_chance", 0f).forGetter(recipe -> recipe.secondaryChance),
            Codec.INT.optionalFieldOf("minimum_machine_tier", 1).forGetter(recipe -> recipe.minimumMachineTier)
        ).apply(builder, DustSeperationRecipe::new)).flatXmap(Serializer::validate, DataResult::success);
        public static final StreamCodec<RegistryFriendlyByteBuf, DustSeperationRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        private static NonNullList<Ingredient> ingredients(List<Ingredient> values) {
            return NonNullList.of(Ingredient.EMPTY, values.toArray(Ingredient[]::new));
        }
        private static DataResult<DustSeperationRecipe> validate(DustSeperationRecipe recipe) {
            if (recipe.output.isEmpty() && recipe.taggedOutput.isEmpty())
                return DataResult.error(() -> "Dust separation recipe requires output or output_tag");
            if (recipe.recipeItems.isEmpty() && recipe.fluidInput.isEmpty())
                return DataResult.error(() -> "Dust separation recipe requires ingredients or fluid_input");
            return DataResult.success(recipe);
        }
        private static void encode(RegistryFriendlyByteBuf buf, DustSeperationRecipe recipe) {
            writeStack(buf, recipe.output); writeTag(buf, recipe.taggedOutput);
            buf.writeVarInt(recipe.recipeItems.size());
            recipe.recipeItems.forEach(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient));
            buf.writeVarInt(recipe.inputCount); writeFluid(buf, recipe.fluidInput);
            writeStack(buf, recipe.secondaryOutput); writeTag(buf, recipe.taggedSecondaryOutput);
            buf.writeFloat(recipe.secondaryChance); buf.writeVarInt(recipe.minimumMachineTier);
        }
        private static DustSeperationRecipe decode(RegistryFriendlyByteBuf buf) {
            Optional<ItemStack> output = readStack(buf);
            Optional<FluidChemicalReactionRecipe.TaggedItemOutput> tagged = readTag(buf);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            int count = buf.readVarInt();
            return new DustSeperationRecipe(output, tagged, ingredients, count, readFluid(buf), readStack(buf), readTag(buf), buf.readFloat(), buf.readVarInt());
        }
        private static void writeStack(RegistryFriendlyByteBuf buf, Optional<ItemStack> stack) {
            buf.writeBoolean(stack.isPresent()); stack.ifPresent(value -> ItemStack.STREAM_CODEC.encode(buf, value));
        }
        private static Optional<ItemStack> readStack(RegistryFriendlyByteBuf buf) {
            return buf.readBoolean() ? Optional.of(ItemStack.STREAM_CODEC.decode(buf)) : Optional.empty();
        }
        private static void writeTag(RegistryFriendlyByteBuf buf, Optional<FluidChemicalReactionRecipe.TaggedItemOutput> output) {
            buf.writeBoolean(output.isPresent());
            output.ifPresent(value -> { buf.writeResourceLocation(value.tag()); buf.writeVarInt(value.count()); });
        }
        private static Optional<FluidChemicalReactionRecipe.TaggedItemOutput> readTag(RegistryFriendlyByteBuf buf) {
            return buf.readBoolean() ? Optional.of(new FluidChemicalReactionRecipe.TaggedItemOutput(buf.readResourceLocation(), buf.readVarInt())) : Optional.empty();
        }
        private static void writeFluid(RegistryFriendlyByteBuf buf, Optional<FluidChemicalReactionRecipe.FluidAmount> fluid) {
            buf.writeBoolean(fluid.isPresent());
            fluid.ifPresent(value -> {
                buf.writeResourceLocation(value.fluid()); buf.writeVarInt(value.amount());
                buf.writeBoolean(value.material().isPresent()); value.material().ifPresent(buf::writeResourceLocation);
                buf.writeBoolean(value.tag());
            });
        }
        private static Optional<FluidChemicalReactionRecipe.FluidAmount> readFluid(RegistryFriendlyByteBuf buf) {
            if (!buf.readBoolean()) return Optional.empty();
            var id = buf.readResourceLocation(); int amount = buf.readVarInt();
            Optional<net.minecraft.resources.ResourceLocation> material = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
            return Optional.of(new FluidChemicalReactionRecipe.FluidAmount(id, amount, material, buf.readBoolean()));
        }
        @Override public MapCodec<DustSeperationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, DustSeperationRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
