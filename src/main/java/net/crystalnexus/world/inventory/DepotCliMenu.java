package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.DepotCliBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.cli.DepotCliParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DepotCliMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private long lastCommandTick = Long.MIN_VALUE;

    public DepotCliMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public DepotCliMenu(int id, Inventory inventory, BlockPos blockPos) {
        super(CrystalnexusModMenus.DEPOT_CLI.get(), id);
        this.blockPos = blockPos.immutable();
        // Add a hidden slot at an off-screen position so JEI/EMI detects this
        // container as valid and shows the recipe/item side panel.
        addSlot(new Slot(inventory, 0, -9999, -9999));
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public DepotCliBlockEntity getTerminal(ServerPlayer player) {
        return player.serverLevel().getBlockEntity(blockPos) instanceof DepotCliBlockEntity cli ? cli : null;
    }

    public boolean hasPermission(ServerPlayer player) {
        DepotCliBlockEntity cli = getTerminal(player);
        return cli != null && cli.canUse(player);
    }

    public boolean isConnected(ServerPlayer player) {
        DepotCliBlockEntity cli = getTerminal(player);
        return cli != null && cli.canUse(player) && cli.isConnected(player.serverLevel());
    }

    public boolean allowCommand(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (!DepotCliParser.mayExecute(lastCommandTick, now)) return false;
        lastCommandTick = now;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return true;
        return level.getBlockEntity(blockPos) instanceof DepotCliBlockEntity cli
                && cli.canUse(player)
                && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
