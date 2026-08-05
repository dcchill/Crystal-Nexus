package net.crystalnexus.world.inventory;

import net.crystalnexus.block.DepotDownloaderBlock;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

public class DepotMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 54;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 140;

    private final SimpleContainer depotView = new SimpleContainer(PAGE_SIZE);
    private final ResourceLocation[] depotItemIds = new ResourceLocation[PAGE_SIZE];
    private final BlockPos sourcePos;
    private final boolean uplink;
    private String search = "";
    private int page;

    public DepotMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos(), extraData.readBoolean());
        extraData.readBoolean(); // Legacy crafting-grid flag; crafting is CLI-only now.
    }

    public DepotMenu(int id, Inventory inv) {
        this(id, inv, null, false);
    }

    public DepotMenu(int id, Inventory inv, boolean uplink, boolean ignoredCraftingGrid) {
        this(id, inv, null, uplink);
    }

    private DepotMenu(int id, Inventory inv, BlockPos sourcePos, boolean uplink) {
        super(CrystalnexusModMenus.DEPOT.get(), id);
        this.sourcePos = sourcePos;
        this.uplink = uplink;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                addSlot(new Slot(depotView, index, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public boolean isDepotSlot(Slot slot) {
        return slot.container == depotView;
    }

    private boolean isDepotSlotId(int slotId) {
        return slotId >= 0 && slotId < PAGE_SIZE;
    }

    public void setDepotPage(String search, int page, List<DepotSavedData.Entry> entries) {
        this.search = search == null ? "" : search;
        this.page = Math.max(0, page);
        depotView.clearContent();
        Arrays.fill(depotItemIds, null);

        for (int i = 0; i < Math.min(entries.size(), PAGE_SIZE); i++) {
            DepotSavedData.Entry entry = entries.get(i);
            Item item = BuiltInRegistries.ITEM.get(entry.itemId());
            if (item == null || item == Items.AIR) continue;
            depotItemIds[i] = entry.itemId();
            depotView.setItem(i, new ItemStack(item,
                    (int) Math.min(entry.count(), item.getDefaultInstance().getMaxStackSize())));
        }
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!isDepotSlotId(slotId)) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!canAccessDepot(serverPlayer)) {
            serverPlayer.closeContainer();
            return;
        }

        DepotSavedData depot = DepotSavedData.get(serverPlayer);
        if (clickType == ClickType.QUICK_MOVE) {
            withdrawStack(serverPlayer, depot, depotItemIds[slotId]);
        } else if (clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
            pickupOrDeposit(depot, depotItemIds[slotId], button);
        } else if (clickType == ClickType.PICKUP_ALL) {
            fillCarriedStack(depot, depotItemIds[slotId]);
        }
        refreshDepot(depot);
    }

    private void pickupOrDeposit(DepotSavedData depot, ResourceLocation itemId, int button) {
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            ResourceLocation carriedId = BuiltInRegistries.ITEM.getKey(carried.getItem());
            int amount = button == 1 ? 1 : carried.getCount();
            carried.shrink((int) depot.deposit(carriedId, amount));
            setCarried(carried);
            return;
        }
        if (itemId == null) return;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return;
        int available = (int) Math.min(depot.getCount(itemId), item.getDefaultInstance().getMaxStackSize());
        int amount = button == 1 ? (available + 1) / 2 : available;
        long removed = depot.remove(itemId, amount);
        if (removed > 0) setCarried(new ItemStack(item, (int) removed));
    }

    private void fillCarriedStack(DepotSavedData depot, ResourceLocation itemId) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || itemId == null
                || !itemId.equals(BuiltInRegistries.ITEM.getKey(carried.getItem()))) return;
        int amount = Math.min(carried.getMaxStackSize() - carried.getCount(),
                (int) Math.min(Integer.MAX_VALUE, depot.getCount(itemId)));
        carried.grow((int) depot.remove(itemId, amount));
        setCarried(carried);
    }

    private static ItemStack withdrawStack(ServerPlayer player, DepotSavedData depot, ResourceLocation itemId) {
        if (itemId == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        int amount = (int) Math.min(depot.getCount(itemId), item.getDefaultInstance().getMaxStackSize());
        if (amount <= 0) return ItemStack.EMPTY;
        depot.remove(itemId, amount);
        ItemStack stack = new ItemStack(item, amount);
        ItemStack original = stack.copy();
        player.getInventory().add(stack);
        if (!stack.isEmpty()) depot.add(itemId, stack.getCount());
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return !(player instanceof ServerPlayer serverPlayer) || canAccessDepot(serverPlayer);
    }

    public boolean canAccessDepot(ServerPlayer player) {
        if (!DepotSavedData.hasPoweredController(player)) return false;
        if (uplink) return true;
        return sourcePos != null
                && player.serverLevel().getBlockState(sourcePos).getBlock() instanceof DepotDownloaderBlock
                && DepotNetwork.isComponentConnected(player.serverLevel(), sourcePos, player.getUUID());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!(player instanceof ServerPlayer serverPlayer) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (!canAccessDepot(serverPlayer)) {
            serverPlayer.closeContainer();
            return ItemStack.EMPTY;
        }
        DepotSavedData depot = DepotSavedData.get(serverPlayer);
        if (isDepotSlotId(index)) {
            ItemStack moved = withdrawStack(serverPlayer, depot, depotItemIds[index]);
            refreshDepot(depot);
            return moved;
        }

        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (depot.tryDepositAll(stack) <= 0) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        refreshDepot(depot);
        return original;
    }

    private void refreshDepot(DepotSavedData depot) {
        setDepotPage(search, page, depot.page(search, page, PAGE_SIZE));
    }
}
