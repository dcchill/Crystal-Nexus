package net.crystalnexus.block.entity;

import net.crystalnexus.block.PartsAssemblerBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.jei_recipes.PartsAssemblingRecipe;
import net.crystalnexus.processing.PartsAssemblingDefaults;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineItemStorage;
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
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Collection;

public final class PartsAssemblerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int CAPACITY = 10_000;
    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = 100;
    private int selectedMode;

    public boolean isLathe() { return getBlockState().is(net.crystalnexus.init.CrystalnexusModBlocks.AZURINE_LATHE.get())
        || getBlockState().getBlock() instanceof net.crystalnexus.block.FerrosteelLatheBlock; }
    public boolean isPress() { return getBlockState().getBlock() instanceof net.crystalnexus.block.PlatePressBlock; }
    public boolean supportsRecipe(ResourceLocation id, PartsAssemblingRecipe recipe) {
        if (isPress()) return PartsAssemblingDefaults.isPressOnly(id);
        if (PartsAssemblingDefaults.isPressOnly(id)) return false;
        return isLathe() ? PartsAssemblingDefaults.isLatheRecipe(recipe) : !PartsAssemblingDefaults.isLatheOnly(id);
    }
    private ItemStack cachedRecipeInput = ItemStack.EMPTY;
    private PartsAssemblingRecipe cachedRecipe;
    private ItemStack cachedDust = ItemStack.EMPTY;
    private RecipeManager cachedRecipeManager;
    private Collection<RecipeHolder<?>> cachedRecipes;
    private ResourceLocation cachedAssignedRecipe;
    private int cachedRecipeMode = -1;

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, net.crystalnexus.config.CrystalnexusConfig.machineEnergyInput(2_048), 2_048) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) setChanged();
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
        if (isLathe() || isPress()) items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (isLathe()) selectedMode = 1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PartsAssemblerBlockEntity blockEntity) {
		if (!net.crystalnexus.util.MachineTickPolicy.shouldTick(level, pos, blockEntity.progress > 0)) return;
        if (!net.crystalnexus.assembly.AssemblyLineMachine.mayTick(level, pos)) return;
        blockEntity.tickServer(level, pos, state);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        ItemStack input = items.get(0);
        ResourceLocation assigned = net.crystalnexus.assembly.AssemblyLineMachine.assignedRecipe(level, pos);
        PartsAssemblingRecipe recipe = findRecipe(level, input, assigned);

        var upgrade = MachineUpgradeHelper.upgrades(this, 2, 3);
        int energyCost = recipe == null ? 0 : MachineUpgradeHelper.energyCost(state, upgrade, recipe.energyPerTick());
        ItemStack result = recipe == null ? ItemStack.EMPTY : (recipe.getResultItem(level.registryAccess())).copy();
        ItemStack dust = cachedDust;
        boolean working = recipe != null && canAccept(result) && (!isLathe() || canAcceptAt(4, dust))
            && energy.getEnergyStored() >= energyCost;
        if (working) {
            double cookTime = MachineTier.from(state).processingTime(recipe.processingTime() * (isLathe() ? 1.25 : 1));
            cookTime = MachineUpgradeHelper.processingTime(upgrade, cookTime, cookTime * 0.75, cookTime * 0.5);
            maxProgress = (int) Math.ceil(MachineUpgradeHelper.cookTime(upgrade, cookTime));
            energy.extractEnergy(energyCost, false);
            progress++;
            if (progress >= maxProgress) {
                craft(result, dust);
                int crafts = MachineUpgradeHelper.parallelCraftCount(upgrade);
                for (int craft = 1; craft < crafts; craft++) {
                    if (!recipe.matches(new SingleRecipeInput(items.get(0)), level)
                            || !canAccept(result) || (isLathe() && !canAcceptAt(4, dust)) || energy.getEnergyStored() < energyCost) break;
                    energy.extractEnergy(energyCost, false);
                    craft(result, dust);
                }
                progress = 0;
            }
        } else if (progress != 0) {
            progress = 0;
        }

        boolean lit = state.getValue(PartsAssemblerBlock.LIT);
        if (lit != working) level.setBlock(pos, state.setValue(PartsAssemblerBlock.LIT, working), 3);
        if (working || lit != working) setChanged();
    }

    private @Nullable PartsAssemblingRecipe findRecipe(Level level, ItemStack input, @Nullable ResourceLocation assigned) {
        RecipeManager manager = level.getRecipeManager();
        var recipes = manager.getRecipes();
        if (manager == cachedRecipeManager && recipes == cachedRecipes && selectedMode == cachedRecipeMode
                && Objects.equals(assigned, cachedAssignedRecipe)
                && input.getCount() == cachedRecipeInput.getCount()
                && ItemStack.isSameItemSameComponents(input, cachedRecipeInput)) return cachedRecipe;

        cachedRecipe = manager.getAllRecipesFor(PartsAssemblingRecipe.Type.INSTANCE).stream()
            .filter(holder -> assigned == null || holder.id().equals(assigned))
            .filter(holder -> supportsRecipe(holder.id(), holder.value()))
            .map(holder -> holder.value())
            .filter(candidate -> candidate.mode().ordinal() == selectedMode)
            .filter(candidate -> !isLathe() || PartsAssemblingDefaults.isLatheRecipe(candidate))
            .filter(candidate -> !isLathe() || candidate.mode() != PartsAssemblingRecipe.Mode.BOLT || PartsAssemblingDefaults.isRod(input))
            .filter(candidate -> candidate.matches(new SingleRecipeInput(input), level))
            .min(java.util.Comparator.comparing(candidate -> candidate.getResultItem(level.registryAccess()),
                PartsAssemblingDefaults.OUTPUT_ORDER)).orElse(null);
        cachedDust = isLathe() ? PartsAssemblingDefaults.associatedDust(input) : ItemStack.EMPTY;
        cachedRecipeManager = manager;
        cachedRecipes = recipes;
        cachedRecipeMode = selectedMode;
        cachedAssignedRecipe = assigned;
        cachedRecipeInput = input.copy();
        return cachedRecipe;
    }

    private boolean canAccept(ItemStack result) {
        return canAcceptAt(1, result);
    }

    private boolean canAcceptAt(int slot, ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack output = items.get(slot);
        return (output.isEmpty() || ItemStack.isSameItemSameComponents(output, result))
            && (long) output.getCount() + result.getCount() <= MachineItemStorage.slotLimit(this, slot, result);
    }

    private void craft(ItemStack result, ItemStack dust) {
        if (isLathe() && level.random.nextInt(100) < 33) {
            if (items.get(4).isEmpty()) items.set(4, dust.copy());
            else items.get(4).grow(dust.getCount());
        }
        items.get(0).shrink(1);
        ItemStack output = items.get(1);
        if (output.isEmpty()) items.set(1, result.copy());
        else output.grow(result.getCount());
        setChanged();
    }

    public void setSelectedMode(int mode) {
        int clamped = PartsAssemblingRecipe.Mode.fromId(mode).ordinal();
        if (isLathe() && clamped == 0 || isPress() && clamped != 0) return;
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
        MachineItemStorage.load(tag, items, registries, this);
        if (tag.get("energy") instanceof IntTag energyTag) energy.deserializeNBT(registries, energyTag);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        selectedMode = PartsAssemblingRecipe.Mode.fromId(tag.getInt("mode")).ordinal();
        if (isLathe() && selectedMode == 0) selectedMode = 1;
        if (isPress()) selectedMode = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        MachineItemStorage.save(tag, items, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("mode", selectedMode);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public int getContainerSize() { return isPress() ? 2 + MachineUpgradeHelper.upgradeSlots(getBlockState())
        : isLathe() ? 3 + MachineUpgradeHelper.upgradeSlots(getBlockState()) : 4; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new PartsAssemblerMenu(id, inventory, this, data);
    }

    public boolean isUpgradeSlot(int slot) { return slot >= 2 && slot < getContainerSize() && (!isLathe() || slot != 4); }
    @Override public int getMaxStackSize() { return MachineItemStorage.isTiered(this) ? MachineTier.from(getBlockState()).itemSlotCapacity() : 64; }
    @Override public int getMaxStackSize(ItemStack stack) { return MachineItemStorage.isTiered(this) ? MachineItemStorage.capacity(this, stack) : super.getMaxStackSize(stack); }
    @Override public void setItem(int slot, ItemStack stack) {
        unpackLootTable(null);
        stack.limitSize(MachineItemStorage.slotLimit(this, slot, stack));
        items.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot) && net.crystalnexus.util.MachineUpgradeHelper.isZeroChip(stack)
                && !getItem(slot).isEmpty()) return false;
        return slot == 0 || isUpgradeSlot(slot) && MachineUpgradeHelper.isMachineUpgrade(stack);
    }
    @Override public int[] getSlotsForFace(Direction side) { return java.util.stream.IntStream.range(0, getContainerSize()).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return slot == 0; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1 || isLathe() && slot == 4; }
    @Override public void onLoad() {
        super.onLoad();
        if (level != null) net.crystalnexus.assembly.AssemblyLineEvents.changed(level, worldPosition, true);
    }
    @Override public void setRemoved() {
        if (level != null) net.crystalnexus.assembly.AssemblyLineEvents.changed(level, worldPosition, true);
        super.setRemoved();
    }
}
