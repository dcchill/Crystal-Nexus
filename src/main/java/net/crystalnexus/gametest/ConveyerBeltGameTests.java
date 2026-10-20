package net.crystalnexus.gametest;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.ConveyerBeltBlock;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltMode;
import net.crystalnexus.util.ConveyorPlacement;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.procedures.ConveyerBeltNeighbourBlockChangesProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_conveyors")
@PrefixGameTestTemplate(false)
public final class ConveyerBeltGameTests {
    private ConveyerBeltGameTests() {}

    @GameTest(template = "zero_point")
    public static void collisionShapesAreReused(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                if (block instanceof ConveyerBeltBlock) state = state.setValue(ConveyerBeltBlock.BLOCKSTATE, 2);
                var first = state.getShape(level, pos, CollisionContext.empty());
                long start = System.nanoTime();
                for (int query = 0; query < 200; query++) state.getShape(level, pos, CollisionContext.empty());
                CrystalnexusMod.LOGGER.info("Conveyor shape benchmark: {} {} / 200 queries: {} ms", block, facing,
                    (System.nanoTime() - start) / 1_000_000.0);
                helper.assertTrue(first == state.getShape(level, pos, CollisionContext.empty()),
                    "Repeated collision queries must reuse the prebuilt shape for " + block + " " + facing);
                if (block instanceof ConveyerBeltBlock) {
                    helper.assertTrue(first.max(Direction.Axis.Y) == 1.0, "Stacked belt supports must retain their full height");
                    var unstacked = state.setValue(ConveyerBeltBlock.BLOCKSTATE, 1);
                    helper.assertTrue(unstacked.getShape(level, pos, CollisionContext.empty()).max(Direction.Axis.Y) == 0.5,
                        "An uncovered belt must retain its low profile");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void breakingStackedBeltsPreservesNeighborsAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get()}) {
            for (int repeat = 0; repeat < 8; repeat++) {
                for (int height = 0; height < 6; height++) level.setBlockAndUpdate(base.above(height), block.defaultBlockState());
                BlockPos target = base.above(3);
                var broken = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(target);
                for (int slot = 0; slot < 4; slot++) broken.setItem(slot, new ItemStack(Items.DIAMOND, 16));
                var below = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(target.below());
                below.setItem(0, new ItemStack(Items.EMERALD, 3));
                helper.assertTrue(level.getBlockState(target.below()).getValue(ConveyerBeltBlock.BLOCKSTATE) == 2,
                    "The lower belt must have stacked supports before breaking");
                level.destroyBlock(target, false);
                helper.assertTrue(level.getBlockState(target).isAir()
                        && level.getBlockEntity(target.below()) == below && below.getItem(0).getCount() == 3,
                    "Breaking one belt must leave neighboring belts and their contents intact");
                helper.assertTrue(level.getBlockState(target.below()).getValue(ConveyerBeltBlock.BLOCKSTATE) == 1,
                    "Breaking the upper belt must remove the lower belt's supports");
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8));
                int diamonds = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
                helper.assertTrue(diamonds == 64, "Broken belt contents must drop exactly once");
                drops.forEach(ItemEntity::discard);
                for (int height = 0; height < 6; height++) level.removeBlock(base.above(height), false);
                level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8)).forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void coverUpdatesOnlyAffectBelts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().defaultBlockState());
        var original = level.getBlockState(pos);
        ConveyerBeltNeighbourBlockChangesProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(level.getBlockState(pos) == original, "A stale conveyor callback must not change an unrelated machine");
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(level.getBlockState(pos).getValue(ConveyerBeltBlock.BLOCKSTATE) == 2, "Any cover must enable supports");
        level.removeBlock(pos.above(), false);
        helper.assertTrue(level.getBlockState(pos).getValue(ConveyerBeltBlock.BLOCKSTATE) == 1, "Removing the cover must remove supports");
        helper.succeed();
    }
    @GameTest(template = "zero_point", timeoutTicks = 220)
    public static void mixedTiersCarryItemsAroundCornerAndUpRamp(GameTestHelper helper) {
        var level=helper.getLevel();
        var start=helper.absolutePos(new BlockPos(1,3,1));
        var route=ConveyorGeometry.plan(start,start.offset(4,1,3),true);
        ConveyorPlacement.placing=true;
        try {
            for(int i=0;i<route.size();i++) {
                var segment=route.get(i);
                level.setBlockAndUpdate(segment.pos().above(),Blocks.AIR.defaultBlockState());
                var block=i%2==0?CrystalnexusModBlocks.CONVEYER_BELT.get():CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get();
                level.setBlockAndUpdate(segment.pos(),segment.state(block.defaultBlockState()));
            }
            for(int i=0;i<route.size();i++) {
                var belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(route.get(i).pos());
                belt.setSplineConnections(i==0?null:route.get(i-1).pos(),i+1==route.size()?null:route.get(i+1).pos());
            }
        } finally {ConveyorPlacement.placing=false;}
        var end=route.getLast();
        var chest=end.pos().relative(end.facing());
        level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        ((ConveyerBeltBaseBlockEntity)level.getBlockEntity(start)).setItem(0,new ItemStack(Items.DIAMOND,7));
        helper.runAfterDelay(180,()->{
            var inventory=(net.minecraft.world.Container)level.getBlockEntity(chest);
            int total=0;
            for(int i=0;i<inventory.getContainerSize();i++) if(inventory.getItem(i).is(Items.DIAMOND)) total+=inventory.getItem(i).getCount();
            helper.assertTrue(total==7,"Curves, ramps and tier transitions must deliver every item exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void geometryPortsSaveAndModeChangesPreserveRamp(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        var state=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING,Direction.EAST).setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.DOWN);
        level.setBlockAndUpdate(pos,state);
        var next=pos.east();level.setBlockAndUpdate(next,CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING,Direction.EAST));
        var belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
        belt.setSplineConnections(pos.west().above(),next);belt.setItem(2,new ItemStack(Items.EMERALD,9));
        var saved=belt.saveWithFullMetadata(level.registryAccess());
        belt.loadWithComponents(saved,level.registryAccess());
        helper.assertTrue(next.equals(belt.getSplineNextPos()),"Valid links must survive reload");
        for(int i=0;i<3;i++) {
            ConveyerBeltMode.cycle(level,pos);
            belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
            helper.assertTrue(belt.getItem(2).getCount()==9 && level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE)==ConveyorGeometry.Slope.DOWN,
                "Mode changes must preserve slope and contents");
        }
        level.removeBlock(next,false);
        helper.assertTrue(belt.getSplineNextPos()==null,"Removed links must be ignored");
        var flat=state.setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.FLAT);
        helper.assertTrue(!ConveyorGeometry.connects(pos,flat,pos.north(),flat),"Parallel adjacent lanes must not connect sideways");
        helper.assertTrue(!ConveyorGeometry.connects(pos,flat,pos.east(),flat.setValue(ConveyerBeltBlock.FACING,Direction.WEST)),"Opposing belts must not connect");
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 100)
    public static void downhillTransferWaitsForJamAndDoesNotLeakToParallelLane(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        var flat=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING,Direction.EAST);
        level.setBlockAndUpdate(pos,flat.setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.DOWN));
        level.setBlockAndUpdate(pos.east(),flat);
        level.setBlockAndUpdate(pos.east(2),flat.setValue(ConveyerBeltBlock.FACING,Direction.WEST));
        level.setBlockAndUpdate(pos.north(),flat);
        var source=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
        var jam=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos.east());
        for(int slot=0;slot<4;slot++) jam.setItem(slot,new ItemStack(Items.EMERALD,1));
        source.setItem(3,new ItemStack(Items.DIAMOND,5));
        helper.runAfterDelay(12,()->{
            helper.assertTrue(source.getItem(3).getCount()==5,"A jam must hold the upstream tail");
            var parallel=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos.north());
            helper.assertTrue(parallel.isEmpty(),"Parallel belt must remain empty");
            jam.clearContent();
        });
        helper.runAfterDelay(32,()->{
            helper.assertTrue(source.isEmpty() && jam.getItem(3).is(Items.DIAMOND) && jam.getItem(3).getCount()==5,
                "Downhill transfer must resume after the jam clears");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void surfacePointsMeetAndKeepUniformArcSpacing(GameTestHelper helper) {
        var base=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState();
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            for(var curve:ConveyerBeltBlock.CurveShape.values()) {
                var state=base.setValue(ConveyerBeltBlock.FACING,facing).setValue(ConveyerBeltBlock.CURVE,curve);
                var first=ConveyorGeometry.point(state,0);var last=ConveyorGeometry.point(state,1);
                var in=ConveyorGeometry.input(state);
                helper.assertTrue(first.distanceTo(new net.minecraft.world.phys.Vec3(.5+in.getStepX()*.5,.5,.5+in.getStepZ()*.5))<1e-8,
                    "Curve input must meet the adjacent block edge");
                helper.assertTrue(last.distanceTo(new net.minecraft.world.phys.Vec3(.5+facing.getStepX()*.5,.5,.5+facing.getStepZ()*.5))<1e-8,
                    "Curve output must meet the adjacent block edge");
                double spacing=first.distanceTo(ConveyorGeometry.point(state,.25));
                for(int n=1;n<4;n++) helper.assertTrue(Math.abs(spacing-ConveyorGeometry.point(state,n*.25).distanceTo(ConveyorGeometry.point(state,(n+1)*.25)))<1e-8,
                    "Each transport slot must occupy the same arc length");
            }
        }
        helper.succeed();
    }

}
