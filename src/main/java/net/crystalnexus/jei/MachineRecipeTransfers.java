package net.crystalnexus.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static net.crystalnexus.init.CrystalnexusModJeiPlugin.*;

/** Uses JEI's inventory checks and server transfer packet; only item inputs are moved. */
public final class MachineRecipeTransfers {
    private MachineRecipeTransfers() {}

    public static void register(IRecipeTransferRegistration registration) {
        add(registration, CrystalPurifierGUIMenu.class, CrystalnexusModMenus.CRYSTAL_PURIFIER_GUI.get(), Purification_Type, 0, 2);
        add(registration, CrusherGuiMenu.class, CrystalnexusModMenus.CRUSHER_GUI.get(), OreCrushingJei_Type, 0);
        add(registration, ExtractinatorGuiMenu.class, CrystalnexusModMenus.EXTRACTINATOR_GUI.get(), ExtractinatorJEI_Type, 0);
        add(registration, MasticatorMenu.class, CrystalnexusModMenus.MASTICATOR.get(), GeneSplicing_Type, 0);
        add(registration, HemolyzerMenu.class, CrystalnexusModMenus.HEMOLYZER.get(), Hemolyzer_Type, 0);
        add(registration, PartsAssemblerMenu.class, CrystalnexusModMenus.PARTS_ASSEMBLER.get(), PartsAssembling_Type, 0);
        add(registration, SeparatorGuiMenu.class, CrystalnexusModMenus.SEPARATOR_GUI.get(), DustSeperation_Type, 0);
        add(registration, CircuitPressGUIMenu.class, CrystalnexusModMenus.CIRCUIT_PRESS_GUI.get(), CircuitPressing_Type, 0, 2);
        add(registration, CircuitPressGUIMenu.class, CrystalnexusModMenus.CIRCUIT_PRESS_GUI.get(), TitaniumCarbideCircuitPress_Type,
            recipe -> IntStream.range(0, 2).filter(i -> recipe.itemInput(i).isPresent()).map(i -> i * 2).toArray());
        add(registration, ChemicalReactionChamberGUIMenu.class, CrystalnexusModMenus.CHEMICAL_REACTION_CHAMBER_GUI.get(), ChemicalReaction_Type, 0, 1, 2);
        add(registration, FluidChemicalReactionChamberGUIMenu.class, CrystalnexusModMenus.FLUID_CHEMICAL_REACTION_CHAMBER_GUI.get(), FluidChemicalReaction_Type,
            recipe -> IntStream.range(0, 2).filter(i -> recipe.itemInput(i).isPresent()).toArray());
        add(registration, CryogenicFlashFreezerMenu.class, CrystalnexusModMenus.CRYOGENIC_FLASH_FREEZER.get(), CryogenicFlashFreezer_Type, 0);
        add(registration, RefineryMenu.class, CrystalnexusModMenus.REFINERY_GUI.get(), Refining_Type, 0);
        add(registration, InverterGuiMenu.class, CrystalnexusModMenus.INVERTER_GUI.get(), InverterJei_Type, 0);
        add(registration, EnergyExtractorGUIMenu.class, CrystalnexusModMenus.ENERGY_EXTRACTOR_GUI.get(), EnergyExtraction_Type, 0);
        add(registration, BioMGuiMenu.class, CrystalnexusModMenus.BIO_M_GUI.get(), BiomaticComposting_Type, 0);
        add(registration, BioSIMGuiMenu.class, CrystalnexusModMenus.BIO_SIM_GUI.get(), BiomaticSimulation_Type, 0);
        add(registration, PistonGenGUIMenu.class, CrystalnexusModMenus.PISTON_GEN_GUI.get(), PistonGeneratorJEI_Type, 0);
        add(registration, ReactionGUIMenu.class, CrystalnexusModMenus.REACTION_GUI.get(), BeamReactionRecipe_Type, 0);
        add(registration, SingularityCompressorGUIMenu.class, CrystalnexusModMenus.SINGULARITY_COMPRESSOR_GUI.get(), SingularityCompression_Type, 0);
        add(registration, MatterTransmutationGUIMenu.class, CrystalnexusModMenus.MATTER_TRANSMUTATION_GUI.get(), MatterTransmutation_Type, 0, 1, 2, 3, 4, 5, 6, 7);
        add(registration, ArcFurnaceMenu.class, CrystalnexusModMenus.ARC_FURNACE.get(), ArcFurnace_Type, 0, 1);
        add(registration, AcceleratorGuiMenu.class, CrystalnexusModMenus.ACCELERATOR_GUI.get(), AcceleratorJei_Type, 7, 5, 6, 0);
        add(registration, CometForgeMenu.class, CrystalnexusModMenus.COMET_FORGE.get(), CometForge_Type, 0, 1, 2, 3);
        add(registration, GravitationalArrayMenu.class, CrystalnexusModMenus.GRAVITATIONAL_ARRAY.get(), GravitationalArray_Type, 0, 1, 2, 3);
        add(registration, SolarSimulatorMenu.class, CrystalnexusModMenus.SOLAR_SIMULATOR.get(), SolarSimulator_Type, 0, 4);
        add(registration, CelestialGearForgeMenu.class, CrystalnexusModMenus.CELESTIAL_GEAR_FORGE.get(), CelestialGearForge_Type,
            IntStream.range(0, net.crystalnexus.recipe.CelestialGearForgeRecipe.INPUT_COUNT).toArray());
        add(registration, CelestialGearForgeMenu.class, CrystalnexusModMenus.CELESTIAL_GEAR_FORGE.get(), CelestialGearForgeEnchanting_Type,
            recipe -> IntStream.range(0, net.crystalnexus.recipe.CelestialGearForgeRecipe.STAR_SLOT)
                .filter(i -> !recipe.inputs().get(i).isEmpty()).toArray());
        add(registration, IronSmelterGuiMenu.class, CrystalnexusModMenus.IRON_SMELTER_GUI.get(), mezz.jei.api.constants.RecipeTypes.SMELTING, 0);
        add(registration, UltimaSmelterGuiMenu.class, CrystalnexusModMenus.ULTIMA_SMELTER_GUI.get(), mezz.jei.api.constants.RecipeTypes.SMELTING, 0);
    }

