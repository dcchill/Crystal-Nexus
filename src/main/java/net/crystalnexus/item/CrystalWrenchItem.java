package net.crystalnexus.item;

import net.crystalnexus.block.*;
import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.util.WrenchLinePlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

public class CrystalWrenchItem extends Item {

    public CrystalWrenchItem() {
        super(new Item.Properties().stacksTo(1).durability(200));
    }
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();

        if (player == null) {
            return InteractionResult.PASS;
        }

        Block block = level.getBlockState(pos).getBlock();
        if (!player.isShiftKeyDown() && (block instanceof ConveyerBeltBlock
                || block instanceof ConveyerBeltInputBlock
                || block instanceof ConveyerBeltOutputBlock)) {
            if (!level.isClientSide) {
                ConveyerBeltMode.Mode mode = ConveyerBeltMode.cycle(level, pos);
                player.displayClientMessage(Component.literal("Conveyor Belt: " + mode), true);
                playWrenchSound(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        InteractionResult linePlacement = WrenchLinePlacement.use(context);
        if (linePlacement != InteractionResult.PASS) {
            return linePlacement;
        }

        if (level.getBlockState(pos).getBlock() instanceof PipeStraightBlock) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PipeStraightBlockEntity pipe) {
                Direction side = PipeStraightBlock.connectionAt(
                    level.getBlockState(pos), pos, context.getClickLocation(), context.getClickedFace());
                int mode = pipe.cycleSideMode(side);
                player.displayClientMessage(Component.literal("Fluid pipe " + side.getName() + ": "
                    + switch (mode) {
                        case 1 -> "INPUT";
                        case 2 -> "OUTPUT";
                        default -> "DEFAULT";
                    }), true);
                playWrenchSound(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (!canPickup(level, pos)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                tryPickupBlock(context, stack);
            }

            return InteractionResult.SUCCESS;
        }


        if (!canRotate(level, pos)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            tryRotateBlock(context, stack);
        }



        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    private InteractionResult tryRotateBlock(
            UseOnContext context,
            ItemStack stack
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);


        if (state.getBlock() instanceof DepotCableBlock) {
            DepotCableMode currentMode =
                    state.getValue(DepotCableBlock.MODE);

            DepotCableMode nextMode =
                    currentMode.next();

            BlockState newState =
                    state.setValue(
                            DepotCableBlock.MODE,
                            nextMode
                    );

            level.setBlockAndUpdate(
                    pos,
                    newState
            );


            if (nextMode == DepotCableMode.IMPORT || nextMode == DepotCableMode.EXPORT) {
                level.scheduleTick(
                        pos,
                        state.getBlock(),
                        1
                );
            }


            Player player = context.getPlayer();

            if (player != null) {
                Component modeMessage = switch (nextMode) {
                    case DEFAULT -> Component.literal(
                                    "Depot Cable Mode: Default"
                            )
                            .withStyle(
                                    net.minecraft.ChatFormatting.GRAY
                            );

                    case IMPORT -> Component.literal(
                                    "Depot Cable Mode: Import"
                            )
                            .withStyle(
                                    net.minecraft.ChatFormatting.GREEN
                            );

                    case EXPORT -> Component.literal(
                                    "Depot Cable Mode: Export (Whitelist Only)"
                            )
                            .withStyle(
                                    net.minecraft.ChatFormatting.AQUA
                            );

                    default -> Component.literal(
                                    "Depot Cable Mode: "
                                            + formatModeName(nextMode)
                            )
                            .withStyle(
                                    net.minecraft.ChatFormatting.YELLOW
                            );
                };

                player.displayClientMessage(
                        modeMessage,
                        true
                );
            }

            playWrenchSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }

        if (state.getBlock() instanceof ChestBlock) {
            return rotateChest(
                    level,
                    pos,
                    state,
                    context.getPlayer()
            );
        }


        BlockState rotated =
                state.rotate(
                        level,
                        pos,
                        Rotation.CLOCKWISE_90
                );

        if (rotated != state) {
            level.setBlockAndUpdate(
                    pos,
                    rotated
            );

            playWrenchSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }


        BlockState rotatedState =
                rotateDirectional(state);

        if (rotatedState != null
                && rotatedState != state) {

            level.setBlockAndUpdate(
                    pos,
                    rotatedState
            );

            playWrenchSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    private InteractionResult rotateChest(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {

        BlockState rotatedState =
                state.rotate(
                        level,
                        pos,
                        Rotation.CLOCKWISE_90
                );

        if (rotatedState == state) {
            return InteractionResult.PASS;
        }


        Direction oldPartnerDirection =
                ChestBlock.getConnectedDirection(state);

        BlockPos oldPartnerPos =
                pos.relative(oldPartnerDirection);

        BlockState oldPartnerState =
                level.getBlockState(oldPartnerPos);

        /*
         * Confirm this is actually a connected double chest.
         *
         * This prevents single chests from accidentally treating
         * some neighboring chest as their partner.
         */
        boolean hasPartner =
                oldPartnerState.getBlock() == state.getBlock()
                        && oldPartnerState.getBlock() instanceof ChestBlock
                        && ChestBlock.getConnectedDirection(oldPartnerState)
                        == oldPartnerDirection.getOpposite();

        /*
         * =========================================================
         * SINGLE CHEST
         * =========================================================
         */
        if (!hasPartner) {
            level.setBlockAndUpdate(
                    pos,
                    rotatedState
            );

            playWrenchSound(
                    level,
                    pos
            );

            return InteractionResult.SUCCESS;
        }

        /*
         * =========================================================
         * DOUBLE CHEST
         * =========================================================
         *
         * Once the clicked half rotates, find where its partner
         * should now be located.
         */
        Direction newPartnerDirection =
                ChestBlock.getConnectedDirection(rotatedState);

        BlockPos newPartnerPos =
                pos.relative(newPartnerDirection);

        /*
         * Don't rotate the chest if the destination is occupied.
         */
        if (!newPartnerPos.equals(oldPartnerPos)
                && !level.getBlockState(newPartnerPos).isAir()) {

            if (player != null) {
                player.displayClientMessage(
                        Component.literal(
                                        "Can't rotate double chest: space is blocked"
                                )
                                .withStyle(
                                        net.minecraft.ChatFormatting.RED
                                ),
                        true
                );
            }

            return InteractionResult.FAIL;
        }

        /*
         * Rotate the partner's state too.
         */
        BlockState rotatedPartnerState =
                oldPartnerState.rotate(
                        level,
                        oldPartnerPos,
                        Rotation.CLOCKWISE_90
                );

        /*
         * Save the partner chest's BlockEntity data before moving it.
         *
         * This preserves the half of the inventory stored in that
         * chest's BlockEntity.
         */
        BlockEntity oldPartnerEntity =
                level.getBlockEntity(oldPartnerPos);

        CompoundTag partnerData = null;

        if (oldPartnerEntity != null) {
            partnerData =
                    oldPartnerEntity.saveWithFullMetadata(
                            level.registryAccess()
                    );

            /*
             * Clear the old inventory before removing the block.
             *
             * This prevents ChestBlock.onRemove() from dumping
             * all the items into the world while we move it.
             */
            if (oldPartnerEntity instanceof Container container) {
                container.clearContent();
            }
        }

        /*
         * Remove the old partner location without immediately
         * causing normal neighbor updates.
         */
        level.setBlock(
                oldPartnerPos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_CLIENTS
        );

        /*
         * Rotate clicked chest.
         */
        level.setBlock(
                pos,
                rotatedState,
                Block.UPDATE_CLIENTS
        );

        /*
         * Place partner at its new position.
         */
        level.setBlock(
                newPartnerPos,
                rotatedPartnerState,
                Block.UPDATE_CLIENTS
        );

        /*
         * Restore partner BlockEntity data.
         */
        if (partnerData != null) {
            BlockEntity newPartnerEntity =
                    level.getBlockEntity(newPartnerPos);

            if (newPartnerEntity != null) {
                newPartnerEntity.loadWithComponents(
                        partnerData,
                        level.registryAccess()
                );

                newPartnerEntity.setChanged();
            }
        }

        /*
         * Now that both chest halves are correctly positioned,
         * perform neighbor updates.
         */
        level.updateNeighborsAt(
                pos,
                rotatedState.getBlock()
        );

        level.updateNeighborsAt(
                newPartnerPos,
                rotatedPartnerState.getBlock()
        );

        level.updateNeighborsAt(
                oldPartnerPos,
                Blocks.AIR
        );

        /*
         * Explicitly synchronize changed states to clients.
         */
        level.sendBlockUpdated(
                pos,
                state,
                rotatedState,
                Block.UPDATE_ALL
        );

        level.sendBlockUpdated(
                newPartnerPos,
                Blocks.AIR.defaultBlockState(),
                rotatedPartnerState,
                Block.UPDATE_ALL
        );

        level.sendBlockUpdated(
                oldPartnerPos,
                oldPartnerState,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
        );

        playWrenchSound(
                level,
                pos
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Attempts to rotate a DirectionProperty called "facing".
     */
    private BlockState rotateDirectional(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty
                    && directionProperty.getName().equals("facing")) {

                Direction currentDirection =
                        state.getValue(directionProperty);

                Direction newDirection =
                        rotateDirection(currentDirection);

                return state.setValue(
                        directionProperty,
                        newDirection
                );
            }
        }

        return null;
    }

    /**
     * Rotates a direction clockwise.
     */
    private static Direction rotateDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;

            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
        };
    }

    /**
     * Shift-right-click dismantle behavior.
     */
    private InteractionResult tryPickupBlock(
            UseOnContext context,
            ItemStack stack
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState state =
                level.getBlockState(pos);

        Block block =
                state.getBlock();

        /*
         * Never allow removal of air or bedrock.
         */
        if (block == Blocks.AIR
                || block == Blocks.BEDROCK) {

            return InteractionResult.PASS;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        boolean hasBlockEntity =
                blockEntity != null;

        /*
         * Cables don't necessarily have BlockEntities.
         */
        boolean isCable =
                isCableBlock(state);

        /*
         * Only dismantle:
         *
         * - machines / BlockEntities
         * - cable-like blocks
         */
        if (!hasBlockEntity
                && !isCable) {

            return InteractionResult.PASS;
        }

        /*
         * Create the block item before removing the block.
         */
        boolean isConveyor = block instanceof ConveyerBeltBlock
                || block instanceof ConveyerBeltInputBlock
                || block instanceof ConveyerBeltOutputBlock;
        boolean preserveBeltContents = blockEntity instanceof ConveyerBeltBaseBlockEntity belt && !belt.isEmpty();
        ItemStack blockItem = new ItemStack(isConveyor ? ConveyerBeltMode.normalBlock(state) : block.asItem());

        /*
         * Preserve BlockEntity data.
         */
        if (blockEntity != null && (!isConveyor || preserveBeltContents)) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    blockItem,
                    tag -> {

                        CompoundTag blockEntityTag =
                                blockEntity.saveWithFullMetadata(
                                        level.registryAccess()
                                );

                        tag.put(
                                "BlockEntityTag",
                                blockEntityTag
                        );
                    }
            );
        }

        if (isConveyor && blockEntity instanceof ConveyerBeltBaseBlockEntity belt) {
            belt.clearContent();
        }

        /*
         * Remove block.
         */
        level.removeBlock(
                pos,
                false
        );

        /*
         * Play normal breaking particles.
         */
        level.levelEvent(
                2001,
                pos,
                Block.getId(state)
        );

        /*
         * Drop the resulting block item.
         */
        if (!blockItem.isEmpty()) {
            Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    blockItem
            );
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Checks whether shift-right-click pickup should be handled.
     */
    private boolean canPickup(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        Block block =
                state.getBlock();

        if (block == Blocks.AIR
                || block == Blocks.BEDROCK) {

            return false;
        }

        boolean hasBlockEntity =
                level.getBlockEntity(pos) != null;

        boolean isCable =
                isCableBlock(state);

        boolean isMachineFrame =
                block instanceof MachineFrameBlock
                || block instanceof CrystalMachineFrameBlock
                || block instanceof ChlorophyteMachineFrameBlock
                || block instanceof InvertiumMachineFrameBlock
                || block instanceof CarbonMachineFrameBlock
                || block instanceof HyperMachineFrameBlock;

        boolean isBlueprint =
                block instanceof BlueprintBaseBlock
                || block instanceof BlueprintFrameBlock
                || block instanceof BlueprintControllerBlock;

        boolean isStorage = block instanceof TankBlock;

        return hasBlockEntity
                || isCable
                || isMachineFrame
                || isBlueprint
                || isStorage;
    }

    /**
     * Checks whether the wrench should consume the interaction.
     */
    private boolean canRotate(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        /*
         * Depot cables always support mode cycling.
         */
        if (state.getBlock() instanceof DepotCableBlock) {
            return true;
        }

        /*
         * Chests use custom rotation logic.
         */
        if (state.getBlock() instanceof ChestBlock) {
            return true;
        }

        /*
         * Native block rotation.
         */
        BlockState rotated =
                state.rotate(
                        level,
                        pos,
                        Rotation.CLOCKWISE_90
                );

        if (rotated != state) {
            return true;
        }

        /*
         * Manual facing-property fallback.
         */
        BlockState rotatedState =
                rotateDirectional(state);

        return rotatedState != null
                && rotatedState != state;
    }

    /**
     * Converts enum names like IMPORT -> Import.
     */
    private String formatModeName(
            DepotCableMode mode
    ) {
        String name =
                mode.name().toLowerCase();

        return Character.toUpperCase(
                name.charAt(0)
        ) + name.substring(1);
    }

    /**
     * Detect cable-like blocks through directional connection
     * properties.
     */
    private boolean isCableBlock(
            BlockState state
    ) {
        for (Property<?> property : state.getProperties()) {
            String name =
                    property.getName();

            if (name.equals("north")
                    || name.equals("east")
                    || name.equals("south")
                    || name.equals("west")) {

                return true;
            }
        }

        return false;
    }

    /**
     * Wrench click sound.
     */
    private void playWrenchSound(
            Level level,
            BlockPos pos
    ) {
        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.BLOCKS,
                0.5f,
                1.0f
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(
                stack,
                context,
                tooltip,
                flag
        );

        tooltip.add(
                Component.literal(
                        "§7Right-click: Rotate blocks / Cycle depot cable modes"
                )
        );

        tooltip.add(
                Component.literal(
                        "§7Shift+Right-click: Dismantle machines, frames, blueprints, storage, and cables"
                )
        );

        tooltip.add(
                Component.literal(
                        "§7Block offhand: Select two points; sneak-use cycles direction"
                )
        );
    }
}
