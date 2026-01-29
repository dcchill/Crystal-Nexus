package net.crystalnexus.procedures;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

public class BluTNTOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double outputAmount = 0;
		double cookTime = 0;
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") == 0) {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
		if (world instanceof Level _level3 && _level3.hasNeighborSignal(BlockPos.containing(x, y, z))) {
			if (100 > getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress")) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
		}
		if (1 < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress")) {
			if (100 > getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress")) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
			if (100 <= getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress")) {
				if ((world instanceof Level _lvl ? _lvl.dimension() : (world instanceof WorldGenLevel _wgl ? _wgl.getLevel().dimension() : Level.OVERWORLD)) == Level.END) {
					world.destroyBlock(BlockPos.containing(x, y, z), false);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 0), (z + 0), 10, Level.ExplosionInteraction.BLOCK);
					if (world instanceof ServerLevel _level)
						_level.holderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.parse("crystalnexus:invertium_crystal_formation"))).value().place(_level, _level.getChunkSource().getGenerator(), _level.getRandom(),
								BlockPos.containing(x, y + 3, z));
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 0), (z + 0), 3, Level.ExplosionInteraction.BLOCK);
				} else {
					world.destroyBlock(BlockPos.containing(x, y, z), false);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 0), (z + 0), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 1), (y + 0), (z + 0), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x - 1), (y + 0), (z + 0), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 1), (z + 0), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y - 1), (z + 0), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 0), (z + 1), 15, Level.ExplosionInteraction.BLOCK);
					if (world instanceof Level _level && !_level.isClientSide())
						_level.explode(null, (x + 0), (y + 0), (z - 1), 15, Level.ExplosionInteraction.BLOCK);
				}
			}
		}
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}
}