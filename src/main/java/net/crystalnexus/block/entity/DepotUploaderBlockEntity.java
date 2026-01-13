package net.crystalnexus.block.entity;

import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DepotUploaderBlockEntity extends BlockEntity implements WorldlyContainer {

    private static final int SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private int tickCounter = 0;

    public DepotUploaderBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DEPOT_UPLOADER.get(), pos, state);
    }

    // ---- ticking upload ----
    public static void tick(Level level, BlockPos pos, BlockState state, DepotUploaderBlockEntity be) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        be.tickCounter++;
        if (be.tickCounter % 5 != 0) return; // every 5 ticks

        DepotSavedData data = DepotSavedData.get(serverLevel);

        for (int i = 0; i < be.getContainerSize(); i++) {
            ItemStack stack = be.getItem(i);
            if (stack.isEmpty()) continue;

            int toMove = Math.min(64, stack.getCount());
            ItemStack extracted = be.removeItem(i, toMove);
            if (extracted.isEmpty()) continue;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(extracted.getItem());
            if (itemId != null) {
                data.add(itemId, extracted.getCount());
            }
        }

        be.setChanged();
    }

    // ---- WorldlyContainer (sided inventory) ----

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[SIZE];
        for (int i = 0; i < SIZE; i++) slots[i] = i;
        return slots;
    }

    // ✅ input-only: allow inserting from any side
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ✅ input-only: deny extracting from any side
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    // ---- Container basics ----

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // ---- Save/load ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        tickCounter = tag.getInt("TickCounter");
    }
}
