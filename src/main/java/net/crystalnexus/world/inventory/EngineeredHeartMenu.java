package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.EngineeredHeartBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public final class EngineeredHeartMenu extends AbstractContainerMenu {
    private final EngineeredHeartBlockEntity heart;
    private final ContainerLevelAccess access;
    public EngineeredHeartMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, at(inventory, data.readBlockPos())); }
    public EngineeredHeartMenu(int id, Inventory inventory, EngineeredHeartBlockEntity heart) {
        super(CrystalnexusModMenus.ENGINEERED_HEART.get(), id);
        this.heart = heart;
        access = ContainerLevelAccess.create(inventory.player.level(), heart == null ? BlockPos.ZERO : heart.getBlockPos());
    }
    private static EngineeredHeartBlockEntity at(Inventory inventory, BlockPos pos) { return inventory.player.level().getBlockEntity(pos) instanceof EngineeredHeartBlockEntity heart ? heart : null; }
    public EngineeredHeartBlockEntity heart() { return heart; }
    @Override public boolean stillValid(Player player) { return heart != null && stillValid(access, player, CrystalnexusModBlocks.ENGINEERED_HEART.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
