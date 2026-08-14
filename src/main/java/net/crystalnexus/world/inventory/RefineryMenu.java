package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class RefineryMenu extends AbstractContainerMenu {
    public final Player entity;
    public final int x, y, z;
    private final ContainerLevelAccess access;
    private final RefineryBlockEntity refinery;

    public RefineryMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        super(CrystalnexusModMenus.REFINERY_GUI.get(), id);
        entity = inventory.player;
        BlockPos pos = data.readBlockPos();
        x = pos.getX(); y = pos.getY(); z = pos.getZ();
        access = ContainerLevelAccess.create(entity.level(), pos);
        refinery = entity.level().getBlockEntity(pos) instanceof RefineryBlockEntity be ? be : null;
        InvWrapper items = new InvWrapper(refinery == null ? new SimpleContainer(3) : refinery);
        addSlot(new SlotItemHandler(items, 0, 52, 64));
        addSlot(new SlotItemHandler(items, 1, 115, 64) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        addSlot(new SlotItemHandler(items, 2, 180, 8) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    public RefineryBlockEntity refinery() { return refinery; }
    @Override public boolean stillValid(Player player) {
        return refinery != null && AbstractContainerMenu.stillValid(access, player, refinery.getBlockState().getBlock());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(), copy = original.copy();
        if (index < 3) {
            if (!moveItemStackTo(original, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 3, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }
}
