package net.crystalnexus.block;

import org.checkerframework.checker.units.qual.s;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Containers;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;


import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.crystalnexus.network.payload.S2C_ZeroPointPreview;

import net.crystalnexus.procedures.ZeroPointMultiblockCheckProcedure;
import net.crystalnexus.procedures.CrystalPurifierBlockAddedProcedure;
import net.crystalnexus.block.entity.ZeroPointBlockEntity;

import java.util.List;

public class ZeroPointBlock extends Block implements EntityBlock {
	public static final IntegerProperty BLOCKSTATE = IntegerProperty.create("blockstate", 0, 2);

	public ZeroPointBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.ANVIL).strength(2f, 40f).lightLevel(s -> (new Object() {
			public int getLightLevel() {
				if (s.getValue(BLOCKSTATE) == 1)
					return 0;
				if (s.getValue(BLOCKSTATE) == 2)
					return 15;
				return 15;
			}
		}.getLightLevel())).noOcclusion().isRedstoneConductor((bs, br, bp) -> false).dynamicShape());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("block.crystalnexus.zero_point.description_0"));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		if (state.getValue(BLOCKSTATE) == 1) {
			return Shapes.or(box(0, 0, 0, 16, 2, 2), box(14, 14, 2, 16, 16, 14), box(0, 14, 0, 16, 16, 2), box(14, 0, 2, 16, 2, 14), box(0, 14, 2, 2, 16, 14), box(0, 0, 2, 2, 2, 14), box(0, 2, 0, 2, 14, 2), box(14, 2, 0, 16, 14, 2),
					box(0, 0, 14, 16, 2, 16), box(14, 2, 14, 16, 14, 16), box(0, 14, 14, 16, 16, 16), box(0, 2, 14, 2, 14, 16), box(2, 2, 2, 14, 14, 14), box(3, 3, 3, 13, 13, 13));
		}
		if (state.getValue(BLOCKSTATE) == 2) {
			return Shapes.or(box(0, 0, 0, 16, 2, 2), box(14, 14, 2, 16, 16, 14), box(0, 14, 0, 16, 16, 2), box(14, 0, 2, 16, 2, 14), box(0, 14, 2, 2, 16, 14), box(0, 0, 2, 2, 2, 14), box(0, 2, 0, 2, 14, 2), box(14, 2, 0, 16, 14, 2),
					box(0, 0, 14, 16, 2, 16), box(14, 2, 14, 16, 14, 16), box(0, 14, 14, 16, 16, 16), box(0, 2, 14, 2, 14, 16), box(2, 2, 2, 14, 14, 14), box(3, 3, 3, 13, 13, 13));
		}
		return Shapes.or(box(0, 0, 0, 16, 2, 2), box(14, 14, 2, 16, 16, 14), box(0, 14, 0, 16, 16, 2), box(14, 0, 2, 16, 2, 14), box(0, 14, 2, 2, 16, 14), box(0, 0, 2, 2, 2, 14), box(0, 2, 0, 2, 14, 2), box(14, 2, 0, 16, 14, 2),
				box(0, 0, 14, 16, 2, 16), box(14, 2, 14, 16, 14, 16), box(0, 14, 14, 16, 16, 16), box(0, 2, 14, 2, 14, 16), box(2, 2, 2, 14, 14, 14), box(3, 3, 3, 13, 13, 13));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BLOCKSTATE);
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		world.scheduleTick(pos, this, 1);
		CrystalPurifierBlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		ZeroPointMultiblockCheckProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 1);
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
		BlockEntity tileEntity = worldIn.getBlockEntity(pos);
		return tileEntity instanceof MenuProvider menuProvider ? menuProvider : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ZeroPointBlockEntity(pos, state);
	}
@Override
public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);

    if (!level.isClientSide && placer instanceof ServerPlayer sp) {
        PacketDistributor.sendToPlayer(sp, new S2C_ZeroPointPreview(pos, "zeropoint_multiblock_v1", 20 * 60));
    }
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
			if (blockEntity instanceof ZeroPointBlockEntity be) {
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
		if (tileentity instanceof ZeroPointBlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		else
			return 0;
	}
}