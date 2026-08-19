package net.crystalnexus.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.crystalnexus.block.entity.ReactorComputerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.reactor.ReactorLayout;

public final class CenteredMultiblockValidator {
	public static final int MAX_RADIUS = CenteredMultiblockDimensions.MAX_RADIUS;

	public CenteredMultiblockValidator() {
	}

	public static Link validateFromController(LevelAccessor world, BlockPos controllerPos,
			Block core, Block controller, TagKey<Block> casingTag) {
		Link link = findStructure(world, controllerPos, core, controller, casingTag);
		setControllerState(world, controllerPos, link != null, link != null ? link.radius : 0,
				link != null ? "" : lastReason);
		if (link != null && core == CrystalnexusModBlocks.REACTOR_CORE.get()) {
			storeLayout(world, controllerPos, link.center, link.radius);
		}
		return link;
	}

	private static void storeLayout(LevelAccessor world, BlockPos controllerPos, BlockPos center, int radius) {
		if (world.isClientSide()) {
			return;
		}
		if (world.getBlockEntity(controllerPos) instanceof ReactorComputerBlockEntity computer) {
			computer.updateLayoutCache(ReactorLayout.analyze(world, center, radius));
		}
	}

	public static Link validateFromCore(LevelAccessor world, BlockPos corePos,
			Block core, Block controller, TagKey<Block> casingTag) {
		Link link = findStructureFromCore(world, corePos, core, controller, casingTag);
		if (link == null) {
			return null;
		}
		setControllerState(world, link.pos, true, link.radius, "");
		if (core == CrystalnexusModBlocks.REACTOR_CORE.get()) {
			storeLayout(world, link.pos, link.center, link.radius);
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

		int horizontalReach = MAX_RADIUS * 2;
		for (int dy = -MAX_RADIUS; dy <= MAX_RADIUS; dy++) {
			for (int dx = -horizontalReach; dx <= horizontalReach; dx++) {
				for (int dz = -horizontalReach; dz <= horizontalReach; dz++) {
					BlockPos controllerPos = portPos.offset(dx, dy, dz);
					Link link = validateFromPortController(world, portPos, controllerPos, core, controller, casingTag);
					if (link != null) {
						if (portEntity != null) {
							portEntity.getPersistentData().putLong("multiblockController", controllerPos.asLong());
						}
						return link;
					}
				}
			}
		}
		return null;
	}

	private static Link validateFromPortController(LevelAccessor world, BlockPos portPos,
			BlockPos controllerPos, Block core, Block controller, TagKey<Block> casingTag) {
		if (world.getBlockState(controllerPos).getBlock() != controller) {
			// Not the controller at this candidate position; not a real failure.
			return null;
		}
		Link structure = findStructure(world, controllerPos, core, controller, casingTag);
		if (structure == null) {
			setControllerState(world, controllerPos, false, 0, lastReason);
			return null;
		}
		int dx = portPos.getX() - structure.center.getX();
		int dy = portPos.getY() - structure.center.getY();
		int dz = portPos.getZ() - structure.center.getZ();
		if (!world.getBlockState(portPos).is(casingTag)) {
			setControllerState(world, controllerPos, false, structure.radius, "Port block is not part of the " + description(controller) + " casing tag");
			return null;
		}
		if (!CenteredMultiblockDimensions.isShellOffset(dx, dy, dz, structure.radius)) {
			setControllerState(world, controllerPos, false, structure.radius, "Port is not on the outer shell in a valid position");
			return null;
		}
		setControllerState(world, controllerPos, true, structure.radius, "");
		if (core == CrystalnexusModBlocks.REACTOR_CORE.get()) {
			storeLayout(world, controllerPos, structure.center, structure.radius);
		}
		return new Link(controllerPos, structure.center, structure.radius);
	}

	/**
	 * Starting from the controller (which must sit on the exterior shell), find a
	 * candidate 3x3x3, 5x5x5 or 7x7x7 cube that contains the controller on its shell.
	 * The structure center is determined independently of any Reactor Core position.
	 */
	private static Link findStructure(LevelAccessor world, BlockPos controllerPos,
			Block core, Block controller, TagKey<Block> casingTag) {
		for (int radius = 1; radius <= MAX_RADIUS; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						// The controller must sit on the shell of the candidate cube.
						if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != radius) {
							continue;
						}
						BlockPos center = controllerPos.offset(-dx, -dy, -dz);
						if (isValid(world, center, controllerPos, radius, core, controller, casingTag)) {
							lastReason = "";
							return new Link(controllerPos, center, radius);
						}
					}
				}
			}
		}
		if (lastReason.isEmpty()) {
			lastReason = "No valid " + description(controller) + " structure found around the controller";
		}
		return null;
	}

	/**
	 * Given a Reactor Core position (which may be anywhere in the interior), find the
	 * enclosing structure and its controller. The core is NOT assumed to be the center.
	 */
	private static Link findStructureFromCore(LevelAccessor world, BlockPos corePos,
			Block core, Block controller, TagKey<Block> casingTag) {
		for (int radius = 1; radius <= MAX_RADIUS; radius++) {
			// The core is an interior block, so the center is within radius-1 of it.
			for (int dx = -radius + 1; dx <= radius - 1; dx++) {
				for (int dy = -radius + 1; dy <= radius - 1; dy++) {
					for (int dz = -radius + 1; dz <= radius - 1; dz++) {
						BlockPos center = corePos.offset(-dx, -dy, -dz);
						for (int cdx = -radius; cdx <= radius; cdx++) {
							for (int cdy = -radius; cdy <= radius; cdy++) {
								for (int cdz = -radius; cdz <= radius; cdz++) {
									if (Math.max(Math.abs(cdx), Math.max(Math.abs(cdy), Math.abs(cdz))) != radius) {
										continue;
									}
									BlockPos controllerPos = center.offset(cdx, cdy, cdz);
									if (world.getBlockState(controllerPos).getBlock() != controller) {
										continue;
									}
									if (isValid(world, center, controllerPos, radius, core, controller, casingTag)) {
										lastReason = "";
										return new Link(controllerPos, center, radius);
									}
								}
							}
						}
					}
				}
			}
		}
		if (lastReason.isEmpty()) {
			lastReason = "No valid " + description(controller) + " structure found around the core";
		}
		return null;
	}

	private static boolean isValid(LevelAccessor world, BlockPos center, BlockPos controllerPos,
			int radius, Block core, Block controller, TagKey<Block> casingTag) {
		if (world.getBlockState(controllerPos).getBlock() != controller) {
			lastReason = "Missing " + description(controller) + " at " + controllerPos.toShortString();
			return false;
		}
		int controllerCount = 0;
		int energyOutputs = 0;
		int wasteOutputs = 0;
		int fluidInputs = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (world.getBlockState(pos).getBlock() == controller) {
						controllerCount++;
					}
					if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.REACTOR_ENERGY_OUTPUT.get()) {
						energyOutputs++;
					}
					if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.REACTOR_WASTE_OUTPUT.get()) {
						wasteOutputs++;
					}
					if (world.getBlockState(pos).getBlock() == CrystalnexusModBlocks.REACTOR_FLUID_INPUT.get()) {
						fluidInputs++;
					}

					if (CenteredMultiblockDimensions.isShellOffset(dx, dy, dz, radius)) {
						if (!pos.equals(controllerPos) && !world.getBlockState(pos).is(casingTag)) {
							lastReason = "Shell block missing at " + describePos(world, pos) + " — must be in casing tag #" + casingTag.location();
							return false;
						}
					} else if (world.getBlockState(pos).getBlock() != core
							&& !(core == CrystalnexusModBlocks.REACTOR_CORE.get()
									&& ReactorLayout.isInternalComponent(world.getBlockState(pos).getBlock()))) {
						lastReason = "Interior block missing at " + describePos(world, pos) + " (expected " + description(core) + ")";
						return false;
					}
				}
			}
		}
		if (controllerCount != 1) {
			lastReason = "Found " + controllerCount + " " + description(controller) + " blocks (need exactly 1)";
			return false;
		}
		if (core == CrystalnexusModBlocks.REACTOR_CORE.get()) {
			if (energyOutputs <= 0) {
				lastReason = "Missing reactor energy output block";
				return false;
			}
			if (wasteOutputs <= 0) {
				lastReason = "Missing reactor waste output block";
				return false;
			}
			if (fluidInputs <= 0) {
				lastReason = "Missing reactor fluid input block";
				return false;
			}
			ReactorLayout layout = ReactorLayout.analyze(world, center, radius);
			if (!layout.valid) {
				lastReason = "Reactor layout invalid: " + layout.reason;
				return false;
			}
		}
		return true;
	}

	private static void setControllerState(LevelAccessor world, BlockPos pos, boolean valid, int radius, String reason) {
		if (world.isClientSide()) {
			return;
		}
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}
		CompoundTag data = blockEntity.getPersistentData();
		if (data.getBoolean("canOpenInventory") == valid && data.getInt("multiblockRadius") == radius
				&& data.getString("validationReason").equals(reason)) {
			return;
		}
		data.putBoolean("canOpenInventory", valid);
		data.putInt("multiblockRadius", radius);
		data.putString("validationReason", reason == null ? "" : reason);
		BlockState state = world.getBlockState(pos);
		if (world instanceof Level level) {
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private static String lastReason = "";

	private static String description(Block block) {
		if (block == null) {
			return "block";
		}
		return block.getName().getString();
	}

	private static String describePos(LevelAccessor world, BlockPos pos) {
		return pos.toShortString() + " (" + description(world.getBlockState(pos).getBlock()) + ")";
	}

	public static class Link {
		public BlockPos pos;
		public BlockPos center;
		public int radius;

		public Link(BlockPos pos, BlockPos center, int radius) {
			this.pos = pos;
			this.center = center;
			this.radius = radius;
		}
	}
}