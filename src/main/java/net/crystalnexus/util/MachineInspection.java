package net.crystalnexus.util;

import java.util.ArrayList;
import java.util.List;
import net.crystalnexus.block.*;
import net.crystalnexus.block.entity.*;
import net.crystalnexus.item.CrystalWrenchItem;
import net.minecraft.core.Direction;
import net.crystalnexus.processing.TieredMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

/** A bounded, read-only view of local machine storage. Never executes processing logic. */
public record MachineInspection(Component name, int tier, boolean inventory, List<ItemEntry> items,
        int extraItems, boolean energy, long storedEnergy, long energyCapacity,
        boolean fluids, List<FluidEntry> tanks, int extraTanks, double progress, List<Component> wrenchStatus) {
    public static final int MAX_ITEMS = 8, MAX_TANKS = 4, MAX_WRENCH_LINES = 8;
    public record ItemEntry(ItemStack icon, int count) {}
    public record FluidEntry(Component name, int amount, int capacity) {}

    public static double fraction(double value, double maximum) {
        return !Double.isFinite(value) || !Double.isFinite(maximum) || maximum <= 0
            ? -1 : Math.clamp(value / maximum, 0, 1);
    }

    public static MachineInspection read(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        var wrenchStatus = wrenchStatus(level, pos);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("crystalnexus"))
            return wrenchStatus.isEmpty() ? null : statusOnly(state.getBlock().getName(), wrenchStatus);
        boolean networkInventory = be instanceof net.crystalnexus.block.entity.DepotControllerBlockEntity
            || be instanceof net.crystalnexus.block.entity.DepotCliBlockEntity
            || be instanceof net.crystalnexus.block.entity.DepotCableBlockEntity;
        var itemHandler = networkInventory ? null : level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        var fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        // Container is the local inventory; capabilities may expose a network view.
        Container container = be instanceof Container c ? c : null;
        boolean hasInventory = container != null || itemHandler != null;
        var tag = be == null ? new net.minecraft.nbt.CompoundTag() : be.getPersistentData();
        double progress = tag.contains("progress") && tag.contains("maxProgress")
            ? fraction(tag.getDouble("progress"), tag.getDouble("maxProgress")) : -1;
        if (be instanceof MasticatorBlockEntity m) progress = fraction(m.data().get(0), m.data().get(1));
        if (!hasInventory && energy == null && fluid == null && progress < 0 && wrenchStatus.isEmpty()) return null;
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
        int tier = state.getBlock() instanceof TieredMachineBlock t ? t.machineTier().level() : -1;
        return new MachineInspection(state.getBlock().getName(), tier, hasInventory, List.copyOf(items),
            Math.max(0, occupied - MAX_ITEMS), energy != null, energy == null ? 0 : energy.getEnergyStored(),
            energy == null ? 0 : energy.getMaxEnergyStored(), fluid != null, List.copyOf(tanks),
            Math.max(0, tankCount - MAX_TANKS), progress,
            wrenchStatus);
    }
    public static boolean canInspect(Level level, BlockPos pos) {
        return CrystalWrenchItem.canRotate(level, pos) || CrystalWrenchItem.canPickup(level, pos)
            || level.getBlockEntity(pos) != null && BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                .getNamespace().equals("crystalnexus");
    }

    private static MachineInspection statusOnly(Component name, List<Component> lines) {
        return new MachineInspection(name, -1, false, List.of(), 0, false, 0, 0,
            false, List.of(), 0, -1, List.copyOf(lines));
    }

    public static MachineInspection read(LUNADroneEntity drone, Player viewer) {
        if (!drone.isAlive()) return null;
        if (!drone.ownedBy(viewer)) {
            return statusOnly(drone.getName(), List.of(Component.literal(boundedStatus(drone.displayStatus())),
                Component.translatable(drone.owner == null ? "gui.crystalnexus.scanner.drone_claim"
                    : "gui.crystalnexus.scanner.drone_owner_only")));
        }
        List<Component> lines = List.of(
            Component.translatable("gui.crystalnexus.router.mode", Component.translatable(
                "gui.crystalnexus.scanner.drone_mode." + drone.mode.name().toLowerCase(java.util.Locale.ROOT))),
            Component.translatable("gui.crystalnexus.scanner.drone_task", boundedStatus(drone.status)),
            Component.translatable("gui.crystalnexus.scanner.drone_health", (int)Math.ceil(drone.getHealth()), (int)Math.ceil(drone.getMaxHealth())),
            Component.translatable("gui.crystalnexus.scanner.drone_open"),
            Component.translatable("gui.crystalnexus.scanner.drone_cycle"),
            Component.translatable("gui.crystalnexus.scanner.drone_cycle_warning"));
        List<ItemEntry> items = new ArrayList<>();
        int occupied = 0;
        for (int i = 0; i < drone.inventory.getSlots(); i++) {
            var stack = drone.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            occupied++;
            if (items.size() < MAX_ITEMS) items.add(new ItemEntry(stack.copyWithCount(1), stack.getCount()));
        }
        List<FluidEntry> tanks = new ArrayList<>();
        for (var tank : List.of(drone.tank, drone.recoveryTank)) {
            var fluid = tank.getFluid();
            Component name = fluid.isEmpty() ? Component.translatable("gui.crystalnexus.scanner.empty") : fluid.getHoverName();
            if (tank == drone.recoveryTank) name = Component.translatable("gui.crystalnexus.scanner.drone_recovery", name);
            tanks.add(new FluidEntry(name, tank.getFluidAmount(), tank.getCapacity()));
        }
        return new MachineInspection(drone.getName(), -1, true, List.copyOf(items), Math.max(0, occupied - MAX_ITEMS),
            true, drone.battery.getEnergyStored(), drone.battery.getMaxEnergyStored(), true, List.copyOf(tanks), 0, -1, lines);
    }

    private static String boundedStatus(String status) {
        return status.length() > 256 ? status.substring(0, 253) + "..." : status;
    }

    private static List<Component> wrenchStatus(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        var be = level.getBlockEntity(pos);
        List<Component> lines = new ArrayList<>();
        Component mode = null;
        if (be instanceof ItemRouterBlockEntity router && !router.isMerger()) mode = router.mode().displayName();
        else if (block instanceof ConveyerBeltBlock || block instanceof ConveyerBeltInputBlock || block instanceof ConveyerBeltOutputBlock)
            mode = Component.translatable("gui.crystalnexus.wrench.mode." + ConveyerBeltMode.modeAt(state).name().toLowerCase(java.util.Locale.ROOT));
        else if (block instanceof DepotCableBlock)
            mode = Component.translatable("gui.crystalnexus.wrench.mode." + state.getValue(DepotCableBlock.MODE).getSerializedName());
        if (mode != null) {
            lines.add(Component.translatable("gui.crystalnexus.router.mode", mode));
            lines.add(Component.translatable("gui.crystalnexus.router.cycle"));
        } else if (be instanceof PipeStraightBlockEntity pipe) {
            for (Direction side : Direction.values()) {
                String sideMode = pipe.isInputSide(side) ? "input" : pipe.isOutputSide(side) ? "output" : "default";
                lines.add(Component.translatable("gui.crystalnexus.wrench.pipe_side",
                    Component.translatable("gui.crystalnexus.wrench.side." + side.getName()),
                    Component.translatable("gui.crystalnexus.wrench.mode." + sideMode)));
            }
            lines.add(Component.translatable("gui.crystalnexus.wrench.pipe_cycle"));
        } else if (CrystalWrenchItem.canRotate(level, pos)) {
            lines.add(Component.translatable("gui.crystalnexus.wrench.rotate"));
        }
        if (!(block instanceof PipeStraightBlock) && CrystalWrenchItem.canPickup(level, pos))
            lines.add(Component.translatable("gui.crystalnexus.wrench.dismantle"));
        return List.copyOf(lines);
    }

}
