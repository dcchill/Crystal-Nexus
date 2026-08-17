package net.crystalnexus.block.entity;

import net.crystalnexus.block.PartsAssemblerBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.world.inventory.PartsAssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;

public final class PartsAssemblerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int CAPACITY = 10_000;
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = 100;
    private int selectedMode;

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, 2_048, 2_048) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) sync();
            return received;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                case 4 -> selectedMode;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 4) selectedMode = value;
        }

        @Override public int getCount() { return 5; }
    };

    public PartsAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.PARTS_ASSEMBLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PartsAssemblerBlockEntity blockEntity) {
        blockEntity.tickServer(level, pos, state);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        ItemStack input = items.get(0);
        PartsAssemblingRecipe recipe = level.getRecipeManager().getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream()
            .map(holder -> holder.value())
            .filter(candidate -> candidate.mode().ordinal() == selectedMode)
            .filter(candidate -> candidate.matches(new SingleRecipeInput(input), level))
            .findFirst().orElse(null);

        ItemStack upgrade = items.get(2);
        int energyCost = recipe == null ? 0 : MachineUpgradeHelper.energyCost(upgrade, recipe.energyPerTick());
        boolean working = recipe != null && canAccept(recipe.getResultItem(level.registryAccess()))
            && energy.getEnergyStored() >= energyCost;
        if (working) {
            double cookTime = recipe.processingTime();
            if (upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get())) cookTime *= 0.75;
            else if (upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())) cookTime *= 0.5;
            maxProgress = (int) Math.ceil(MachineUpgradeHelper.cookTime(upgrade, cookTime));
            energy.extractEnergy(energyCost, false);
            progress++;
            if (progress >= maxProgress) {
                craft(recipe.getResultItem(level.registryAccess()));
                progress = 0;
            }
        } else if (progress != 0) {
            progress = 0;
        }

        boolean lit = state.getValue(PartsAssemblerBlock.LIT);
        if (lit != working) level.setBlock(pos, state.setValue(PartsAssemblerBlock.LIT, working), 3);
        if (working || lit != working) sync();
    }

    private boolean canAccept(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack output = items.get(1);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(ItemStack result) {
        items.get(0).shrink(1);
        ItemStack output = items.get(1);
        if (output.isEmpty()) items.set(1, result.copy());
        else output.grow(result.getCount());
        setChanged();
    }

    public void setSelectedMode(int mode) {
        int clamped = PartsAssemblingRecipe.Mode.fromId(mode).ordinal();
        if (selectedMode != clamped) {
            selectedMode = clamped;
            progress = 0;
            sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public EnergyStorage getEnergyStorage() { return energy; }
    public ContainerData getData() { return data; }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.get("energy") instanceof IntTag energyTag) energy.deserializeNBT(registries, energyTag);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        selectedMode = PartsAssemblingRecipe.Mode.fromId(tag.getInt("mode")).ordinal();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("mode", selectedMode);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public int getContainerSize() { return 3; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.crystalnexus.parts_assembler"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new PartsAssemblerMenu(id, inventory, this, data);
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 || slot == 2 && stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
    }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{1} : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return slot == 0; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }
}
