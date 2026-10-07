package net.crystalnexus.procedures;

import net.crystalnexus.util.MachineUpgradeHelper;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;

public class FluidPackagerOnTickUpdateProcedure {
	public static String execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        int crafts = machine instanceof net.minecraft.world.Container inventory && inventory.getContainerSize() > 2 && !MachineUpgradeHelper.isZeroChip(inventory.getItem(2)) ? net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(inventory.getItem(2)) : 1;
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, crafts, () -> executeSingle(world, x, y, z));
        return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
		double cookTime = 0;
		double outputAmount = 0;

		ItemStack fluidItem = ItemStack.EMPTY;
		outputAmount = 1;

		if (net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, BlockPos.containing(x, y, z), getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress"))) {
			{
				int _value = 1;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp
						&& _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		} else {
			{
				int _value = 2;
				BlockPos _pos = BlockPos.containing(x, y, z);
				BlockState _bs = world.getBlockState(_pos);
				if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp
						&& _integerProp.getPossibleValues().contains(_value))
					world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
			}
		}

		cookTime = MachineUpgradeHelper.processingTime(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 2), 50, 30, 10);

		FluidStack fs = getFluidInTank(world, BlockPos.containing(x, y, z), 1, null);
		if (!fs.isEmpty()) {
			ResourceLocation id = BuiltInRegistries.FLUID.getKey(fs.getFluid());
			if (id != null) {
				if (id.getNamespace().equals("crystalnexus") && id.getPath().equals("crude_oil")) {
					fluidItem = new ItemStack(CrystalnexusModItems.OIL_FUEL_CELL.get()).copy();
				} else if (id.getNamespace().equals("crystalnexus") && id.getPath().equals("gasoline")) {
					fluidItem = new ItemStack(CrystalnexusModItems.GAS_FUEL_CELL.get()).copy();
				}
			}
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

		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).getItem() == CrystalnexusModItems.EMPTY_FUEL_CELL.get()) {
			if (!fluidItem.isEmpty()
					&& 4096 <= getEnergyStored(world, BlockPos.containing(x, y, z), null)
					&& itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + outputAmount <= fluidItem.getMaxStackSize()
                    && (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).isEmpty() || ItemStack.isSameItemSameComponents(itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1), fluidItem))
					&& 250 <= getFluidTankLevel(world, BlockPos.containing(x, y, z), 1, null)) {

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
						_level.sendParticles(ParticleTypes.DRAGON_BREATH, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.25, 0, 0.25, 0);
				}

				if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {

					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						ItemStack _setstack = fluidItem.copy();
						_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 1).getCount() + outputAmount));
						_itemHandlerModifiable.setStackInSlot(1, _setstack);
					}

					if (world instanceof ILevelExtension _ext) {
						IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
						if (_entityStorage != null)
							_entityStorage.extractEnergy(4096, false);
					}

					if (world instanceof ILevelExtension _ext) {
						IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
						if (_fluidHandler != null)
							_fluidHandler.drain(250, IFluidHandler.FluidAction.EXECUTE);
					}

					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						int _slotid = 0;
						ItemStack _stk = _itemHandlerModifiable.getStackInSlot(_slotid).copy();
						_stk.shrink(1);
						_itemHandlerModifiable.setStackInSlot(_slotid, _stk);
					}

					if (!world.isClientSide()) {
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockEntity _blockEntity = world.getBlockEntity(_bp);
						BlockState _bs = world.getBlockState(_bp);
						if (_blockEntity != null)
							_blockEntity.getPersistentData().putDouble("progress", 0);
        completed = true;
						if (world instanceof Level _level)
							_level.sendBlockUpdated(_bp, _bs, _bs, 3);
					}
				}
			}
		}

		return completed;
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

	private static int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null)
				return fluidHandler.getFluidInTank(tank).getAmount();
		}
		return 0;
	}

	private static FluidStack getFluidInTank(LevelAccessor level, BlockPos pos, int tank, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IFluidHandler fluidHandler = levelExtension.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
			if (fluidHandler != null) {
				return fluidHandler.getFluidInTank(tank);
			}
		}
		return FluidStack.EMPTY;
	}
}
