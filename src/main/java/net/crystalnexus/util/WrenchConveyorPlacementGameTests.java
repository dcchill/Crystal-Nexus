package net.crystalnexus.util;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltBlock;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("crystalnexus_conveyors")
@PrefixGameTestTemplate(false)
public final class WrenchConveyorPlacementGameTests {
    @GameTest(template = "zero_point")
    public static void wrenchChoosesGateFacingAttachedInventoryFirst(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 4, 3));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new GameProfile(UUID.randomUUID(), "wrench-gates"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean behind : new boolean[] {false, true}) {
            for (Direction side : Direction.Plane.HORIZONTAL)
                level.setBlockAndUpdate(pos.relative(side), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get().defaultBlockState()
                .setValue(ConveyerBeltBlock.FACING, facing));
            var belt = (net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            belt.setManualMode(true);
            belt.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
            level.setBlockAndUpdate(pos.relative(behind ? facing.getOpposite() : facing), Blocks.BARREL.defaultBlockState());
            use(player, pos);
            var state = level.getBlockState(pos);
            helper.assertTrue(state.is(behind ? CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()
                    : CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get()), "First wrench click must put the gate next to storage");
            helper.assertTrue(state.getValue(ConveyerBeltBlock.FACING) == facing,
                "Choosing the gate must preserve travel direction");
            belt = (net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(belt.getItem(2).getCount() == 3
                    && net.crystalnexus.block.ConveyerBeltMode.tierAt(state) == net.crystalnexus.block.ConveyerBeltTier.METEORITE,
                "Choosing the gate must preserve contents and tier");
            use(player, pos);
            helper.assertTrue(level.getBlockState(pos).is(behind ? CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get()
                    : CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()), "The alternate mode must remain reachable");
            use(player, pos);
            helper.assertTrue(level.getBlockState(pos).is(CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get()),
                "The third click must return to normal mode");
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void wrenchBuildsConnectedTurnsAndSlopesForEveryTier(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
            new GameProfile(UUID.randomUUID(), "wrench-conveyors"), ClientInformation.createDefault());
        for (var block : List.of(CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get())) {
            for (int dy : new int[] {-1, 1}) for (boolean xFirst : new boolean[] {false, true}) {
                var start = helper.absolutePos(new BlockPos(1, 4, 1));
                var end = start.offset(3, dy, 3);
                for (int x = 0; x <= 3; x++) for (int z = 0; z <= 3; z++) for (int y = -2; y <= 2; y++)
                    level.setBlockAndUpdate(start.offset(x, y, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(start.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(end.below(), Blocks.STONE.defaultBlockState());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(block, 20));
                player.setYRot(xFirst ? -90 : 0);
                use(player, start.below());
                use(player, end.below());
                var route = ConveyorGeometry.plan(start, end, xFirst);
                for (int i = 0; i < route.size(); i++) {
                    var segment = route.get(i);
                    var state = level.getBlockState(segment.pos());
                    helper.assertTrue(state.is(block)
                            && state.getValue(ConveyerBeltBlock.FACING) == segment.facing()
                            && state.getValue(ConveyerBeltBlock.CURVE) == segment.curve()
                            && state.getValue(ConveyorGeometry.SLOPE) == segment.slope(),
                        "Wrench must place the planned tier, direction, turn and slope at " + segment.pos() + "; got " + state);
                    if (i > 0) {
                        var previous = route.get(i - 1).pos();
                        helper.assertTrue(ConveyorGeometry.connects(previous, level.getBlockState(previous), segment.pos(), state),
                            "Every turn and ramp must connect to the preceding belt");
                    }
                }
                helper.assertTrue(player.getOffhandItem().getCount() == 20 - route.size(), "Consume exactly the placed belts");
                helper.assertTrue(BlockPos.of(ConveyorPlacement.preview(player.getMainHandItem()).getLong("start")).equals(end),
                    "Keep the endpoint as the next fixed node");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void wrenchContinuesFromFixedNodes(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
            new GameProfile(UUID.randomUUID(), "wrench-nodes"), ClientInformation.createDefault());
        var block = CrystalnexusModBlocks.CONVEYER_BELT.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(block, 20));
        player.setYRot(-90);
        var start = helper.absolutePos(new BlockPos(1, 4, 1));
        var node = start.east(2);
        var end = node.east(2).south(2);
        var first = ConveyorGeometry.plan(start, node, true);
        var second = ConveyorGeometry.plan(node, end, true);
        for (var route : List.of(first, second)) for (var segment : route) {
            level.setBlockAndUpdate(segment.pos(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(segment.pos().above(), Blocks.AIR.defaultBlockState());
        }
        for (var pos : List.of(start, node, end))
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        use(player, start.below());
        helper.assertTrue(ConveyorPlacement.cancel(player)
                && ConveyorPlacement.preview(player.getMainHandItem()).isEmpty()
                && level.isEmptyBlock(start) && player.getOffhandItem().getCount() == 20,
            "Cancelling the first selection must clear it without building or consuming belts");
        use(player, start.below());
        use(player, node.below());
        var nodeEntity = level.getBlockEntity(node);
        level.setBlockAndUpdate(second.get(1).pos(), Blocks.OBSIDIAN.defaultBlockState());
        use(player, end.below());
        helper.assertTrue(BlockPos.of(ConveyorPlacement.preview(player.getMainHandItem()).getLong("start")).equals(node)
                && player.getOffhandItem().getCount() == 20 - first.size(),
            "Failed continuation must keep the node and materials");
        level.setBlockAndUpdate(second.get(1).pos(), Blocks.AIR.defaultBlockState());
        use(player, end.below());
        for (var route : List.of(first, second)) for (var segment : route) {
            var state = level.getBlockState(segment.pos());
            helper.assertTrue(state.is(block) && state.equals(segment.state(state)),
                "Both sections must retain their planned direction, corner and slope at " + segment.pos());
        }
        helper.assertTrue(level.getBlockEntity(node) == nodeEntity, "Continuation must preserve the shared node");
        helper.assertTrue(player.getOffhandItem().getCount() == 20 - first.size() - second.size() + 1,
            "The shared node must only cost one belt");
        helper.assertTrue(BlockPos.of(ConveyorPlacement.preview(player.getMainHandItem()).getLong("start")).equals(end),
            "Advance the node after each placement");
        helper.assertTrue(ConveyorPlacement.cancel(player), "An active route must accept cancellation");
        helper.assertTrue(ConveyorPlacement.preview(player.getMainHandItem()).isEmpty()
                && ConveyorGeometry.isBelt(level.getBlockState(end))
                && player.getOffhandItem().getCount() == 20 - first.size() - second.size() + 1,
            "Cancelling must keep the built route and remaining materials");
        helper.assertTrue(!ConveyorPlacement.cancel(player), "Normal wrench attacks must pass through without an active route");
        use(player, end);
        use(player, end);
        helper.assertTrue(ConveyorPlacement.preview(player.getMainHandItem()).isEmpty(), "Clicking the node still finishes");
        helper.succeed();
    }

    private static void use(ServerPlayer player, BlockPos anchor) {
        var wrench = player.getMainHandItem();
        var hit = new BlockHitResult(Vec3.atCenterOf(anchor).add(0, .5, 0), Direction.UP, anchor, false);
        wrench.getItem().onItemUseFirst(wrench,
            new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, wrench, hit));
    }
}
