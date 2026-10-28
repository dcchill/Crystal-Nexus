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
    public record Step(Operation operation, Ingredient ingredient, int time, int energyPerTick,
                       java.util.Optional<net.neoforged.neoforge.fluids.FluidStack> fluid) {
        public Step(Operation operation, Ingredient ingredient, int time, int energyPerTick) {
            this(operation, ingredient, time, energyPerTick, java.util.Optional.empty());
        }
        public boolean valid() {
            return time >= 1 && time <= 12000 && energyPerTick >= 1 && energyPerTick <= 1000000
                && (operation == Operation.PRESS || operation == Operation.WELD || operation == Operation.ASSEMBLE || ingredient.isEmpty())
                && (operation == Operation.APPLY_FLUID
                    ? fluid.isPresent() && !fluid.get().isEmpty() && fluid.get().getAmount() <= 4000
                    : fluid.isEmpty());
        }
        private static final Codec<net.neoforged.neoforge.fluids.FluidStack> FLUID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            net.minecraft.core.registries.BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(net.neoforged.neoforge.fluids.FluidStack::getFluid),
            Codec.intRange(1, 4000).fieldOf("amount").forGetter(net.neoforged.neoforge.fluids.FluidStack::getAmount)
        ).apply(instance, net.neoforged.neoforge.fluids.FluidStack::new));
        private static final Codec<Step> CODEC = RecordCodecBuilder.<Step>create(instance -> instance.group(
            Codec.STRING.comapFlatMap(name -> switch (name) {
                case "press" -> DataResult.success(Operation.PRESS);
                case "engrave" -> DataResult.success(Operation.ENGRAVE);
                case "drill" -> DataResult.success(Operation.DRILL);
                case "nanofabricate" -> DataResult.success(Operation.NANOFABRICATE);
                case "weld" -> DataResult.success(Operation.WELD);
                case "assemble" -> DataResult.success(Operation.ASSEMBLE);
                case "apply_fluid" -> DataResult.success(Operation.APPLY_FLUID);
                default -> DataResult.error(() -> "Unknown processing operation: " + name);
            }, operation -> switch (operation) {
                case PRESS -> "press";
                case ENGRAVE -> "engrave";
                case DRILL -> "drill";
                case NANOFABRICATE -> "nanofabricate";
                case WELD -> "weld";
                case ASSEMBLE -> "assemble";
                case APPLY_FLUID -> "apply_fluid";
            }).fieldOf("module").forGetter(Step::operation),
            Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(Step::ingredient),
            Codec.intRange(1, 12000).optionalFieldOf("time", 20).forGetter(Step::time),
            Codec.intRange(1, 1000000).optionalFieldOf("energy_per_tick").forGetter(step -> java.util.Optional.of(step.energyPerTick())),
            FLUID_CODEC.optionalFieldOf("fluid").forGetter(Step::fluid)
        ).apply(instance, (operation, ingredient, time, energy, fluid) -> new Step(operation, ingredient, time,
            energy.orElse(operation == Operation.NANOFABRICATE ? 160 : 20), fluid)));
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
            return !recipe.steps.isEmpty() && recipe.steps.size() <= 64 && recipe.result.getCount() == 1
                    && recipe.steps.stream().allMatch(Step::valid)
                ? DataResult.success(recipe) : DataResult.error(() -> "Processing recipes need 1-64 valid steps, a single-item result, ingredients only on press/weld/assemble, and fluid only on apply_fluid steps");
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
                    buffer.writeBoolean(step.fluid().isPresent());
                    step.fluid().ifPresent(fluid -> net.neoforged.neoforge.fluids.FluidStack.STREAM_CODEC.encode(buffer, fluid));
                });
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            }, buffer -> {
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int size = buffer.readVarInt();
                if (size < 1 || size > 64) throw new IllegalArgumentException("Invalid processing step count: " + size);
                List<Step> steps = java.util.stream.IntStream.range(0, size)
                    .mapToObj(i -> new Step(buffer.readEnum(Operation.class), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()
                            ? java.util.Optional.of(net.neoforged.neoforge.fluids.FluidStack.STREAM_CODEC.decode(buffer)) : java.util.Optional.empty())).toList();
                var recipe = new ProcessingLineRecipe(input, steps, ItemStack.STREAM_CODEC.decode(buffer));
                return validate(recipe).getOrThrow();
            });
        @Override public MapCodec<ProcessingLineRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ProcessingLineRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
