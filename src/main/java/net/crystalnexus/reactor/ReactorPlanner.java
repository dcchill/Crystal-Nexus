package net.crystalnexus.reactor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.ReactorFuelCellItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/** Buildable column layouts, evaluated by the same analyzer as placed reactors. */
public final class ReactorPlanner {
    public static final int MIN_SIZE = 4;
    public static final int MAX_SIZE = 17;

    private ReactorPlanner() { }

    public record Build(int size, Map<BlockPos, BlockState> blocks, ReactorLayout layout) implements BlockGetter {
        @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public int getHeight() { return size; }
        @Override public int getMinBuildHeight() { return 0; }
    }

    public record Estimate(Build build, int fePerTick, int coolantDemand, double operatingRate, double burnPerTick) { }

    private static Block[] components() {
        // Stable IDs used by saved plans and the network packet.
        return new Block[] { CrystalnexusModBlocks.REACTOR_CORE.get(), CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get(),
                CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get(), CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get(),
                CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get(), Blocks.AIR };
    }

    public static byte[] columns(Build build) {
        int width = build.size() - 2;
        byte[] columns = new byte[width * width];
        List<Block> components = List.of(components());
        for (int z = 0; z < width; z++) for (int x = 0; x < width; x++)
            columns[z * width + x] = (byte) components.indexOf(build.getBlockState(new BlockPos(x + 1, 1, z + 1)).getBlock());
        return columns;
    }

