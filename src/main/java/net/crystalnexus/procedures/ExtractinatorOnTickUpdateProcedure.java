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
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;

public class ExtractinatorOnTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(net.crystalnexus.util.MachineUpgradeHelper.upgrades(machine, 7, 8)), () -> executeSingle(world, x, y, z));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
        var machine = world.getBlockEntity(BlockPos.containing(x, y, z));
        if (world.isClientSide()) return false;
		double cookTime = 0;
		var upgrade = MachineUpgradeHelper.upgrades(world.getBlockEntity(BlockPos.containing(x, y, z)), 7, 8);
		if (net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, BlockPos.containing(x, y, z), getBlockNBTNumber(world, BlockPos.containing(x, y, z), "progress"))) {
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
		cookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);
		cookTime = MachineUpgradeHelper.cookTime(upgrade, cookTime);
		if (!world.isClientSide()) {
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockEntity _blockEntity = world.getBlockEntity(_bp);
			BlockState _bs = world.getBlockState(_bp);
			if (_blockEntity != null)
				_blockEntity.getPersistentData().putDouble("maxProgress", cookTime);
			if (world instanceof Level _level)
				_level.sendBlockUpdated(_bp, _bs, _bs, 3);
		}
		ItemStack input = itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0);
		int baseEnergy = input.is(ItemTags.create(ResourceLocation.parse("c:sands"))) || input.is(Blocks.GRAVEL.asItem()) ? 4096 : 1024;
		int requiredEnergy = Math.min(energyCost(world, BlockPos.containing(x, y, z), upgrade, baseEnergy), net.crystalnexus.config.CrystalnexusConfig.MACHINES.EXTRACTINATOR.maxExtract());
		if (requiredEnergy <= getEnergyStored(world, BlockPos.containing(x, y, z), null)) {
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
                    BlockPos _bp = BlockPos.containing(x, y, z);
                    java.util.List<ItemStack> drops = new java.util.ArrayList<>();
                    if (input.is(ItemTags.create(ResourceLocation.parse("c:sands")))) {
                        drops.add(new ItemStack(Items.IRON_NUGGET));
                        if (rareDrop(world, _bp, 8)) drops.add(new ItemStack(CrystalnexusModItems.ANCIENT_CRYSTAL.get()));
                    } else if (input.is(Blocks.GRAVEL.asItem())) {
                        drops.add(new ItemStack(Items.RAW_COPPER));
                        if (rareDrop(world, _bp, 3)) drops.add(new ItemStack(Items.COAL));
                    } else if (input.is(Blocks.COBBLESTONE.asItem())) {
                        drops.add(new ItemStack(Items.IRON_NUGGET));
                        if (rareDrop(world, _bp, 8)) drops.add(new ItemStack(Items.GOLD_NUGGET));
                        if (rareDrop(world, _bp, 125)) drops.add(new ItemStack(Items.DIAMOND));
                    } else if (input.is(Blocks.SOUL_SAND.asItem())) {
                        drops.add(new ItemStack(Items.GOLD_NUGGET));
                        if (rareDrop(world, _bp, 300)) drops.add(new ItemStack(Items.NETHERITE_SCRAP));
                    } else if (input.is(Blocks.COBBLED_DEEPSLATE.asItem())) {
                        drops.add(new ItemStack(Items.IRON_NUGGET));
                        if (rareDrop(world, _bp, 8)) drops.add(new ItemStack(Items.GOLD_NUGGET));
                    } else if (input.is(CrystalnexusModBlocks.TARROCK.get().asItem())) {
                        drops.add(new ItemStack(Items.GOLD_NUGGET));
                        if (rareDrop(world, _bp, 4)) drops.add(new ItemStack(CrystalnexusModItems.SILICON.get()));
                        if (rareDrop(world, _bp, 64)) drops.add(new ItemStack(CrystalnexusModItems.CARBON_COMPOSITE.get()));
                    }
                    if (drops.isEmpty() || !(world instanceof ILevelExtension ext)
                        || !(ext.getCapability(Capabilities.ItemHandler.BLOCK, _bp, null) instanceof IItemHandlerModifiable inventory)) return false;
                    ItemStack[] planned = new ItemStack[6];
                    for (int i = 0; i < planned.length; i++) planned[i] = inventory.getStackInSlot(i + 1).copy();
                    for (ItemStack drop : drops) {
                        ItemStack remaining = (drop).copy();
                        for (int i = 0; i < planned.length && !remaining.isEmpty(); i++) {
                            if (!planned[i].isEmpty() && !ItemStack.isSameItemSameComponents(planned[i], remaining)) continue;
                            int added = Math.min(remaining.getCount(), net.crystalnexus.util.MachineItemStorage.slotLimit((net.minecraft.world.Container) machine, i + 1, remaining) - planned[i].getCount());
                            if (added <= 0) continue;
                            if (planned[i].isEmpty()) planned[i] = remaining.copyWithCount(added);
                            else planned[i].grow(added);
                            remaining.shrink(added);
                        }
                        if (!remaining.isEmpty()) return false;
                    }
                    for (int i = 0; i < planned.length; i++) inventory.setStackInSlot(i + 1, planned[i]);
                    inventory.setStackInSlot(0, input.copyWithCount(input.getCount() - 1));
                    IEnergyStorage energy = ext.getCapability(Capabilities.EnergyStorage.BLOCK, _bp, null);
                    if (energy != null) energy.extractEnergy(requiredEnergy, false);
                    machine.getPersistentData().putDouble("progress", 0);
                    machine.setChanged();
                    completed = true;
                }
            }
        }
		return completed;
    }

	static int energyCost(LevelAccessor world, BlockPos pos, java.util.List<ItemStack> upgrade, int baseEnergy) {
		return MachineTier.from(world.getBlockState(pos)).energyCost(MachineUpgradeHelper.energyCost(upgrade, baseEnergy));
	}

	static boolean rareDrop(LevelAccessor world, BlockPos pos, int baseOdds) {
		return rareDrop(world, pos, baseOdds, 1);
	}

	static boolean rareDrop(LevelAccessor world, BlockPos pos, int baseOdds, int abundance) {
		int successfulRolls = (MachineTier.from(world.getBlockState(pos)) == MachineTier.TITANIUM ? 2 : 1) * abundance;
		return Mth.nextInt(RandomSource.create(), 1, baseOdds) <= successfulRolls;
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
