package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.block.ItemRouterBlock;
import net.crystalnexus.block.entity.ItemRouterBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.network.MachineInspectionNetwork;
import net.crystalnexus.util.MachineInspection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("crystalnexus_routers")
@PrefixGameTestTemplate(false)
public final class ItemRouterGameTests {
    private static ItemRouterBlockEntity place(GameTestHelper h, boolean merger, Direction facing) {
        var pos = h.absolutePos(new BlockPos(3, 4, 3));
        var level = h.getLevel();
        for (Direction side : Direction.values()) level.setBlockAndUpdate(pos.relative(side), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, (merger ? CrystalnexusModBlocks.MERGER : CrystalnexusModBlocks.SPLITTER)
            .get().defaultBlockState().setValue(ItemRouterBlock.FACING, facing));
        return (ItemRouterBlockEntity) level.getBlockEntity(pos);
    }

    private static Container chest(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        return (Container) h.getLevel().getBlockEntity(pos);
    }

    private static int count(Container container) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) count += container.getItem(i).getCount();
        return count;
    }

    @GameTest(template = "zero_point")
    public static void sidedInsertionSimulationAndComponents(GameTestHelper h) {
        for (boolean merger : new boolean[]{false, true}) for (Direction facing : Direction.Plane.HORIZONTAL) {
            var router = place(h, merger, facing);
            var level = h.getLevel();
            var pos = router.getBlockPos();
            for (Direction side : Direction.values()) {
                var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                boolean accepts = merger ? side.getAxis().isHorizontal() && side != facing.getOpposite() : side == facing.getOpposite();
                var stack = new ItemStack(Items.IRON_INGOT, 64);
                int remainder = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler, stack, true).getCount();
                h.assertTrue(remainder == (accepts ? 0 : 64) && router.isEmpty(), "Simulation must obey rotated ports without changing inventory");
                if (!accepts) continue;
                int slot = merger ? (side == facing ? 0 : side == facing.getCounterClockWise() ? 1 : 2) : 0;
                h.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler, stack, false).isEmpty()
                    && router.getItem(slot).getCount() == 64, "Each input must reach its own buffer");
                h.assertTrue(handler.extractItem(0, 64, false).isEmpty(), "Inputs must not allow automated extraction");
                h.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler, stack, false).getCount() == 64, "Full buffers reject more items");
                router.setItem(slot, new ItemStack(Items.IRON_INGOT, 1));
                stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Different"));
                h.assertTrue(net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(handler, stack, false).getCount() == 64,
                    "Different components cannot stack together");
                router.clearContent();
            }
        }
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void splitterBalancesSkipsBlockedAndHonorsPriorities(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var router = place(h, false, facing);
            Container[] sinks = new Container[3];
            for (int i = 0; i < 3; i++) sinks[i] = chest(h, router.getBlockPos().relative(ItemRouterBlock.redPort(facing, i)));
            for (int i = 0; i < 6; i++) {
                router.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
                router.tick();
                h.assertTrue(router.isEmpty(), "Each batch must transfer in one tick");
            }
            for (var sink : sinks) h.assertTrue(count(sink) == 64, "Round robin must distribute whole batches equally in every orientation");
            for (int i = 0; i < sinks[0].getContainerSize(); i++) sinks[0].setItem(i, new ItemStack(Items.STONE, 64));
            router.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
            router.tick();
            h.assertTrue(count(sinks[1]) == 96 && router.isEmpty(), "Blocked front output must be skipped for the whole batch");
            for (int mode = 1; mode <= 3; mode++) {
                router.cycleMode();
                for (var sink : sinks) sink.clearContent();
                router.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
                router.tick();
                h.assertTrue(count(sinks[mode - 1]) == 32 && router.isEmpty(), "Priority must send the whole batch to its named port");
            }
            for (var sink : sinks) for (int i = 0; i < sink.getContainerSize(); i++) sink.setItem(i, new ItemStack(Items.STONE, 64));
            router.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
            int before = count(router);
            router.tick();
            h.assertTrue(count(router) == before, "Blocked outputs must retain items");
        }
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void mergerBalancesMixedInputsAndPersistsProgress(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var router = place(h, true, facing);
            var sink = chest(h, router.getBlockPos().relative(facing.getOpposite()));
            router.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
            router.setItem(1, new ItemStack(Items.GOLD_INGOT, 32));
            router.setItem(2, new ItemStack(Items.DIAMOND, 32));
            router.tick();
            var saved = router.saveWithFullMetadata(h.getLevel().registryAccess());
            router.loadWithComponents(saved, h.getLevel().registryAccess());
            router.tick();
            h.assertTrue(router.getItem(0).isEmpty() && router.getItem(1).isEmpty() && router.getItem(2).getCount() == 32,
                "Reload must preserve the next round-robin input");
            router.tick();
            h.assertTrue(count(sink) == 96 && router.isEmpty(), "Mixed input batches must merge without loss");
            router.setItem(1, new ItemStack(Items.GOLD_INGOT, 32));
            router.setItem(2, new ItemStack(Items.DIAMOND, 32));
            saved = router.saveWithFullMetadata(h.getLevel().registryAccess());
            saved.putInt("RoutingMode", ItemRouterBlockEntity.Mode.PRIORITY_RIGHT.ordinal());
            router.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(router.cycleMode() == ItemRouterBlockEntity.Mode.ROUND_ROBIN,
                "Mergers must ignore legacy settings and cannot cycle modes");
            router.tick();
            h.assertTrue(router.getItem(1).isEmpty() && router.getItem(2).getCount() == 32,
                "Automatic merging must skip empty inputs even after loading an old priority mode");
            for (int i = 0; i < sink.getContainerSize(); i++) sink.setItem(i, new ItemStack(Items.STONE, 64));
            int before = count(router);
            router.tick();
            h.assertTrue(count(router) == before, "Full output must retain every input");
            saved = router.saveWithFullMetadata(h.getLevel().registryAccess());
            router.cycleMode();
            router.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(router.mode() == ItemRouterBlockEntity.Mode.ROUND_ROBIN && count(router) == before
                    && !saved.contains("RoutingMode"),
                "Merger inventories must survive reload without saving settings");
        }
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void mergerWrenchRotatesWithoutOfferingSettings(GameTestHelper h) {
        var router = place(h, true, Direction.NORTH);
        var level = h.getLevel();
        var pos = router.getBlockPos();
        router.setItem(0, new ItemStack(Items.DIAMOND, 32));
        var status = MachineInspection.read(level, pos).wrenchStatus();
        h.assertTrue(status.equals(java.util.List.of(
            net.minecraft.network.chat.Component.translatable("gui.crystalnexus.wrench.rotate"),
            net.minecraft.network.chat.Component.translatable("gui.crystalnexus.wrench.dismantle"))),
            "Merger popup must only offer rotation and dismantling");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "merger-wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var wrench = player.getMainHandItem();
        wrench.getItem().onItemUseFirst(wrench, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(router.mode() == ItemRouterBlockEntity.Mode.ROUND_ROBIN
                && level.getBlockState(pos).getValue(ItemRouterBlock.FACING) == Direction.EAST
                && count(router) == 32,
            "Wrench must rotate the merger and preserve its buffered items");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void routersFeedEachOtherAndConveyors(GameTestHelper h) {
        var splitter = place(h, false, Direction.NORTH);
        var level = h.getLevel();
        var pos = splitter.getBlockPos();
        var mergerPos = pos.east();
        level.setBlockAndUpdate(mergerPos, CrystalnexusModBlocks.MERGER.get().defaultBlockState());
        var merger = (ItemRouterBlockEntity) level.getBlockEntity(mergerPos);
        var sink = chest(h, mergerPos.south());
        var input = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.SOUTH);
        net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(input, new ItemStack(Items.DIAMOND, 32), false);
        splitter.tick();
        h.assertTrue(merger.getItem(1).getCount() == 32, "Splitter must feed the merger's corresponding red-side buffer as a whole batch");
        merger.tick();
        h.assertTrue(count(sink) == 32 && splitter.isEmpty() && merger.isEmpty(), "Router chains must conserve whole batches");
        var beltPos = pos.north();
        level.setBlockAndUpdate(beltPos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(beltPos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING, Direction.NORTH));
        splitter.cycleMode();
        net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(input, new ItemStack(Items.DIAMOND, 32), false);
        splitter.tick();
        var belt = (Container) level.getBlockEntity(beltPos);
        h.assertTrue(count(belt) == 32 && splitter.isEmpty(), "Splitter must push whole batches into conveyors through their item capability");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void partialBatchTransfersRetainExactlyTheRemainder(GameTestHelper h) {
        for (boolean merger : new boolean[]{false, true}) {
            var router = place(h, merger, Direction.NORTH);
            var sink = chest(h, router.getBlockPos().relative(merger ? Direction.SOUTH : Direction.NORTH));
            for (int slot = 0; slot < sink.getContainerSize(); slot++) sink.setItem(slot, new ItemStack(Items.STONE, 64));
            sink.setItem(0, new ItemStack(Items.DIAMOND, 56));
            router.setItem(0, new ItemStack(Items.DIAMOND, 32));
            router.tick();
            h.assertTrue(sink.getItem(0).getCount() == 64 && router.getItem(0).getCount() == 24,
                "Only the accepted part of a batch may leave its input buffer");
            router.tick();
            h.assertTrue(router.getItem(0).getCount() == 24, "A full destination must retain the remainder");
            sink.setItem(0, ItemStack.EMPTY);
            router.tick();
            h.assertTrue(sink.getItem(0).getCount() == 24 && router.isEmpty(), "The remaining batch must transfer without loss or duplication");
        }
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void otherWrenchModesAppearInPopup(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var blocks = new net.minecraft.world.level.block.Block[]{
            CrystalnexusModBlocks.CONVEYER_BELT.get(), CrystalnexusModBlocks.DEPOT_CABLE.get(),
            CrystalnexusModBlocks.PIPE_STRAIGHT.get()};
        for (var block : blocks) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var before = MachineInspection.read(level, pos);
            h.assertTrue(before != null && !before.wrenchStatus().isEmpty(), "Configurable blocks need popup controls");
            if (block instanceof net.crystalnexus.block.DepotCableBlock)
                level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(net.crystalnexus.block.DepotCableBlock.MODE,
                    net.crystalnexus.block.DepotCableMode.EXPORT));
            else if (level.getBlockEntity(pos) instanceof net.crystalnexus.block.entity.PipeStraightBlockEntity pipe)
                pipe.cycleSideMode(Direction.NORTH);
            else net.crystalnexus.block.ConveyerBeltMode.cycle(level, pos);
            var after = MachineInspection.read(level, pos);
            h.assertTrue(!before.wrenchStatus().equals(after.wrenchStatus()), "Popup must reflect changed mode");
            h.assertTrue(after.wrenchStatus().size() <= MachineInspection.MAX_WRENCH_LINES, "Status fits packet bound");
            var packet = new MachineInspectionNetwork.Response(level.dimension().location(), pos, 1,
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()), after);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                MachineInspectionNetwork.Response.CODEC.encode(buffer, packet);
                var decoded = MachineInspectionNetwork.Response.CODEC.decode(buffer);
                h.assertTrue(decoded.snapshot().wrenchStatus().equals(after.wrenchStatus()) && buffer.readableBytes() == 0,
                    "Packet must preserve all wrench status lines");
            } finally { buffer.release(); }
        }
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.MACHINE_FRAME.get().defaultBlockState());
        h.assertTrue(!net.crystalnexus.item.CrystalWrenchItem.canPickup(level, pos), "Frames without block entities must not advertise unsupported pickup");
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(MachineInspection.read(level, pos) == null, "Ordinary blocks do not show machine popups");
        h.succeed();
    }

    @GameTest(template = "zero_point")
    public static void wrenchPopupPacketAndPickupConserveItems(GameTestHelper h) {
        var router = place(h, false, Direction.NORTH);
        var level = h.getLevel();
        var pos = router.getBlockPos();
        router.setItem(0, new ItemStack(Items.DIAMOND, 17));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "router-wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        var wrench = player.getMainHandItem();
        wrench.getItem().onItemUseFirst(wrench, context);
        h.assertTrue(router.mode() == ItemRouterBlockEntity.Mode.PRIORITY_FRONT
            && router.getBlockState().getValue(ItemRouterBlock.FACING) == Direction.NORTH, "Wrench must cycle mode without rotating");
        var inspection = MachineInspection.read(level, pos);
        var packet = new MachineInspectionNetwork.Response(level.dimension().location(), pos, 1,
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(router.getBlockState().getBlock()), inspection);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            MachineInspectionNetwork.Response.CODEC.encode(buffer, packet);
            var decoded = MachineInspectionNetwork.Response.CODEC.decode(buffer);
            h.assertTrue(decoded.snapshot().wrenchStatus().equals(inspection.wrenchStatus()) && decoded.snapshot().items().getFirst().count() == 17,
                "Inspection packet must preserve routing mode and items");
        } finally { buffer.release(); }
        player.setShiftKeyDown(true);
        wrench.getItem().onItemUseFirst(wrench, context);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        ItemStack packed = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(CrystalnexusModItems.SPLITTER.get())).findFirst().orElseThrow();
        h.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(Items.DIAMOND)), "Wrench pickup must not duplicate packed contents");
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.SPLITTER.get().defaultBlockState());
        var state = level.getBlockState(pos);
        state.getBlock().setPlacedBy(level, pos, state, player, packed);
        var restored = (ItemRouterBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(restored.getItem(0).getCount() == 17 && restored.mode() == ItemRouterBlockEntity.Mode.PRIORITY_FRONT,
            "Placement must restore packed contents and mode");
        drops.forEach(ItemEntity::discard);
        level.destroyBlock(pos, true);
        int diamonds = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
            .map(ItemEntity::getItem).filter(s -> s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(diamonds == 17, "Breaking must drop each buffered item exactly once");
        h.succeed();
    }
}
