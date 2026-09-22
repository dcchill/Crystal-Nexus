package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.item.ReactorFuelCellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class ReactorCoreBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final double[] wearProgress = new double[3];

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.REACTOR_CORE.get(), pos, state);
    }

    @Override public int getContainerSize() { return 3; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.reactor_core"); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) { return null; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof ReactorFuelCellItem; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    public void addWear(int slot, double amount) {
        ItemStack stack = getItem(slot);
        if (!(stack.getItem() instanceof ReactorFuelCellItem)) return;
        wearProgress[slot] += amount;
        int damage = (int) wearProgress[slot];
        if (damage > 0) {
            wearProgress[slot] -= damage;
            if (stack.getDamageValue() + damage >= stack.getMaxDamage()) {
                setItem(slot, new ItemStack(net.crystalnexus.init.CrystalnexusModItems.SPENT_REACTOR_CELL.get()));
                wearProgress[slot] = 0;
            } else {
                stack.setDamageValue(stack.getDamageValue() + damage);
                setChanged();
            }
        }
        setChanged();
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (!ItemStack.isSameItemSameComponents(getItem(slot), stack)) wearProgress[slot] = 0;
        super.setItem(slot, stack);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(3, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < 3; i++) wearProgress[i] = tag.getDouble("Wear" + i);
    }

    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        for (int i = 0; i < 3; i++) tag.putDouble("Wear" + i, wearProgress[i]);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