    /** Reconstruct a bounded, whitelisted design; never accept arbitrary blocks or block-entity data from clients. */
    public static Build fromColumns(int size, byte[] columns) {
        if (size < MIN_SIZE || size > MAX_SIZE || columns.length != (size - 2) * (size - 2))
            throw new IllegalArgumentException("Invalid reactor plan size");
        Block[] components = components();
        for (byte column : columns) if (column < 0 || column >= components.length)
            throw new IllegalArgumentException("Invalid reactor component");
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, size - 1, size - 1, size - 1)) {
            Block block = CrystalnexusModBlocks.REACTOR_BLOCK.get();
            if (pos.getX() > 0 && pos.getX() < size - 1 && pos.getZ() > 0 && pos.getZ() < size - 1) {
                Block column = components[columns[(pos.getZ() - 1) * (size - 2) + pos.getX() - 1]];
                if (pos.getY() > 0 && pos.getY() < size - 1) block = column;
                if (pos.getY() == size - 1 && column == CrystalnexusModBlocks.REACTOR_CORE.get())
                    block = CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get();
            }
            blocks.put(pos.immutable(), block.defaultBlockState());
        }
        blocks.put(new BlockPos(1, 1, 0), CrystalnexusModBlocks.REACTOR_COMPUTER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        blocks.put(new BlockPos(2, 1, 0), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get().defaultBlockState());
        blocks.put(new BlockPos(1, 2, 0), CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get().defaultBlockState());
        blocks.put(new BlockPos(2, 2, 0), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState());
        blocks.put(new BlockPos(0, 1, 1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get().defaultBlockState());
        blocks.put(new BlockPos(0, 2, 1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get().defaultBlockState());
        Build view = new Build(size, Map.copyOf(blocks), null);
        return new Build(size, view.blocks(), ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(size - 1, size - 1, size - 1)));
    }

    public static List<Build> candidates(int requestedSize) {
        int size = Mth.clamp(requestedSize, MIN_SIZE, MAX_SIZE);
        List<Build> builds = new ArrayList<>();
        for (int spacing = 1; spacing <= 2; spacing++) {
            for (int rows = 1; rows <= 4; rows++) {
                for (int offset = 0; offset < (spacing == 1 ? 1 : 6); offset++) {
                    byte[] columns = new byte[(size - 2) * (size - 2)];
                    for (int z = 1; z < size - 1; z++) for (int x = 1; x < size - 1; x++)
                        columns[(z - 1) * (size - 2) + x - 1] = (byte) ((z - 1) % (rows + 1) == 0 ? 4
                                : (x + offset) % spacing == 0 ? 0 : 1 + offset / 2);
                    Build build = fromColumns(size, columns);
                    if (build.layout().valid && build.layout().activeCoolantChannels > 0) builds.add(build);
                }
            }
        }
        return List.copyOf(builds);
    }
    /** Full normal fuel cells, water, no upgrade, at the cooling target temperature. */
    public static Estimate estimate(Build build, int speedPercent) {
        ReactorLayout layout = build.layout();
        ReactorFuelCellItem fuel = (ReactorFuelCellItem) CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get();
        double rate = Mth.clamp(speedPercent, 0, 100) / 100.0;
        double heat = ReactorBalance.BASE_HEAT_PER_ROD_T * layout.fuelRods * layout.heatMultiplier * rate
                * fuel.heatMultiplier() * (1 + (layout.fuelRods - layout.fuelColumns) * 0.08);
        int demand = ReactorSimulation.runningCoolantDemand(ReactorBalance.TARGET_TEMPERATURE, heat, 1);
        double cooling = demand == 0 ? 1 : Math.max(ReactorBalance.MIN_OPERATING_FACTOR,
                Math.min(1, layout.coolantCapacityMbT / (double) demand));
        int unthrottled = (int) Math.round(ReactorBalance.BASE_FE_PER_ROD_T * layout.fuelRods * layout.outputMultiplier
                * rate * fuel.feMultiplier() * layout.fuelEfficiency * ReactorSimulation.temperatureCurve(ReactorBalance.TARGET_TEMPERATURE));
        return new Estimate(build, (int) Math.round(unthrottled * cooling), demand, rate * cooling,
                layout.fuelRods * rate * cooling / layout.fuelEfficiency * ReactorBalance.FUEL_BURN_RATE_MULTIPLIER);
    }

    /** Energy obtained per unit of fuel wear; includes reflector output and moderation. */
    public static double energyPerFuel(Estimate estimate) {
        if (estimate.burnPerTick() > 0) return estimate.fePerTick() / estimate.burnPerTick();
        ReactorLayout layout = estimate.build().layout();
        ReactorFuelCellItem fuel = (ReactorFuelCellItem) CrystalnexusModItems.BLUTONIUM_FUEL_CELL.get();
        return ReactorBalance.BASE_FE_PER_ROD_T * fuel.feMultiplier() * layout.outputMultiplier
                * layout.fuelEfficiency * layout.fuelEfficiency
                * ReactorSimulation.temperatureCurve(ReactorBalance.TARGET_TEMPERATURE) / ReactorBalance.FUEL_BURN_RATE_MULTIPLIER;
    }

    private record Objective(double weight, double outputScale, double economyScale) {
        double score(Estimate estimate) {
            return (1 - weight) * estimate.fePerTick() / outputScale + weight * energyPerFuel(estimate) / economyScale;
        }

        int compare(Estimate a, Estimate b) {
            boolean aCooled = a.coolantDemand() <= a.build().layout().coolantCapacityMbT;
            boolean bCooled = b.coolantDemand() <= b.build().layout().coolantCapacityMbT;
            int comparison = Boolean.compare(aCooled, bCooled);
            if (comparison == 0) comparison = Double.compare(score(a), score(b));
            if (comparison == 0) comparison = Integer.compare(a.fePerTick(), b.fePerTick());
            if (comparison == 0) comparison = Integer.compare(b.coolantDemand(), a.coolantDemand());
            return comparison;
        }
    }

    private static Objective objective(List<Estimate> estimates, int efficiencyPercent) {
        return new Objective(Mth.clamp(efficiencyPercent, 0, 100) / 100.0,
                Math.max(1, estimates.stream().mapToInt(Estimate::fePerTick).max().orElse(1)),
                Math.max(1, estimates.stream().mapToDouble(ReactorPlanner::energyPerFuel).max().orElse(1)));
    }

    /** Quick initial result while the design search runs. */
    public static Estimate select(List<Build> builds, int efficiencyPercent, int speedPercent) {
        List<Estimate> estimates = builds.stream().map(build -> estimate(build, speedPercent)).toList();
        return estimates.stream().max(objective(estimates, efficiencyPercent)::compare).orElseThrow();
    }

    /** Search component placements, using live cooling connectivity and fuel/output rules for every trial. */
    public static Estimate optimize(List<Build> builds, int efficiencyPercent, int speedPercent) {
        List<Estimate> estimates = builds.stream().map(build -> estimate(build, speedPercent)).toList();
        Objective objective = objective(estimates, efficiencyPercent);
        List<Estimate> starts = estimates.stream().sorted(objective::compare).toList().reversed();
        Estimate best = starts.getFirst();
        Block[] components = components();
        // ponytail: bounded multi-start local search; exhaustive global search grows as 6^((size-2)^2).
        for (Estimate start : starts.stream().limit(3).toList()) {
            Estimate current = start;
            int size = start.build().size();
            int budget = Math.max(600, (size - 2) * (size - 2) * components.length * 2);
            boolean improved = true;
            while (improved && budget > 0) {
                improved = false;
                for (int z = 1; z < size - 1 && budget > 0; z++) for (int x = 1; x < size - 1 && budget > 0; x++) {
                    for (Block component : components) {
                        if (current.build().getBlockState(new BlockPos(x, 1, z)).is(component)) continue;
                        if (--budget < 0) break;
                        Build trial = replaceColumn(current.build(), x, z, component);
                        if (!trial.layout().valid || trial.layout().activeCoolantChannels == 0) continue;
                        Estimate candidate = estimate(trial, speedPercent);
                        if (objective.compare(candidate, current) > 0) {
                            current = candidate;
                            improved = true;
                        }
                    }
                }
            }
            if (objective.compare(current, best) > 0) best = current;
        }
        return best;
    }

    private static Build replaceColumn(Build build, int x, int z, Block component) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>(build.blocks());
        int size = build.size();
        for (int y = 1; y < size - 1; y++) blocks.put(new BlockPos(x, y, z), component.defaultBlockState());
        blocks.put(new BlockPos(x, size - 1, z), (component == CrystalnexusModBlocks.REACTOR_CORE.get()
                ? CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get() : CrystalnexusModBlocks.REACTOR_BLOCK.get()).defaultBlockState());
        Build view = new Build(size, blocks, null);
        ReactorLayout layout = ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(size - 1, size - 1, size - 1));
        return new Build(size, Map.copyOf(blocks), layout);
    }
}
