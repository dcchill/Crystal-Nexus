package net.crystalnexus.procedures;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.reactor.ReactorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CenteredMultiblockValidator {
	private CenteredMultiblockValidator() {
	}

	public static Link validateFromController(LevelAccessor world, BlockPos controllerPos,
			Block core, Block controller, TagKey<Block> casingTag) {
		Link link = findStructure(world, controllerPos, core, controller, casingTag);
		setControllerState(world, controllerPos, link, link == null ? lastReason : "");
		storeLayout(world, link, core);
		return link;
	}

	public static Link validateFromCore(LevelAccessor world, BlockPos corePos,
			Block core, Block controller, TagKey<Block> casingTag) {
		Link link = findStructureFromCore(world, corePos, core, controller, casingTag);
		if (link != null) {
			setControllerState(world, link.pos, link, "");
			storeLayout(world, link, core);
		}
		return link;
	}

	public static Link validateFromPort(LevelAccessor world, BlockPos portPos,
			Block core, Block controller, TagKey<Block> casingTag) {
		BlockEntity portEntity = world.getBlockEntity(portPos);
		if (portEntity != null && portEntity.getPersistentData().contains("multiblockController")) {
			BlockPos cachedPos = BlockPos.of(portEntity.getPersistentData().getLong("multiblockController"));
			Link cached = validateFromPortController(world, portPos, cachedPos, core, controller, casingTag);
			if (cached != null) {
				return cached;
			}
			portEntity.getPersistentData().remove("multiblockController");
		}

		if (!world.getBlockState(portPos).is(casingTag)) {
			return null;
		}
		BlockPos controllerPos = findControllerOnShell(world, portPos, controller, casingTag);
		if (controllerPos == null) {
			return null;
		}
		Link link = validateFromPortController(world, portPos, controllerPos, core, controller, casingTag);
		if (link != null && portEntity != null) {
			portEntity.getPersistentData().putLong("multiblockController", controllerPos.asLong());
		}
		return link;
	}

	private static Link validateFromPortController(LevelAccessor world, BlockPos portPos,
			BlockPos controllerPos, Block core, Block controller, TagKey<Block> casingTag) {
		if (world.getBlockState(controllerPos).getBlock() != controller) {
			return null;
		}
		Link structure = findStructure(world, controllerPos, core, controller, casingTag);
		if (structure == null) {
			setControllerState(world, controllerPos, null, lastReason);
			return null;
		}
		if (!world.getBlockState(portPos).is(casingTag)
				|| !CenteredMultiblockDimensions.isShellPosition(portPos, structure.minBounds, structure.maxBounds)) {
			setControllerState(world, controllerPos, null, "Port is not on the outer shell in a valid position");
			return null;
		}
		setControllerState(world, controllerPos, structure, "");
		storeLayout(world, structure, core);
		return structure;
	}

	/** Discover bounds from the connected exterior shell, never from a core position. */
	private static Link findStructure(LevelAccessor world, BlockPos controllerPos,
			Block core, Block controller, TagKey<Block> casingTag) {
		lastReason = "";
		if (world.getBlockState(controllerPos).getBlock() != controller) {
			lastReason = "Missing " + description(controller) + " at " + controllerPos.toShortString();
			return null;
		}
		Bounds bounds = discoverBounds(world, controllerPos, casingTag);
		if (bounds == null || !CenteredMultiblockDimensions.isValidBounds(bounds.min, bounds.max)) {
			lastReason = "Reactor dimensions must each be at least 3 blocks";
			return null;
		}
		if (!CenteredMultiblockDimensions.isShellPosition(controllerPos, bounds.min, bounds.max)) {
			lastReason = description(controller) + " is not on the exterior shell";
			return null;
		}
		if (!isValid(world, controllerPos, bounds, core, controller, casingTag)) {
			return null;
		}
		lastReason = "";
		return new Link(controllerPos, bounds.min, bounds.max);
	}

	/** Locate a controller from a core, then let that controller discover the bounds. */
	private static Link findStructureFromCore(LevelAccessor world, BlockPos corePos,
			Block core, Block controller, TagKey<Block> casingTag) {
		lastReason = "";
		for (Direction direction : Direction.values()) {
			BlockPos cursor = corePos;
			while (true) {
				cursor = cursor.relative(direction);
				BlockState state = world.getBlockState(cursor);
				if (state.is(casingTag)) {
					BlockPos controllerPos = findControllerOnShell(world, cursor, controller, casingTag);
					if (controllerPos != null) {
						Link link = findStructure(world, controllerPos, core, controller, casingTag);
						if (link != null && CenteredMultiblockDimensions.isInside(corePos, link.minBounds, link.maxBounds)) {
							return link;
						}
					}
					break;
				}
				if (!isAllowedInterior(state.getBlock(), core)) {
					break;
				}
			}
		}
		if (lastReason.isEmpty()) {
			lastReason = "No valid " + description(controller) + " structure found around the core";
		}
		return null;
	}

	private static Bounds discoverBounds(LevelAccessor world, BlockPos shellStart, TagKey<Block> casingTag) {
		if (!world.getBlockState(shellStart).is(casingTag)) {
			return null;
		}
		Set<BlockPos> shell = collectShell(world, shellStart, casingTag);
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPos pos : shell) {
			minX = Math.min(minX, pos.getX());
			minY = Math.min(minY, pos.getY());
			minZ = Math.min(minZ, pos.getZ());
			maxX = Math.max(maxX, pos.getX());
			maxY = Math.max(maxY, pos.getY());
			maxZ = Math.max(maxZ, pos.getZ());
		}
		return new Bounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
	}

	private static BlockPos findControllerOnShell(LevelAccessor world, BlockPos shellStart,
			Block controller, TagKey<Block> casingTag) {
		for (BlockPos pos : collectShell(world, shellStart, casingTag)) {
			if (world.getBlockState(pos).getBlock() == controller) {
				return pos;
			}
		}
		return null;
	}

	private static Set<BlockPos> collectShell(LevelAccessor world, BlockPos start, TagKey<Block> casingTag) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(start.immutable());
		queue.add(start.immutable());
		while (!queue.isEmpty()) {
			BlockPos pos = queue.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (!visited.contains(next) && world.getBlockState(next).is(casingTag)) {
					BlockPos immutable = next.immutable();
					visited.add(immutable);
					queue.add(immutable);
				}
			}
		}
		return visited;
	}

	private static boolean isValid(LevelAccessor world, BlockPos controllerPos, Bounds bounds,
			Block core, Block controller, TagKey<Block> casingTag) {
		int controllerCount = 0;
		int energyOutputs = 0;
		int wasteOutputs = 0;
		int fluidInputs = 0;
		for (BlockPos pos : BlockPos.betweenClosed(bounds.min, bounds.max)) {
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (CenteredMultiblockDimensions.isShellPosition(pos, bounds.min, bounds.max)) {
				if (!state.is(casingTag)) {
					lastReason = "Shell block missing at " + describePos(world, pos)
							+ " — must be in casing tag #" + casingTag.location();
					return false;
				}
				controllerCount += block == controller ? 1 : 0;
				energyOutputs += block == CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT.get() ? 1 : 0;
				wasteOutputs += block == CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get() ? 1 : 0;
				fluidInputs += block == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get() ? 1 : 0;
			} else if (!isAllowedInterior(block, core)) {
				lastReason = "Interior block missing at " + describePos(world, pos)
						+ " (expected " + description(core) + ")";
				return false;
			}
		}
		if (controllerCount != 1) {
			lastReason = "Found " + controllerCount + " " + description(controller) + " blocks (need exactly 1)";
			return false;
		}
		if (core == CrystalnexusModBlocks.REACTOR_CORE.get()) {
			if (energyOutputs <= 0 || wasteOutputs <= 0 || fluidInputs <= 0) {
				lastReason = energyOutputs <= 0 ? "Missing reactor energy output block"
						: wasteOutputs <= 0 ? "Missing reactor waste output block" : "Missing reactor fluid input block";
				return false;
			}
			ReactorLayout layout = ReactorLayout.analyze(world, bounds.min, bounds.max);
			if (!layout.valid) {
				lastReason = "Reactor layout invalid: " + layout.reason;
				return false;
			}
		}
		return world.getBlockState(controllerPos).getBlock() == controller;
	}

	private static boolean isAllowedInterior(Block block, Block core) {
		return block == core || core == CrystalnexusModBlocks.REACTOR_CORE.get()
				&& (block == Blocks.AIR || ReactorLayout.isInteriorComponent(block));
	}

	private static void storeLayout(LevelAccessor world, Link link, Block core) {
		if (link == null || core != CrystalnexusModBlocks.REACTOR_CORE.get() || world.isClientSide()) {
			return;
		}
		if (world.getBlockEntity(link.pos) instanceof ReactorComputerBlockEntity computer) {
			computer.updateLayoutCache(ReactorLayout.analyze(world, link.minBounds, link.maxBounds));
		}
	}

	private static void setControllerState(LevelAccessor world, BlockPos pos, Link link, String reason) {
		if (world.isClientSide()) {
			return;
		}
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}
		boolean valid = link != null;
		int radius = valid ? link.radius : 0;
		long min = valid ? link.minBounds.asLong() : 0;
		long max = valid ? link.maxBounds.asLong() : 0;
		String message = reason == null ? "" : reason;
		CompoundTag data = blockEntity.getPersistentData();
		if (data.getBoolean("canOpenInventory") == valid && data.getInt("multiblockRadius") == radius
				&& data.getLong("multiblockMinBounds") == min && data.getLong("multiblockMaxBounds") == max
				&& data.getString("validationReason").equals(message)) {
			return;
		}
		data.putBoolean("canOpenInventory", valid);
		data.putInt("multiblockRadius", radius);
		data.putLong("multiblockMinBounds", min);
		data.putLong("multiblockMaxBounds", max);
		data.putString("validationReason", message);
		BlockState state = world.getBlockState(pos);
		if (world instanceof Level level) {
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private static String lastReason = "";

	private static String description(Block block) {
		return block == null ? "block" : block.getName().getString();
	}

	private static String describePos(LevelAccessor world, BlockPos pos) {
		return pos.toShortString() + " (" + description(world.getBlockState(pos).getBlock()) + ")";
	}

	private record Bounds(BlockPos min, BlockPos max) {
	}

	public static final class Link {
		public final BlockPos pos;
		public final BlockPos center;
		public final BlockPos minBounds;
		public final BlockPos maxBounds;
		public final int radius;

		public Link(BlockPos pos, BlockPos minBounds, BlockPos maxBounds) {
			this.pos = pos;
			this.minBounds = minBounds;
			this.maxBounds = maxBounds;
			this.center = CenteredMultiblockDimensions.center(minBounds, maxBounds);
			this.radius = CenteredMultiblockDimensions.legacyRadius(minBounds, maxBounds);
		}
	}
}
