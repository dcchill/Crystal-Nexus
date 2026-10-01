package net.crystalnexus.jei_recipes;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class CelestialGearForgeEnchantingCategory implements IRecipeCategory<CelestialGearForgeEnchantingCategory.Example> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "celestial_gear_forge_enchanting");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "textures/screens/gear_forge_jei.png");
    private final IDrawable icon;

    public record Example(List<ItemStack> inputs, CelestialGearForgeEnchanting.Operation operation) {}

    public CelestialGearForgeEnchantingCategory(IGuiHelper helper) {
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CrystalnexusModBlocks.CELESTIAL_GEAR_FORGE.get()));
    }

    public static List<Example> examples(RegistryAccess access) {
        var sharpness = access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var examples = new ArrayList<Example>();
        for (var star : List.of(CrystalnexusModItems.YELLOW_DWARF_STAR.get(), CrystalnexusModItems.ORANGE_STAR.get(),
                CrystalnexusModItems.BLUE_STAR.get(), CrystalnexusModItems.PINK_STAR.get(), CrystalnexusModItems.ZERO_STAR.get())) {
            for (int mode = 0; mode < 3; mode++) {
                var inputs = new ArrayList<ItemStack>();
                for (int slot = 0; slot < CelestialGearForgeBlockEntity.INPUT_COUNT; slot++) inputs.add(ItemStack.EMPTY);
                ItemStack target = new ItemStack(mode == 2 ? Items.DIAMOND_SWORD : Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(target, mutable -> mutable.set(sharpness, 5));
                inputs.set(0, target);
                inputs.set(CelestialGearForgeBlockEntity.STAR_SLOT, new ItemStack(star));
                if (mode != 0) {
                    for (int slot = 1; slot <= CelestialGearForgeEnchanting.bookLimit(inputs.get(8)); slot++) {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                        int donorLevel = 4 + slot;
                        EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(sharpness, donorLevel));
                        inputs.set(slot, book);
                    }
                }
                var operation = CelestialGearForgeEnchanting.calculate(inputs, sharpness);
                if (operation != null) examples.add(new Example(List.copyOf(inputs), operation));
            }
        }
        return List.copyOf(examples);
    }

    @Override public mezz.jei.api.recipe.RecipeType<Example> getRecipeType() { return CrystalnexusModJeiPlugin.CelestialGearForgeEnchanting_Type; }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.celestial_gear_forge.enchanting"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 112; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Example example, IFocusGroup focuses) {
        var positions = CelestialGearForgeMenu.SLOT_POSITIONS;
        for (int slot = 0; slot < example.inputs().size(); slot++) {
            if (example.inputs().get(slot).isEmpty()) continue;
            builder.addSlot(slot == CelestialGearForgeBlockEntity.STAR_SLOT ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT,
                positions[slot][0], positions[slot][1]).addItemStack(example.inputs().get(slot));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 37).addItemStack(example.operation().result());
    }

    @Override public void draw(Example example, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, 176, 92, 256, 256);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge.power_rate", example.operation().energyPerTick()), 5, 82, 0xffded5c4, false);
        graphics.drawString(font, Component.translatable("jei.crystalnexus.celestial_gear_forge.cycle"), 5, 94, 0xff808080, false);
        graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge.book_limit", CelestialGearForgeEnchanting.bookLimit(example.inputs().get(8))), 5, 104, 0xff808080, false);
    }

    @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder builder, Example example, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseY >= 80) {
            builder.add(Component.translatable("gui.crystalnexus.celestial_gear_forge.energy_rule"));
            builder.add(Component.translatable("gui.crystalnexus.celestial_gear_forge.energy_total", (long) example.operation().energyPerTick() * CelestialGearForgeBlockEntity.DURATION));
            builder.add(Component.translatable("gui.crystalnexus.celestial_gear_forge.star"));
        }
    }
}
