package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.item.ReactorFuelCellItem;
import net.crystalnexus.block.entity.ReactorCoreBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemInputBlockEntity;
import net.crystalnexus.block.entity.MultiblockItemOutputBlockEntity;
import net.crystalnexus.block.entity.ReactorControlRodBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.reactor.ReactorBalance;
import net.crystalnexus.reactor.ReactorLayout;
import net.crystalnexus.reactor.ReactorPlanner;
import net.crystalnexus.reactor.ReactorSimulation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class ReactorCoolingGameTests {
	private static final int INTERIOR_Y = 1;
	private static final BlockPos CORE_POS = new BlockPos(3, 1, 3);

	private ReactorCoolingGameTests() {
	}

	private static final class NotificationCountingComputer extends ReactorComputerBlockEntity {
		private int inventoryNotifications;
		private NotificationCountingComputer(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
			super(pos, state);
		}
		@Override public void setChanged() {
			inventoryNotifications++;
			super.setChanged();
		}
	}

	@GameTest(template = "zero_point")
	public static void routineControllerUpdatesPreserveInventoryNotifications(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity original = preparedComputer(helper, pos);
		var computer = new NotificationCountingComputer(helper.absolutePos(pos), original.getBlockState());
		helper.getLevel().setBlockEntity(computer);
		computer.updateLayoutCache(original.getCachedLayout());
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.inventoryNotifications = 0;
		computer.getEnergyStorage().generateEnergy(100, false);
		computer.getEnergyStorage().extractEnergy(50, false);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(computer.inventoryNotifications == 0,
				"Routine energy, coolant and telemetry must avoid inventory notifications");
		var restored = new ReactorComputerBlockEntity(helper.absolutePos(pos), computer.getBlockState());
		restored.loadAdditional(computer.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
		helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == computer.getEnergyStorage().getEnergyStored()
				&& restored.getFluidTank().getFluidAmount() == computer.getFluidTank().getFluidAmount()
				&& restored.getPersistentData().getDouble("heat") == computer.getPersistentData().getDouble("heat"),
				"Routine updates must still save and reload energy, coolant and telemetry");
		computer.setItem(1, new ItemStack(CrystalnexusModItems.REACTOR_UPGRADE.get()));
		helper.assertTrue(computer.inventoryNotifications > 0, "Inventory changes must retain normal notifications");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fractionalWearPersistsWithoutDirtyingOnZero(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, CrystalnexusModBlocks.REACTOR_CORE.get());
		ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
		core.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
		var chunk = helper.getLevel().getChunkAt(helper.absolutePos(pos));
		try {
			chunk.setUnsaved(false);
			core.addWear(0, 0);
			helper.assertTrue(!chunk.isUnsaved(), "Zero wear must not dirty the chunk");
			core.addWear(0, 0.4);
			helper.assertTrue(chunk.isUnsaved() && core.getItem(0).getDamageValue() == 0,
					"Fractional wear must dirty the chunk without damaging the cell yet");
			var saved = core.saveWithFullMetadata(helper.getLevel().registryAccess());
			var restored = new ReactorCoreBlockEntity(helper.absolutePos(pos), core.getBlockState());
			restored.loadAdditional(saved, helper.getLevel().registryAccess());
			restored.addWear(0, 0.6);
			helper.assertTrue(restored.getItem(0).getDamageValue() == 1,
					"Saved fractional wear must contribute to damage after reload");
			restored.getItem(0).setDamageValue(restored.getItem(0).getMaxDamage() - 1);
			restored.addWear(0, 1);
			helper.assertTrue(restored.getItem(0).is(CrystalnexusModItems.SPENT_REACTOR_CELL.get()),
					"Exhaustion must still replace fuel with a spent cell");
		} finally {
			chunk.setUnsaved(true);
		}
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void simulationRefreshesCoreAndRodReferencesEveryCall(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		var world = helper.getLevel();
		var rod = computer.getCachedLayout().fuelRods().get(0);
		ReactorSimulation.tick(world, helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") > 0, "Initial core must generate energy");
		world.setBlock(rod.pos(), Blocks.AIR.defaultBlockState(), 3);
		ReactorSimulation.tick(world, helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") == 0, "Removed cores must be forgotten immediately");
		world.setBlock(rod.pos(), CrystalnexusModBlocks.REACTOR_CORE.get().defaultBlockState(), 3);
		ReactorCoreBlockEntity replacement = (ReactorCoreBlockEntity) world.getBlockEntity(rod.pos());
		replacement.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
		((ReactorControlRodBlockEntity) world.getBlockEntity(rod.controlRodPos())).setInsertion(100);
		ReactorSimulation.tick(world, helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") == 0, "Rod changes must apply on the next call");
		world.setBlock(rod.controlRodPos(), Blocks.AIR.defaultBlockState(), 3);
		ReactorSimulation.tick(world, helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") > 0, "Replaced core and removed control must use fresh references");
		ReactorLayout changed = layout(helper, 0, 1, "FC");
		computer.updateLayoutCache(changed);
		loadFuel(helper, changed, 3);
		ReactorSimulation.tick(world, helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") > 0, "Changed layouts must rebuild simulation storage");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void periodicValidationDetectsNonPlayerStructureBreaks(GameTestHelper helper) {
		byte[] columns = new byte[9];
		java.util.Arrays.fill(columns, (byte) 4);
		columns[4] = 0;
		var build = ReactorPlanner.fromColumns(5, columns);
		for (BlockPos cursor : BlockPos.betweenClosed(-1, -1, -1, 5, 5, 5)) helper.setBlock(cursor, Blocks.AIR);
		for (var entry : build.blocks().entrySet()) helper.setBlock(entry.getKey(), entry.getValue());
		BlockPos pos = new BlockPos(1, 1, 0);
		ReactorComputerBlockEntity computer = helper.getBlockEntity(pos);
		ReactorComputerBlockEntity.tick(helper.getLevel(), helper.absolutePos(pos), computer.getBlockState(), computer);
		helper.assertTrue(computer.getCachedLayout().valid, "Initial structure must validate");
		helper.setBlock(new BlockPos(0, 3, 3), Blocks.AIR);
		for (int i = 0; i < 21; i++)
			ReactorComputerBlockEntity.tick(helper.getLevel(), helper.absolutePos(pos), computer.getBlockState(), computer);
		helper.assertTrue(!computer.getCachedLayout().valid && !computer.getPersistentData().getBoolean("canOpenInventory"),
				"Periodic validation must detect structure changes without a player break event");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void feGraphTracksRealDrawAndAvailableGeneration(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		var data = computer.getPersistentData();
		data.putDouble("heat", 700);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		double fullCapacity = data.getDouble("maxFEt");
		helper.assertTrue(fullCapacity > 0 && data.getDouble("lastFEOutput") == 0,
				"An unloaded energy grid must show zero delivered FE and positive generation capacity");
		computer.getEnergyStorage().extractEnergy(900, true);
		computer.getEnergyStorage().extractEnergy(100, false);
		computer.getEnergyStorage().extractEnergy(200, false);
		data.putDouble("heat", 700);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(data.getDouble("lastFEOutput") == 300 && data.getDouble("maxFEt") == fullCapacity,
				"FE output must count real deliveries while maximum generation stays independent of draw");
		computer.getFluidTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
		data.putDouble("heat", 700);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(data.getDouble("lastFEOutput") == 0 && data.getDouble("maxFEt") > 0
				&& data.getDouble("maxFEt") < fullCapacity && data.getDouble("maxFEt") == data.getDouble("lastFEt"),
				"Generation ceiling must reflect coolant throttling and output must reset after draw stops");
		data.putBoolean("canOpenInventory", false);
		computer.getEnergyStorage().extractEnergy(50, false);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(data.getDouble("maxFEt") == 0 && data.getDouble("lastFEOutput") == 50,
				"Offline reactors can deliver stored FE but have no generation capacity");
		data.putBoolean("canOpenInventory", true);
		data.putDouble("heat", 1200);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(data.getDouble("maxFEt") == 0, "SCRAM must clear the generation ceiling");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void graphRecordsWithoutAnOpenMenuAndRestoresOnReopen(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		computer.shouldRecheckLayout(Integer.MAX_VALUE);
		var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "reactor-history"),
				net.minecraft.server.level.ClientInformation.createDefault());
		for (int opening = 0; opening < 2; opening++) {
			for (int tick = 0; tick < 10; tick++)
				ReactorComputerBlockEntity.tick(helper.getLevel(), computer.getBlockPos(), computer.getBlockState(), computer);
			var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
			try {
				buffer.writeBlockPos(computer.getBlockPos());
				var rods = computer.getCachedLayout().fuelRods();
				buffer.writeVarInt(rods.size());
				for (var rod : rods) buffer.writeBlockPos(rod.pos());
				computer.graphHistory().write(buffer);
				var menu = new net.crystalnexus.world.inventory.ReactorGUIMenu(1, player.getInventory(), buffer);
				helper.assertTrue(menu.graphHistory().size() == opening + 1,
						"Opening the GUI must restore samples recorded while no menu was open");
				helper.assertTrue(menu.graphHistory().heat(opening) == computer.getPersistentData().getDouble("heatGenerated")
						&& menu.graphHistory().coolingCapacity(opening) == computer.getPersistentData().getDouble("maxCooling")
						&& menu.graphHistory().temperature(opening) == computer.getPersistentData().getDouble("heat")
						&& menu.graphHistory().feOutput(opening) == computer.getPersistentData().getDouble("lastFEOutput")
						&& menu.graphHistory().maxFe(opening) == computer.getPersistentData().getDouble("maxFEt"),
						"Reopened history must retain heat, cooling, temperature, actual FE output and maximum FE/t");
				menu.removed(player);
			} finally {
				buffer.release();
			}
		}
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void coolingCeilingTracksThroughputSupplyAndCoolantType(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		int[] amounts = {1000, 1, 0};
		double[] waterCeilings = {900.34, 15.34, 0.34};
		for (var fluid : java.util.List.of(Fluids.WATER, CrystalnexusModFluids.NITROGEN.get())) {
			for (int i = 0; i < amounts.length; i++) {
				computer.getFluidTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
				if (amounts[i] > 0) computer.getFluidTank().fill(new FluidStack(fluid, amounts[i]), IFluidHandler.FluidAction.EXECUTE);
				computer.getPersistentData().putDouble("heat", 700);
				tickAtFullLoad(helper, computer, pos);
				var data = computer.getPersistentData();
				double expected = fluid == Fluids.WATER ? waterCeilings[i] : (waterCeilings[i] - 0.34) * 2.5 + 0.34;
				helper.assertTrue(Math.abs(data.getDouble("maxCooling") - expected) < 1e-9,
						"Cooling ceiling must reflect throughput and coolant available before consumption: " + fluid + ", " + amounts[i]);
				helper.assertTrue(data.getDouble("heatRemoved") <= data.getDouble("maxCooling") + 1e-9,
						"Actual cooling must not exceed the available ceiling");
			}
		}
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		computer.getPersistentData().putDouble("heat", 700);
		computer.setAllControlRodInsertion(100);
		tickAtFullLoad(helper, computer, pos);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("maxCooling") - 900.34) < 1e-9,
				"Idle reactors must retain their available cooling ceiling");
		computer.getPersistentData().putBoolean("canOpenInventory", false);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("maxCooling") == 0, "Invalid reactors must clear their cooling ceiling");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void coolingCeilingIncludesPermafrostContribution(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		for (int slot = 0; slot < 3; slot++) core.setItem(slot, new ItemStack(CrystalnexusModItems.OVERTONIUM_FUEL_CELL.get()));
		computer.setItem(1, new ItemStack(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()));
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
		computer.getPersistentData().putDouble("heat", 1000);
		tickAtFullLoad(helper, computer, pos);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("maxCooling") - 30.49) < 1e-9,
				"Ceiling must include available water, permafrost cooling and passive heat loss");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void thermalReadingsIncludePassiveLossAndExplainTemperatureChange(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		for (int mode = 0; mode < 3; mode++) {
			computer.getPersistentData().putDouble("heat", 700);
			computer.getFluidTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
			if (mode != 1) computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
			if (mode == 2) computer.setAllControlRodInsertion(100);
			tickAtFullLoad(helper, computer, pos);
			var data = computer.getPersistentData();
			double heating = data.getDouble("heatGenerated"), cooling = data.getDouble("heatRemoved");
			double expectedCooling = data.getDouble("coolantUsed") * ReactorBalance.HEAT_PER_MB_COOLANT
					+ (700 - ReactorBalance.AMBIENT_TEMPERATURE) * ReactorBalance.PASSIVE_HEAT_LOSS_PER_DEGREE;
			helper.assertTrue(Math.abs(cooling - expectedCooling) < 1e-9, "Cooling readings must include passive heat loss in mode " + mode);
			helper.assertTrue(Math.abs(data.getDouble("heat") - 700 - (heating - cooling)) < 1e-9,
					"Actual heat rates must explain temperature change in mode " + mode);
			helper.assertTrue(mode == 1 ? heating > cooling : cooling > heating,
					"Actual rates must distinguish dry heating from supplied or idle cooling in mode " + mode);
		}
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void demandCountsOnlyRealExtractionAndResets(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		var energy = computer.getEnergyStorage();
		energy.generateEnergy(1000, false);
		energy.extractEnergy(100, true);
		helper.assertTrue(computer.consumeExtractedEnergy() == 0, "Simulation must not count as grid demand");
		energy.extractEnergy(200, false);
		energy.extractEnergy(300, false);
		helper.assertTrue(computer.consumeExtractedEnergy() == 500, "Real transfers must accumulate");
		helper.assertTrue(computer.consumeExtractedEnergy() == 0, "Reading demand must reset it");
		energy.extractEnergy(1000, false);
		energy.extractEnergy(1000, false);
		helper.assertTrue(computer.consumeExtractedEnergy() == 500, "Unfulfilled extraction must not count");
		energy.generateEnergy(1000, false);
		energy.extractEnergy(1000, false);
		computer.getPersistentData().putBoolean("canOpenInventory", false);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(computer.consumeExtractedEnergy() == 0, "Offline ticks must discard demand");
		energy.generateEnergy(1000, false);
		energy.extractEnergy(1000, false);
		computer.loadAdditional(computer.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
		helper.assertTrue(computer.consumeExtractedEnergy() == 0, "Loading must clear transient demand");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void outputPortDrawRaisesHeatAndStoppingDrawRestoresIdleHeat(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		BlockPos outputPos = pos.east();
		helper.setBlock(outputPos, CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
		helper.setBlock(outputPos.east(), CrystalnexusModBlocks.BATTERY.get());
		MachineEnergyOutputBlockEntity output = helper.getBlockEntity(outputPos);
		computer.getPersistentData().putLong("multiblockMinBounds", helper.absolutePos(pos).asLong());
		computer.getPersistentData().putLong("multiblockMaxBounds", helper.absolutePos(outputPos.east()).asLong());
		output.bindController(helper.absolutePos(pos));
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		double idleHeat = computer.getPersistentData().getDouble("heatGenerated");
		output.pushEnergy();
		computer.getPersistentData().putDouble("heat", ReactorBalance.AMBIENT_TEMPERATURE);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("heatGenerated") - idleHeat * 5) < 1e-9,
				"Delivering full output through a port must produce five times idle heat");
		computer.getPersistentData().putDouble("heat", ReactorBalance.AMBIENT_TEMPERATURE);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("heatGenerated") - idleHeat) < 1e-9,
				"Stopping draw must restore idle heat on the next tick");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void loadRaisesCoolantDemandWithoutChangingGenerationOrFuelRules(GameTestHelper helper) {
		BlockPos pos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, pos);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 10000), IFluidHandler.FluidAction.EXECUTE);
		ReactorCoreBlockEntity core = (ReactorCoreBlockEntity) helper.getLevel().getBlockEntity(computer.getCachedLayout().fuelRods().get(0).pos());
		for (int slot = 0; slot < 3; slot++) core.setItem(slot, new ItemStack(CrystalnexusModItems.OVERTONIUM_FUEL_CELL.get()));
		double idleHeat = 0, idleCoolant = 0, idleFe = 0, idleBurn = 0, idleWear = 0, idleMaxCooling = 0;
		for (int load = 0; load <= 1; load++) {
			computer.getPersistentData().putDouble("heat", 1000);
			computer.getPersistentData().putDouble("progress", 1999.999);
			computer.setItem(2, ItemStack.EMPTY);
			double wearBefore = core.getUpdateTag(helper.getLevel().registryAccess()).getDouble("Wear0");
			if (load == 1) computer.getEnergyStorage().extractEnergy((int) idleFe, false);
			ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
			var data = computer.getPersistentData();
			double wear = core.getUpdateTag(helper.getLevel().registryAccess()).getDouble("Wear0") - wearBefore;
			if (load == 0) {
				idleHeat = data.getDouble("heatGenerated");
				idleCoolant = data.getDouble("coolantDemand");
				idleFe = data.getDouble("lastFEt");
				idleBurn = data.getDouble("progress");
				idleWear = wear;
				idleMaxCooling = data.getDouble("maxCooling");
			} else {
				helper.assertTrue(Math.abs(data.getDouble("heatGenerated") - idleHeat * 5) < 1e-9
						&& data.getDouble("coolantDemand") > idleCoolant, "Full load must raise heat and coolant demand");
				helper.assertTrue(data.getDouble("maxCooling") == idleMaxCooling,
						"Cooling ceiling must be independent of energy draw when coolant throughput and supply are unchanged");
				helper.assertTrue(data.getDouble("lastFEt") == idleFe && data.getDouble("progress") == idleBurn
						&& Math.abs(wear - idleWear) < 1e-9, "Load alone must preserve FE generation, fuel wear and cycle progress");
			}
			helper.assertTrue(computer.getItem(2).getCount() == 1, "Each completed cycle must produce the same waste");
		}
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void plannerIncludesReflectorsInBuildsAndEstimates(GameTestHelper helper) {
		var candidates = ReactorPlanner.candidates(5);
		var reflector = CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get();
		var build = candidates.stream().filter(candidate -> candidate.blocks().values().stream()
				.anyMatch(state -> state.is(reflector))).findFirst().orElseThrow();
		helper.assertTrue(build.layout().valid, "Reflector candidates must be valid reactors");
		var withoutReflectors = new java.util.LinkedHashMap<>(build.blocks());
		withoutReflectors.replaceAll((pos, state) -> state.is(reflector) ? Blocks.AIR.defaultBlockState() : state);
		var view = new ReactorPlanner.Build(build.size(), withoutReflectors, null);
		var layout = ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(4, 4, 4));
		var baseline = new ReactorPlanner.Build(build.size(), withoutReflectors, layout);
		helper.assertTrue(layout.valid && build.layout().outputMultiplier > layout.outputMultiplier
				&& build.layout().heatMultiplier > layout.heatMultiplier,
				"Planner estimates must include reflector output and heat bonuses");
		helper.assertTrue(ReactorPlanner.estimate(build, 100).fePerTick() > ReactorPlanner.estimate(baseline, 100).fePerTick(),
				"Reflectors must increase estimated output");
		helper.assertTrue(candidates.stream().anyMatch(candidate -> candidate.blocks().values().stream()
				.anyMatch(state -> state.is(CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get()))),
				"Moderator candidates must remain available");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void reactorEnergyOutputDoesNotFillAnUnboundInputPort(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(10, 1, 10);
		BlockPos outputPos = computerPos.east();
		BlockPos receiverPos = outputPos.east();
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		helper.setBlock(outputPos, CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
		helper.setBlock(receiverPos, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		MachineEnergyOutputBlockEntity output = helper.getBlockEntity(outputPos);
		MachineEnergyInputBlockEntity receiver = helper.getBlockEntity(receiverPos);
		BlockPos absoluteComputer = helper.absolutePos(computerPos);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putLong("multiblockMinBounds", absoluteComputer.asLong());
		computer.getPersistentData().putLong("multiblockMaxBounds", helper.absolutePos(receiverPos).asLong());
		output.bindController(absoluteComputer);
		computer.getEnergyStorage().generateEnergy(1_000, false);

		computer.pushEnergyOutputs();

		helper.assertTrue(receiver.getEnergyStorage().getEnergyStored() == 0 && computer.getEnergyStorage().getEnergyStored() == 1_000,
				"An unbound energy input must not buffer FE from an adjacent output");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fuelCellTiersHaveExpectedOutputAndHeat(GameTestHelper helper) {
		ReactorFuelCellItem normal = (ReactorFuelCellItem) CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get();
		ReactorFuelCellItem pure = (ReactorFuelCellItem) CrystalnexusModItems.PURE_BLUTONIUM_FUEL_CELL.get();
		ReactorFuelCellItem overtonium = (ReactorFuelCellItem) CrystalnexusModItems.OVERTONIUM_FUEL_CELL.get();
		helper.assertTrue(pure.feMultiplier() == normal.feMultiplier() && pure.heatMultiplier() < normal.heatMultiplier(),
				"Pure Blutonium must retain normal output with less heat");
		helper.assertTrue(overtonium.feMultiplier() == normal.feMultiplier() * 3
				&& overtonium.heatMultiplier() > normal.heatMultiplier(),
				"Overtonium must provide triple output with more heat");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void spacedNeutronicRedstoneFuelCellsProduceMoreEnergyAndHeat(GameTestHelper helper) {
		var manager = helper.getLevel().getRecipeManager();
		var redstoneRecipe = manager.byKey(net.minecraft.resources.ResourceLocation.parse("crystalnexus:fluid_chemical_reaction_neutronic_redstone"));
		var fuelCellRecipe = manager.byKey(net.minecraft.resources.ResourceLocation.parse("crystalnexus:neutronic_redstone_fuel_cell"));
		helper.assertTrue(redstoneRecipe.isPresent() && fuelCellRecipe.isPresent(),
				"Neutronic Redstone and fuel cell recipes must load");
		if (fuelCellRecipe.isPresent()) {
			var recipe = fuelCellRecipe.get().value();
			helper.assertTrue(recipe.getIngredients().size() == 5
					&& recipe.getIngredients().stream().filter(ingredient -> ingredient.test(new ItemStack(CrystalnexusModItems.REACTOR_FUEL_CELL.get()))).count() == 4
					&& recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(CrystalnexusModItems.NEUTRONIC_REDSTONE.get())))
					&& recipe.getResultItem(helper.getLevel().registryAccess()).is(CrystalnexusModItems.NEUTRONIC_REDSTONE_FUEL_CELL.get())
					&& recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 4,
					"Fuel-cell recipe must use one Neutronic Redstone and four reactor fuel cells to make four neutronic cells");
		}
		ReactorLayout layout = layout(helper, 0, 0, "F");
		helper.assertTrue(layout.valid, "Fuel rod test layout must be valid: " + layout.reason);
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.getPersistentData().putDouble("heat", 740);
		ReactorCoreBlockEntity first = (ReactorCoreBlockEntity) helper.getLevel().getBlockEntity(layout.fuelRods().get(0).pos());
		((ReactorControlRodBlockEntity) helper.getLevel().getBlockEntity(layout.fuelRods().get(0).controlRodPos())).setInsertion(0);
		ItemStack neutronic = new ItemStack(CrystalnexusModItems.NEUTRONIC_REDSTONE_FUEL_CELL.get());
		first.setItem(0, neutronic);
		first.setItem(2, neutronic.copy());
		helper.assertTrue(ReactorSimulation.isFuel(neutronic)
				&& ((ReactorFuelCellItem) neutronic.getItem()).heatMultiplier()
						== ((ReactorFuelCellItem) CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()).heatMultiplier(),
				"Neutronic Redstone Fuel Cells must be accepted as fuel without changing base heat");
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		double spacedOutput = computer.getPersistentData().getDouble("lastFEt");
		double spacedHeat = computer.getPersistentData().getDouble("heatGenerated");
		helper.assertTrue(spacedOutput > 0 && spacedHeat > 0 && computer.getPersistentData().getDouble("progress") > 0,
				"Spaced fuel cells must generate energy and advance the burn cycle");
		first.setItem(1, neutronic.copy());
		first.setItem(2, ItemStack.EMPTY);
		computer.getPersistentData().putDouble("heat", 740);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		double adjacentOutput = computer.getPersistentData().getDouble("lastFEt");
		double adjacentHeat = computer.getPersistentData().getDouble("heatGenerated");
		helper.assertTrue(Math.abs(spacedOutput - adjacentOutput * 2) < 4,
				"Two spaced neutronic cells must produce twice the energy of consecutive cells");
		helper.assertTrue(Math.abs(spacedHeat - adjacentHeat * 2) < 4,
				"Two spaced neutronic cells must produce twice the heat of consecutive cells");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fissileFuelCellsHaveDistinctHeatAndLifetimes(GameTestHelper helper) {
		ItemStack overtonium = new ItemStack(CrystalnexusModItems.OVERTONIUM_FUEL_CELL.get());
		ItemStack azurine = new ItemStack(CrystalnexusModItems.AZURINE_FUEL_CELL.get());
		ItemStack chlorophyte = new ItemStack(CrystalnexusModItems.CHLOROPHYTE_FUEL_CELL.get());
		ReactorFuelCellItem overtoniumFuel = (ReactorFuelCellItem) overtonium.getItem();
		ReactorFuelCellItem azurineFuel = (ReactorFuelCellItem) azurine.getItem();
		ReactorFuelCellItem chlorophyteFuel = (ReactorFuelCellItem) chlorophyte.getItem();
		helper.assertTrue(azurineFuel.heatMultiplier() == 1670.0
				&& azurineFuel.feMultiplier() == overtoniumFuel.feMultiplier(),
				"Fissile Azurine must retain its output and require nitrogen cooling");
		helper.assertTrue(azurine.getMaxDamage() == overtonium.getMaxDamage() * 3 / 2,
				"Fissile Azurine must last fifty percent longer at the same reactor wear rate");
		helper.assertTrue(chlorophyteFuel.heatMultiplier() == 1670.0
				&& chlorophyte.getMaxDamage() == azurine.getMaxDamage(),
				"Fissile Chlorophyte must require nitrogen cooling and retain the standard medium burn duration");
		helper.assertTrue(ReactorSimulation.isFuel(azurine) && ReactorSimulation.isFuel(chlorophyte),
				"Both fissile cells must be accepted as reactor fuel");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void highHeatFuelRequiresNitrogenForSafeOperation(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");
		BlockPos waterPos = new BlockPos(15, 1, 15);
		BlockPos nitrogenPos = new BlockPos(20, 1, 15);
		helper.setBlock(waterPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		helper.setBlock(nitrogenPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity water = helper.getBlockEntity(waterPos);
		ReactorComputerBlockEntity nitrogen = helper.getBlockEntity(nitrogenPos);
		ReactorCoreBlockEntity core = helper.getLevel().getBlockEntity(layout.fuelRods().get(0).pos()) instanceof ReactorCoreBlockEntity reactorCore
				? reactorCore : null;
		helper.assertTrue(core != null, "The reference reactor must have a fuel core");
		for (ReactorComputerBlockEntity computer : java.util.List.of(water, nitrogen)) {
			computer.updateLayoutCache(layout);
			computer.getPersistentData().putBoolean("canOpenInventory", true);
			computer.getPersistentData().putInt("multiblockRadius", 1);
			computer.getPersistentData().putDouble("heat", 1000);
		}
		water.getFluidTank().fill(new FluidStack(Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
		nitrogen.getFluidTank().fill(new FluidStack(CrystalnexusModFluids.NITROGEN.get(), 16000), IFluidHandler.FluidAction.EXECUTE);
		for (var fuel : java.util.List.of(CrystalnexusModItems.AZURINE_FUEL_CELL.get(),
				CrystalnexusModItems.CHLOROPHYTE_FUEL_CELL.get(), CrystalnexusModItems.DARK_MATTER_FUEL_CELL.get())) {
			for (int slot = 0; slot < 3; slot++) core.setItem(slot, new ItemStack(fuel));
			water.getPersistentData().putDouble("heat", 1000);
			nitrogen.getPersistentData().putDouble("heat", 1000);
			for (int tick = 0; tick < 10; tick++) {
				tickAtFullLoad(helper, water, waterPos);
				tickAtFullLoad(helper, nitrogen, nitrogenPos);
			}
			helper.assertTrue("SCRAM".equals(water.getPersistentData().getString("reactorStatus")),
					fuel + " must exceed water's safe cooling capacity");
			helper.assertTrue(!"SCRAM".equals(nitrogen.getPersistentData().getString("reactorStatus")),
					fuel + " must remain safe with nitrogen cooling");
		}
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void threeSlotsScalePowerAndSpentCellsStopGenerating(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		int fullPower = (int) computer.getPersistentData().getDouble("lastFEt");
		core.setItem(1, ItemStack.EMPTY);
		core.setItem(2, ItemStack.EMPTY);
		computer.getPersistentData().putDouble("heat", ReactorBalance.AMBIENT_TEMPERATURE);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("lastFEt") * 3 - fullPower) <= 3,
				"One loaded cell should provide one third of a full core's power");
		core.setItem(0, new ItemStack(CrystalnexusModItems.SPENT_REACTOR_CELL.get()));
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") == 0,
				"Spent cells must not generate FE");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void itemPortsLoadCellsAndCollectOnlySpentCells(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		core.clearContent();
		computer.getPersistentData().putLong("multiblockMinBounds", helper.absolutePos(new BlockPos(1, 0, 1)).asLong());
		computer.getPersistentData().putLong("multiblockMaxBounds", helper.absolutePos(new BlockPos(5, 2, 5)).asLong());
		BlockPos inputPos = new BlockPos(1, 1, 3);
		BlockPos outputPos = new BlockPos(5, 1, 3);
		helper.setBlock(inputPos, CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get());
		helper.setBlock(outputPos, CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
		computer.updateLayoutCache(ReactorLayout.analyze(helper.getLevel(),
			helper.absolutePos(new BlockPos(1, 0, 1)), helper.absolutePos(new BlockPos(5, 2, 5))));
		MultiblockItemInputBlockEntity input = helper.getBlockEntity(inputPos);
		MultiblockItemOutputBlockEntity output = helper.getBlockEntity(outputPos);
		input.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
		input.setItem(1, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
		computer.pullFuelInputs();
		helper.assertTrue(core.getItem(0).is(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get())
				&& core.getItem(1).is(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()) && input.getItem(0).isEmpty() && input.getItem(1).isEmpty(),
				"Item input should fill empty core slots");
		computer.pushSpentCells();
		helper.assertTrue(output.isEmpty(), "Item output must leave live cells in cores");
		core.setItem(0, new ItemStack(CrystalnexusModItems.SPENT_REACTOR_CELL.get()));
		computer.pushSpentCells();
		helper.assertTrue(core.getItem(0).isEmpty() && output.getItem(0).is(CrystalnexusModItems.SPENT_REACTOR_CELL.get()),
				"Item output should collect spent cells");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void spentCellsWaitWhenOutputIsFull(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		computer.getPersistentData().putLong("multiblockMinBounds", helper.absolutePos(new BlockPos(1, 0, 1)).asLong());
		computer.getPersistentData().putLong("multiblockMaxBounds", helper.absolutePos(new BlockPos(5, 2, 5)).asLong());
		BlockPos outputPos = new BlockPos(5, 1, 3);
		helper.setBlock(outputPos, CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get());
		MultiblockItemOutputBlockEntity output = helper.getBlockEntity(outputPos);
		for (int slot = 0; slot < output.getContainerSize(); slot++)
			output.setItem(slot, new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get(), 64));
		core.setItem(0, new ItemStack(CrystalnexusModItems.SPENT_REACTOR_CELL.get()));
		computer.pushSpentCells();
		helper.assertTrue(core.getItem(0).is(CrystalnexusModItems.SPENT_REACTOR_CELL.get()),
				"A spent cell must stay in the core when all outputs are full");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fuelWearProducesSpentCell(GameTestHelper helper) {
		helper.setBlock(CORE_POS, CrystalnexusModBlocks.REACTOR_CORE.get());
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		ItemStack fuel = new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get());
		fuel.setDamageValue(1999);
		core.setItem(0, fuel);
		core.addWear(0, 0.5);
		helper.assertTrue(core.getItem(0).getDamageValue() == 1999, "Partial wear must be retained");
		core.addWear(0, 0.5);
		helper.assertTrue(core.getItem(0).is(CrystalnexusModItems.SPENT_REACTOR_CELL.get()),
				"Exhausted fuel must become a spent reactor cell");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void coreSavesDurabilityAndDropsStoredCells(GameTestHelper helper) {
		helper.setBlock(CORE_POS, CrystalnexusModBlocks.REACTOR_CORE.get());
		ReactorCoreBlockEntity core = helper.getBlockEntity(CORE_POS);
		ItemStack cell = new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get());
		cell.setDamageValue(123);
		core.setItem(1, cell);
		var registries = helper.getLevel().registryAccess();
		var saved = core.saveWithFullMetadata(registries);
		var restored = new ReactorCoreBlockEntity(helper.absolutePos(CORE_POS), core.getBlockState());
		restored.loadWithComponents(saved, registries);
		helper.assertTrue(restored.getItem(1).getDamageValue() == 123,
				"Cell durability must survive core save and load");
		helper.setBlock(CORE_POS, Blocks.AIR);
		var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(CORE_POS)).inflate(1));
		helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get())
				&& drop.getItem().getDamageValue() == 123), "Breaking a core must drop its stored cell");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void directContactActivatesOnlySideChannels(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");

		assertCooling(helper, layout, 8, 4);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void conductorsActivateTheReachedCoolantNetwork(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCCCC",
				"CHHHC",
				"CHFHC",
				"CHHHC",
				"CCCCC");

		assertCooling(helper, layout, 16, 16);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fourConductorsCanRelayHeat(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 5, "FHHHHC");

		assertCooling(helper, layout, 1, 1);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fiveConductorsCannotRelayHeat(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 6, "FHHHHHC");

		assertCooling(helper, layout, 1, 0);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fluidInputDoesNotNeedToTouchCoolantChannels(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FHC");

		assertCooling(helper, layout, 1, 1);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void sharedNetworksAreCountedOnce(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"HHH",
				"FRF");

		assertCooling(helper, layout, 3, 3);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void availableWaterRemovesStoredHeat(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.getPersistentData().putDouble("heat", 800);
		computer.setItem(0, ItemStack.EMPTY);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

		helper.assertTrue(computer.getPersistentData().getDouble("coolantUsed") > 1,
				"A hot reactor must consume coolant to shed stored heat");
		helper.assertTrue(computer.getPersistentData().getDouble("heat") < 800
				&& computer.getPersistentData().getDouble("heat") > ReactorBalance.AMBIENT_TEMPERATURE,
				"Cooling must reduce stored heat gradually instead of snapping to ambient");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void availableWaterCoolsAnIdleReactor(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.getPersistentData().putDouble("heat", 800);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

		helper.assertTrue(computer.getPersistentData().getDouble("coolantUsed") > 0
				&& computer.getPersistentData().getDouble("heat") < 800
				&& computer.getPersistentData().getDouble("heat") > ReactorBalance.AMBIENT_TEMPERATURE,
				"Available water must gradually cool stored heat after fuel generation stops");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void nitrogenRemovesTwoAndAHalfTimesTheHeatOfWater(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");
		BlockPos waterPos = new BlockPos(15, 1, 15);
		BlockPos nitrogenPos = new BlockPos(20, 1, 15);
		helper.setBlock(waterPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		helper.setBlock(nitrogenPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity water = helper.getBlockEntity(waterPos);
		ReactorComputerBlockEntity nitrogen = helper.getBlockEntity(nitrogenPos);
		for (ReactorComputerBlockEntity computer : java.util.List.of(water, nitrogen)) {
			computer.updateLayoutCache(layout);
			computer.getPersistentData().putBoolean("canOpenInventory", true);
			computer.getPersistentData().putInt("multiblockRadius", 1);
			computer.getPersistentData().putDouble("heat", 800);
		}
		water.getFluidTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
		nitrogen.getFluidTank().fill(new FluidStack(CrystalnexusModFluids.NITROGEN.get(), 1), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(waterPos), water);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(nitrogenPos), nitrogen);

		helper.assertTrue(nitrogen.getPersistentData().getDouble("heatRemoved") > water.getPersistentData().getDouble("heatRemoved") * 2.4,
				"Nitrogen must remove 2.5 times as much reactor heat per mB as water");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void suppliedReactorSettlesInTheTargetBand(GameTestHelper helper) {
		ReactorComputerBlockEntity computer = preparedComputer(helper, new BlockPos(15, 1, 15));
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);

		tick(helper, computer, new BlockPos(15, 1, 15), 350);

		double heat = computer.getPersistentData().getDouble("heat");
		helper.assertTrue(heat >= 600 && heat <= 750, "Supplied reactor should settle in the target band, got " + heat);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void limitedCoolantThrottlesInsteadOfStopping(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		computer.getPersistentData().putDouble("heat", 700);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);

		tickAtFullLoad(helper, computer, computerPos);

		helper.assertTrue("Coolant Limited".equals(computer.getPersistentData().getString("reactorStatus")),
				"Restricted coolant should report a limited reactor");
		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") > 0,
				"A coolant-limited reactor should keep generating throttled power");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void dryReactorKeepsMinimumHeatUntilScram(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		computer.getPersistentData().putDouble("heat", 1199.9);

		tickAtFullLoad(helper, computer, computerPos);

		helper.assertTrue("SCRAM".equals(computer.getPersistentData().getString("reactorStatus")),
				"A dry reactor must retain enough minimum fission heat to eventually SCRAM");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void permafrostReactorReachesTargetWithoutWater(GameTestHelper helper) {
		BlockPos computerPos = new BlockPos(15, 1, 15);
		ReactorComputerBlockEntity computer = preparedComputer(helper, computerPos);
		computer.setItem(1, new ItemStack(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()));

		tick(helper, computer, computerPos, 350);

		double heat = computer.getPersistentData().getDouble("heat");
		helper.assertTrue(heat >= 600 && heat <= 750 && computer.getPersistentData().getDouble("coolantUsed") == 0,
				"Permafrost should reach the target band without consuming water");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void hotterReactorsGenerateMoreEnergy(GameTestHelper helper) {
		BlockPos firstPos = new BlockPos(15, 1, 15);
		BlockPos secondPos = new BlockPos(20, 1, 15);
		ReactorComputerBlockEntity first = preparedComputer(helper, firstPos);
		ReactorComputerBlockEntity second = preparedComputer(helper, secondPos);
		first.getPersistentData().putDouble("heat", 700);
		second.getPersistentData().putDouble("heat", 900);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(firstPos), first);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(secondPos), second);

		helper.assertTrue(second.getPersistentData().getDouble("lastFEt") > first.getPersistentData().getDouble("lastFEt"),
				"A hotter reactor must generate more FE/t");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void controlRodsRegulateColumnsIndependently(GameTestHelper helper) {
		helper.assertTrue(ReactorControlRodBlockEntity.reactivity(0) == 1.0
				&& ReactorControlRodBlockEntity.reactivity(50) == 0.5
				&& ReactorControlRodBlockEntity.reactivity(100) == 0.0,
				"Control rod reactivity must equal one minus insertion percent");
		ReactorLayout layout = layout(helper, 0, 1, "FCF");
		ReactorControlRodBlockEntity left = helper.getBlockEntity(new BlockPos(2, 2, 2));
		ReactorControlRodBlockEntity right = helper.getBlockEntity(new BlockPos(4, 2, 2));

		left.setInsertion(0);
		right.setInsertion(100);
		ReactorLayout.OperatingTotals oneActive = layout.operatingTotals(helper.getLevel());
		helper.assertTrue(oneActive.reactiveFuelRods() == 1 && oneActive.reactiveFuelColumns() == 1,
				"A fully inserted control rod must shut down only its own fuel column");

		left.setInsertion(50);
		right.setInsertion(0);
		ReactorLayout.OperatingTotals oneAndAHalfActive = layout.operatingTotals(helper.getLevel());
		helper.assertTrue(oneAndAHalfActive.reactiveFuelRods() == 1.5 && oneAndAHalfActive.reactiveFuelColumns() == 1.5,
				"Independent 50% and 0% insertion should produce 150% total column reactivity");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void masterRodControlSetsEveryColumn(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FCF");
		helper.setBlock(new BlockPos(15, 1, 15), CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(new BlockPos(15, 1, 15));
		computer.updateLayoutCache(layout);
		computer.setAllControlRodInsertion(65);
		helper.assertTrue(layout.fuelRods().stream().allMatch(rod -> ((ReactorControlRodBlockEntity) helper.getLevel()
				.getBlockEntity(rod.controlRodPos())).getInsertion() == 65),
				"Master rod control must set every reactor control rod");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void multiRodEfficiencyIsAnAverage(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FCF");

		helper.assertTrue(layout.valid, "Expected a valid multi-rod layout, got: " + layout.reason);
		helper.assertTrue(Math.abs(layout.fuelEfficiency - 1.0) < 0.0001,
				"Two unmodified fuel rods must average to 1.0 efficiency, got " + layout.fuelEfficiency);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void airDoesNotHideMixedInteriorColumns(GameTestHelper helper) {
		BlockPos minBounds = new BlockPos(1, 0, 1);
		BlockPos maxBounds = new BlockPos(3, 4, 3);
		helper.setBlock(new BlockPos(2, 1, 2), CrystalnexusModBlocks.REACTOR_CORE.get());
		helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
		helper.setBlock(new BlockPos(2, 3, 2), CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get());

		ReactorLayout layout = ReactorLayout.analyze(helper.getLevel(), helper.absolutePos(minBounds), helper.absolutePos(maxBounds));

		helper.assertTrue(!layout.valid && layout.reason.contains("mixes"),
				"Air gaps must not hide a mixed core/coolant column; got: " + layout.reason);
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void fullInsertionStillCoolsStoredHeat(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FC");
		ReactorControlRodBlockEntity controlRod = helper.getBlockEntity(new BlockPos(2, 2, 2));
		controlRod.setInsertion(100);
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.getPersistentData().putDouble("heat", 800);
		loadFuel(helper, layout, 3);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") == 0
				&& computer.getPersistentData().getDouble("heatGenerated") == 0
				&& computer.getPersistentData().getDouble("heat") < 800
				&& computer.getPersistentData().getDouble("coolantUsed") > 0,
				"A fully inserted reactor must stop fission while continuing to cool");
		ReactorCoreBlockEntity core = helper.getBlockEntity(new BlockPos(2, 1, 2));
		helper.assertTrue(core.getItem(0).getDamageValue() == 0 && computer.getPersistentData().getDouble("progress") == 0,
				"A fully inserted reactor must not consume fuel");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void undersizedCoolingHardwareHasItsOwnWarning(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 4, "FFFFC");
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		computer.getPersistentData().putDouble("heat", 700);
		loadFuel(helper, layout, 3);
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

		helper.assertTrue("Cooling Capacity Limited".equals(computer.getPersistentData().getString("reactorStatus")),
				"Insufficient installed cooling capacity must have a distinct warning");
		helper.succeed();
	}

	@GameTest(template = "zero_point")
	public static void insertionScalesOutputHeatFuelAndWaste(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FC");
		ReactorControlRodBlockEntity controlRod = helper.getBlockEntity(new BlockPos(2, 2, 2));
		BlockPos computerPos = new BlockPos(15, 1, 15);
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		loadFuel(helper, layout, 3);

		controlRod.setInsertion(0);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		double fullFe = computer.getPersistentData().getDouble("lastFEt");
		double fullHeat = computer.getPersistentData().getDouble("heatGenerated");
		double fullBurn = computer.getPersistentData().getDouble("progress");

		computer.getPersistentData().putDouble("heat", ReactorBalance.AMBIENT_TEMPERATURE);
		computer.getPersistentData().putDouble("progress", 0);
		controlRod.setInsertion(50);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("lastFEt") - fullFe * 0.5) <= 1,
				"50% insertion must halve FE/t generation");
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("heatGenerated") - fullHeat * 0.5) < 0.0001,
				"50% insertion must halve heat generation");
		helper.assertTrue(Math.abs(computer.getPersistentData().getDouble("progress") - fullBurn * 0.5) < 0.0001,
				"50% insertion must halve fuel consumption progress");

		computer.setItem(2, ItemStack.EMPTY);
		computer.getPersistentData().putDouble("progress", 2000 - fullBurn * 1.25);
		controlRod.setInsertion(50);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(computer.getItem(2).isEmpty(),
				"Half reactivity must delay the fuel and waste cycle");

		controlRod.setInsertion(0);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(computer.getItem(2).getCount() == 1,
				"Full reactivity must complete the same fuel and waste cycle; fuel=" + computer.getItem(0).getCount()
						+ ", waste=" + computer.getItem(2).getCount() + ", progress=" + computer.getPersistentData().getDouble("progress"));
		helper.succeed();
	}

	private static ReactorLayout layout(GameTestHelper helper, int fluidInputRow, int fluidInputColumn, String... rows) {
		int width = rows[0].length();
		BlockPos minBounds = new BlockPos(1, 0, 1);
		BlockPos maxBounds = new BlockPos(width + 2, 2, rows.length + 2);
		for (int x = minBounds.getX(); x <= maxBounds.getX(); x++) {
			for (int y = minBounds.getY(); y <= maxBounds.getY(); y++) {
				for (int z = minBounds.getZ(); z <= maxBounds.getZ(); z++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		for (int row = 0; row < rows.length; row++) {
			helper.assertTrue(rows[row].length() == width, "Cooling test rows must have equal widths");
			for (int column = 0; column < width; column++) {
				BlockPos pos = new BlockPos(column + 2, INTERIOR_Y, row + 2);
				char symbol = rows[row].charAt(column);
				helper.setBlock(pos, blockFor(symbol));
				if (symbol == 'F') {
					helper.setBlock(pos.above(), CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get());
				}
			}
		}
		BlockPos inputTarget = new BlockPos(fluidInputColumn + 2, INTERIOR_Y, fluidInputRow + 2);
		helper.setBlock(inputTarget.north(), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
		return ReactorLayout.analyze(helper.getLevel(), helper.absolutePos(minBounds), helper.absolutePos(maxBounds));
	}

	private static ReactorComputerBlockEntity preparedComputer(GameTestHelper helper, BlockPos computerPos) {
		ReactorLayout layout = layout(helper, 0, 0,
				"CCC",
				"CFC",
				"CCC");
		helper.setBlock(computerPos, CrystalnexusModBlocks.REACTOR_COMPUTER.get());
		ReactorComputerBlockEntity computer = helper.getBlockEntity(computerPos);
		computer.updateLayoutCache(layout);
		computer.getPersistentData().putBoolean("canOpenInventory", true);
		computer.getPersistentData().putInt("multiblockRadius", 1);
		loadFuel(helper, layout, 3);
		return computer;
	}

	private static void loadFuel(GameTestHelper helper, ReactorLayout layout, int cellsPerRod) {
		for (ReactorLayout.FuelRod rod : layout.fuelRods()) {
			ReactorCoreBlockEntity core = (ReactorCoreBlockEntity) helper.getLevel().getBlockEntity(rod.pos());
			for (int slot = 0; slot < cellsPerRod; slot++)
				core.setItem(slot, new ItemStack(CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get()));
		}
	}

	private static void tick(GameTestHelper helper, ReactorComputerBlockEntity computer, BlockPos computerPos, int ticks) {
		for (int tick = 0; tick < ticks; tick++) {
			tickAtFullLoad(helper, computer, computerPos);
		}
	}

	private static void tickAtFullLoad(GameTestHelper helper, ReactorComputerBlockEntity computer, BlockPos pos) {
		int previousFe = Math.max(1, (int) computer.getPersistentData().getDouble("lastFEt"));
		computer.getPersistentData().putDouble("lastFEt", previousFe);
		computer.getEnergyStorage().generateEnergy(previousFe, false);
		computer.getEnergyStorage().extractEnergy(previousFe, false);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(pos), computer);
	}

	private static Block blockFor(char symbol) {
		return switch (symbol) {
			case 'F' -> CrystalnexusModBlocks.REACTOR_CORE.get();
			case 'C' -> CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get();
			case 'H' -> CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get();
			case 'R' -> CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get();
			default -> throw new IllegalArgumentException("Unknown reactor test symbol: " + symbol);
		};
	}

	private static void assertCooling(GameTestHelper helper, ReactorLayout layout, int installed, int active) {
		helper.assertTrue(layout.valid, "Expected a valid reactor layout, got: " + layout.reason);
		helper.assertTrue(layout.coolantChannels == installed,
				"Expected " + installed + " installed coolant channels, got " + layout.coolantChannels);
		helper.assertTrue(layout.activeCoolantChannels == active,
				"Expected " + active + " active coolant channels, got " + layout.activeCoolantChannels);
		helper.assertTrue(layout.coolantCapacityMbT == active * ReactorBalance.COOLANT_PER_CHANNEL_MB_T,
				"Coolant capacity must equal active channels times the per-channel balance value");
	}
}
