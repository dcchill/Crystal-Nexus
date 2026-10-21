package net.crystalnexus.util;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltBlock;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
public final class ConveyorPlacementGameTests {
    @GameTest(template = "zero_point")
    public static void individualBeltsAutomaticallySlopeAndRequireClearance(GameTestHelper helper) {
        var player = equippedPlayer(helper, "conveyor-slopes", 1);
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        for (var block : List.of(CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get())) {
            for (Direction facing : Direction.Plane.HORIZONTAL) for (var slope : List.of(ConveyorGeometry.Slope.UP, ConveyorGeometry.Slope.DOWN)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                player.setYRot(facing.toYRot());
                var flat = block.defaultBlockState().setValue(ConveyerBeltBlock.FACING, facing);
                var high = pos.relative(slope == ConveyorGeometry.Slope.UP ? facing : facing.getOpposite()).above();
                level.setBlockAndUpdate(high, flat);
                var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
                var context = new net.minecraft.world.item.context.BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
                helper.assertTrue(block.getStateForPlacement(context).getValue(ConveyorGeometry.SLOPE) == slope,
                    "Placement must find the raised connection for " + block + " " + facing + " " + slope);
                level.setBlockAndUpdate(pos, flat);
                ConveyorGeometry.updateAutomatic(level, pos);
                helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == slope,
                    "Neighbor updates must select the slope");
                level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                ConveyorGeometry.updateAutomatic(level, pos);
                helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.FLAT,
                    "Blocked clearance must prevent ramps");
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(high, flat.setValue(ConveyerBeltBlock.FACING, facing.getOpposite()));
                ConveyorGeometry.updateAutomatic(level, pos);
                helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.FLAT,
                    "Opposing belts must not create ramps");
                level.removeBlock(high, false);
                ConveyorGeometry.updateAutomatic(level, pos);
                helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.FLAT,
                    "Removing raised belts must flatten the conveyor");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void diagonalPlacementAndRemovalUpdateExistingBelts(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var flat = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, flat);
        helper.runAfterDelay(3, () -> level.setBlockAndUpdate(pos.east().above(), flat));
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.UP,
                "Placing a diagonal upper belt must update the lower belt");
            level.removeBlock(pos.east().above(), false);
        });
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.FLAT,
                "Removing the upper belt must flatten the lower belt");
            level.setBlockAndUpdate(pos.west().above(), flat);
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.DOWN,
                "A raised feeder must create a downward slope");
            level.removeBlock(pos.west().above(), false);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.FLAT,
                "Removing a raised feeder must flatten the belt");
            helper.succeed();
        });
    }
    @GameTest(template = "zero_point")
    public static void conveyorRoutePlacesCurvesAndRejectsObstructions(GameTestHelper helper) {
        var player=equippedPlayer(helper,"conveyor-route",20);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CrystalnexusModBlocks.CONVEYER_BELT.get(),20));
        var start=helper.absolutePos(new BlockPos(1,3,1));var end=start.offset(3,1,3);
        var route=net.crystalnexus.block.ConveyorGeometry.plan(start,end,true);
        for(var segment:route) {
            helper.getLevel().setBlockAndUpdate(segment.pos(),Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(segment.pos().above(),Blocks.AIR.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(start.below(),Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(end.below(),Blocks.STONE.defaultBlockState());
        player.setYRot(-90);
        use(player,start.below(),Direction.UP);
        helper.assertTrue(!ConveyorPlacement.preview(player.getMainHandItem()).isEmpty(),"First click must create a preview");
        var obstruction=route.get(2).pos();helper.getLevel().setBlockAndUpdate(obstruction,Blocks.OBSIDIAN.defaultBlockState());
        use(player,end.below(),Direction.UP);
        helper.assertTrue(helper.getLevel().isEmptyBlock(start) && player.getOffhandItem().getCount()==20,"Blocked routes must not place or consume anything");
        helper.getLevel().setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
        use(player,end.below(),Direction.UP);
        for(var segment:route) {
            var state=helper.getLevel().getBlockState(segment.pos());
            helper.assertTrue(net.crystalnexus.block.ConveyorGeometry.isBelt(state),"Expected belt at "+segment.pos());
            helper.assertTrue(state.getValue(net.crystalnexus.block.ConveyerBeltBlock.CURVE)==segment.curve()
                && state.getValue(net.crystalnexus.block.ConveyorGeometry.SLOPE)==segment.slope(),"Placed shape must match preview");
        }
        helper.assertTrue(player.getOffhandItem().getCount()==20-route.size(),"Only new belts must be consumed");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void snappingPreservesEndpointsAndChargesOnlyNewBelts(GameTestHelper helper) {
        var player=equippedPlayer(helper,"conveyor-snap",10);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CrystalnexusModBlocks.CONVEYER_BELT.get(),10));
        var start=helper.absolutePos(new BlockPos(1,3,1));var end=start.east(3);
        var state=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING,Direction.EAST);
        helper.getLevel().setBlockAndUpdate(start.east(),Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(start.east(2),Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(start,state);helper.getLevel().setBlockAndUpdate(end,state);
        var belt=(net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity)helper.getLevel().getBlockEntity(start);
        belt.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
        use(player,start,Direction.UP);use(player,end,Direction.UP);
        helper.assertTrue(player.getOffhandItem().getCount()==8,"Existing endpoints must not cost belts");
        helper.assertTrue(helper.getLevel().getBlockEntity(start)==belt && belt.getItem(0).getCount()==3,"Snapping must preserve endpoint inventory and identity");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void deniedPlacementRollsBackWholeRoute(GameTestHelper helper) {
        var player=equippedPlayer(helper,"conveyor-denied",10);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CrystalnexusModBlocks.CONVEYER_BELT.get(),10));
        var start=helper.absolutePos(new BlockPos(1,3,1));var end=start.east(3);
        for(int i=0;i<4;i++) helper.getLevel().setBlockAndUpdate(start.east(i),Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(start.below(),Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(end.below(),Blocks.STONE.defaultBlockState());
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> deny=event -> {
            if(event.getPos().equals(start.east(2))) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try {use(player,start.below(),Direction.UP);use(player,end.below(),Direction.UP);}
        finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
        for(int i=0;i<4;i++) helper.assertTrue(helper.getLevel().isEmptyBlock(start.east(i)),"Denied route must roll back every placed block");
        helper.assertTrue(player.getOffhandItem().getCount()==10,"Denied route must not consume belts");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void snappingUsesRampPortHeightAndRejectsWrongDirection(GameTestHelper helper) {
        var level=helper.getLevel();var player=equippedPlayer(helper,"conveyor-ramp-snap",20);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CrystalnexusModBlocks.CONVEYER_BELT.get(),20));
        var start=helper.absolutePos(new BlockPos(1,3,1));var end=start.east(3).above();
        var flat=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING,Direction.EAST);
        level.setBlockAndUpdate(start,flat.setValue(net.crystalnexus.block.ConveyorGeometry.SLOPE,net.crystalnexus.block.ConveyorGeometry.Slope.UP));
        for(int i=1;i<=3;i++) level.setBlockAndUpdate(start.east(i).above(),Blocks.AIR.defaultBlockState());
        var route=ConveyorPlacement.plan(level,start,end,true);
        helper.assertTrue(route.get(1).pos().getY()==start.getY()+1,"Ramp extension must start at its raised output");
        helper.assertTrue(ConveyorPlacement.validate(level,player,player.getOffhandItem(),route)==null,"Raised output must accept a flat extension");
        level.setBlockAndUpdate(end,flat.setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING,Direction.WEST));
        route=ConveyorPlacement.plan(level,start,end,true);
        helper.assertTrue(ConveyorPlacement.validate(level,player,player.getOffhandItem(),route)!=null,"Opposing endpoint must be rejected");
        helper.succeed();
    }

    private static ServerPlayer equippedPlayer(GameTestHelper helper, String name, int cableCount) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
            new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.CRYSTAL_WRENCH.get()));
        player.setItemInHand(InteractionHand.OFF_HAND,
            new ItemStack(CrystalnexusModItems.BASIC_ENERGY_CABLE.get(), cableCount));
        return player;
    }

    private static void use(ServerPlayer player, BlockPos clicked, Direction face) {
        Vec3 location = Vec3.atCenterOf(clicked).add(face.getStepX() * 0.5,
            face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(location, face, clicked, false);
        ItemStack wrench = player.getMainHandItem();
        wrench.getItem().onItemUseFirst(wrench,
            new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, wrench, hit));
    }

}
