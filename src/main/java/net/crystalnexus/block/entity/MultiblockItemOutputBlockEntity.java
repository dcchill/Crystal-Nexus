package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.WasteOutputGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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
import java.util.stream.IntStream;

public final class MultiblockItemOutputBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);

    public MultiblockItemOutputBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.MULTIBLOCK_ITEM_OUTPUT.get(), pos, state);
    }

    public boolean insert(ItemStack source, boolean simulate) {
        ItemStack remaining = source.copy();
        for (int slot = 0; slot < stacks.size() && !remaining.isEmpty(); slot++) {
            ItemStack current = stacks.get(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) continue;
            int room = current.isEmpty() ? remaining.getMaxStackSize() : current.getMaxStackSize() - current.getCount();
            int moved = Math.min(room, remaining.getCount());
            if (!simulate && moved > 0) {
                if (current.isEmpty()) stacks.set(slot, remaining.copyWithCount(moved)); else current.grow(moved);
            }
            remaining.shrink(moved);
        }
        if (!simulate && remaining.getCount() != source.getCount()) sync();
        return remaining.isEmpty();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable("block.crystalnexus.multiblock_item_output"); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, stacks.size()).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new WasteOutputGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
    }
    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
