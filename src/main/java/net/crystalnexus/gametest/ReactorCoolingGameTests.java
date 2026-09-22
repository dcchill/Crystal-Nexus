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

	@GameTest(template = "zero_point")
	public static void reactorEnergyOutputPushesStoredEnergy(GameTestHelper helper) {
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

		helper.assertTrue(receiver.getEnergyStorage().getEnergyStored() == 1_000,
				"A reactor energy output must push the computer's FE to adjacent receivers");
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
	public static void conductorsDoNotCarryCoolantFromFluidInputs(GameTestHelper helper) {
		ReactorLayout layout = layout(helper, 0, 1, "FHC");

		assertCooling(helper, layout, 1, 0);
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
	public static void nitrogenRemovesTwiceTheHeatOfWater(GameTestHelper helper) {
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

		helper.assertTrue(nitrogen.getPersistentData().getDouble("heatRemoved") > water.getPersistentData().getDouble("heatRemoved") * 1.9,
				"Nitrogen must remove twice as much reactor heat per mB as water");
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

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

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

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

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
			ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		}
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
