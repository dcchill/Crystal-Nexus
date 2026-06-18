package net.crystalnexus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CrystalnexusConfig {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final Recipes RECIPES;
	public static final Machines MACHINES;
	public static final Items ITEMS;
	public static final ModConfigSpec SPEC;

	static {
		RECIPES = new Recipes(BUILDER);
		MACHINES = new Machines(BUILDER);
		ITEMS = new Items(BUILDER);
		SPEC = BUILDER.build();
	}

	private CrystalnexusConfig() {
	}

	private static boolean getOrDefault(ModConfigSpec.BooleanValue value, boolean fallback) {
		try {
			return value.get();
		} catch (IllegalStateException ignored) {
			return fallback;
		}
	}

	private static int getOrDefault(ModConfigSpec.IntValue value, int fallback) {
		try {
			return value.get();
		} catch (IllegalStateException ignored) {
			return fallback;
		}
	}

	private static double getOrDefault(ModConfigSpec.DoubleValue value, double fallback) {
		try {
			return value.get();
		} catch (IllegalStateException ignored) {
			return fallback;
		}
	}

	public static final class Recipes {
		private static final boolean DEFAULT_ENABLE_ZERO_POINT_BLOCK_RECIPE = true;
		private static final boolean DEFAULT_ENABLE_ZERO_POINT_CORE_RECIPE = true;

		private final ModConfigSpec.BooleanValue enableZeroPointBlockRecipe;
		private final ModConfigSpec.BooleanValue enableZeroPointCoreRecipe;

		private Recipes(ModConfigSpec.Builder builder) {
			builder.push("recipes");
			enableZeroPointBlockRecipe = builder.comment("Allow the Matter Transmutation recipe that crafts the Zero Point block.")
					.define("enableZeroPointBlockRecipe", DEFAULT_ENABLE_ZERO_POINT_BLOCK_RECIPE);
			enableZeroPointCoreRecipe = builder.comment("Allow the Matter Transmutation recipe that crafts the Zero Point Core.")
					.define("enableZeroPointCoreRecipe", DEFAULT_ENABLE_ZERO_POINT_CORE_RECIPE);
			builder.pop();
		}

		public boolean enableZeroPointBlockRecipe() {
			return getOrDefault(enableZeroPointBlockRecipe, DEFAULT_ENABLE_ZERO_POINT_BLOCK_RECIPE);
		}

		public boolean enableZeroPointCoreRecipe() {
			return getOrDefault(enableZeroPointCoreRecipe, DEFAULT_ENABLE_ZERO_POINT_CORE_RECIPE);
		}
	}

	public static final class Machines {
		private static final int DEFAULT_TANK_PER_BLOCK_FLUID_CAPACITY = 8000;
		private static final int DEFAULT_DEPOT_BASE_CAPACITY = 20480;

		public final EnergyValues AOE_CHARGER;
		public final AoeChargerValues AOE_CHARGER_BEHAVIOR;
		public final EnergyValues BASIC_ENERGY_CABLE;
		public final EnergyValues BATTERY;
		public final EnergyValues BIOMATIC_COMPOSTER;
		public final EnergyValues BIOMATIC_CONSTRUCTOR;
		public final EnergyValues BIOMATIC_SIMULATOR;
		public final EnergyValues BLOCK_PLACER;
		public final EnergyValues CHEMICAL_REACTION_CHAMBER;
		public final EnergyValues CHLOROPHYTE_ACCELERATOR;
		public final EnergyValues CHLOROPHYTE_SMELTER;
		public final EnergyValues CIRCUIT_PRESS;
		public final EnergyValues COMPUTATION_CLUSTER;
		public final EnergyValues CONDUCTIVE_ENERGY_GUIDE;
		public final EnergyValues CRAFTING_FACTORY;
		public final EnergyValues CRYSTAL_ACCEPTER;
		public final EnergyValues CRYSTAL_CRUSHER;
		public final EnergyValues CRYSTAL_ENERGY_GUIDE;
		public final EnergyValues CRYSTAL_PURIFIER;
		public final EnergyValues CRYSTAL_SMELTER;
		public final EnergyValues DUST_SEPARATOR;
		public final EnergyValues EE_BATTERY;
		public final EnergyValues ELECTROMAGNET;
		public final EnergyValues ENERGY_CABLE;
		public final EnergyValues ENERGY_CABLE_MK2;
		public final EnergyValues ENERGY_EXTRACTOR;
		public final EnergyValues EXTRACTINATOR;
		public final EnergyValues FACTORY_CONTROLLER;
		public final EnergyValues FACTORY_ITEM_CONTROLLER;
		public final EnergyValues FACTORY_OUTPUT_CONTROLLER;
		public final EnergyValues FLUID_PACKAGER;
		public final EnergyValues INVERT_PISTON_GENERATOR;
		public final EnergyValues INVERTER;
		public final EnergyValues INVERTIUM_SMELTER;
		public final EnergyValues IRON_SMELTER;
		public final EnergyValues ITEM_CHARGER;
		public final EnergyValues MACHINE_ENERGY_INPUT;
		public final EnergyValues MATTER_TRANSMUTATION_TABLE;
		public final MachineProcessValues MATTER_TRANSMUTATION_PROCESS;
		public final EnergyValues METALLURGIC_RECRYSTALLIZER;
		public final EnergyValues NODE_EXTRACTOR;
		public final EnergyValues NODE_MINER;
		public final EnergyValues ORE_PROCESSOR;
		public final EnergyValues PISTON_GENERATOR;
		public final EnergyValues QUANTUM_MINER;
		public final EnergyValues QUARRY;
		public final EnergyValues REACTION_CHAMBER_COMPUTER;
		public final EnergyValues REACTION_ENERGY_INPUT;
		public final EnergyValues REACTOR_COMPUTER;
		public final EnergyValues REACTOR_ENERGY_OUTPUT;
		public final EnergyValues SINGULARITY_COMPRESSOR;
		public final EnergyValues STEAM_ENGINE;
		public final EnergyValues STEAM_ENGINE_UPGRADE;
		public final EnergyValues TESSERACT;
		public final EnergyValues TESSERACT_OUTPUT;
		private final ModConfigSpec.IntValue tankPerBlockFluidCapacity;
		private final ModConfigSpec.IntValue depotBaseCapacity;
		public final EnergyValues TURBINE;
		public final EnergyValues ULTIMA_SMELTER;
		public final EnergyValues WARP_PAD;
		public final WarpPadValues WARP_PAD_BEHAVIOR;
		public final EnergyValues ZERO_POINT;
		public final ZeroPointValues ZERO_POINT_MULTIBLOCK;

		private Machines(ModConfigSpec.Builder builder) {
			builder.push("machines");
			AOE_CHARGER = new EnergyValues(builder, "aoe_charger", 20480, 20480, 10240);
			AOE_CHARGER_BEHAVIOR = new AoeChargerValues(builder);
			BASIC_ENERGY_CABLE = new EnergyValues(builder, "basic_energy_cable", 10240, 1024, 1024);
			BATTERY = new EnergyValues(builder, "battery", 4096000, 512000, 512000);
			BIOMATIC_COMPOSTER = new EnergyValues(builder, "biomatic_composter", 10240, 2048, 8192);
			BIOMATIC_CONSTRUCTOR = new EnergyValues(builder, "biomatic_constructor", 10240, 2048, 8192);
			BIOMATIC_SIMULATOR = new EnergyValues(builder, "biomatic_simulator", 10240, 2048, 8192);
			BLOCK_PLACER = new EnergyValues(builder, "block_placer", 10240, 256, 256);
			CHEMICAL_REACTION_CHAMBER = new EnergyValues(builder, "chemical_reaction_chamber", 10240, 1024, 10240);
			CHLOROPHYTE_ACCELERATOR = new EnergyValues(builder, "chlorophyte_accelerator", 8192, 512, 256);
			CHLOROPHYTE_SMELTER = new EnergyValues(builder, "chlorophyte_smelter", 10240, 2048, 2048);
			CIRCUIT_PRESS = new EnergyValues(builder, "circuit_press", 10240, 2048, 2048);
			COMPUTATION_CLUSTER = new EnergyValues(builder, "computation_cluster", 409600, 11264, 409600);
			CONDUCTIVE_ENERGY_GUIDE = new EnergyValues(builder, "conductive_energy_guide", 409600, 409600, 409600);
			CRAFTING_FACTORY = new EnergyValues(builder, "crafting_factory", 40960, 2048, 1024);
			CRYSTAL_ACCEPTER = new EnergyValues(builder, "crystal_accepter", 409600, 2048, 2048);
			CRYSTAL_CRUSHER = new EnergyValues(builder, "crystal_crusher", 10240, 2048, 2048);
			CRYSTAL_ENERGY_GUIDE = new EnergyValues(builder, "crystal_energy_guide", 409600, 409600, 409600);
			CRYSTAL_PURIFIER = new EnergyValues(builder, "crystal_purifier", 10240, 2048, 2048);
			CRYSTAL_SMELTER = new EnergyValues(builder, "crystal_smelter", 10240, 2048, 2048);
			DUST_SEPARATOR = new EnergyValues(builder, "dust_separator", 10240, 2048, 2048);
			EE_BATTERY = new EnergyValues(builder, "ee_battery", 20480000, 1024000, 1024000);
			ELECTROMAGNET = new EnergyValues(builder, "electromagnet", 512000, 20480, 10240);
			ENERGY_CABLE = new EnergyValues(builder, "energy_cable", 102400, 51200, 51200);
			ENERGY_CABLE_MK2 = new EnergyValues(builder, "energy_cable_mk2", 1024000, 512000, 512000);
			ENERGY_EXTRACTOR = new EnergyValues(builder, "energy_extractor", 2048000, 512000, 512000);
			EXTRACTINATOR = new EnergyValues(builder, "extractinator", 10240, 2048, 2048);
			FACTORY_CONTROLLER = new EnergyValues(builder, "factory_controller", 65536, 65536, 65536);
			FACTORY_ITEM_CONTROLLER = new EnergyValues(builder, "factory_item_controller", 65536, 65536, 65536);
			FACTORY_OUTPUT_CONTROLLER = new EnergyValues(builder, "factory_output_controller", 65536, 65536, 65536);
			FLUID_PACKAGER = new EnergyValues(builder, "fluid_packager", 10240, 2048, 2048);
			INVERT_PISTON_GENERATOR = new EnergyValues(builder, "invert_piston_generator", 81290, 4096, 2048);
			INVERTER = new EnergyValues(builder, "inverter", 10240, 2048, 2048);
			INVERTIUM_SMELTER = new EnergyValues(builder, "invertium_smelter", 10240, 2048, 2048);
			IRON_SMELTER = new EnergyValues(builder, "iron_smelter", 10240, 2048, 2048);
			ITEM_CHARGER = new EnergyValues(builder, "item_charger", 20480, 20480, 10240);
			MACHINE_ENERGY_INPUT = new EnergyValues(builder, "machine_energy_input", 40960, 20480, 40960);
			MATTER_TRANSMUTATION_TABLE = new EnergyValues(builder, "matter_transmutation_table", 20480, 2048, 2048);
			MATTER_TRANSMUTATION_PROCESS = new MachineProcessValues(builder, "matter_transmutation_process", 100, 1024);
			METALLURGIC_RECRYSTALLIZER = new EnergyValues(builder, "metallurgic_recrystallizer", 40960, 5120, 20480);
			NODE_EXTRACTOR = new EnergyValues(builder, "node_extractor", 25600, 16384, 8192);
			NODE_MINER = new EnergyValues(builder, "node_miner", 25600, 16384, 8192);
			ORE_PROCESSOR = new EnergyValues(builder, "ore_processor", 102400, 10240, 20480);
			PISTON_GENERATOR = new EnergyValues(builder, "piston_generator", 40960, 2048, 1024);
			QUANTUM_MINER = new EnergyValues(builder, "quantum_miner", 512000, 32768, 32768);
			QUARRY = new EnergyValues(builder, "quarry", 409600, 20480, 10240);
			REACTION_CHAMBER_COMPUTER = new EnergyValues(builder, "reaction_chamber_computer", 10240000, 512000, 10240000);
			REACTION_ENERGY_INPUT = new EnergyValues(builder, "reaction_energy_input", 8192000, 4096000, 512000);
			REACTOR_COMPUTER = new EnergyValues(builder, "reactor_computer", 4096000, 1024000, 1024000);
			REACTOR_ENERGY_OUTPUT = new EnergyValues(builder, "reactor_energy_output", 10024000, 512000, 10024000);
			SINGULARITY_COMPRESSOR = new EnergyValues(builder, "singularity_compressor", 1024000, 1024000, 1024000);
			STEAM_ENGINE = new EnergyValues(builder, "steam_engine", 20480, 1024, 2048);
			STEAM_ENGINE_UPGRADE = new EnergyValues(builder, "steam_engine_upgrade", 40960, 4096, 8192);
			TESSERACT = new EnergyValues(builder, "tesseract", 4096000, 4096000, 4096000);
			TESSERACT_OUTPUT = new EnergyValues(builder, "tesseract_output", 4009600, 4009600, 4009600);
			builder.push("tank");
			tankPerBlockFluidCapacity = builder.comment("Fluid tank capacity per connected tank block, in mB.")
					.defineInRange("perBlockFluidCapacity", DEFAULT_TANK_PER_BLOCK_FLUID_CAPACITY, 1, Integer.MAX_VALUE);
			builder.pop();
			builder.push("depot");
			depotBaseCapacity = builder.comment("Base depot item capacity before storage upgrades. Each upgrade doubles this value.")
					.defineInRange("baseCapacity", DEFAULT_DEPOT_BASE_CAPACITY, 1, Integer.MAX_VALUE);
			builder.pop();
			TURBINE = new EnergyValues(builder, "turbine", 10480, 4096, 8192);
			ULTIMA_SMELTER = new EnergyValues(builder, "ultima_smelter", 20480, 8192, 8192);
			WARP_PAD = new EnergyValues(builder, "warp_pad", 409600, 5120, 409600);
			WARP_PAD_BEHAVIOR = new WarpPadValues(builder);
			ZERO_POINT = new EnergyValues(builder, "zero_point", 10240000, 10240000, 10240000);
			ZERO_POINT_MULTIBLOCK = new ZeroPointValues(builder, "zero_point_multiblock", 300, 1024000);
			builder.pop();
		}

		public int tankPerBlockFluidCapacity() {
			return getOrDefault(tankPerBlockFluidCapacity, DEFAULT_TANK_PER_BLOCK_FLUID_CAPACITY);
		}

		public long depotBaseCapacity() {
			return getOrDefault(depotBaseCapacity, DEFAULT_DEPOT_BASE_CAPACITY);
		}
	}

	public static final class Items {
		public final EnergyValues BATTERY_CELL;
		public final EnergyValues DENSE_BATTERY_CELL;
		public final EnergyValues CARBON_BATTERY_CELL;
		public final EnergyValues DARK_BATTERY_CELL;
		public final MiningLaserValues MINING_LASER;
		public final GravityGunValues GRAVITY_GUN;
		public final CompoundToolValues COMPOUND_PICKAXE;
		public final CompoundToolValues COMPOUND_SWORD;
		public final DarkMatterValues DARK_MATTER;

		private Items(ModConfigSpec.Builder builder) {
			builder.push("items");
			BATTERY_CELL = new EnergyValues(builder, "battery_cell", 50000, 2500, 2500);
			DENSE_BATTERY_CELL = new EnergyValues(builder, "dense_battery_cell", 250000, 12500, 12500);
			CARBON_BATTERY_CELL = new EnergyValues(builder, "carbon_battery_cell", 1000000, 50000, 50000);
			DARK_BATTERY_CELL = new EnergyValues(builder, "dark_battery_cell", 5000000, 250000, 250000);
			MINING_LASER = new MiningLaserValues(builder);
			GRAVITY_GUN = new GravityGunValues(builder);
			COMPOUND_PICKAXE = new CompoundToolValues(builder, "compound_pickaxe", 200, "FE drained from a carried battery for each block mined.");
			COMPOUND_SWORD = new CompoundToolValues(builder, "compound_sword", 500, "FE drained from a carried battery for each entity hit.");
			DARK_MATTER = new DarkMatterValues(builder);
			builder.pop();
		}
	}

	public static final class EnergyValues {
		private final ModConfigSpec.IntValue capacity;
		private final ModConfigSpec.IntValue maxReceive;
		private final ModConfigSpec.IntValue maxExtract;
		private final int defaultCapacity;
		private final int defaultMaxReceive;
		private final int defaultMaxExtract;

		private EnergyValues(ModConfigSpec.Builder builder, String path, int defaultCapacity, int defaultMaxReceive, int defaultMaxExtract) {
			this.defaultCapacity = defaultCapacity;
			this.defaultMaxReceive = defaultMaxReceive;
			this.defaultMaxExtract = defaultMaxExtract;
			builder.push(path);
			capacity = builder.comment("Maximum FE this block or item can store.")
					.defineInRange("capacity", defaultCapacity, 1, Integer.MAX_VALUE);
			maxReceive = builder.comment("Maximum FE this block or item can receive per operation.")
					.defineInRange("maxReceive", defaultMaxReceive, 0, Integer.MAX_VALUE);
			maxExtract = builder.comment("Maximum FE this block or item can extract per operation.")
					.defineInRange("maxExtract", defaultMaxExtract, 0, Integer.MAX_VALUE);
			builder.pop();
		}

		public int capacity() {
			return getOrDefault(capacity, defaultCapacity);
		}

		public int maxReceive() {
			return getOrDefault(maxReceive, defaultMaxReceive);
		}

		public int maxExtract() {
			return getOrDefault(maxExtract, defaultMaxExtract);
		}
	}

	public static final class MachineProcessValues {
		private final ModConfigSpec.IntValue ticksPerCraft;
		private final ModConfigSpec.IntValue energyPerCraft;
		private final int defaultTicksPerCraft;
		private final int defaultEnergyPerCraft;

		private MachineProcessValues(ModConfigSpec.Builder builder, String path, int defaultTicksPerCraft, int defaultEnergyPerCraft) {
			this.defaultTicksPerCraft = defaultTicksPerCraft;
			this.defaultEnergyPerCraft = defaultEnergyPerCraft;
			builder.push(path);
			ticksPerCraft = builder.comment("Ticks required to complete one craft.")
					.defineInRange("ticksPerCraft", defaultTicksPerCraft, 1, Integer.MAX_VALUE);
			energyPerCraft = builder.comment("FE consumed when one craft completes.")
					.defineInRange("energyPerCraft", defaultEnergyPerCraft, 0, Integer.MAX_VALUE);
			builder.pop();
		}

		public int ticksPerCraft() {
			return getOrDefault(ticksPerCraft, defaultTicksPerCraft);
		}

		public int energyPerCraft() {
			return getOrDefault(energyPerCraft, defaultEnergyPerCraft);
		}
	}

	public static final class AoeChargerValues {
		private static final int DEFAULT_BASE_TRANSFER_PER_TICK = 512;
		private static final double DEFAULT_BASE_RANGE = 24.0D;
		private static final double DEFAULT_ACCELERATION_UPGRADE_MULTIPLIER = 1.5D;
		private static final double DEFAULT_CARBON_ACCELERATION_UPGRADE_MULTIPLIER = 3.0D;
		private static final double DEFAULT_RANGE_UPGRADE_RANGE = 48.0D;
		private static final double DEFAULT_CARBON_RANGE_UPGRADE_RANGE = 64.0D;
		private static final double DEFAULT_MIN_TRANSFER_MULTIPLIER = 0.05D;
		private static final double DEFAULT_MAX_TRANSFER_MULTIPLIER = 8.0D;
		private static final double DEFAULT_SPARK_CHANCE = 0.15D;

		private final ModConfigSpec.IntValue baseTransferPerTick;
		private final ModConfigSpec.DoubleValue baseRange;
		private final ModConfigSpec.DoubleValue accelerationUpgradeMultiplier;
		private final ModConfigSpec.DoubleValue carbonAccelerationUpgradeMultiplier;
		private final ModConfigSpec.DoubleValue rangeUpgradeRange;
		private final ModConfigSpec.DoubleValue carbonRangeUpgradeRange;
		private final ModConfigSpec.DoubleValue minTransferMultiplier;
		private final ModConfigSpec.DoubleValue maxTransferMultiplier;
		private final ModConfigSpec.DoubleValue sparkChance;

		private AoeChargerValues(ModConfigSpec.Builder builder) {
			builder.push("aoe_charger_behavior");
			baseTransferPerTick = builder.comment("Base FE/t the AOE Charger can move into nearby items before upgrade multipliers.")
					.defineInRange("baseTransferPerTick", DEFAULT_BASE_TRANSFER_PER_TICK, 0, Integer.MAX_VALUE);
			baseRange = builder.comment("Default charging range in blocks.")
					.defineInRange("baseRange", DEFAULT_BASE_RANGE, 1.0D, 256.0D);
			accelerationUpgradeMultiplier = builder.comment("Transfer multiplier when an Acceleration Upgrade is installed.")
					.defineInRange("accelerationUpgradeMultiplier", DEFAULT_ACCELERATION_UPGRADE_MULTIPLIER, 0.0D, 1024.0D);
			carbonAccelerationUpgradeMultiplier = builder.comment("Transfer multiplier when a Carbon Acceleration Upgrade is installed.")
					.defineInRange("carbonAccelerationUpgradeMultiplier", DEFAULT_CARBON_ACCELERATION_UPGRADE_MULTIPLIER, 0.0D, 1024.0D);
			rangeUpgradeRange = builder.comment("Charging range in blocks when a Range Upgrade is installed.")
					.defineInRange("rangeUpgradeRange", DEFAULT_RANGE_UPGRADE_RANGE, 1.0D, 256.0D);
			carbonRangeUpgradeRange = builder.comment("Charging range in blocks when a Carbon Range Upgrade is installed.")
					.defineInRange("carbonRangeUpgradeRange", DEFAULT_CARBON_RANGE_UPGRADE_RANGE, 1.0D, 256.0D);
			minTransferMultiplier = builder.comment("Minimum transfer multiplier after upgrade and SSD modifiers.")
					.defineInRange("minTransferMultiplier", DEFAULT_MIN_TRANSFER_MULTIPLIER, 0.0D, 1024.0D);
			maxTransferMultiplier = builder.comment("Maximum transfer multiplier after upgrade and SSD modifiers.")
					.defineInRange("maxTransferMultiplier", DEFAULT_MAX_TRANSFER_MULTIPLIER, 0.0D, 1024.0D);
			sparkChance = builder.comment("Chance per tick to emit electric spark particles while charging.")
					.defineInRange("sparkChance", DEFAULT_SPARK_CHANCE, 0.0D, 1.0D);
			builder.pop();
		}

		public int baseTransferPerTick() {
			return getOrDefault(baseTransferPerTick, DEFAULT_BASE_TRANSFER_PER_TICK);
		}

		public double baseRange() {
			return getOrDefault(baseRange, DEFAULT_BASE_RANGE);
		}

		public double accelerationUpgradeMultiplier() {
			return getOrDefault(accelerationUpgradeMultiplier, DEFAULT_ACCELERATION_UPGRADE_MULTIPLIER);
		}

		public double carbonAccelerationUpgradeMultiplier() {
			return getOrDefault(carbonAccelerationUpgradeMultiplier, DEFAULT_CARBON_ACCELERATION_UPGRADE_MULTIPLIER);
		}

		public double rangeUpgradeRange() {
			return getOrDefault(rangeUpgradeRange, DEFAULT_RANGE_UPGRADE_RANGE);
		}

		public double carbonRangeUpgradeRange() {
			return getOrDefault(carbonRangeUpgradeRange, DEFAULT_CARBON_RANGE_UPGRADE_RANGE);
		}

		public double minTransferMultiplier() {
			return getOrDefault(minTransferMultiplier, DEFAULT_MIN_TRANSFER_MULTIPLIER);
		}

		public double maxTransferMultiplier() {
			return getOrDefault(maxTransferMultiplier, DEFAULT_MAX_TRANSFER_MULTIPLIER);
		}

		public double sparkChance() {
			return getOrDefault(sparkChance, DEFAULT_SPARK_CHANCE);
		}
	}

	public static final class WarpPadValues {
		private static final int DEFAULT_CHARGE_TICKS = 300;

		private final ModConfigSpec.IntValue chargeTicks;

		private WarpPadValues(ModConfigSpec.Builder builder) {
			builder.push("warp_pad_behavior");
			chargeTicks = builder.comment("Ticks a fully powered Warp Pad must charge before teleporting is ready.")
					.defineInRange("chargeTicks", DEFAULT_CHARGE_TICKS, 1, Integer.MAX_VALUE);
			builder.pop();
		}

		public int chargeTicks() {
			return getOrDefault(chargeTicks, DEFAULT_CHARGE_TICKS);
		}
	}

	public static final class ZeroPointValues {
		private final ModConfigSpec.IntValue soundCycleTicks;
		private final ModConfigSpec.IntValue maxOutputPerSide;
		private final int defaultSoundCycleTicks;
		private final int defaultMaxOutputPerSide;

		private ZeroPointValues(ModConfigSpec.Builder builder, String path, int defaultSoundCycleTicks, int defaultMaxOutputPerSide) {
			this.defaultSoundCycleTicks = defaultSoundCycleTicks;
			this.defaultMaxOutputPerSide = defaultMaxOutputPerSide;
			builder.push(path);
			soundCycleTicks = builder.comment("Ticks between Zero Point active sound pulses.")
					.defineInRange("soundCycleTicks", defaultSoundCycleTicks, 1, Integer.MAX_VALUE);
			maxOutputPerSide = builder.comment("Maximum FE the formed Zero Point multiblock can push to each adjacent output per tick.")
					.defineInRange("maxOutputPerSide", defaultMaxOutputPerSide, 0, Integer.MAX_VALUE);
			builder.pop();
		}

		public int soundCycleTicks() {
			return getOrDefault(soundCycleTicks, defaultSoundCycleTicks);
		}

		public int maxOutputPerSide() {
			return getOrDefault(maxOutputPerSide, defaultMaxOutputPerSide);
		}
	}

	public static final class CompoundToolValues {
		private final ModConfigSpec.IntValue energyCost;
		private final int defaultEnergyCost;

		private CompoundToolValues(ModConfigSpec.Builder builder, String path, int defaultEnergyCost, String comment) {
			this.defaultEnergyCost = defaultEnergyCost;
			builder.push(path);
			energyCost = builder.comment(comment)
					.defineInRange("energyCost", defaultEnergyCost, 0, Integer.MAX_VALUE);
			builder.pop();
		}

		public int energyCost() {
			return getOrDefault(energyCost, defaultEnergyCost);
		}
	}

	public static final class DarkMatterValues {
		private static final int DEFAULT_BASE_BLACK_HOLE_DURATION_TICKS = 1200;
		private static final int DEFAULT_MAX_DURATION_BONUS_TICKS = 180;
		private static final int DEFAULT_DURATION_BONUS_PER_ITEM = 12;
		private static final double DEFAULT_BASE_RADIUS = 64.0D;
		private static final double DEFAULT_MAX_RADIUS_BONUS = 10.0D;
		private static final double DEFAULT_RADIUS_BONUS_PER_ITEM = 0.75D;
		private static final double DEFAULT_CENTER_CONSUME_DISTANCE = 2.75D;
		private static final int DEFAULT_BLOCK_PULL_ATTEMPTS_PER_TICK = 40;
		private static final double DEFAULT_FALLING_BLOCK_SPAWN_CHANCE = 1.0D;
		private static final int DEFAULT_MIN_BLOCK_DECAY_STEPS_PER_TICK = 900;
		private static final double DEFAULT_BLOCK_DECAY_STEPS_PER_RADIUS = 12.0D;
		private static final double DEFAULT_EDGE_BREAK_CHANCE = 0.06D;
		private static final double DEFAULT_CENTER_DAMAGE = 8.0D;
		private static final double DEFAULT_PERIODIC_DAMAGE = 2.0D;
		private static final double DEFAULT_ENTITY_PULL_BASE = 0.17D;
		private static final double DEFAULT_ENTITY_PULL_BONUS = 0.34D;
		private static final double DEFAULT_FALLING_BLOCK_PULL_SPEED = 0.45D;

		private final ModConfigSpec.IntValue baseBlackHoleDurationTicks;
		private final ModConfigSpec.IntValue maxDurationBonusTicks;
		private final ModConfigSpec.IntValue durationBonusPerItem;
		private final ModConfigSpec.DoubleValue baseRadius;
		private final ModConfigSpec.DoubleValue maxRadiusBonus;
		private final ModConfigSpec.DoubleValue radiusBonusPerItem;
		private final ModConfigSpec.DoubleValue centerConsumeDistance;
		private final ModConfigSpec.IntValue blockPullAttemptsPerTick;
		private final ModConfigSpec.DoubleValue fallingBlockSpawnChance;
		private final ModConfigSpec.IntValue minBlockDecayStepsPerTick;
		private final ModConfigSpec.DoubleValue blockDecayStepsPerRadius;
		private final ModConfigSpec.DoubleValue edgeBreakChance;
		private final ModConfigSpec.DoubleValue centerDamage;
		private final ModConfigSpec.DoubleValue periodicDamage;
		private final ModConfigSpec.DoubleValue entityPullBase;
		private final ModConfigSpec.DoubleValue entityPullBonus;
		private final ModConfigSpec.DoubleValue fallingBlockPullSpeed;

		private DarkMatterValues(ModConfigSpec.Builder builder) {
			builder.push("dark_matter");
			baseBlackHoleDurationTicks = builder.comment("Base black hole lifetime in ticks.")
					.defineInRange("baseBlackHoleDurationTicks", DEFAULT_BASE_BLACK_HOLE_DURATION_TICKS, 1, Integer.MAX_VALUE);
			maxDurationBonusTicks = builder.comment("Maximum extra black hole lifetime from stack size.")
					.defineInRange("maxDurationBonusTicks", DEFAULT_MAX_DURATION_BONUS_TICKS, 0, Integer.MAX_VALUE);
			durationBonusPerItem = builder.comment("Extra black hole lifetime ticks per item in the triggering stack.")
					.defineInRange("durationBonusPerItem", DEFAULT_DURATION_BONUS_PER_ITEM, 0, Integer.MAX_VALUE);
			baseRadius = builder.comment("Base black hole radius in blocks.")
					.defineInRange("baseRadius", DEFAULT_BASE_RADIUS, 1.0D, 512.0D);
			maxRadiusBonus = builder.comment("Maximum extra black hole radius from stack size.")
					.defineInRange("maxRadiusBonus", DEFAULT_MAX_RADIUS_BONUS, 0.0D, 512.0D);
			radiusBonusPerItem = builder.comment("Extra black hole radius per item in the triggering stack.")
					.defineInRange("radiusBonusPerItem", DEFAULT_RADIUS_BONUS_PER_ITEM, 0.0D, 512.0D);
			centerConsumeDistance = builder.comment("Distance from the center where entities and falling blocks are consumed or damaged.")
					.defineInRange("centerConsumeDistance", DEFAULT_CENTER_CONSUME_DISTANCE, 0.0D, 128.0D);
			blockPullAttemptsPerTick = builder.comment("Falling-block pull attempts per black hole tick.")
					.defineInRange("blockPullAttemptsPerTick", DEFAULT_BLOCK_PULL_ATTEMPTS_PER_TICK, 0, Integer.MAX_VALUE);
			fallingBlockSpawnChance = builder.comment("Chance for each pull attempt to turn a valid block into a falling block.")
					.defineInRange("fallingBlockSpawnChance", DEFAULT_FALLING_BLOCK_SPAWN_CHANCE, 0.0D, 1.0D);
			minBlockDecayStepsPerTick = builder.comment("Minimum direct block decay attempts per black hole tick.")
					.defineInRange("minBlockDecayStepsPerTick", DEFAULT_MIN_BLOCK_DECAY_STEPS_PER_TICK, 0, Integer.MAX_VALUE);
			blockDecayStepsPerRadius = builder.comment("Additional direct block decay attempts scale, multiplied by black hole radius.")
					.defineInRange("blockDecayStepsPerRadius", DEFAULT_BLOCK_DECAY_STEPS_PER_RADIUS, 0.0D, 1024.0D);
			edgeBreakChance = builder.comment("Minimum direct block break chance near the edge of the black hole.")
					.defineInRange("edgeBreakChance", DEFAULT_EDGE_BREAK_CHANCE, 0.0D, 1.0D);
			centerDamage = builder.comment("Magic damage dealt to entities in the consume distance.")
					.defineInRange("centerDamage", DEFAULT_CENTER_DAMAGE, 0.0D, 1024.0D);
			periodicDamage = builder.comment("Magic damage dealt every second to nearby living entities.")
					.defineInRange("periodicDamage", DEFAULT_PERIODIC_DAMAGE, 0.0D, 1024.0D);
			entityPullBase = builder.comment("Base entity pull strength.")
					.defineInRange("entityPullBase", DEFAULT_ENTITY_PULL_BASE, 0.0D, 16.0D);
			entityPullBonus = builder.comment("Additional entity pull strength near the center.")
					.defineInRange("entityPullBonus", DEFAULT_ENTITY_PULL_BONUS, 0.0D, 16.0D);
			fallingBlockPullSpeed = builder.comment("Initial pull speed applied to falling blocks.")
					.defineInRange("fallingBlockPullSpeed", DEFAULT_FALLING_BLOCK_PULL_SPEED, 0.0D, 16.0D);
			builder.pop();
		}

		public int baseBlackHoleDurationTicks() {
			return getOrDefault(baseBlackHoleDurationTicks, DEFAULT_BASE_BLACK_HOLE_DURATION_TICKS);
		}

		public int maxDurationBonusTicks() {
			return getOrDefault(maxDurationBonusTicks, DEFAULT_MAX_DURATION_BONUS_TICKS);
		}

		public int durationBonusPerItem() {
			return getOrDefault(durationBonusPerItem, DEFAULT_DURATION_BONUS_PER_ITEM);
		}

		public double baseRadius() {
			return getOrDefault(baseRadius, DEFAULT_BASE_RADIUS);
		}

		public double maxRadiusBonus() {
			return getOrDefault(maxRadiusBonus, DEFAULT_MAX_RADIUS_BONUS);
		}

		public double radiusBonusPerItem() {
			return getOrDefault(radiusBonusPerItem, DEFAULT_RADIUS_BONUS_PER_ITEM);
		}

		public double centerConsumeDistance() {
			return getOrDefault(centerConsumeDistance, DEFAULT_CENTER_CONSUME_DISTANCE);
		}

		public int blockPullAttemptsPerTick() {
			return getOrDefault(blockPullAttemptsPerTick, DEFAULT_BLOCK_PULL_ATTEMPTS_PER_TICK);
		}

		public double fallingBlockSpawnChance() {
			return getOrDefault(fallingBlockSpawnChance, DEFAULT_FALLING_BLOCK_SPAWN_CHANCE);
		}

		public int minBlockDecayStepsPerTick() {
			return getOrDefault(minBlockDecayStepsPerTick, DEFAULT_MIN_BLOCK_DECAY_STEPS_PER_TICK);
		}

		public double blockDecayStepsPerRadius() {
			return getOrDefault(blockDecayStepsPerRadius, DEFAULT_BLOCK_DECAY_STEPS_PER_RADIUS);
		}

		public double edgeBreakChance() {
			return getOrDefault(edgeBreakChance, DEFAULT_EDGE_BREAK_CHANCE);
		}

		public double centerDamage() {
			return getOrDefault(centerDamage, DEFAULT_CENTER_DAMAGE);
		}

		public double periodicDamage() {
			return getOrDefault(periodicDamage, DEFAULT_PERIODIC_DAMAGE);
		}

		public double entityPullBase() {
			return getOrDefault(entityPullBase, DEFAULT_ENTITY_PULL_BASE);
		}

		public double entityPullBonus() {
			return getOrDefault(entityPullBonus, DEFAULT_ENTITY_PULL_BONUS);
		}

		public double fallingBlockPullSpeed() {
			return getOrDefault(fallingBlockPullSpeed, DEFAULT_FALLING_BLOCK_PULL_SPEED);
		}
	}

	public static final class MiningLaserValues {
		private static final double DEFAULT_RANGE = 32.0D;
		private static final int DEFAULT_SUSTAIN_FE_PER_TICK = 16;
		private static final int DEFAULT_MINE_FE_PER_BLOCK = 256;
		private static final int DEFAULT_MINE_INTERVAL_TICKS = 2;

		private final ModConfigSpec.DoubleValue range;
		private final ModConfigSpec.IntValue sustainFePerTick;
		private final ModConfigSpec.IntValue mineFePerBlock;
		private final ModConfigSpec.IntValue mineIntervalTicks;

		private MiningLaserValues(ModConfigSpec.Builder builder) {
			builder.push("mining_laser");
			range = builder.comment("Maximum block mining range.")
					.defineInRange("range", DEFAULT_RANGE, 1.0D, 256.0D);
			sustainFePerTick = builder.comment("FE consumed each tick while the laser is held active.")
					.defineInRange("sustainFePerTick", DEFAULT_SUSTAIN_FE_PER_TICK, 0, Integer.MAX_VALUE);
			mineFePerBlock = builder.comment("Extra FE consumed for each mined block.")
					.defineInRange("mineFePerBlock", DEFAULT_MINE_FE_PER_BLOCK, 0, Integer.MAX_VALUE);
			mineIntervalTicks = builder.comment("Ticks between mining attempts while the laser is held active.")
					.defineInRange("mineIntervalTicks", DEFAULT_MINE_INTERVAL_TICKS, 1, Integer.MAX_VALUE);
			builder.pop();
		}

		public double range() {
			return getOrDefault(range, DEFAULT_RANGE);
		}

		public int sustainFePerTick() {
			return getOrDefault(sustainFePerTick, DEFAULT_SUSTAIN_FE_PER_TICK);
		}

		public int mineFePerBlock() {
			return getOrDefault(mineFePerBlock, DEFAULT_MINE_FE_PER_BLOCK);
		}

		public int mineIntervalTicks() {
			return getOrDefault(mineIntervalTicks, DEFAULT_MINE_INTERVAL_TICKS);
		}
	}

	public static final class GravityGunValues {
		private static final double DEFAULT_PICKUP_RANGE = 8.0D;
		private static final double DEFAULT_MAX_HOLD_DISTANCE = 12.0D;
		private static final double DEFAULT_THROW_SPEED = 2.8D;
		private static final double DEFAULT_PROJECTILE_DAMAGE = 9.0D;
		private static final int DEFAULT_PROJECTILE_MAX_DAMAGE = 20;

		private final ModConfigSpec.DoubleValue pickupRange;
		private final ModConfigSpec.DoubleValue maxHoldDistance;
		private final ModConfigSpec.DoubleValue throwSpeed;
		private final ModConfigSpec.DoubleValue projectileDamage;
		private final ModConfigSpec.IntValue projectileMaxDamage;

		private GravityGunValues(ModConfigSpec.Builder builder) {
			builder.push("gravity_gun");
			pickupRange = builder.comment("Maximum distance for grabbing entities or blocks.")
					.defineInRange("pickupRange", DEFAULT_PICKUP_RANGE, 1.0D, 128.0D);
			maxHoldDistance = builder.comment("Maximum scroll-adjusted hold distance.")
					.defineInRange("maxHoldDistance", DEFAULT_MAX_HOLD_DISTANCE, 1.0D, 128.0D);
			throwSpeed = builder.comment("Velocity applied when throwing held blocks.")
					.defineInRange("throwSpeed", DEFAULT_THROW_SPEED, 0.0D, 128.0D);
			projectileDamage = builder.comment("Base thrown block impact damage.")
					.defineInRange("projectileDamage", DEFAULT_PROJECTILE_DAMAGE, 0.0D, 1024.0D);
			projectileMaxDamage = builder.comment("Maximum thrown block impact damage.")
					.defineInRange("projectileMaxDamage", DEFAULT_PROJECTILE_MAX_DAMAGE, 0, Integer.MAX_VALUE);
			builder.pop();
		}

		public double pickupRange() {
			return getOrDefault(pickupRange, DEFAULT_PICKUP_RANGE);
		}

		public double maxHoldDistance() {
			return getOrDefault(maxHoldDistance, DEFAULT_MAX_HOLD_DISTANCE);
		}

		public double throwSpeed() {
			return getOrDefault(throwSpeed, DEFAULT_THROW_SPEED);
		}

		public double projectileDamage() {
			return getOrDefault(projectileDamage, DEFAULT_PROJECTILE_DAMAGE);
		}

		public int projectileMaxDamage() {
			return getOrDefault(projectileMaxDamage, DEFAULT_PROJECTILE_MAX_DAMAGE);
		}
	}
}
