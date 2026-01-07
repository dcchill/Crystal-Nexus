/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import net.crystalnexus.world.inventory.WarpPadGuiMenu;
import net.crystalnexus.world.inventory.UltimaSmelterGuiMenu;
import net.crystalnexus.world.inventory.TurbineGUIMenu;
import net.crystalnexus.world.inventory.TesseractGuiMenu;
import net.crystalnexus.world.inventory.SteamChamberGUIMenu;
import net.crystalnexus.world.inventory.SingularityCompressorGUIMenu;
import net.crystalnexus.world.inventory.SeparatorGuiMenu;
import net.crystalnexus.world.inventory.ReactorGUIMenu;
import net.crystalnexus.world.inventory.ReactionGUIMenu;
import net.crystalnexus.world.inventory.QuantumMinerGUIMenu;
import net.crystalnexus.world.inventory.PistonGenGUIMenu;
import net.crystalnexus.world.inventory.OreProGUIMenu;
import net.crystalnexus.world.inventory.OreGenGUIMenu;
import net.crystalnexus.world.inventory.MultiblockGuiPage5Menu;
import net.crystalnexus.world.inventory.MultiblockGuiPage4Menu;
import net.crystalnexus.world.inventory.MultiblockGuiPage3Menu;
import net.crystalnexus.world.inventory.MultiblockGuiPage2Menu;
import net.crystalnexus.world.inventory.MultiblockGuiPage1Menu;
import net.crystalnexus.world.inventory.MatterTransmutationGUIMenu;
import net.crystalnexus.world.inventory.MRecrystallGuiMenu;
import net.crystalnexus.world.inventory.ItemElevatorGuiMenu;
import net.crystalnexus.world.inventory.ItemElevatorGuiDownMenu;
import net.crystalnexus.world.inventory.ItemCollectorGUIMenu;
import net.crystalnexus.world.inventory.IronSmelterGuiMenu;
import net.crystalnexus.world.inventory.InverterGuiMenu;
import net.crystalnexus.world.inventory.GrowthChamberGuiMenu;
import net.crystalnexus.world.inventory.FluidInputGuiMenu;
import net.crystalnexus.world.inventory.FactoryItemControllerGuiMenu;
import net.crystalnexus.world.inventory.FactoryControllerGuiMenu;
import net.crystalnexus.world.inventory.ExtractinatorGuiMenu;
import net.crystalnexus.world.inventory.EnergyExtractorGUIMenu;
import net.crystalnexus.world.inventory.CrystalPurifierGUIMenu;
import net.crystalnexus.world.inventory.CrusherGuiMenu;
import net.crystalnexus.world.inventory.CraftingFactoryGUIMenu;
import net.crystalnexus.world.inventory.ContainerGUIMenu;
import net.crystalnexus.world.inventory.CircuitPressGUIMenu;
import net.crystalnexus.world.inventory.ChemicalReactionChamberGUIMenu;
import net.crystalnexus.world.inventory.BlockPlacerGuiMenu;
import net.crystalnexus.world.inventory.BioSIMGuiMenu;
import net.crystalnexus.world.inventory.BioMGuiMenu;
import net.crystalnexus.world.inventory.BioMCGuiMenu;
import net.crystalnexus.world.inventory.BatteryMonitorGuiMenu;
import net.crystalnexus.world.inventory.AccepterGUIMenu;
import net.crystalnexus.network.MenuStateUpdateMessage;
import net.crystalnexus.CrystalnexusMod;

import java.util.Map;

