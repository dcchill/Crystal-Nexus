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

/**
 * Centralized "Hold [SHIFT] for info" tooltip handler for all Crystal Nexus
 * items and blocks.
 * Tooltip descriptions sourced from tutorial.md.
 * Registered automatically on the NeoForge GAME event bus (client-side only).
 */
@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public class TooltipInfoHandler {

	private static final Map<String, String[]> TOOLTIP_DATA = new HashMap<>();

	static {
		// =============================================
		// 1. POWER & ENERGY GENERATION
		// =============================================
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

		// Steam Power
		addTooltip("steam_chamber",
				"Heats water to produce steam.",
				"Part of the steam power generation loop.");
		addTooltip("steam_collector",
				"Collects steam for power generation.",
				"Pair with a Steam Engine to convert steam into energy.");
		addTooltip("steam_engine",
				"Converts steam into usable energy.",
				"Basic power generation from collected steam.");
		addTooltip("steam_engine_upgrade",
				"High Pressure Steam Engine.",
				"Increases steam-to-energy conversion rate.");

		// Piston Generators
		addTooltip("piston_generator",
				"Generates energy using gasoline.",
				"Place fuel or required inputs to start generation.");
		addTooltip("invert_piston_generator",
				"Generates energy using gasoline.",
				"Higher energy output using Invertium technology.");

		// Reactor (Multiblock)
		addTooltip("reactor_block",
				"Outer casing for the Reactor multiblock.",
				"Forms the walls of the reactor structure.");
		addTooltip("reactor_core",
				"Core component of the Reactor multiblock.",
				"Place internally within the reactor structure.");
		addTooltip("reactor_computer",
				"Master control block for the Reactor.",
				"Right-click to manage the reaction process.",
				"Requires Reactor Blocks, Core, Fluid Input, and Energy Output.");
		addTooltip("reactor_fluid_input",
				"Provides coolant to the Reactor.",
				"Integrate into the reactor wall for fluid input.");
		addTooltip("reactor_energy_output",
				"Extracts energy from the Reactor.",
				"Integrate into the reactor wall for power output.");
		addTooltip("reactor_waste_output",
				"Outputs waste from the Reactor.",
				"Handles byproducts of the reactor process.");
		addTooltip("reactor_upgrade",
				"Reactor speed upgrade.",
				"Boosts reactor energy output.");
		addTooltip("reactor_upgrade_permafrost",
				"Reactor Permafrost upgrade.",
				"Removes coolant requirement.");

		// Zero Point (Multiblock)
		addTooltip("zero_point",
				"Infinite power.",
				"Used to craft the Zero Point Core.");
		addTooltip("zero_point_core",
				"Core for the Zero Point multiblock.",
				"Infinite power generation.");

		// Energy Cables
		addTooltip("basic_energy_cable",
				"Basic energy transfer cable.",
				"Transfers energy from generators to machines.");
		addTooltip("energy_cable",
				"Standard energy transfer cable.",
				"Transfers energy from generators to machines.");
		addTooltip("energy_cable_mk_2",
				"Advanced energy transfer cable.",
				"Higher throughput energy transfer.");
		addTooltip("energy_splitter",
				"Splits energy into multiple directions.",
				"Divide power lines.");
		addTooltip("energy_refractor",
				"Redirects energy flow.",
				"Route energy in specific directions.");
		addTooltip("conductive_energy_guide",
				"Upgraded energy routing through the air.",
				"Guides energy along a beam.");
		addTooltip("conductive_energy_refractor",
				"Upgraded energy redirector.",
				"Routes conductive energy in specific directions.");
		addTooltip("conductive_energy_splitter",
				"Upgraded energy splitter.",
				"Divide conductive power into multiple directions.");
		addTooltip("crystal_energy_guide",
				"Energy routing through the air.",
				"Guides energy along a beam.");
		addTooltip("crystal_guide",
				"Crystal energy guide block.",
				"Routes crystal energy in specific directions.");

		// =============================================
		// 2. RESOURCE PROCESSING MACHINES
		// =============================================
		addTooltip("crystal_crusher",
				"Crushes raw ores into dusts.",
				"Multiplies ore yield for better efficiency.");
		addTooltip("ore_processor",
				"Advanced ore processing plant.",
				"Processes raw ores.");
		addTooltip("chlorophyte_smelter",
				"Specialized Chlorophyte furnace.",
				"Smelts dusts and raw materials.");
		addTooltip("iron_smelter",
				"Specialized iron-tier furnace.",
				"Smelts basic dusts and materials.");
		addTooltip("invertium_smelter",
				"Specialized Invertium furnace.",
				"Smelts dusts and raw materials.");
		addTooltip("crystal_smelter",
				"Specialized crystal furnace.",
				"Smelts dusts and raw materials.");
		addTooltip("ultima_smelter",
				"Multi-purpose high-tier furnace.",
				"Smelts multiple stacks of dusts and raw materials.");
		addTooltip("metallurgic_recrystallizer",
				"Recrystallizes materials.",
				"Reverts ingots back to raw material.");
		addTooltip("singularity_compressor",
				"Extreme high-pressure compressor.",
				"Condenses thousands of items into Singularities.");
		addTooltip("crystal_purifier",
				"Purifies and upgrades energy crystals.",
				"Produces clear usable energy crystals.");
		addTooltip("chemical_reaction_chamber",
				"Combines base resources with reactants.",
				"Processes chemical reactions for materials.");
		addTooltip("reaction_chamber_computer",
				"Controls the Reaction Chamber multiblock.",
				"Converts energy into EE Matter.");
		addTooltip("reaction_chamber_block",
				"Outer casing for the Reaction Chamber.",
				"Forms the structure of the Reaction Chamber multiblock.");
		addTooltip("reaction_chamber_core",
				"Core of the Reaction Chamber.",
				"Central component for EE Matter production.");
		addTooltip("reaction_energy_input",
				"Energy input for Reaction Chamber.",
				"Supplies power to the Reaction Chamber multiblock.");
		addTooltip("circuit_press",
				"Stamps raw materials into circuits.",
				"Creates printed circuits and chips for machine components.");
		addTooltip("dust_separator",
				"Sifts through mixed dust or waste.",
				"Separates useful trace minerals from extracted dust.");
		addTooltip("matter_transmutation_table",
				"Endgame resource conversion block.",
				"Converts energy or EE-matter directly into resources,",
				"and allows for advanced crafting");

		// =============================================
		// 3. RESOURCE GATHERING & LOGISTICS
		// =============================================
		addTooltip("quarry",
				"Automated laser mining machine.",
				"Automatically mines resources within a chunk.");
		addTooltip("quantum_miner",
				"Quantum resource extraction.",
				"Pulls resources from \"another dimension\" (energy intensive).");
		addTooltip("node_miner",
				"Mines from resource Nodes.",
				"Place on specific node deposits for continuous mining.");
		addTooltip("node_extractor",
				"Targets infinite ore Nodes.",
				"Slowly extracts resources at the cost of power.");

		// Nodes
		addTooltip("iron_node",
				"Infinite Iron resource node.",
				"Extract with a Node Miner or Node Extractor.");
		addTooltip("gold_node",
				"Infinite Gold resource node.",
				"Extract with a Node Miner or Node Extractor.");
		addTooltip("copper_node",
				"Infinite Copper resource node.",
				"Extract with a Node Miner or Node Extractor.");
		addTooltip("ancient_debris_node",
				"Infinite Ancient Debris node.",
				"Extract with a Node Miner or Node Extractor.");
		addTooltip("lava_node",
				"Infinite Lava resource node.",
				"Extract with a Node Miner or Node Extractor.");
		addTooltip("oil_node",
				"Infinite Oil resource node.",
				"Extract with a Node Miner or Node Extractor.");

		// Conveyor System
		addTooltip("conveyer_belt",
				"Transports items horizontally.",
				"Drop items on it to move them automatically.");
		addTooltip("conveyer_belt_input",
				"Inserts items from adjacent containers.",
				"Pushes items onto the conveyor belt system.");
		addTooltip("conveyer_belt_output",
				"Extracts items into adjacent containers.",
				"Pulls items from conveyor into machines or chests.");
		addTooltip("item_elevator",
				"Moves items vertically upward.",
				"Safely transports items.");
		addTooltip("item_elevator_down",
				"Moves items vertically downward.",
				"Safely transports items.");
		addTooltip("smart_splitter",
				"Intelligent item routing.",
				"Routes items based on configured filters.");

		// Pipes & Fluids
		addTooltip("pipe_junction",
				"Multi-directional fluid transport.",
				"Connects fluid lines from multiple directions.");
		addTooltip("pipe_straight",
				"Straight fluid transport pipe.",
				"Transfers fluids between tanks and machines.");
		addTooltip("fluid_packager",
				"Packages fluids into cells.",
				"Cans fluids for manual transport or crafting.");
		addTooltip("tank",
				"Mass fluid storage.",
				"Stores large quantities of fluids.");

		// Remote Item Transfer
		addTooltip("depot_uploader",
				"Wireless item upload station.",
				"Pushes items to linked Depots over long distances instantly.");
		addTooltip("depot_downloader",
				"Wireless item download station.",
				"Pulls items from linked Depots over long distances instantly.");
		addTooltip("depot_uplink",
				"Expandable wireless storage system.",
				"Wirelessly transfers blocks and items to your inventory.");
		addTooltip("depot_storage_upgrade",
				"Expands depot storage capacity.",
				"Doubles the storage of connected depot systems.");
		addTooltip("tesseract",
				"Endgame wireless transfer gateway.",
				"Transfers Energy across any distance.");
		addTooltip("tesseract_output",
				"Tesseract output endpoint.",
				"Receives Energy from a linked Tesseract.");
		addTooltip("link_card",
				"Used to link machines or blocks together.");

		// =============================================
		// 4. UTILITY & CRAFTING
		// =============================================
		addTooltip("crafting_factory",
				"Automated recipe crafter",
				"Crafts assigned recipe",
				"Ignores crafting shape restriciotns");
		addTooltip("factory_controller",
				"Energy controller for Machines.",
				"Supplies power to linked Machines.");
		addTooltip("factory_item_controller",
				"Item controller for Machines.",
				"Manages item input for linked Machines.");
		addTooltip("factory_output_controller",
				"Output controller for Machines.",
				"Manages item output from linked Machines.");
		addTooltip("biomatic_composter",
				"Processes organic matter into biomass/fuel.",
				"Converts organic materials into usable biomass.");
		addTooltip("biomatic_simulator",
				"Simulates organic compound growth.",
				"Grows wood or crops using power instead of farming.");
		addTooltip("biomatic_constructor",
				"Constructs organic compounds.",
				"Builds complex organic materials from biomass using power.");
		addTooltip("multiblock_research_station",
				"Multiblock layout research station.",
				"Unlock and view multiblock shapes required for progression.");
		addTooltip("block_placer",
				"Automated block placement.",
				"Places blocks in the facing direction on redstone pulse.");
		addTooltip("item_charger",
				"Charges energy-based items.",
				"Charge tools, batteries, jetpacks, and hoverpacks inside it.");
		addTooltip("aoe_charger",
				"Area-of-effect item charger.",
				"Charges energy items in a radius around it.");
		addTooltip("electromagnet",
				"Powers the Particle Accelerator.",
				"The more Electromagnets, the faster the crafting speed.");
		addTooltip("item_collector",
				"Automated item pickup.",
				"Attracts and picks up dropped items.");

		// =============================================
		// 5. STORAGE
		// =============================================
		addTooltip("container",
				"High density portable item storage.",
				"Large storage capacity in a single block.");

		// =============================================
		// 6. SPECIAL RESOURCES
		// =============================================
		addTooltip("crude_oil_bucket",
				"Bucket of Crude Oil.",
				"Process into gasoline or fuels via Chemical Reaction Chamber.");
		addTooltip("gasoline_bucket",
				"Bucket of Gasoline.",
				"Refined fuel for power generation.");
		addTooltip("oil_fuel_cell",
				"Oil Fuel Cell.",
				"Portable oil fuel for generators.");
		addTooltip("gas_fuel_cell",
				"Gas Fuel Cell.",
				"Portable gas fuel for generators.");
		addTooltip("empty_fuel_cell",
				"Empty Fuel Cell.",
				"Fill with oil or gas at a Fluid Packager.");
		addTooltip("overfuel_cell",
				"Overfuel Cell.",
				"High-energy fuel cell for advanced generators.");
		addTooltip("biomass",
				"Processed organic biomass.",
				"Fuel source produced by Biomatic Composter.");

		// =============================================
		// 7. EQUIPMENT & ITEMS
		// =============================================
		// Tools & Combat
		addTooltip("compound_pickaxe",
				"Compound Paxel \u2014 all-in-one mining tool.",
				"Mines all block types. Requires battery power in inventory.");
		addTooltip("compound_sword",
				"Compound Sword \u2014 energy-powered weapon.",
				"High-damage combat weapon. Requires battery power.");
		addTooltip("mining_laser",
				"High-tech ranged mining laser.",
				"Mines blocks at a distance using energy.");
		addTooltip("paint_gun",
				"Paintball Gun.",
				"Fires colored paintballs to dye blocks.");
		addTooltip("flamethrower",
				"Flamethrower weapon.",
				"Projects flames for combat and utility.");
		addTooltip("crystal_extractor",
				"Crystal Extractor tool.",
				"Extracts crystals from crystal formations.");
		addTooltip("ore_scanner",
				"Ore Scanner tool.",
				"Scans for nearby ore deposits underground.");
		addTooltip("geiger_counter",
				"Radiation detector.",
				"Measures radiation levels in the environment.");

		// Armor & Mobility
		addTooltip("jet_pack_chestplate",
				"Jetpack \u2014 continuous thrust flight.",
				"Uses internal fuel for vertical and forward thrust.",
				"Great for fast movement and scaling heights.");
		addTooltip("hover_pack_chestplate",
				"Hoverpack \u2014 stable gravity-defying hover.",
				"Provides fine mid-air control for building.",
				"Uses energy from battery items in inventory.");

		// Machine Upgrades
		addTooltip("acceleration_upgrade",
				"Machine Acceleration Upgrade.",
				"Increases processing speed of compatible machines.");
		addTooltip("carbon_acceleration_upgrade",
				"Carbon Acceleration Upgrade.",
				"Advanced speed boost for compatible machines.");
		addTooltip("efficiency_upgrade",
				"Machine Efficiency Upgrade.",
				"Reduces power consumption of compatible machines.");
		addTooltip("carbon_efficiency_upgrade",
				"Carbon Efficiency Upgrade.",
				"Advanced power reduction for compatible machines.");
		addTooltip("range_upgrade",
				"Machine Range Upgrade.",
				"Increases operational range of logistics blocks.");
		addTooltip("carbon_range_upgrade",
				"Carbon Range Upgrade.",
				"Advanced range boost for logistics blocks.");

		// Crystals & Singularities
		addTooltip("destabilized_crystal",
				"Destabilized Crystal.",
				"An unstable crystal used in advanced crafting.");
		addTooltip("stable_crystal",
				"Stable Crystal.",
				"A stabilized crystal for mid-tier crafting.");
		addTooltip("controlled_crystal",
				"Controlled Crystal.",
				"A precisely controlled crystal for advanced recipes.");
		addTooltip("regulated_crystal",
				"Regulated Crystal.",
				"A finely regulated crystal for high-tier crafting.");
		addTooltip("ultimate_crystal",
				"Ultimate Crystal.",
				"Top-tier crystal for endgame crafting.");
		addTooltip("godlike_crystal",
				"Godlike Crystal.",
				"The most powerful crystal \u2014 used for endgame components.");
		addTooltip("dragon_crystal",
				"Dragon Crystal.",
				"A rare crystal imbued with draconic energy.");
		addTooltip("iron_singularity",
				"Compressed Iron Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("gold_singularity",
				"Compressed Gold Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("diamond_singularity",
				"Compressed Diamond Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("copper_singularity",
				"Compressed Copper Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("coal_singularity",
				"Compressed Coal Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("quartz_singularity",
				"Compressed Quartz Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("redstone_singularity",
				"Compressed Redstone Singularity.",
				"Dense crafting material for endgame components.");
		addTooltip("energy_singularity",
				"Compressed Energy Singularity.",
				"Dense crafting material for endgame components.");

		// Batteries & Cells
		addTooltip("battery_part",
				"Battery Part.",
				"Component used to craft portable batteries.");

		// SSDs & Data Storage
		addTooltip("ssd",
				"Solid State Drive.",
				"Stores digital patterns and recipes for autocrafting machines.");
		addTooltip("rare_ssd",
				"Rare SSD \u2014 increased data capacity.",
				"Higher data storage for computational machines.");
		addTooltip("epic_ssd",
				"Epic SSD \u2014 maximum data capacity.",
				"Dramatically increases processing abilities of machines.");
		addTooltip("blank_ssd",
				"Blank SSD \u2014 unformatted.",
				"Format in a machine to create a usable SSD.");

		// EE Matter
		addTooltip("ee_matter",
				"EE Matter \u2014 energy-equivalent matter.",
				"Key material produced by Reaction Chambers.",
				"Used in endgame crafting and transmutation.");
		addTooltip("unstable_ee_matter",
				"Unstable EE Matter.",
				"Volatile form of EE Matter. Handle with care.");
		addTooltip("ee_matter_block",
				"Block of EE Matter.",
				"Compressed EE Matter for storage or building.");

		// Computation
		addTooltip("computation_node",
				"Computation Node.",
				"Processing component for the Computation Cluster.");
		addTooltip("computation_cluster",
				"Computation Cluster.",
				"High-performance computing block for complex automation.");

		// Misc Machines & Blocks
		addTooltip("extractinator",
				"Resource extraction machine.",
				"Processes raw materials into refined outputs.");
		addTooltip("crystal_accepter",
				"Crystal Accepter.",
				"Accepts and processes crystal inputs.");
		addTooltip("inverter",
				"Energy Inverter.",
				"Inverts or converts energy types.");
		addTooltip("energy_extractor",
				"Energy Extractor.",
				"Converts item-stored energy into grid energy.");
		addTooltip("battery_monitor",
				"Battery Monitor.",
				"Displays power status of connected batteries.");
		addTooltip("singularity_matrix",
				"Singularity Matrix.",
				"Converts items into EE Matter.");
		addTooltip("particle_accelerator_controller",
				"Particle Accelerator Controller.",
				"Controls the particle acceleration process.");
		addTooltip("particle_accelerator_tube",
				"Particle Accelerator Tube.",
				"Forms the ring of the Particle Accelerator.");
		addTooltip("turbine",
				"Power generation turbine.",
				"Converts steam from blutonium into usable energy.");
		addTooltip("turbine_blade",
				"Turbine Blade.",
				"Component used to craft Turbines.");
		addTooltip("warp_pad",
				"Teleportation Warp Pad.",
				"Instantly teleport between linked Warp Pads.");

		// Crafting Materials
		addTooltip("conductive_alloy",
				"Conductive Alloy ingot.",
				"Used to craft conductive energy cables and components.");
		addTooltip("crystalized_alloy",
				"Crystalized Alloy ingot.",
				"Refined alloy used in crystal-tier crafting.");
		addTooltip("crystalized_alloy_magnet",
				"Crystalized Alloy Magnet.",
				"Magnetic component for item collection systems.");
		addTooltip("florathane",
				"Florathane compound.",
				"Organic growth accelerant for plant-based systems.");
		addTooltip("florathane_wand",
				"Florathane Wand.",
				"Applies Florathane to grow plants instantly.");
		addTooltip("fertilizer",
				"Fertilizer.",
				"Boosts crop growth when applied.");
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
