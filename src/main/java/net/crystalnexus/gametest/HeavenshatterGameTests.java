package net.crystalnexus.gametest;

import net.crystalnexus.block.entity.HeavenshatterBeamControllerBlockEntity;
import net.crystalnexus.block.entity.MachineEnergyInputBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.multiblock.HeavenshatterMaterials;
import net.crystalnexus.multiblock.HeavenshatterMeteor;
import net.crystalnexus.recipe.GravitationalArrayCostSchedule;
import net.crystalnexus.world.inventory.HeavenshatterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_heavenshatter")
@PrefixGameTestTemplate(false)
public final class HeavenshatterGameTests {
    // GameTest places the template one block above its structure block.
    private static final BlockPos CONTROLLER = new BlockPos(11, 2, 22);
    private record Machine(HeavenshatterBeamControllerBlockEntity controller, MachineEnergyInputBlockEntity port, BlockPos casing) {}

    private static Machine setup(GameTestHelper helper) {
        var state = helper.getBlockState(CONTROLLER);
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.setBlock(CONTROLLER, state);
        HeavenshatterBeamControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(!controller.validateStructureNow(), "At least one Energy Input is required");
        BlockPos casing = findCarbon(helper);
        helper.assertTrue(casing != null, "Template contains carbon casing");
        helper.setBlock(casing, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get());
        MachineEnergyInputBlockEntity port = helper.getBlockEntity(casing);
        helper.assertTrue(controller.validateStructureNow(), "NBT structure forms with a substituted energy input");
        helper.assertTrue(port.isBoundTo(controller.getBlockPos()), "Energy Input binds to the controller");
        helper.assertTrue(controller.emitters().size() == 4, "One upper gravity point per side supplies four beams");
        return new Machine(controller, port, casing);
    }

    private static BlockPos findCarbon(GameTestHelper helper) {
        BlockPos casing = null;
        for (int x = 0; x < 23 && casing == null; x++) for (int y = 1; y <= 15 && casing == null; y++) for (int z = 0; z < 23 && casing == null; z++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (helper.getBlockState(pos).is(CrystalnexusModBlocks.CARBON_BLOCK.get())) casing = pos;
        }
        return casing;
    }

