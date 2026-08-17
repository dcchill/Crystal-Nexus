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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PartsAssemblerMenu extends AbstractContainerMenu {
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
        checkContainerSize(container, 3);
        container.startOpen(inventory.player);

        addSlot(new Slot(container, 0, 43, 35));
        addSlot(new Slot(container, 1, 115, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(container, 2, 180, 8) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
            }
        });
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
        return blockEntity instanceof PartsAssemblerBlockEntity assembler ? assembler : new SimpleContainer(3);
    }

    public int progress() { return data.get(0); }
    public int maxProgress() { return Math.max(1, data.get(1)); }
    public int energy() { return data.get(2); }
    public int maxEnergy() { return Math.max(1, data.get(3)); }
    public int selectedMode() { return data.get(4); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > 2) return false;
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
        if (index < 3) {
            if (!moveItemStackTo(original, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean upgrade = original.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
            if (!moveItemStackTo(original, upgrade ? 2 : 0, upgrade ? 3 : 1, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }

    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
}
