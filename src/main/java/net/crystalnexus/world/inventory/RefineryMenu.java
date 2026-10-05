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
    private final int machineSlots;
    public final Player entity;
    public final int x, y, z;
    private final ContainerLevelAccess access;
    private final RefineryBlockEntity refinery;

    public RefineryMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        super(CrystalnexusModMenus.REFINERY_GUI.get(), id);
        entity = inventory.player;
        BlockPos pos = data != null && data.readableBytes() >= Long.BYTES ? data.readBlockPos() : BlockPos.ZERO;
        x = pos.getX(); y = pos.getY(); z = pos.getZ();
        access = ContainerLevelAccess.create(entity.level(), pos);
        refinery = entity.level().getBlockEntity(pos) instanceof RefineryBlockEntity be ? be : null;
        InvWrapper items = new InvWrapper(refinery == null ? new SimpleContainer(8) : refinery);
        addSlot(new SlotItemHandler(items, 0, 52, 64));
        addSlot(new SlotItemHandler(items, 1, 115, 64) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        var upgradeState = entity.level().getBlockState(pos);
        addSlot(new MachineUpgradeSlot(items, 2, upgradeState, 0));
        for (int i = 1; i < net.crystalnexus.processing.MachineTier.from(upgradeState).upgradeSlots(); i++)
            addSlot(new MachineUpgradeSlot(items, 3 + i - 1, upgradeState, i));
        machineSlots = slots.size();
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
        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (net.crystalnexus.util.MachineUpgradeHelper.isMachineUpgrade(original)) {
                boolean moved = false;
                for (int i = 0; i < machineSlots && !original.isEmpty(); i++)
                    if (slots.get(i) instanceof MachineUpgradeSlot) moved |= moveItemStackTo(original, i, i + 1, false);
                if (!moved) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 2, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }
}
