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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class RefiningRecipe implements CrystalNexusRecipe {
    private final FluidChemicalReactionRecipe.FluidAmount input;
    private final Optional<ItemStack> output;
    private final Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedOutput;
    private final int minimumMachineTier;

    public RefiningRecipe(FluidChemicalReactionRecipe.FluidAmount input, Optional<ItemStack> output,
                          Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedOutput) {
		this(input, output, taggedOutput, 1);
	}

	public RefiningRecipe(FluidChemicalReactionRecipe.FluidAmount input, Optional<ItemStack> output,
						  Optional<FluidChemicalReactionRecipe.TaggedItemOutput> taggedOutput, int minimumMachineTier) {
        this.input = input;
        this.output = output.map(ItemStack::copy);
        this.taggedOutput = taggedOutput;
		this.minimumMachineTier = Math.max(1, Math.min(4, minimumMachineTier));
    }

    public FluidChemicalReactionRecipe.FluidAmount input() { return input; }
    public int minimumMachineTier() { return minimumMachineTier; }
    public ItemStack output() {
        return output.map(ItemStack::copy)
            .or(() -> taggedOutput.map(FluidChemicalReactionRecipe.TaggedItemOutput::stack))
            .orElse(ItemStack.EMPTY);
    }

    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) { return output(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return output(); }
    @Override public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() { return NonNullList.create(); }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<RefiningRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<RefiningRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<RefiningRecipe> CODEC = RecordCodecBuilder.<RefiningRecipe>mapCodec(instance -> instance.group(
            FluidChemicalReactionRecipe.FluidAmount.CODEC.fieldOf("fluid_input").forGetter(recipe -> recipe.input),
            ItemStack.STRICT_CODEC.optionalFieldOf("item_output").forGetter(recipe -> recipe.output),
            FluidChemicalReactionRecipe.TaggedItemOutput.CODEC.optionalFieldOf("item_output_tag").forGetter(recipe -> recipe.taggedOutput),
			Codec.INT.optionalFieldOf("minimum_machine_tier", 1).forGetter(recipe -> recipe.minimumMachineTier)
        ).apply(instance, RefiningRecipe::new)).flatXmap(recipe -> recipe.output.isEmpty() && recipe.taggedOutput.isEmpty()
            ? DataResult.error(() -> "Refining recipe requires item_output or item_output_tag")
            : DataResult.success(recipe), DataResult::success);
        public static final StreamCodec<RegistryFriendlyByteBuf, RefiningRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                buffer.writeResourceLocation(recipe.input.fluid());
                buffer.writeVarInt(recipe.input.amount());
                buffer.writeBoolean(recipe.input.material().isPresent());
                recipe.input.material().ifPresent(buffer::writeResourceLocation);
                buffer.writeBoolean(recipe.input.tag());
                buffer.writeBoolean(recipe.output.isPresent());
                recipe.output.ifPresent(stack -> ItemStack.STREAM_CODEC.encode(buffer, stack));
                buffer.writeBoolean(recipe.taggedOutput.isPresent());
                recipe.taggedOutput.ifPresent(output -> {
                    buffer.writeResourceLocation(output.tag());
                    buffer.writeVarInt(output.count());
                });
				buffer.writeVarInt(recipe.minimumMachineTier);
            }, buffer -> {
                var fluid = buffer.readResourceLocation();
                int amount = buffer.readVarInt();
                Optional<net.minecraft.resources.ResourceLocation> material = buffer.readBoolean()
                    ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
                boolean tag = buffer.readBoolean();
                Optional<ItemStack> output = buffer.readBoolean()
                    ? Optional.of(ItemStack.STREAM_CODEC.decode(buffer)) : Optional.empty();
                Optional<FluidChemicalReactionRecipe.TaggedItemOutput> tagged = buffer.readBoolean()
                    ? Optional.of(new FluidChemicalReactionRecipe.TaggedItemOutput(
                        buffer.readResourceLocation(), buffer.readVarInt())) : Optional.empty();
                return new RefiningRecipe(new FluidChemicalReactionRecipe.FluidAmount(fluid, amount, material, tag),
                    output, tagged, buffer.readVarInt());
            });

        @Override public MapCodec<RefiningRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RefiningRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
