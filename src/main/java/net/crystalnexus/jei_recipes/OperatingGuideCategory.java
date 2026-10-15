package net.crystalnexus.jei_recipes;

import java.util.List;
import java.util.function.Supplier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.block.entity.CometForgeControllerBlockEntity;
import net.crystalnexus.block.entity.SolarSimulatorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.recipe.DysonOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import static net.crystalnexus.jei_recipes.OperatingGuideLayout.*;

public final class OperatingGuideCategory implements IRecipeCategory<OperatingGuideCategory.Page> {
    public enum Page {
        COMET("comet", () -> CrystalnexusModItems.DIAMOND_SINGULARITY.get(), () -> CrystalnexusModItems.IRON_SINGULARITY.get(), () -> CrystalnexusModItems.RESOURCE_COMET.get()),
        SOLAR_RESOURCES("solar_resources", () -> CrystalnexusModItems.YELLOW_DWARF_STAR.get(), () -> CrystalnexusModItems.TERRA.get(), () -> CrystalnexusModItems.RESOURCE_COMET.get()),
        SOLAR_DYSON("solar_dyson", () -> CrystalnexusModItems.DYSON_STRUCTURE.get(), () -> CrystalnexusModItems.CARBON_SOLAR_SHEET.get(), () -> CrystalnexusModItems.DYSON_REPAIR.get()),
        ASSEMBLY("assembly", () -> CrystalnexusModItems.ASSEMBLY_LINE_CASING.get(), () -> CrystalnexusModItems.MULTIBLOCK_ITEM_INPUT.get(), () -> CrystalnexusModItems.MULTIBLOCK_ITEM_OUTPUT.get()),
        GRAVITY("gravity", () -> CrystalnexusModItems.MACHINE_ENERGY_INPUT.get(), () -> CrystalnexusModItems.MACHINE_FLUID_INPUT.get(), () -> CrystalnexusModItems.YELLOW_DWARF_STAR.get()),
        PLASMA("plasma", () -> CrystalnexusModItems.PLASMA_INJECTOR.get(), () -> CrystalnexusModItems.INDUCTION_COIL.get(), () -> CrystalnexusModItems.FERROSTEEL_HEATSINK.get()),
        GEAR_CRAFTING("gear_crafting", () -> CrystalnexusModItems.YELLOW_DWARF_STAR.get(), () -> Items.DIAMOND, () -> Items.DIAMOND_SWORD),
        GEAR_ENCHANTING("gear_enchanting", () -> Items.DIAMOND_SWORD, () -> Items.ENCHANTED_BOOK, () -> CrystalnexusModItems.YELLOW_DWARF_STAR.get());

        final String key;
        final List<Supplier<? extends ItemLike>> examples;

        @SafeVarargs Page(String key, Supplier<? extends ItemLike>... examples) {
            this.key = key;
            this.examples = List.of(examples);
        }

        Component text(String suffix, Object... args) {
            return Component.translatable("jei.crystalnexus.guide." + key + "." + suffix, args);
        }

        Component operationDetails() {
            return switch (this) {
                case COMET -> text("operation_details", CometForgeControllerBlockEntity.ENERGY,
                    CometForgeControllerBlockEntity.FLUID, CometForgeControllerBlockEntity.DURATION / 20);
                case SOLAR_RESOURCES -> text("operation_details", SolarSimulatorControllerBlockEntity.DURATION);
                case SOLAR_DYSON -> text("operation_details", DysonOutput.CARBON_SHEETS_PER_STRUCTURE,
                    DysonOutput.SOLAR_SHEETS_PER_STRUCTURE, DysonOutput.MAX_INTEGRITY,
                    SolarSimulatorControllerBlockEntity.DYSON_REPAIR_PER_TICK);
                case GEAR_CRAFTING -> text("operation_details", CelestialGearForgeBlockEntity.CRAFT_ENERGY_PER_TICK,
                    CelestialGearForgeBlockEntity.DURATION / 20);
                case GEAR_ENCHANTING -> text("operation_details", CelestialGearForgeBlockEntity.DURATION / 20);
                default -> text("operation_details");
            };
        }
    }

