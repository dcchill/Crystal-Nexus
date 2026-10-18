package net.crystalnexus.util;

import java.util.ArrayList;
import java.util.List;
import net.crystalnexus.block.entity.MasticatorBlockEntity;
import net.crystalnexus.processing.TieredMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/** A bounded, read-only view of local machine storage. Never executes processing logic. */
public record MachineInspection(Component name, int tier, boolean inventory, List<ItemEntry> items,
        int extraItems, boolean energy, long storedEnergy, long energyCapacity,
        boolean fluids, List<FluidEntry> tanks, int extraTanks, double progress) {
    public static final int MAX_ITEMS = 8, MAX_TANKS = 4;
    public record ItemEntry(ItemStack icon, int count) {}
    public record FluidEntry(Component name, int amount, int capacity) {}

    public static double fraction(double value, double maximum) {
        return !Double.isFinite(value) || !Double.isFinite(maximum) || maximum <= 0
            ? -1 : Math.clamp(value / maximum, 0, 1);
    }

    public static MachineInspection read(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null || !BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace().equals("crystalnexus")) return null;
        boolean networkInventory = be instanceof net.crystalnexus.block.entity.DepotControllerBlockEntity
            || be instanceof net.crystalnexus.block.entity.DepotCliBlockEntity
            || be instanceof net.crystalnexus.block.entity.DepotCableBlockEntity;
        var itemHandler = networkInventory ? null : level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        var fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        // Container is the local inventory; capabilities may expose a network view.
        Container container = be instanceof Container c ? c : null;
        boolean hasInventory = container != null || itemHandler != null;
        var tag = be.getPersistentData();
        double progress = tag.contains("progress") && tag.contains("maxProgress")
            ? fraction(tag.getDouble("progress"), tag.getDouble("maxProgress")) : -1;
        if (be instanceof MasticatorBlockEntity m) progress = fraction(m.data().get(0), m.data().get(1));
        if (!hasInventory && energy == null && fluid == null && progress < 0) return null;
        List<ItemEntry> items = new ArrayList<>();
        int occupied = 0;
        int slots = !hasInventory ? 0 : container != null ? container.getContainerSize() : itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = container != null ? container.getItem(i) : itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            occupied++;
            if (items.size() < MAX_ITEMS) items.add(new ItemEntry(stack.copyWithCount(1), stack.getCount()));
        }
        List<FluidEntry> tanks = new ArrayList<>();
        int tankCount = fluid == null ? 0 : fluid.getTanks();
        for (int i = 0; i < Math.min(MAX_TANKS, tankCount); i++) {
            var stack = fluid.getFluidInTank(i);
            tanks.add(new FluidEntry(stack.isEmpty() ? Component.translatable("gui.crystalnexus.scanner.empty") : stack.getHoverName(),
                stack.getAmount(), fluid.getTankCapacity(i)));
        }
        int tier = be.getBlockState().getBlock() instanceof TieredMachineBlock t ? t.machineTier().level() : -1;
        return new MachineInspection(be.getBlockState().getBlock().getName(), tier, hasInventory, List.copyOf(items),
            Math.max(0, occupied - MAX_ITEMS), energy != null, energy == null ? 0 : energy.getEnergyStored(),
            energy == null ? 0 : energy.getMaxEnergyStored(), fluid != null, List.copyOf(tanks),
            Math.max(0, tankCount - MAX_TANKS), progress);
    }
}
