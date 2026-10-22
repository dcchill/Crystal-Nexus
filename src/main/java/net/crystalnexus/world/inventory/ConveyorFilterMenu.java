package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ConveyorFilterMenu extends AbstractContainerMenu {
    private static final String FILTER_TAG = "ConveyorFilterTag";
    private int blacklist;
    private final Player owner;
    private final BlockPos pos;
    private final SimpleContainer filters = new SimpleContainer(ConveyerBeltBaseBlockEntity.FILTER_SLOTS);

    public ConveyorFilterMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public ConveyorFilterMenu(int id, Inventory inventory, BlockPos pos) {
        super(CrystalnexusModMenus.CONVEYOR_FILTER.get(), id);
        this.owner = inventory.player;
        this.pos = pos.immutable();
        for (int i = 0; i < filters.getContainerSize(); i++) {
            addSlot(new Slot(filters, i, 26 + i * 18, 54) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18, 108 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 26 + col * 18, 166));
        refreshFilters();
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return blacklist; }
            @Override public void set(int value) { blacklist = value; }
        });
    }

    public boolean isBlacklist() { return blacklist != 0; }

    public BlockPos conveyorPos() { return pos; }

    private ConveyerBeltBaseBlockEntity belt() {
        return owner.level().hasChunkAt(pos) && owner.level().getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt
            ? belt : null;
    }

    private void refreshFilters() {
        var belt = belt();
        if (belt == null) return;
        blacklist = belt.isFilterBlacklist() ? 1 : 0;
        for (int i = 0; i < filters.getContainerSize(); i++) {
            ItemStack display = belt.getFilter(i);
            var tag = belt.getFilterTag(i);
            if (!display.isEmpty() && tag != null) {
                var data = new net.minecraft.nbt.CompoundTag();
                data.putString(FILTER_TAG, tag.toString());
                display.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(data));
            }
            filters.setItem(i, display);
        }
    }

    public net.minecraft.resources.ResourceLocation getFilterTag(int slot) {
        if (slot < 0 || slot >= filters.getContainerSize()) return null;
        var data = filters.getItem(slot).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        String tagId = data == null ? "" : data.copyTag().getString(FILTER_TAG);
        return tagId.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(tagId);
    }

    public boolean selectTag(Player player, int menuId, int slot, net.minecraft.resources.ResourceLocation tag) {
        if (player.level().isClientSide || player.containerMenu != this || containerId != menuId || !stillValid(player)) return false;
        if (!belt().setFilterTag(slot, tag)) return false;
        broadcastChanges();
        return true;
    }

    @Override public void broadcastChanges() {
        if (!owner.level().isClientSide) refreshFilters();
        super.broadcastChanges();
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || slot >= slots.size() || slot < 0 && slot != -999) return;
        if (slot >= 0 && slot < filters.getContainerSize()) {
            if (!player.level().isClientSide && type == ClickType.PICKUP && (button == 0 || button == 1)) {
                belt().setFilter(slot, button == 1 ? ItemStack.EMPTY : getCarried());
                broadcastChanges();
            }
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || player.level().isClientSide || !stillValid(player)) return false;
        var belt = belt();
        belt.setFilterBlacklist(!belt.isFilterBlacklist());
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        return player == owner && belt() != null && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
