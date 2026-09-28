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

import java.util.UUID;

public class DepotUploaderBlockEntity extends BlockEntity implements WorldlyContainer {

    private static final int SIZE = 1;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private int tickCounter = 0;
    private UUID owner;

    public DepotUploaderBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.DEPOT_UPLOADER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DepotUploaderBlockEntity be) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        be.tickCounter++;
        if (be.tickCounter % 5 != 0) return; // every 5 ticks

        if (be.owner == null) return;
        if (!DepotSavedData.hasPoweredController(serverLevel, be.owner)) return;
        DepotSavedData data = DepotSavedData.get(serverLevel, be.owner);

        boolean changed = false;

        for (int i = 0; i < be.getContainerSize(); i++) {
            ItemStack stack = be.getItem(i);
            if (stack.isEmpty()) continue;

            int want = Math.min(64, stack.getCount());

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) continue;

            long acceptedLong = data.addCapped(itemId, want);
            int accepted = (int) Math.min(Integer.MAX_VALUE, acceptedLong);

            if (accepted <= 0) {
                break;
            }

            stack.shrink(accepted);
            if (stack.isEmpty()) {
                be.items.set(i, ItemStack.EMPTY);
            }

            changed = true;

            if (data.getFree() <= 0) break;
        }

        if (changed) {
            be.setChanged();
        }
    }


    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[SIZE];
        for (int i = 0; i < SIZE; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }


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

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("TickCounter", tickCounter);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        tickCounter = tag.getInt("TickCounter");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
