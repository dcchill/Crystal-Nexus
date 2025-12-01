package net.crystalnexus.procedures;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModParticleTypes;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class CrystalEnergyGuideOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		double crystalCount = 0;
		double yOffset = 0;
		double xOffset = 0;
		double cookTime = 0;
		double zOffset = 0;
		double T = 0;
		double Zo = 0;
		double Yo = 0;
		double Za = 0;
		double Xo = 0;
		double Ya = 0;
		double Xa = 0;
		double energy = 0;
		double outputAmount = 0;
		double range = 0;
		double nrgstrt = 0;
		nrgstrt = 81920;
		if (("up").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 8;
			zOffset = 0;
		}
		if (("down").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = -8;
			zOffset = 0;
		}
		if (("north").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = -8;
		}
		if (("south").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 0;
			yOffset = 0;
			zOffset = 8;
		}
		if (("east").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = 8;
			yOffset = 0;
			zOffset = 0;
		}
		if (("west").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
			xOffset = -8;
			yOffset = 0;
			zOffset = 0;
		}
		if (0 < getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
			if (("east").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index0 = 0; index0 < 8; index0++) {
					if (canReceiveEnergy(world, BlockPos.containing(x + T, y, z), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x + T, y, z), (int) energy, Direction.WEST);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x + T, y, z), Direction.WEST);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x + T, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x + T + 0.5), (y + 0.5), (z + 0.5), 1, 0.5, 0, 0, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x + T, y, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x + T, y, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x + T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
			if (("west").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index1 = 0; index1 < 8; index1++) {
					if (canReceiveEnergy(world, BlockPos.containing(x - T, y, z), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x - T, y, z), (int) energy, Direction.EAST);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x - T, y, z), Direction.EAST);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x - T, y, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x - T + 0.5), (y + 0.5), (z + 0.5), 1, 0.5, 0, 0, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x - T, y, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x - T, y, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x - T, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
			if (("south").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index2 = 0; index2 < 8; index2++) {
					if (canReceiveEnergy(world, BlockPos.containing(x, y, z + T), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z + T), (int) energy, Direction.NORTH);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z + T), Direction.NORTH);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x, y, z + T))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x + 0.5), (y + 0.5), (z + T + 0.5), 1, 0, 0, 0.5, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x, y, z + T))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x, y, z + T))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z + T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
			if (("north").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index3 = 0; index3 < 8; index3++) {
					if (canReceiveEnergy(world, BlockPos.containing(x, y, z - T), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x, y, z - T), (int) energy, Direction.SOUTH);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z - T), Direction.SOUTH);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x, y, z - T))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x + 0.5), (y + 0.5), (z - T + 0.5), 1, 0, 0, 0.5, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x, y, z - T))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x, y, z - T))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z - T);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
			if (("up").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index4 = 0; index4 < 8; index4++) {
					if (canReceiveEnergy(world, BlockPos.containing(x, y + T, z), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x, y + T, z), (int) energy, Direction.DOWN);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y + T, z), Direction.DOWN);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x, y + T, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x + 0.5), (y + T + 0.5), (z + 0.5), 1, 0, 0.5, 0, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x, y + T, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x, y + T, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y + T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
			if (("down").equals(getBlockNBTString(world, BlockPos.containing(x, y, z), "rotation"))) {
				T = 1;
				for (int index5 = 0; index5 < 8; index5++) {
					if (canReceiveEnergy(world, BlockPos.containing(x, y - T, z), null)) {
						energy = extractEnergySimulate(world, BlockPos.containing(x, y, z), (int) nrgstrt, null);
						energy = receiveEnergySimulate(world, BlockPos.containing(x, y - T, z), (int) energy, Direction.UP);
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy((int) energy, false);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - T, z), Direction.UP);
							if (_entityStorage != null)
								_entityStorage.receiveEnergy((int) energy, false);
						}
						break;
					} else if (T > 8) {
						break;
					} else if (!(world.getBlockState(BlockPos.containing(x, y - T, z))).is(BlockTags.create(ResourceLocation.parse("crystalnexus:lasertransparent")))) {
						break;
					} else {
						if (world instanceof ServerLevel _level)
							_level.sendParticles((SimpleParticleType) (CrystalnexusModParticleTypes.LASER_PART.get()), (x + 0.5), (y - T + 0.5), (z + 0.5), 1, 0, 0.5, 0, 0);
						T = T + 1;
					}
					if (CrystalnexusModBlocks.ENERGY_REFRACTOR.get() == (world.getBlockState(BlockPos.containing(x, y - T, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
					if (CrystalnexusModBlocks.ENERGY_SPLITTER.get() == (world.getBlockState(BlockPos.containing(x, y - T, z))).getBlock()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putBoolean("refracting", true);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originX", x);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originY", y);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y - T, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("originZ", z);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
					}
				}
			}
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putBoolean("crystal", false);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
	}

	private static String getBlockNBTString(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getString(tag);
		return "";
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static boolean canReceiveEnergy(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.canReceive();
		}
		return false;
	}

	private static int extractEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.extractEnergy(amount, true);
		}
		return 0;
	}

	private static int receiveEnergySimulate(LevelAccessor level, BlockPos pos, int amount, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.receiveEnergy(amount, true);
		}
		return 0;
	}
}