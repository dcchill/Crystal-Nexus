package net.crystalnexus.block.entity;

import net.crystalnexus.block.DepotCliBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DepotCliBlockEntity extends BlockEntity implements MenuProvider {
    private UUID owner;

    public DepotCliBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DEPOT_CLI.get(), pos, state);
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean canUse(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean isConnected(ServerLevel level) {
        return DepotNetwork.isComponentConnected(level, worldPosition, owner);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DepotCliBlockEntity cli) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 20 != 0) return;
        boolean connected = cli.isConnected(serverLevel);
        if (state.getValue(DepotCliBlock.CONNECTED) != connected) {
            level.setBlock(pos, state.setValue(DepotCliBlock.CONNECTED, connected), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crystalnexus.depot_cli");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DepotCliMenu(id, inventory, worldPosition);
    }
}
