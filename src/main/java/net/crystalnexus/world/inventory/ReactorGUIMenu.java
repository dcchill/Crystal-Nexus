package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.block.entity.ReactorCoreBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.crystalnexus.item.ReactorFuelCellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ReactorGUIMenu extends AbstractContainerMenu {
	public static final int MASTER_INSERTION_BUTTON = 10_000;
    private final Level level;
    private final ReactorComputerBlockEntity controller;
    private final ContainerLevelAccess access;
    private final List<BlockPos> rods;
    private int page;
	private int masterInsertion;

    public ReactorGUIMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        super(CrystalnexusModMenus.REACTOR_GUI.get(), id);
        level = inventory.player.level();
        BlockPos pos = data.readBlockPos();
        controller = level.getBlockEntity(pos) instanceof ReactorComputerBlockEntity found ? found : null;
        access = ContainerLevelAccess.create(level, pos);
        int count = Math.min(3375, Math.max(0, data.readVarInt()));
        rods = new ArrayList<>(count);
        for (int i = 0; i < count; i++) rods.add(data.readBlockPos());
        if (!level.isClientSide && controller != null && !controller.getItem(0).isEmpty()) {
            ItemStack legacyFuel = controller.removeItemNoUpdate(0);
            controller.setChanged();
            inventory.placeItemBackInInventory(legacyFuel);
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return page; }
            @Override public void set(int value) { page = value; }
        });
		addDataSlot(new DataSlot() {
			@Override public int get() { return controller == null ? masterInsertion : controller.masterControlRodInsertion(); }
			@Override public void set(int value) { masterInsertion = net.minecraft.util.Mth.clamp(value, 0, 100); }
		});
        Container safe = controller == null ? new SimpleContainer(3) : controller;
        addSlot(new Slot(safe, 1, 180, 8) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(CrystalnexusModItems.REACTOR_UPGRADE.get())
                    || stack.is(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get());
            }
        });
        addSlot(new Slot(safe, 2, 180, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 9; row++) {
            final int rodRow = row;
            Container binding = new SimpleContainer(3) {
                private ReactorCoreBlockEntity core() { return coreAt(rodRow); }
                @Override public ItemStack getItem(int slot) { return core() == null ? ItemStack.EMPTY : core().getItem(slot); }
                @Override public ItemStack removeItem(int slot, int amount) { return core() == null ? ItemStack.EMPTY : core().removeItem(slot, amount); }
                @Override public ItemStack removeItemNoUpdate(int slot) { return core() == null ? ItemStack.EMPTY : core().removeItemNoUpdate(slot); }
                @Override public void setItem(int slot, ItemStack stack) { if (core() != null) core().setItem(slot, stack); }
                @Override public void setChanged() { if (core() != null) core().setChanged(); }
            };
            for (int cell = 0; cell < 3; cell++) addSlot(new Slot(binding, cell,
                    8 + (row % 3) * 54 + cell * 18, 32 + (row / 3) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return coreAt(rodRow) != null && stack.getItem() instanceof ReactorFuelCellItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
    }

    public ReactorComputerBlockEntity controller() { return controller; }
    public int page() { return page; }
    public int pageCount() { return Math.max(1, (rods.size() + 8) / 9); }
    public int rodCount() { return rods.size(); }
	public int masterInsertion() { return masterInsertion; }
    public BlockPos rodPosition(int row) { int index = page * 9 + row; return index < rods.size() ? rods.get(index) : null; }
    private ReactorCoreBlockEntity coreAt(int row) {
        BlockPos pos = rodPosition(row);
        return pos != null && level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core ? core : null;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
		if (button >= MASTER_INSERTION_BUTTON && button <= MASTER_INSERTION_BUTTON + 100) {
			if (!stillValid(player) || controller == null) return false;
			masterInsertion = button - MASTER_INSERTION_BUTTON;
			controller.setAllControlRodInsertion(masterInsertion);
			broadcastChanges();
			return true;
		}
        if (!stillValid(player) || button < 0 || button >= pageCount()) return false;
        page = button;
        broadcastFullState();
        return true;
    }

    public void setClientPage(int page) { if (page >= 0 && page < pageCount()) this.page = page; }
	public void setClientMasterInsertion(int insertion) { masterInsertion = net.minecraft.util.Mth.clamp(insertion, 0, 100); }

	@Override public boolean stillValid(Player player) {
		return controller != null && (level.isClientSide || controller.getPersistentData().getBoolean("canOpenInventory")
			&& stillValid(access, player, CrystalnexusModBlocks.REACTOR_COMPUTER.get())
			&& controller.getCachedLayout().fuelRods().stream().map(rod -> rod.pos()).toList().equals(rods));
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < 29) moved = moveItemStackTo(stack, 29, 65, true);
        else if (stack.getItem() instanceof ReactorFuelCellItem) moved = moveItemStackTo(stack, 2, 29, false);
        else if (stack.is(CrystalnexusModItems.REACTOR_UPGRADE.get()) || stack.is(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()))
            moved = moveItemStackTo(stack, 0, 1, false);
        else moved = false;
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
}
