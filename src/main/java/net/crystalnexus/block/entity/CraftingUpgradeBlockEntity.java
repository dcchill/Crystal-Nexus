package net.crystalnexus.block.entity;

import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.crystalnexus.world.inventory.CraftingProcessorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CraftingUpgradeBlockEntity extends BlockEntity implements MenuProvider {
    private int ticks;

    public CraftingUpgradeBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.CRAFTING_UPGRADE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Crafting Processor");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CraftingProcessorMenu(id, inventory, worldPosition);
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, CraftingUpgradeBlockEntity upgrade) {
        if (++upgrade.ticks % 20 != 0) return;
        java.util.UUID owner = DepotNetwork.craftingProcessorOwner(level, pos);
        boolean connected = owner != null;
        boolean active = connected && DepotSavedData.get(level, owner).getCraftingJob() != null;
        if (state.getValue(CraftingUpgradeBlock.CONNECTED) != connected
                || state.getValue(CraftingUpgradeBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(CraftingUpgradeBlock.CONNECTED, connected)
                    .setValue(CraftingUpgradeBlock.ACTIVE, active), 3);
        }
    }
}
