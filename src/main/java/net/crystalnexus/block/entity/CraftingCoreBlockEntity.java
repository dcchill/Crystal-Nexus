package net.crystalnexus.block.entity;

import net.crystalnexus.block.CraftingCoreBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Tracks whether this core belongs to a connected horizontal 1x1 through 2x2 core cluster. */
public class CraftingCoreBlockEntity extends BlockEntity {
    private int ticks;
    private boolean refreshSoon;

    public CraftingCoreBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.CRAFTING_CORE.get(), pos, state);
    }

    public void refreshSoon() {
        refreshSoon = true;
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, CraftingCoreBlockEntity core) {
        if (!core.refreshSoon && ++core.ticks % 20 != 0) return;
        core.refreshSoon = false;
        UUID owner = DepotNetwork.componentOwner(level, pos);
        boolean connected = owner != null;
        // The illuminated texture represents a valid core that is receiving
        // power from its Depot network, not whether a job happens this tick.
        boolean active = connected && DepotNetwork.craftingCoreSize(level, pos) > 0;
        if (state.getValue(CraftingCoreBlock.CONNECTED) != connected
            || state.getValue(CraftingCoreBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(CraftingCoreBlock.CONNECTED, connected)
                .setValue(CraftingCoreBlock.ACTIVE, active), 3);
        }
    }

    public static boolean isCore(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(CrystalnexusModBlocks.CRAFTING_CORE.get());
    }
}
