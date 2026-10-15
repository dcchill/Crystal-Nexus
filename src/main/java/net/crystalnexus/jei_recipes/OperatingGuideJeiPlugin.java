package net.crystalnexus.jei_recipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class OperatingGuideJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("crystalnexus", "operating_guides");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        for (var machine : OperatingGuideCategory.Machine.values()) {
            registration.addRecipeCategories(new OperatingGuideCategory(registration.getJeiHelpers().getGuiHelper(), machine));
        }
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        for (var machine : OperatingGuideCategory.Machine.values()) registration.addRecipes(machine.type, machine.pages);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var machine : OperatingGuideCategory.Machine.values()) {
            registration.addRecipeCatalyst(new ItemStack(machine.controller.get()), machine.type);
        }
    }
}