    public enum Machine {
        COMET("comet_forge", () -> CrystalnexusModItems.COMET_FORGE_CONTROLLER.get(), Page.COMET),
        SOLAR("solar_simulator", () -> CrystalnexusModItems.SOLAR_SIMULATOR_CONTROLLER.get(), Page.SOLAR_RESOURCES, Page.SOLAR_DYSON),
        ASSEMBLY("assembly_line", () -> CrystalnexusModItems.ASSEMBLY_LINE_CONTROLLER.get(), Page.ASSEMBLY),
        GRAVITY("gravitational_array", () -> CrystalnexusModItems.GRAVITATIONAL_ARRAY_CONTROLLER.get(), Page.GRAVITY),
        PLASMA("plasma_generator", () -> CrystalnexusModItems.PLASMA_GENERATOR_CONTROLLER.get(), Page.PLASMA),
        GEAR("celestial_gear_forge", () -> CrystalnexusModItems.CELESTIAL_GEAR_FORGE.get(), Page.GEAR_CRAFTING, Page.GEAR_ENCHANTING);

        public final String id;
        public final Supplier<? extends ItemLike> controller;
        public final List<Page> pages;
        public final RecipeType<Page> type;

        Machine(String id, Supplier<? extends ItemLike> controller, Page... pages) {
            this.id = id;
            this.controller = controller;
            this.pages = List.of(pages);
            this.type = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("crystalnexus", id + "_guide"), Page.class);
        }
    }

    private final Machine machine;
    private final IDrawable icon;
    private final IDrawable slot;

    public OperatingGuideCategory(IGuiHelper helper, Machine machine) {
        this.machine = machine;
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(machine.controller.get()));
        slot = helper.getSlotDrawable();
    }

    @Override public RecipeType<Page> getRecipeType() { return machine.type; }
    @Override public Component getTitle() { return Component.translatable("jei.crystalnexus.guide." + machine.id + ".title"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 240; }
    @Override public int getHeight() { return 250; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 58, 20).setBackground(slot, -1, -1)
            .addItemStack(new ItemStack(machine.controller.get()));
        // Guide icons illustrate equipment, not a transferable recipe or guaranteed outputs.
        for (int i = 0; i < page.examples.size(); i++) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 122 + i * 22, 20).setBackground(slot, -1, -1)
                .addItemStack(new ItemStack(page.examples.get(i).get()));
        }
    }

    @Override public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, page.text("summary"), (getWidth() - font.width(page.text("summary"))) / 2, 5, 0x303030, false);
        for (int i = 1; i <= 3; i++) {
            step(graphics, 42 + (i - 1) * 40, Integer.toString(i), page.text("step" + i + "_title"), page.text("step" + i));
        }
        panel(graphics, 4, 162, 232, 60, 0xffdedede);
        graphics.drawString(font, page.text("tip_title"), 10, 168, 0x303030, false);
        graphics.drawWordWrap(font, page.text("tip"), 10, 181, 220, page == Page.PLASMA ? 0x8b2920 : 0x404040);
        detailLabel(graphics, 4, "setup", Component.translatable("jei.crystalnexus.guide.setup_label"), mouseX, mouseY);
        detailLabel(graphics, 124, "power", Component.translatable("jei.crystalnexus.guide.operation_label"), mouseX, mouseY);
    }

    @Override public void getTooltip(ITooltipBuilder builder, Page page, IRecipeSlotsView slots, double mouseX, double mouseY) {
        String detail = detailAt(mouseX, mouseY);
        if ("setup".equals(detail)) builder.add(page.text("setup_details"));
        else if ("power".equals(detail)) builder.add(page.operationDetails());
    }
}
