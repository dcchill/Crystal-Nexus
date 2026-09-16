package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.block.HemolyzerBlock;
import net.crystalnexus.jei_recipes.HemolyzerRecipe;
import net.crystalnexus.world.inventory.HemolyzerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;

public final class HemolyzerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int BLOOD_TANK_CAPACITY = 4_000;
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int FE_PER_ITEM = 1_024;
    public static final int PROCESSING_TICKS = 40;

    private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : PROCESSING_TICKS; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int getCount() { return 2; }
    };
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) sync();
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate);
            if (extracted > 0 && !simulate) sync();
            return extracted;
        }
    };
    private final FluidTank bloodTank = new FluidTank(BLOOD_TANK_CAPACITY) {
        @Override protected void onContentsChanged() { sync(); }
    };

    public HemolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.HEMOLYZER.get(), pos, state);
    }

    public FluidTank getBloodTank() { return bloodTank; }
    public EnergyStorage getEnergyStorage() { return energy; }
    public ContainerData data() { return data; }
    public static boolean accepts(Level level, ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(HemolyzerRecipe.Type.INSTANCE).stream().anyMatch(recipe -> recipe.value().matches(stack));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HemolyzerBlockEntity blockEntity) {
        if (level.isClientSide) return;
        ItemStack input = blockEntity.getItem(0);
        HemolyzerRecipe recipe = level.getRecipeManager().getAllRecipesFor(HemolyzerRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.value()).filter(candidate -> candidate.matches(input)).findFirst().orElse(null);
        int yield = recipe == null ? 0 : recipe.blood().amount();
        boolean hasRoom = yield > 0 && blockEntity.bloodTank.fill(recipe.blood().stack(), IFluidHandler.FluidAction.SIMULATE) == yield;
        boolean processing = hasRoom && (blockEntity.progress > 0 || blockEntity.energy.getEnergyStored() >= FE_PER_ITEM);
        if (state.getValue(HemolyzerBlock.LIT) != processing) level.setBlock(pos, state.setValue(HemolyzerBlock.LIT, processing), 3);
        if (!processing) {
            if (blockEntity.progress != 0) { blockEntity.progress = 0; blockEntity.setChanged(); }
            return;
        }
        if (blockEntity.progress == 0) blockEntity.energy.extractEnergy(FE_PER_ITEM, false);
        if (++blockEntity.progress >= PROCESSING_TICKS) {
            blockEntity.bloodTank.fill(recipe.blood().stack(), IFluidHandler.FluidAction.EXECUTE);
            input.shrink(1);
            blockEntity.progress = 0;
        }
        blockEntity.setChanged();
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.crystalnexus.hemolyzer"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new HemolyzerMenu(id, inventory, this); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, registries);
        if (tag.get("blood") instanceof CompoundTag blood) bloodTank.readFromNBT(registries, blood);
        if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored);
        progress = tag.getInt("progress");
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, stacks, registries);
        tag.put("blood", bloodTank.writeToNBT(registries, new CompoundTag()));
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
    }
    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public int getContainerSize() { return stacks.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { this.stacks = stacks; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return level != null && slot == 0 && accepts(level, stack); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[] { 0 }; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
}
