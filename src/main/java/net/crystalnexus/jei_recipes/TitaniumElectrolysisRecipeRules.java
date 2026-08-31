package net.crystalnexus.jei_recipes;

final class TitaniumElectrolysisRecipeRules {
    private TitaniumElectrolysisRecipeRules() {}

    static String validationError(boolean hasFluidInput, boolean hasItemInput, int inputAmount, int outputAmount) {
        if (hasFluidInput == hasItemInput)
            return "Titanium electrolysis recipes require exactly one fluid_input or item_input";
        if (inputAmount < 1 || outputAmount < 1)
            return "Titanium electrolysis recipe amounts must be positive";
        return null;
    }
}
