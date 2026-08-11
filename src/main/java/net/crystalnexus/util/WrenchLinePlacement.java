package net.crystalnexus.util;

import net.crystalnexus.item.CrystalWrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public final class WrenchLinePlacement {
    static final String STATE_KEY = "crystalnexusWrenchLine";
    public static final String PREVIEW_KEY = "crystalnexusWrenchLinePreview";
    private static final Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH,
        Direction.WEST, Direction.UP, Direction.DOWN
    };

    private WrenchLinePlacement() {
    }

    public static InteractionResult use(UseOnContext wrenchContext) {
        Player player = wrenchContext.getPlayer();
        if (player == null) return InteractionResult.PASS;

        CompoundTag root = player.getPersistentData();
        boolean active = root.contains(STATE_KEY);
        ItemStack offhand = player.getOffhandItem();
        BlockItem blockItem = eligibleBlockItem(offhand);

        if (!active && blockItem == null) return InteractionResult.PASS;
        if (wrenchContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        if (active && blockItem == null) {
            cancel(player, "Line placement cancelled: incompatible offhand item");
            return InteractionResult.FAIL;
        }

        if (active && player.isShiftKeyDown()) {
            CompoundTag state = root.getCompound(STATE_KEY);
            Direction next = nextDirection(Direction.from3DDataValue(state.getInt("direction")));
            state.putInt("direction", next.get3DDataValue());
            root.put(STATE_KEY, state);
            writePreview(wrenchContext.getItemInHand(), state);
            message(player, "Line direction: " + next.getName().toUpperCase());
            return InteractionResult.SUCCESS;
        }

        BlockPlaceContext endpointContext = new BlockPlaceContext(
            wrenchContext.getLevel(), player, InteractionHand.OFF_HAND, offhand,
            new BlockHitResult(wrenchContext.getClickLocation(), wrenchContext.getClickedFace(),
                wrenchContext.getClickedPos(), wrenchContext.isInside())
        );
        BlockPos endpoint = endpointContext.getClickedPos();

        if (!active) {
            if (!isCandidate(endpointContext, blockItem)) {
                message(player, "Cannot start a line here");
                return InteractionResult.FAIL;
            }

            CompoundTag state = new CompoundTag();
            state.putLong("start", endpoint.asLong());
            state.putInt("face", wrenchContext.getClickedFace().get3DDataValue());
            state.putInt("direction", wrenchContext.getClickedFace().get3DDataValue());
            state.putString("dimension", wrenchContext.getLevel().dimension().location().toString());
            state.putString("block", BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
            root.put(STATE_KEY, state);
            writePreview(wrenchContext.getItemInHand(), state);
            message(player, "Line start set. Sneak-use to change direction; use start again to cancel");
            return InteractionResult.SUCCESS;
        }

        CompoundTag state = root.getCompound(STATE_KEY);
        BlockPos start = BlockPos.of(state.getLong("start"));
        if (endpoint.equals(start)) {
            cancel(player, "Line placement cancelled");
            return InteractionResult.SUCCESS;
        }

        String problem = validateActiveState(player, state, blockItem, offhand);
        if (problem != null) {
            cancel(player, problem);
            return InteractionResult.FAIL;
        }

        Direction initial = Direction.from3DDataValue(state.getInt("direction"));
        List<BlockPos> path = planPath(start, endpoint, initial);
        int available = player.isCreative() ? path.size() : countMatching(player, offhand);
        if (available < path.size()) {
            message(player, "Need " + path.size() + " blocks; have " + available);
            return InteractionResult.FAIL;
        }

        List<Placement> placements = validatePath((ServerPlayer) player, blockItem, offhand, path);
        if (placements == null) return InteractionResult.FAIL;

        if (!placeAtomically((ServerPlayer) player, blockItem, offhand, placements)) {
            message(player, "Line placement failed; no blocks were used");
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) consumeMatching(player, offhand, path.size());
        root.remove(STATE_KEY);
        clearPreview(player);
        message(player, "Placed " + path.size() + " blocks");
        return InteractionResult.SUCCESS;
    }

    public static List<BlockPos> planPath(BlockPos start, BlockPos end, Direction initialDirection) {
        List<BlockPos> path = new ArrayList<>();
        path.add(start.immutable());
        BlockPos.MutableBlockPos cursor = start.mutable();
        Direction.Axis first = initialDirection.getAxis();
        Direction.Axis[] axes = first == Direction.Axis.X
            ? new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y}
            : first == Direction.Axis.Z
                ? new Direction.Axis[] {Direction.Axis.Z, Direction.Axis.X, Direction.Axis.Y}
                : new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z};

        for (Direction.Axis axis : axes) {
            int target = coordinate(end, axis);
            while (coordinate(cursor, axis) != target) {
                int step = Integer.compare(target, coordinate(cursor, axis));
                cursor.move(axis == Direction.Axis.X ? step : 0,
                    axis == Direction.Axis.Y ? step : 0,
                    axis == Direction.Axis.Z ? step : 0);
                path.add(cursor.immutable());
            }
        }
        return path;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.getPersistentData().contains(STATE_KEY)) return;

        CompoundTag state = player.getPersistentData().getCompound(STATE_KEY);
        if (!(player.getMainHandItem().getItem() instanceof CrystalWrenchItem)) {
            cancel(player, "Line placement cancelled: wrench switched");
            return;
        }

        BlockItem blockItem = eligibleBlockItem(player.getOffhandItem());
        String problem = validateActiveState(player, state, blockItem, player.getOffhandItem());
        if (problem != null) cancel(player, problem);
    }

    private static List<Placement> validatePath(ServerPlayer player, BlockItem blockItem,
                                                  ItemStack template, List<BlockPos> path) {
        Level level = player.level();
        List<Placement> placements = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            BlockPos pos = path.get(i);
            Direction travel = travelDirection(path, i);
            ItemStack single = template.copyWithCount(1);
            DirectedPlaceContext context = placementContext(level, player, single, pos, travel);
            BlockState oldState = level.getBlockState(pos);
            BlockState newState = blockItem.getBlock().getStateForPlacement(context);

            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !player.mayUseItemAt(pos, travel.getOpposite(), single)
                || !oldState.canBeReplaced(context) || newState == null
                || !newState.canSurvive(level, pos)
                || !level.isUnobstructed(newState, pos, CollisionContext.of(player))) {
                message(player, "Line blocked at " + pos.toShortString());
                return null;
            }

            CompoundTag blockEntityTag = null;
            BlockEntity oldBlockEntity = level.getBlockEntity(pos);
            if (oldBlockEntity != null) {
                blockEntityTag = oldBlockEntity.saveWithFullMetadata(level.registryAccess());
            }
            placements.add(new Placement(pos.immutable(), oldState, blockEntityTag, travel));
        }
        return placements;
    }

    private static boolean placeAtomically(ServerPlayer player, BlockItem blockItem,
                                           ItemStack template, List<Placement> placements) {
        Level level = player.level();
        int placed = 0;
        for (Placement placement : placements) {
            ItemStack single = template.copyWithCount(1);
            InteractionResult result = CommonHooks.onPlaceItemIntoWorld(placementContext(
                level, player, single, placement.pos(), placement.travel()
            ));
            if (!result.consumesAction()) {
                rollback(level, placements, placed);
                return false;
            }
            placed++;
        }
        for (Placement placement : placements) {
            level.updateNeighborsAt(placement.pos(), level.getBlockState(placement.pos()).getBlock());
        }
        return true;
    }

    private static void rollback(Level level, List<Placement> placements, int placed) {
        for (int i = placed - 1; i >= 0; i--) {
            Placement placement = placements.get(i);
            level.setBlock(placement.pos(), placement.oldState(), Block.UPDATE_ALL);
            if (placement.blockEntityTag() != null) {
                BlockEntity restored = level.getBlockEntity(placement.pos());
                if (restored != null) {
                    restored.loadWithComponents(placement.blockEntityTag(), level.registryAccess());
                    restored.setChanged();
                }
            }
        }
    }

    private static DirectedPlaceContext placementContext(Level level, Player player, ItemStack stack,
                                                          BlockPos pos, Direction travel) {
        return new DirectedPlaceContext(level, player, stack,
            new BlockHitResult(Vec3.atCenterOf(pos), travel.getOpposite(), pos, false), travel);
    }

    private static boolean isCandidate(BlockPlaceContext context, BlockItem item) {
        BlockPos pos = context.getClickedPos();
        BlockState newState = item.getBlock().getStateForPlacement(context);
        Player player = context.getPlayer();
        return context.getLevel().isLoaded(pos)
            && !context.getLevel().isOutsideBuildHeight(pos)
            && context.getLevel().getWorldBorder().isWithinBounds(pos)
            && (player == null || player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            && context.getLevel().getBlockState(pos).canBeReplaced(context)
            && newState != null && newState.canSurvive(context.getLevel(), pos)
            && (player == null || context.getLevel().isUnobstructed(newState, pos, CollisionContext.of(player)));
    }

    private static String validateActiveState(Player player, CompoundTag state,
                                              BlockItem blockItem, ItemStack offhand) {
        if (blockItem == null) return "Line placement cancelled: incompatible offhand item";
        if (!state.getString("dimension").equals(player.level().dimension().location().toString()))
            return "Line placement cancelled: dimension changed";
        if (!state.getString("block").equals(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString()))
            return "Line placement cancelled: offhand block changed";

        BlockPos start = BlockPos.of(state.getLong("start"));
        Direction face = Direction.from3DDataValue(state.getInt("face"));
        DirectedPlaceContext context = placementContext(player.level(), player, offhand.copyWithCount(1), start, face);
        if (!isCandidate(context, blockItem)) return "Line placement cancelled: start is no longer valid";
        return null;
    }

    private static BlockItem eligibleBlockItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item : null;
    }

    private static int countMatching(Player player, ItemStack template) {
        int count = template.getCount();
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, template)) count += stack.getCount();
        }
        return count;
    }

    private static void consumeMatching(Player player, ItemStack template, int amount) {
        ItemStack match = template.copyWithCount(1);
        int fromOffhand = Math.min(amount, player.getOffhandItem().getCount());
        player.getOffhandItem().shrink(fromOffhand);
        amount -= fromOffhand;
        for (ItemStack stack : player.getInventory().items) {
            if (amount == 0) break;
            if (!ItemStack.isSameItemSameComponents(stack, match)) continue;
            int taken = Math.min(amount, stack.getCount());
            stack.shrink(taken);
            amount -= taken;
        }
    }

    private static Direction travelDirection(List<BlockPos> path, int index) {
        BlockPos from = index + 1 < path.size() ? path.get(index) : path.get(index - 1);
        BlockPos to = index + 1 < path.size() ? path.get(index + 1) : path.get(index);
        return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : axis == Direction.Axis.Y ? pos.getY() : pos.getZ();
    }

    private static Direction nextDirection(Direction current) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i] == current) return DIRECTIONS[(i + 1) % DIRECTIONS.length];
        }
        return Direction.NORTH;
    }

    private static void cancel(Player player, String text) {
        player.getPersistentData().remove(STATE_KEY);
        clearPreview(player);
        message(player, text);
    }

    private static void writePreview(ItemStack wrench, CompoundTag state) {
        CustomData.update(DataComponents.CUSTOM_DATA, wrench, tag -> tag.put(PREVIEW_KEY, state.copy()));
    }

    private static void clearPreview(Player player) {
        clearPreview(player.getMainHandItem());
        clearPreview(player.getOffhandItem());
        for (ItemStack stack : player.getInventory().items) clearPreview(stack);
    }

    private static void clearPreview(ItemStack stack) {
        if (stack.getItem() instanceof CrystalWrenchItem) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(PREVIEW_KEY));
        }
    }

    private static void message(Player player, String text) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) return;
        player.displayClientMessage(Component.literal(text), true);
    }

    private record Placement(BlockPos pos, BlockState oldState, CompoundTag blockEntityTag, Direction travel) {
    }

    private static final class DirectedPlaceContext extends BlockPlaceContext {
        private final Direction direction;

        private DirectedPlaceContext(Level level, Player player, ItemStack stack,
                                     BlockHitResult hit, Direction direction) {
            super(level, player, InteractionHand.OFF_HAND, stack, hit);
            this.direction = direction;
        }

        @Override
        public Direction getNearestLookingDirection() {
            return direction;
        }

        @Override
        public Direction getNearestLookingVerticalDirection() {
            return direction.getAxis() == Direction.Axis.Y ? direction : super.getNearestLookingVerticalDirection();
        }

        @Override
        public Direction[] getNearestLookingDirections() {
            Direction[] ordered = new Direction[6];
            ordered[0] = direction;
            int index = 1;
            for (Direction candidate : super.getNearestLookingDirections()) {
                if (candidate != direction) ordered[index++] = candidate;
            }
            return ordered;
        }
    }
}
