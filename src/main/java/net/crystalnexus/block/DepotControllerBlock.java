package net.crystalnexus.block;

import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class DepotControllerBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public DepotControllerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.5f, 30f).requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(POWERED) ? 15 : 0)
                .emissiveRendering((state, level, pos) -> state.getValue(POWERED)));
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotControllerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller) {
            controller.setOwner(player.getUUID());
            DepotSavedData.get(serverLevel, player.getUUID()).setController(serverLevel, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller) {
            if (!serverPlayer.getUUID().equals(controller.getOwner())) {
                serverPlayer.displayClientMessage(Component.literal("This Depot Controller belongs to another player.")
                        .withStyle(ChatFormatting.RED), true);
            } else {
                String status = DepotSavedData.hasPoweredController(serverPlayer) ? "Online" : "Offline";
                serverPlayer.displayClientMessage(Component.literal("Depot Controller: " + status + " ("
                        + controller.getEnergyStorage().getEnergyStored() + " / "
                        + controller.getEnergyStorage().getMaxEnergyStored() + " FE)")
                        .withStyle(status.equals("Online") ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller
                && controller.getOwner() != null) {
            DepotSavedData.get(serverLevel, controller.getOwner()).clearController(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != CrystalnexusModBlockEntities.DEPOT_CONTROLLER.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) ->
                DepotControllerBlockEntity.tick((ServerLevel) tickLevel, pos, tickState,
                        (DepotControllerBlockEntity) blockEntity);
    }
}
