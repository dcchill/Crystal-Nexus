package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.RefineryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public final class RefineryBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int TANK_CAPACITY = 4000;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private final FluidTank[] tanks = { createTank(true), createTank(false) };

    private FluidTank createTank(boolean slurryOnly) {
        return new FluidTank(TANK_CAPACITY, stack -> !slurryOnly
                || net.crystalnexus.processing.MaterialProcessingCatalog.slurryMaterial(stack).isPresent()) {
            @Override protected void onContentsChanged() {
                setChanged();
                if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        };
    }

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.REFINERY.get(), pos, state);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, provider);
        if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
        for (int i = 0; i < tanks.length; i++)
            if (tag.get("tank" + i) instanceof CompoundTag tank) tanks[i].readFromNBT(provider, tank);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, provider);
        tag.put("energyStorage", energyStorage.serializeNBT(provider));
        for (int i = 0; i < tanks.length; i++) tag.put("tank" + i, tanks[i].writeToNBT(provider, new CompoundTag()));
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }
    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.literal("refinery"); }
    @Override public Component getDisplayName() { return Component.literal("Refinery"); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { this.stacks = stacks; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new RefineryMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot != 1; }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    private final EnergyStorage energyStorage = new EnergyStorage(
        CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.capacity(),
        CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.maxReceive(),
        CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.maxExtract(), 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate); if (!simulate) changed(); return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate); if (!simulate) changed(); return extracted;
        }
        private void changed() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].getFluid(); }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && net.crystalnexus.processing.MaterialProcessingCatalog.slurryMaterial(stack).isPresent();
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? tanks[0].fill(stack, action) : 0;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return tanks[1].drain(stack, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return tanks[1].drain(amount, action); }
    };

    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public FluidTank getTank(int index) { return tanks[index]; }
    public IFluidHandler getFluidHandler() { return fluidHandler; }
    public void purge(int tank) { if (tank >= 0 && tank < tanks.length) tanks[tank].setFluid(FluidStack.EMPTY); }
}
