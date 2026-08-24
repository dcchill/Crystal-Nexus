package net.crystalnexus.reactor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.crystalnexus.block.entity.ReactorControlRodBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ReactorLayout {
	private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	private static final Direction[] ALL = Direction.values();

	public final boolean valid;
	public final String reason;
	public final int radius;
	public final int fuelRods;
	public final int fuelColumns;
	public final int coolantChannels;
	public final int activeCoolantChannels;
	public final int coolantCapacityMbT;
	public final double outputMultiplier;
	public final double heatMultiplier;
	public final double fuelEfficiency;
	public final int hash;
	private final List<FuelColumn> fuelColumnData;

	private ReactorLayout(boolean valid, String reason, int radius, int fuelRods, int fuelColumns, int coolantChannels, int activeCoolantChannels,
			double outputMultiplier, double heatMultiplier, double fuelEfficiency, int hash, List<FuelColumn> fuelColumnData) {
		this.valid = valid;
		this.reason = reason;
		this.radius = radius;
		this.fuelRods = fuelRods;
		this.fuelColumns = fuelColumns;
		this.coolantChannels = coolantChannels;
		this.activeCoolantChannels = activeCoolantChannels;
		this.coolantCapacityMbT = activeCoolantChannels * ReactorBalance.COOLANT_PER_CHANNEL_MB_T;
		this.outputMultiplier = outputMultiplier;
		this.heatMultiplier = heatMultiplier;
		this.fuelEfficiency = fuelEfficiency;
		this.hash = hash;
		this.fuelColumnData = List.copyOf(fuelColumnData);
	}

	public static ReactorLayout invalid(String reason) {
		return new ReactorLayout(false, reason, 0, 0, 0, 0, 0, 0, 0, 1, 0, List.of());
	}

	public static ReactorLayout analyze(LevelAccessor world, BlockPos center, int radius) {
		return analyze(world, center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
	}

	public static ReactorLayout analyze(LevelAccessor world, BlockPos minBounds, BlockPos maxBounds) {
		Set<BlockPos> fuel = new HashSet<>();
		Set<BlockPos> columns = new HashSet<>();
		Set<BlockPos> coolant = new HashSet<>();
		Set<BlockPos> conductors = new HashSet<>();
		int hash = 1;
		double output = 0;
		double heat = 0;
		double efficiency = 0;
		Map<BlockPos, FuelColumn> fuelColumnData = new HashMap<>();
		for (int x = minBounds.getX() + 1; x < maxBounds.getX(); x++) {
			for (int y = minBounds.getY() + 1; y < maxBounds.getY(); y++) {
				for (int z = minBounds.getZ() + 1; z < maxBounds.getZ(); z++) {
					BlockPos pos = new BlockPos(x, y, z);
					Block block = world.getBlockState(pos).getBlock();
					hash = 31 * hash + BuiltInRegistries.BLOCK.getKey(block).hashCode();
					hash = 31 * hash + pos.hashCode();
					if (block == CrystalnexusModBlocks.REACTOR_CORE.get()) {
						fuel.add(pos);
						columns.add(new BlockPos(pos.getX(), 0, pos.getZ()));
					} else if (block == CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get()) {
						coolant.add(pos);
					} else if (block == CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get()) {
						conductors.add(pos);
					}
				}
			}
		}
		if (fuel.isEmpty()) {
			return invalid("Missing fuel rods");
		}
		// Enforce the vertical column rule: every interior (x,z) column's non-air
		// blocks must be one uniform, allowed component type.
		for (int x = minBounds.getX() + 1; x < maxBounds.getX(); x++) {
			for (int z = minBounds.getZ() + 1; z < maxBounds.getZ(); z++) {
				Block columnType = null;
				for (int y = minBounds.getY() + 1; y < maxBounds.getY(); y++) {
					BlockPos pos = new BlockPos(x, y, z);
					Block block = world.getBlockState(pos).getBlock();
					if (block == Blocks.AIR) {
						continue;
					}
					if (!isInteriorComponent(block)) {
						return invalid("Invalid interior block at " + pos.toShortString() + ": " + blockName(block));
					}
					if (columnType == null) {
						columnType = block;
					} else if (block != columnType) {
						return invalid("Interior column at " + pos.toShortString() + " mixes "
								+ blockName(columnType) + " and " + blockName(block));
					}
				}
			}
		}
		for (BlockPos column : columns) {
			BlockPos controlRod = new BlockPos(column.getX(), maxBounds.getY(), column.getZ());
			if (world.getBlockState(controlRod).getBlock() != CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get()) {
				return invalid("Fuel column missing roof control rod");
			}
		}
		// Every control rod on the top shell must have a reactor core column directly below it.
		for (int x = minBounds.getX(); x <= maxBounds.getX(); x++) {
			for (int z = minBounds.getZ(); z <= maxBounds.getZ(); z++) {
				BlockPos rodPos = new BlockPos(x, maxBounds.getY(), z);
				if (world.getBlockState(rodPos).getBlock() != CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get()) {
					continue;
				}
				for (int y = minBounds.getY() + 1; y < maxBounds.getY(); y++) {
					BlockPos below = new BlockPos(x, y, z);
					if (world.getBlockState(below).getBlock() != CrystalnexusModBlocks.REACTOR_CORE.get()) {
						return invalid("Control rod at " + rodPos.toShortString() + " has no reactor core column below it");
					}
				}
			}
		}
		Set<BlockPos> activeCoolant = findActiveCoolant(world, minBounds, maxBounds, fuel, coolant, conductors);
		for (BlockPos rod : fuel) {
			double rodOutput = 1;
			double rodHeat = 1;
			double rodEfficiency = 1;
			for (Direction direction : HORIZONTAL) {
				Block near = world.getBlockState(rod.relative(direction)).getBlock();
				Block far = world.getBlockState(rod.relative(direction, 2)).getBlock();
				if (near == CrystalnexusModBlocks.REACTOR_CORE.get()) {
					rodOutput += ReactorBalance.DIRECT_FUEL_OUTPUT;
					rodHeat += ReactorBalance.DIRECT_FUEL_HEAT;
					rodEfficiency -= 0.12;
				} else if (near == CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get() && far == CrystalnexusModBlocks.REACTOR_CORE.get()) {
					rodOutput += ReactorBalance.MODERATED_FUEL_OUTPUT;
					rodHeat += ReactorBalance.MODERATED_FUEL_HEAT * ReactorBalance.CARBON_MODERATOR_HEAT_REDUCTION;
					rodEfficiency += ReactorBalance.CARBON_MODERATOR_EFFICIENCY_BONUS;
				} else if (near == CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get()) {
					rodOutput += ReactorBalance.REFLECTOR_OUTPUT;
					rodHeat += ReactorBalance.REFLECTOR_HEAT;
				}
			}
			output += rodOutput;
			heat += rodHeat;
			efficiency += Math.max(0.25, rodEfficiency);
			BlockPos controlRodPos = new BlockPos(rod.getX(), maxBounds.getY(), rod.getZ());
			fuelColumnData.merge(controlRodPos, new FuelColumn(controlRodPos, 1, rodOutput, rodHeat), FuelColumn::merge);
		}
		int legacyRadius = Math.max(maxBounds.getX() - minBounds.getX() + 1,
				Math.max(maxBounds.getY() - minBounds.getY() + 1, maxBounds.getZ() - minBounds.getZ() + 1)) / 2;
		return new ReactorLayout(true, "Stable", legacyRadius, fuel.size(), columns.size(), coolant.size(), activeCoolant.size(),
				output / fuel.size(), heat / fuel.size(), Math.max(0.25, efficiency / fuel.size()), hash,
				fuelColumnData.values().stream().toList());
	}

	public OperatingTotals operatingTotals(LevelAccessor world) {
		double reactiveFuelRods = 0;
		double reactiveFuelColumns = 0;
		double output = 0;
		double heat = 0;
		for (FuelColumn column : fuelColumnData) {
			double reactivity = world.getBlockEntity(column.controlRodPos()) instanceof ReactorControlRodBlockEntity controlRod
					? controlRod.getReactivity()
					: 1.0;
			reactiveFuelRods += column.fuelRods() * reactivity;
			reactiveFuelColumns += reactivity;
			output += column.output() * reactivity;
			heat += column.heat() * reactivity;
		}
		return new OperatingTotals(reactiveFuelRods, reactiveFuelColumns, output, heat);
	}

	public record OperatingTotals(double reactiveFuelRods, double reactiveFuelColumns, double output, double heat) {
	}

	private record FuelColumn(BlockPos controlRodPos, int fuelRods, double output, double heat) {
		private FuelColumn merge(FuelColumn other) {
			return new FuelColumn(controlRodPos, fuelRods + other.fuelRods, output + other.output, heat + other.heat);
		}
	}

	private static String blockName(Block block) {
		if (block == null) {
			return "block";
		}
		return block.getName().getString();
	}

	public static boolean isInternalComponent(Block block) {
		return block == Blocks.AIR
				|| block == CrystalnexusModBlocks.REACTOR_CORE.get()
				|| block == CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get()
				|| block == CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT.get()
				|| block == CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get()
				|| block == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get()
				|| block == CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get();
	}

	public static boolean isControlRod(Block block) {
		return block == CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get();
	}

	private static Set<BlockPos> findActiveCoolant(LevelAccessor world, BlockPos minBounds, BlockPos maxBounds,
			Set<BlockPos> fuel, Set<BlockPos> coolant, Set<BlockPos> conductors) {
		Set<BlockPos> active = new HashSet<>();
		Set<BlockPos> remaining = new HashSet<>(coolant);
		Set<BlockPos> reachableConductors = reachableConductors(fuel, conductors);
		while (!remaining.isEmpty()) {
			Set<BlockPos> component = coolantComponent(remaining.iterator().next(), remaining, coolant);
			boolean supplied = component.stream().anyMatch(pos -> touchesFluidInput(world, pos, minBounds, maxBounds));
			if (!supplied) {
				continue;
			}
			for (BlockPos pos : component) {
				if (touchesAny(pos, fuel)) {
					active.add(pos);
				}
			}
			if (component.stream().anyMatch(pos -> touchesAny(pos, reachableConductors))) {
				active.addAll(component);
			}
		}
		return active;
	}

	private static Set<BlockPos> coolantComponent(BlockPos start, Set<BlockPos> remaining, Set<BlockPos> coolant) {
		Set<BlockPos> component = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		remaining.remove(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.removeFirst();
			component.add(pos);
			for (Direction direction : ALL) {
				BlockPos next = pos.relative(direction);
				if (coolant.contains(next) && remaining.remove(next)) {
					queue.add(next);
				}
			}
		}
		return component;
	}

	private static Set<BlockPos> reachableConductors(Set<BlockPos> fuel, Set<BlockPos> conductors) {
		Set<BlockPos> reached = new HashSet<>();
		Set<BlockPos> frontier = new HashSet<>();
		for (BlockPos rod : fuel) {
			for (Direction direction : ALL) {
				BlockPos next = rod.relative(direction);
				if (conductors.contains(next)) {
					frontier.add(next);
				}
			}
		}
		for (int distance = 1; distance <= ReactorBalance.CONDUCTOR_RANGE && !frontier.isEmpty(); distance++) {
			reached.addAll(frontier);
			Set<BlockPos> nextFrontier = new HashSet<>();
			for (BlockPos pos : frontier) {
				for (Direction direction : ALL) {
					BlockPos next = pos.relative(direction);
					if (conductors.contains(next) && !reached.contains(next)) {
						nextFrontier.add(next);
					}
				}
			}
			frontier = nextFrontier;
		}
		return reached;
	}

	private static boolean touchesAny(BlockPos pos, Set<BlockPos> targets) {
		for (Direction direction : ALL) {
			if (targets.contains(pos.relative(direction))) {
				return true;
			}
		}
		return false;
	}

	private static boolean touchesFluidInput(LevelAccessor world, BlockPos pos, BlockPos minBounds, BlockPos maxBounds) {
		for (Direction direction : ALL) {
			BlockPos next = pos.relative(direction);
			if (isShellPosition(next, minBounds, maxBounds)) {
				if (world.getBlockState(next).getBlock() == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Valid non-air interior column components. Ports, control rods and other
	 * shell blocks are not valid interior components.
	 */
	public static boolean isInteriorComponent(Block block) {
		return block == CrystalnexusModBlocks.REACTOR_CORE.get()
				|| block == CrystalnexusModBlocks.REACTOR_CARBON_MODERATOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get()
				|| block == CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get();
	}

	private static boolean isShellPosition(BlockPos pos, BlockPos minBounds, BlockPos maxBounds) {
		return pos.getX() >= minBounds.getX() && pos.getX() <= maxBounds.getX()
				&& pos.getY() >= minBounds.getY() && pos.getY() <= maxBounds.getY()
				&& pos.getZ() >= minBounds.getZ() && pos.getZ() <= maxBounds.getZ()
				&& (pos.getX() == minBounds.getX() || pos.getX() == maxBounds.getX()
						|| pos.getY() == minBounds.getY() || pos.getY() == maxBounds.getY()
						|| pos.getZ() == minBounds.getZ() || pos.getZ() == maxBounds.getZ());
	}

}
