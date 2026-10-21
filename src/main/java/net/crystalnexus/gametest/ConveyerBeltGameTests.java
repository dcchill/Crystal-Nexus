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

    @GameTest(template = "zero_point", timeoutTicks = 100)
    public static void entitiesEnterAndClimbRampFromFlatBelt(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 3, 1));
        var passengers = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        var flat = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        ConveyorPlacement.placing = true;
        try {
            for (int track = 0; track < 2; track++) {
                var base = start.south(track * 3);
                var positions = new java.util.ArrayList<BlockPos>();
                for (int i = 0; i < 8; i++) {
                    var pos = base.east(i).above(i >= 2 ? 1 : 0);
                    positions.add(pos);
                    for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(pos.above(dy), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pos, flat.setValue(ConveyorGeometry.SLOPE,
                        i == 1 ? ConveyorGeometry.Slope.UP : ConveyorGeometry.Slope.FLAT));
                }
                for (int i = 0; i < positions.size(); i++) {
                    var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(positions.get(i));
                    belt.setSplineConnections(i == 0 ? null : positions.get(i - 1),
                        i + 1 == positions.size() ? null : positions.get(i + 1));
                }
                net.minecraft.world.entity.Entity entity;
                if (track == 0) {
                    var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                    pig.setNoAi(true);
                    entity = pig;
                } else {
                    entity = new net.minecraft.world.entity.decoration.ArmorStand(level, 0, 0, 0);
                }
                entity.setPos(base.getX() + .5, base.getY() + .5, base.getZ() + .5);
                level.addFreshEntity(entity);
                passengers.add(entity);
            }
        } finally { ConveyorPlacement.placing = false; }
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var rampPos = start.east();
        var ramp = level.getBlockState(rampPos);
        player.setPos(rampPos.getX() + .5, rampPos.getY() + 1, rampPos.getZ() + .5);
        double support = ConveyorGeometry.supportHeight(ramp, player.getBoundingBox().move(-rampPos.getX(), -rampPos.getY(), -rampPos.getZ()));
        player.setPos(player.getX(), rampPos.getY() + support, player.getZ());
        level.addFreshEntity(player);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(rampPos)).serverTick();
        helper.assertTrue(player.getDeltaMovement().x > 0 && player.getDeltaMovement().y > .08,
            "Uphill player velocity must lift against gravity");
        player.discard();
        helper.runAfterDelay(45, () -> {
            for (var entity : passengers) {
                helper.assertTrue(entity.getX() > start.getX() + 2.3 && entity.getY() >= start.getY() + 1.45,
                    "Entity must travel from flat belt to the raised landing: " + entity.getType() + " at " + entity.position());
                entity.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void nonItemEntitiesFollowEveryBeltSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) for (var slope : ConveyorGeometry.Slope.values())
                    for (var curve : ConveyerBeltBlock.CurveShape.values()) {
                if (slope != ConveyorGeometry.Slope.FLAT && curve != ConveyerBeltBlock.CurveShape.STRAIGHT) continue;
                var state = block.defaultBlockState().setValue(ConveyerBeltBlock.FACING, facing)
                    .setValue(ConveyorGeometry.SLOPE, slope).setValue(ConveyerBeltBlock.CURVE, curve);
                level.setBlockAndUpdate(pos, state);
                var surface = ConveyorGeometry.point(state, .5).add(pos.getX(), pos.getY(), pos.getZ());
                var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, surface.x, surface.y, surface.z);
                double support = ConveyorGeometry.supportHeight(state, stand.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()));
                stand.setPos(surface.x, pos.getY() + support, surface.z);
                double startY = stand.getY();
                stand.setNoGravity(true);
                level.addFreshEntity(stand);
                var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
                belt.serverTick();
                double t = ConveyorGeometry.progress(state, stand.getX() - pos.getX(), stand.getZ() - pos.getZ());
                helper.assertTrue(t > .5, "Non-item entities must move along " + block + " " + facing + " " + slope + " " + curve);
                if (slope == ConveyorGeometry.Slope.UP) helper.assertTrue(stand.getY() > startY, "Entities must climb ramps");
                if (slope == ConveyorGeometry.Slope.DOWN) helper.assertTrue(stand.getY() < startY, "Entities must descend ramps");
                var moved = stand.position();
                belt.serverTick();
                helper.assertTrue(stand.position().equals(moved), "Overlapping belt ticks must not move an entity twice");
                stand.discard();
            }
        }
        var flat = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, flat);
        var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 3, 2));
        pig.setNoAi(true);
        pig.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos)).serverTick();
        helper.assertTrue(pig.getX() > pos.getX() + .5, "Conveyors must also carry living mobs");
        pig.discard();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        level.addFreshEntity(player);
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().x > 0 && player.hurtMarked,
            "Player belt velocity must be synchronized to the client");
        player.getPersistentData().remove("crystalnexusConveyorMoveTick");
        player.setDeltaMovement(0, .42, 0);
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().y == .42 && player.getDeltaMovement().x == 0,
            "Jumping players must be able to leave the conveyor");
        player.setDeltaMovement(0, 0, 0);
        player.getAbilities().flying = true;
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0, "Flying players must not be pulled onto conveyors");
        player.discard();
        var ramp = flat.setValue(ConveyorGeometry.SLOPE, ConveyorGeometry.Slope.UP);
        level.setBlockAndUpdate(pos, ramp);
        level.setBlockAndUpdate(pos.east().above(), ramp);
        var crossing = new net.minecraft.world.entity.decoration.ArmorStand(level, pos.getX() + .95, pos.getY() + 1, pos.getZ() + .5);
        var bounds = crossing.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ());
        double support = Math.max(ConveyorGeometry.supportHeight(ramp, bounds),
            1 + ConveyorGeometry.supportHeight(ramp, bounds.move(-1, -1, 0)));
        crossing.setPos(crossing.getX(), pos.getY() + support, crossing.getZ());
        level.addFreshEntity(crossing);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos)).serverTick();
        helper.assertTrue(crossing.getX() > pos.getX() + 1 && crossing.getY() > pos.getY() + support,
            "Wide entities must cross smoothly between consecutive ramps");
        crossing.discard();
        helper.succeed();
    }

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
                for (double t : new double[]{0, .25, .5, .75, 1}) {
                    var point = ConveyorGeometry.point(state, t);
                    helper.assertTrue(Math.abs(t - ConveyorGeometry.progress(state, point.x, point.z)) < 1e-8,
                        "Entity progress must match the belt surface in every direction");
                }
            }
        }
        helper.succeed();
    }

}
