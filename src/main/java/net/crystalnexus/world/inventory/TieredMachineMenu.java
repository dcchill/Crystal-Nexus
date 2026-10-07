package net.crystalnexus.world.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class TieredMachineMenu extends AbstractContainerMenu {
    protected TieredMachineMenu(MenuType<?> type, int id) { super(type, id); }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof MachineItemSlot slot) {
            ItemStack stored = slot.getItem();
            if (stored.getCount() > stored.getMaxStackSize()) {
                // Swapping a whole machine stack would put an oversized stack on the cursor or hotbar.
                if (type == ClickType.SWAP) {
                    if ((button >= 0 && button < 9 || button == 40) && player.getInventory().getItem(button).isEmpty()
                            && slot.mayPickup(player)) {
                        ItemStack taken = slot.remove(stored.getMaxStackSize());
                        player.getInventory().setItem(button, taken);
                        slot.onTake(player, taken);
                    }
                    return;
                }
                if (type == ClickType.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameComponents(stored, getCarried())) return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    @Override protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        // Merge first, then fill empty slots. Each destination owns its capacity.
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = reverse ? end - 1 : start; !stack.isEmpty() && (reverse ? i >= start : i < end); i += reverse ? -1 : 1) {
                Slot slot = slots.get(i);
                ItemStack current = slot.getItem();
                if (!slot.mayPlace(stack) || (pass == 0 ? current.isEmpty() : !current.isEmpty())) continue;
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) continue;
                int amount = Math.min(stack.getCount(), Math.max(0, slot.getMaxStackSize(stack) - current.getCount()));
                if (amount == 0) continue;
                slot.setByPlayer(stack.copyWithCount(current.getCount() + amount));
                stack.shrink(amount);
                slot.setChanged();
                moved = true;
            }
        }
        return moved;
    }
}
