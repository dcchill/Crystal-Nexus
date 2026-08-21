package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MultiblockStructureRecipeCategory implements IRecipeCategory<MultiblockStructureRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("crystalnexus", "multiblock_structures");
    private static final int PREVIEW_X = 6;
    private static final int PREVIEW_Y = 3;
    private static final int PREVIEW_SIZE = 180;
    private final IDrawable icon;

    public MultiblockStructureRecipeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(CrystalnexusModBlocks.MULTIBLOCK_RESEARCH_STATION.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<MultiblockStructureRecipe> getRecipeType() {
        return CrystalnexusModJeiPlugin.MultiblockStructure_Type;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Multiblock Structures");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 294;
    }

    @Override
    public int getHeight() {
        return 142;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockStructureRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            int x = 145 + (i % 8) * 18;
            int y = 18 + (i / 8) * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStack(recipe.ingredients().get(i));
        }
    }

    @Override
    public void draw(MultiblockStructureRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        recipe.preview().render(guiGraphics, minecraft.font, minecraft.level.registryAccess(), 0, 0, (int) mouseX, (int) mouseY);
        guiGraphics.drawString(minecraft.font, recipe.title(), 145, 5, 0x404040, false);
        recipe.preview().renderHoverTooltip(guiGraphics, minecraft.font, minecraft.level.registryAccess(), 0, 0, (int) mouseX, (int) mouseY);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockStructureRecipe recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new PreviewInput(recipe));
    }

    @Override
    public ResourceLocation getRegistryName(MultiblockStructureRecipe recipe) {
        return recipe.id();
    }

    private record PreviewInput(MultiblockStructureRecipe recipe) implements IJeiGuiEventListener {
        @Override
        public ScreenRectangle getArea() {
            return new ScreenRectangle(PREVIEW_X, PREVIEW_Y, PREVIEW_SIZE, PREVIEW_SIZE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return recipe.preview().mouseClicked(mouseX, mouseY, button, Minecraft.getInstance().level.registryAccess(), -PREVIEW_X, -PREVIEW_Y);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return recipe.preview().mouseDragged(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return recipe.preview().mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return recipe.preview().mouseScrolled(mouseX, mouseY, scrollY, Minecraft.getInstance().level.registryAccess(), -PREVIEW_X, -PREVIEW_Y);
        }
    }
}
