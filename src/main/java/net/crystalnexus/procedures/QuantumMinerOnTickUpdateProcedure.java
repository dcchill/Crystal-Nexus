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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModItems;

import java.util.concurrent.ThreadLocalRandom;

public class QuantumMinerOnTickUpdateProcedure {
	private static final int ENERGY_PER_CYCLE = 40960;
	private static final int BASE_COOK_TIME = 20;
	private static final int ACCELERATED_COOK_TIME = 12;
	private static final int CARBON_ACCELERATED_COOK_TIME = 6;
	private static final WeightedOutput[] OUTPUTS = {
			new WeightedOutput("minecraft:raw_iron", 18),
			new WeightedOutput("minecraft:raw_copper", 18),
			new WeightedOutput("minecraft:coal", 14),
			new WeightedOutput("minecraft:redstone", 12),
			new WeightedOutput("minecraft:lapis_lazuli", 10),
			new WeightedOutput("minecraft:raw_gold", 8),
			new WeightedOutput("minecraft:diamond", 3),
			new WeightedOutput("minecraft:emerald", 2),
			new WeightedOutput("crystalnexus:ancient_crystal", 5),
			new WeightedOutput("crystalnexus:raw_carbon", 4),
			new WeightedOutput("crystalnexus:sulfur_dust", 4)
	};

	public static String execute(LevelAccessor world, double x, double y, double z) {
		int outputAmount;
		int cookTime;
		if (ENERGY_PER_CYCLE > getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
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
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getItem() == CrystalnexusModItems.EFFICIENCY_UPGRADE.get()) {
			outputAmount = 2;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getItem() == CrystalnexusModItems.CARBON_EFFICIENCY_UPGRADE.get()) {
			outputAmount = 3;
		} else {
			outputAmount = 1;
		}
		if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get()) {
			cookTime = ACCELERATED_COOK_TIME;
		} else if ((itemFromBlockInventory(world, BlockPos.containing(x, y, z), 9).copy()).getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) {
			cookTime = CARBON_ACCELERATED_COOK_TIME;
		} else {
			cookTime = BASE_COOK_TIME;
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
		if (ENERGY_PER_CYCLE <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
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
					_level.sendParticles(ParticleTypes.ENCHANT, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.125, 0.25, 0.125, 0.1);
			}
			if (getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress") >= cookTime) {
				ItemStack output = randomOutput(world, outputAmount);
				if (output.isEmpty() || !insertOutput(world, BlockPos.containing(x, y, z), output)) {
					return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
				}
				if (world instanceof ILevelExtension _ext) {
					IEnergyStorage _entityStorage = _ext.getCapability(Capabilities.EnergyStorage.BLOCK, BlockPos.containing(x, y, z), null);
					if (_entityStorage != null)
						_entityStorage.extractEnergy(ENERGY_PER_CYCLE, false);
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
			}
		}
		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, BlockPos.containing(x, y, z), null));
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension levelExtension) {
			IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energyStorage != null)
				return energyStorage.getEnergyStored();
		}
		return 0;
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

	private static ItemStack randomOutput(LevelAccessor world, int amount) {
		int totalWeight = 0;
		for (WeightedOutput output : OUTPUTS) {
			if (output.item() != Items.AIR) {
				totalWeight += output.weight;
			}
		}
		if (totalWeight <= 0) {
			return ItemStack.EMPTY;
		}

		int roll = world instanceof Level level ? level.random.nextInt(totalWeight) : ThreadLocalRandom.current().nextInt(totalWeight);
		for (WeightedOutput output : OUTPUTS) {
			Item item = output.item();
			if (item == Items.AIR) {
				continue;
			}
			roll -= output.weight;
			if (roll < 0) {
				return new ItemStack(item, amount);
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean insertOutput(LevelAccessor world, BlockPos pos, ItemStack output) {
		if (!(world instanceof ILevelExtension ext && ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable itemHandler)) {
			return false;
		}
		for (int slot = 0; slot < 9; slot++) {
			ItemStack existing = itemHandler.getStackInSlot(slot);
			if (existing.isEmpty() || existing.getItem() == Blocks.AIR.asItem()) {
				itemHandler.setStackInSlot(slot, output.copy());
				return true;
			}
			if (ItemStack.isSameItemSameComponents(existing, output) && existing.getCount() + output.getCount() <= existing.getMaxStackSize()) {
				ItemStack merged = existing.copy();
				merged.grow(output.getCount());
				itemHandler.setStackInSlot(slot, merged);
				return true;
			}
		}
		return false;
	}

	private record WeightedOutput(ResourceLocation id, int weight) {
		private WeightedOutput(String id, int weight) {
			this(ResourceLocation.parse(id), weight);
		}

		private Item item() {
			return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
		}
	}
}
