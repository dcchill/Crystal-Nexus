package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Display-only continuous generation recipe; the core is not consumed. */
public record NeutronFluxChamberJeiRecipe(ItemStack core, FluidStack flux) {
}