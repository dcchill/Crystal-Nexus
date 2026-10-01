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

    public static List<Build> candidates(int requestedSize) {
        int size = Mth.clamp(requestedSize, MIN_SIZE, MAX_SIZE);
        List<Build> builds = new ArrayList<>();
        // ponytail: bounded stripe search, not a global optimum; add local search if more layout variety is needed.
        for (int spacing = 1; spacing <= 2; spacing++) {
            for (int rows = 1; rows <= 3; rows++) {
                for (int offset = 0; offset < 2; offset++) {
                    Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
                    for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, size - 1, size - 1, size - 1)) {
                        if (pos.getX() == 0 || pos.getY() == 0 || pos.getZ() == 0
                                || pos.getX() == size - 1 || pos.getY() == size - 1 || pos.getZ() == size - 1)
                            blocks.put(pos.immutable(), CrystalnexusModBlocks.REACTOR_BLOCK.get().defaultBlockState());
                    }
                    for (int z = 1; z < size - 1; z++) for (int x = 1; x < size - 1; x++) {
                        Block block = (z - 1) % (rows + 1) == 0 ? CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get()
                                : (x + offset) % spacing == 0 ? CrystalnexusModBlocks.REACTOR_CORE.get()
                                : CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get();
                        for (int y = 1; y < size - 1; y++) blocks.put(new BlockPos(x, y, z), block.defaultBlockState());
                        if (block == CrystalnexusModBlocks.REACTOR_CORE.get())
                            blocks.put(new BlockPos(x, size - 1, z), CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get().defaultBlockState());
                        if (block == CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get())
                            blocks.put(new BlockPos(x, 0, z), CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get().defaultBlockState());
                    }
                    blocks.put(new BlockPos(1, 1, 0), CrystalnexusModBlocks.REACTOR_COMPUTER.get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
                    blocks.put(new BlockPos(2, 1, 0), CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get().defaultBlockState());
                    blocks.put(new BlockPos(1, 2, 0), CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get().defaultBlockState());
                    blocks.put(new BlockPos(0, 1, 1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get().defaultBlockState());
                    blocks.put(new BlockPos(0, 2, 1), CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get().defaultBlockState());
                    Build view = new Build(size, Map.copyOf(blocks), null);
                    ReactorLayout layout = ReactorLayout.analyze(view, BlockPos.ZERO, new BlockPos(size - 1, size - 1, size - 1));
                    if (layout.valid) builds.add(new Build(size, view.blocks(), layout));
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

    public static Estimate optimize(List<Build> builds, int efficiencyPercent, int speedPercent) {
        List<Estimate> estimates = builds.stream().map(build -> estimate(build, speedPercent)).toList();
        double maxOutput = estimates.stream().mapToDouble(Estimate::fePerTick).max().orElse(1);
        double maxEfficiency = builds.stream().mapToDouble(build -> build.layout().fuelEfficiency).max().orElse(1);
        double weight = Mth.clamp(efficiencyPercent, 0, 100) / 100.0;
        Estimate best = null;
        double bestScore = -1;
        for (Estimate estimate : estimates) {
            double score = (1 - weight) * estimate.fePerTick() / Math.max(1, maxOutput)
                    + weight * estimate.build().layout().fuelEfficiency / maxEfficiency;
            if (best == null || score > bestScore || (score == bestScore && estimate.fePerTick() > best.fePerTick())) {
                best = estimate;
                bestScore = score;
            }
        }
        return best;
    }
}
