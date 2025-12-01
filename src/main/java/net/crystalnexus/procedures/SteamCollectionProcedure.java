package net.crystalnexus.procedures;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.crystalnexus.init.CrystalnexusModFluids;
import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.Comparator;

public class SteamCollectionProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double nrgstrt = 0;
		double yOffset = 0;
		double xOffset = 0;
		double T = 0;
		double zOffset = 0;
		double energy = 0;
		boolean on = false;
		{
			int _value = 1;
			BlockPos _pos = BlockPos.containing(x, y, z);
			BlockState _bs = world.getBlockState(_pos);
			if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
				world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
		}
		T = 1;
		if (CrystalnexusModBlocks.STEAM_CHAMBER.get() == (world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock()) {
			if (getBlockNBTLogic(world, BlockPos.containing(x, y - 1, z), "running")) {
				{
					int _value = 2;
					BlockPos _pos = BlockPos.containing(x, y, z);
					BlockState _bs = world.getBlockState(_pos);
					if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
						world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
				}
				if (world instanceof ILevelExtension _ext) {
					IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
					if (_fluidHandler != null)
						_fluidHandler.fill(new FluidStack(CrystalnexusModFluids.STEAM.get(), 25), IFluidHandler.FluidAction.EXECUTE);
				}
			} else {
				{
					int _value = 3;
					BlockPos _pos = BlockPos.containing(x, y, z);
					BlockState _bs = world.getBlockState(_pos);
					if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
						world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
				}
			}
		} else {
			for (int index0 = 0; index0 < 8; index0++) {
				if (Blocks.WATER == (world.getBlockState(BlockPos.containing(x, y - T, z))).getBlock()) {
					if (findEntityInWorldRange(world, ItemEntity.class, x, (y - T), z, 2) != null) {
						if (Math.floor((findEntityInWorldRange(world, ItemEntity.class, x, (y - T), z, 2)).getX()) == x && Math.floor((findEntityInWorldRange(world, ItemEntity.class, x, (y - T), z, 2)).getZ()) == z) {
							if (((findEntityInWorldRange(world, ItemEntity.class, x, (y - T), z, 2)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).is(ItemTags.create(ResourceLocation.parse("crystalnexus:radioactive")))) {
								{
									int _value = 4;
									BlockPos _pos = BlockPos.containing(x, y, z);
									BlockState _bs = world.getBlockState(_pos);
									if (_bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
										world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
								}
								if (world instanceof ILevelExtension _ext) {
									IFluidHandler _fluidHandler = _ext.getCapability(Capabilities.FluidHandler.BLOCK, BlockPos.containing(x, y, z), null);
									if (_fluidHandler != null)
										_fluidHandler.fill(new FluidStack(CrystalnexusModFluids.STEAM.get(), 25), IFluidHandler.FluidAction.EXECUTE);
								}
								break;
							}
						}
					}
					break;
				} else if (T > 8) {
					break;
				} else {
					T = T + 1;
				}
			}
		}
	}

	private static boolean getBlockNBTLogic(LevelAccessor world, BlockPos pos, String tag) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity != null)
			return blockEntity.getPersistentData().getBoolean(tag);
		return false;
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}
}