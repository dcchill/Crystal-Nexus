package net.crystalnexus.block;

import net.crystalnexus.block.entity.CraftingUpgradeBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.util.DepotNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CraftingUpgradeBlock extends Block implements EntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CraftingUpgradeBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3f, 20f).requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(CONNECTED, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED, ACTIVE);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(ACTIVE) ? 10 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        java.util.UUID owner = DepotNetwork.craftingProcessorOwner(serverLevel, pos);
        if (owner == null) {
            player.displayClientMessage(Component.literal("Crafting Processor is not connected to a powered depot."), true);
            return InteractionResult.SUCCESS;
        }
        if (!owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.literal("You do not own this depot system."), true);
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(pos) instanceof CraftingUpgradeBlockEntity processor) {
            serverPlayer.openMenu(processor, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingUpgradeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != CrystalnexusModBlockEntities.CRAFTING_UPGRADE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> CraftingUpgradeBlockEntity.tick(
                (net.minecraft.server.level.ServerLevel) tickLevel, pos, tickState,
                (CraftingUpgradeBlockEntity) blockEntity);
    }
}
