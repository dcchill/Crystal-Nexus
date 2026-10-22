package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.PartsAssemblerBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PartsAssemblerMenu extends TieredMachineMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public PartsAssemblerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, findContainer(inventory, buffer.readBlockPos()), new SimpleContainerData(5));
    }

    public PartsAssemblerMenu(int id, Inventory inventory, PartsAssemblerBlockEntity assembler, ContainerData data) {
        this(id, inventory, (Container) assembler, data);
    }

    private PartsAssemblerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(CrystalnexusModMenus.PARTS_ASSEMBLER.get(), id);
        this.container = container;
        this.data = data;
        this.pos = container instanceof BlockEntity blockEntity ? blockEntity.getBlockPos() : BlockPos.ZERO;
        checkContainerSize(container, machineSlots());
        container.startOpen(inventory.player);

        var items = new MachineItemHandler(container);
        addSlot(new MachineItemSlot(items, 0, MachineGuiLayout.grid(43, 8), MachineGuiLayout.ASSEMBLER_SLOT_Y));
        addSlot(new MachineItemSlot(items, 1, MachineGuiLayout.grid(115, 8), MachineGuiLayout.ASSEMBLER_SLOT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int index = 2; index < machineSlots(); index++) {
            if (isLathe() && index == 4) {
                addSlot(new MachineItemSlot(items, 4, MachineGuiLayout.grid(133, 8), MachineGuiLayout.ASSEMBLER_SLOT_Y) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                });
            } else {
                int ordinal = isLathe() && index >= 5 ? index - 3 : index - 2;
                final int physicalSlot = index;
                addSlot(new Slot(container, index, MachineGuiLayout.sideX(ordinal), MachineGuiLayout.sideY(ordinal)) {
                    @Override public int getMaxStackSize(ItemStack stack) {
                        return Math.min(net.crystalnexus.util.MachineUpgradeHelper.upgradeStackLimit(stack), super.getMaxStackSize(stack));
                    }
                    @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(physicalSlot, stack); }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }

    private static Container findContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof PartsAssemblerBlockEntity assembler ? assembler : new SimpleContainer(4);
    }

    public boolean isLathe() { return container instanceof PartsAssemblerBlockEntity assembler && assembler.isLathe(); }
    public boolean isPress() { return container instanceof PartsAssemblerBlockEntity assembler && assembler.isPress(); }
    private int machineSlots() { return container.getContainerSize(); }

    public String machineId() {
        return container instanceof BlockEntity entity
            ? net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getPath() : "parts_assembler";
    }

    public int progress() { return data.get(0); }
    public int maxProgress() { return Math.max(1, data.get(1)); }
    public int energy() { return data.get(2); }
    public int maxEnergy() { return Math.max(1, data.get(3)); }
    public int selectedMode() { return data.get(4); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (isPress() || id < (isLathe() ? 1 : 0) || id > 2) return false;
        if (container instanceof PartsAssemblerBlockEntity assembler) assembler.setSelectedMode(id);
        return true;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < machineSlots()) {
            if (!moveItemStackTo(original, machineSlots(), slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean upgrade = original.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
            if (!moveItemStackTo(original, upgrade ? 2 : 0, upgrade ? machineSlots() : 1, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }

    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    public BlockPos guiPosition() { return pos; }
}