public class CrystalnexusModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, CrystalnexusMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<CrystalPurifierGUIMenu>> CRYSTAL_PURIFIER_GUI = REGISTRY.register("crystal_purifier_gui", () -> IMenuTypeExtension.create(CrystalPurifierGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<AccepterGUIMenu>> ACCEPTER_GUI = REGISTRY.register("accepter_gui", () -> IMenuTypeExtension.create(AccepterGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<GrowthChamberGuiMenu>> GROWTH_CHAMBER_GUI = REGISTRY.register("growth_chamber_gui", () -> IMenuTypeExtension.create(GrowthChamberGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CrusherGuiMenu>> CRUSHER_GUI = REGISTRY.register("crusher_gui", () -> IMenuTypeExtension.create(CrusherGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<SeparatorGuiMenu>> SEPARATOR_GUI = REGISTRY.register("separator_gui", () -> IMenuTypeExtension.create(SeparatorGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<OreGenGUIMenu>> ORE_GEN_GUI = REGISTRY.register("ore_gen_gui", () -> IMenuTypeExtension.create(OreGenGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ItemCollectorGUIMenu>> ITEM_COLLECTOR_GUI = REGISTRY.register("item_collector_gui", () -> IMenuTypeExtension.create(ItemCollectorGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ExtractinatorGuiMenu>> EXTRACTINATOR_GUI = REGISTRY.register("extractinator_gui", () -> IMenuTypeExtension.create(ExtractinatorGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MRecrystallGuiMenu>> M_RECRYSTALL_GUI = REGISTRY.register("m_recrystall_gui", () -> IMenuTypeExtension.create(MRecrystallGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ReactorGUIMenu>> REACTOR_GUI = REGISTRY.register("reactor_gui", () -> IMenuTypeExtension.create(ReactorGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<IronSmelterGuiMenu>> IRON_SMELTER_GUI = REGISTRY.register("iron_smelter_gui", () -> IMenuTypeExtension.create(IronSmelterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<TesseractGuiMenu>> TESSERACT_GUI = REGISTRY.register("tesseract_gui", () -> IMenuTypeExtension.create(TesseractGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CircuitPressGUIMenu>> CIRCUIT_PRESS_GUI = REGISTRY.register("circuit_press_gui", () -> IMenuTypeExtension.create(CircuitPressGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<FactoryControllerGuiMenu>> FACTORY_CONTROLLER_GUI = REGISTRY.register("factory_controller_gui", () -> IMenuTypeExtension.create(FactoryControllerGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<FactoryItemControllerGuiMenu>> FACTORY_ITEM_CONTROLLER_GUI = REGISTRY.register("factory_item_controller_gui", () -> IMenuTypeExtension.create(FactoryItemControllerGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<InverterGuiMenu>> INVERTER_GUI = REGISTRY.register("inverter_gui", () -> IMenuTypeExtension.create(InverterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ReactionGUIMenu>> REACTION_GUI = REGISTRY.register("reaction_gui", () -> IMenuTypeExtension.create(ReactionGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<UltimaSmelterGuiMenu>> ULTIMA_SMELTER_GUI = REGISTRY.register("ultima_smelter_gui", () -> IMenuTypeExtension.create(UltimaSmelterGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<EnergyExtractorGUIMenu>> ENERGY_EXTRACTOR_GUI = REGISTRY.register("energy_extractor_gui", () -> IMenuTypeExtension.create(EnergyExtractorGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<FluidInputGuiMenu>> FLUID_INPUT_GUI = REGISTRY.register("fluid_input_gui", () -> IMenuTypeExtension.create(FluidInputGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MatterTransmutationGUIMenu>> MATTER_TRANSMUTATION_GUI = REGISTRY.register("matter_transmutation_gui", () -> IMenuTypeExtension.create(MatterTransmutationGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BlockPlacerGuiMenu>> BLOCK_PLACER_GUI = REGISTRY.register("block_placer_gui", () -> IMenuTypeExtension.create(BlockPlacerGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<SingularityCompressorGUIMenu>> SINGULARITY_COMPRESSOR_GUI = REGISTRY.register("singularity_compressor_gui", () -> IMenuTypeExtension.create(SingularityCompressorGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ChemicalReactionChamberGUIMenu>> CHEMICAL_REACTION_CHAMBER_GUI = REGISTRY.register("chemical_reaction_chamber_gui", () -> IMenuTypeExtension.create(ChemicalReactionChamberGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ContainerGUIMenu>> CONTAINER_GUI = REGISTRY.register("container_gui", () -> IMenuTypeExtension.create(ContainerGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<QuantumMinerGUIMenu>> QUANTUM_MINER_GUI = REGISTRY.register("quantum_miner_gui", () -> IMenuTypeExtension.create(QuantumMinerGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<TurbineGUIMenu>> TURBINE_GUI = REGISTRY.register("turbine_gui", () -> IMenuTypeExtension.create(TurbineGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<OreProGUIMenu>> ORE_PRO_GUI = REGISTRY.register("ore_pro_gui", () -> IMenuTypeExtension.create(OreProGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BioMGuiMenu>> BIO_M_GUI = REGISTRY.register("bio_m_gui", () -> IMenuTypeExtension.create(BioMGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BioMCGuiMenu>> BIO_MC_GUI = REGISTRY.register("bio_mc_gui", () -> IMenuTypeExtension.create(BioMCGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BioSIMGuiMenu>> BIO_SIM_GUI = REGISTRY.register("bio_sim_gui", () -> IMenuTypeExtension.create(BioSIMGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BatteryMonitorGuiMenu>> BATTERY_MONITOR_GUI = REGISTRY.register("battery_monitor_gui", () -> IMenuTypeExtension.create(BatteryMonitorGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiblockGuiPage1Menu>> MULTIBLOCK_GUI_PAGE_1 = REGISTRY.register("multiblock_gui_page_1", () -> IMenuTypeExtension.create(MultiblockGuiPage1Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiblockGuiPage2Menu>> MULTIBLOCK_GUI_PAGE_2 = REGISTRY.register("multiblock_gui_page_2", () -> IMenuTypeExtension.create(MultiblockGuiPage2Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiblockGuiPage3Menu>> MULTIBLOCK_GUI_PAGE_3 = REGISTRY.register("multiblock_gui_page_3", () -> IMenuTypeExtension.create(MultiblockGuiPage3Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiblockGuiPage4Menu>> MULTIBLOCK_GUI_PAGE_4 = REGISTRY.register("multiblock_gui_page_4", () -> IMenuTypeExtension.create(MultiblockGuiPage4Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiblockGuiPage5Menu>> MULTIBLOCK_GUI_PAGE_5 = REGISTRY.register("multiblock_gui_page_5", () -> IMenuTypeExtension.create(MultiblockGuiPage5Menu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<WarpPadGuiMenu>> WARP_PAD_GUI = REGISTRY.register("warp_pad_gui", () -> IMenuTypeExtension.create(WarpPadGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<PistonGenGUIMenu>> PISTON_GEN_GUI = REGISTRY.register("piston_gen_gui", () -> IMenuTypeExtension.create(PistonGenGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<SteamChamberGUIMenu>> STEAM_CHAMBER_GUI = REGISTRY.register("steam_chamber_gui", () -> IMenuTypeExtension.create(SteamChamberGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ItemElevatorGuiMenu>> ITEM_ELEVATOR_GUI = REGISTRY.register("item_elevator_gui", () -> IMenuTypeExtension.create(ItemElevatorGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<ItemElevatorGuiDownMenu>> ITEM_ELEVATOR_GUI_DOWN = REGISTRY.register("item_elevator_gui_down", () -> IMenuTypeExtension.create(ItemElevatorGuiDownMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CraftingFactoryGUIMenu>> CRAFTING_FACTORY_GUI = REGISTRY.register("crafting_factory_gui", () -> IMenuTypeExtension.create(CraftingFactoryGUIMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof CrystalnexusModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}