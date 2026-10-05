package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.procedures.CrystalCrusherOnTickUpdateProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder("crystalnexus_upgrades")
@PrefixGameTestTemplate(false)
public final class NexusDebugGameTests {
    @GameTest(template = "zero_point")
    public static void debugCommandCapturesStagesAndStops(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var source = server.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        try {
            dispatcher.execute("nexusdebug start 5", source.withPermission(0));
            helper.fail("Unprivileged players must not start profiling");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
            helper.assertTrue(!net.crystalnexus.commands.NexusDebugCommand.isCapturing(server), "Denied commands must not enable profiling");
        }
        var directory = server.getServerDirectory().resolve("debug");
        Set<Path> before;
        if (Files.exists(directory)) {
            try (var files = Files.list(directory)) { before = files.collect(Collectors.toSet()); }
        } else before = Set.of();
        helper.assertTrue(dispatcher.execute("nexusdebug start 5", source) == 1, "Capture must start");
        helper.assertTrue(dispatcher.execute("nexusdebug start 5", source) == 0, "Duplicate capture must be rejected");
        helper.assertTrue(dispatcher.execute("nexusdebug stop", source) == 0,
                "Stopping before the profiler's first tick must not leave a delayed profiler running");
        helper.setBlock(new BlockPos(1, 1, 1), CrystalnexusModBlocks.CRYSTAL_CRUSHER.get().defaultBlockState()
                .setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2));
        CrystalCrusherBlockEntity machine = helper.getBlockEntity(new BlockPos(1, 1, 1));
        machine.setItem(0, new ItemStack(Items.IRON_ORE, 4));
        helper.runAfterDelay(2, () -> {
            BlockPos pos = machine.getBlockPos();
            helper.getLevel().setBlock(pos, machine.getBlockState()
                    .setValue(net.crystalnexus.block.CrystalCrusherBlock.BLOCKSTATE, 2), 2);
            machine.getEnergyStorage().receiveEnergy(2048, false);
            machine.getPersistentData().putDouble("progress", 100);
            CrystalCrusherOnTickUpdateProcedure.execute(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ());
            helper.assertTrue(!machine.getItem(1).isEmpty(), "Profile must include a completed craft");
            machine.machineSync.flushPending();
        });
        helper.runAfterDelay(4, () -> {
            try {
                helper.assertTrue(dispatcher.execute("nexusdebug stop", source) == 1 && !net.crystalnexus.commands.NexusDebugCommand.isCapturing(server),
                        "Stop must end the profiler and save the report");
                helper.assertTrue(dispatcher.execute("nexusdebug stop", source) == 0, "Stopped captures must release their state");
                try (var files = Files.list(directory)) {
                    var report = files.filter(path -> !before.contains(path) && path.getFileName().toString().startsWith("crystalnexus-profile-"))
                            .findFirst().orElseThrow();
                    String text = Files.readString(report);
                    for (String expected : new String[]{"TPS", "MSPT", "crystalnexus:crystal_crusher", "Top entities",
                            "crystalnexus_recipe_lookup", "crystalnexus_crusher_processing", "crystalnexus_energy_receive",
                            "crystalnexus_energy_extract", "crystalnexus_persistence", "crystalnexus_inventory", "crystalnexus_sync"})
                        helper.assertTrue(text.contains(expected), "Report is missing diagnostic section: " + expected);
                }
                helper.succeed();
            } catch (Exception exception) {
                helper.fail("Debug report failed: " + exception.getMessage());
            }
        });
    }
}
