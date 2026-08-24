package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.block.entity.ReactorControlRodBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
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

	private ReactorCoolingGameTests() {
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
	public static void temperatureEfficiencyChangesSmoothlyAtMilestones(GameTestHelper helper) {
		BlockPos firstPos = new BlockPos(15, 1, 15);
		BlockPos secondPos = new BlockPos(20, 1, 15);
		ReactorComputerBlockEntity first = preparedComputer(helper, firstPos);
		ReactorComputerBlockEntity second = preparedComputer(helper, secondPos);
		first.getPersistentData().putDouble("heat", 499);
		second.getPersistentData().putDouble("heat", 500);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(firstPos), first);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(secondPos), second);

		helper.assertTrue(Math.abs(first.getPersistentData().getDouble("lastFEt") - second.getPersistentData().getDouble("lastFEt")) < 50,
				"Crossing a temperature milestone should not cause a large FE/t jump");
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
		computer.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get()));
		computer.getFluidTank().fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);

		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);

		helper.assertTrue(computer.getPersistentData().getDouble("lastFEt") == 0
				&& computer.getPersistentData().getDouble("heatGenerated") == 0
				&& computer.getPersistentData().getDouble("heat") < 800
				&& computer.getPersistentData().getDouble("coolantUsed") > 0,
				"A fully inserted reactor must stop fission while continuing to cool");
		helper.assertTrue(computer.getItem(0).getCount() == 1 && computer.getPersistentData().getDouble("progress") == 0,
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
		computer.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get()));
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
		computer.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get(), 2));

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

		computer.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get(), 2));
		computer.setItem(2, ItemStack.EMPTY);
		computer.getPersistentData().putDouble("progress", 2000 - fullBurn * 1.25);
		controlRod.setInsertion(50);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(computer.getItem(0).getCount() == 2 && computer.getItem(2).isEmpty(),
				"Half reactivity must delay the fuel and waste cycle");

		controlRod.setInsertion(0);
		ReactorSimulation.tick(helper.getLevel(), helper.absolutePos(computerPos), computer);
		helper.assertTrue(computer.getItem(0).getCount() == 1 && computer.getItem(2).getCount() == 1,
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
		helper.setBlock(inputTarget.north(), CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get());
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
		computer.setItem(0, new ItemStack(CrystalnexusModItems.BLUTONIUM_INGOT.get()));
		return computer;
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
