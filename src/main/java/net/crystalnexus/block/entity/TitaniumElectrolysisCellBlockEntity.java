package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.TitaniumElectrolysisRecipe;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.world.inventory.TitaniumElectrolysisCellMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public final class TitaniumElectrolysisCellBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int TANK_CAPACITY = 4000;
    public static final int BASE_ENERGY_PER_OPERATION = 4096;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private final FluidTank inputTank = createTank();
    private final FluidTank outputTank = createTank();

    private final EnergyStorage energyStorage = new EnergyStorage(
        machineTier().minimumCapacity(CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.capacity(), BASE_ENERGY_PER_OPERATION),
        CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.maxReceive(),
        CrystalnexusConfig.MACHINES.CHEMICAL_REACTION_CHAMBER.maxExtract(), 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate); if (!simulate) sync(); return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate); if (!simulate) sync(); return extracted;
        }
    };

    public TitaniumElectrolysisCellBlockEntity(BlockPos pos, BlockState state) {
        super(CrystalnexusModBlockEntities.TITANIUM_ELECTROLYSIS_CELL.get(), pos, state);
    }

    private FluidTank createTank() {
        return new FluidTank(TANK_CAPACITY) { @Override protected void onContentsChanged() { sync(); } };
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        emptyFluidContainer();
        MachineTier machineTier = machineTier();
        ItemStack upgrade = stacks.get(2);
        double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade,
            upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 75
                : upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 50 : 100));
        int energyCost = machineTier.energyCost(
            MachineUpgradeHelper.energyCost(upgrade, BASE_ENERGY_PER_OPERATION));
        TitaniumElectrolysisRecipe recipe = findRecipe(serverLevel, machineTier);
        getPersistentData().putDouble("maxProgress", cookTime);
        if (recipe == null || energyStorage.getEnergyStored() < energyCost || !canOutput(recipe)) {
            setActive(false);
            sync();
            return;
        }

        setActive(true);
        double progress = getPersistentData().getDouble("progress") + 1;
        getPersistentData().putDouble("progress", progress);
        if (progress < cookTime) { sync(); return; }

        recipe.fluidInput().ifPresent(input -> inputTank.drain(input.amount(), IFluidHandler.FluidAction.EXECUTE));
        recipe.itemInput().ifPresent(input -> stacks.get(0).shrink(recipe.itemInputCount()));
        outputTank.fill(recipe.fluidOutput().stack(), IFluidHandler.FluidAction.EXECUTE);
        consumeEnergy(energyCost);
        getPersistentData().putDouble("progress", 0);
        sync();
    }

    private TitaniumElectrolysisRecipe findRecipe(ServerLevel level, MachineTier machineTier) {
        for (var holder : level.getRecipeManager().getAllRecipesFor(TitaniumElectrolysisRecipe.Type.INSTANCE)) {
            TitaniumElectrolysisRecipe recipe = holder.value();
            if (!machineTier.supports(recipe.minimumMachineTier())) continue;
            if (recipe.fluidInput().map(input -> input.matches(inputTank.getFluid())).orElse(false)
                    || recipe.itemInput().map(input -> input.test(stacks.get(0))
                        && stacks.get(0).getCount() >= recipe.itemInputCount()).orElse(false)) return recipe;
        }
        return null;
    }

    private boolean canOutput(TitaniumElectrolysisRecipe recipe) {
        FluidStack output = recipe.fluidOutput().stack();
        return outputTank.fill(output, IFluidHandler.FluidAction.SIMULATE) == output.getAmount();
    }

    private void consumeEnergy(int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int extracted = energyStorage.extractEnergy(remaining, false);
            if (extracted == 0) return;
            remaining -= extracted;
        }
    }

    private void emptyFluidContainer() {
        ItemStack stack = stacks.get(0);
        if (stack.getCount() != 1 || !canAcceptReturnedContainer()) return;
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return;
        FluidStack offered = item.drain(TANK_CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        int accepted = inputTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drainable = item.drain(accepted, IFluidHandler.FluidAction.SIMULATE);
        if (drainable.getAmount() != accepted) return;
        inputTank.fill(item.drain(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        ItemStack container = item.getContainer();
        stacks.set(0, ItemStack.EMPTY);
        if (!container.isEmpty()) {
            if (stacks.get(1).isEmpty()) stacks.set(1, container.copy()); else stacks.get(1).grow(container.getCount());
        }
        sync();
    }

    private boolean canAcceptReturnedContainer() {
        ItemStack stack = stacks.get(0);
        ItemStack copy = stack.copy();
        IFluidHandlerItem item = copy.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return true;
        FluidStack offered = item.drain(TANK_CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        int accepted = inputTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return true;
        FluidStack drainable = item.drain(accepted, IFluidHandler.FluidAction.SIMULATE);
        if (drainable.getAmount() != accepted) return true;
        item.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        ItemStack container = item.getContainer();
        return container.isEmpty() || stacks.get(1).isEmpty()
            || ItemStack.isSameItemSameComponents(stacks.get(1), container)
                && stacks.get(1).getCount() + container.getCount() <= container.getMaxStackSize();
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        int value = active ? 2 : 1;
        if (state.hasProperty(ChemicalReactionChamberBlock.BLOCKSTATE)
                && state.getValue(ChemicalReactionChamberBlock.BLOCKSTATE) != value)
            level.setBlock(worldPosition, state.setValue(ChemicalReactionChamberBlock.BLOCKSTATE, value), 3);
    }

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return getTank(tank).getFluid(); }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && !stack.isEmpty(); }
        @Override public int fill(FluidStack stack, FluidAction action) { return inputTank.fill(stack, action); }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return outputTank.drain(stack, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return outputTank.drain(amount, action); }
    };

    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public MachineTier machineTier() { return MachineTier.from(getBlockState()); }
    public FluidTank getTank(int index) { return index == 0 ? inputTank : outputTank; }
    public IFluidHandler getFluidHandler() { return fluidHandler; }
    public void purge(int tank) { if (tank == 0) inputTank.setFluid(FluidStack.EMPTY); else if (tank == 1) outputTank.setFluid(FluidStack.EMPTY); }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, provider);
        if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
        if (tag.get("inputTank") instanceof CompoundTag tank) inputTank.readFromNBT(provider, tank);
        if (tag.get("outputTank") instanceof CompoundTag tank) outputTank.readFromNBT(provider, tank);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, provider);
        tag.put("energyStorage", energyStorage.serializeNBT(provider));
        tag.put("inputTank", inputTank.writeToNBT(provider, new CompoundTag()));
        tag.put("outputTank", outputTank.writeToNBT(provider, new CompoundTag()));
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }
    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public Component getDefaultName() { return Component.translatable(machineTier() == MachineTier.CHLOROPHYTE
        ? "block.crystalnexus.chlorophyte_electrolysis_cell"
        : "block.crystalnexus.titanium_electrolysis_cell"); }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TitaniumElectrolysisCellMenu(id, inventory,
            new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 || slot == 2 && stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
    }
    @Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
}
