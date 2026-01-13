package net.crystalnexus.world.inventory;

import net.crystalnexus.init.CrystalnexusModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DepotMenu extends AbstractContainerMenu {

    public static final int PAGE_SIZE = 60;

    // Used by IMenuTypeExtension.create(DepotMenu::new)
    public DepotMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv);
    }

    public DepotMenu(int id, Inventory inv) {
        super(CrystalnexusModMenus.DEPOT.get(), id);

        // ✅ Add player inventory slots OFFSCREEN so inventory sync works while GUI is open
        addPlayerInventorySlotsOffscreen(inv);
    }

    private void addPlayerInventorySlotsOffscreen(Inventory inv) {
        // Put slots far offscreen; they won't be visible but WILL sync.
        int x0 = -10000;
        int y0 = -10000;

        // Player inventory (3 rows × 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                this.addSlot(new Slot(inv, index, x0 + col * 18, y0 + row * 18));
            }
        }

        // Hotbar (1 row × 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, x0 + col * 18, y0 + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // wireless
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No meaningful shift-click behavior for the depot list (it's custom-rendered)
        return ItemStack.EMPTY;
    }
}