    @GameTest(template = "heavenshatter_beam")
    public static void recordedDiskFiltersExactMeteorAndSurvivesReload(GameTestHelper helper) {
        var machine = setup(helper);
        var controller = machine.controller();
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var meteor = net.crystalnexus.multiblock.RecordedMeteor.create(BlockPos.ZERO, 7, 8765,
            java.util.List.of(Blocks.STONE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState()));
        var disk = new net.minecraft.world.item.ItemStack(net.crystalnexus.init.CrystalnexusModItems.METEOR_DATA_DISK.get());
        net.crystalnexus.item.MeteorDataDiskItem.write(disk, meteor);
        var filter = controller.meteorFilter();
        helper.assertTrue(!filter.isItemValid(0, new net.minecraft.world.item.ItemStack(net.crystalnexus.init.CrystalnexusModItems.METEOR_DATA_DISK.get()))
            && !filter.isItemValid(0, new net.minecraft.world.item.ItemStack(Blocks.DIRT)), "Filter rejects blank disks and ordinary items");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "meteor-filter"));
        player.setPos(controller.getBlockPos().getCenter());
        var menu = new HeavenshatterMenu(1, player.getInventory(), controller);
        player.containerMenu = menu;
        player.getInventory().setItem(9, disk.copy());
        helper.assertTrue(!menu.quickMoveStack(player, 1).isEmpty() && !filter.getStackInSlot(0).isEmpty(), "Shift-click inserts a recorded disk");
        controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING
            && controller.status() == HeavenshatterBeamControllerBlockEntity.Status.NO_POWER, "Filter still needs acquisition power");
        supply(machine, 1000);
        player.containerMenu = player.inventoryMenu;
        controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCKED && controller.trackingIdentified(), "Disk bypasses tuning even with the menu closed");
        helper.assertTrue(controller.diameter() == meteor.diameter() && controller.palette().equals(meteor.palette())
            && controller.cells().equals(HeavenshatterMeteor.cells(meteor.diameter(), meteor.seed(), meteor.palette().size())), "Filter reproduces exact recorded geometry and composition");
        helper.assertTrue(controller.duration() == HeavenshatterMeteor.DURATION && controller.multiblockEnergyInput().getEnergyStored() == 0, "Filter costs 1000 FE once and uses the base retrieval duration");
        helper.assertTrue(net.minecraft.world.item.ItemStack.matches(filter.getStackInSlot(0), disk), "Filtering does not consume or alter the disk");
        var saved = controller.saveWithoutMetadata(registries);
        controller.loadAdditional(saved, registries);
        helper.assertTrue(net.minecraft.world.item.ItemStack.matches(filter.getStackInSlot(0), disk)
            && controller.lockedComet().seed() == meteor.seed(), "Filter and exact lock survive reload");
        filter.extractItem(0, 1, false);
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING && controller.lockedComet() == null, "Removing filter before pulling restores the normal minigame");
        filter.insertItem(0, disk.copy(), false);
        supply(machine, 1000);
        controller.serverTick();
        var target = controller.lockedComet();
        player.containerMenu = menu;
        int firstCost = Math.toIntExact(GravitationalArrayCostSchedule.next(controller.cost(), 0, controller.duration()));
        supply(machine, firstCost);
        controller.controls(player, controller.frequency(), controller.bearing(), true);
        controller.serverTick();
        helper.assertTrue(controller.progress() == 1, "Filtered meteor still uses normal explicit retrieval and power");
        filter.extractItem(0, 1, false);
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.RETRIEVING
            && controller.lockedComet().equals(target), "Removing the disk during retrieval cannot change the target");
        filter.insertItem(0, disk.copy(), false);
        controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ESCAPING, "Filtered pulls still escape when power fails");
        for (int tick = 0; tick < HeavenshatterBeamControllerBlockEntity.ESCAPE_TICKS; tick++) controller.serverTick();
        supply(machine, 1000);
        controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCKED
            && controller.lockedComet().seed() == meteor.seed(), "Reusable filter finds the same recorded meteor on the next cycle");
        player.containerMenu = player.inventoryMenu;
        helper.setBlock(CONTROLLER, Blocks.AIR);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(controller.getBlockPos()).inflate(2));
        helper.assertTrue(drops.stream().filter(item -> net.minecraft.world.item.ItemStack.matches(item.getItem(), disk)).count() == 1,
            "Breaking the controller drops its recorded disk exactly once");
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void menuResendsInitialSnapshotAfterReloadAndReopen(GameTestHelper helper) {
        var controller = setup(helper).controller();
        var level = helper.getLevel();
        controller.serverTick();
        var saved = controller.saveWithoutMetadata(level.registryAccess());
        controller.loadAdditional(saved, level.registryAccess());
        var packets = new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "menu-reload"),
            net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
            new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
            net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { packets.add(packet); }
        };
        player.setPos(controller.getBlockPos().getCenter());
        level.players().add(player);
        try {
            for (int reopen = 0; reopen < 2; reopen++) {
                player.openMenu(controller, controller.getBlockPos());
                var menu = (HeavenshatterMenu) player.containerMenu;
                packets.clear();
                // Controller state is deliberately unchanged since the early initialization broadcast.
                menu.broadcastChanges();
                var snapshots = packets.stream()
                    .filter(packet -> packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)
                    .map(packet -> ((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket) packet).payload())
                    .filter(payload -> payload instanceof net.crystalnexus.network.payload.S2C_CelestialTracking)
                    .map(payload -> (net.crystalnexus.network.payload.S2C_CelestialTracking) payload).toList();
                helper.assertTrue(snapshots.size() == 1 && snapshots.getFirst().menuId() == menu.containerId,
                    "First attached broadcast delivers tracking even when initial state did not change");
                helper.assertTrue(snapshots.getFirst().state().equals(controller.trackingState())
                    && snapshots.getFirst().state().getList("markers", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 5,
                    "Reopened menu receives complete restored radar state");
                player.closeContainer();
            }
        } finally {
            player.containerMenu = player.inventoryMenu;
            level.players().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void formationAndPortLifecycle(GameTestHelper helper) { formation(helper); }
    @GameTest(template = "heavenshatter_beam", rotationSteps = 1)
    public static void formationEast(GameTestHelper helper) { formation(helper); }
    @GameTest(template = "heavenshatter_beam", rotationSteps = 2)
    public static void formationNorth(GameTestHelper helper) { formation(helper); }
    @GameTest(template = "heavenshatter_beam", rotationSteps = 3)
    public static void formationWest(GameTestHelper helper) { formation(helper); }

    private static void formation(GameTestHelper helper) {
        Machine machine = setup(helper);
        var controller = machine.controller();
        helper.assertTrue(machine.port().getEnergyStorage().receiveEnergy(1000, false) == 1000, "Bound input forwards FE");
        helper.assertTrue(controller.multiblockEnergyInput().getEnergyStored() == 1000, "FE is stored centrally");
        BlockPos broken = findCarbon(helper);
        var original = helper.getBlockState(broken);
        helper.assertTrue(!original.isAir(), "Chosen central block belongs to the template");
        helper.setBlock(broken, Blocks.AIR);
        helper.assertTrue(!controller.validateStructureNow(), "Broken structure invalidates controller");
        helper.assertTrue(!machine.port().isBoundTo(controller.getBlockPos()), "Invalid structure unbinds energy input");
        helper.assertTrue(machine.port().getEnergyStorage().receiveEnergy(1000, false) == 0, "Unbound input rejects FE");
        helper.setBlock(broken, original);
        helper.assertTrue(controller.validateStructureNow(), "Repair restores the multiblock");
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.assertTrue(!machine.port().isBoundTo(controller.getBlockPos()), "Breaking controller unbinds ports");
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam", timeoutTicks = 200)
    public static void locatorPullReloadAndMineOnce(GameTestHelper helper) {
        Machine machine = setup(helper);
        var controller = machine.controller();
        var registries = helper.getLevel().registryAccess();
        controller.serverTick();
        var save = controller.saveWithoutMetadata(registries);
        var candidates = save.getCompound("celestial").getList("candidates", net.minecraft.nbt.Tag.TAG_COMPOUND);
        helper.assertTrue(candidates.size() == 5, "Five candidates are generated");
        var answer = candidates.getCompound(0);
        int answerFrequency = answer.getInt("frequency"), answerBearing = answer.getInt("azimuth"), answerElevation = answer.getInt("elevation");
        helper.assertTrue(!controller.getUpdateTag(registries).contains("celestial")
            && !controller.trackingState().toString().contains("seed"), "Client snapshots never contain hidden candidate data");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "heavenshatter-test"));
        helper.getLevel().players().add(player);
        player.setPos(controller.getBlockPos().getX() + 0.5, controller.getBlockPos().getY() + 1, controller.getBlockPos().getZ() + 0.5);
        controller.controls(player, 0, answerFrequency, answerBearing, answerElevation, 0);
        helper.assertTrue(controller.frequency() == 50, "Controls require an open menu");
        player.containerMenu = new HeavenshatterMenu(1, player.getInventory(), controller);
        controller.controls(player, 0, 101, answerBearing, answerElevation, 0);
        helper.assertTrue(controller.frequency() == 50, "Out-of-range controls are rejected");
        supply(machine, 100000);
        controller.controls(player, 0, answerFrequency, answerBearing, answerElevation, 0);
        helper.runAfterDelay(65, () -> {
        try {
            helper.assertTrue(controller.trackingState().getBoolean("identified"), "Frequency matching identifies the comet");
            helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING, "Lock requires an explicit action");
            // Refill because this test intentionally waits longer than the stabilization threshold.
            supply(machine, 100000);
            var savedPuzzle = controller.saveWithoutMetadata(registries);
            helper.assertTrue(savedPuzzle.getCompound("celestial").getList("discoveries", net.minecraft.nbt.Tag.TAG_STRING).size() == 1, "Signature is remembered");
            controller.controls(player, 0, answerFrequency, answerBearing, answerElevation, 1);
            helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCKED, "Explicit action establishes lock");
            helper.assertTrue(controller.lockedComet() != null && controller.lockedComet().seed() == answer.getLong("seed"), "Lock keeps the selected candidate");
            var lockedSave = controller.saveWithoutMetadata(registries);
            controller.loadAdditional(lockedSave, registries);
            helper.assertTrue(controller.lockedComet() != null && controller.lockedComet().seed() == answer.getLong("seed"), "Lock survives reload");
            controller.multiblockEnergyInput().extractEnergy(Integer.MAX_VALUE, false);
            int firstCost = (int) GravitationalArrayCostSchedule.next(controller.cost(), 0, controller.duration());
            supply(machine, firstCost);
            controller.controls(player, answerFrequency, answerBearing, true);
            player.containerMenu = player.inventoryMenu;
            controller.serverTick();
            helper.assertTrue(controller.progress() == 1, "Retrieval continues with the GUI closed");
            var descentSave = controller.saveWithoutMetadata(registries);
            var expectedCells = controller.cells();
            controller.loadAdditional(descentSave, registries);
            helper.assertTrue(controller.progress() == 1 && controller.cells().equals(expectedCells), "Descent and geometry survive reload");

            BlockPos obstacle = controller.arrival().offset(0, 20, 0);
            helper.getLevel().setBlock(obstacle, Blocks.STONE.defaultBlockState(), 3);
            controller.validateStructureNow();
            int stored = controller.multiblockEnergyInput().getEnergyStored();
            controller.serverTick();
            helper.assertTrue(controller.progress() == 1 && controller.status() == HeavenshatterBeamControllerBlockEntity.Status.BLOCKED, "Blocked sky pauses descent");
            helper.assertTrue(controller.multiblockEnergyInput().getEnergyStored() == stored
                && helper.getLevel().getBlockState(obstacle).is(Blocks.STONE), "Pause consumes no FE and preserves the obstacle");
            helper.getLevel().setBlock(obstacle, Blocks.AIR.defaultBlockState(), 3);
            controller.validateStructureNow();
            long charged = GravitationalArrayCostSchedule.cumulative(controller.cost(), 1, controller.duration());
            for (int i = 1; i < controller.duration(); i++) charged += poweredTick(machine);
            helper.assertTrue(charged == controller.cost() && controller.multiblockEnergyInput().getEnergyStored() == 0, "Exactly the quoted FE is charged");
            helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ARRIVED, "Meteor materializes at completion");
            BlockPos base = controller.meteorBase();
            for (var cell : expectedCells) helper.assertTrue(helper.getLevel().getBlockState(base.offset(cell.x(), cell.y(), cell.z())).equals(controller.palette().get(cell.material())), "Placed meteor matches descending visual");
            var first = expectedCells.getFirst();
            BlockPos mined = base.offset(first.x(), first.y(), first.z());
            var recorded = net.crystalnexus.data.SummonedMeteorSavedData.get(helper.getLevel()).at(helper.getLevel(), mined);
            helper.assertTrue(recorded != null && recorded.palette().equals(controller.palette())
                && recorded.totalWeight() == expectedCells.size(), "Beam registers the original meteor for scanning on arrival");
            controller.loadAdditional(controller.saveWithoutMetadata(registries), registries);
            controller.serverTick();
            helper.assertTrue(net.crystalnexus.data.SummonedMeteorSavedData.get(helper.getLevel()).at(helper.getLevel(), mined).id().equals(recorded.id()),
                "Reloading an arrived controller preserves meteor identity");
            helper.getLevel().setBlock(mined, Blocks.AIR.defaultBlockState(), 3);
            for (int i = 0; i < 25; i++) controller.serverTick();
            helper.assertTrue(helper.getLevel().getBlockState(mined).isAir() && controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ARRIVED, "Mined blocks are never recreated; remaining meteor blocks new pulls");
            for (var cell : expectedCells) helper.getLevel().setBlock(base.offset(cell.x(), cell.y(), cell.z()), Blocks.AIR.defaultBlockState(), 3);
            controller.validateStructureNow();
            controller.serverTick();
            helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING && controller.diameter() == 0, "Clearing enables a new puzzle");
            helper.succeed();
        } finally {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().players().remove(player);
        }
        });
    }

    private static void supply(Machine machine, int amount) {
        // Energy inputs limit each transfer, so fill the test buffer through multiple real transfers.
        while (amount > 0) {
            int accepted = machine.port().getEnergyStorage().receiveEnergy(amount, false);
            if (accepted == 0) throw new AssertionError("Machine rejected test energy");
            amount -= accepted;
        }
    }

    private static long poweredTick(Machine machine) {
        var controller = machine.controller();
        int needed = (int) GravitationalArrayCostSchedule.next(controller.cost(), controller.progress(), controller.duration());
        supply(machine, needed);
        controller.serverTick();
        return needed;
    }

    @GameTest(template = "heavenshatter_beam")
    public static void powerLossEscapesPersistsAndRequiresANewPuzzle(GameTestHelper helper) {
        Machine machine = setup(helper);
        var controller = machine.controller();
        var registries = helper.getLevel().registryAccess();
        controller.serverTick();
        CompoundTag save = controller.saveWithoutMetadata(registries);
        save.putInt("stage", HeavenshatterBeamControllerBlockEntity.Stage.RETRIEVING.ordinal());
        save.putInt("diameter", 10);
        save.putInt("progress", HeavenshatterMeteor.DURATION / 2);
        save.putLong("seed", 12345);
        var palette = HeavenshatterMaterials.select(helper.getLevel(), controller.getBlockPos(), helper.getLevel().random);
        var materials = new net.minecraft.nbt.ListTag();
        for (var state : palette) materials.add(net.minecraft.nbt.NbtUtils.writeBlockState(state));
        save.put("palette", materials);
        controller.loadAdditional(save, registries);
        controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ESCAPING
            && controller.status() == HeavenshatterBeamControllerBlockEntity.Status.ESCAPING, "Missing FE releases the meteor");
        helper.assertTrue(controller.progress() == HeavenshatterMeteor.DURATION / 2, "Descent stops at its last paid step");
        var oldCells = controller.cells();
        BlockPos base = controller.meteorBase();
        machine.port().getEnergyStorage().receiveEnergy(200_000, false);
        int restoredEnergy = controller.multiblockEnergyInput().getEnergyStored();
        for (int i = 0; i < 34; i++) controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.ESCAPING, "Restoring power cannot recover a lost meteor");
        save = controller.saveWithoutMetadata(registries);
        controller.loadAdditional(save, registries);
        helper.assertTrue(controller.escapeTicks() == 34, "Escape animation survives reload");
        for (int i = 34; i < HeavenshatterBeamControllerBlockEntity.ESCAPE_TICKS; i++) controller.serverTick();
        helper.assertTrue(controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING
            && controller.diameter() == 0 && controller.lockProgress() == 0 && controller.palette().isEmpty(), "Escape discards the target and resets the puzzle");
        helper.assertTrue(controller.multiblockEnergyInput().getEnergyStored() == restoredEnergy, "Escape never consumes restored FE");
        for (var cell : oldCells) helper.assertTrue(helper.getLevel().getBlockState(base.offset(cell.x(), cell.y(), cell.z())).isAir(), "Lost meteor never materializes");
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void unloadedDestinationAndWorldHeightPause(GameTestHelper helper) {
        var controller = setup(helper).controller();
        controller.serverTick();
        var registries = helper.getLevel().registryAccess();
        CompoundTag original = controller.saveWithoutMetadata(registries);
        CompoundTag saved = original.copy();
        saved.putInt("stage", HeavenshatterBeamControllerBlockEntity.Stage.LOCKED.ordinal());
        saved.putLong("arrival", new BlockPos(10_000_000, 100, 10_000_000).asLong());
        controller.loadAdditional(saved, registries);
        controller.serverTick();
        helper.assertTrue(controller.status() == HeavenshatterBeamControllerBlockEntity.Status.UNLOADED, "Unloaded destination does not force load chunks");
        helper.assertTrue(!helper.getLevel().hasChunkAt(new BlockPos(10_000_000, 100, 10_000_000)), "Destination remains unloaded");
        saved.putLong("arrival", controller.getBlockPos().atY(helper.getLevel().getMaxBuildHeight() - 5).asLong());
        controller.loadAdditional(saved, registries);
        controller.serverTick();
        helper.assertTrue(controller.status() == HeavenshatterBeamControllerBlockEntity.Status.HEIGHT_LIMIT, "Meteor cannot exceed build height");
        controller.loadAdditional(original, registries);
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void materialPaletteFiltersUnsafeBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CONTROLLER);
        for (var block : new net.minecraft.world.level.block.Block[] {Blocks.SAND, Blocks.CHEST, Blocks.WATER, Blocks.BEDROCK, Blocks.AIR})
            helper.assertTrue(!HeavenshatterMaterials.eligible(block.defaultBlockState(), level, pos), "Unsafe material is rejected");
        helper.assertTrue(HeavenshatterMaterials.eligible(Blocks.DIAMOND_ORE.defaultBlockState(), level, pos), "Rare normal ores remain eligible");
        for (int i = 0; i < 50; i++) {
            var palette = HeavenshatterMaterials.select(level, pos, level.random);
            helper.assertTrue(palette.size() >= 2 && palette.size() <= 5 && palette.stream().distinct().count() == palette.size(), "Palette has two to five distinct types");
            helper.assertTrue(palette.stream().allMatch(state -> HeavenshatterMaterials.eligible(state, level, pos)), "All selected materials are safe");
        }
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void validationAndPortsNeverLoadMissingChunks(GameTestHelper helper) {
        Machine machine = setup(helper);
        var level = helper.getLevel();
        BlockPos remote = new BlockPos(12_000_000, 100, 12_000_000);
        helper.assertTrue(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null, "Remote chunk starts unloaded");
        int loaded = level.getChunkSource().getLoadedChunksCount();
        var match = net.crystalnexus.multiblock.StructureNbtValidator.validate(level,
            net.minecraft.resources.ResourceLocation.parse("crystalnexus:heavenshatter_beam"), remote, net.minecraft.core.Direction.SOUTH,
            CrystalnexusModBlocks.HEAVENSHATTER_BEAM_CONTROLLER.get(), net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
            java.util.Map.of(), false, false);
        helper.assertTrue(match.isEmpty(), "Unloaded multiblock fails validation without waiting");
        machine.port().bindController(remote);
        helper.assertTrue(machine.port().getEnergyStorage().receiveEnergy(1000, false) == 0, "Energy relay does not wait for an unloaded controller");
        helper.assertTrue(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null,
            "Validation and relay do not generate or load the remote chunk");
        helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded, "No surrounding remote chunks were loaded either");
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam")
    public static void celestialSignaturesSnapshotsAndValidation(GameTestHelper helper) {
        var machine = setup(helper);
        var scanner = new net.crystalnexus.celestial.CelestialScanner();
        scanner.generate(helper.getLevel(), machine.controller().getBlockPos());
        helper.assertTrue(scanner.candidates.size() == 5, "Datapack definitions produce five targets");
        var target = scanner.target();
        scanner.controls(0, target.azimuth(), target.elevation(), target.frequency());
        for (int i = 0; i < 39; i++) scanner.tick(true);
        helper.assertTrue(!scanner.identified() && !scanner.lock(), "Identification and lock cannot happen early");
        scanner.tick(true);
        helper.assertTrue(scanner.identified() && !scanner.lock(), "Identification occurs after 40 ticks; stability still required");
        var saved = scanner.save();
        var loaded = new net.crystalnexus.celestial.CelestialScanner();
        loaded.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.target().equals(target) && loaded.identified(), "Candidate and identified signature survive reload");
        helper.assertTrue(loaded.alignmentTicks == 40, "Reload preserves unfinished stability");
        for (int i = 0; i < 60; i++) loaded.tick(true);
        helper.assertTrue(loaded.lock() && loaded.lockedComet().equals(target), "Lock retains exactly the identified comet");
        int count = net.crystalnexus.multiblock.HeavenshatterMeteor.cells(target.diameter(), target.seed(), target.palette().size()).size();
        helper.assertTrue(target.information().getInt("mass") == count, "Displayed mass matches geometry");
        helper.assertTrue(!loaded.publicState().contains("candidates") && !loaded.publicState().toString().contains("seed"), "Observations exclude hidden generation data");
        loaded.reset();
        helper.assertTrue(loaded.discoveries.contains(target.classification().toString()), "New sessions keep discoveries");
        loaded.candidates.add(target);
        loaded.controls(0, target.azimuth(), target.elevation(), target.frequency());
        for (int i = 0; i < 19; i++) loaded.tick(true);
        helper.assertTrue(!loaded.identified(), "Known signatures still require tuning");
        loaded.tick(true);
        helper.assertTrue(loaded.identified(), "Known signatures identify after 20 ticks");
        loaded.tick(false);
        helper.assertTrue(loaded.alignmentTicks == 0 && loaded.identified(), "Power loss clears stability but keeps identification");
        var json = com.google.gson.JsonParser.parseString("{\"name\":\"Bad\",\"weight\":1,\"difficulty\":4,\"min_diameter\":7,\"max_diameter\":14,\"min_distance\":1,\"max_distance\":100}").getAsJsonObject();
        boolean rejected = false;
        try { net.crystalnexus.celestial.CometDefinitions.parse(net.minecraft.resources.ResourceLocation.parse("crystalnexus:bad"), json); }
        catch (IllegalArgumentException ex) { rejected = true; }
        helper.assertTrue(rejected, "Invalid definitions are rejected");
        helper.succeed();
    }

    @GameTest(template = "heavenshatter_beam", timeoutTicks = 100)
    public static void celestialPowerAndSharedControls(GameTestHelper helper) {
        var machine = setup(helper);
        var controller = machine.controller();
        controller.serverTick();
        var candidates = controller.saveWithoutMetadata(helper.getLevel().registryAccess()).getCompound("celestial").getList("candidates", net.minecraft.nbt.Tag.TAG_COMPOUND);
        var target = candidates.getCompound(0);
        var first = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "scanner-first"));
        var second = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "scanner-second"));
        for (var player : java.util.List.of(first, second)) {
            player.setPos(controller.getBlockPos().getX()+.5, controller.getBlockPos().getY()+1, controller.getBlockPos().getZ()+.5);
            player.containerMenu = new HeavenshatterMenu(1, player.getInventory(), controller);
            helper.getLevel().players().add(player);
        }
        machine.port().getEnergyStorage().receiveEnergy(10000, false);
        controller.controls(first, 0, target.getInt("frequency"), target.getInt("azimuth"), target.getInt("elevation"), 0);
        controller.controls(second, 0, target.getInt("frequency"), target.getInt("azimuth"), target.getInt("elevation"), 0);
        helper.runAfterDelay(5, () -> {
            int remaining = controller.multiblockEnergyInput().getEnergyStored();
            helper.assertTrue(remaining >= 4000 && remaining <= 6000, "Two viewers consume only one scanning charge per tick");
            controller.serverTick();
            int once = controller.multiblockEnergyInput().getEnergyStored();
            controller.serverTick();
            helper.assertTrue(controller.multiblockEnergyInput().getEnergyStored() == once, "Repeated calls in one server tick cannot double charge");
        });
        helper.runAfterDelay(15, () -> {
            try {
                helper.assertTrue(controller.multiblockEnergyInput().getEnergyStored() == 0, "Scanning consumes supplied energy");
                helper.assertTrue(controller.status() == HeavenshatterBeamControllerBlockEntity.Status.NO_POWER, "Power exhaustion pauses scanning");
                helper.assertTrue(controller.lockProgress() == 0 && controller.stage() == HeavenshatterBeamControllerBlockEntity.Stage.LOCATING, "Power loss cannot complete a lock");
                helper.assertTrue(controller.saveWithoutMetadata(helper.getLevel().registryAccess()).getCompound("celestial").getList("candidates", net.minecraft.nbt.Tag.TAG_COMPOUND).equals(candidates), "Power loss never rerolls candidates");
                helper.succeed();
            } finally {
                for (var player : java.util.List.of(first, second)) { player.containerMenu = player.inventoryMenu; helper.getLevel().players().remove(player); }
            }
        });
    }
}
