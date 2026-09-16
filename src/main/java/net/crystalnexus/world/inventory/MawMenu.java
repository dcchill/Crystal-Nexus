package net.crystalnexus.world.inventory;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class MawMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public MawMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        super(CrystalnexusModMenus.MAW.get(), id);
        BlockPos pos = data.readBlockPos();
        access = ContainerLevelAccess.create(inventory.player.level(), pos);
        Container container = inventory.player.level().getBlockEntity(pos) instanceof Container c ? c : new SimpleContainer(3);
        for (int i = 0; i < 3; i++) addSlot(new Slot(container, i, 62 + i * 18, 33) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, CrystalnexusModBlocks.MAW.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < 30) {
            if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
