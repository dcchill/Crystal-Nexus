package net.crystalnexus.util;

import net.crystalnexus.block.entity.PistonGeneratorBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class MachineSyncGameTests {
    @GameTest(template = "zero_point", templateNamespace = "crystalnexus_upgrades", timeoutTicks = 80)
    public static void collectorReusesScanAndKeepsBatchAccounting(GameTestHelper helper) {
        BlockPos relative = new BlockPos(6, 6, 6);
        helper.setBlock(relative, CrystalnexusModBlocks.OXYGEN_COLLECTOR.get());
        net.crystalnexus.block.entity.OxygenCollectorBlockEntity collector = helper.getBlockEntity(relative);
        var level = helper.getLevel();
        BlockPos pos = collector.getBlockPos();
        var empty = collector.nearbyLeaves();
        helper.assertTrue(empty.isEmpty(), "Collector starts without leaves");
        BlockPos leaf = pos.above();
        level.setBlockAndUpdate(leaf, net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        helper.assertTrue(collector.nearbyLeaves() == empty, "Repeated reads must reuse even an empty scan");
        helper.runAfterDelay(21, () -> {
            var leaves = collector.nearbyLeaves();
            helper.assertTrue(leaves.size() == 1 && leaves.getFirst().equals(leaf), "Scan must discover new leaves within 20 ticks");
            collector.getEnergyStorage().receiveEnergy(512, false);
            for (int tick = 0; tick < 39; tick++)
                net.crystalnexus.procedures.OxygenCollectorOnTickUpdateProcedure.execute(level, pos);
            helper.assertTrue(collector.getFluidTank().isEmpty(), "One leaf must still take 40 ticks per batch");
            net.crystalnexus.procedures.OxygenCollectorOnTickUpdateProcedure.execute(level, pos);
            helper.assertTrue(collector.getFluidTank().getFluidAmount() == 10
                    && collector.getEnergyStorage().getEnergyStored() == 256
                    && collector.nearbyLeaves() == leaves, "Batch must use 256 FE, make 10 mB, and reuse the scan");

            collector.getFluidTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                    net.crystalnexus.init.CrystalnexusModFluids.ATMOSPHERE.get(), 8000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            collector.machineSync.setDouble("progress", 12);
            long due = level.getGameTime() + 1;
            while (!MachineSync.isUpdateTick(due, pos)) due++;
            collector.machineSync.takeUpdate(due, pos);
            net.crystalnexus.procedures.OxygenCollectorOnTickUpdateProcedure.execute(level, pos);
            helper.assertTrue(collector.getPersistentData().getDouble("progress") == 0
                    && collector.getEnergyStorage().getEnergyStored() == 256
                    && collector.machineSync.takeUpdate(due + 5, pos), "Full tank must reset and sync progress without spending FE");
            var restored = new net.crystalnexus.block.entity.OxygenCollectorBlockEntity(pos, collector.getBlockState());
            restored.loadWithComponents(collector.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            helper.assertTrue(restored.getFluidTank().getFluidAmount() == 8000
                    && restored.getEnergyStorage().getEnergyStored() == 256, "Batched updates must not affect persistence");
            level.removeBlock(leaf, false);
        });
        helper.runAfterDelay(42, () -> {
            helper.assertTrue(collector.nearbyLeaves().isEmpty(), "Removed leaves must expire from the scan");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point", templateNamespace = "crystalnexus_upgrades")
    public static void assemblyStopsProbingAfterEnergyAcceptance(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        BlockPos pos = helper.absolutePos(relative);
        var probes = new java.util.concurrent.atomic.AtomicInteger();
        var machine = new net.crystalnexus.block.entity.CrystalCrusherBlockEntity(pos, helper.getLevel().getBlockState(pos)) {
            @Override public net.neoforged.neoforge.energy.EnergyStorage getEnergyStorage() {
                probes.incrementAndGet();
                return super.getEnergyStorage();
            }
        };
        helper.getLevel().setBlockEntity(machine);
        var profile = new net.crystalnexus.assembly.AssemblyLineMachine.SideProfile(1, 2, 4, 8, 16, 32);
        var worker = net.crystalnexus.assembly.AssemblyLineMachine.at(helper.getLevel(), pos, profile);
        var energy = machine.getEnergyStorage();
        probes.set(0);
        helper.assertTrue(worker.receiveEnergy(512, true) == 512 && energy.getEnergyStored() == 0
                && probes.get() == 1, "Simulation must stop at the first accepting capability without spending FE");
        probes.set(0);
        helper.assertTrue(worker.receiveEnergy(512, false) == 512 && energy.getEnergyStored() == 512
                && probes.get() == 1, "Transfer must stop probing after the unsided capability accepts FE");
        while (energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        helper.assertTrue(worker.receiveEnergy(512, false) == 0, "Full worker must still reject energy");
        helper.assertTrue(profile.receiveOrder()[0] == net.minecraft.core.Direction.WEST
                && profile.extractOrder()[0] == net.minecraft.core.Direction.UP
                && java.util.Arrays.stream(profile.receiveOrder()).distinct().count() == 6,
                "Cached side ordering must retain preferred sides and every fallback");
        helper.succeed();
    }

    @GameTest(template = "zero_point", templateNamespace = "crystalnexus_upgrades")
    public static void energyAndProgressSaveWithoutInventoryNotifications(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.CRYSTAL_CRUSHER.get().defaultBlockState()
                .setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2));
        BlockPos pos = helper.absolutePos(relative);
        var inventoryNotifications = new java.util.concurrent.atomic.AtomicInteger();
        var machine = new net.crystalnexus.block.entity.CrystalCrusherBlockEntity(pos, helper.getLevel().getBlockState(pos)) {
            @Override public void setChanged() {
                inventoryNotifications.incrementAndGet();
                super.setChanged();
            }
        };
        helper.getLevel().setBlockEntity(machine);
        inventoryNotifications.set(0);
        var chunk = helper.getLevel().getChunkAt(pos);
        var energy = machine.getEnergyStorage();
        chunk.setUnsaved(false);
        energy.receiveEnergy(512, true);
        helper.assertTrue(!chunk.isUnsaved() && energy.getEnergyStored() == 0,
                "Simulated transfers must not dirty the chunk");
        energy.receiveEnergy(512, false);
        machine.machineSync.setDouble("progress", 40);
        helper.assertTrue(chunk.isUnsaved() && inventoryNotifications.get() == 0,
                "Energy and progress must persist without comparator/neighbor notifications");
        var restored = new net.crystalnexus.block.entity.CrystalCrusherBlockEntity(pos, machine.getBlockState());
        restored.loadAdditional(machine.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == 512
                && restored.getPersistentData().getDouble("progress") == 40, "Energy and progress must survive saving");

        machine.machineSync.flushPending();
        machine.machineSync.setDouble("progress", 41);
        machine.machineSync.flushPending();
        long next = helper.getLevel().getGameTime() + 1;
        while (!MachineSync.isUpdateTick(next, pos)) next++;
        helper.assertTrue(machine.machineSync.takeUpdate(next, pos),
                "A change after this tick's snapshot must stay queued for the next snapshot");

        machine.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_ORE, 4));
        machine.setItem(2, new net.minecraft.world.item.ItemStack(net.crystalnexus.init.CrystalnexusModItems.PARALLELIZATION_CHIP.get(), 2));
        int perCraft = Math.min(4096, net.crystalnexus.config.CrystalnexusConfig.MACHINES.CRYSTAL_CRUSHER.maxExtract());
        int budget = perCraft * 4;
        while (energy.getEnergyStored() < budget && energy.receiveEnergy(budget - energy.getEnergyStored(), false) > 0) {}
        machine.machineSync.setDouble("progress", 100);
        inventoryNotifications.set(0);
        net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(machine.getItem(0).isEmpty() && !machine.getItem(1).isEmpty()
                && energy.getEnergyStored() == 0, "Four parallel crafts must consume their inputs and energy");
        helper.assertTrue(inventoryNotifications.get() == 2,
                "A parallel batch must notify once for the input and once for the output, rather than once per craft");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void displayReadsDoNotProcessRecipes(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.CIRCUIT_PRESS.get());
        net.crystalnexus.block.entity.CircuitPressBlockEntity press = helper.getBlockEntity(relative);
        press.getEnergyStorage().receiveEnergy(2048, false);
        press.getPersistentData().putDouble("progress", 99);
        var before = press.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockPos pos = press.getBlockPos();
        for (int i = 0; i < 100; i++)
            helper.assertTrue(MachineEnergyDisplay.text(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ()).equals("FE: 2048"),
                "Display must read the current energy value");
        helper.assertTrue(before.equals(press.saveWithFullMetadata(helper.getLevel().registryAccess())),
            "Rendering energy text must leave inventory, progress and energy untouched");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void transfersCoalesceAndFinalChangeSurvives(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, CrystalnexusModBlocks.PISTON_GENERATOR.get());
        PistonGeneratorBlockEntity machine = helper.getBlockEntity(relative);
        var sync = machine.machineSync;
        var energy = machine.getEnergyStorage();
        BlockPos pos = machine.getBlockPos();
        long due = helper.getLevel().getGameTime();
        while (!MachineSync.isUpdateTick(due, pos)) due++;
        energy.generateEnergy(1000, true);
        energy.extractEnergy(1000, true);
        helper.assertTrue(energy.getEnergyStored() == 0 && !sync.takeUpdate(due, pos),
            "Simulation must neither mutate energy nor queue a snapshot");
        for (int i = 0; i < 10; i++) {
            energy.generateEnergy(100, false);
            energy.extractEnergy(10, false);
        }
        helper.assertTrue(!sync.takeUpdate(due - 1, pos) && sync.takeUpdate(due, pos)
                && !sync.takeUpdate(due, pos), "Many transfers must coalesce into one scheduled snapshot");
        energy.extractEnergy(1, false);
        helper.assertTrue(!sync.takeUpdate(due, pos), "A second tick invocation must not duplicate a snapshot");
        for (int offset = 1; offset < 5; offset++)
            helper.assertTrue(!sync.takeUpdate(due + offset, pos), "Off-cycle change must remain pending");
        helper.assertTrue(sync.takeUpdate(due + 5, pos) && !sync.takeUpdate(due + 10, pos),
            "Final change must flush without another transfer, then stop sending");
        sync.setDouble("maxProgress", 350);
        helper.assertTrue(sync.takeUpdate(due + 15, pos), "Changed progress limit must be synchronized");
        sync.setDouble("maxProgress", 350);
        helper.assertTrue(!sync.takeUpdate(due + 20, pos), "Unchanged progress limit must not queue work");

        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = new PistonGeneratorBlockEntity(pos, machine.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == 899
                && restored.getPersistentData().getDouble("maxProgress") == 350,
            "Persisted values must not depend on client synchronization");
        helper.succeed();
    }
}