    private static <C extends AbstractContainerMenu, R> void add(IRecipeTransferRegistration registration,
            Class<C> menuClass, MenuType<C> menuType, RecipeType<R> recipeType, int... inputs) {
        add(registration, menuClass, menuType, recipeType, recipe -> inputs);
    }

    private static <C extends AbstractContainerMenu, R> void add(IRecipeTransferRegistration registration,
            Class<C> menuClass, MenuType<C> menuType, RecipeType<R> recipeType, Function<R, int[]> inputs) {
        var info = new IRecipeTransferInfo<C, R>() {
            @Override public Class<C> getContainerClass() { return menuClass; }
            @Override public Optional<MenuType<C>> getMenuType() { return Optional.of(menuType); }
            @Override public RecipeType<R> getRecipeType() { return recipeType; }
            @Override public boolean canHandle(C menu, R recipe) { return true; }
            @Override public List<Slot> getRecipeSlots(C menu, R recipe) {
                return Arrays.stream(inputs.apply(recipe)).mapToObj(menu.slots::get).toList();
            }
            @Override public List<Slot> getInventorySlots(C menu, R recipe) {
                return menu.slots.subList(menu.slots.size() - 36, menu.slots.size());
            }
        };
        var helper = registration.getTransferHelper();
        var delegate = helper.createUnregisteredRecipeTransferHandler(info);
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<C, R>() {
            @Override public Class<C> getContainerClass() { return menuClass; }
            @Override public Optional<MenuType<C>> getMenuType() { return Optional.of(menuType); }
            @Override public RecipeType<R> getRecipeType() { return recipeType; }
            @Override public IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots,
                    Player player, boolean maxTransfer, boolean doTransfer) {
                var itemSlots = slots.getSlotViews().stream().filter(slot ->
                    slot.getRole() != RecipeIngredientRole.INPUT || slot.getIngredients(NeoForgeTypes.FLUID_STACK).findAny().isEmpty()).toList();
                if (itemSlots.stream().noneMatch(slot -> slot.getRole() == RecipeIngredientRole.INPUT
                        && slot.getItemStacks().anyMatch(stack -> !stack.isEmpty()))) return helper.createInternalError();
                var error = delegate.transferRecipe(menu, recipe, helper.createRecipeSlotsView(itemSlots), player, maxTransfer, doTransfer);
                if (error == null && doTransfer && recipe instanceof net.crystalnexus.jei_recipes.PartsAssemblingRecipe assembling)
                    net.minecraft.client.Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, assembling.mode().ordinal());
                return error;
            }
        }, recipeType);
    }
}
