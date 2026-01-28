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
import net.crystalnexus.jei_recipes.SinglePurificationRecipeCategory;
import net.crystalnexus.jei_recipes.SinglePurificationRecipe;
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
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipeCategory;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipe;
import net.crystalnexus.jei_recipes.BiomaticCompostingRecipeCategory;
import net.crystalnexus.jei_recipes.BiomaticCompostingRecipe;
import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipeCategory;
import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipe;
import net.crystalnexus.jei_recipes.AcceleratorJeiRecipeCategory;
import net.crystalnexus.jei_recipes.AcceleratorJeiRecipe;

import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.List;

@JeiPlugin
public class CrystalnexusModJeiPlugin implements IModPlugin {
	public static mezz.jei.api.recipe.RecipeType<SinglePurificationRecipe> SinglePurification_Type = new mezz.jei.api.recipe.RecipeType<>(SinglePurificationRecipeCategory.UID, SinglePurificationRecipe.class);
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
		registration.addRecipeCategories(new SinglePurificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
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
		registration.addRecipeCategories(new BiomaticCompostingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new BiomaticSimulationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new PistonGeneratorJEIRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new AcceleratorJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RecipeManager recipeManager = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();
		List<SinglePurificationRecipe> SinglePurificationRecipes = recipeManager.getAllRecipesFor(SinglePurificationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(SinglePurification_Type, SinglePurificationRecipes);
		List<PurificationRecipe> PurificationRecipes = recipeManager.getAllRecipesFor(PurificationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(Purification_Type, PurificationRecipes);
		List<ExtractinatorJEIRecipe> ExtractinatorJEIRecipes = recipeManager.getAllRecipesFor(ExtractinatorJEIRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(ExtractinatorJEI_Type, ExtractinatorJEIRecipes);
		List<BeamReactionRecipeRecipe> BeamReactionRecipeRecipes = recipeManager.getAllRecipesFor(BeamReactionRecipeRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(BeamReactionRecipe_Type, BeamReactionRecipeRecipes);
		List<UnfurnaceRecipe> UnfurnaceRecipes = recipeManager.getAllRecipesFor(UnfurnaceRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(Unfurnace_Type, UnfurnaceRecipes);
		List<OreCrushingJeiRecipe> OreCrushingJeiRecipes = recipeManager.getAllRecipesFor(OreCrushingJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(OreCrushingJei_Type, OreCrushingJeiRecipes);
		List<DustSeperationRecipe> DustSeperationRecipes = recipeManager.getAllRecipesFor(DustSeperationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(DustSeperation_Type, DustSeperationRecipes);
		List<ReactorMultiblockGuideRecipe> ReactorMultiblockGuideRecipes = recipeManager.getAllRecipesFor(ReactorMultiblockGuideRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(ReactorMultiblockGuide_Type, ReactorMultiblockGuideRecipes);
		List<CircuitPressingRecipe> CircuitPressingRecipes = recipeManager.getAllRecipesFor(CircuitPressingRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(CircuitPressing_Type, CircuitPressingRecipes);
		List<InverterJeiRecipe> InverterJeiRecipes = recipeManager.getAllRecipesFor(InverterJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(InverterJei_Type, InverterJeiRecipes);
		List<ReactionJEIRecipe> ReactionJEIRecipes = recipeManager.getAllRecipesFor(ReactionJEIRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(ReactionJEI_Type, ReactionJEIRecipes);
		List<EnergyExtractionRecipe> EnergyExtractionRecipes = recipeManager.getAllRecipesFor(EnergyExtractionRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(EnergyExtraction_Type, EnergyExtractionRecipes);
		List<ReactionMultiblockGuideRecipe> ReactionMultiblockGuideRecipes = recipeManager.getAllRecipesFor(ReactionMultiblockGuideRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(ReactionMultiblockGuide_Type, ReactionMultiblockGuideRecipes);
		List<MatterTransmutationRecipe> MatterTransmutationRecipes = recipeManager.getAllRecipesFor(MatterTransmutationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(MatterTransmutation_Type, MatterTransmutationRecipes);
		List<SingularityCompressionRecipe> SingularityCompressionRecipes = recipeManager.getAllRecipesFor(SingularityCompressionRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(SingularityCompression_Type, SingularityCompressionRecipes);
		List<ChemicalReactionRecipe> ChemicalReactionRecipes = recipeManager.getAllRecipesFor(ChemicalReactionRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(ChemicalReaction_Type, ChemicalReactionRecipes);
		List<BiomaticCompostingRecipe> BiomaticCompostingRecipes = recipeManager.getAllRecipesFor(BiomaticCompostingRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(BiomaticComposting_Type, BiomaticCompostingRecipes);
		List<BiomaticSimulationRecipe> BiomaticSimulationRecipes = recipeManager.getAllRecipesFor(BiomaticSimulationRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(BiomaticSimulation_Type, BiomaticSimulationRecipes);
		List<PistonGeneratorJEIRecipe> PistonGeneratorJEIRecipes = recipeManager.getAllRecipesFor(PistonGeneratorJEIRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(PistonGeneratorJEI_Type, PistonGeneratorJEIRecipes);
		List<AcceleratorJeiRecipe> AcceleratorJeiRecipes = recipeManager.getAllRecipesFor(AcceleratorJeiRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).collect(Collectors.toList());
		registration.addRecipes(AcceleratorJei_Type, AcceleratorJeiRecipes);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_PURIFIER.get().asItem()), SinglePurification_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_PURIFIER.get().asItem()), Purification_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.EXTRACTINATOR.get().asItem()), ExtractinatorJEI_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_GUIDE.get().asItem()), BeamReactionRecipe_Type);
		registration.addRecipeCatalyst(new ItemStack(Items.END_CRYSTAL), BeamReactionRecipe_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.METALLURGIC_RECRYSTALLIZER.get().asItem()), Unfurnace_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.CRYSTAL_CRUSHER.get().asItem()), OreCrushingJei_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.DUST_SEPARATOR.get().asItem()), DustSeperation_Type);
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
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.BIOMATIC_COMPOSTER.get().asItem()), BiomaticComposting_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().asItem()), BiomaticSimulation_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.PISTON_GENERATOR.get().asItem()), PistonGeneratorJEI_Type);
		registration.addRecipeCatalyst(new ItemStack(CrystalnexusModBlocks.PARTICLE_ACCELERATOR_CONTROLLER.get().asItem()), AcceleratorJei_Type);
	}
}