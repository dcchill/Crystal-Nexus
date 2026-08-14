package net.crystalnexus.init;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import net.crystalnexus.jei_recipes.UnfurnaceRecipeCategory;
import net.crystalnexus.jei_recipes.UnfurnaceRecipe;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipeCategory;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipe;
import net.crystalnexus.jei_recipes.ReactorMultiblockGuideRecipeCategory;
import net.crystalnexus.jei_recipes.ReactorMultiblockGuideRecipe;
import net.crystalnexus.jei_recipes.ReactionMultiblockGuideRecipeCategory;
import net.crystalnexus.jei_recipes.ReactionMultiblockGuideRecipe;
import net.crystalnexus.jei_recipes.ReactionJEIRecipeCategory;
import net.crystalnexus.jei_recipes.ReactionJEIRecipe;
import net.crystalnexus.jei_recipes.PurificationRecipeCategory;
import net.crystalnexus.jei_recipes.PurificationRecipe;
import net.crystalnexus.jei_recipes.PistonGeneratorJEIRecipeCategory;
import net.crystalnexus.jei_recipes.PistonGeneratorJEIRecipe;
import net.crystalnexus.jei_recipes.OreCrushingJeiRecipeCategory;
import net.crystalnexus.jei_recipes.OreCrushingJeiRecipe;
import net.crystalnexus.jei_recipes.MatterTransmutationRecipeCategory;
import net.crystalnexus.jei_recipes.MatterTransmutationRecipe;
import net.crystalnexus.jei_recipes.InverterJeiRecipeCategory;
import net.crystalnexus.jei_recipes.InverterJeiRecipe;
import net.crystalnexus.jei_recipes.ExtractinatorJEIRecipeCategory;
import net.crystalnexus.jei_recipes.ExtractinatorJEIRecipe;
import net.crystalnexus.jei_recipes.EnergyExtractionRecipeCategory;
import net.crystalnexus.jei_recipes.EnergyExtractionRecipe;
import net.crystalnexus.jei_recipes.DustSeperationRecipeCategory;
import net.crystalnexus.jei_recipes.DustSeperationRecipe;
import net.crystalnexus.jei_recipes.CircuitPressingRecipeCategory;
import net.crystalnexus.jei_recipes.CircuitPressingRecipe;
import net.crystalnexus.jei_recipes.ChemicalReactionRecipeCategory;
import net.crystalnexus.jei_recipes.ChemicalReactionRecipe;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipeCategory;
import net.crystalnexus.jei_recipes.RefiningRecipe;
import net.crystalnexus.jei_recipes.RefiningRecipeCategory;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipeCategory;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipe;
import net.crystalnexus.jei_recipes.BiomaticCompostingRecipeCategory;
import net.crystalnexus.jei_recipes.BiomaticCompostingRecipe;
import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipeCategory;
import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipe;
import net.crystalnexus.jei_recipes.AcceleratorJeiRecipeCategory;
import net.crystalnexus.jei_recipes.AcceleratorJeiRecipe;
import net.crystalnexus.util.CrushingRecipeSupport;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin;

import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;

import java.util.Objects;
import java.util.List;

