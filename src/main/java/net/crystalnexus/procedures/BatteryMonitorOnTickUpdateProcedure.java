package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class BatteryMonitorOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world.isClientSide()) return;
		double connectedCount = 0;

		{
			java.util.Set<BlockPos> visited = new java.util.HashSet<>();
			java.util.function.Function<BlockPos, Integer> countConnected = new java.util.function.Function<>() {
				@Override
				public Integer apply(BlockPos pos) {
					if (visited.contains(pos))
						return 0;
					Block block = world.getBlockState(pos).getBlock();
					String name = block.builtInRegistryHolder().key().location().toString();
					if (!(name.equals("crystalnexus:battery") || name.equals("crystalnexus:ee_battery")))
						return 0;
					visited.add(pos);
					int count = 1;
					for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
						count += this.apply(pos.relative(dir));
					}
					return count;
				}
			};
			BlockPos startPos = BlockPos.containing(x, y, z);
			BlockPos foundBattery = null;
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
				BlockPos neighbor = startPos.relative(dir);
				Block block = world.getBlockState(neighbor).getBlock();
				String name = block.builtInRegistryHolder().key().location().toString();
				if (name.equals("crystalnexus:battery") || name.equals("crystalnexus:ee_battery")) {
					foundBattery = neighbor;
					break;
				}
			}
			if (foundBattery != null) {
				connectedCount = countConnected.apply(foundBattery);
			} else {
				connectedCount = 0;
			}
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null && _blockEntity.getPersistentData().getDouble("connectedCount") != connectedCount) {
				_blockEntity.getPersistentData().putDouble("connectedCount", connectedCount);
				_blockEntity.setChanged();
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 2);
			}
		}
	}

    public static String displayText(LevelAccessor world, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos source = pos;
        for (Direction direction : new Direction[] { Direction.UP, Direction.DOWN, Direction.NORTH,
                Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            if (canReceiveEnergy(world, pos.relative(direction), null)) {
                source = pos.relative(direction);
                break;
            }
        }
        BlockEntity machine = world.getBlockEntity(pos);
        double count = machine == null ? 0 : machine.getPersistentData().getDouble("connectedCount");
        return new java.text.DecimalFormat("\u00A7fTotal Energy: ##.## FE").format(getEnergyStored(world, source, null) * count);
    }

	private static boolean canReceiveEnergy(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.canReceive();
		}
		return false;
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}
}
