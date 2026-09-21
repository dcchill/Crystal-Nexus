package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.GasolineGeneratorControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public final class GasolineGeneratorMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final GasolineGeneratorControllerBlockEntity controller;

    public GasolineGeneratorMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, controllerAt(inventory, data.readBlockPos())); }
    public GasolineGeneratorMenu(int id, Inventory inventory, GasolineGeneratorControllerBlockEntity controller) {
        super(CrystalnexusModMenus.DIESEL_GENERATOR.get(), id);
        this.controller = controller;
        access = ContainerLevelAccess.create(inventory.player.level(), controller == null ? BlockPos.ZERO : controller.getBlockPos());
    }
    private static GasolineGeneratorControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof GasolineGeneratorControllerBlockEntity controller ? controller : null;
    }
    public GasolineGeneratorControllerBlockEntity controller() { return controller; }
    @Override public boolean stillValid(Player player) { return controller != null && stillValid(access, player, CrystalnexusModBlocks.DIESEL_GENERATOR_CONTROLLER.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
