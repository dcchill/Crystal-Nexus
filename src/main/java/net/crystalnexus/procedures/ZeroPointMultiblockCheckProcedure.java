package net.crystalnexus.procedures;

import net.crystalnexus.init.CrystalnexusModBlocks;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ZeroPointMultiblockCheckProcedure {
	private static final int MAX_OUTPUT = 10240000;
	private static final String PROGRESS_TAG = "progress";
	private static final ResourceLocation ACTIVE_SOUND = ResourceLocation.parse("crystalnexus:zero_point_active");
	private static final Direction[] OUTPUT_SIDES = {Direction.DOWN, Direction.UP, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH};
	private static final int[][] OUTPUT_OFFSETS = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
	private static final int[][] ZERO_POINT_OFFSETS = {{0, 0, 0}};
	private static final int[][] CARBON_BLOCK_OFFSETS = {
			{-11, -1, -3}, {-11, -1, -2}, {-11, -1, -1}, {-11, -1, 0}, {-11, -1, 1}, {-11, -1, 2}, {-11, -1, 3}, {-10, -1, -5}, {-10, -1, -4}, {-10, -1, 4}, {-10, -1, 5}, {-9, -1, -7},
			{-9, -1, -6}, {-9, -1, 6}, {-9, -1, 7}, {-8, -1, -8}, {-8, -1, 8}, {-7, -1, -9}, {-7, -1, 9}, {-6, -1, -9}, {-6, -1, 9}, {-5, -1, -10}, {-5, -1, 10}, {-4, -1, -10},
			{-4, -1, 10}, {-3, -1, -11}, {-3, -1, 11}, {-2, -1, -11}, {-2, -1, 11}, {-1, -1, -11}, {-1, -1, 11}, {0, -1, -11}, {0, -1, 11}, {1, -1, -11}, {1, -1, 11}, {2, -1, -11},
			{2, -1, 11}, {3, -1, -11}, {3, -1, 11}, {4, -1, -10}, {4, -1, 10}, {5, -1, -10}, {5, -1, 10}, {6, -1, -9}, {6, -1, 9}, {7, -1, -9}, {7, -1, 9}, {8, -1, -8},
			{8, -1, 8}, {9, -1, -7}, {9, -1, -6}, {9, -1, 6}, {9, -1, 7}, {10, -1, -5}, {10, -1, -4}, {10, -1, 4}, {10, -1, 5}, {11, -1, -3}, {11, -1, -2}, {11, -1, -1},
			{11, -1, 0}, {11, -1, 1}, {11, -1, 2}, {11, -1, 3}, {-12, 0, -3}, {-12, 0, 3}, {-11, 0, -5}, {-11, 0, -4}, {-11, 0, 4}, {-11, 0, 5}, {-10, 0, -7}, {-10, 0, -6},
			{-10, 0, 6}, {-10, 0, 7}, {-9, 0, -8}, {-9, 0, 8}, {-8, 0, -9}, {-8, 0, 9}, {-7, 0, -10}, {-7, 0, 10}, {-6, 0, -10}, {-6, 0, 10}, {-5, 0, -11}, {-5, 0, 11},
			{-4, 0, -11}, {-4, 0, 11}, {-3, 0, -12}, {-3, 0, 12}, {3, 0, -12}, {3, 0, 12}, {4, 0, -11}, {4, 0, 11}, {5, 0, -11}, {5, 0, 11}, {6, 0, -10}, {6, 0, 10},
			{7, 0, -10}, {7, 0, 10}, {8, 0, -9}, {8, 0, 9}, {9, 0, -8}, {9, 0, 8}, {10, 0, -7}, {10, 0, -6}, {10, 0, 6}, {10, 0, 7}, {11, 0, -5}, {11, 0, -4},
			{11, 0, 4}, {11, 0, 5}, {12, 0, -3}, {12, 0, 3}, {-11, 1, -3}, {-11, 1, -2}, {-11, 1, -1}, {-11, 1, 0}, {-11, 1, 1}, {-11, 1, 2}, {-11, 1, 3}, {-10, 1, -5},
			{-10, 1, -4}, {-10, 1, 4}, {-10, 1, 5}, {-9, 1, -7}, {-9, 1, -6}, {-9, 1, 6}, {-9, 1, 7}, {-8, 1, -8}, {-8, 1, 8}, {-7, 1, -9}, {-7, 1, 9}, {-6, 1, -9}, {-6, 1, 9},
			{-5, 1, -10}, {-5, 1, 10}, {-4, 1, -10}, {-4, 1, 10}, {-3, 1, -11}, {-3, 1, 11}, {-2, 1, -11}, {-2, 1, 11}, {-1, 1, -11}, {-1, 1, 11}, {0, 1, -11}, {0, 1, 11},
			{1, 1, -11}, {1, 1, 11}, {2, 1, -11}, {2, 1, 11}, {3, 1, -11}, {3, 1, 11}, {4, 1, -10}, {4, 1, 10}, {5, 1, -10}, {5, 1, 10}, {6, 1, -9}, {6, 1, 9},
			{7, 1, -9}, {7, 1, 9}, {8, 1, -8}, {8, 1, 8}, {9, 1, -7}, {9, 1, -6}, {9, 1, 6}, {9, 1, 7}, {10, 1, -5}, {10, 1, -4}, {10, 1, 4}, {10, 1, 5},
			{11, 1, -3}, {11, 1, -2}, {11, 1, -1}, {11, 1, 0}, {11, 1, 1}, {11, 1, 2}, {11, 1, 3}
	};
	private static final int[][] CARBON_GLASS_OFFSETS = {
			{-12, 0, -2}, {-12, 0, -1}, {-12, 0, 0}, {-12, 0, 1}, {-12, 0, 2}, {-2, 0, -12}, {-2, 0, 12}, {-1, 0, -12}, {-1, 0, 12}, {0, 0, -12}, {0, 0, 12}, {1, 0, -12},
			{1, 0, 12}, {2, 0, -12}, {2, 0, 12}, {12, 0, -2}, {12, 0, -1}, {12, 0, 0}, {12, 0, 1}, {12, 0, 2}
	};
	private static final int[][] CARBON_MACHINE_FRAME_OFFSETS = {
			{-11, 0, 0}, {-10, 0, 0}, {-9, 0, 0}, {-8, 0, 0}, {-7, 0, 0}, {-6, 0, 0}, {-5, 0, 0}, {-4, 0, 0}, {-3, 0, 0}, {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1},
			{-1, 0, -2}, {-1, 0, -1}, {-1, 0, 1}, {-1, 0, 2}, {0, 0, -11}, {0, 0, -10}, {0, 0, -9}, {0, 0, -8}, {0, 0, -7}, {0, 0, -6}, {0, 0, -5}, {0, 0, -4},
			{0, 0, -3}, {0, 0, -2}, {0, 0, 2}, {0, 0, 3}, {0, 0, 4}, {0, 0, 5}, {0, 0, 6}, {0, 0, 7}, {0, 0, 8}, {0, 0, 9}, {0, 0, 10}, {0, 0, 11},
			{1, 0, -2}, {1, 0, -1}, {1, 0, 1}, {1, 0, 2}, {2, 0, -1}, {2, 0, 0}, {2, 0, 1}, {3, 0, 0}, {4, 0, 0}, {5, 0, 0}, {6, 0, 0}, {7, 0, 0},
			{8, 0, 0}, {9, 0, 0}, {10, 0, 0}, {11, 0, 0}
	};

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos controllerPos = BlockPos.containing(x, y, z);
		BlockState controllerState = world.getBlockState(controllerPos);
		BlockEntity controllerEntity = world.getBlockEntity(controllerPos);
		boolean formed = isValidStructure(world, controllerPos);

		setControllerBlockstate(world, controllerPos, controllerState, formed ? 2 : 1);
		if (!formed) {
			return;
		}

		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5, y + 0.5, z + 0.5, 5, 3, 0, 3, 0.2);
		}

		handleProgress(world, controllerPos, controllerEntity);
		pushEnergy(world, controllerPos);
	}

	private static boolean isValidStructure(LevelAccessor world, BlockPos controllerPos) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		Block carbonBlock = CrystalnexusModBlocks.CARBON_BLOCK.get();
		Block carbonGlass = CrystalnexusModBlocks.CARBON_GLASS.get();
		Block carbonMachineFrame = CrystalnexusModBlocks.CARBON_MACHINE_FRAME.get();
		Block zeroPoint = CrystalnexusModBlocks.ZERO_POINT.get();
		return matches(world, controllerPos, cursor, CARBON_BLOCK_OFFSETS, carbonBlock)
				&& matches(world, controllerPos, cursor, CARBON_GLASS_OFFSETS, carbonGlass)
				&& matches(world, controllerPos, cursor, CARBON_MACHINE_FRAME_OFFSETS, carbonMachineFrame)
				&& matches(world, controllerPos, cursor, ZERO_POINT_OFFSETS, zeroPoint);
	}

	private static boolean matches(LevelAccessor world, BlockPos origin, BlockPos.MutableBlockPos cursor, int[][] offsets, Block expected) {
		for (int[] offset : offsets) {
			cursor.set(origin.getX() + offset[0], origin.getY() + offset[1], origin.getZ() + offset[2]);
			if (world.getBlockState(cursor).getBlock() != expected) {
				return false;
			}
		}
		return true;
	}

	private static void setControllerBlockstate(LevelAccessor world, BlockPos pos, BlockState state, int value) {
		if (!(state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty integerProp)) {
			return;
		}
		if (!integerProp.getPossibleValues().contains(value) || state.getValue(integerProp) == value) {
			return;
		}
		world.setBlock(pos, state.setValue(integerProp, value), 3);
	}

	private static void handleProgress(LevelAccessor world, BlockPos controllerPos, BlockEntity controllerEntity) {
		if (controllerEntity == null || world.isClientSide()) {
			return;
		}

		int progress = (int) controllerEntity.getPersistentData().getDouble(PROGRESS_TAG);
		BlockState state = world.getBlockState(controllerPos);
		if (progress >= 300) {
			controllerEntity.getPersistentData().putDouble(PROGRESS_TAG, 0);
			if (world instanceof Level level) {
				level.sendBlockUpdated(controllerPos, state, state, 3);
				level.playSound(null, controllerPos, BuiltInRegistries.SOUND_EVENT.get(ACTIVE_SOUND), SoundSource.NEUTRAL, 1, 1);
			}
			return;
		}

		controllerEntity.getPersistentData().putDouble(PROGRESS_TAG, progress + 1);
		if (world instanceof Level level) {
			level.sendBlockUpdated(controllerPos, state, state, 3);
		}
	}

	private static void pushEnergy(LevelAccessor world, BlockPos controllerPos) {
		for (int i = 0; i < OUTPUT_SIDES.length; i++) {
			int[] offset = OUTPUT_OFFSETS[i];
			Direction direction = OUTPUT_SIDES[i];
			BlockPos outputPos = controllerPos.offset(offset[0], offset[1], offset[2]);
			IEnergyStorage storage = getEnergyStorage(world, outputPos, direction);
			if (storage == null || !storage.canReceive()) {
				continue;
			}

			int received = storage.receiveEnergy(MAX_OUTPUT, true);
			if (received > 0) {
				storage.receiveEnergy(received, false);
			}
		}
	}

	private static IEnergyStorage getEnergyStorage(LevelAccessor world, BlockPos pos, Direction direction) {
		if (world instanceof ILevelExtension levelExtension) {
			return levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
		}
		return null;
	}
}
