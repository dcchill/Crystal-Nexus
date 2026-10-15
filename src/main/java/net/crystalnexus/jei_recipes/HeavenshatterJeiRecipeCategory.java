package net.crystalnexus.jei_recipes;

import mezz.jei.api.constants.VanillaTypes;

import static net.crystalnexus.jei_recipes.OperatingGuideLayout.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModJeiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class HeavenshatterJeiRecipeCategory implements IRecipeCategory<HeavenshatterJeiRecipeCategory.Guide> {
    public static final ResourceLocation UID = ResourceLocation.parse("crystalnexus:heavenshatter_beam");
    public enum Guide { INSTANCE }
    private final IDrawable icon;
    private final IDrawable slot;

    public HeavenshatterJeiRecipeCategory(IGuiHelper helper) {
        slot = helper.getSlotDrawable();
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
            new ItemStack(CrystalnexusModItems.HEAVENSHATTER_BEAM_CONTROLLER.get()));
    }

    @Override public mezz.jei.api.recipe.RecipeType<Guide> getRecipeType() { return CrystalnexusModJeiPlugin.Heavenshatter_Type; }
    @Override public Component getTitle() { return Component.translatable("block.crystalnexus.heavenshatter_beam_controller"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 240; }
    @Override public int getHeight() { return 250; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Guide guide, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 58, 20).setBackground(slot, -1, -1)
            .addItemStack(new ItemStack(CrystalnexusModItems.HEAVENSHATTER_BEAM_CONTROLLER.get()));
        // These illustrate a placed meteor, not guaranteed item outputs for automated crafting.
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 122, 20).setBackground(slot, -1, -1).addItemStack(new ItemStack(Items.STONE));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 144, 20).setBackground(slot, -1, -1).addItemStack(new ItemStack(Items.IRON_ORE));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 166, 20).setBackground(slot, -1, -1).addItemStack(new ItemStack(Items.DIAMOND_ORE));
        builder.addSlot(RecipeIngredientRole.CATALYST, 10, 173).setBackground(slot, -1, -1)
            .addItemStack(new ItemStack(CrystalnexusModItems.METEOR_DATA_DISK.get()));
    }

    @Override public void draw(Guide guide, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text("summary"), (getWidth() - font.width(text("summary"))) / 2, 5, 0x303030, false);
        graphics.fill(86, 27, 110, 29, 0xff606060);
        graphics.fill(106, 25, 108, 31, 0xff606060);
        graphics.fill(104, 23, 106, 33, 0xff606060);

        step(graphics, 42, "1", text("build"), text("structure"));
        step(graphics, 82, "2", text("locate"), text("operation"));
        step(graphics, 122, "3", text("pull"), text("mining"));
        panel(graphics, 4, 162, 232, 38, 0xffdedede);
        graphics.drawString(font, text("disk"), 34, 167, 0x303030, false);
        graphics.drawWordWrap(font, text("filter"), 34, 179, 196, 0x404040);

        graphics.drawWordWrap(font, text("clearance"), 8, 204, 224, 0x404040);
        graphics.drawString(font, text("warning"), 8, 225, 0x8b2920, false);
        detailLabel(graphics, 4, "setup", text("setup_label"), mouseX, mouseY);
        detailLabel(graphics, 124, "power", text("power_label"), mouseX, mouseY);
    }

    @Override public void getTooltip(ITooltipBuilder builder, Guide guide, IRecipeSlotsView slots, double mouseX, double mouseY) {
        String detail = detailAt(mouseX, mouseY);
        if (detail != null) builder.add(text(detail + "_details"));
    }

    private static Component text(String key) { return Component.translatable("jei.crystalnexus.heavenshatter." + key); }
}
