package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.init.CrystalnexusModEntities;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.CrystalWrenchItem;
import net.crystalnexus.network.MachineInspectionNetwork;
import net.crystalnexus.util.MachineInspection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_drones")
@PrefixGameTestTemplate(false)
public final class WrenchInspectionGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "wrench-inspect"));
    }
    private static LUNADroneEntity drone(GameTestHelper h) {
        var drone = CrystalnexusModEntities.UNIVERSAL_DRONE.get().create(h.getLevel());
        drone.setPos(h.absoluteVec(new Vec3(3.5, 4, 5.5)));
        return drone;
    }
    private static void aim(ServerPlayer player, Vec3 point) {
        var delta = point.subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }
    private static MachineInspectionNetwork.DroneResponse roundTrip(GameTestHelper h, LUNADroneEntity drone, MachineInspection snapshot) {
        var packet = new MachineInspectionNetwork.DroneResponse(h.getLevel().dimension().location(), drone.getId(), drone.getUUID(), 2, snapshot);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            MachineInspectionNetwork.DroneResponse.CODEC.encode(buffer, packet);
            var result = MachineInspectionNetwork.DroneResponse.CODEC.decode(buffer);
            h.assertTrue(buffer.readableBytes() == 0, "Packet consumes exactly its snapshot");
            return result;
        } finally { buffer.release(); }
    }
    @GameTest(template = "empty")
    public static void doubleChestInspectionIncludesBothHalvesAndBoundsPreview(GameTestHelper h) {
        var level = h.getLevel();
        var leftPos = h.absolutePos(new BlockPos(2, 2, 2));
        var rightPos = leftPos.east();
        var state = Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,
            net.minecraft.core.Direction.NORTH);
        level.setBlockAndUpdate(leftPos, state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
            net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        level.setBlockAndUpdate(rightPos, state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
            net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        var left = (net.minecraft.world.Container) level.getBlockEntity(leftPos);
        var right = (net.minecraft.world.Container) level.getBlockEntity(rightPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 32));
        right.setItem(0, new ItemStack(Items.EMERALD, 16));
        for (var pos : List.of(leftPos, rightPos)) {
            var snapshot = MachineInspection.read(level, pos);
            h.assertTrue(snapshot.inventory() && snapshot.items().size() == 2
                && snapshot.items().stream().anyMatch(item -> item.icon().is(Items.DIAMOND) && item.count() == 32)
                && snapshot.items().stream().anyMatch(item -> item.icon().is(Items.EMERALD) && item.count() == 16),
                "Either half of a double chest must show all contents");
        }
        for (int slot = 1; slot < 10; slot++) right.setItem(slot, new ItemStack(Items.IRON_INGOT, slot));
        var snapshot = MachineInspection.read(level, leftPos);
        h.assertTrue(snapshot.items().size() == MachineInspection.MAX_ITEMS && snapshot.extraItems() == 3,
            "Large inventories must keep the bounded preview and overflow count");
        h.assertTrue(left.getItem(0).getCount() == 32 && right.getItem(0).getCount() == 16,
            "Reading both halves must not change their contents");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void droneSnapshotsKeepPrivateFieldsOwnerOnly(GameTestHelper h) {
        var owner = player(h);
        var stranger = player(h);
        var drone = drone(h);
        drone.owner = owner.getUUID();
        drone.authorized.add(stranger.getUUID());
        drone.status = "Private current task";
        drone.setHealth(13);
        for (int i = 0; i < 9; i++) drone.inventory.setStackInSlot(i, new ItemStack(Items.DIAMOND, i + 1));
        drone.tank.setFluid(new FluidStack(Fluids.WATER, 1500));
        drone.recoveryTank.setFluid(new FluidStack(Fluids.LAVA, 250));
        var details = roundTrip(h, drone, MachineInspection.read(drone, owner)).snapshot();
        h.assertTrue(details.items().size() == 8 && details.extraItems() == 1 && details.items().getFirst().count() == 1,
            "Owner cargo is bounded without losing overflow count");
        h.assertTrue(details.energy() && details.storedEnergy() == 200000 && details.tanks().size() == 2
            && details.tanks().get(1).amount() == 250, "Owner sees battery and recovery fluid");
        h.assertTrue(details.wrenchStatus().contains(Component.translatable("gui.crystalnexus.scanner.drone_health", 13, 20)),
            "Owner sees current health");
        for (boolean authorized : new boolean[]{true, false}) {
            if (!authorized) drone.authorized.clear();
            var publicView = roundTrip(h, drone, MachineInspection.read(drone, stranger)).snapshot();
            h.assertTrue(!publicView.inventory() && publicView.items().isEmpty() && !publicView.energy()
                && publicView.storedEnergy() == 0 && !publicView.fluids() && publicView.tanks().isEmpty(), "Nonowners receive no private storage fields");
            h.assertTrue(publicView.wrenchStatus().size() == 2 && publicView.wrenchStatus().getFirst().equals(Component.literal(drone.displayStatus())),
                "Nonowners receive only the existing public status and notice");
        }
        drone.owner = null;
        var unowned = MachineInspection.read(drone, stranger);
        h.assertTrue(drone.owner == null && unowned.wrenchStatus().contains(Component.translatable("gui.crystalnexus.scanner.drone_claim")),
            "Inspection offers claim without claiming");
        drone.owner = owner.getUUID();
        var before = drone.mode;
        drone.program.running = true;
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        owner.setShiftKeyDown(true);
        drone.mobInteract(owner, InteractionHand.MAIN_HAND);
        h.assertTrue(drone.mode == before.next() && !drone.program.running, "Existing wrench interaction changes mode and stops program");
        drone.battery.extractEnergy(1000, false);
        drone.inventory.setStackInSlot(0, ItemStack.EMPTY);
        drone.recoveryTank.setFluid(FluidStack.EMPTY);
        var changed = MachineInspection.read(drone, owner);
        h.assertTrue(changed.storedEnergy() == 199000 && changed.extraItems() == 0 && changed.tanks().get(1).amount() == 0
            && !changed.wrenchStatus().equals(details.wrenchStatus()), "Fresh snapshots reflect all changes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void droneRequestsRequireActualUnobstructedTarget(GameTestHelper h) {
        var player = player(h);
        var drone = drone(h);
        h.getLevel().addFreshEntity(drone);
        player.setPos(h.absoluteVec(new Vec3(3.5, 4, 3.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        aim(player, drone.getBoundingBox().getCenter());
        var request = new MachineInspectionNetwork.DroneRequest(h.getLevel().dimension().location(), drone.getId(), drone.getUUID(), 1);
        h.assertTrue(MachineInspectionNetwork.canInspect(player, request), "Nearby aimed drone is inspectable");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, request), "Wrench is required");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        h.assertTrue(MachineInspectionNetwork.canInspect(player, request), "Offhand wrench works");
        player.setYHeadRot(player.getYHeadRot() + 180);
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, request), "Looking away rejects request");
        aim(player, drone.getBoundingBox().getCenter());
        var wall = h.absolutePos(new BlockPos(3, 4, 4));
        h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, request), "Solid blocks hide drones");
        h.getLevel().removeBlock(wall, false); h.getLevel().removeBlock(wall.above(), false);
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, new MachineInspectionNetwork.DroneRequest(
            request.dimension(), request.entityId(), UUID.randomUUID(), 1)), "Reused entity IDs do not match UUIDs");
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, new MachineInspectionNetwork.DroneRequest(
            Level.NETHER.location(), request.entityId(), request.uuid(), 1)), "Wrong dimension rejected");
        player.setPos(player.position().add(0, 0, -10)); aim(player, drone.getBoundingBox().getCenter());
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, request), "Out of reach rejected");
        drone.discard();
        h.assertTrue(!MachineInspectionNetwork.canInspect(player, request), "Removed drones rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void snapshotsRejectStaleIdentityAndOversizedLists(GameTestHelper h) {
        var drone = drone(h);
        var viewer = player(h);
        var response = roundTrip(h, drone, MachineInspection.read(drone, viewer));
        h.assertTrue(response.matches(response.dimension(), drone.getId(), drone.getUUID(), 1, 2), "Current response accepted");
        h.assertTrue(!response.matches(response.dimension(), drone.getId(), drone.getUUID(), 2, 3)
            && !response.matches(response.dimension(), drone.getId(), drone.getUUID(), 0, 1)
            && !response.matches(response.dimension(), drone.getId() + 1, drone.getUUID(), 0, 2)
            && !response.matches(response.dimension(), drone.getId(), UUID.randomUUID(), 0, 2)
            && !response.matches(Level.NETHER.location(), drone.getId(), drone.getUUID(), 0, 2), "Old, unsent, other-target and other-dimension responses rejected");
        h.assertTrue(MachineInspectionNetwork.fresh(19, 0) && !MachineInspectionNetwork.fresh(20, 0), "Stale panels expire");
        var snapshot = response.snapshot();
        var oversized = new MachineInspection(snapshot.name(), -1, false, List.of(), 0, false, 0, 0,
            false, List.of(), 0, -1, java.util.Collections.nCopies(MachineInspection.MAX_WRENCH_LINES + 1, Component.literal("bad")));
        boolean rejected = false;
        try { roundTrip(h, drone, oversized); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "Oversized status list rejected at decoding boundary");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void restrictedFacingDoesNotCrashInspectionOrWrenchUse(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var player = player(h);
        var wrench = new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var blocks = new java.util.ArrayList<net.minecraft.world.level.block.Block>();
        blocks.add(Blocks.HOPPER);
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
            net.minecraft.resources.ResourceLocation.parse("create:chute")).ifPresent(blocks::add);
        for (var block : blocks) {
            var facing = (net.minecraft.world.level.block.state.properties.DirectionProperty) block.getStateDefinition().getProperty("facing");
            var down = block.defaultBlockState().setValue(facing, net.minecraft.core.Direction.DOWN);
            level.setBlockAndUpdate(pos, down);
            h.assertTrue(!CrystalWrenchItem.canRotate(level, pos), "Unsupported upward facing must not count as rotation");
            h.assertTrue(MachineInspection.canInspect(level, pos) && !MachineInspection.read(level, pos).wrenchStatus()
                .contains(Component.translatable("gui.crystalnexus.wrench.rotate")), "Restricted block remains inspectable without a rotate hint");
            var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            wrench.getItem().onItemUseFirst(wrench, new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            h.assertTrue(level.getBlockState(pos).getValue(facing) == net.minecraft.core.Direction.DOWN, "Wrench must leave unsupported facing unchanged");
            level.setBlockAndUpdate(pos, down.setValue(facing, net.minecraft.core.Direction.NORTH));
            h.assertTrue(CrystalWrenchItem.canRotate(level, pos), "Allowed horizontal rotation remains available");
        }
        level.setBlockAndUpdate(pos, Blocks.OBSERVER.defaultBlockState().setValue(
            net.minecraft.world.level.block.ObserverBlock.FACING, net.minecraft.core.Direction.DOWN));
        h.assertTrue(CrystalWrenchItem.canRotate(level, pos), "Full facing properties retain vertical rotation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaContainersShowContentsAndWrenchHints(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var level = h.getLevel();
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((net.minecraft.world.Container)level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 32));
        var chest = MachineInspection.read(level, pos);
        h.assertTrue(MachineInspection.canInspect(level, pos) && chest.inventory() && chest.items().size() == 1
            && chest.items().getFirst().icon().is(Items.DIAMOND) && chest.items().getFirst().count() == 32
            && chest.wrenchStatus().contains(Component.translatable("gui.crystalnexus.wrench.rotate"))
            && chest.wrenchStatus().contains(Component.translatable("gui.crystalnexus.wrench.dismantle")), "Chest exposes its items and wrench hints");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var response = new MachineInspectionNetwork.Response(level.dimension().location(), pos, 1,
                net.minecraft.resources.ResourceLocation.parse("minecraft:chest"), chest);
            MachineInspectionNetwork.Response.CODEC.encode(buffer, response);
            var received = MachineInspectionNetwork.Response.CODEC.decode(buffer).snapshot();
            h.assertTrue(received.inventory() && received.items().getFirst().icon().is(Items.DIAMOND)
                && received.items().getFirst().count() == 32, "Container contents must reach the client unchanged");
        } finally { buffer.release(); }
        for (var block : List.of(Blocks.BARREL, Blocks.HOPPER, Blocks.FURNACE, Blocks.SHULKER_BOX)) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var inventory = (net.minecraft.world.Container) level.getBlockEntity(pos);
            h.assertTrue(MachineInspection.read(level, pos).inventory() && MachineInspection.read(level, pos).items().isEmpty(),
                "Empty containers must still display their inventory section");
            inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 12));
            var contents = MachineInspection.read(level, pos);
            h.assertTrue(contents.inventory() && contents.items().getFirst().icon().is(Items.IRON_INGOT)
                && contents.items().getFirst().count() == 12 && inventory.getItem(0).getCount() == 12,
                "Container inspection must report contents without extracting items");
            inventory.setItem(0, ItemStack.EMPTY);
            h.assertTrue(MachineInspection.read(level, pos).items().isEmpty(), "Fresh snapshots must reflect emptied slots");
        }
        level.setBlockAndUpdate(pos, Blocks.OAK_STAIRS.defaultBlockState());
        h.assertTrue(MachineInspection.canInspect(level, pos) && !CrystalWrenchItem.canPickup(level, pos)
            && MachineInspection.read(level, pos).wrenchStatus().size() == 1, "Rotatable block without entity exposes only rotation");
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        h.assertTrue(!MachineInspection.canInspect(level, pos) && MachineInspection.read(level, pos) == null, "Unsupported blocks have no popup");
        h.succeed();
    }
}
