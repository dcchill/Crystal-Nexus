package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.block.BlockPlacerBlock;

public class BlockPlacerOnTickUpdateProcedure {
	private static final int ENERGY_PER_USE = 256;

	public static String execute(LevelAccessor world, double x, double y, double z) {
		BlockPos placerPos = BlockPos.containing(x, y, z);
		if (world instanceof ServerLevel level && getEnergyStored(level, placerPos, null) >= ENERGY_PER_USE
				&& level.getCapability(Capabilities.ItemHandler.BLOCK, placerPos, null) instanceof IItemHandlerModifiable handler
				&& !handler.getStackInSlot(0).isEmpty()) {
			Direction facing = level.getBlockState(placerPos).getValue(BlockPlacerBlock.FACING);
			useItem(level, placerPos, facing, handler);
		}
		return new java.text.DecimalFormat("FE: ##.##").format(getEnergyStored(world, placerPos, null));
	}

	private static void useItem(ServerLevel level, BlockPos placerPos, Direction facing, IItemHandlerModifiable handler) {
		FakePlayer player = FakePlayerFactory.getMinecraft(level);
		double eyeY = placerPos.getY() + 0.5;
		player.setPos(placerPos.getX() + 0.5, eyeY - player.getEyeHeight(), placerPos.getZ() + 0.5);
		player.setYRot(facing.toYRot());
		player.setXRot(facing == Direction.UP ? -90 : facing == Direction.DOWN ? 90 : 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, handler.getStackInSlot(0).copy());

		BlockPos targetPos = placerPos.relative(facing);
		Vec3 hitLocation = new Vec3(targetPos.getX() + 0.5 - facing.getStepX() * 0.5,
				targetPos.getY() + 0.5 - facing.getStepY() * 0.5,
				targetPos.getZ() + 0.5 - facing.getStepZ() * 0.5);
		BlockHitResult hit = new BlockHitResult(hitLocation, facing.getOpposite(), targetPos, false);
		InteractionResult result = player.gameMode.useItemOn(player, level, player.getMainHandItem(),
				InteractionHand.MAIN_HAND, hit);
		if (result == InteractionResult.PASS) {
			result = player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
		}

		handler.setStackInSlot(0, player.getMainHandItem().copy());
		player.stopUsingItem();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		if (result.consumesAction()) {
			IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, placerPos, null);
			if (energy != null) energy.extractEnergy(ENERGY_PER_USE, false);
		}
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
