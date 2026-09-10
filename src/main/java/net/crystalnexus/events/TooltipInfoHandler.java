package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public class TooltipInfoHandler {

	private static final Map<String, String[]> TOOLTIP_DATA = new HashMap<>();

	static {

		// POWER & ENERGY GENERATION

		addTooltip("battery",
				"Stores energy for later use.",
				"Connect machines to this to buffer your power grid.");
		addTooltip("battery_cell",
				"Portable energy storage cell.",
				"Powers items in your inventory.");
		addTooltip("carbon_battery_cell",
				"Upgraded portable energy storage.",
				"Powers items in your inventory.");
		addTooltip("dense_battery_cell",
				"High-density portable energy storage.",
				"Powers items in your inventory.");
		addTooltip("dark_battery_cell",
				"Advanced portable energy storage.",
				"Powers items in your inventory.");
		addTooltip("ee_battery",
				"EE-based energy storage block.",
				"Connect machines to this to buffer your power grid.");

		addTooltip("steam_chamber",
				"Heats water with Blutonium to produce steam.",
				"Part of the steam power generation loop.");
		addTooltip("steam_collector",
				"Collects steam for power generation.",
				"Place above a Steam Chamber to collect steam.",
				"Use fluid pipes to send steam to Steam Engines.");
		addTooltip("steam_engine",
				"Converts steam into energy.",
				"Basic power generation from collected steam.");
		addTooltip("steam_engine_upgrade",
				"High Pressure Steam Engine.",
				"Increased power generation from collected steam.");

		addTooltip("piston_generator",
				"Generates energy using fuel.",
				"Insert fuel to start generation.");
		addTooltip("invert_piston_generator",
				"Generates energy using fuel.",
				"Insert fuel to start generation.");

		addTooltip("reactor_block",
				"Outer casing for the Reactor multiblock.",
				"Forms the walls of the reactor structure.");
		addTooltip("reactor_core",
				"Fuel-bearing core component.",
				"Every core column needs a Control Rod directly above it.");
		addTooltip("reactor_computer",
				"Master control block for the Reactor.",
				"Holds fuel and upgrades, and controls the reaction.");
		addTooltip("reactor_fluid_input",
				"Provides coolant to the Reactor.",
				"Connect Coolant Channels to this wall port.");
		addTooltip("reactor_energy_output",
				"Extracts energy from the Reactor.",
				"Integrate into the reactor wall for power output.");
		addTooltip("reactor_waste_output",
				"Outputs waste from the Reactor.",
				"Collects Blutonium Waste produced by the reaction.");
		addTooltip("reactor_control_rod",
				"Regulates the core column directly below it.",
				"Right-click to set insertion and throttle the reaction.");
		addTooltip("reactor_carbon_moderator",
				"Between cores, improves fuel efficiency and reduces heat.");
		addTooltip("reactor_neutron_reflector",
				"Next to a core, reflects neutrons to increase output.");
		addTooltip("reactor_coolant_channel",
				"Carries coolant from a connected Fluid Input.",
				"Removes reactor heat.");
		addTooltip("reactor_heat_conductor",
				"Extends cooling from the core to coolant channels.",
				"Works up to four blocks from the core.");
		addTooltip("reactor_upgrade",
				"Reactor energy upgrade.",
				"Boosts reactor energy production.");
		addTooltip("reactor_upgrade_permafrost",
				"Reactor Permafrost upgrade.",
				"Removes coolant requirement.");

		addTooltip("zero_point",
				"Infinite power.",
				"Core of the full Zero Point Multiblock.",
				"Outputs up to 1,024,000 FE/t per side.");
		addTooltip("zero_point_core",
				"Used to craft the Zero Point Block.");

		addTooltip("basic_energy_cable",
				"Basic energy transfer cable.",
				"Transfers energy to machines.");
		addTooltip("energy_cable",
				"Standard energy transfer cable.",
				"Transfers energy with medium throughput to machines.");
		addTooltip("energy_cable_mk_2",
				"Advanced energy transfer cable.",
				"Transfers energy with high throughput to machines.");
		addTooltip("cryogenically_cooled_energy_cable",
				"Cryogenically cooled high-capacity energy cable.",
				"Transfers up to 2,048,000 FE/t.");

		addTooltip("crystal_guide",
				"End Crystal beam guide.",
				"Redirects the End Crystal beam.");

		// RESOURCE PROCESSING

		addTooltip("crystal_crusher",
				"Crushes raw ores into dusts.",
				"Multiplies ore yield for better efficiency.");
		addTooltip("chlorophyte_crusher",
				"Chlorophyte-tier ore crusher.",
				"Processes recipes available to its machine tier.");
		addTooltip("invertium_crusher",
				"Invertium-tier ore crusher.",
				"Processes advanced crushing recipes faster.");
		addTooltip("hyper_crusher",
				"Hyper-tier ore crusher.",
				"Processes the highest-tier crushing recipes.");
		addTooltip("ore_processor",
				"Advanced ore processing plant.",
				"Processes raw ores.");
		addTooltip("parts_assembler",
				"Forms ingots into plates, rods, or bolts.",
				"Select the output shape from its GUI.",
				"Accepts machine upgrades in the side slot.");

		addTooltip("chlorophyte_smelter",
				"Specialized Chlorophyte furnace.",
				"Smelts dusts and raw materials into ingots.");
		addTooltip("iron_smelter",
				"Specialized iron-tier furnace.",
				"Smelts dusts and raw materials into ingots.");
		addTooltip("invertium_smelter",
				"Specialized Invertium furnace.",
				"Smelts dusts and raw materials into ingots.");
		addTooltip("crystal_smelter",
				"Specialized crystal furnace.",
				"Smelts dusts and raw materials into ingots.");
		addTooltip("ultima_smelter",
				"Multi-purpose high-tier furnace.",
				"Smelts multiple stacks of materials.");

		addTooltip("singularity_compressor",
				"Extreme high-pressure compressor.",
				"Condenses thousands of items into Singularities.");
		addTooltip("crystal_purifier",
				"Creates Crystalized Alloy and Nitrile.");
		addTooltip("chemical_reaction_chamber",
				"Combines base resources with reactants.",
				"Processes chemical reactions for materials.");
		addTooltip("fluid_chemical_reaction_chamber",
				"Combines fluids and items into advanced materials.",
				"Check JEI for valid tagged inputs and outputs.");
		addTooltip("refinery",
				"Refines processing fluids into useful materials.",
				"Basic refinery tier.");
		addTooltip("chlorophyte_refinery",
				"Chlorophyte-tier material refinery.",
				"Handles recipes available to its machine tier.");
		addTooltip("invertium_refinery",
				"Invertium-tier material refinery.",
				"Handles advanced refining recipes.");
		addTooltip("hyper_refinery",
				"Hyper-tier material refinery.",
				"Handles the highest-tier refining recipes.");

		addTooltip("reaction_chamber_computer",
				"Controls the Reaction Chamber multiblock.",
				"Converts energy into EE Matter.");
		addTooltip("reaction_chamber_block",
				"Outer casing for the Reaction Chamber.",
				"Forms the structure of the Reaction Chamber.");
		addTooltip("reaction_chamber_core",
				"Core of the Reaction Chamber.",
				"Central component for EE Matter production.");
		addTooltip("reaction_energy_input",
				"Energy input for Reaction Chamber.",
				"Supplies power to the Reaction Chamber.");

		addTooltip("circuit_press",
				"Stamps raw materials into circuits.",
				"Creates printed circuits and chips.");
		addTooltip("dust_separator",
				"Sifts through mixed dust.",
				"Separates dust into nuggets.");
		addTooltip("chlorophyte_dust_separator",
				"Chlorophyte-tier dust separator.",
				"Processes recipes available to its machine tier.");
		addTooltip("invertium_dust_separator",
				"Invertium-tier dust separator.",
				"Processes advanced separation recipes faster.");
		addTooltip("hyper_dust_separator",
				"Hyper-tier dust separator.",
				"Processes the highest-tier separation recipes.");
		addTooltip("matter_transmutation_table",
				"Endgame resource conversion block.",
				"Converts EE-matter into resources,",
				"and allows for advanced crafting.");

		// RESOURCE GATHERING

		addTooltip("quarry",
				"Automated laser mining machine.",
				"Automatically mines resources within a chunk.");
		addTooltip("quantum_miner",
				"Quantum resource extraction.",
				"Produces weighted common resources at high FE cost.");
		addTooltip("node_miner",
				"Mines from Ore Nodes.",
				"Slowly mines resources at the cost of power");
		addTooltip("node_extractor",
				"Extracts fluid from Fluid Nodes.",
				"Slowly extracts fluids at the cost of power.");

		addTooltip("iron_node",
				"Infinite Iron resource node.",
				"Extract with Node Miner.");
		addTooltip("gold_node",
				"Infinite Gold resource node.",
				"Extract with Node Miner.");
		addTooltip("copper_node",
				"Infinite Copper resource node.",
				"Extract with Node Miner.");
		addTooltip("ancient_debris_node",
				"Infinite Ancient Debris node.",
				"Extract with Node Miner.");
		addTooltip("lava_node",
				"Infinite Lava node.",
				"Extract with Node Extractor.");
		addTooltip("oil_node",
				"Infinite Oil node.",
				"Extract with Node Extractor.");

		addTooltip("conveyer_belt",
				"Transports items horizontally.",
				"Basic tier: half the original conveyor speed.");
		addTooltip("titanium_conveyer_belt",
				"Transports items horizontally.",
				"Titanium tier: twice the basic conveyor speed.");
		addTooltip("meteorite_conveyer_belt",
				"Transports items horizontally.",
				"Meteorite tier: twice the titanium conveyor speed.");
		addTooltip("conveyer_belt_input",
				"Inserts items from adjacent containers.",
				"Puts items onto conveyor system.");
		addTooltip("conveyer_belt_output",
				"Extracts items into containers.",
				"Pulls items from conveyor system.");
		addTooltip("item_elevator",
				"Moves items vertically upward.",
				"Safely transports items.");
		addTooltip("item_elevator_down",
				"Moves items vertically downward.",
				"Safely transports items.");
		addTooltip("smart_splitter",
				"Intelligent item routing.",
				"Routes items based on filters.");

		addTooltip("pipe_junction",
				"Multi-directional fluid transport.",
				"Connects fluid lines.");
		addTooltip("pipe_straight",
				"Straight fluid transport pipe.",
				"Transfers fluids between machines.");
		addTooltip("fluid_packager",
				"Packages fluids into cells.",
				"Enables manual fluid transport.");
		addTooltip("tank",
				"Mass fluid storage.",
				"Stores large quantities of fluids.");

		addTooltip("depot_uploader",
				"Wireless item upload station.",
				"Sends items to your personal Depot.");
		addTooltip("depot_downloader",
				"Wireless item download station.",
				"Retrieves items from your personal Depot.");
		addTooltip("depot_controller",
				"The powered center of your personal Depot system.",
				"Uses 20 FE/t base power; each connected Depot component adds 2 FE/t.");
		addTooltip("depot_cli",
				"Terminal for browsing and crafting from Depot storage.",
				"Connect it to a powered Depot Controller network.");
		addTooltip("depot_cable",
				"Links Depot Controllers to Depot components.",
				"Does not connect to energy cables or transfer FE.");
		addTooltip("crafting_upgrade",
				"Unlocks crafting in the Depot CLI.",
				"Add more processors to increase crafting speed.");
		addTooltip("crafting_core",
				"Adds an extra crafting process.",
				"Increases Depot Controller power draw.");
		addTooltip("depot_uplink",
				"Expandable wireless storage system.",
				"Transfers items wirelessly to your personal Depot.");
		addTooltip("depot_storage_upgrade",
				"Doubles depot storage capacity.");
		addTooltip("tesseract",
				"Endgame wireless transfer gateway.",
				"Transfers energy across any distance.");
		addTooltip("tesseract_output",
				"Tesseract output endpoint.",
				"Receives energy from linked Tesseract.");
		addTooltip("link_card",
				"Used to link machines or blocks.");

		// UTILITY

		addTooltip("crafting_factory",
				"Automated recipe crafter.",
				"Crafts assigned recipes.",
				"Ignores recipe shape.");
		addTooltip("factory_controller",
				"Energy controller for Machines.",
				"Supplies power to linked Machines.");
		addTooltip("factory_item_controller",
				"Item controller for Machines.",
				"Manages item input.");
		addTooltip("factory_output_controller",
				"Output controller for Machines.",
				"Manages item output.");
		addTooltip("machineblock",
				"General machine block for factory multiblocks.",
				"Connect it to the appropriate controllers.");
		addTooltip("machine_core",
				"Processing core for factory multiblocks.",
				"Install it inside the machine structure.");
		addTooltip("machine_energy_input",
				"Energy input for factory multiblocks.",
				"Connect FE cables to this block.");

		addTooltip("biomatic_composter",
				"Processes organic matter into biomass.",
				"Converts organic materials into fuel.");
		addTooltip("biomatic_simulator",
				"Simulates organic growth.",
				"Grows crops using energy.");
		addTooltip("biomatic_constructor",
				"Constructs organic compounds.",
				"Builds materials from biomass.");

		addTooltip("multiblock_research_station",
				"Multiblock layout research station.",
				"Shows multiblock structures.");
		addTooltip("block_placer",
				"Automated block placement.",
				"Places blocks on redstone pulse.");
		addTooltip("item_charger",
				"Charges energy-based items.",
				"Charges tools and batteries.");
		addTooltip("aoe_charger",
				"Area-of-effect item charger.",
				"Charges nearby inventory items in a radius.");
		addTooltip("temporal_exploiter",
				"Accelerates nearby block ticks using FE.",
				"Configure its range and operation from the GUI.");

		addTooltip("electromagnet",
				"Powers Particle Accelerator.",
				"Increases speed with more magnets.");
		addTooltip("item_collector",
				"Automated item pickup.",
				"Attracts nearby items.");

		addTooltip("container",
				"High density portable item storage.",
				"Large storage capacity.");
		addTooltip("blueprint_base",
				"Blueprint designer floor block.",
				"Build a complete flat floor with Blueprint Base.",
				"Everything saved must sit inside the framed interior volume.");
		addTooltip("blueprint_frame",
				"Blueprint designer frame block.",
				"Build four corner pillars from the floor and connect them across the top.",
				"Forms the save bounds for the blueprint designer.");
		addTooltip("blueprint_controller",
				"Blueprint designer controller.",
				"Attach it to a valid Blueprint Base and Frame structure.",
				"Open the GUI, enter a name, and save everything inside the blueprint volume.");

		// RESOURCES

		addTooltip("oil_fuel_cell",
				"Oil Fuel Cell.",
				"Refine into better fuels via processing.");
		addTooltip("gas_fuel_cell",
				"Gas Fuel Cell.",
				"Refined fuel for generators.");
		addTooltip("empty_fuel_cell",
				"Empty Fuel Cell.",
				"Fill at Fluid Packager.");
		addTooltip("overfuel_cell",
				"Overfuel Cell.",
				"High-energy fuel cell.");
		addTooltip("biomass",
				"Processed organic biomass.",
				"Fuel source from composting.");

		// EQUIPMENT

		addTooltip("compound_pickaxe",
				"Compound Paxel - all-in-one mining tool.",
				"Mines all block types. Uses 200 FE per block.");
		addTooltip("compound_sword",
				"Energy-powered combat weapon.",
				"High damage weapon. Uses 500 FE per hit.");
		addTooltip("mining_laser",
				"High-tech mining laser.",
				"Mines blocks remotely using battery energy.");
		addTooltip("paint_gun",
				"Paintball Gun.",
				"Fires paint to color blocks.");
		addTooltip("flamethrower",
				"Flamethrower weapon.",
				"Projects flames for combat.");
		addTooltip("ore_scanner",
				"Ore Scanner tool.",
				"Scans for ore deposits.");
		addTooltip("geiger_counter",
				"Radiation detector.",
				"Measures radiation levels.");
		addTooltip("build_gun",
				"Schematic builder and placement tool.",
				"Press the Buildgun Menu key to choose a saved blueprint.",
				"Shift Right Click loads placement mode.",
				"Shift Left Click toggles Default and Prefer Flat Ground placement modes.",
				"Prefer Flat Ground settles the whole schematic onto nearby support like a real placement.",
				"Scroll moves the preview. Hold Shift and scroll to rotate.",
				"Right Click places the loaded schematic.",
				"Shows required and missing materials on screen while preparing placement.",
				"Can pull materials from inventory, shulker boxes, and Container items you are carrying.",
				"Creative mode ignores material requirements.",
				"Placed storage blocks keep the block but do not restore stored item contents.");

		addTooltip("jet_pack_chestplate",
				"Jetpack - continuous thrust flight.",
				"Uses fuel for fast movement.");
		addTooltip("hover_pack_chestplate",
				"Hoverpack - stable flight control.",
				"Uses battery power for hovering.");

		addTooltip("acceleration_upgrade",
				"Machine Acceleration Upgrade.",
				"Increases processing speed.");
		addTooltip("carbon_acceleration_upgrade",
				"Carbon Acceleration Upgrade.",
				"Advanced speed boost.");
		addTooltip("fe_efficiency_upgrade",
				"Machine FE Efficiency Upgrade.",
				"Reduces power consumption.");
		addTooltip("carbon_fe_efficiency_upgrade",
				"Carbon FE Efficiency Upgrade.",
				"Advanced power reduction.");
		addTooltip("range_upgrade",
				"Machine Range Upgrade.",
				"Increases operational range.");
		addTooltip("carbon_range_upgrade",
				"Carbon Range Upgrade.",
				"Advanced range boost.");

		addTooltip("iron_singularity",
				"Compressed Iron Singularity.",
				"Dense advanced material.");
		addTooltip("gold_singularity",
				"Compressed Gold Singularity.",
				"Dense advanced material.");
		addTooltip("diamond_singularity",
				"Compressed Diamond Singularity.",
				"Dense advanced material.");
		addTooltip("copper_singularity",
				"Compressed Copper Singularity.",
				"Dense advanced material.");
		addTooltip("coal_singularity",
				"Compressed Coal Singularity.",
				"Dense advanced material.");
		addTooltip("quartz_singularity",
				"Compressed Quartz Singularity.",
				"Dense advanced material.");
		addTooltip("redstone_singularity",
				"Compressed Redstone Singularity.",
				"Dense advanced material.");
		addTooltip("energy_singularity",
				"Compressed Energy Singularity.",
				"Dense advanced material.");
		addTooltip("wood_singularity",
				"Contains 102,400 logs.",
				"Craft alone to extract a stack of oak logs.");
		addTooltip("stone_singularity",
				"Contains 102,400 stone blocks.",
				"Craft alone to extract a stack of stone.");
		addTooltip("dirt_singularity",
				"Contains 102,400 dirt blocks.",
				"Craft alone to extract a stack of dirt.");

		addTooltip("battery_part",
				"Battery Part.",
				"Component for battery construction.");

		addTooltip("ssd",
				"Randomized speed and FE efficiency.",
				"Installed in upgrade-capable machines.");
		addTooltip("rare_ssd",
				"Rare SSD - improved speed and FE efficiency.",
				"Better chance of strong bonuses.");
		addTooltip("epic_ssd",
				"Epic SSD - best speed and FE efficiency.",
				"Highest chance of powerful bonuses.");
		addTooltip("blank_ssd",
				"Blank SSD - unformatted.",
				"Format in Computation Node.");

		addTooltip("ee_matter",
				"EE Matter - energy-equivalent matter.",
				"Produced by Reaction Chambers.",
				"Used for advanced crafting.");
		addTooltip("unstable_ee_matter",
				"Unstable EE Matter.",
				"Highly volatile energy matter.");
		addTooltip("ee_matter_block",
				"Block of EE Matter.",
				"Compressed energy matter.");

		addTooltip("computation_cluster",
				"Computation Cluster.",
				"Decrypts SSD Upgrades.");

		addTooltip("extractinator",
				"Resource extraction machine.",
				"Sifts through loose sediment to find resources.");
		addTooltip("titanium_extractinator",
				"Titanium-tier resource extraction machine.",
				"Consumes 4x energy for twice the secondary-drop chance.");
		addTooltip("inverter",
				"Invertium Inverter.",
				"Inverts energy types.");
		addTooltip("energy_extractor",
				"Energy Extractor.",
				"Extracts energy from items.");
		addTooltip("battery_monitor",
				"Battery Monitor.",
				"Displays battery storage.");
		addTooltip("singularity_matrix",
				"Singularity Matrix.",
				"Converts items into EE Matter.");

		addTooltip("particle_accelerator_controller",
				"Particle Accelerator Controller.",
				"Accelerates items in a loop.");
		addTooltip("particle_accelerator_tube",
				"Particle Accelerator Tube.",
				"Forms acceleration loop.");

		addTooltip("turbine",
				"Power generation turbine.",
				"Converts steam into energy.");
		addTooltip("turbine_blade",
				"Turbine Blade.",
				"Component for turbines.");

		addTooltip("warp_pad",
				"Teleportation Warp Pad.",
				"Instant teleport between pads.");

		addTooltip("chlorophyte_accelerator",
				"Chlorophyte Accelerator.",
				"Speeds up crop growth using energy.");

		addTooltip("conductive_alloy",
				"Conductive Alloy.",
				"Used in energy systems and components.");
		addTooltip("crystalized_alloy_magnet",
				"Crystalized Alloy Magnet.",
				"Magnetic component for machines.",
				"Also attracts items when held in hand.");
		addTooltip("florathane",
				"Florathane compound.",
				"Powerful biofuel.");
		addTooltip("florathane_wand",
				"Florathane Wand.",
				"Applies growth acceleration.");
		addTooltip("fertilizer",
				"Fertilizer.",
				"Boosts crop growth in an AOE.");
	}

	private static void addTooltip(String registryName, String... lines) {
		TOOLTIP_DATA.put(registryName, lines);
	}

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!itemId.getNamespace().equals(CrystalnexusMod.MODID)) {
			return;
		}
		String path = itemId.getPath();
		String[] tooltipLines = TOOLTIP_DATA.get(path);
		if (tooltipLines == null) {
			return;
		}
		if (Screen.hasShiftDown()) {
			for (String line : tooltipLines) {
				event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
			}
		} else {
			event.getToolTip().add(
					Component.literal("Hold ").withStyle(ChatFormatting.DARK_GRAY)
							.append(Component.literal("[SHIFT]").withStyle(ChatFormatting.YELLOW))
							.append(Component.literal(" for info").withStyle(ChatFormatting.DARK_GRAY)));
		}
	}
}