@JeiPlugin
public class CrystalnexusModJeiPlugin implements IModPlugin {
	public static mezz.jei.api.recipe.RecipeType<PurificationRecipe> Purification_Type = new mezz.jei.api.recipe.RecipeType<>(PurificationRecipeCategory.UID, PurificationRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<ExtractinatorJEIRecipe> ExtractinatorJEI_Type = new mezz.jei.api.recipe.RecipeType<>(ExtractinatorJEIRecipeCategory.UID, ExtractinatorJEIRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<BeamReactionRecipeRecipe> BeamReactionRecipe_Type = new mezz.jei.api.recipe.RecipeType<>(BeamReactionRecipeRecipeCategory.UID, BeamReactionRecipeRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<UnfurnaceRecipe> Unfurnace_Type = new mezz.jei.api.recipe.RecipeType<>(UnfurnaceRecipeCategory.UID, UnfurnaceRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<OreCrushingJeiRecipe> OreCrushingJei_Type = new mezz.jei.api.recipe.RecipeType<>(OreCrushingJeiRecipeCategory.UID, OreCrushingJeiRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<DustSeperationRecipe> DustSeperation_Type = new mezz.jei.api.recipe.RecipeType<>(DustSeperationRecipeCategory.UID, DustSeperationRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<ReactorMultiblockGuideRecipe> ReactorMultiblockGuide_Type = new mezz.jei.api.recipe.RecipeType<>(ReactorMultiblockGuideRecipeCategory.UID, ReactorMultiblockGuideRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<CircuitPressingRecipe> CircuitPressing_Type = new mezz.jei.api.recipe.RecipeType<>(CircuitPressingRecipeCategory.UID, CircuitPressingRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<InverterJeiRecipe> InverterJei_Type = new mezz.jei.api.recipe.RecipeType<>(InverterJeiRecipeCategory.UID, InverterJeiRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<ReactionJEIRecipe> ReactionJEI_Type = new mezz.jei.api.recipe.RecipeType<>(ReactionJEIRecipeCategory.UID, ReactionJEIRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<EnergyExtractionRecipe> EnergyExtraction_Type = new mezz.jei.api.recipe.RecipeType<>(EnergyExtractionRecipeCategory.UID, EnergyExtractionRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<ReactionMultiblockGuideRecipe> ReactionMultiblockGuide_Type = new mezz.jei.api.recipe.RecipeType<>(ReactionMultiblockGuideRecipeCategory.UID, ReactionMultiblockGuideRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<MatterTransmutationRecipe> MatterTransmutation_Type = new mezz.jei.api.recipe.RecipeType<>(MatterTransmutationRecipeCategory.UID, MatterTransmutationRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<SingularityCompressionRecipe> SingularityCompression_Type = new mezz.jei.api.recipe.RecipeType<>(SingularityCompressionRecipeCategory.UID, SingularityCompressionRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<ChemicalReactionRecipe> ChemicalReaction_Type = new mezz.jei.api.recipe.RecipeType<>(ChemicalReactionRecipeCategory.UID, ChemicalReactionRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<FluidChemicalReactionRecipe> FluidChemicalReaction_Type = new mezz.jei.api.recipe.RecipeType<>(FluidChemicalReactionRecipeCategory.UID, FluidChemicalReactionRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<RefiningRecipe> Refining_Type = new mezz.jei.api.recipe.RecipeType<>(RefiningRecipeCategory.UID, RefiningRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<BiomaticCompostingRecipe> BiomaticComposting_Type = new mezz.jei.api.recipe.RecipeType<>(BiomaticCompostingRecipeCategory.UID, BiomaticCompostingRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<BiomaticSimulationRecipe> BiomaticSimulation_Type = new mezz.jei.api.recipe.RecipeType<>(BiomaticSimulationRecipeCategory.UID, BiomaticSimulationRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<PistonGeneratorJEIRecipe> PistonGeneratorJEI_Type = new mezz.jei.api.recipe.RecipeType<>(PistonGeneratorJEIRecipeCategory.UID, PistonGeneratorJEIRecipe.class);
	public static mezz.jei.api.recipe.RecipeType<AcceleratorJeiRecipe> AcceleratorJei_Type = new mezz.jei.api.recipe.RecipeType<>(AcceleratorJeiRecipeCategory.UID, AcceleratorJeiRecipe.class);

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.parse("crystalnexus:jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new PurificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new ExtractinatorJEIRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new BeamReactionRecipeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new UnfurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new OreCrushingJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new DustSeperationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new ReactorMultiblockGuideRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new CircuitPressingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new InverterJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new ReactionJEIRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new EnergyExtractionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new ReactionMultiblockGuideRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new MatterTransmutationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new SingularityCompressionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new ChemicalReactionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new FluidChemicalReactionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new RefiningRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new BiomaticCompostingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new BiomaticSimulationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new PistonGeneratorJEIRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new AcceleratorJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RecipeManager recipeManager = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();
		List<PurificationRecipe> PurificationRecipes = recipes(recipeManager, PurificationRecipe.class);
		registration.addRecipes(Purification_Type, PurificationRecipes);
		List<ExtractinatorJEIRecipe> ExtractinatorJEIRecipes = recipes(recipeManager, ExtractinatorJEIRecipe.class);
		registration.addRecipes(ExtractinatorJEI_Type, ExtractinatorJEIRecipes);
		List<BeamReactionRecipeRecipe> BeamReactionRecipeRecipes = recipes(recipeManager, BeamReactionRecipeRecipe.class);
		registration.addRecipes(BeamReactionRecipe_Type, BeamReactionRecipeRecipes);
		List<UnfurnaceRecipe> UnfurnaceRecipes = recipes(recipeManager, UnfurnaceRecipe.class);
		registration.addRecipes(Unfurnace_Type, UnfurnaceRecipes);
		registration.addRecipes(OreCrushingJei_Type, CrushingRecipeSupport.jeiRecipes(Minecraft.getInstance().level));
		List<OreCrushingJeiRecipe> generatedCrushing = CrushingRecipeSupport.generatedJeiRecipes(Minecraft.getInstance().level);
		registration.addRecipes(OreCrushingJei_Type, generatedCrushing);
		List<DustSeperationRecipe> DustSeperationRecipes = recipes(recipeManager, DustSeperationRecipe.class);
		registration.addRecipes(DustSeperation_Type, DustSeperationRecipes);
		List<DustSeperationRecipe> generatedSeparation = MaterialProcessingCatalog.generatedSeparatorRecipes(Minecraft.getInstance().level);
		registration.addRecipes(DustSeperation_Type, generatedSeparation);
		List<ReactorMultiblockGuideRecipe> ReactorMultiblockGuideRecipes = recipes(recipeManager, ReactorMultiblockGuideRecipe.class);
		registration.addRecipes(ReactorMultiblockGuide_Type, ReactorMultiblockGuideRecipes);
		List<CircuitPressingRecipe> CircuitPressingRecipes = recipes(recipeManager, CircuitPressingRecipe.class);
		registration.addRecipes(CircuitPressing_Type, CircuitPressingRecipes);
		List<InverterJeiRecipe> InverterJeiRecipes = recipes(recipeManager, InverterJeiRecipe.class);
		registration.addRecipes(InverterJei_Type, InverterJeiRecipes);
		List<ReactionJEIRecipe> ReactionJEIRecipes = recipes(recipeManager, ReactionJEIRecipe.class);
		registration.addRecipes(ReactionJEI_Type, ReactionJEIRecipes);
		List<EnergyExtractionRecipe> EnergyExtractionRecipes = recipes(recipeManager, EnergyExtractionRecipe.class);
		registration.addRecipes(EnergyExtraction_Type, EnergyExtractionRecipes);
		List<ReactionMultiblockGuideRecipe> ReactionMultiblockGuideRecipes = recipes(recipeManager, ReactionMultiblockGuideRecipe.class);
		registration.addRecipes(ReactionMultiblockGuide_Type, ReactionMultiblockGuideRecipes);
		List<MatterTransmutationRecipe> MatterTransmutationRecipes = recipes(recipeManager, MatterTransmutationRecipe.class);
		registration.addRecipes(MatterTransmutation_Type, MatterTransmutationRecipes);
		List<SingularityCompressionRecipe> SingularityCompressionRecipes = recipes(recipeManager, SingularityCompressionRecipe.class);
		registration.addRecipes(SingularityCompression_Type, SingularityCompressionRecipes);
		List<ChemicalReactionRecipe> ChemicalReactionRecipes = recipes(recipeManager, ChemicalReactionRecipe.class);
		registration.addRecipes(ChemicalReaction_Type, ChemicalReactionRecipes);
		List<FluidChemicalReactionRecipe> FluidChemicalReactionRecipes = recipes(recipeManager, FluidChemicalReactionRecipe.class);
		registration.addRecipes(FluidChemicalReaction_Type, FluidChemicalReactionRecipes);
		List<FluidChemicalReactionRecipe> generatedFluid = MaterialProcessingCatalog.generatedFluidRecipes(Minecraft.getInstance().level);
		registration.addRecipes(FluidChemicalReaction_Type, generatedFluid);
		List<RefiningRecipe> RefiningRecipes = recipes(recipeManager, RefiningRecipe.class);
		registration.addRecipes(Refining_Type, RefiningRecipes);
		List<RefiningRecipe> generatedRefining = MaterialProcessingCatalog.generatedRefiningRecipes(Minecraft.getInstance().level);
		registration.addRecipes(Refining_Type, generatedRefining);
		CrystalnexusJeiRuntimePlugin.seedMaterialRecipes(generatedCrushing, generatedFluid, generatedRefining, generatedSeparation);
		List<BiomaticCompostingRecipe> BiomaticCompostingRecipes = recipes(recipeManager, BiomaticCompostingRecipe.class);
		registration.addRecipes(BiomaticComposting_Type, BiomaticCompostingRecipes);
		List<BiomaticSimulationRecipe> BiomaticSimulationRecipes = recipes(recipeManager, BiomaticSimulationRecipe.class);
		registration.addRecipes(BiomaticSimulation_Type, BiomaticSimulationRecipes);
		List<PistonGeneratorJEIRecipe> PistonGeneratorJEIRecipes = recipes(recipeManager, PistonGeneratorJEIRecipe.class);
		registration.addRecipes(PistonGeneratorJEI_Type, PistonGeneratorJEIRecipes);
		List<AcceleratorJeiRecipe> AcceleratorJeiRecipes = recipes(recipeManager, AcceleratorJeiRecipe.class);
		registration.addRecipes(AcceleratorJei_Type, AcceleratorJeiRecipes);
	}

	private static <T> List<T> recipes(RecipeManager manager, Class<T> recipeClass) {
		return manager.getRecipes().stream().map(RecipeHolder::value)
				.filter(recipeClass::isInstance).map(recipeClass::cast).toList();
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.IRON_SMELTER.get().asItem()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_SMELTER.get().asItem()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.INVERTIUM_SMELTER.get().asItem()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CHLOROPHYTE_SMELTER.get().asItem()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.ULTIMA_SMELTER.get().asItem()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_PURIFIER.get().asItem()), Purification_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.EXTRACTINATOR.get().asItem()), ExtractinatorJEI_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_GUIDE.get().asItem()), BeamReactionRecipe_Type);
		registration.addRecipeCatalyst(new ItemStack(Items.END_CRYSTAL), BeamReactionRecipe_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.METALLURGIC_RECRYSTALLIZER.get().asItem()), Unfurnace_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_CRUSHER.get().asItem()), OreCrushingJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CHLOROPHYTE_CRUSHER.get().asItem()), OreCrushingJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.INVERTIUM_CRUSHER.get().asItem()), OreCrushingJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.HYPER_CRUSHER.get().asItem()), OreCrushingJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.DUST_SEPARATOR.get().asItem()), DustSeperation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CHLOROPHYTE_DUST_SEPARATOR.get().asItem()), DustSeperation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.INVERTIUM_DUST_SEPARATOR.get().asItem()), DustSeperation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.HYPER_DUST_SEPARATOR.get().asItem()), DustSeperation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTOR_COMPUTER.get().asItem()), ReactorMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT.get().asItem()), ReactorMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get().asItem()), ReactorMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTOR_BLOCK.get().asItem()), ReactorMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTOR_CORE.get().asItem()), ReactorMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CIRCUIT_PRESS.get().asItem()), CircuitPressing_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.INVERTER.get().asItem()), InverterJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get().asItem()), ReactionJEI_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.ENERGY_EXTRACTOR.get().asItem()), EnergyExtraction_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTION_ENERGY_INPUT.get().asItem()), ReactionMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER.get().asItem()), ReactionMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTION_CHAMBER_CORE.get().asItem()), ReactionMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REACTION_CHAMBER_BLOCK.get().asItem()), ReactionMultiblockGuide_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE.get().asItem()), MatterTransmutation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.SINGULARITY_COMPRESSOR.get().asItem()), SingularityCompression_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CHEMICAL_REACTION_CHAMBER.get().asItem()), ChemicalReaction_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.FLUID_CHEMICAL_REACTION_CHAMBER.get().asItem()), FluidChemicalReaction_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.REFINERY.get().asItem()), Refining_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CHLOROPHYTE_REFINERY.get().asItem()), Refining_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.INVERTIUM_REFINERY.get().asItem()), Refining_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.HYPER_REFINERY.get().asItem()), Refining_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.BIOMATIC_COMPOSTER.get().asItem()), BiomaticComposting_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().asItem()), BiomaticSimulation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.PISTON_GENERATOR.get().asItem()), PistonGeneratorJEI_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get().asItem()), AcceleratorJei_Type);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addUniversalRecipeTransferHandler(new net.crystalnexus.client.DepotCliJeiTransferHandler());
		net.crystalnexus.jei.CrystalnexusJeiRuntimePlugin.registerCategoryTransferHandlers(registration);
	}
}
