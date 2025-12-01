package net.crystalnexus.block.entity;

import net.neoforged.neoforge.energy.EnergyStorage;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

import net.crystalnexus.world.inventory.FactoryItemControllerGuiMenu;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class FactoryItemControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> stacks = NonNullList.withSize(24, ItemStack.EMPTY);

    public FactoryItemControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.FACTORY_ITEM_CONTROLLER.get(), pos, state);
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(compound, lookupProvider);
        if (!this.tryLoadLootTable(compound))
            this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
        if (compound.get("energyStorage") instanceof IntTag intTag)
            energyStorage.deserializeNBT(lookupProvider, intTag);
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(compound, lookupProvider);
        if (!this.trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
        }
        compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
        return this.saveWithFullMetadata(lookupProvider);
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.stacks)
            if (!itemstack.isEmpty())
                return false;
        return true;
    }

    @Override
    public Component getDefaultName() {
        return Component.literal("factory_item_controller");
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FactoryItemControllerGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Factory Item Controller");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // Prevent upgrades from going into regular input slots
        if (index >= 14 && index <= 18) { // input slots
            return !stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.tryParse("crystalnexus:machine_upgrades")));
        }
        return true;
    }

    /** --- WORLDLY CONTAINER METHODS --- **/

    @Override
    public int[] getSlotsForFace(Direction side) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;

        if (side == Direction.UP) return new int[]{14}; // top input
        if (side == Direction.DOWN) return new int[0]; // bottom does nothing

        // relative sides
        if (side == facing) return new int[]{17}; // front
        if (side == facing.getOpposite()) return new int[]{15}; // back
        if (side == facing.getClockWise(Direction.Axis.Y)) return new int[]{16}; // right
        if (side == facing.getCounterClockWise(Direction.Axis.Y)) return new int[]{18}; // left

        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        // prevent taking link items
        if (index >= 9 && index <= 13) return false;
        return true;
    }

    /** --- ENERGY STORAGE --- **/
    private final EnergyStorage energyStorage = new EnergyStorage(80192, 80192, 80192, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int retval = super.receiveEnergy(maxReceive, simulate);
            if (!simulate) {
                setChanged();
                level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
            }
            return retval;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int retval = super.extractEnergy(maxExtract, simulate);
            if (!simulate) {
                setChanged();
                level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
            }
            return retval;
        }
    };

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
