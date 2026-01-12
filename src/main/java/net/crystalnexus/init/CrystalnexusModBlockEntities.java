/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import net.crystalnexus.block.entity.ZeroPointBlockEntity;
import net.crystalnexus.block.entity.WarpPadBlockEntity;
import net.crystalnexus.block.entity.UltimaSmelterBlockEntity;
import net.crystalnexus.block.entity.TurbineBlockEntity;
import net.crystalnexus.block.entity.TesseractOutputBlockEntity;
import net.crystalnexus.block.entity.TesseractBlockEntity;
import net.crystalnexus.block.entity.SteamEngineUpgradeBlockEntity;
import net.crystalnexus.block.entity.SteamEngineBlockEntity;
import net.crystalnexus.block.entity.SteamCollectorBlockEntity;
import net.crystalnexus.block.entity.SteamChamberBlockEntity;
import net.crystalnexus.block.entity.SingularityMatrixBlockEntity;
import net.crystalnexus.block.entity.SingularityCompressorBlockEntity;
import net.crystalnexus.block.entity.ReactorFluidInputBlockEntity;
import net.crystalnexus.block.entity.ReactorEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.block.entity.ReactionEnergyInputBlockEntity;
import net.crystalnexus.block.entity.ReactionChamberComputerBlockEntity;
import net.crystalnexus.block.entity.QuantumMinerBlockEntity;
import net.crystalnexus.block.entity.PistonGeneratorBlockEntity;
import net.crystalnexus.block.entity.PipeStraightBlockEntity;
import net.crystalnexus.block.entity.PipeJunctionBlockEntity;
import net.crystalnexus.block.entity.OreProcessorBlockEntity;
import net.crystalnexus.block.entity.NodeMinerBlockEntity;
import net.crystalnexus.block.entity.MultiblockResearchStationBlockEntity;
import net.crystalnexus.block.entity.MetallurgicRecrystallizerBlockEntity;
import net.crystalnexus.block.entity.MatterTransmutationTableBlockEntity;
import net.crystalnexus.block.entity.MachineblockBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MachineCoreBlockEntity;
import net.crystalnexus.block.entity.ItemElevatorDownBlockEntity;
import net.crystalnexus.block.entity.ItemElevatorBlockEntity;
import net.crystalnexus.block.entity.ItemCollectorBlockEntity;
import net.crystalnexus.block.entity.IronSmelterBlockEntity;
import net.crystalnexus.block.entity.InvertiumSmelterBlockEntity;
import net.crystalnexus.block.entity.InverterBlockEntity;
import net.crystalnexus.block.entity.InvertPistonGeneratorBlockEntity;
import net.crystalnexus.block.entity.GrowthChamberOffBlockEntity;
import net.crystalnexus.block.entity.FactoryOutputControllerBlockEntity;
import net.crystalnexus.block.entity.FactoryItemControllerBlockEntity;
import net.crystalnexus.block.entity.FactoryControllerBlockEntity;
import net.crystalnexus.block.entity.ExtractinatorBlockEntity;
import net.crystalnexus.block.entity.EnergySplitterBlockEntity;
import net.crystalnexus.block.entity.EnergyRefractorBlockEntity;
import net.crystalnexus.block.entity.EnergyExtractorBlockEntity;
import net.crystalnexus.block.entity.EEBatteryBlockEntity;
import net.crystalnexus.block.entity.DustSeparatorBlockEntity;
import net.crystalnexus.block.entity.CrystalSmelterBlockEntity;
import net.crystalnexus.block.entity.CrystalPurifierBlockEntity;
import net.crystalnexus.block.entity.CrystalGuideBlockEntity;
import net.crystalnexus.block.entity.CrystalEnergyGuideBlockEntity;
import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.block.entity.CrystalAccepterBlockEntity;
import net.crystalnexus.block.entity.CraftingFactoryBlockEntity;
import net.crystalnexus.block.entity.ConveyerBeltOutputBlockEntity;
import net.crystalnexus.block.entity.ConveyerBeltInputBlockEntity;
import net.crystalnexus.block.entity.ConveyerBeltBlockEntity;
import net.crystalnexus.block.entity.ContainerBlockEntity;
import net.crystalnexus.block.entity.ConductiveEnergySplitterBlockEntity;
import net.crystalnexus.block.entity.ConductiveEnergyRefractorBlockEntity;
import net.crystalnexus.block.entity.ConductiveEnergyGuideBlockEntity;
import net.crystalnexus.block.entity.CircuitPressBlockEntity;
import net.crystalnexus.block.entity.ChlorophyteSmelterBlockEntity;
import net.crystalnexus.block.entity.ChlorophyteAcceleratorBlockEntity;
import net.crystalnexus.block.entity.ChemicalReactionChamberBlockEntity;
import net.crystalnexus.block.entity.BluTNTBlockEntity;
import net.crystalnexus.block.entity.BlockPlacerBlockEntity;
import net.crystalnexus.block.entity.BiomaticSimulatorBlockEntity;
import net.crystalnexus.block.entity.BiomaticConstructorBlockEntity;
import net.crystalnexus.block.entity.BiomaticComposterBlockEntity;
import net.crystalnexus.block.entity.BatteryMonitorBlockEntity;
import net.crystalnexus.block.entity.BatteryBlockEntity;
import net.crystalnexus.block.entity.AdvancedConveyerBeltBlockEntity;
import net.crystalnexus.CrystalnexusMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CrystalnexusModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CrystalnexusMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalPurifierBlockEntity>> CRYSTAL_PURIFIER = register("crystal_purifier", CrystalnexusModBlocks.CRYSTAL_PURIFIER, CrystalPurifierBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalAccepterBlockEntity>> CRYSTAL_ACCEPTER = register("crystal_accepter", CrystalnexusModBlocks.CRYSTAL_ACCEPTER, CrystalAccepterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrowthChamberOffBlockEntity>> GROWTH_CHAMBER = register("growth_chamber", CrystalnexusModBlocks.GROWTH_CHAMBER, GrowthChamberOffBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalCrusherBlockEntity>> CRYSTAL_CRUSHER = register("crystal_crusher", CrystalnexusModBlocks.CRYSTAL_CRUSHER, CrystalCrusherBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DustSeparatorBlockEntity>> DUST_SEPARATOR = register("dust_separator", CrystalnexusModBlocks.DUST_SEPARATOR, DustSeparatorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalGuideBlockEntity>> CRYSTAL_GUIDE = register("crystal_guide", CrystalnexusModBlocks.CRYSTAL_GUIDE, CrystalGuideBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemCollectorBlockEntity>> ITEM_COLLECTOR = register("item_collector", CrystalnexusModBlocks.ITEM_COLLECTOR, ItemCollectorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractinatorBlockEntity>> EXTRACTINATOR = register("extractinator", CrystalnexusModBlocks.EXTRACTINATOR, ExtractinatorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChlorophyteAcceleratorBlockEntity>> CHLOROPHYTE_ACCELERATOR = register("chlorophyte_accelerator", CrystalnexusModBlocks.CHLOROPHYTE_ACCELERATOR,
			ChlorophyteAcceleratorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetallurgicRecrystallizerBlockEntity>> METALLURGIC_RECRYSTALLIZER = register("metallurgic_recrystallizer", CrystalnexusModBlocks.METALLURGIC_RECRYSTALLIZER,
			MetallurgicRecrystallizerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorComputerBlockEntity>> REACTOR_COMPUTER = register("reactor_computer", CrystalnexusModBlocks.REACTOR_COMPUTER, ReactorComputerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorEnergyOutputBlockEntity>> REACTOR_ENERGY_OUTPUT = register("reactor_energy_output", CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT, ReactorEnergyOutputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorFluidInputBlockEntity>> REACTOR_FLUID_INPUT = register("reactor_fluid_input", CrystalnexusModBlocks.REACTOR_FLUID_INPUT, ReactorFluidInputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronSmelterBlockEntity>> IRON_SMELTER = register("iron_smelter", CrystalnexusModBlocks.IRON_SMELTER, IronSmelterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalSmelterBlockEntity>> CRYSTAL_SMELTER = register("crystal_smelter", CrystalnexusModBlocks.CRYSTAL_SMELTER, CrystalSmelterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InvertiumSmelterBlockEntity>> INVERTIUM_SMELTER = register("invertium_smelter", CrystalnexusModBlocks.INVERTIUM_SMELTER, InvertiumSmelterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChlorophyteSmelterBlockEntity>> CHLOROPHYTE_SMELTER = register("chlorophyte_smelter", CrystalnexusModBlocks.CHLOROPHYTE_SMELTER, ChlorophyteSmelterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TesseractBlockEntity>> TESSERACT = register("tesseract", CrystalnexusModBlocks.TESSERACT, TesseractBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TesseractOutputBlockEntity>> TESSERACT_OUTPUT = register("tesseract_output", CrystalnexusModBlocks.TESSERACT_OUTPUT, TesseractOutputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CircuitPressBlockEntity>> CIRCUIT_PRESS = register("circuit_press", CrystalnexusModBlocks.CIRCUIT_PRESS, CircuitPressBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BluTNTBlockEntity>> BLU_TNT = register("blu_tnt", CrystalnexusModBlocks.BLU_TNT, BluTNTBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryControllerBlockEntity>> FACTORY_CONTROLLER = register("factory_controller", CrystalnexusModBlocks.FACTORY_CONTROLLER, FactoryControllerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryItemControllerBlockEntity>> FACTORY_ITEM_CONTROLLER = register("factory_item_controller", CrystalnexusModBlocks.FACTORY_ITEM_CONTROLLER,
			FactoryItemControllerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryOutputControllerBlockEntity>> FACTORY_OUTPUT_CONTROLLER = register("factory_output_controller", CrystalnexusModBlocks.FACTORY_OUTPUT_CONTROLLER,
			FactoryOutputControllerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InverterBlockEntity>> INVERTER = register("inverter", CrystalnexusModBlocks.INVERTER, InverterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactionChamberComputerBlockEntity>> REACTION_CHAMBER_COMPUTER = register("reaction_chamber_computer", CrystalnexusModBlocks.REACTION_CHAMBER_COMPUTER,
			ReactionChamberComputerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactionEnergyInputBlockEntity>> REACTION_ENERGY_INPUT = register("reaction_energy_input", CrystalnexusModBlocks.REACTION_ENERGY_INPUT, ReactionEnergyInputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UltimaSmelterBlockEntity>> ULTIMA_SMELTER = register("ultima_smelter", CrystalnexusModBlocks.ULTIMA_SMELTER, UltimaSmelterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyExtractorBlockEntity>> ENERGY_EXTRACTOR = register("energy_extractor", CrystalnexusModBlocks.ENERGY_EXTRACTOR, EnergyExtractorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalEnergyGuideBlockEntity>> CRYSTAL_ENERGY_GUIDE = register("crystal_energy_guide", CrystalnexusModBlocks.CRYSTAL_ENERGY_GUIDE, CrystalEnergyGuideBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyRefractorBlockEntity>> ENERGY_REFRACTOR = register("energy_refractor", CrystalnexusModBlocks.ENERGY_REFRACTOR, EnergyRefractorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergySplitterBlockEntity>> ENERGY_SPLITTER = register("energy_splitter", CrystalnexusModBlocks.ENERGY_SPLITTER, EnergySplitterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterTransmutationTableBlockEntity>> MATTER_TRANSMUTATION_TABLE = register("matter_transmutation_table", CrystalnexusModBlocks.MATTER_TRANSMUTATION_TABLE,
			MatterTransmutationTableBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockPlacerBlockEntity>> BLOCK_PLACER = register("block_placer", CrystalnexusModBlocks.BLOCK_PLACER, BlockPlacerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingularityCompressorBlockEntity>> SINGULARITY_COMPRESSOR = register("singularity_compressor", CrystalnexusModBlocks.SINGULARITY_COMPRESSOR,
			SingularityCompressorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZeroPointBlockEntity>> ZERO_POINT = register("zero_point", CrystalnexusModBlocks.ZERO_POINT, ZeroPointBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalReactionChamberBlockEntity>> CHEMICAL_REACTION_CHAMBER = register("chemical_reaction_chamber", CrystalnexusModBlocks.CHEMICAL_REACTION_CHAMBER,
			ChemicalReactionChamberBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ContainerBlockEntity>> CONTAINER = register("container", CrystalnexusModBlocks.CONTAINER, ContainerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConductiveEnergyGuideBlockEntity>> CONDUCTIVE_ENERGY_GUIDE = register("conductive_energy_guide", CrystalnexusModBlocks.CONDUCTIVE_ENERGY_GUIDE,
			ConductiveEnergyGuideBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConductiveEnergyRefractorBlockEntity>> CONDUCTIVE_ENERGY_REFRACTOR = register("conductive_energy_refractor", CrystalnexusModBlocks.CONDUCTIVE_ENERGY_REFRACTOR,
			ConductiveEnergyRefractorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConductiveEnergySplitterBlockEntity>> CONDUCTIVE_ENERGY_SPLITTER = register("conductive_energy_splitter", CrystalnexusModBlocks.CONDUCTIVE_ENERGY_SPLITTER,
			ConductiveEnergySplitterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuantumMinerBlockEntity>> QUANTUM_MINER = register("quantum_miner", CrystalnexusModBlocks.QUANTUM_MINER, QuantumMinerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY = register("battery", CrystalnexusModBlocks.BATTERY, BatteryBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EEBatteryBlockEntity>> EE_BATTERY = register("ee_battery", CrystalnexusModBlocks.EE_BATTERY, EEBatteryBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineBlockEntity>> TURBINE = register("turbine", CrystalnexusModBlocks.TURBINE, TurbineBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineblockBlockEntity>> MACHINEBLOCK = register("machineblock", CrystalnexusModBlocks.MACHINEBLOCK, MachineblockBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreProcessorBlockEntity>> ORE_PROCESSOR = register("ore_processor", CrystalnexusModBlocks.ORE_PROCESSOR, OreProcessorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineCoreBlockEntity>> MACHINE_CORE = register("machine_core", CrystalnexusModBlocks.MACHINE_CORE, MachineCoreBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineEnergyInputBlockEntity>> MACHINE_ENERGY_INPUT = register("machine_energy_input", CrystalnexusModBlocks.MACHINE_ENERGY_INPUT, MachineEnergyInputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomaticComposterBlockEntity>> BIOMATIC_COMPOSTER = register("biomatic_composter", CrystalnexusModBlocks.BIOMATIC_COMPOSTER, BiomaticComposterBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomaticConstructorBlockEntity>> BIOMATIC_CONSTRUCTOR = register("biomatic_constructor", CrystalnexusModBlocks.BIOMATIC_CONSTRUCTOR, BiomaticConstructorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomaticSimulatorBlockEntity>> BIOMATIC_SIMULATOR = register("biomatic_simulator", CrystalnexusModBlocks.BIOMATIC_SIMULATOR, BiomaticSimulatorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryMonitorBlockEntity>> BATTERY_MONITOR = register("battery_monitor", CrystalnexusModBlocks.BATTERY_MONITOR, BatteryMonitorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiblockResearchStationBlockEntity>> MULTIBLOCK_RESEARCH_STATION = register("multiblock_research_station", CrystalnexusModBlocks.MULTIBLOCK_RESEARCH_STATION,
			MultiblockResearchStationBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarpPadBlockEntity>> WARP_PAD = register("warp_pad", CrystalnexusModBlocks.WARP_PAD, WarpPadBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PistonGeneratorBlockEntity>> PISTON_GENERATOR = register("piston_generator", CrystalnexusModBlocks.PISTON_GENERATOR, PistonGeneratorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InvertPistonGeneratorBlockEntity>> INVERT_PISTON_GENERATOR = register("invert_piston_generator", CrystalnexusModBlocks.INVERT_PISTON_GENERATOR,
			InvertPistonGeneratorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeStraightBlockEntity>> PIPE_STRAIGHT = register("pipe_straight", CrystalnexusModBlocks.PIPE_STRAIGHT, PipeStraightBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeJunctionBlockEntity>> PIPE_JUNCTION = register("pipe_junction", CrystalnexusModBlocks.PIPE_JUNCTION, PipeJunctionBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamCollectorBlockEntity>> STEAM_COLLECTOR = register("steam_collector", CrystalnexusModBlocks.STEAM_COLLECTOR, SteamCollectorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamChamberBlockEntity>> STEAM_CHAMBER = register("steam_chamber", CrystalnexusModBlocks.STEAM_CHAMBER, SteamChamberBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyerBeltBlockEntity>> CONVEYER_BELT = register("conveyer_belt", CrystalnexusModBlocks.CONVEYER_BELT, ConveyerBeltBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyerBeltInputBlockEntity>> CONVEYER_BELT_INPUT = register("conveyer_belt_input", CrystalnexusModBlocks.CONVEYER_BELT_INPUT, ConveyerBeltInputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyerBeltOutputBlockEntity>> CONVEYER_BELT_OUTPUT = register("conveyer_belt_output", CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT, ConveyerBeltOutputBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemElevatorBlockEntity>> ITEM_ELEVATOR = register("item_elevator", CrystalnexusModBlocks.ITEM_ELEVATOR, ItemElevatorBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemElevatorDownBlockEntity>> ITEM_ELEVATOR_DOWN = register("item_elevator_down", CrystalnexusModBlocks.ITEM_ELEVATOR_DOWN, ItemElevatorDownBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedConveyerBeltBlockEntity>> ADVANCED_CONVEYER_BELT = register("advanced_conveyer_belt", CrystalnexusModBlocks.ADVANCED_CONVEYER_BELT,
			AdvancedConveyerBeltBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingFactoryBlockEntity>> CRAFTING_FACTORY = register("crafting_factory", CrystalnexusModBlocks.CRAFTING_FACTORY, CraftingFactoryBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NodeMinerBlockEntity>> NODE_MINER = register("node_miner", CrystalnexusModBlocks.NODE_MINER, NodeMinerBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE = register("steam_engine", CrystalnexusModBlocks.STEAM_ENGINE, SteamEngineBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineUpgradeBlockEntity>> STEAM_ENGINE_UPGRADE = register("steam_engine_upgrade", CrystalnexusModBlocks.STEAM_ENGINE_UPGRADE, SteamEngineUpgradeBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingularityMatrixBlockEntity>> SINGULARITY_MATRIX = register("singularity_matrix", CrystalnexusModBlocks.SINGULARITY_MATRIX, SingularityMatrixBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_PURIFIER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRYSTAL_PURIFIER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_ACCEPTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRYSTAL_ACCEPTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CRYSTAL_ACCEPTER.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GROWTH_CHAMBER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GROWTH_CHAMBER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GROWTH_CHAMBER.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_CRUSHER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRYSTAL_CRUSHER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DUST_SEPARATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DUST_SEPARATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_GUIDE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_COLLECTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EXTRACTINATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EXTRACTINATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHLOROPHYTE_ACCELERATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHLOROPHYTE_ACCELERATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, METALLURGIC_RECRYSTALLIZER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, METALLURGIC_RECRYSTALLIZER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTOR_COMPUTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, REACTOR_COMPUTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REACTOR_COMPUTER.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTOR_ENERGY_OUTPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, REACTOR_ENERGY_OUTPUT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTOR_FLUID_INPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, REACTOR_FLUID_INPUT.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IRON_SMELTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, IRON_SMELTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_SMELTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRYSTAL_SMELTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INVERTIUM_SMELTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INVERTIUM_SMELTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHLOROPHYTE_SMELTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHLOROPHYTE_SMELTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TESSERACT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TESSERACT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TESSERACT_OUTPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TESSERACT_OUTPUT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CIRCUIT_PRESS.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CIRCUIT_PRESS.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLU_TNT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FACTORY_CONTROLLER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FACTORY_CONTROLLER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FACTORY_ITEM_CONTROLLER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FACTORY_ITEM_CONTROLLER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FACTORY_OUTPUT_CONTROLLER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FACTORY_OUTPUT_CONTROLLER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INVERTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INVERTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTION_CHAMBER_COMPUTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, REACTION_CHAMBER_COMPUTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, REACTION_ENERGY_INPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, REACTION_ENERGY_INPUT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ULTIMA_SMELTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ULTIMA_SMELTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_EXTRACTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_EXTRACTOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRYSTAL_ENERGY_GUIDE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRYSTAL_ENERGY_GUIDE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_REFRACTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENERGY_SPLITTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MATTER_TRANSMUTATION_TABLE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MATTER_TRANSMUTATION_TABLE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_PLACER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BLOCK_PLACER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SINGULARITY_COMPRESSOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SINGULARITY_COMPRESSOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ZERO_POINT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ZERO_POINT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHEMICAL_REACTION_CHAMBER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHEMICAL_REACTION_CHAMBER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONTAINER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONDUCTIVE_ENERGY_GUIDE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONDUCTIVE_ENERGY_GUIDE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONDUCTIVE_ENERGY_REFRACTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONDUCTIVE_ENERGY_SPLITTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QUANTUM_MINER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QUANTUM_MINER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTERY.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EE_BATTERY.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EE_BATTERY.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TURBINE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TURBINE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINEBLOCK.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ORE_PROCESSOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ORE_PROCESSOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_CORE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_ENERGY_INPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MACHINE_ENERGY_INPUT.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BIOMATIC_COMPOSTER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BIOMATIC_COMPOSTER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BIOMATIC_CONSTRUCTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BIOMATIC_CONSTRUCTOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BIOMATIC_SIMULATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BIOMATIC_SIMULATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTERY_MONITOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MULTIBLOCK_RESEARCH_STATION.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WARP_PAD.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WARP_PAD.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PISTON_GENERATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PISTON_GENERATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PISTON_GENERATOR.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INVERT_PISTON_GENERATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INVERT_PISTON_GENERATOR.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, INVERT_PISTON_GENERATOR.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PIPE_STRAIGHT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PIPE_STRAIGHT.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PIPE_JUNCTION.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PIPE_JUNCTION.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STEAM_COLLECTOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_COLLECTOR.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STEAM_CHAMBER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYER_BELT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYER_BELT_INPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONVEYER_BELT_OUTPUT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_ELEVATOR.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_ELEVATOR_DOWN.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ADVANCED_CONVEYER_BELT.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRAFTING_FACTORY.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CRAFTING_FACTORY.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NODE_MINER.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NODE_MINER.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STEAM_ENGINE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STEAM_ENGINE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_ENGINE.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STEAM_ENGINE_UPGRADE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STEAM_ENGINE_UPGRADE.get(), (blockEntity, side) -> blockEntity.getEnergyStorage());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_ENGINE_UPGRADE.get(), (blockEntity, side) -> blockEntity.getFluidTank());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SINGULARITY_MATRIX.get(), SidedInvWrapper::new);
	}
}