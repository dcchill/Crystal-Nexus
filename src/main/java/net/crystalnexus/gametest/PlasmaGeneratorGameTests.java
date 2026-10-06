package net.crystalnexus.gametest;

import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.block.PlasmaBlock;
import net.crystalnexus.block.entity.PlasmaBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.block.entity.PlasmaGeneratorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.energy.PlasmaGrid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.crystalnexus.world.inventory.PlasmaGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("crystalnexus_plasma")
@PrefixGameTestTemplate(false)
public final class PlasmaGeneratorGameTests {
    private PlasmaGeneratorGameTests() {}

    @GameTest(template = "plasma_gen")
    public static void consumesArgonAndGeneratesThroughReplaceablePorts(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.assertTrue(casing.size() >= 2,
            "The Plasma Generator template needs two replaceable Titanium Carbide Block positions");

        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        helper.assertTrue(!controller.validateStructureNow(), "The generator must require a fluid input port");

        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        MachineFluidInputBlockEntity input = helper.getBlockEntity(casing.get(0));

        helper.assertTrue(controller.validateStructureNow(),
            "A fluid input port must replace a Titanium Carbide Block anywhere in the structure");
        input.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        controller.serverTick();
        helper.assertTrue(input.isBoundTo(helper.absolutePos(controllerPos))
                && controller.getArgonTank().getFluidAmount() == 100,
            "A formed generator must bind its fluid input and accept Argon before an energy output is installed");
        helper.assertTrue(controller.getStatus().equals("No Plasma Injectors"),
            "An empty component grid must not operate");

        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
        MachineEnergyOutputBlockEntity output = helper.getBlockEntity(casing.get(1));
        helper.assertTrue(controller.validateStructureNow(),
            "An energy output port must be accepted at any Titanium Carbide Block position");
        helper.assertTrue(output.isBoundTo(helper.absolutePos(controllerPos)),
            "The energy output must bind to the formed Plasma Generator controller");
        installBalancedGroup(controller);
        controller.serverTick();

        helper.assertTrue(controller.isOperating() && controller.getStatus().equals("Generating"),
            "A formed generator with Argon and output capacity must report Generating");
        helper.assertTrue(!find(helper, CrystalnexusModBlocks.PLASMA_BLOCK.get()).isEmpty(),
            "An operating generator must place its configured physical plasma trail in the tube");
        helper.assertTrue(controller.getArgonTank().getFluidAmount() == 92,
            "One balanced group must consume eight mB per tick");
        helper.assertTrue(controller.getOutputPerTick() == 640_000
                && output.getEnergyStorage().getEnergyStored() == 640_000,
            "The generator must emit its reported FE/t through the multiblock energy output");
        List<BlockPos> heatingCores = find(helper, CrystalnexusModBlocks.HEATING_CORE.get());
        helper.assertTrue(!heatingCores.isEmpty() && heatingCores.stream()
                .allMatch(pos -> helper.getBlockState(pos).getValue(HeatingCoreBlock.LIT)),
            "Every heating core must use its lit texture and emit light while the Plasma Generator is working");

        helper.setBlock(casing.get(1), CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.assertTrue(controller.validateStructureNow(),
            "Removing the optional energy output must leave the fluid-fed structure formed");
        controller.getArgonTank().drain(10_000, IFluidHandler.FluidAction.EXECUTE);
        controller.serverTick();
        helper.assertTrue(find(helper, CrystalnexusModBlocks.PLASMA_BLOCK.get()).isEmpty(),
            "The generator must remove its physical plasma block when it stops");
        helper.assertTrue(heatingCores.stream()
                .noneMatch(pos -> helper.getBlockState(pos).getValue(HeatingCoreBlock.LIT)),
            "Heating cores must turn off when the Plasma Generator stops working");
        helper.succeed();
    }

    @GameTest(template = "plasma_gen")
    public static void runningStructureBreakCausesPlasmaArcRupture(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        BlockState controllerState = helper.getBlockState(controllerPos);
        helper.setBlock(controllerPos, Blocks.AIR);
        helper.setBlock(controllerPos, controllerState);
        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        MachineFluidInputBlockEntity input = helper.getBlockEntity(casing.get(0));
        helper.assertTrue(controller.validateStructureNow(), "Plasma Generator must form before a core is breached");
        input.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        installBalancedGroup(controller);
        controller.serverTick();
        helper.assertTrue(controller.isOperating(), "Plasma Generator must be running when a core is breached");

        List<BlockPos> heatingCores = find(helper, CrystalnexusModBlocks.HEATING_CORE.get());
        helper.assertTrue(!heatingCores.isEmpty(), "Plasma Generator template must contain a heating core to breach");
        int carbideBeforeBreach = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get()).size();
        helper.setBlock(heatingCores.getFirst(), Blocks.AIR);
        helper.assertTrue(!controller.validateStructureNow(), "A breached Plasma Generator must rupture");
        helper.assertTrue(controller.getArgonTank().isEmpty(), "The plasma arc rupture must vent all buffered Argon");
        helper.assertTrue(controller.getStatus().equals("Plasma Arc Failure"), "The controller must report its arc failure");
        assertReleasedPlasma(helper);
        helper.assertTrue(find(helper, CrystalnexusModBlocks.HEATING_CORE.get()).isEmpty(),
            "The plasma arc rupture must destroy every remaining heating core");
        helper.assertTrue(find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get()).size() == carbideBeforeBreach,
            "The plasma arc rupture must leave its carbide shell intact, unlike a stellar containment collapse");
        helper.succeed();
    }

    @GameTest(template = "plasma_gen")
    public static void breakingControllerReleasesPlasmaAndDestroysHeatingCores(GameTestHelper helper) {
        BlockPos controllerPos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.setBlock(casing.get(0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        helper.setBlock(casing.get(1), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get());
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        MachineFluidInputBlockEntity input = helper.getBlockEntity(casing.get(0));
        helper.assertTrue(controller.validateStructureNow(), "Plasma Generator must form before its controller breaks");
        input.getFluidInput().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        installBalancedGroup(controller);
        controller.serverTick();
        helper.assertTrue(controller.isOperating()
                && !find(helper, CrystalnexusModBlocks.PLASMA_BLOCK.get()).isEmpty(),
            "Plasma Generator must be operating with physical plasma before teardown");

        helper.setBlock(controllerPos, Blocks.AIR);

        assertReleasedPlasma(helper);
        helper.assertTrue(find(helper, CrystalnexusModBlocks.HEATING_CORE.get()).isEmpty(),
            "Breaking the controller must also destroy every heating core");
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
            new net.minecraft.world.phys.AABB(helper.absolutePos(controllerPos)).inflate(2)).stream()
            .filter(entity -> PlasmaGeneratorControllerBlockEntity.componentType(entity.getItem()) != PlasmaGrid.EMPTY)
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(dropped == 3, "Running controller removal must drop all three installed components once");
        helper.succeed();
    }

    private static void installBalancedGroup(PlasmaGeneratorControllerBlockEntity controller) {
        controller.setItem(0, new ItemStack(CrystalnexusModItems.FERROSTEEL_HEATSINK.get()));
        controller.setItem(1, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get()));
        controller.setItem(2, new ItemStack(CrystalnexusModItems.INDUCTION_COIL.get()));
    }

    private static PlasmaGeneratorControllerBlockEntity formedController(GameTestHelper helper) {
        BlockPos pos = find(helper, CrystalnexusModBlocks.PLASMA_GENERATOR_CONTROLLER.get()).getFirst();
        List<BlockPos> casing = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get());
        helper.setBlock(casing.getFirst(), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get());
        PlasmaGeneratorControllerBlockEntity controller = helper.getBlockEntity(pos);
        helper.assertTrue(controller.validateStructureNow(), "Generator must form");
        return controller;
    }

    @GameTest(template = "plasma_gen")
    public static void starvationFullBufferAndPartialOutput(GameTestHelper helper) {
        var controller = formedController(helper);
        installBalancedGroup(controller);
        controller.getArgonTank().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 7), IFluidHandler.FluidAction.EXECUTE);
        controller.serverTick();
        helper.assertTrue(!controller.isOperating() && controller.getArgonTank().getFluidAmount() == 7,
            "Insufficient fuel must pause the entire grid without draining it");
        controller.getArgonTank().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        var saved = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putInt("energy", 99_999_999);
        controller.loadWithComponents(saved, helper.getLevel().registryAccess());
        controller.serverTick();
        helper.assertTrue(controller.isOperating() && controller.getOutputPerTick() == 1
            && controller.getArgonTank().getFluidAmount() == 99, "Partial FE capacity must still consume full fuel");
        controller.serverTick();
        helper.assertTrue(!controller.isOperating() && controller.getStatus().equals("Energy Output Full")
            && controller.getArgonTank().getFluidAmount() == 99, "Full buffer must pause injection");
        helper.succeed();
    }

    @GameTest(template = "plasma_gen")
    public static void localHeatPersistsAndRupturesWithoutExtraction(GameTestHelper helper) {
        var controller = formedController(helper);
        controller.setItem(0, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get()));
        controller.getArgonTank().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 10_000), IFluidHandler.FluidAction.EXECUTE);
        controller.serverTick();
        helper.assertTrue(controller.isOperating() && controller.getOutputPerTick() == 0 && controller.getHeat(0) == 8,
            "Unconnected Injector must still burn Argon, generate heat, and activate plasma");
        controller.removeItem(0, 1);
        controller.serverTick();
        helper.assertTrue(controller.getHeat(0) == 7, "Removing components must not reset local heat");
        controller.setItem(0, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get()));
        controller.serverTick();
        var saved = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putDouble("heat0", PlasmaGrid.HEAT_LIMIT - 1);
        saved.putInt("energy", 123_456);
        controller.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(controller.getHeat(0) == PlasmaGrid.HEAT_LIMIT - 1 && !controller.getItem(0).isEmpty(),
            "Inventory and local heat must survive NBT reload");
        controller.serverTick();
        helper.assertTrue(controller.getStatus().equals("Plasma Arc Failure") && controller.getArgonTank().isEmpty()
            && controller.getHottestHeat() == 0 && !controller.getItem(0).isEmpty()
            && controller.multiblockEnergyOutput().getEnergyStored() == 123_456,
            "Overheating must rupture, vent fuel, reset heat, and retain components");
        helper.assertTrue(find(helper, CrystalnexusModBlocks.HEATING_CORE.get()).isEmpty(), "Rupture must destroy heating cores");
        assertReleasedPlasma(helper);
        helper.succeed();
    }

    @GameTest(template = "plasma_gen")
    public static void componentSlotsTransferAndDropExactlyOnce(GameTestHelper helper) {
        var controller = formedController(helper);
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        PlasmaGeneratorMenu menu = new PlasmaGeneratorMenu(1, player.getInventory(), controller);
        helper.assertTrue(menu.slots.size() == 81 && menu.slots.get(0).getMaxStackSize() == 1,
            "Menu must contain 45 component slots and 36 player slots");
        for (var side : net.minecraft.core.Direction.values())
            helper.assertTrue(controller.getSlotsForFace(side).length == 0
                && !controller.canPlaceItemThroughFace(0, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get()), side)
                && !controller.canTakeItemThroughFace(0, ItemStack.EMPTY, side), "Components must remain manual-only");
        ItemStack invalid = new ItemStack(Blocks.DIRT);
        helper.assertTrue(!menu.slots.get(0).mayPlace(invalid) && !controller.canPlaceItem(0, invalid),
            "Non-components must be rejected by both menu and inventory");
        controller.setItem(0, invalid);
        helper.assertTrue(controller.isEmpty(), "Inventory must reject direct invalid insertion");
        player.getInventory().setItem(9, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get(), 3));
        menu.clicked(45, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        helper.assertTrue(controller.getItem(0).getCount() == 1 && controller.getItem(1).getCount() == 1
            && controller.getItem(2).getCount() == 1, "Shift click must distribute stacks one per slot");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(controller.getItem(0).isEmpty(), "Shift click must return components to player inventory");
        controller.onControllerRemoved();
        controller.onControllerRemoved();
        int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
            new net.minecraft.world.phys.AABB(controller.getBlockPos()).inflate(2)).stream()
            .filter(entity -> entity.getItem().is(CrystalnexusModItems.PLASMA_INJECTOR.get()))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(count == 2 && controller.isEmpty(), "Repeated teardown must drop each installed component once");
        helper.succeed();
    }

    @GameTest(template = "plasma_gen")
    public static void legacySavesAndClientSnapshots(GameTestHelper helper) {
        var controller = formedController(helper);
        installBalancedGroup(controller);
        controller.getArgonTank().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        var legacy = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
        legacy.remove("Items");
        legacy.putInt("energy", 123_456);
        for (int slot = 0; slot < PlasmaGrid.SIZE; slot++) legacy.remove("heat" + slot);
        controller.loadWithComponents(legacy, helper.getLevel().registryAccess());
        controller.serverTick();
        helper.assertTrue(controller.isEmpty() && controller.getHottestHeat() == 0 && !controller.isOperating()
            && controller.getArgonTank().getFluidAmount() == 100
            && controller.multiblockEnergyOutput().getEnergyStored() == 123_456,
            "Legacy saves must keep Argon and FE, load an empty cool grid, and require components");
        controller.setItem(0, new ItemStack(CrystalnexusModItems.PLASMA_INJECTOR.get()));
        controller.serverTick();
        var snapshot = controller.getUpdateTag(helper.getLevel().registryAccess());
        var clientCopy = new PlasmaGeneratorControllerBlockEntity(controller.getBlockPos(), controller.getBlockState());
        clientCopy.loadWithComponents(snapshot, helper.getLevel().registryAccess());
        helper.assertTrue(clientCopy.getHeat(0) == controller.getHeat(0)
            && clientCopy.getGrid().argonPerTick() == controller.getGrid().argonPerTick()
            && clientCopy.getStatus().equals(controller.getStatus()) && clientCopy.isOperating(),
            "Client snapshot must include inventory, heat, grid statistics, and operating state");
        helper.succeed();
    }

    private static void assertReleasedPlasma(GameTestHelper helper) {
        List<BlockPos> plasma = find(helper, CrystalnexusModBlocks.PLASMA_BLOCK.get());
        helper.assertTrue(!plasma.isEmpty() && plasma.stream()
            .allMatch(pos -> !helper.getBlockState(pos).getValue(PlasmaBlock.CONTAINED)),
            "Failure must leave loose plasma that can escape through the breach");
    }

    @GameTest(template = "plasma_gen")
    public static void bottomBreachReleasesPlasmaAndNormalShutdownDoesNotDeleteIt(GameTestHelper helper) {
        var controller = formedController(helper);
        installBalancedGroup(controller);
        controller.getArgonTank().fill(new FluidStack(CrystalnexusModFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE);
        controller.serverTick();
        BlockPos floor = find(helper, CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get()).stream()
            .min(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
        helper.assertTrue(floor.getY() < helper.relativePos(controller.getBlockPos()).getY(),
            "The breach must be in the bottom casing layer");
        helper.setBlock(floor, Blocks.AIR);
        helper.assertTrue(!controller.validateStructureNow(), "A missing floor must rupture the generator");
        assertReleasedPlasma(helper);
        controller.serverTick();
        assertReleasedPlasma(helper);
        helper.succeed();
    }

    @GameTest(template = "plasma_gen", timeoutTicks = 100)
    public static void plasmaRisesSettlesAndStaysSettled(GameTestHelper helper) {
        BlockPos origin = new BlockPos(5, 6, 5);
        for (int y = 5; y <= 10; y++) for (int x = 4; x <= 6; x++) for (int z = 4; z <= 6; z++)
            helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        for (int y = 6; y <= 9; y++) helper.setBlock(new BlockPos(5, y, 5), Blocks.AIR);
        helper.setBlock(origin, CrystalnexusModBlocks.PLASMA_BLOCK.get());
        helper.runAfterDelay(24, () -> {
            BlockPos top = new BlockPos(5, 9, 5);
            helper.assertTrue(helper.getBlockState(origin).isAir() && helper.getBlockState(top)
                .is(CrystalnexusModBlocks.PLASMA_BLOCK.get()) && helper.getBlockState(top).getValue(PlasmaBlock.SETTLED),
                "Loose plasma must rise to the ceiling and settle");
            helper.setBlock(top.above(), Blocks.AIR);
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(helper.getBlockState(top).is(CrystalnexusModBlocks.PLASMA_BLOCK.get())
                    && helper.getBlockState(top.above()).isAir(), "Settled plasma must stop pathfinding");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "plasma_gen", timeoutTicks = 100)
    public static void plasmaCanDescendThroughABreachToReachAHigherPocket(GameTestHelper helper) {
        for (int y = 5; y <= 10; y++) for (int x = 4; x <= 8; x++) for (int z = 4; z <= 6; z++)
            helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        BlockPos origin = new BlockPos(5, 8, 5), destination = new BlockPos(7, 9, 5);
        for (BlockPos air : List.of(origin.below(), origin.below().east(), origin.below().east(2),
                new BlockPos(7, 8, 5), destination)) helper.setBlock(air, Blocks.AIR);
        helper.setBlock(origin, CrystalnexusModBlocks.PLASMA_BLOCK.get());
        helper.runAfterDelay(6, () -> {
            BlockPos lower = origin.below();
            helper.assertTrue(helper.getBlockState(lower).is(CrystalnexusModBlocks.PLASMA_BLOCK.get()),
                "Plasma must visibly descend one block through the breach before climbing");
            PlasmaBlockEntity moving = helper.getBlockEntity(lower);
            var saved = moving.saveWithFullMetadata(helper.getLevel().registryAccess());
            moving.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(!moving.getRoute().isEmpty(), "A downward detour route must survive NBT reload");
        });
        helper.runAfterDelay(32, () -> {
            helper.assertTrue(helper.getBlockState(origin).isAir() && helper.getBlockState(destination)
                .is(CrystalnexusModBlocks.PLASMA_BLOCK.get()) && helper.getBlockState(destination).getValue(PlasmaBlock.SETTLED),
                "Plasma must navigate down, sideways, and back up into the highest reachable pocket");
            helper.assertTrue(helper.getBlockState(origin.above()).is(Blocks.STONE), "Plasma must not destroy obstacles");
            helper.succeed();
        });
    }

    @GameTest(template = "plasma_gen", timeoutTicks = 40)
    public static void loosePlasmaDisappearsAtBuildHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos top = helper.absolutePos(new BlockPos(5, 0, 5)).atY(level.getMaxBuildHeight() - 1);
        BlockPos placedAtTop = top.east(2);
        var plasma = CrystalnexusModBlocks.PLASMA_BLOCK.get().defaultBlockState();
        level.setBlockAndUpdate(top, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(top.below(), plasma);
        level.setBlockAndUpdate(placedAtTop, plasma);
        helper.runAfterDelay(8, () -> {
            for (BlockPos pos : List.of(top.below(), top, placedAtTop)) {
                helper.assertTrue(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null,
                    "Loose plasma reaching or placed at build height must disappear with its block entity");
            }
            helper.succeed();
        });
    }

    private static List<BlockPos> find(GameTestHelper helper, Block block) {
        List<BlockPos> found = new ArrayList<>();
        helper.forEveryBlockInStructure(pos -> {
            if (helper.getBlockState(pos).is(block)) found.add(pos.immutable());
        });
        return found;
    }
}
