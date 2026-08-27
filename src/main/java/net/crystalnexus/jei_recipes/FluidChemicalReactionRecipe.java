package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidStack;
import net.crystalnexus.init.CrystalnexusModDataComponents;
import com.mojang.datafixers.util.Either;

import java.util.Optional;

public class FluidChemicalReactionRecipe implements CrystalNexusRecipe {
    public record FluidAmount(ResourceLocation fluid, int amount, Optional<ResourceLocation> material, boolean tag) {
        private static final Codec<FluidAmount> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidAmount::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidAmount::amount),
            ResourceLocation.CODEC.optionalFieldOf("material").forGetter(FluidAmount::material)
        ).apply(instance, (fluid, amount, material) -> new FluidAmount(fluid, amount, material, false)));
        private static final Codec<FluidAmount> TAG_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("tag").forGetter(FluidAmount::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidAmount::amount),
            ResourceLocation.CODEC.optionalFieldOf("material").forGetter(FluidAmount::material)
        ).apply(instance, (fluid, amount, material) -> new FluidAmount(fluid, amount, material, true)));
        static final Codec<FluidAmount> CODEC = Codec.either(DIRECT_CODEC, TAG_CODEC).xmap(
            value -> value.map(java.util.function.Function.identity(), java.util.function.Function.identity()),
            value -> value.tag ? Either.right(value) : Either.left(value));

        public FluidAmount(ResourceLocation fluid, int amount) { this(fluid, amount, Optional.empty(), false); }
        public FluidAmount(ResourceLocation fluid, int amount, Optional<ResourceLocation> material) {
            this(fluid, amount, material, false);
        }

        public FluidStack stack() {
            Fluid value = tag ? BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, fluid))
                .stream().flatMap(set -> set.stream())
                .sorted(java.util.Comparator.comparing(holder -> BuiltInRegistries.FLUID.getKey(holder.value()).toString()))
                .map(net.minecraft.core.Holder::value).findFirst().orElse(net.minecraft.world.level.material.Fluids.EMPTY)
                : BuiltInRegistries.FLUID.get(fluid);
            FluidStack stack = new FluidStack(value, amount);
            material.ifPresent(materialId -> stack.set(CrystalnexusModDataComponents.MATERIAL.get(), materialId));
            return stack;
        }

        public boolean matches(FluidStack stack) {
            if (stack.getAmount() < amount) return false;
            boolean fluidMatches = tag ? stack.is(TagKey.create(Registries.FLUID, fluid))
                : stack.is(BuiltInRegistries.FLUID.get(fluid));
            return fluidMatches && (material.isEmpty() || material.equals(Optional.ofNullable(
                stack.get(CrystalnexusModDataComponents.MATERIAL.get()))));
        }
    }

    public record TaggedItemOutput(ResourceLocation tag, int count) {
        static final Codec<TaggedItemOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("tag").forGetter(TaggedItemOutput::tag),
            Codec.INT.fieldOf("count").forGetter(TaggedItemOutput::count)
        ).apply(instance, TaggedItemOutput::new));

        public ItemStack stack() {
            return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                .flatMap(items -> items.stream().sorted(java.util.Comparator.comparing(
                    item -> BuiltInRegistries.ITEM.getKey(item.value()).toString())).findFirst())
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
        }
    }

    private final Optional<FluidAmount> fluidInput1;
    private final Optional<FluidAmount> fluidInput2;
    private final Optional<Ingredient> itemInput1;
    private final Optional<Ingredient> itemInput2;
    private final Optional<FluidAmount> fluidOutput;
    private final Optional<ItemStack> itemOutput;
    private final Optional<TaggedItemOutput> taggedItemOutput;
    private final int itemInput1Count;
    private final int itemInput2Count;

    public FluidChemicalReactionRecipe(Optional<FluidAmount> fluidInput1, Optional<FluidAmount> fluidInput2,
                                       Optional<Ingredient> itemInput1, Optional<Ingredient> itemInput2,
                                       Optional<FluidAmount> fluidOutput, Optional<ItemStack> itemOutput,
                                       Optional<TaggedItemOutput> taggedItemOutput) {
        this(fluidInput1, fluidInput2, itemInput1, itemInput2, fluidOutput, itemOutput, taggedItemOutput, 1, 1);
    }

    public FluidChemicalReactionRecipe(Optional<FluidAmount> fluidInput1, Optional<FluidAmount> fluidInput2,
                                       Optional<Ingredient> itemInput1, Optional<Ingredient> itemInput2,
                                       Optional<FluidAmount> fluidOutput, Optional<ItemStack> itemOutput,
                                       Optional<TaggedItemOutput> taggedItemOutput, int itemInput1Count, int itemInput2Count) {
        this.fluidInput1 = fluidInput1;
        this.fluidInput2 = fluidInput2;
        this.itemInput1 = itemInput1;
        this.itemInput2 = itemInput2;
        this.fluidOutput = fluidOutput;
        this.itemOutput = itemOutput.map(ItemStack::copy);
        this.taggedItemOutput = taggedItemOutput;
        this.itemInput1Count = Math.max(1, itemInput1Count);
        this.itemInput2Count = Math.max(1, itemInput2Count);
    }

    public Optional<FluidAmount> fluidInput(int tank) { return tank == 0 ? fluidInput1 : fluidInput2; }
    public Optional<Ingredient> itemInput(int slot) { return slot == 0 ? itemInput1 : itemInput2; }
    public int itemInputCount(int slot) { return slot == 0 ? itemInput1Count : itemInput2Count; }
    public Optional<FluidAmount> fluidOutput() { return fluidOutput; }
    public Optional<ItemStack> itemOutput() {
        return itemOutput.map(ItemStack::copy)
            .or(() -> taggedItemOutput.map(TaggedItemOutput::stack).filter(stack -> !stack.isEmpty()));
    }

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
		static final MapCodec<FluidChemicalReactionRecipe> CODEC = RecordCodecBuilder.<FluidChemicalReactionRecipe>mapCodec(instance -> instance.group(
            FluidAmount.CODEC.optionalFieldOf("fluid_input_1").forGetter(recipe -> recipe.fluidInput1),
            FluidAmount.CODEC.optionalFieldOf("fluid_input_2").forGetter(recipe -> recipe.fluidInput2),
            Ingredient.CODEC.optionalFieldOf("item_input_1").forGetter(recipe -> recipe.itemInput1),
            Ingredient.CODEC.optionalFieldOf("item_input_2").forGetter(recipe -> recipe.itemInput2),
            FluidAmount.CODEC.optionalFieldOf("output").forGetter(recipe -> recipe.fluidOutput),
            ItemStack.STRICT_CODEC.optionalFieldOf("item_output").forGetter(recipe -> recipe.itemOutput),
            TaggedItemOutput.CODEC.optionalFieldOf("item_output_tag").forGetter(recipe -> recipe.taggedItemOutput),
            Codec.INT.optionalFieldOf("item_input_1_count", 1).forGetter(recipe -> recipe.itemInput1Count),
            Codec.INT.optionalFieldOf("item_input_2_count", 1).forGetter(recipe -> recipe.itemInput2Count)
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
            buffer.writeBoolean(recipe.taggedItemOutput.isPresent());
            recipe.taggedItemOutput.ifPresent(output -> {
                buffer.writeResourceLocation(output.tag());
                buffer.writeVarInt(output.count());
            });
            buffer.writeVarInt(recipe.itemInput1Count);
            buffer.writeVarInt(recipe.itemInput2Count);
        }

        private static FluidChemicalReactionRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new FluidChemicalReactionRecipe(readFluid(buffer), readFluid(buffer), readIngredient(buffer),
                readIngredient(buffer), readFluid(buffer),
                buffer.readBoolean() ? Optional.of(ItemStack.STREAM_CODEC.decode(buffer)) : Optional.empty(),
                buffer.readBoolean() ? Optional.of(new TaggedItemOutput(buffer.readResourceLocation(), buffer.readVarInt())) : Optional.empty(),
                buffer.readVarInt(), buffer.readVarInt());
        }

        private static DataResult<FluidChemicalReactionRecipe> validate(FluidChemicalReactionRecipe recipe) {
            return recipe.fluidOutput.isEmpty() && recipe.itemOutput.isEmpty() && recipe.taggedItemOutput.isEmpty()
                ? DataResult.error(() -> "Fluid chemical reaction recipes require output, item_output, or item_output_tag")
                : DataResult.success(recipe);
        }

        private static void writeFluid(RegistryFriendlyByteBuf buffer, Optional<FluidAmount> value) {
            buffer.writeBoolean(value.isPresent());
            value.ifPresent(fluid -> {
                buffer.writeResourceLocation(fluid.fluid());
                buffer.writeVarInt(fluid.amount());
                buffer.writeBoolean(fluid.material().isPresent());
                fluid.material().ifPresent(buffer::writeResourceLocation);
                buffer.writeBoolean(fluid.tag());
            });
        }
        private static Optional<FluidAmount> readFluid(RegistryFriendlyByteBuf buffer) {
            if (!buffer.readBoolean()) return Optional.empty();
            ResourceLocation fluid = buffer.readResourceLocation();
            int amount = buffer.readVarInt();
            Optional<ResourceLocation> material = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
            return Optional.of(new FluidAmount(fluid, amount, material, buffer.readBoolean()));
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
