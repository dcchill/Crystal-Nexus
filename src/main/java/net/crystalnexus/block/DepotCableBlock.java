package net.crystalnexus.block;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.entity.DepotCableBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

public class DepotCableBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final EnumProperty<DepotCableMode> MODE = EnumProperty.create("mode", DepotCableMode.class);
    public static final TagKey<Block> COMPONENTS = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(CrystalnexusMod.MODID, "depot_components"));
    private static final ResourceLocation AE2_CONTROLLER =
            ResourceLocation.fromNamespaceAndPath("ae2", "controller");
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape ARM_NORTH = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape ARM_EAST = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ARM_DOWN = Block.box(6, 0, 6, 10, 6, 10);
    private static final VoxelShape ARM_UP = Block.box(6, 10, 6, 10, 16, 10);

    public DepotCableBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
                .setValue(MODE, DepotCableMode.DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, MODE);
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotCableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DepotCableBlockEntity cable)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            cable.refreshConnections();
            if (cable.connections().isEmpty()) return InteractionResult.PASS;
            cable.setOpeningSide(hit.getDirection());
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(cable, buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(level, pos, state);
    }

    private BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.setValue(NORTH, connects(level, pos, Direction.NORTH))
                .setValue(EAST, connects(level, pos, Direction.EAST))
                .setValue(SOUTH, connects(level, pos, Direction.SOUTH))
                .setValue(WEST, connects(level, pos, Direction.WEST))
                .setValue(UP, connects(level, pos, Direction.UP))
                .setValue(DOWN, connects(level, pos, Direction.DOWN));
    }

    private boolean connects(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        return neighbor.is(this) || neighbor.is(COMPONENTS) || isAe2Controller(neighbor)
                || level instanceof ServerLevel serverLevel
                && DepotNetwork.hasTransferHandler(serverLevel, neighborPos);
    }

    public static boolean isAe2Controller(BlockState state) {
        return AE2_CONTROLLER.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_DOWN);
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP);
        return shape;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) DepotNetwork.invalidate(serverLevel);
        if (level instanceof ServerLevel serverLevel && isAutomatedMode(state)) {
            serverLevel.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof DepotCableBlockEntity cable) cable.refreshConnections();
        if (level instanceof ServerLevel serverLevel) DepotNetwork.invalidate(serverLevel);
        if (level instanceof ServerLevel serverLevel && isAutomatedMode(state)) {
            serverLevel.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !state.is(newState.getBlock())) DepotNetwork.invalidate(serverLevel);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isImportMode(state)) importFromNeighbors(level, pos);
        else if (isExportMode(state)) exportToNeighbors(level, pos);
        else return;

        level.scheduleTick(pos, this, 20);
    }

    public static boolean isImportMode(BlockState state) {
        return state.hasProperty(MODE) && state.getValue(MODE) == DepotCableMode.IMPORT;
    }

    public static boolean isExportMode(BlockState state) {
        return state.hasProperty(MODE) && state.getValue(MODE) == DepotCableMode.EXPORT;
    }

    public static boolean isDefaultMode(BlockState state) {
        return state.hasProperty(MODE) && state.getValue(MODE) == DepotCableMode.DEFAULT;
    }

    private static boolean isAutomatedMode(BlockState state) {
        return isImportMode(state) || isExportMode(state);
    }

    private void exportListedToNeighbors(ServerLevel level, BlockPos pos) {
        var owner = DepotNetwork.componentOwner(level, pos);
        if (owner == null) return;
        DepotNetwork.exportListedFromCable(level, pos, DepotSavedData.get(level, owner), 64);
    }

    private void exportToNeighbors(ServerLevel level, BlockPos pos) {
        var owner = DepotNetwork.componentOwner(level, pos);
        if (owner == null) return;
        DepotSavedData depot = DepotSavedData.get(level, owner);
        exportListedToNeighbors(level, pos);
        DepotNetwork.exportFluidsFromCable(level, pos, depot, 1_000);
        var controller = DepotSavedData.getController(level, owner);
        if (controller != null) DepotNetwork.transferEnergyFromCable(level, pos, controller, false, 20_4800);
    }

    private void importFromNeighbors(ServerLevel level, BlockPos pos) {
        var energyOwner = DepotNetwork.energyImportOwner(level, pos);
        if (energyOwner != null) {
            var controller = DepotSavedData.getController(level, energyOwner);
            if (controller != null) DepotNetwork.transferEnergyFromCable(level, pos, controller, true, 20_480);
        }
        var owner = DepotNetwork.componentOwner(level, pos);
        if (owner == null) return;

        DepotSavedData depot = DepotSavedData.get(level, owner);
        if (depot == null) return;

        DepotNetwork.importFluidsFromCable(level, pos, depot, 1_000);

        int remaining = 64; // Maximum items imported per tick

        for (Direction direction : Direction.values()) {
            if (remaining <= 0) return;

            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) continue;

            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    neighborPos,
                    direction.getOpposite()
            );

            if (handler == null) {
                handler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        neighborPos,
                        null
                );
            }

            if (handler == null) continue;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (remaining <= 0) return;

                ItemStack simulated = handler.extractItem(slot, remaining, true);
                if (simulated.isEmpty()) continue;

                ResourceLocation itemId =
                        BuiltInRegistries.ITEM.getKey(simulated.getItem());

                if (itemId == null) continue;

                long accepted = depot.addCapped(
                        itemId,
                        Math.min(simulated.getCount(), remaining)
                );

                if (accepted <= 0) continue;

                ItemStack actual = handler.extractItem(
                        slot,
                        (int) accepted,
                        false
                );

                int actuallyExtracted = actual.getCount();

                if (actuallyExtracted < accepted) {
                    depot.remove(itemId, accepted - actuallyExtracted);
                }

                remaining -= actuallyExtracted;
            }
        }
    }
}
