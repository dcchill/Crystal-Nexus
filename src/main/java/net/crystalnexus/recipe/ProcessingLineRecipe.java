package net.crystalnexus.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crystalnexus.block.ProcessingModuleBlock.Operation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ProcessingLineRecipe implements Recipe<RecipeInput> {
    public record Step(Operation operation, Ingredient ingredient, int time, int energyPerTick) {
        private static final Codec<Step> CODEC = RecordCodecBuilder.<Step>create(instance -> instance.group(
            Codec.STRING.comapFlatMap(name -> switch (name) {
                case "press" -> DataResult.success(Operation.PRESS);
                case "engrave" -> DataResult.success(Operation.ENGRAVE);
                case "drill" -> DataResult.success(Operation.DRILL);
                case "nanofabricate" -> DataResult.success(Operation.NANOFABRICATE);
                case "weld" -> DataResult.success(Operation.WELD);
                default -> DataResult.error(() -> "Unknown processing operation: " + name);
            }, operation -> switch (operation) {
                case PRESS -> "press";
                case ENGRAVE -> "engrave";
                case DRILL -> "drill";
                case NANOFABRICATE -> "nanofabricate";
                case WELD -> "weld";
            }).fieldOf("module").forGetter(Step::operation),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(Step::ingredient),
            Codec.intRange(1, 12000).optionalFieldOf("time", 20).forGetter(Step::time),
            Codec.intRange(1, 1000000).optionalFieldOf("energy_per_tick").forGetter(step -> java.util.Optional.of(step.energyPerTick()))
        ).apply(instance, (operation, ingredient, time, energy) -> new Step(operation, ingredient, time,
            energy.orElse(operation == Operation.NANOFABRICATE ? 160 : 20))));
    }

    private final Ingredient input;
    private final List<Step> steps;
    private final ItemStack result;

    public ProcessingLineRecipe(Ingredient input, List<Step> steps, ItemStack result) {
        this.input = input;
        this.steps = List.copyOf(steps);
        this.result = result.copy();
    }

    public Ingredient input() { return input; }
    public List<Step> steps() { return steps; }
    public boolean matches(ItemStack stack) { return input.test(stack); }
    @Override public boolean matches(RecipeInput input, Level level) { return input.size() > 0 && this.input.test(input.getItem(0)); }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input); }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<ProcessingLineRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<ProcessingLineRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<ProcessingLineRecipe> CODEC = RecordCodecBuilder.<ProcessingLineRecipe>mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ProcessingLineRecipe::input),
            Step.CODEC.listOf().fieldOf("steps").forGetter(ProcessingLineRecipe::steps),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter((ProcessingLineRecipe recipe) -> recipe.result)
        ).apply(instance, ProcessingLineRecipe::new)).flatXmap(Serializer::validate,
            (ProcessingLineRecipe recipe) -> DataResult.success(recipe));
        private static DataResult<ProcessingLineRecipe> validate(ProcessingLineRecipe recipe) {
            return !recipe.steps.isEmpty() && recipe.result.getCount() == 1
                    && recipe.steps.stream().allMatch(step -> switch (step.operation()) {
                        case PRESS -> !step.ingredient().isEmpty();
                        case WELD -> true;
                        default -> step.ingredient().isEmpty();
                    })
                ? DataResult.success(recipe) : DataResult.error(() -> "Processing recipes need steps, a single-item result, required press ingredients, and ingredients only on press or weld steps");
        }
        private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingLineRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                buffer.writeVarInt(recipe.steps.size());
                recipe.steps.forEach(step -> {
                    buffer.writeEnum(step.operation());
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, step.ingredient());
                    buffer.writeVarInt(step.time());
                    buffer.writeVarInt(step.energyPerTick());
                });
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            }, buffer -> {
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int size = buffer.readVarInt();
                if (size < 1 || size > 64) throw new IllegalArgumentException("Invalid processing step count: " + size);
                List<Step> steps = java.util.stream.IntStream.range(0, size)
                    .mapToObj(i -> new Step(buffer.readEnum(Operation.class), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        Math.max(1, buffer.readVarInt()), Math.max(1, buffer.readVarInt()))).toList();
                return new ProcessingLineRecipe(input, steps, ItemStack.STREAM_CODEC.decode(buffer));
            });
        @Override public MapCodec<ProcessingLineRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ProcessingLineRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
