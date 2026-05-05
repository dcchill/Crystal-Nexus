package net.crystalnexus.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.entity.BlackHoleTntBlockEntity;
import net.crystalnexus.events.DarkMatterBlackHoleEvents;

public class BlackHoleTntBlock extends Block implements EntityBlock {
	private static final int FUSE_TICKS = 80;
	private static final float EXPLOSION_POWER = 4.0F;

	public BlackHoleTntBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GRASS).strength(0.0F, 14.0F));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		if (!oldState.is(blockstate.getBlock()) && world.hasNeighborSignal(pos)) {
			prime(world, pos, null);
		}
	}

	@Override
	public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
		if (world.hasNeighborSignal(pos)) {
			prime(world, pos, null);
		}
	}

	@Override
	public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
		prime(world, pos, null);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
			return super.useItemOn(stack, state, world, pos, player, hand, hit);
		}

		prime(world, pos, player);
		if (!world.isClientSide()) {
			if (stack.is(Items.FLINT_AND_STEEL)) {
				stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			} else if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return ItemInteractionResult.sidedSuccess(world.isClientSide());
	}

	private static void prime(Level world, BlockPos pos, LivingEntity igniter) {
		if (world.isClientSide()) {
			return;
		}

		if (!world.getBlockState(pos).isAir()) {
			world.removeBlock(pos, false);
		}
		world.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.82F);

		if (!(world instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 center = Vec3.atCenterOf(pos);
		serverLevel.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		CrystalnexusMod.queueServerWork(FUSE_TICKS, () -> {
			if (!serverLevel.isLoaded(pos)) {
				return;
			}

			serverLevel.explode(igniter, center.x, center.y, center.z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
			DarkMatterBlackHoleEvents.spawnBlackHole(serverLevel, center);
		});
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlackHoleTntBlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof BlackHoleTntBlockEntity be) {
				Containers.dropContents(world, pos, be);
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
		BlockEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof BlackHoleTntBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}
