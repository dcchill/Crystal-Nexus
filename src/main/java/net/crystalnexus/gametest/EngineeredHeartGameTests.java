package net.crystalnexus.gametest;

import net.crystalnexus.block.EngineeredHeartBlock;
import net.crystalnexus.block.entity.EngineeredHeartBlockEntity;
import net.crystalnexus.block.entity.HeartBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyOutputBlockEntity;
import net.crystalnexus.block.entity.MachineFluidInputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.multiblock.EngineeredHeartStructure;
import net.crystalnexus.multiblock.EngineeredHeartStructure.Part;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class EngineeredHeartGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 4, 4);

    @GameTest(template = "zero_point")
    public static void heartStructureRotatesRejectsDamageAndUnloadedChunks(GameTestHelper helper) {
        helper.assertTrue(EngineeredHeartStructure.CELLS.stream().filter(c -> c.part() == Part.FRAME).count() == 33
            && EngineeredHeartStructure.CELLS.stream().filter(c -> c.part() == Part.FLESH).count() == 8,
            "The guide must contain 33 frames and 8 flesh corners");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            EngineeredHeartBlockEntity controller = build(helper, facing);
            controller.serverTick();
            helper.assertTrue(controller.isFormed(), "The heart must form facing " + facing);
            for (var cell : EngineeredHeartStructure.CELLS) {
                if (cell.part() == Part.CONTROLLER) continue;
                BlockPos pos = CENTER.offset(EngineeredHeartStructure.rotate(cell.offset(), facing));
                var original = helper.getBlockState(pos);
                helper.setBlock(pos, cell.part() == Part.AIR ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                controller.serverTick();
                helper.assertTrue(!controller.isFormed() && controller.multiblockFluidInput() == null
                    && controller.multiblockEnergyOutput() == null, "Wrong or missing cell must stop the heart: " + cell);
                helper.setBlock(pos, original);
                controller.serverTick();
                helper.assertTrue(controller.isFormed(), "Replacing the cell must reform the heart");
            }
        }
        BlockPos unloaded = new BlockPos(20_000_000, 80, 20_000_000);
        helper.assertTrue(!helper.getLevel().hasChunkAt(unloaded), "Test location must be unloaded");
        helper.assertTrue(!EngineeredHeartStructure.matches(helper.getLevel(), unloaded, Direction.NORTH)
            && !helper.getLevel().hasChunkAt(unloaded), "Validation must reject unloaded chunks without loading them");
        com.mojang.logging.LogUtils.getLogger().info("Engineered Heart GameTest passed: rotations, damage, unloaded chunks");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void heartPulsesAtomicallyThroughPortsAndSurvivesReload(GameTestHelper helper) {
        EngineeredHeartBlockEntity controller = build(helper, Direction.NORTH);
        MachineFluidInputBlockEntity input = helper.getBlockEntity(CENTER.offset(-2, -2, 0));
        MachineEnergyOutputBlockEntity output = helper.getBlockEntity(CENTER.offset(2, -2, 0));
        HeartBlockEntity heart = helper.getBlockEntity(CENTER);
        controller.serverTick();
        helper.assertTrue(input.getFluidInput().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
            "The input must reject non-blood fluids");
        helper.assertTrue(input.getFluidInput().fill(blood(4000), FluidAction.EXECUTE) == 4000, "The input must fill the controller tank");
        helper.assertTrue(input.getFluidInput().drain(1000, FluidAction.EXECUTE).isEmpty()
            && !output.getEnergyStorage().canReceive(), "Ports must only insert fluid and extract energy");
        var stalePort = new MachineFluidInputBlockEntity(helper.absolutePos(CENTER.offset(0, 0, 4)), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState());
        stalePort.setLevel(helper.getLevel());
        stalePort.bindController(controller.getBlockPos());
        helper.assertTrue(stalePort.getFluidInput().getTanks() == 0, "A stale port outside the current layout must not access the controller");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, controller.getBlockPos(), side) == null
                && helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, controller.getBlockPos(), side) == null,
                "The controller must not expose direct fluid or energy capabilities");
        }
        controller.unbindStructure();
        tick(controller, 19);
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 0 && controller.getBloodTank().getFluidAmount() == 4000,
            "No blood or FE may move before tick twenty");
        tick(controller, 1);
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 100000 && controller.getBloodTank().getFluidAmount() == 3500
            && heart.beatAge(0) == 0, "A successful beat must atomically use 500 mB, create 100000 FE and animate");
        tick(controller, 40);
        helper.assertTrue(controller.getBloodTank().getFluidAmount() == 3500 && controller.getStatus().equals("Energy Full"),
            "Full storage must stop consumption");
        output.getEnergyStorage().extractEnergy(99999, false);
        tick(controller, 20);
        helper.assertTrue(controller.getBloodTank().getFluidAmount() == 3500, "Even one stored FE must prevent a partial pulse");
        output.getEnergyStorage().extractEnergy(1, false);
        tick(controller, 20);
        helper.assertTrue(controller.getBloodTank().getFluidAmount() == 3000, "The next scheduled beat must resume after storage drains");

        var saved = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new EngineeredHeartBlockEntity(controller.getBlockPos(), controller.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(!restored.isFormed() && restored.getBloodTank().getFluidAmount() == 3000
            && restored.getEnergyStorage().getEnergyStored() == 100000, "Reload must preserve stores and require revalidation");
        helper.setBlock(CENTER.offset(0, -2, -2), Blocks.AIR);
        helper.getLevel().setBlock(controller.getBlockPos(), controller.getBlockState(), 3);
        helper.getLevel().setBlockEntity(restored);
        restored.getEnergyStorage().extractEnergy(100000, false);
        tick(restored, 19);
        helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == 0, "Reload must not replay an offline beat");
        tick(restored, 1);
        helper.assertTrue(restored.getBloodTank().getFluidAmount() == 2500 && restored.getEnergyStorage().getEnergyStored() == 100000,
            "Reload must start a fresh twenty-tick period");

        helper.setBlock(CENTER.offset(0, -2, -2), Blocks.AIR);
        helper.assertTrue(!heart.isFormed() && !input.isBoundTo(controller.getBlockPos()) && !output.isBoundTo(controller.getBlockPos()),
            "Removing the controller must immediately unbind the heart and both ports");
        com.mojang.logging.LogUtils.getLogger().info("Engineered Heart GameTest passed: pulses, ports, reload");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void heartInsufficientBloodAndBrokenFramePreserveContents(GameTestHelper helper) {
        EngineeredHeartBlockEntity controller = build(helper, Direction.NORTH);
        controller.serverTick();
        MachineFluidInputBlockEntity input = helper.getBlockEntity(CENTER.offset(-2, -2, 0));
        MachineEnergyOutputBlockEntity output = helper.getBlockEntity(CENTER.offset(2, -2, 0));
        input.getFluidInput().fill(blood(499), FluidAction.EXECUTE);
        tick(controller, 40);
        helper.assertTrue(controller.getBloodTank().getFluidAmount() == 499 && controller.getEnergyStorage().getEnergyStored() == 0
            && controller.getStatus().equals("Insufficient Blood"), "Insufficient blood must not be consumed");
        input.getFluidInput().fill(blood(1), FluidAction.EXECUTE);
        tick(controller, 19);
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 100000, "The periodic heartbeat must resume without banking missed pulses");
        BlockPos frame = CENTER.offset(2, 0, 2);
        helper.setBlock(frame, Blocks.STONE);
        helper.assertTrue(input.getFluidInput().fill(blood(500), FluidAction.EXECUTE) == 0
            && output.getEnergyStorage().extractEnergy(100000, false) == 0,
            "Port calls between controller ticks must reject a broken structure");
        tick(controller, 20);
        helper.assertTrue(controller.getEnergyStorage().getEnergyStored() == 100000 && !controller.isFormed(),
            "Dismantling must retain the energy buffer");
        helper.setBlock(frame, CrystalnexusModBlocks.FLESH_MACHINE_FRAME.get());
        tick(controller, 1);
        helper.assertTrue(output.getEnergyStorage().extractEnergy(100000, false) == 100000, "Rebuilding must expose retained energy");
        com.mojang.logging.LogUtils.getLogger().info("Engineered Heart GameTest passed: starvation, dismantling, retained contents");
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 80)
    public static void heartAutomaticallyTicksAndPushesEnergyToAdjacentBattery(GameTestHelper helper) {
        EngineeredHeartBlockEntity controller = build(helper, Direction.NORTH);
        BlockPos batteryPos = CENTER.offset(3, -2, 0);
        helper.setBlock(batteryPos, CrystalnexusModBlocks.BATTERY.get());
        controller.getBloodTank().fill(blood(500), FluidAction.EXECUTE);
        helper.succeedWhen(() -> {
            var battery = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(batteryPos), Direction.WEST);
            helper.assertTrue(battery != null && battery.getEnergyStored() == 100000,
                "The real server ticker must form the structure and push a heartbeat into the adjacent battery");
            HeartBlockEntity heart = helper.getBlockEntity(CENTER);
            HeartBlockEntity clientCopy = new HeartBlockEntity(heart.getBlockPos(), heart.getBlockState());
            clientCopy.setLevel(helper.getLevel());
            clientCopy.loadWithComponents(heart.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(clientCopy.isFormed() && clientCopy.beatAge(0) == heart.beatAge(0)
                && controller.getBloodTank().isEmpty(), "Client update must carry formation and the successful beat timestamp");
            com.mojang.logging.LogUtils.getLogger().info("Engineered Heart GameTest passed: real ticks, battery output, animation synchronization");
        });
    }

    private static FluidStack blood(int amount) { return new FluidStack(CrystalnexusModFluids.BLOOD.get(), amount); }
    private static void tick(EngineeredHeartBlockEntity controller, int count) {
        for (int i = 0; i < count; i++) controller.serverTick();
    }
    private static EngineeredHeartBlockEntity build(GameTestHelper helper, Direction facing) {
        // Clear previous rotations, including their block entities, before placing a fresh controller.
        for (var cell : EngineeredHeartStructure.CELLS) helper.setBlock(CENTER.offset(cell.offset()), Blocks.AIR);
        for (var cell : EngineeredHeartStructure.CELLS) {
            var state = cell.state();
            if (cell.part() == Part.CONTROLLER) state = state.setValue(EngineeredHeartBlock.FACING, facing);
            helper.setBlock(CENTER.offset(EngineeredHeartStructure.rotate(cell.offset(), facing)), state);
        }
        return helper.getBlockEntity(CENTER.offset(EngineeredHeartStructure.rotate(new BlockPos(0, -2, -2), facing)));
    }
}
