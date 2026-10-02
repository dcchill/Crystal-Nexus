package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.GradientItemName;
import net.crystalnexus.item.GradientItemName.Palette;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.processing.TieredMachineBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public class TooltipInfoHandler {

	private static final Map<String, String[]> TOOLTIP_DATA = new HashMap<>();
	private static Object cachedRecipes;
	private static Level cachedLevel;
	private static Map<Item, FrameColors> frameRecipeColors = Map.of();

	private record FrameColors(Palette palette) {
		Component tierLabel(MachineTier tier) {
			if (tier == MachineTier.HYPER) return tier.tierLabel();
			Component label = Component.translatable("tooltip.crystalnexus.machine_tier", tier.displayNumber());
			return label.copy().withStyle(style -> style.withColor(palette.midpoint()));
		}
	}

	private static FrameColors frameColors(Item item) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		if (!id.getNamespace().equals(CrystalnexusMod.MODID)) return null;
		return switch (id.getPath()) {
			case "machine_frame", "iron_machine_frame" -> new FrameColors(Palette.IRON);
			case "crystal_machine_frame" -> new FrameColors(Palette.ANCIENT_CRYSTAL);
			case "chlorophyte_machine_frame" -> new FrameColors(Palette.CHLOROPHYTE);
			case "invertium_machine_frame" -> new FrameColors(Palette.INVERTIUM);
			case "azurine_machine_frame" -> new FrameColors(Palette.AZURINE);
			case "carbon_machine_frame" -> new FrameColors(Palette.CARBON_FIBER);
			case "ferrosteel_machine_frame" -> new FrameColors(Palette.FERROSTEEL);
			case "obsidrax_machine_frame" -> new FrameColors(Palette.OBSIDRAX);
			case "hyper_machine_frame" -> new FrameColors(Palette.METEORITE_ALLOY);
			case "flesh_machine_frame" -> new FrameColors(Palette.ENGINEERED_FLESH);
			default -> null;
		};
	}

	private static Map<Item, FrameColors> frameRecipeColors(Level level) {
		Object recipes = level.getRecipeManager().getRecipes();
		if (cachedLevel != level || cachedRecipes != recipes) {
			Map<Item, FrameColors> colors = new HashMap<>();
				for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
					// check only crystalnexus
					if (!holder.id().getNamespace().equals(CrystalnexusMod.MODID)) {
						continue;
					}

					FrameColors frame = null;
				for (Ingredient ingredient : holder.value().getIngredients()) {
					for (ItemStack input : ingredient.getItems()) {
						frame = frameColors(input.getItem());
						if (frame != null) break;
					}
					if (frame != null) break;
				}
				if (frame != null) {
					ItemStack result = holder.value().getResultItem(level.registryAccess());
					if (!result.isEmpty()) colors.putIfAbsent(result.getItem(), frame);
				}
			}
			frameRecipeColors = colors;
			cachedRecipes = recipes;
			cachedLevel = level;
		}
		return frameRecipeColors;
	}

	static {
		addTooltip("maw", "Damages mobs standing on top and eats them when they die.", "Collects their drops and creates 1 biomass per mob.", "Passive: requires no energy.");
		addTooltip("hemochanter", "Holds 32 buckets (32,000 mB) of Blood.", "Each higher enchantment level costs more Blood and FE, and takes longer.", "Randomly raises an existing enchantment to level 32.");
		addTooltip("meteor_sword", "Hold Shift + right-click to charge, then release when ready.", "Each second adds a charge level, up to 5; your next successful hit consumes it.", "Each level doubles damage and FE use, up to 32x.", "Base hit costs 500 FE; a full charge costs 16,000 FE.", "Grants +2 blocks of reach and a wide forward sweep.", "Supports standard sword enchantments, including Sweeping Edge.");


		addTooltip("battery",
				"Stores energy.",
				"Connect machines to this to buffer your power grid.");
		addTooltip("battery_cell",
				"Portable energy storage cell.",
				"Automatically charges FE items in your inventory.");
		addTooltip("carbon_battery_cell",
				"Upgraded portable energy storage.",
				"Automatically charges FE items in your inventory.");
		addTooltip("dense_battery_cell",
				"High-density portable energy storage.",
				"Automatically charges FE items in your inventory.");
		addTooltip("dark_battery_cell",
				"Advanced portable energy storage.",
				"Automatically charges FE items in your inventory.");
		addTooltip("ee_battery",
				"High-capacity energy storage block.",
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
				"Holds fuel and upgrades, and controls the reactor.");
		addTooltip("reactor_waste_output",
				"Outputs waste from the Reactor.",
				"Collects Blutonium Waste produced by the reaction.");
		addTooltip("reactor_control_rod",
				"Regulates the fuel column directly below it.",
				"Right-click to adjust insertion and throttle the reaction.");
		addTooltip("reactor_carbon_moderator",
				"Place between reactor cores to moderate the fuel.",
				"Reduces heat and fuel efficiency while moderated.");
		addTooltip("reactor_neutron_reflector",
				"Place next to a reactor core.",
				"Reflects neutrons back into the core to increase output.");
		addTooltip("reactor_coolant_channel",
				"Carries coolant from a connected Fluid Input.",
				"Removes heat from adjacent fuel or connected conductors.");
		addTooltip("reactor_heat_conductor",
				"Relays core heat to connected coolant channels.",
				"Conductor chains can reach up to four blocks from a fuel rod.",
				"The coolant network still requires a Fluid Input.");
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
		addTooltip("energy_cable_mk_2",
				"Advanced energy transfer cable.",
				"Transfers energy with high throughput to machines.");


		addTooltip("crystal_crusher",
				"Crushes raw ores into dusts.");
		addTooltip("chlorophyte_crusher",
				"Tier 3 ore crusher.");
		addTooltip("invertium_crusher",
				"Tier 5 ore crusher.");
		addTooltip("hyper_crusher",
				"Tier 9 ore crusher.");
		addTooltip("ore_processor",
				"Advanced ore processing plant.",
				"Processes raw ores.");
		addTooltip("parts_assembler",
				"Forms ingots into plates, rods, or bolts.",
				"Select the output shape from its GUI.");

		addTooltip("chlorophyte_smelter",
				"Specialized Chlorophyte furnace.");
		addTooltip("iron_smelter",
				"Specialized Tier 1 furnace.");
		addTooltip("invertium_smelter",
				"Specialized Invertium furnace.");
		addTooltip("crystal_smelter",
				"Specialized crystal furnace.");
		addTooltip("ultima_smelter",
				"Multi-purpose high-tier furnace.",
				"Smelts multiple stacks of materials.");

		addTooltip("singularity_compressor",
				"Extreme high-pressure compressor.",
				"Condenses thousands of items into Singularities.");
		addTooltip("crystal_purifier",
				"PLACEHOLDER.");
		addTooltip("chemical_reaction_chamber",
				"Processes solid reactions for materials.");
		addTooltip("fluid_chemical_reaction_chamber",
				"Processes fluid reactions for materials.");
		addTooltip("refinery",
				"Refines processing fluids into materials.");
		addTooltip("chlorophyte_refinery",
				"Tier 3 material refinery.",
				"Refines processing fluids into materials.");
		addTooltip("invertium_refinery",
				"Tier 5 material refinery.",
				"Refines processing fluids into materials.");
		addTooltip("hyper_refinery",
				"Tier 9 material refinery.",
				"Refines processing fluids into materials.");

		addTooltip("reaction_chamber_computer",
				"Controls the Reaction Chamber multiblock.",
				"Converts energy into EE Matter.");
		addTooltip("reaction_chamber_block",
				"Outer casing for the Reaction Chamber.",
				"Forms the structure of the Reaction Chamber.");
		addTooltip("reaction_chamber_core",
				"Core of the Reaction Chamber.",
				"Central component for EE Matter production.");

		addTooltip("circuit_press",
				"Stamps raw materials into circuits.",
				"Creates printed circuits and chips.");
		addTooltip("gene_splicer",
				"Extracts biological materials from captured mobs.",
				"Put Biomass in the green slot",
				"and a filled Prison Cube in the middle slot.");
		addTooltip("dust_separator",
				"Separates dust into nuggets.");
		addTooltip("chlorophyte_dust_separator",
				"Tier 3 dust separator.",
				"Separates dust into nuggets.");
		addTooltip("invertium_dust_separator",
				"Tier 5 dust separator.",
				"Separates dust into nuggets.");
		addTooltip("hyper_dust_separator",
				"Tier 9 dust separator.",
				"Separates dust into nuggets.");
		addTooltip("matter_transmutation_table",
				"Matter conversion block.",
				"Allows for advanced crafting.");


		addTooltip("quarry",
				"Automated laser mining machine.",
				"Automatically mines resources within a chunk.");
		addTooltip("quantum_miner",
				"PLACEHOLDER.");
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
				"Transports items.");
		addTooltip("azurine_conveyer_belt",
				"Transports items faster.");
		addTooltip("meteorite_conveyer_belt",
				"Transports items fastest.");
		addTooltip("conveyer_belt_input",
				"Inserts items from adjacent containers.",
				"Puts items onto conveyor system.");
		addTooltip("conveyer_belt_output",
				"Extracts items into containers.",
				"Pulls items from conveyor system.");
		addTooltip("item_elevator",
				"Moves items vertically upward.");
		addTooltip("item_elevator_down",
				"Moves items vertically downward.");
		addTooltip("smart_splitter",
				"Intelligent item routing.",
				"Routes items based on filters.");

		addTooltip("pipe_junction",
				"Multi-directional fluid transport.",
				"Connects fluid lines.");
		addTooltip("pipe_straight",
				"Fluid transport pipe.",
				"Transfers fluids between machines.");
		addTooltip("copper_fluid_pipe",
				"Basic fluid transport pipe.",
				"Transfers fluids between machines.");
		addTooltip("fluid_packager",
				"Packages fluids into cells.",
				"Enables fluid transport in stackable item form.");
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
				"The powered center of your personal Depot system.");
		addTooltip("depot_cli",
				"Terminal for browsing and crafting from Depot storage.",
				"Allows for advanced automation and control of your Depot storage.");
		addTooltip("depot_cable",
				"Links Depot Controllers to Depot components.",
				"Right click with wrench to set Import or Export modes to basic storage.",
				"Right click with hand to change settings.");
		addTooltip("crafting_upgrade",
				"Unlocks crafting in the Depot CLI.",
				"Add more processors to increase crafting speed.");
		addTooltip("crafting_core",
				"Adds an extra crafting process.");
		addTooltip("depot_uplink",
				"Transfers items wirelessly to your personal Depot.");
		addTooltip("depot_storage_upgrade",
				"Doubles depot storage capacity.");
		addTooltip("tesseract",
				"Wireless transfer.",
				"Transfers energy across any distance.");
		addTooltip("tesseract_output",
				"Tesseract output endpoint.",
				"Receives energy from linked Tesseract.");
		addTooltip("link_card",
				"Used to link machines or blocks.");


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
				"PLACEHOLDER.");
		addTooltip("machine_core",
				"PLACEHOLDER.");
		addTooltip("machine_energy_input",
				"Energy input for factory multiblocks.",
				"Connect FE cables to this block.");
		addTooltip("solar_simulator_controller",
				"Generates resources from Planets.",
				"Or generates FE from stars, using a Dyson Sphere.",
				"Different Stars effect resource and FE generation.",
				"Dyson integrity decays over time and lowers output. Insert a Dyson Repair Kit to restore it.");

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
				"Accelerates facing block ticks using Temporal Essence.");

		addTooltip("electromagnet",
				"Powers Particle Accelerator.",
				"Increases speed with more magnets.");
		addTooltip("item_collector",
				"Automated item pickup.",
				"Attracts nearby items.");

		addTooltip("container",
				"High density portable item storage.");
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
				"Must be outside the blueprint build volume to function.");


		addTooltip("oil_fuel_cell",
				"Oil Fuel Cell.");
		addTooltip("gas_fuel_cell",
				"Gas Fuel Cell.");
		addTooltip("empty_fuel_cell",
				"Empty Fuel Cell.");
		addTooltip("overfuel_cell",
				"Overfuel Cell.");
		addTooltip("dark_matter_fuel_cell",
				"Weak alone; resonates with every Dark Matter Fuel Cell in the reactor.",
				"Total FE output from these cells grows with the square of their count.");
		addTooltip("biomass",
				"Processed organic biomass.",
				"Fuel source from composting.");
		addTooltip("dyson_repair",
				"Repairs Dyson integrity in Dyson mode.",
				"Each point of durability restores one point of integrity.");


		addTooltip("prison_cube",
				"Captures and transports one mob at a time.",
				"Right-click a mob with an empty cube to begin sealing.",
				"Keep the cube in hand and stay nearby until it closes.",
				"Shift + right-click a block with room to release the mob.",
				"Insert a filled cube into a Gene Splicer to process its mob.");

		addTooltip("compound_pickaxe",
				"Omni-tool that uses energy to mine blocks.");
		addTooltip("compound_sword",
				"Sword that uses energy to attack.");
		addTooltip("mining_laser",
				"Mines blocks using stored energy.");
		addTooltip("paint_gun",
				"Fires paint to color blocks.");
		addTooltip("flamethrower",
				"PLACEHOLDER.");
		addTooltip("ore_scanner",
				"Scans for ore deposits.",
				"Right Click to scan for ores.",
				"Shift Right Click to set a filter.");
		addTooltip("geiger_counter",
				"Measures radiation levels.");
		addTooltip("build_gun",
				"Schematic builder and placement tool.",
				"Press the Buildgun Menu key to choose a saved blueprint.",
				"Shift Right Click loads placement mode.",
				"Shift Left Click toggles Default and Prefer Flat Ground placement modes.",
				"Prefer Flat Ground settles the whole schematic onto nearby support.",
				"Scroll moves the placement. Hold Shift and scroll to rotate.",
				"Right Click places the loaded schematic.",
				"Shows required and missing materials on screen while preparing placement.",
				"Can pull materials from inventory, shulker boxes, and Container items you are carrying.",
				"Placed storage blocks keep the block but do not restore stored item contents.");
		addTooltip("structure_tracker",
				"Locates a selected world structure or Resource Meteor.",
				"Right Click scans for the selected structure using 5,000 FE.",
				"The held compass uses 1 FE per tick after a successful scan.",
				"Shift Right Click changes the selected structure.");

		addTooltip("jet_pack_chestplate",
				"Jetpack - continuous thrust flight.",
				"Uses fuel for fast movement.");
		addTooltip("hover_pack_chestplate",
				"Hoverpack - stable flight control.",
				"Uses stored FE for hovering.");

		addTooltip("acceleration_upgrade",
				"Machine Acceleration Upgrade.",
				"Increases processing speed.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");
		addTooltip("parallelization_chip",
				"Machine Parallelization Chip.",
				"Runs two crafts per chip in compatible machines.",
				"Stack up to 4 in one slot.");
		addTooltip("carbon_acceleration_upgrade",
				"Carbon Acceleration Upgrade.",
				"Advanced speed boost.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");
		addTooltip("fe_efficiency_upgrade",
				"Machine FE Efficiency Upgrade.",
				"Reduces power consumption.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");
		addTooltip("carbon_fe_efficiency_upgrade",
				"Carbon FE Efficiency Upgrade.",
				"Advanced power reduction.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");
		addTooltip("range_upgrade",
				"Machine Range Upgrade.",
				"Increases operational range.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");
		addTooltip("carbon_range_upgrade",
				"Carbon Range Upgrade.",
				"Advanced range boost.",
				"Stack up to 16 in one slot; bonuses taper at higher counts.");

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
		addTooltip("azurine_extractinator",
				"Tier 5 resource extraction machine.",
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
		var highLevelEnchants = EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().stream()
			.filter(entry -> entry.getIntValue() > 255)
			.map(entry -> entry.getKey().value().description().getString()).toList();
		for (int i = 0; i < event.getToolTip().size(); i++) {
			Component line = event.getToolTip().get(i);
			if (highLevelEnchants.stream().anyMatch(name -> line.getString().startsWith(name)))
				event.getToolTip().set(i, GradientItemName.rainbow(line));
		}
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String path = itemId.getPath();
		boolean tierX = itemId.getNamespace().equals(CrystalnexusMod.MODID)
				&& (path.equals("zero_point") || path.equals("zero_point_core") || path.equals("zero_star") || path.equals("bear"));
		FrameColors colors = null;
		if (itemId.getNamespace().equals(CrystalnexusMod.MODID)) {
			if (path.equals("flesh_block") || path.equals("flesh_machine_frame") || path.equals("maw")
					|| path.equals("hemochanter") || path.equals("hemolyzer") || path.equals("gene_splicer")) {
				colors = new FrameColors(Palette.ENGINEERED_FLESH);
			} else if (path.equals("singularity_compressor") || path.equals("matter_transmutation_table")
					|| path.equals("ee_matter") || path.equals("ee_matter_block")
					|| path.equals("compound_e") || path.equals("compound_pickaxe")
					|| path.equals("compound_sword") || path.equals("carbon_battery_cell")) {
				colors = new FrameColors(Palette.EE_MATTER);
			} else if (!tierX) {
				colors = frameColors(stack.getItem());
				Level level = Minecraft.getInstance().level;
				if (colors == null && level != null) colors = frameRecipeColors(level).get(stack.getItem());
			}
		}
		MachineTier tier = stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredMachineBlock machine
				? machine.machineTier() : null;
		if (tier != null) {
			event.getToolTip().add(Math.min(1, event.getToolTip().size()),
					colors == null ? tier.tierLabel() : colors.tierLabel(tier));
		}
		if (tierX) {
			event.getToolTip().add(Math.min(1, event.getToolTip().size()),
					GradientItemName.rainbow(Component.translatable("tooltip.crystalnexus.machine_tier_x")));
		}
		if (!itemId.getNamespace().equals(CrystalnexusMod.MODID)) {
			return;
		}
		if (!stack.has(DataComponents.CUSTOM_NAME) && !event.getToolTip().isEmpty()
				&& !path.equals("meteor_sword") && !path.equals("solaris")) {
			if (tierX) {
				event.getToolTip().set(0, GradientItemName.rainbow(event.getToolTip().get(0)));
			} else {
				Palette palette = colors != null ? colors.palette() : tier == null ? materialPalette(path) : tier.primaryPalette();
				if (palette != null) {
					Component name = event.getToolTip().get(0);
					event.getToolTip().set(0, GradientItemName.gradient(name, palette));
				}
			}
		}
		String[] tooltipLines = TOOLTIP_DATA.get(path);
		if (tooltipLines == null) return;
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

	private static Palette materialPalette(String path) {
		if (path.contains("blutonium") || path.equals("blu_tnt")) return Palette.BLUTONIUM;
		if (path.contains("crystalized_alloy") || path.startsWith("cystalized_")
				|| path.startsWith("crystalalloy_") || path.startsWith("crystal_alloy_")) return Palette.ANCIENT_CRYSTAL;
		if (path.startsWith("hyper_")) return Palette.METEORITE_ALLOY;
		if (path.contains("ferrosteel") || path.contains("titanium_carbide")) return Palette.FERROSTEEL;
		if (path.contains("obsidrax")) return Palette.OBSIDRAX;
		if (path.contains("meteorite")) return Palette.METEORITE_ALLOY;
		if (path.contains("chlorophyte") || path.equals("circuit_press") || path.equals("energy_extractor")) return Palette.CHLOROPHYTE;
		if (path.contains("invertium") || path.equals("inverter") || path.equals("invert_piston_generator")) return Palette.INVERTIUM;
		if (path.contains("azurine") || path.contains("titanium") || path.equals("tank") || path.equals("jet_pack_chestplate")) return Palette.AZURINE;
		if (path.contains("ancient_crystal") || path.startsWith("raw_crystal")
				|| path.startsWith("crystal_") && !path.startsWith("crystal_alloy")) return Palette.ANCIENT_CRYSTAL;
		if (path.startsWith("carbon_") || path.startsWith("raw_carbon") || path.contains("_carbon_")) return Palette.CARBON_FIBER;
		return null;
	}
}
