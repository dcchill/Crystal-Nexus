package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.init.CrystalnexusModBlocks;

public class ExtractinatorOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		String registry_name_no_namespace = "";
		String registry_name_nugget = "";
		String registry_name = "";
		double crystalCount = 0;
		double outputAmount = 0;
		double cookTime = 0;
		double slotnumbercheck = 0;
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
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy()).getItem() == CrystalnexusModItems.EFFICIENCY_UPGRADE.get()) {
			outputAmount = 2;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy()).getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
			outputAmount = 3;
		} else {
			outputAmount = 1;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = 75;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 7).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = 50;
		} else {
			cookTime = 100;
		}
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		slotnumbercheck = 1;
		if (4096 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
			if (!((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.AIR.asItem())) {
				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") < cookTime) {
					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putDouble("progress", (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") + 1));
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.WHITE_ASH, (x + 0.5), (y + 0.9), (z + 0.5), 1, 0.125, 0.25, 0.125, 0);
				}
				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).is(ItemTags.create(ResourceLocation.parse("c:sands")))) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(4096, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index0 = 0; index0 < 6; index0++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.IRON_NUGGET) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.IRON_NUGGET).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.2));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 8)) {
							for (int index1 = 0; index1 < 6; index1++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == CrystalnexusModItems.ANCIENT_CRYSTAL.get()) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(CrystalnexusModItems.ANCIENT_CRYSTAL.get()).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.GRAVEL.asItem()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(4096, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index2 = 0; index2 < 6; index2++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.RAW_COPPER) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.RAW_COPPER).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.7));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 3)) {
							for (int index3 = 0; index3 < 6; index3++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.COAL) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.COAL).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.COBBLESTONE.asItem()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(1024, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index4 = 0; index4 < 6; index4++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.IRON_NUGGET) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.IRON_NUGGET).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.7));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 8)) {
							for (int index5 = 0; index5 < 6; index5++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.GOLD_NUGGET) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.GOLD_NUGGET).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 125)) {
							for (int index6 = 0; index6 < 6; index6++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.DIAMOND) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.DIAMOND).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.SOUL_SAND.asItem()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(1024, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index7 = 0; index7 < 6; index7++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.GOLD_NUGGET) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.GOLD_NUGGET).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.7));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 300)) {
							for (int index8 = 0; index8 < 6; index8++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.NETHERITE_SCRAP) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.NETHERITE_SCRAP).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == Blocks.COBBLED_DEEPSLATE.asItem()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(1024, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index9 = 0; index9 < 6; index9++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.IRON_NUGGET) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.IRON_NUGGET).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.7));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 8)) {
							for (int index10 = 0; index10 < 6; index10++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.GOLD_NUGGET) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.GOLD_NUGGET).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 64)) {
							for (int index11 = 0; index11 < 6; index11++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.ECHO_SHARD) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(Items.ECHO_SHARD).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
					if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModBlocks.TARROCK.get().asItem()) {
						if (!world.isClientSide()) {
							BlockPos _bp = BlockPos.containing(x, y, z);
							BlockEntity _blockEntity = world.getBlockEntity(_bp);
							BlockState _bs = world.getBlockState(_bp);
							if (_blockEntity != null)
								_blockEntity.getPersistentData().putDouble("progress", 0);
							if (world instanceof Level _level)
								_level.sendBlockUpdated(_bp, _bs, _bs, 3);
						}
						if (world instanceof ILevelExtension _ext) {
							IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
							if (_entityStorage != null)
								_entityStorage.extractEnergy(1024, false);
						}
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							int _slotid = 0;
							ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
							_stk.shrink(1);
							_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
						}
						for (int index12 = 0; index12 < 6; index12++) {
							if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
									|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Items.GOLD_NUGGET) {
								if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
									if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
										ItemStack _setstack = new ItemStack(Items.GOLD_NUGGET).copy();
										_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount * 1.7));
										_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
									}
									slotnumbercheck = 1;
									break;
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							} else {
								slotnumbercheck = 1 + slotnumbercheck;
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 4)) {
							for (int index13 = 0; index13 < 6; index13++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == CrystalnexusModItems.SILICON.get()) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(CrystalnexusModItems.SILICON.get()).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
						if (1 == Mth.nextInt(RandomSource.create(), 1, 64)) {
							for (int index14 = 0; index14 < 6; index14++) {
								if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == Blocks.AIR.asItem()
										|| (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).copy()).getItem() == CrystalnexusModItems.NETHERITE_SCRAP_DUST.get()) {
									if (64 != itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount()) {
										if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
											ItemStack _setstack = new ItemStack(CrystalnexusModItems.NETHERITE_SCRAP_DUST.get()).copy();
											_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) slotnumbercheck).getCount() + outputAmount));
											_itemHandlerModifiable.setStackInSlot((int) slotnumbercheck, _setstack);
										}
										slotnumbercheck = 1;
										break;
									} else {
										slotnumbercheck = 1 + slotnumbercheck;
									}
								} else {
									slotnumbercheck = 1 + slotnumbercheck;
								}
							}
						}
					}
				}
			}
		}
		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
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