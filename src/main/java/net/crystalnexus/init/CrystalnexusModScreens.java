/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.crystalnexus.client.gui.WarpPadGuiScreen;
import net.crystalnexus.client.gui.UltimaSmelterGuiScreen;
import net.crystalnexus.client.gui.TurbineGUIScreen;
import net.crystalnexus.client.gui.TesseractGuiScreen;
import net.crystalnexus.client.gui.SteamEngineGUIScreen;
import net.crystalnexus.client.gui.SteamChamberGUIScreen;
import net.crystalnexus.client.gui.SingularityMatrixGUIScreen;
import net.crystalnexus.client.gui.SingularityCompressorGUIScreen;
import net.crystalnexus.client.gui.SeparatorGuiScreen;
import net.crystalnexus.client.gui.ReactorGUIScreen;
import net.crystalnexus.client.gui.ReactionGUIScreen;
import net.crystalnexus.client.gui.QuantumMinerGUIScreen;
import net.crystalnexus.client.gui.PistonGenGUIScreen;
import net.crystalnexus.client.gui.OreProGUIScreen;
import net.crystalnexus.client.gui.OreGenGUIScreen;
import net.crystalnexus.client.gui.NodeMinerGUIScreen;
import net.crystalnexus.client.gui.MultiblockGuiPage5Screen;
import net.crystalnexus.client.gui.MultiblockGuiPage4Screen;
import net.crystalnexus.client.gui.MultiblockGuiPage3Screen;
import net.crystalnexus.client.gui.MultiblockGuiPage2Screen;
import net.crystalnexus.client.gui.MultiblockGuiPage1Screen;
import net.crystalnexus.client.gui.MatterTransmutationGUIScreen;
import net.crystalnexus.client.gui.MRecrystallGuiScreen;
import net.crystalnexus.client.gui.ItemElevatorGuiScreen;
import net.crystalnexus.client.gui.ItemElevatorGuiDownScreen;
import net.crystalnexus.client.gui.ItemCollectorGUIScreen;
import net.crystalnexus.client.gui.IronSmelterGuiScreen;
import net.crystalnexus.client.gui.InverterGuiScreen;
import net.crystalnexus.client.gui.GrowthChamberGuiScreen;
import net.crystalnexus.client.gui.FluidInputGuiScreen;
import net.crystalnexus.client.gui.FactoryItemControllerGuiScreen;
import net.crystalnexus.client.gui.FactoryControllerGuiScreen;
import net.crystalnexus.client.gui.ExtractinatorGuiScreen;
import net.crystalnexus.client.gui.EnergyExtractorGUIScreen;
import net.crystalnexus.client.gui.CrystalPurifierGUIScreen;
import net.crystalnexus.client.gui.CrusherGuiScreen;
import net.crystalnexus.client.gui.CraftingFactoryGUIScreen;
import net.crystalnexus.client.gui.ContainerGUIScreen;
import net.crystalnexus.client.gui.CircuitPressGUIScreen;
import net.crystalnexus.client.gui.ChemicalReactionChamberGUIScreen;
import net.crystalnexus.client.gui.BlockPlacerGuiScreen;
import net.crystalnexus.client.gui.BioSIMGuiScreen;
import net.crystalnexus.client.gui.BioMGuiScreen;
import net.crystalnexus.client.gui.BioMCGuiScreen;
import net.crystalnexus.client.gui.BatteryMonitorGuiScreen;
import net.crystalnexus.client.gui.AccepterGUIScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CrystalnexusModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(CrystalnexusModMenus.CRYSTAL_PURIFIER_GUI.get(), CrystalPurifierGUIScreen::new);
		event.register(CrystalnexusModMenus.ACCEPTER_GUI.get(), AccepterGUIScreen::new);
		event.register(CrystalnexusModMenus.GROWTH_CHAMBER_GUI.get(), GrowthChamberGuiScreen::new);
		event.register(CrystalnexusModMenus.CRUSHER_GUI.get(), CrusherGuiScreen::new);
		event.register(CrystalnexusModMenus.SEPARATOR_GUI.get(), SeparatorGuiScreen::new);
		event.register(CrystalnexusModMenus.ORE_GEN_GUI.get(), OreGenGUIScreen::new);
		event.register(CrystalnexusModMenus.ITEM_COLLECTOR_GUI.get(), ItemCollectorGUIScreen::new);
		event.register(CrystalnexusModMenus.EXTRACTINATOR_GUI.get(), ExtractinatorGuiScreen::new);
		event.register(CrystalnexusModMenus.M_RECRYSTALL_GUI.get(), MRecrystallGuiScreen::new);
		event.register(CrystalnexusModMenus.REACTOR_GUI.get(), ReactorGUIScreen::new);
		event.register(CrystalnexusModMenus.IRON_SMELTER_GUI.get(), IronSmelterGuiScreen::new);
		event.register(CrystalnexusModMenus.TESSERACT_GUI.get(), TesseractGuiScreen::new);
		event.register(CrystalnexusModMenus.CIRCUIT_PRESS_GUI.get(), CircuitPressGUIScreen::new);
		event.register(CrystalnexusModMenus.FACTORY_CONTROLLER_GUI.get(), FactoryControllerGuiScreen::new);
		event.register(CrystalnexusModMenus.FACTORY_ITEM_CONTROLLER_GUI.get(), FactoryItemControllerGuiScreen::new);
		event.register(CrystalnexusModMenus.INVERTER_GUI.get(), InverterGuiScreen::new);
		event.register(CrystalnexusModMenus.REACTION_GUI.get(), ReactionGUIScreen::new);
		event.register(CrystalnexusModMenus.ULTIMA_SMELTER_GUI.get(), UltimaSmelterGuiScreen::new);
		event.register(CrystalnexusModMenus.ENERGY_EXTRACTOR_GUI.get(), EnergyExtractorGUIScreen::new);
		event.register(CrystalnexusModMenus.FLUID_INPUT_GUI.get(), FluidInputGuiScreen::new);
		event.register(CrystalnexusModMenus.MATTER_TRANSMUTATION_GUI.get(), MatterTransmutationGUIScreen::new);
		event.register(CrystalnexusModMenus.BLOCK_PLACER_GUI.get(), BlockPlacerGuiScreen::new);
		event.register(CrystalnexusModMenus.SINGULARITY_COMPRESSOR_GUI.get(), SingularityCompressorGUIScreen::new);
		event.register(CrystalnexusModMenus.CHEMICAL_REACTION_CHAMBER_GUI.get(), ChemicalReactionChamberGUIScreen::new);
		event.register(CrystalnexusModMenus.CONTAINER_GUI.get(), ContainerGUIScreen::new);
		event.register(CrystalnexusModMenus.QUANTUM_MINER_GUI.get(), QuantumMinerGUIScreen::new);
		event.register(CrystalnexusModMenus.TURBINE_GUI.get(), TurbineGUIScreen::new);
		event.register(CrystalnexusModMenus.ORE_PRO_GUI.get(), OreProGUIScreen::new);
		event.register(CrystalnexusModMenus.BIO_M_GUI.get(), BioMGuiScreen::new);
		event.register(CrystalnexusModMenus.BIO_MC_GUI.get(), BioMCGuiScreen::new);
		event.register(CrystalnexusModMenus.BIO_SIM_GUI.get(), BioSIMGuiScreen::new);
		event.register(CrystalnexusModMenus.BATTERY_MONITOR_GUI.get(), BatteryMonitorGuiScreen::new);
		event.register(CrystalnexusModMenus.MULTIBLOCK_GUI_PAGE_1.get(), MultiblockGuiPage1Screen::new);
		event.register(CrystalnexusModMenus.MULTIBLOCK_GUI_PAGE_2.get(), MultiblockGuiPage2Screen::new);
		event.register(CrystalnexusModMenus.MULTIBLOCK_GUI_PAGE_3.get(), MultiblockGuiPage3Screen::new);
		event.register(CrystalnexusModMenus.MULTIBLOCK_GUI_PAGE_4.get(), MultiblockGuiPage4Screen::new);
		event.register(CrystalnexusModMenus.MULTIBLOCK_GUI_PAGE_5.get(), MultiblockGuiPage5Screen::new);
		event.register(CrystalnexusModMenus.WARP_PAD_GUI.get(), WarpPadGuiScreen::new);
		event.register(CrystalnexusModMenus.PISTON_GEN_GUI.get(), PistonGenGUIScreen::new);
		event.register(CrystalnexusModMenus.STEAM_CHAMBER_GUI.get(), SteamChamberGUIScreen::new);
		event.register(CrystalnexusModMenus.ITEM_ELEVATOR_GUI.get(), ItemElevatorGuiScreen::new);
		event.register(CrystalnexusModMenus.ITEM_ELEVATOR_GUI_DOWN.get(), ItemElevatorGuiDownScreen::new);
		event.register(CrystalnexusModMenus.CRAFTING_FACTORY_GUI.get(), CraftingFactoryGUIScreen::new);
		event.register(CrystalnexusModMenus.NODE_MINER_GUI.get(), NodeMinerGUIScreen::new);
		event.register(CrystalnexusModMenus.STEAM_ENGINE_GUI.get(), SteamEngineGUIScreen::new);
		event.register(CrystalnexusModMenus.SINGULARITY_MATRIX_GUI.get(), SingularityMatrixGUIScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}