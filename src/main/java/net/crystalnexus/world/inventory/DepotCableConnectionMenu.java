package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.DepotCableBlockEntity;
import net.crystalnexus.block.entity.DepotCableConnectionConfig;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DepotCableConnectionMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOTS = DepotCableConnectionConfig.FILTER_SLOTS;
    private final Inventory playerInventory;
    private final BlockPos cablePos;
    private final DepotCableBlockEntity cable;
    private final Container filters = new SimpleContainer(FILTER_SLOTS);
    private int sideOrdinal;
    private int filterMode;
    private int priority;

    public DepotCableConnectionMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, inventory.player.level().getBlockEntity(data.readBlockPos()) instanceof DepotCableBlockEntity cable
                ? cable : null);
    }

    public DepotCableConnectionMenu(int id, Inventory inventory, DepotCableBlockEntity cable) {
        super(CrystalnexusModMenus.DEPOT_CABLE_CONNECTION.get(), id);
        this.playerInventory = inventory;
        this.cable = cable;
        this.cablePos = cable == null ? BlockPos.ZERO : cable.getBlockPos();
        Direction initial = cable == null ? Direction.NORTH : cable.openingSide();
        sideOrdinal = initial.ordinal();

        for (int i = 0; i < FILTER_SLOTS; i++) addSlot(new Slot(filters, i, 8 + i * 18, 80) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 119 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 177));

        addDataSlot(new DataSlot() {
            @Override public int get() { return sideOrdinal; }
            @Override public void set(int value) { sideOrdinal = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return filterMode; }
            @Override public void set(int value) { filterMode = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return priority; }
            @Override public void set(int value) { priority = value; }
        });
        loadSide();
    }

    public BlockPos cablePos() { return cablePos; }
    public Direction side() { return Direction.values()[Math.floorMod(sideOrdinal, Direction.values().length)]; }
    public int priority() { return priority; }
    public String connectionMode() {
        var state = playerInventory.player.level().getBlockState(cablePos);
        return state.hasProperty(net.crystalnexus.block.DepotCableBlock.MODE)
                ? state.getValue(net.crystalnexus.block.DepotCableBlock.MODE).getSerializedName().toUpperCase(java.util.Locale.ROOT)
                : "DEFAULT";
    }
    public Component targetName() {
        return playerInventory.player.level().getBlockState(cablePos.relative(side())).getBlock().getName();
    }
    public DepotCableConnectionConfig.FilterMode filterMode() {
        return filterMode == DepotCableConnectionConfig.FilterMode.ALLOW_LISTED.ordinal()
                ? DepotCableConnectionConfig.FilterMode.ALLOW_LISTED
                : DepotCableConnectionConfig.FilterMode.BLOCK_LISTED;
    }

    private List<Direction> sides() {
        if (cable == null) return List.of(side());
        return cable.connections().keySet().stream().sorted(Comparator.comparingInt(Direction::ordinal)).toList();
    }

    private void loadSide() {
        if (cable == null) return;
        DepotCableConnectionConfig config = cable.connection(side());
        if (config == null) {
            List<Direction> available = sides();
            if (available.isEmpty()) return;
            sideOrdinal = available.getFirst().ordinal();
            config = cable.connection(side());
        }
        filterMode = config.filterMode().ordinal();
        priority = config.priority();
        for (int i = 0; i < FILTER_SLOTS; i++) filters.setItem(i, config.itemFilters().get(i));
    }

    private void saveSide() {
        if (cable == null) return;
        List<ItemStack> stacks = new ArrayList<>(FILTER_SLOTS);
        for (int i = 0; i < FILTER_SLOTS; i++) stacks.add(filters.getItem(i));
        cable.update(side(), priority, filterMode(), stacks);
        broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (cable == null) return false;
        List<Direction> available = sides();
        if (available.isEmpty()) return false;
        int index = Math.max(0, available.indexOf(side()));
        switch (id) {
            case 0 -> sideOrdinal = available.get(Math.floorMod(index - 1, available.size())).ordinal();
            case 1 -> sideOrdinal = available.get((index + 1) % available.size()).ordinal();
            case 2 -> filterMode = filterMode() == DepotCableConnectionConfig.FilterMode.ALLOW_LISTED
                    ? DepotCableConnectionConfig.FilterMode.BLOCK_LISTED.ordinal()
                    : DepotCableConnectionConfig.FilterMode.ALLOW_LISTED.ordinal();
            case 3 -> priority = Math.max(-100, priority - 1);
            case 4 -> priority = Math.min(100, priority + 1);
            case 5 -> priority = Math.max(-100, priority - 10);
            case 6 -> priority = Math.min(100, priority + 10);
            default -> { return false; }
        }
        if (id <= 1) loadSide(); else saveSide();
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOTS) {
            ItemStack selected = button == 1 ? ItemStack.EMPTY : getCarried();
            filters.setItem(slotId, selected.isEmpty() ? ItemStack.EMPTY : selected.copyWithCount(1));
            saveSide();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        return cable != null && !cable.isRemoved() && player.distanceToSqr(
                cablePos.getX() + 0.5, cablePos.getY() + 0.5, cablePos.getZ() + 0.5) <= 64.0;
    }
}
