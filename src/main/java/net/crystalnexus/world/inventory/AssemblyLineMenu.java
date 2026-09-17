package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.AssemblyLineControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Menu anchor for the full-screen graph editor. Item movement stays at the physical ports. */
public final class AssemblyLineMenu extends AbstractContainerMenu {
    public final AssemblyLineControllerBlockEntity controller;
    public final Inventory playerInventory;
    private final ContainerLevelAccess access;

    public AssemblyLineMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, find(inventory, data.readBlockPos()));
    }

    public AssemblyLineMenu(int id, Inventory inventory, AssemblyLineControllerBlockEntity controller) {
        super(CrystalnexusModMenus.ASSEMBLY_LINE.get(), id);
        this.controller = controller;
        this.playerInventory = inventory;
        this.access = controller == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(inventory.player.level(), controller.getBlockPos());
    }

    private static AssemblyLineControllerBlockEntity find(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller ? controller : null;
    }

    @Override public boolean stillValid(Player player) {
        return controller != null && AbstractContainerMenu.stillValid(access, player, controller.getBlockState().getBlock());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}