package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PartsAssemblingRecipeCategory implements IRecipeCategory<PartsAssemblingRecipe> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:parts_assembling");
    public enum Kind { ASSEMBLER, LATHE, PRESS }
    private final Kind kind;
    private final boolean lathe;
    private final IDrawable background;
    private final IDrawable icon;

    public PartsAssemblingRecipeCategory(IGuiHelper helper) {
        this(helper, Kind.ASSEMBLER);
    }

    public PartsAssemblingRecipeCategory(IGuiHelper helper, Kind kind) {
        this.kind = kind;
        this.lathe = kind == Kind.LATHE;
        background = helper.createDrawable(ResourceLocation.parse("crystalnexus:textures/screens/iron_smelter_jei.png"), 0, 0, 176, 80);
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(switch (kind) {
            case ASSEMBLER -> CrystalnexusModBlocks.PARTS_ASSEMBLER.get();
            case LATHE -> CrystalnexusModBlocks.AZURINE_LATHE.get();
            case PRESS -> CrystalnexusModBlocks.AZURINE_PRESS.get();
        }));
    }

    @Override public mezz.jei.api.recipe.RecipeType<PartsAssemblingRecipe> getRecipeType() { return switch (kind) {
        case ASSEMBLER -> CrystalnexusModJeiPlugin.PartsAssembling_Type;
        case LATHE -> CrystalnexusModJeiPlugin.AzurineLathing_Type;
        case PRESS -> CrystalnexusModJeiPlugin.PlatePressing_Type;
    }; }
    @Override public Component getTitle() { return Component.translatable(switch (kind) {
        case ASSEMBLER -> "jei.crystalnexus.parts_assembling";
        case LATHE -> "jei.crystalnexus.lathing";
        case PRESS -> "jei.crystalnexus.plate_pressing";
    }); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return background.getWidth(); }
    @Override public int getHeight() { return background.getHeight(); }

    @Override
    public void draw(PartsAssemblingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        if (lathe) graphics.drawString(Minecraft.getInstance().font, "33%", 138, 24, 0xff404040, false);
        graphics.drawString(Minecraft.getInstance().font,
            Component.translatable("gui.crystalnexus.parts_assembler.mode." + recipe.mode().serializedName()),
            75, 58, 0xff404040, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PartsAssemblingRecipe recipe, IFocusGroup focuses) {
        var inputs = java.util.Arrays.stream(recipe.ingredient().getItems())
            .filter(stack -> !lathe || !net.crystalnexus.processing.PartsAssemblingDefaults.associatedDust(stack).isEmpty())
            .filter(stack -> !lathe || recipe.mode() != PartsAssemblingRecipe.Mode.BOLT
                || net.crystalnexus.processing.PartsAssemblingDefaults.isRod(stack)).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 36).addItemStacks(inputs);
        if (lathe) builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 36).addItemStacks(inputs.stream()
            .map(net.crystalnexus.processing.PartsAssemblingDefaults::associatedDust).toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 36).addItemStack(recipe.getResultItem(null));
    }
}
