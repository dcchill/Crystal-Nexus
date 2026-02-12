package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModGameRules;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class ReactorComputerOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		String registry_name_dust = "";
		String registry_name_no_namespace = "";
		String registry_name_ore = "";
		String registry_name = "";
		String registry_name_nugget = "";
		BlockState core = Blocks.AIR.defaultBlockState();
		double outputAmount = 0;
		double cookTime = 0;
		double energy = 0;
		core = CrystalnexusModBlocks.REACTOR_CORE.get().defaultBlockState();
		if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == core.getBlock()) {
			BlocksCheckerProcedure.execute(world, x + 1, y, z);
		} else if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == core.getBlock()) {
			BlocksCheckerProcedure.execute(world, x - 1, y, z);
		} else if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == core.getBlock()) {
			BlocksCheckerProcedure.execute(world, x, y, z + 1);
		} else if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == core.getBlock()) {
			BlocksCheckerProcedure.execute(world, x, y, z - 1);
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == CrystalnexusModItems.REACTOR_UPGRADE.get()) {
			energy = 512000;
		} else {
			energy = 327680;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get()) {
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("heat", (-1));
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", 2000);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxHeat", 1000);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		if (getBlockNBTLogic(world, BlockPos.containing(x, y, z), "canOpenInventory") == true && itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount() != 64) {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
			if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_INGOT.get()
					|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.COAL_SINGULARITY.get()) {
				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat") > getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
					if (4096000 >= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxProgress")) {
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
								if (_entityStorage != null)
									_entityStorage.receiveEnergy((int) energy, false);
							}
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.VAULT_CONNECTION, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.5, 0, 0.5, 0);
						}
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxProgress")) {
							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
								if (_entityStorage != null)
									_entityStorage.receiveEnergy((int) energy, false);
							}
							if (world instanceof ILevelExtension _ext) {
								IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
								if (_fluidHandler != null)
									_fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
							}
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_INGOT.get()) {
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									int _slotid = 0;
									ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
									_stk.shrink(1);
									_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
								}
							}
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", 0);
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
								_setstack.setCount(1 + itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount());
								_itemHandlerModifiable.setStackInSlot(2, _setstack);
							}
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4, (float) 0.7);
								} else {
									_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4, (float) 0.7, false);
								}
							}
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.WARPED_SPORE, (x + 0.5), (y + 0.5), (z + 0.5), 5, 0.5, 0, 0.5, 0);
						}
						if (0 < getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
							if (0 < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
								if (!world.isClientSide()) {
									BlockPos _bp = BlockPos.containing(x, y, z);
									BlockEntity _blockEntity = world.getBlockEntity(_bp);
									BlockState _bs = world.getBlockState(_bp);
									if (_blockEntity != null)
										_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") - 5));
									if (world instanceof Level _level)
										_level.sendBlockUpdated(_bp, _bs, _bs, 3);
								}
							}
						} else {
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") + 1));
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
						}
					}
				}
			} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get()) {
				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat") > getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
					if (4096000 >= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxProgress")) {
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
								if (_entityStorage != null)
									_entityStorage.receiveEnergy((int) (energy * 1.75), false);
							}
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.VAULT_CONNECTION, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.5, 0, 0.5, 0);
						}
						if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxProgress")) {
							if (world instanceof ILevelExtension _ext) {
								IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
								if (_entityStorage != null)
									_entityStorage.receiveEnergy((int) (energy * 1.75), false);
							}
							if (world instanceof ILevelExtension _ext) {
								IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
								if (_fluidHandler != null)
									_fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
							}
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.PURE_BLUTONIUM.get()) {
								if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
									int _slotid = 0;
									ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
									_stk.shrink(1);
									_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
								}
							}
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								ItemStack _setstack = new ItemStack(CrystalnexusModItems.BLUTONIUM_WASTE.get()).copy();
								_setstack.setCount(1 + itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2).getCount());
								_itemHandlerModifiable.setStackInSlot(2, _setstack);
							}
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("progress", 0);
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4, (float) 0.7);
								} else {
									_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4, (float) 0.7, false);
								}
							}
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.WARPED_SPORE, (x + 0.5), (y + 0.5), (z + 0.5), 5, 0.5, 0, 0.5, 0);
						}
						if (0 < getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {
							if (0 < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
								if (!world.isClientSide()) {
									BlockPos _bp = BlockPos.containing(x, y, z);
									BlockEntity _blockEntity = world.getBlockEntity(_bp);
									BlockState _bs = world.getBlockState(_bp);
									if (_blockEntity != null)
										_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") - 5));
									if (world instanceof Level _level)
										_level.sendBlockUpdated(_bp, _bs, _bs, 3);
								}
							}
						} else {
							if (!world.isClientSide()) {
								BlockPos _bp = BlockPos.containing(x, y, z);
								BlockEntity _blockEntity = world.getBlockEntity(_bp);
								BlockState _bs = world.getBlockState(_bp);
								if (_blockEntity != null)
									_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") + 1));
								if (world instanceof Level _level)
									_level.sendBlockUpdated(_bp, _bs, _bs, 3);
							}
						}
					}
				}
			}
		} else {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat") == getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
			if (false == world.getLevelData().getGameRules().getBoolean(CrystalnexusModGameRules.DISABLE_MELTDOWNS)) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crystalnexus:reactor_failure")), SoundSource.BLOCKS, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("crystalnexus:reactor_failure")), SoundSource.BLOCKS, 1, 1, false);
					}
				}
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_blockEntity != null)
						_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") + 1));
					if (world instanceof Level _level)
						_level.sendBlockUpdated(_bp, _bs, _bs, 3);
				}
			}
		}
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat") < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4,
							(float) (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") / 1000));
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.fire.extinguish")), SoundSource.BLOCKS, (float) 0.4, (float) (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") / 1000),
							false);
				}
			}
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (x + 0.5), (y + 0.5), (z + 0.5), 3, 1, 1, 1, 0);
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") + 1));
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		}
		if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "maxHeat") + 200 < getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat")) {
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("heat", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "heat") + 1));
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
			if (false == world.getLevelData().getGameRules().getBoolean(CrystalnexusModGameRules.DISABLE_MELTDOWNS)) {
				if (world instanceof Level _level && !_level.isClientSide())
					_level.explode(null, x, y, z, 100, Level.ExplosionInteraction.TNT);
				if (world instanceof Level _level && !_level.isClientSide())
					_level.explode(null, x, y, z, 20, Level.ExplosionInteraction.TNT);
				world.setBlock(BlockPos.containing(x, y, z), CrystalnexusModBlocks.RAD_PLACEHOLDER.get().defaultBlockState(), 3);
			}
		}
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
	}

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}
}