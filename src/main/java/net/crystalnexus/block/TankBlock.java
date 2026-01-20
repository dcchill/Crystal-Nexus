package net.crystalnexus.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.core.BlockPos;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.crystalnexus.block.entity.TankBlockEntity;

public class TankBlock extends Block implements EntityBlock {
	public TankBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GLASS).strength(0.8f, 6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TankBlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}
@Override
public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
    super.tick(state, level, pos, random);
    net.crystalnexus.block.entity.TankNetwork.rebuildAround(level, pos);
}

@Override
public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
    super.onPlace(state, level, pos, oldState, isMoving);
    net.crystalnexus.block.entity.TankNetwork.rebuildAround(level, pos);
}


@Override
public void neighborChanged(BlockState state, Level level, BlockPos pos,
                            net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
    super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    if (!level.isClientSide) net.crystalnexus.block.entity.TankNetwork.rebuildAround(level, pos);
}





@Override
public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
	net.crystalnexus.block.entity.TankNetwork.rebuildAround(level, pos);
    super.onRemove(state, level, pos, newState, isMoving);
    if (!level.isClientSide) net.crystalnexus.block.entity.TankNetwork.rebuildAround(level, pos);
}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}
}