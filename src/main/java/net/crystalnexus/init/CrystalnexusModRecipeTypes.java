package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;

import net.crystalnexus.jei_recipes.UnfurnaceRecipe;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipe;
import net.crystalnexus.jei_recipes.SinglePurificationRecipe;
import net.crystalnexus.jei_recipes.ReactorMultiblockGuideRecipe;
import net.crystalnexus.jei_recipes.ReactionMultiblockGuideRecipe;
import net.crystalnexus.jei_recipes.ReactionJEIRecipe;
import net.crystalnexus.jei_recipes.PurificationRecipe;
import net.crystalnexus.jei_recipes.PistonGeneratorJEIRecipe;
import net.crystalnexus.jei_recipes.OreCrushingJeiRecipe;
import net.crystalnexus.jei_recipes.MatterTransmutationRecipe;
import net.crystalnexus.jei_recipes.InverterJeiRecipe;
import net.crystalnexus.jei_recipes.ExtractinatorJEIRecipe;
import net.crystalnexus.jei_recipes.EnergyExtractionRecipe;
import net.crystalnexus.jei_recipes.DustSeperationRecipe;
import net.crystalnexus.jei_recipes.CircuitPressingRecipe;
import net.crystalnexus.jei_recipes.ChemicalReactionRecipe;
import net.crystalnexus.jei_recipes.BiomaticSimulationRecipe;
import net.crystalnexus.jei_recipes.BiomaticCompostingRecipe;
import net.crystalnexus.jei_recipes.BeamReactionRecipeRecipe;
import net.crystalnexus.jei_recipes.AcceleratorJeiRecipe;
import net.crystalnexus.CrystalnexusMod;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CrystalnexusModRecipeTypes {
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, "crystalnexus");
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "crystalnexus");

	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		IEventBus bus = ModList.get().getModContainerById("crystalnexus").get().getEventBus();
		event.enqueueWork(() -> {
			RECIPE_TYPES.register(bus);
			SERIALIZERS.register(bus);
			RECIPE_TYPES.register("single_purification", () -> SinglePurificationRecipe.Type.INSTANCE);
			SERIALIZERS.register("single_purification", () -> SinglePurificationRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("purification", () -> PurificationRecipe.Type.INSTANCE);
			SERIALIZERS.register("purification", () -> PurificationRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("extractination", () -> ExtractinatorJEIRecipe.Type.INSTANCE);
			SERIALIZERS.register("extractination", () -> ExtractinatorJEIRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("beam_reaction_recipe", () -> BeamReactionRecipeRecipe.Type.INSTANCE);
			SERIALIZERS.register("beam_reaction_recipe", () -> BeamReactionRecipeRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("unfurnace", () -> UnfurnaceRecipe.Type.INSTANCE);
			SERIALIZERS.register("unfurnace", () -> UnfurnaceRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("ore_crushing_jei", () -> OreCrushingJeiRecipe.Type.INSTANCE);
			SERIALIZERS.register("ore_crushing_jei", () -> OreCrushingJeiRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("dust_seperation", () -> DustSeperationRecipe.Type.INSTANCE);
			SERIALIZERS.register("dust_seperation", () -> DustSeperationRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("reactor_multiblock_guide", () -> ReactorMultiblockGuideRecipe.Type.INSTANCE);
			SERIALIZERS.register("reactor_multiblock_guide", () -> ReactorMultiblockGuideRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("circuit_pressing", () -> CircuitPressingRecipe.Type.INSTANCE);
			SERIALIZERS.register("circuit_pressing", () -> CircuitPressingRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("inverter_jei", () -> InverterJeiRecipe.Type.INSTANCE);
			SERIALIZERS.register("inverter_jei", () -> InverterJeiRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("reaction_jei", () -> ReactionJEIRecipe.Type.INSTANCE);
			SERIALIZERS.register("reaction_jei", () -> ReactionJEIRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("energy_extraction", () -> EnergyExtractionRecipe.Type.INSTANCE);
			SERIALIZERS.register("energy_extraction", () -> EnergyExtractionRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("reaction_multiblock_guide", () -> ReactionMultiblockGuideRecipe.Type.INSTANCE);
			SERIALIZERS.register("reaction_multiblock_guide", () -> ReactionMultiblockGuideRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("matter_transmutation", () -> MatterTransmutationRecipe.Type.INSTANCE);
			SERIALIZERS.register("matter_transmutation", () -> MatterTransmutationRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("singularity_compression", () -> SingularityCompressionRecipe.Type.INSTANCE);
			SERIALIZERS.register("singularity_compression", () -> SingularityCompressionRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("chemical_reaction", () -> ChemicalReactionRecipe.Type.INSTANCE);
			SERIALIZERS.register("chemical_reaction", () -> ChemicalReactionRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("biomatic_composting", () -> BiomaticCompostingRecipe.Type.INSTANCE);
			SERIALIZERS.register("biomatic_composting", () -> BiomaticCompostingRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("biomatic_simulation", () -> BiomaticSimulationRecipe.Type.INSTANCE);
			SERIALIZERS.register("biomatic_simulation", () -> BiomaticSimulationRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("piston_generator_jei", () -> PistonGeneratorJEIRecipe.Type.INSTANCE);
			SERIALIZERS.register("piston_generator_jei", () -> PistonGeneratorJEIRecipe.Serializer.INSTANCE);
			RECIPE_TYPES.register("accelerator_jei", () -> AcceleratorJeiRecipe.Type.INSTANCE);
			SERIALIZERS.register("accelerator_jei", () -> AcceleratorJeiRecipe.Serializer.INSTANCE);
		});
	}
}