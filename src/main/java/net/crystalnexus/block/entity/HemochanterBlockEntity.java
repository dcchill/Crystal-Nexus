package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.world.inventory.HemochanterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class HemochanterBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int BLOOD_TANK_CAPACITY = 32_000, ENERGY_CAPACITY = 1_000_000, MAX_LEVEL = 32;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
    private int progress;
    private int operationLevel;
    private int operationTicks = 40;
    private final EnchantingTableBlockEntity book;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : operationTicks; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else operationTicks = value; }
        @Override public int getCount() { return 2; }
    };
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, 0) { @Override public int receiveEnergy(int amount, boolean simulate) { int received = super.receiveEnergy(amount, simulate); if (received > 0 && !simulate) sync(); return received; } };
    private final FluidTank bloodTank = new FluidTank(BLOOD_TANK_CAPACITY, fluid -> fluid.getFluid() == net.crystalnexus.init.CrystalnexusModFluids.BLOOD.get()) { @Override protected void onContentsChanged() { sync(); } };

    public HemochanterBlockEntity(BlockPos pos, BlockState state) { super(CrystalnexusModBlockEntities.HEMOCHANTER.get(), pos, state); book = new EnchantingTableBlockEntity(pos, Blocks.ENCHANTING_TABLE.defaultBlockState()); }
    public FluidTank getBloodTank() { return bloodTank; }
    public EnergyStorage getEnergyStorage() { return energy; }
    public ContainerData data() { return data; }
    public EnchantingTableBlockEntity book() { return book; }
    public static boolean accepts(ItemStack stack) { return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(); }

    public static void tick(Level level, BlockPos pos, BlockState state, HemochanterBlockEntity blockEntity) {
        if (level.isClientSide) { EnchantingTableBlockEntity.bookAnimationTick(level, pos, state, blockEntity.book); return; }
        ItemStack input = blockEntity.getItem(0);
        if (blockEntity.progress > 0 && blockEntity.operationLevel == 0) blockEntity.progress = 0;
        if (blockEntity.progress == 0) {
            blockEntity.operationLevel = randomUpgradeableLevel(level, input);
            blockEntity.operationTicks = processingTicks(blockEntity.operationLevel);
        }
        int bloodCost = bloodCost(blockEntity.operationLevel), energyCost = energyCost(blockEntity.operationLevel);
        if (blockEntity.operationLevel == 0 || blockEntity.bloodTank.getFluidAmount() < bloodCost || (blockEntity.progress == 0 && blockEntity.energy.getEnergyStored() < energyCost)) {
            if (blockEntity.progress != 0) { blockEntity.progress = 0; blockEntity.setChanged(); }
            blockEntity.operationLevel = 0;
            blockEntity.operationTicks = 40;
            return;
        }
        if (blockEntity.progress == 0) blockEntity.energy.extractEnergy(energyCost, false);
        if (++blockEntity.progress >= processingTicks(blockEntity.operationLevel)) {
            upgradeRandomEnchantment(level, input, blockEntity.operationLevel);
            blockEntity.bloodTank.drain(bloodCost, IFluidHandler.FluidAction.EXECUTE);
            blockEntity.progress = 0;
            blockEntity.operationLevel = 0;
            blockEntity.operationTicks = 40;
        }
        blockEntity.setChanged();
    }

    private static int randomUpgradeableLevel(Level level, ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Integer> eligible = new ArrayList<>();
        enchantments.entrySet().forEach(entry -> { if (entry.getIntValue() < MAX_LEVEL) eligible.add(entry.getIntValue()); });
        return eligible.isEmpty() ? 0 : eligible.get(level.random.nextInt(eligible.size()));
    }
    private static int bloodCost(int level) { return 500 * level; }
    private static int energyCost(int level) { return 500 * level * level; }
    private static int processingTicks(int level) { return Math.max(40, 40 * level); }
    private static void upgradeRandomEnchantment(Level level, ItemStack stack, int enchantmentLevel) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Holder<Enchantment>> eligible = new ArrayList<>();
        enchantments.entrySet().forEach(entry -> { if (entry.getIntValue() == enchantmentLevel) eligible.add(entry.getKey()); });
        if (eligible.isEmpty()) return;
        Holder<Enchantment> selected = eligible.get(level.random.nextInt(eligible.size()));
        ItemEnchantments.Mutable upgraded = new ItemEnchantments.Mutable(enchantments);
        upgraded.set(selected, enchantments.getLevel(selected) + 1);
        stack.set(DataComponents.ENCHANTMENTS, upgraded.toImmutable());
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.crystalnexus.hemochanter"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new HemochanterMenu(id, inventory, this); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); stacks = NonNullList.withSize(1, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, stacks, registries); if (tag.get("blood") instanceof CompoundTag blood) bloodTank.readFromNBT(registries, blood); if (tag.get("energy") instanceof IntTag stored) energy.deserializeNBT(registries, stored); progress = tag.getInt("progress"); operationLevel = tag.getInt("operationLevel"); operationTicks = processingTicks(operationLevel); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, stacks, registries); tag.put("blood", bloodTank.writeToNBT(registries, new CompoundTag())); tag.put("energy", energy.serializeNBT(registries)); tag.putInt("progress", progress); tag.putInt("operationLevel", operationLevel); }
    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
    @Override public int getContainerSize() { return 1; }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { this.stacks = stacks; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && accepts(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[] { 0 }; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
}
