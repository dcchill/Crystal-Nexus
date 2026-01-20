package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

public class TankBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int PER_BLOCK_CAPACITY = 8_000;

    private BlockPos controllerPos;
    private int memberCount = 1;

    // ✅ MCreator-friendly ItemHandler shim (1 slot, locked)
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false; // lock inserts
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack; // refuse inserts
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // refuse extracts
        }
    };

    // ✅ expose it for capability registration
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private final FluidTank tank = new FluidTank(PER_BLOCK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };	
    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.TANK.get(), pos, state);
    }
public net.neoforged.neoforge.fluids.capability.templates.FluidTank getFluidTank() {
    return getNetworkTank();
}

    public boolean isController() {
        return controllerPos == null || controllerPos.equals(worldPosition);
    }

    public BlockPos getControllerPos() {
        return (controllerPos == null) ? worldPosition : controllerPos;
    }

    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    public void setMemberCount(int count) {
        this.memberCount = Math.max(1, count);
        tank.setCapacity(this.memberCount * PER_BLOCK_CAPACITY);
        setChanged();
    }

    public int getMemberCount() {
        return memberCount;
    }

    public FluidTank getTank() {
        return tank;
    }

    public TankBlockEntity getController() {
        if (level == null) return this;
        BlockPos cpos = getControllerPos();
        BlockEntity be = level.getBlockEntity(cpos);
        return (be instanceof TankBlockEntity t) ? t : this;
    }

    public FluidTank getNetworkTank() {
        return getController().tank;
    }

    // --------- Sync / NBT ----------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);

        if (controllerPos != null && !controllerPos.equals(worldPosition)) {
            tag.putLong("Controller", controllerPos.asLong());
        }
        tag.putInt("Members", memberCount);

        // ✅ save item handler
        tag.put("Inv", itemHandler.serializeNBT(lookup));

        // save fluid tank
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(lookup, tankTag);
        tag.put("Tank", tankTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);

        controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
        memberCount = Math.max(1, tag.getInt("Members"));

        // ✅ load item handler
        if (tag.contains("Inv")) itemHandler.deserializeNBT(lookup, tag.getCompound("Inv"));

        if (tag.contains("Tank")) tank.readFromNBT(lookup, tag.getCompound("Tank"));
        tank.setCapacity(memberCount * PER_BLOCK_CAPACITY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithFullMetadata(lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

@Override
public int getContainerSize() { return 0; }

@Override
public boolean isEmpty() { return true; }

@Override
public ItemStack getItem(int slot) { return ItemStack.EMPTY; }

@Override
public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }

@Override
public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }

@Override
public void setItem(int slot, ItemStack stack) {}

@Override
public boolean stillValid(Player player) { return true; }

@Override
public void clearContent() {}

@Override
public int[] getSlotsForFace(Direction side) { return new int[0]; }

@Override
public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) { return false; }

@Override
public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) { return false; }

@Override
public boolean canPlaceItem(int slot, ItemStack stack) { return false; }

}
