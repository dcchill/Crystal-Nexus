package net.crystalnexus.block.entity;

import net.crystalnexus.block.ProcessingModuleBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.ProcessingModuleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;

public final class ProcessingModuleBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_PER_TICK = 20;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final EnergyStorage energy = new EnergyStorage(
            operation() == ProcessingModuleBlock.Operation.NANOFABRICATE ? 128_000 : ENERGY_CAPACITY,
            operation() == ProcessingModuleBlock.Operation.NANOFABRICATE ? 16_000 : 1_000, Integer.MAX_VALUE) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) sync();
            return received;
        }
    };
    private int progress;
    private int maxProgress = 1;
    private boolean working;
    private final net.crystalnexus.block.ProcessingModuleAnimation animation = new net.crystalnexus.block.ProcessingModuleAnimation();
    @Nullable private ResourceLocation currentRecipe;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : index == 1 ? maxProgress : 0; }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = Math.max(1, value);
        }
        @Override public int getCount() { return 2; }
    };

    public ProcessingModuleBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PROCESSING_MODULE.get(), pos, state);
    }

    public ProcessingModuleBlock.Operation operation() {
        return getBlockState().getBlock() instanceof ProcessingModuleBlock module ? module.operation() : ProcessingModuleBlock.Operation.PRESS;
    }
    public boolean hasMaterialSlot() {
        return operation() == ProcessingModuleBlock.Operation.PRESS || operation() == ProcessingModuleBlock.Operation.WELD;
    }
    public EnergyStorage energy() { return energy; }
    public ContainerData data() { return data; }
    public int energyStored() { return energy.getEnergyStored(); }
    @Nullable public ResourceLocation currentRecipe() { return currentRecipe; }
    public ItemStack material() { return items.get(0); }
    public boolean isWorking() { return working; }
    public float animationProgress(float partialTick) {
        return animation.progress(currentRecipe == null ? null : currentRecipe.toString(), progress, maxProgress,
            working, (level == null ? 0 : level.getGameTime()) + (double) partialTick);
    }

    public boolean canSupply(net.crystalnexus.recipe.ProcessingLineRecipe.Step step, int count) {
        return step.ingredient().isEmpty() || items.get(0).getCount() >= count && step.ingredient().test(items.get(0));
    }

    public boolean consumeMaterial(net.crystalnexus.recipe.ProcessingLineRecipe.Step step, int count) {
        if (!canSupply(step, count)) return false;
        if (!step.ingredient().isEmpty()) {
            items.get(0).shrink(count);
            if (items.get(0).isEmpty()) items.set(0, ItemStack.EMPTY);
        }
        setChanged();
        return true;
    }

    public boolean consumeEnergy(int amount) {
        if (energy.extractEnergy(amount, true) < amount) return false;
        energy.extractEnergy(amount, false);
        setChanged();
        return true;
    }

    public void setProcessing(@Nullable ResourceLocation recipe, int progress, int duration, boolean working) {
        if (java.util.Objects.equals(currentRecipe, recipe) && this.progress == progress && maxProgress == duration && this.working == working) return;
        currentRecipe = recipe;
        this.working = working;
        this.progress = Math.max(0, progress);
        maxProgress = Math.max(1, duration);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = items.get(0).split(amount);
        if (items.get(0).isEmpty()) items.set(0, ItemStack.EMPTY);
        if (!result.isEmpty()) sync();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack result = items.get(0);
        items.set(0, ItemStack.EMPTY);
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0 || !hasMaterialSlot()) return;
        items.set(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(Math.min(stack.getCount(), 64)));
        sync();
    }
    @Override public void clearContent() { items.set(0, ItemStack.EMPTY); sync(); }
    @Override public int[] getSlotsForFace(Direction side) { return hasMaterialSlot() ? new int[] {0} : new int[0]; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && hasMaterialSlot(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && hasMaterialSlot(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ProcessingModuleMenu(id, inventory, this, data); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("material", items.get(0).saveOptional(registries));
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("working", working);
        if (currentRecipe != null) tag.putString("recipe", currentRecipe.toString());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.set(0, ItemStack.parseOptional(registries, tag.getCompound("material")));
        if (tag.get("energy") instanceof IntTag energyTag) energy.deserializeNBT(registries, energyTag);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        working = tag.getBoolean("working");
        currentRecipe = ResourceLocation.tryParse(tag.getString("recipe"));
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
}
