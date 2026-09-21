package net.crystalnexus.jei_recipes;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/** A display-only Solar Simulator operation. */
public record SolarSimulatorJeiRecipe(ItemStack planet, List<ItemStack> materialOutputs,
                                      Optional<FluidStack> fluidOutput) {
    public List<ItemStack> planetInput() { return List.of(planet.copy()); }
}
