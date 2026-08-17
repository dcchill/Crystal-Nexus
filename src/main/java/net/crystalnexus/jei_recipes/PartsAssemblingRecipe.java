package net.crystalnexus.jei_recipes;

import com.mojang.serialization.Codec;
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

public final class PartsAssemblingRecipe implements CrystalNexusRecipe {
    public enum Mode {
        PLATE("plate"), ROD("rod"), BOLT("bolt");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String serializedName() {
            return name;
        }

        public static Mode fromName(String name) {
            for (Mode mode : values()) {
                if (mode.name.equals(name)) return mode;
            }
            return PLATE;
        }

        public static Mode fromId(int id) {
            return values()[Math.max(0, Math.min(values().length - 1, id))];
        }
    }

    private final Ingredient ingredient;
    private final ItemStack result;
    private final Mode mode;
    private final int processingTime;
    private final int energyPerTick;

    public PartsAssemblingRecipe(Ingredient ingredient, ItemStack result, String mode, int processingTime, int energyPerTick) {
        this(ingredient, result, Mode.fromName(mode), processingTime, energyPerTick);
    }

    public PartsAssemblingRecipe(Ingredient ingredient, ItemStack result, Mode mode, int processingTime, int energyPerTick) {
        this.ingredient = ingredient;
        this.result = result.copy();
        this.mode = mode;
        this.processingTime = Math.max(1, processingTime);
        this.energyPerTick = Math.max(1, energyPerTick);
    }

    public Ingredient ingredient() { return ingredient; }
    public Mode mode() { return mode; }
    public int processingTime() { return processingTime; }
    public int energyPerTick() { return energyPerTick; }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return input.size() > 0 && ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    public static final class Type implements RecipeType<PartsAssemblingRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }

    public static final class Serializer implements RecipeSerializer<PartsAssemblingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<PartsAssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Codec.STRING.fieldOf("mode").forGetter(recipe -> recipe.mode.serializedName()),
            Codec.INT.optionalFieldOf("processing_time", 100).forGetter(recipe -> recipe.processingTime),
            Codec.INT.optionalFieldOf("energy_per_tick", 10).forGetter(recipe -> recipe.energyPerTick)
        ).apply(instance, PartsAssemblingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PartsAssemblingRecipe> STREAM_CODEC = StreamCodec.of(
            (buffer, recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                buffer.writeEnum(recipe.mode);
                buffer.writeVarInt(recipe.processingTime);
                buffer.writeVarInt(recipe.energyPerTick);
            },
            buffer -> new PartsAssemblingRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                ItemStack.STREAM_CODEC.decode(buffer),
                buffer.readEnum(Mode.class),
                buffer.readVarInt(),
                buffer.readVarInt())
        );

        @Override public MapCodec<PartsAssemblingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PartsAssemblingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
