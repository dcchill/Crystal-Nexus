package net.crystalnexus.procedures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModBlocks;

public class MachineBlocksCheckerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockState middleLineBlock = Blocks.AIR.defaultBlockState();
		BlockState interfaceBlock = Blocks.AIR.defaultBlockState();
		BlockState crossBlockMiddleLayer = Blocks.AIR.defaultBlockState();
		BlockState frame = Blocks.AIR.defaultBlockState();
		BlockState ouputBlock = Blocks.AIR.defaultBlockState();
		BlockState inputBlock = Blocks.AIR.defaultBlockState();
		frame = CrystalnexusModBlocks.MACHINEBLOCK.get().defaultBlockState();
		interfaceBlock = CrystalnexusModBlocks.ORE_PROCESSOR.get().defaultBlockState();
		middleLineBlock = CrystalnexusModBlocks.MACHINEBLOCK.get().defaultBlockState();
		if ((world.getBlockState(BlockPos.containing(x + 1, y + 1, z + 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x - 1, y + 1, z - 1))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x + 1, y + 1, z - 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x - 1, y + 1, z + 1))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x, y + 1, z))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x + 1, y + 1, z))).getBlock() == middleLineBlock.getBlock()
				&& (world.getBlockState(BlockPos.containing(x - 1, y + 1, z))).getBlock() == middleLineBlock.getBlock() && (world.getBlockState(BlockPos.containing(x, y + 1, z + 1))).getBlock() == middleLineBlock.getBlock()
				&& (world.getBlockState(BlockPos.containing(x, y + 1, z - 1))).getBlock() == middleLineBlock.getBlock() && (world.getBlockState(BlockPos.containing(x + 1, y - 1, z + 1))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x - 1, y - 1, z - 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x + 1, y - 1, z - 1))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x - 1, y - 1, z + 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x + 1, y - 1, z))).getBlock() == middleLineBlock.getBlock() && (world.getBlockState(BlockPos.containing(x - 1, y - 1, z))).getBlock() == middleLineBlock.getBlock()
				&& (world.getBlockState(BlockPos.containing(x, y - 1, z + 1))).getBlock() == middleLineBlock.getBlock() && (world.getBlockState(BlockPos.containing(x, y - 1, z - 1))).getBlock() == middleLineBlock.getBlock()
				&& (world.getBlockState(BlockPos.containing(x + 1, y, z + 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x - 1, y, z - 1))).getBlock() == frame.getBlock()
				&& (world.getBlockState(BlockPos.containing(x + 1, y, z - 1))).getBlock() == frame.getBlock() && (world.getBlockState(BlockPos.containing(x - 1, y, z + 1))).getBlock() == frame.getBlock()) {
			if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))
					&& (world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x + 1, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", true);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))
					&& (world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x - 1, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", true);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))
					&& (world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z + 1);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", true);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:machine_blocks")))
					&& (world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z - 1);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", true);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else {
				if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x + 1, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x - 1, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z + 1);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z - 1);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				}
			}
		} else {
			if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x + 1, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x - 1, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z + 1);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:mutliblock_machines")))) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z - 1);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putBoolean("canOpenInventory", false);
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
		}
	}
}