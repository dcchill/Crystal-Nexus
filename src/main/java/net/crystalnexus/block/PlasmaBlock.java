package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.crystalnexus.block.entity.PlasmaBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlasmaBlock extends InvertiumCrystalBlockBlock implements EntityBlock {
    public static final BooleanProperty CONTAINED = BooleanProperty.create("contained");
    public static final BooleanProperty SETTLED = BooleanProperty.create("settled");
    public static final int FLOW_INTERVAL = 4;
    public static final int SEARCH_LIMIT = 4096;

    public PlasmaBlock() {
        registerDefaultState(stateDefinition.any().setValue(CONTAINED, false).setValue(SETTLED, false));
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlasmaBlockEntity(pos, state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONTAINED, SETTLED);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !state.getValue(CONTAINED) && !state.getValue(SETTLED))
            level.scheduleTick(pos, this, FLOW_INTERVAL);
    }

    public static void release(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PlasmaBlock plasma) {
            level.setBlockAndUpdate(pos, state.setValue(CONTAINED, false).setValue(SETTLED, false));
            if (level.getBlockEntity(pos) instanceof PlasmaBlockEntity data) data.setRoute(List.of());
            level.scheduleTick(pos, plasma, FLOW_INTERVAL);
        }
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getBlockState(pos).isAir();
    }

    /** Plan a shortest route to higher air, allowing downward detours through breaches. */
    private static List<BlockPos> riseRoute(ServerLevel level, BlockPos origin) {
        if (open(level, origin.above())) return List.of(origin.above());
        var queue = new ArrayDeque<BlockPos>();
        var parents = new HashMap<BlockPos, BlockPos>();
        queue.add(origin);
        parents.put(origin, origin);
        // ponytail: at most 4096 loaded cells per search; use incremental searches for larger detours.
        while (!queue.isEmpty() && parents.size() < SEARCH_LIMIT) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (parents.containsKey(next) || !open(level, next)) continue;
                if (parents.size() >= SEARCH_LIMIT) break;
                parents.put(next, current);
                if (next.getY() > origin.getY()) {
                    var route = new ArrayList<BlockPos>();
                    for (BlockPos step = next; !step.equals(origin); step = parents.get(step)) route.add(step);
                    Collections.reverse(route);
                    return route;
                }
                queue.addLast(next);
            }
        }
        return List.of();
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(CONTAINED)) return;
        if (pos.getY() >= level.getMaxBuildHeight() - 1) {
            level.removeBlock(pos, false);
            return;
        }
        if (state.getValue(SETTLED)
            || !(level.getBlockEntity(pos) instanceof PlasmaBlockEntity data)) return;
        List<BlockPos> route = data.getRoute();
        if (route.isEmpty() || pos.distManhattan(route.getFirst()) != 1 || !open(level, route.getFirst()))
            route = riseRoute(level, pos);
        if (route.isEmpty()) {
            data.setRoute(List.of());
            level.setBlockAndUpdate(pos, state.setValue(SETTLED, true));
            return;
        }
        BlockPos destination = route.getFirst();
        if (destination.getY() >= level.getMaxBuildHeight() - 1) {
            level.removeBlock(pos, false);
            return;
        }
        if (level.setBlockAndUpdate(destination, state)) {
            if (level.getBlockEntity(destination) instanceof PlasmaBlockEntity next)
                next.setRoute(route.subList(1, route.size()));
            level.removeBlock(pos, false);
        }
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
