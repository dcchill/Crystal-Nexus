package net.crystalnexus.util;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class DepotNetwork {
    private static final int MAX_CABLES = 4096;

    private DepotNetwork() {
    }

    public static boolean hasCraftingUpgrade(ServerPlayer player) {
        DepotControllerBlockEntity controller = DepotSavedData.getController(player.serverLevel(), player.getUUID());
        if (controller == null || !controller.isPowered() || !(controller.getLevel() instanceof ServerLevel level)) return false;
        return scan(level, controller.getBlockPos(),
                pos -> level.getBlockState(pos).getBlock() instanceof CraftingUpgradeBlock);
    }

    public static boolean isCraftingUpgradeConnected(ServerLevel level, BlockPos upgradePos) {
        return scan(level, upgradePos, pos -> {
            if (!(level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller)
                    || controller.getOwner() == null || !controller.isPowered()) return false;
            return DepotSavedData.get(level, controller.getOwner()).isController(level, pos);
        });
    }

    private static boolean scan(ServerLevel level, BlockPos start, Predicate<BlockPos> target) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos cable = start.relative(direction);
            if (level.hasChunkAt(cable) && level.getBlockState(cable).getBlock() instanceof DepotCableBlock) {
                open.add(cable);
                visited.add(cable);
            }
        }

        // ponytail: bounded loaded-chunk scan; raise MAX_CABLES only if legitimate networks exceed it.
        while (!open.isEmpty() && visited.size() <= MAX_CABLES) {
            BlockPos pos = open.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                if (target.test(next)) return true;
                if (level.getBlockState(next).getBlock() instanceof DepotCableBlock && visited.add(next)) open.addLast(next);
            }
        }
        return false;
    }
}
