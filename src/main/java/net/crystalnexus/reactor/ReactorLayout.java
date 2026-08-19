package net.crystalnexus.reactor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	public final int controlRodInsertion;
	public final int coolantChannels;
	public final int activeCoolantChannels;
	public final int coolantCapacityMbT;
	public final double outputMultiplier;
	public final double heatMultiplier;
	public final double fuelEfficiency;
	public final double conductorCoolingAccess;
	public final int hash;

	private ReactorLayout(boolean valid, String reason, int radius, int fuelRods, int fuelColumns, int coolantChannels, int activeCoolantChannels,
			double outputMultiplier, double heatMultiplier, double fuelEfficiency, double conductorCoolingAccess, int hash) {
		this.valid = valid;
		this.reason = reason;
		this.radius = radius;
		this.fuelRods = fuelRods;
		this.fuelColumns = fuelColumns;
		this.controlRodInsertion = 0;
		this.coolantChannels = coolantChannels;
		this.activeCoolantChannels = activeCoolantChannels;
		this.coolantCapacityMbT = activeCoolantChannels * ReactorBalance.COOLANT_PER_CHANNEL_MB_T;
		this.outputMultiplier = outputMultiplier;
		this.heatMultiplier = heatMultiplier;
		this.fuelEfficiency = fuelEfficiency;
		this.conductorCoolingAccess = conductorCoolingAccess;
		this.hash = hash;
	}

	public static ReactorLayout invalid(String reason) {
		return new ReactorLayout(false, reason, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
	}

	public static ReactorLayout analyze(LevelAccessor world, BlockPos center, int radius) {
		Set<BlockPos> fuel = new HashSet<>();
		Set<BlockPos> columns = new HashSet<>();
		Set<BlockPos> coolant = new HashSet<>();
		Set<BlockPos> conductors = new HashSet<>();
		int hash = 1;
		double output = 0;
		double heat = 0;
		double efficiency = 1;
		for (int dx = -radius + 1; dx <= radius - 1; dx++) {
			for (int dy = -radius + 1; dy <= radius - 1; dy++) {
				for (int dz = -radius + 1; dz <= radius - 1; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
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
		// Enforce the vertical column rule: every interior (x,z) column must be a
		// single, uniform component type (no mixing of core/coolant/conductor/etc per column).
		// Air, ports, control rods and other shell blocks are not valid interior components.
		for (int dx = -radius + 1; dx <= radius - 1; dx++) {
			for (int dz = -radius + 1; dz <= radius - 1; dz++) {
				Block columnType = null;
				for (int dy = -radius + 1; dy <= radius - 1; dy++) {
					BlockPos pos = center.offset(dx, dy, dz);
					Block block = world.getBlockState(pos).getBlock();
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
			BlockPos controlRod = new BlockPos(column.getX(), center.getY() + radius, column.getZ());
			if (world.getBlockState(controlRod).getBlock() != CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get()) {
				return invalid("Fuel column missing roof control rod");
			}
		}
		// Every control rod on the top shell must have a reactor core column directly below it.
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos rodPos = new BlockPos(center.getX() + dx, center.getY() + radius, center.getZ() + dz);
				if (world.getBlockState(rodPos).getBlock() != CrystalnexusModBlocks.REACTOR_CONTROL_ROD.get()) {
					continue;
				}
				for (int dy = -radius + 1; dy <= radius - 1; dy++) {
					BlockPos below = new BlockPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (world.getBlockState(below).getBlock() != CrystalnexusModBlocks.REACTOR_CORE.get()) {
						return invalid("Control rod at " + rodPos.toShortString() + " has no reactor core column below it");
					}
				}
			}
		}
		Set<BlockPos> activeCoolant = findActiveCoolant(world, center, radius, coolant, conductors);
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
				} else if (near == CrystalnexusModBlocks.REACTOR_GRAPHITE_MODERATOR.get() && far == CrystalnexusModBlocks.REACTOR_CORE.get()) {
					rodOutput += ReactorBalance.MODERATED_FUEL_OUTPUT;
					rodHeat += ReactorBalance.MODERATED_FUEL_HEAT;
					rodEfficiency += ReactorBalance.MODERATED_FUEL_EFFICIENCY;
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
			efficiency += Math.max(0.25, rodEfficiency) - 1;
		}
		double conductorAccess = 0;
		for (BlockPos rod : fuel) {
			conductorAccess += hasCoolingPath(rod, activeCoolant, conductors) ? 1 : 0.35;
		}
		return new ReactorLayout(true, "Stable", radius, fuel.size(), columns.size(), coolant.size(), activeCoolant.size(),
				output / fuel.size(), heat / fuel.size(), Math.max(0.25, efficiency / fuel.size()), conductorAccess / fuel.size(), hash);
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
				|| block == CrystalnexusModBlocks.REACTOR_GRAPHITE_MODERATOR.get()
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

	private static Set<BlockPos> findActiveCoolant(LevelAccessor world, BlockPos center, int radius, Set<BlockPos> coolant, Set<BlockPos> conductors) {
		Set<BlockPos> active = new HashSet<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos coolantPos : coolant) {
			if (touchesFluidInput(world, coolantPos, center, radius) || hasSideFluidInput(world, coolantPos, center, radius)) {
				queue.add(coolantPos);
				seen.add(coolantPos);
			}
		}
		while (!queue.isEmpty()) {
			BlockPos pos = queue.removeFirst();
			if (coolant.contains(pos)) {
				active.add(pos);
			}
			for (Direction direction : ALL) {
				BlockPos next = pos.relative(direction);
				if (!seen.contains(next) && (coolant.contains(next) || conductors.contains(next))) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return active;
	}

	private static boolean touchesFluidInput(LevelAccessor world, BlockPos pos, BlockPos center, int radius) {
		for (Direction direction : ALL) {
			BlockPos next = pos.relative(direction);
			if (Math.abs(next.getX() - center.getX()) == radius || Math.abs(next.getY() - center.getY()) == radius || Math.abs(next.getZ() - center.getZ()) == radius) {
				if (world.getBlockState(next).getBlock() == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get()) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasSideFluidInput(LevelAccessor world, BlockPos pos, BlockPos center, int radius) {
		for (Direction direction : HORIZONTAL) {
			BlockPos next = pos.relative(direction);
			if (Math.abs(next.getX() - center.getX()) < radius && Math.abs(next.getY() - center.getY()) < radius && Math.abs(next.getZ() - center.getZ()) < radius) {
				if (world.getBlockState(next).getBlock() == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Valid interior column components. Air, ports, control rods and other shell
	 * blocks are NOT valid interior components.
	 */
	private static boolean isInteriorComponent(Block block) {
		return block == CrystalnexusModBlocks.REACTOR_CORE.get()
				|| block == CrystalnexusModBlocks.REACTOR_COOLANT_CHANNEL.get()
				|| block == CrystalnexusModBlocks.REACTOR_NEUTRON_REFLECTOR.get()
				|| block == CrystalnexusModBlocks.REACTOR_HEAT_CONDUCTOR.get();
	}

	private static boolean hasCoolingPath(BlockPos rod, Set<BlockPos> activeCoolant, Set<BlockPos> conductors) {
		List<BlockPos> frontier = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		for (Direction direction : ALL) {
			BlockPos next = rod.relative(direction);
			if (activeCoolant.contains(next)) {
				return true;
			}
			if (conductors.contains(next)) {
				frontier.add(next);
				seen.add(next);
			}
		}
		for (int depth = 0; depth < ReactorBalance.CONDUCTOR_RANGE && !frontier.isEmpty(); depth++) {
			List<BlockPos> nextFrontier = new ArrayList<>();
			for (BlockPos pos : frontier) {
				for (Direction direction : ALL) {
					BlockPos next = pos.relative(direction);
					if (activeCoolant.contains(next)) {
						return true;
					}
					if (!seen.contains(next) && conductors.contains(next)) {
						seen.add(next);
						nextFrontier.add(next);
					}
				}
			}
			frontier = nextFrontier;
		}
		return false;
	}
}
