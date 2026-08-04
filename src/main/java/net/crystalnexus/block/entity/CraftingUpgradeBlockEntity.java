package net.crystalnexus.block.entity;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingUpgradeBlockEntity extends BlockEntity {
    private int ticks;

    public CraftingUpgradeBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.CRAFTING_UPGRADE.get(), pos, state);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, CraftingUpgradeBlockEntity upgrade) {
        if (++upgrade.ticks % 20 != 0) return;
        boolean connected = DepotNetwork.isCraftingUpgradeConnected(level, pos);
        if (state.getValue(CraftingUpgradeBlock.CONNECTED) != connected) {
            level.setBlock(pos, state.setValue(CraftingUpgradeBlock.CONNECTED, connected), 3);
        }
    }
}
