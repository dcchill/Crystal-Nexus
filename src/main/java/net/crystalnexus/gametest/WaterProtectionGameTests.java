package net.crystalnexus.gametest;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_water")
@PrefixGameTestTemplate(false)
public final class WaterProtectionGameTests {
    @GameTest(template = "zero_point")
    public static void everyModBlockStateRejectsDestructiveWater(GameTestHelper helper) throws Exception {
        // Exercise the actual fluid routing check, including blocks with no collision.
        var canHold = FlowingFluid.class.getDeclaredMethod("canHoldFluid", BlockGetter.class, BlockPos.class, BlockState.class, Fluid.class);
        canHold.setAccessible(true);
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(CrystalnexusMod.MODID) || block instanceof LiquidBlock) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                for (Fluid water : new Fluid[]{Fluids.WATER, Fluids.FLOWING_WATER}) {
                    helper.assertTrue(!state.canBeReplaced(water), "Water must not replace " + id + ": " + state);
                    if (!(block instanceof LiquidBlockContainer))
                        helper.assertTrue(!(boolean) canHold.invoke(Fluids.WATER, helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), state, water), "Flowing water must not destroy " + id + ": " + state);
                }
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "Must check registered mod block states");
        helper.assertTrue(Blocks.TORCH.defaultBlockState().canBeReplaced(Fluids.WATER)
                && (boolean) canHold.invoke(Fluids.WATER, helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), Blocks.TORCH.defaultBlockState(), Fluids.WATER), "Vanilla water replacement must still work");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void flowingAndFallingWaterPreserveModBlocks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 3, 3);
        for (Block block : new Block[]{CrystalnexusModBlocks.BASIC_ENERGY_CABLE.get(), CrystalnexusModBlocks.ENERGY_CABLE_MK_2.get(), CrystalnexusModBlocks.HYPER_ENERGY_CABLE.get(), CrystalnexusModBlocks.ZERO_CABLE.get(), CrystalnexusModBlocks.PIPE_STRAIGHT.get(), CrystalnexusModBlocks.DEPOT_CABLE.get()}) {
            helper.setBlock(pos, block);
            helper.setBlock(pos.above(), Blocks.WATER);
            BlockPos source = helper.absolutePos(pos.above());
            Fluids.WATER.tick(helper.getLevel(), source, helper.getLevel().getFluidState(source));
            helper.assertTrue(helper.getBlockState(pos).is(block), "Falling water must preserve " + block);
            helper.setBlock(pos.above(), Blocks.AIR);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos.west().below(), Blocks.STONE);
            helper.setBlock(pos.west(), Blocks.WATER);
            source = helper.absolutePos(pos.west());
            Fluids.WATER.tick(helper.getLevel(), source, helper.getLevel().getFluidState(source));
            helper.assertTrue(helper.getBlockState(pos).is(block), "Sideways water must preserve " + block);
            helper.setBlock(pos.west(), Blocks.AIR);
        }
        helper.setBlock(pos, CrystalnexusModBlocks.NODE_EXTRACTOR.get());
        var state = helper.getBlockState(pos);
        var container = (LiquidBlockContainer) state.getBlock();
        helper.assertTrue(container.placeLiquid(helper.getLevel(), helper.absolutePos(pos), state, Fluids.WATER.defaultFluidState()), "Waterlogging must remain available");
        helper.assertTrue(helper.getBlockState(pos).is(CrystalnexusModBlocks.NODE_EXTRACTOR.get()) && helper.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED), "Waterlogging must preserve the block");
        helper.succeed();
    }
}
