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
