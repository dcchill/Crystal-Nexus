package net.crystalnexus.multiblock;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record CryogenicFreezerLayout(boolean valid, String reason, int coolingCoils,
		List<BlockPos> fluidInputs, List<BlockPos> fluidOutputs, List<BlockPos> itemInputs, List<BlockPos> itemOutputs,
		List<BlockPos> energyInputs) {
	private static final int MAX_SHELL_BLOCKS = 32_768;
	private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

	public static CryogenicFreezerLayout invalid(String reason) {
		return new CryogenicFreezerLayout(false, reason, 0, List.of(), List.of(), List.of(), List.of(), List.of());
	}

	public static CryogenicFreezerLayout analyze(ServerLevel level, BlockPos controller) {
		Set<BlockPos> connected = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		connected.add(controller);
		queue.add(controller);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (isShellBlock(level.getBlockState(next).getBlock()) && connected.add(next)) {
					if (connected.size() > MAX_SHELL_BLOCKS) return invalid("Structure is too large");
					queue.add(next);
				}
			}
		}

		int minX = connected.stream().mapToInt(BlockPos::getX).min().orElse(controller.getX());
		int minY = connected.stream().mapToInt(BlockPos::getY).min().orElse(controller.getY());
		int minZ = connected.stream().mapToInt(BlockPos::getZ).min().orElse(controller.getZ());
		int maxX = connected.stream().mapToInt(BlockPos::getX).max().orElse(controller.getX());
		int maxY = connected.stream().mapToInt(BlockPos::getY).max().orElse(controller.getY());
		int maxZ = connected.stream().mapToInt(BlockPos::getZ).max().orElse(controller.getZ());
		if (maxX - minX + 1 < 5 || maxZ - minZ + 1 < 5 || maxY - minY + 1 < 3)
			return invalid("Minimum exterior size is 5x3x5");

		int hatches = 0;
		for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
			boolean shell = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
			if (!shell) continue;
			Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
			if (!isShellBlock(block)) return invalid("Incomplete insulated titanium shell");
			if (block == CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get()) hatches++;
		}
		if (hatches != 1) return invalid("Structure requires exactly one freezer hatch");

		int coils = 0;
		for (int x = minX + 1; x < maxX; x++) for (int z = minZ + 1; z < maxZ; z++) {
			boolean pillar = level.getBlockState(new BlockPos(x, minY + 1, z)).is(CrystalnexusModBlocks.COOLING_COIL.get());
			for (int y = minY + 1; y < maxY; y++) {
				BlockPos pos = new BlockPos(x, y, z);
				Block block = level.getBlockState(pos).getBlock();
				if (pillar) {
					if (block != CrystalnexusModBlocks.COOLING_COIL.get()) return invalid("Cooling coils must form full-height pillars");
					for (Direction direction : HORIZONTAL)
						if (!level.getBlockState(pos.relative(direction)).isAir()) return invalid("Cooling-coil pillars need air on every horizontal side");
					coils++;
				} else if (!level.getBlockState(pos).isAir()) return invalid("Only air and cooling-coil pillars are allowed inside");
			}
		}
		if (coils == 0) return invalid("At least one cooling-coil pillar is required");

		return new CryogenicFreezerLayout(true, "Formed", coils,
			positions(level, connected, CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get()),
			positions(level, connected, CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get()),
			positions(level, connected, CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get()),
			positions(level, connected, CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get()),
			positions(level, connected, CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get()));
	}

	public static boolean isShellBlock(Block block) {
		return block == CrystalnexusModBlocks.INSULATED_TITANIUM_CASING.get()
			|| block == CrystalnexusModBlocks.CRYOGENIC_FLASH_FREEZER_HATCH.get()
			|| block == CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get()
			|| block == CrystalnexusModBlocks.MACHINE_ENERGY_OUTPUT.get()
			|| block == CrystalnexusModBlocks.MACHINE_FLUID_INPUT.get()
			|| block == CrystalnexusModBlocks.MULTIBLOCK_FLUID_OUTPUT.get()
			|| block == CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get()
			|| block == CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get();
	}

	private static List<BlockPos> positions(ServerLevel level, Set<BlockPos> shell, Block block) {
		return shell.stream().filter(pos -> level.getBlockState(pos).is(block)).map(BlockPos::immutable).toList();
	}
}
