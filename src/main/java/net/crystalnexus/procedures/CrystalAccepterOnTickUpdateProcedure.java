package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.crystalnexus.init.CrystalnexusModItems;

import java.util.Comparator;

public class CrystalAccepterOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
		double crystalCount = 0;
		double rangeCount = 0;
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getCount() > 0) {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
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
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == CrystalnexusModItems.RANGE_UPGRADE.get()) {
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("rangeCount", 9);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).copy()).getItem() == CrystalnexusModItems.CARBON_RANGE_UPGRADE.get()) {
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("rangeCount", 12);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		} else {
			if (!world.isClientSide()) {
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockEntity _blockEntity = world.getBlockEntity(_bp);
				BlockState _bs = world.getBlockState(_bp);
				if (_blockEntity != null)
					_blockEntity.getPersistentData().putDouble("rangeCount", 6);
				if (world instanceof Level _level)
					_level.sendBlockUpdated(_bp, _bs, _bs, 3);
			}
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.STABLE_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (512 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 20)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.CONTROLLED_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (1024 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 25)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.REGULATED_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (2048 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 30)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.ULTIMATE_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (4096 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 50)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.BLUTONIUM_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (8192 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 50)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.GODLIKE_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (10240 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 75)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.DRAGON_CRYSTAL.get()) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands()
									.performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
													.withSuppressedOutput(),
											("data modify entity @e[type=minecraft:end_crystal,limit=1,sort=nearest] beam_target set value [I; " + Math.round(x) + ", " + Math.round(y - 2) + ", " + Math.round(z) + "]"));
						crystalCount = crystalCount + 1;
					}
				}
			}
			if (world instanceof ILevelExtension _ext) {
				IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y - 1, z), Direction.UP);
				if (_entityStorage != null)
					_entityStorage.receiveEnergy((int) (2048 * crystalCount), false);
			}
			if (1 <= crystalCount) {
				if (1 == Mth.nextInt(RandomSource.create(), 1, 50)) {
					if (world instanceof ILevelExtension _ext && world instanceof ServerLevel _serverLevel
							&& _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.hurtAndBreak(1, _serverLevel, null, _stkprov -> {
						});
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}
				}
			}
		} else {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate((getBlockNBTNumber(world, BlockPos.containing(x, y, z), "rangeCount")) / 2d), e -> true).stream()
						.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (entityiterator instanceof EndCrystal) {
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(
									new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
											.withSuppressedOutput(),
									"summon end_crystal ~ ~ ~ {ShowBottom:0b}");
						if (world instanceof ServerLevel _level)
							_level.getServer().getCommands().performPrefixedCommand(
									new CommandSourceStack(CommandSource.NULL, new Vec3((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null)
											.withSuppressedOutput(),
									"kill @e[type=minecraft:end_crystal,limit=1,sort=nearest]");
					}
				}
			}
			crystalCount = 0;
		}
		return new java.text.DecimalFormat("MB : ##.##").format(getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null));
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private static double getBlockNBTNumber(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getDouble(tag);
		return -1;
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