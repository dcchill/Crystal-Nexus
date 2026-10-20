package net.crystalnexus.gametest;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.ConveyerBeltBlock;
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
}
