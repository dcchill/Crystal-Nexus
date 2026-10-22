package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.crystalnexus.block.ConveyerBeltInputBlock;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltMovement;
import net.crystalnexus.block.ConveyerBeltMode;
import net.crystalnexus.block.ConveyerBeltOutputBlock;
import net.crystalnexus.block.ConveyerBeltRenderClock;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public abstract class ConveyerBeltBaseBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String PREV_POS_TAG = "SplinePrevPos";
    private static final String NEXT_POS_TAG = "SplineNextPos";
    private static final String INCOMING_HEAD_TIME_TAG = "IncomingHeadTime";
    private static final String MANUAL_MODE_TAG = "ManualMode";

    public static final int FILTER_SLOTS = 9;
    private final net.minecraft.core.NonNullList<ItemStack> filters = net.minecraft.core.NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);

    private final net.minecraft.resources.ResourceLocation[] filterTags = new net.minecraft.resources.ResourceLocation[FILTER_SLOTS];

    public ItemStack getFilter(int slot) {
        return filters.get(slot).copy();
    }

    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        filterTags[slot] = null;
        filters.set(slot, stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem()));
        sync();
    }

    @Nullable
    public net.minecraft.resources.ResourceLocation getFilterTag(int slot) {
        return slot >= 0 && slot < FILTER_SLOTS ? filterTags[slot] : null;
    }

    public boolean setFilterTag(int slot, @Nullable net.minecraft.resources.ResourceLocation tag) {
        if (slot < 0 || slot >= FILTER_SLOTS || filters.get(slot).isEmpty()) return false;
        if (tag != null && !filters.get(slot).is(net.minecraft.tags.ItemTags.create(tag))) return false;
        filterTags[slot] = tag;
        sync();
        return true;
    }

    private boolean filterBlacklist;

    public boolean isFilterBlacklist() { return filterBlacklist; }

    public void setFilterBlacklist(boolean blacklist) {
        filterBlacklist = blacklist;
        sync();
    }

    public boolean hasFilters() {
        return filters.stream().anyMatch(filter -> !filter.isEmpty());
    }

    public boolean matchesFilter(ItemStack stack) {
        boolean configured = false;
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            ItemStack filter = filters.get(slot);
            if (filter.isEmpty()) continue;
            configured = true;
            boolean matches = filterTags[slot] == null ? stack.is(filter.getItem())
                : stack.is(net.minecraft.tags.ItemTags.create(filterTags[slot]));
            if (matches) return !filterBlacklist;
        }
        return !configured || filterBlacklist;
    }

    public static final int SEGMENTS = 4;
    protected final ItemStack[] belt = new ItemStack[SEGMENTS];
	private static final int GAP_SEGMENTS = 1; 
    private double lastMoveGameTime = 0L;
    private double incomingHeadGameTime = Long.MIN_VALUE;
    private final ConveyerBeltRenderClock clientRenderClock = new ConveyerBeltRenderClock();
    private boolean manualMode;
    @Nullable
    private BlockPos splinePrevPos;
    @Nullable
    private BlockPos splineNextPos;

public float getRenderProgress(int segment, float partialTick) {
    if (level == null) return 0f;
    double startTime = level.isClientSide
            ? clientRenderClock.startTime(segment, lastMoveGameTime, incomingHeadGameTime, level.getGameTime())
            : ConveyerBeltMovement.renderStartTime(segment, lastMoveGameTime, incomingHeadGameTime);
    float dt = (float)((level.getGameTime() - startTime) + partialTick);
    float p = dt / (float) Math.max(1.0, ticksPerMove());
    if (p < 0f) p = 0f;
    if (p > 1f) p = 1f;
    return p;
}

public float getRenderDistance() {
    return (float) Math.max(1.0, 1.0 / ticksPerMove());
}

public boolean canAdvanceForRender(int segment) {
    if (level == null || segment < 0 || segment >= SEGMENTS || belt[segment].isEmpty()) return false;

    int occupiedSegments = 0;
    for (int i = 0; i < SEGMENTS; i++) {
        if (!belt[i].isEmpty()) occupiedSegments |= 1 << i;
    }

    BlockState state = getBlockState();
    if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;

    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    BlockPos frontPos = ConveyorGeometry.neighbor(level, worldPosition, getBlockState(), true);
    if (!level.hasChunkAt(frontPos)) return false;
    BlockEntity front = level.getBlockEntity(frontPos);
    boolean tailCanAdvance;
    if (front instanceof ConveyerBeltBaseBlockEntity nextBelt) {
        tailCanAdvance = ConveyorGeometry.connects(worldPosition, state, nextBelt.getBlockPos(), nextBelt.getBlockState()) && nextBelt.belt[0].isEmpty();
    } else {
        IItemHandler frontInventory = isInputBelt() ? findFrontInventory(facing, facing.getOpposite()) : null;
        tailCanAdvance = frontInventory == null
                || matchesFilter(belt[SEGMENTS - 1]) && ItemHandlerHelper.insertItem(frontInventory, belt[SEGMENTS - 1], true).getCount() < belt[SEGMENTS - 1].getCount();
    }
    return ConveyerBeltMovement.canAdvance(occupiedSegments, segment, tailCanAdvance);
}

    protected ConveyerBeltBaseBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < SEGMENTS; i++) belt[i] = ItemStack.EMPTY;
    }

    public ItemStack getSegment(int idx) {
        if (idx < 0 || idx >= SEGMENTS) return ItemStack.EMPTY;
        return belt[idx];
    }

    @Nullable
    public BlockPos getSplinePrevPos() {
        return splinePrevPos != null && level != null && level.hasChunkAt(splinePrevPos)
            && ConveyorGeometry.connects(splinePrevPos, level.getBlockState(splinePrevPos), worldPosition, getBlockState()) ? splinePrevPos : null;
    }

    @Nullable
    public BlockPos getSplineNextPos() {
        return splineNextPos != null && level != null && level.hasChunkAt(splineNextPos)
            && ConveyorGeometry.connects(worldPosition, getBlockState(), splineNextPos, level.getBlockState(splineNextPos)) ? splineNextPos : null;
    }

    public void setSplineConnections(@Nullable BlockPos prevPos, @Nullable BlockPos nextPos) {
        splinePrevPos = prevPos;
        splineNextPos = nextPos;
        sync();
    }

    public boolean isManualMode() {
        return manualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
        setChanged();
    }

public void serverTick() {
    if (level == null) return;
    if (level.isClientSide) return;

    moveEntities();
    double gameTime = level.getGameTime();
    // Process sub-tick moves, but never replay time spent unloaded.
    lastMoveGameTime = Math.max(lastMoveGameTime, gameTime - Math.max(1.0, ticksPerMove()));
    boolean changed = false;
    while (gameTime - lastMoveGameTime >= ticksPerMove()) {
        lastMoveGameTime += ticksPerMove();
        changed |= moveItems(lastMoveGameTime);
    }
    if (changed) sync();
}

private boolean moveItems(double gameTime) {

    BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction back = ConveyorGeometry.input(state);

        boolean changed = false;

        
        
        boolean movedToNextBelt = tryMoveToNextBelt(facing);
        changed |= movedToNextBelt;

        for (int i = SEGMENTS - 2; i >= 0; i--) {
            ItemStack cur = belt[i];
            if (cur.isEmpty()) continue;
            if (i == 0 && incomingHeadGameTime != Long.MIN_VALUE
                    && gameTime - incomingHeadGameTime < ticksPerMove()) continue;

            ItemStack next = belt[i + 1];
            if (next.isEmpty()) {
                belt[i + 1] = cur;
                belt[i] = ItemStack.EMPTY;
                if (i == 0) incomingHeadGameTime = Long.MIN_VALUE;
                changed = true;
            }
        }

        changed |= tryPullFromAbove();

        if (isOutputBelt()) changed |= tryPullFromBehind(back, facing);

        IItemHandler frontInventory = null;
        if (isInputBelt()) {
            BlockPos frontPos = ConveyorGeometry.neighbor(level, worldPosition, getBlockState(), true);
            if (level.hasChunkAt(frontPos) && !(level.getBlockEntity(frontPos) instanceof ConveyerBeltBaseBlockEntity)) {
                frontInventory = findFrontInventory(facing, back);
                if (frontInventory != null) changed |= tryPushToFront(frontInventory);
            }
        }

        if (ConveyerBeltMovement.shouldDropOffFront(movedToNextBelt, frontInventory != null)) {
            changed |= tryDropOffFront(facing);
        }

        return changed;
    }

    private void moveEntities() {
        BlockState state = getBlockState();
        double speed = .25 / ticksPerMove();
        double length = state.getValue(net.crystalnexus.block.ConveyerBeltBlock.CURVE)
            == net.crystalnexus.block.ConveyerBeltBlock.CurveShape.STRAIGHT ? 1 : Math.PI / 4;
        var area = new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0, 1.5, 0);
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null, area,
                e -> !(e instanceof ItemEntity) && e.isAlive() && !e.isSpectator() && !e.isPassenger())) {
            var data = entity.getPersistentData();
            if (data.contains("crystalnexusConveyorMoveTick") && data.getLong("crystalnexusConveyorMoveTick") == level.getGameTime()) continue;
            double x = entity.getX() - worldPosition.getX(), z = entity.getZ() - worldPosition.getZ();
            if (x < 0 || x >= 1 || z < 0 || z >= 1 || entity.getDeltaMovement().y > .3
                    || entity instanceof Player player && player.getAbilities().flying) continue;
            double t = ConveyorGeometry.progress(state, x, z);
            double feet = entity.getY() - worldPosition.getY();
            var bounds = entity.getBoundingBox().move(-worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ());
            double support = entitySupportHeight(state, bounds);
            if (Double.isNaN(support) || feet > support + .13
                    || feet < ConveyorGeometry.point(state, t).y - speed - .13) continue;
            double next = t + speed / length;
            var target = ConveyorGeometry.point(state, Math.min(1, next));
            if (next > 1) target = target.add(net.minecraft.world.phys.Vec3.atLowerCornerOf(ConveyorGeometry.output(state).getNormal())
                .scale((next - 1) * length));
            var horizontal = new net.minecraft.world.phys.Vec3(target.x - x, 0, target.z - z);
            if (horizontal.length() > speed) horizontal = horizontal.normalize().scale(speed);
            double nextSupport = entitySupportHeight(state, bounds.move(horizontal));
            double lift = (Double.isNaN(nextSupport) ? target.y : nextSupport) - feet + .035;
            if (entity instanceof Player) {
                double y = entity.getDeltaMovement().y;
                // Counter gravity while entering or climbing a ramp; stepping alone can stall at its foot.
                if (nextSupport > support || state.getValue(ConveyorGeometry.SLOPE) == ConveyorGeometry.Slope.UP)
                    y = Math.max(y, lift + .08);
                entity.setDeltaMovement(horizontal.x, y, horizontal.z);
                // Players simulate movement on their client, so send the belt velocity to them.
                entity.hurtMarked = true;
            } else {
                // Descend after moving off the higher step; collision movement resolves Y first.
                if (lift < 0) {
                    entity.move(net.minecraft.world.entity.MoverType.SELF, horizontal);
                    entity.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, lift, 0));
                } else {
                    entity.move(net.minecraft.world.entity.MoverType.SELF,
                        new net.minecraft.world.phys.Vec3(horizontal.x, lift, horizontal.z));
                }
            }
            data.putLong("crystalnexusConveyorMoveTick", level.getGameTime());
            entity.hasImpulse = true;
        }
    }

    private double entitySupportHeight(BlockState state, net.minecraft.world.phys.AABB bounds) {
        double height = ConveyorGeometry.supportHeight(state, bounds);
        for (boolean out : new boolean[]{false, true}) {
            BlockPos pos = ConveyorGeometry.neighbor(level, worldPosition, state, out);
            if (!level.hasChunkAt(pos)) continue;
            BlockState other = level.getBlockState(pos);
            if (!(out ? ConveyorGeometry.connects(worldPosition, state, pos, other)
                    : ConveyorGeometry.connects(pos, other, worldPosition, state))) continue;
            BlockPos offset = pos.subtract(worldPosition);
            double adjacent = ConveyorGeometry.supportHeight(other, bounds.move(-offset.getX(), -offset.getY(), -offset.getZ())) + offset.getY();
            if (!Double.isNaN(adjacent)) height = Double.isNaN(height) ? adjacent : Math.max(height, adjacent);
        }
        return height;
    }

    protected boolean isOutputBelt() {
        return getBlockState().getBlock() instanceof ConveyerBeltOutputBlock;
    }

    protected boolean isInputBelt() {
        return getBlockState().getBlock() instanceof ConveyerBeltInputBlock;
    }

    private double ticksPerMove() {
        return ConveyerBeltMode.tierAt(getBlockState()).ticksPerMove();
    }
private boolean hasGapAtHead() {
    for (int i = 0; i <= GAP_SEGMENTS; i++) {
        if (i >= SEGMENTS) break;
        if (!belt[i].isEmpty()) return false;
    }
    return true;
}



    protected boolean tryInsertToHead(ItemStack moving) {
        if (moving.isEmpty()) return false;

        if (!belt[0].isEmpty()) return false;

        belt[0] = moving.copy();
        moving.setCount(0);
        markIncomingHead();
        return true;
    }

    private void markIncomingHead() {
        if (level == null) return;
        incomingHeadGameTime = level.getGameTime();
    }

    private boolean tryMoveToNextBelt(Direction facing) {
        if (level == null) return false;

        BlockPos frontPos = ConveyorGeometry.neighbor(level, worldPosition, getBlockState(), true);
        if (!level.hasChunkAt(frontPos)) return false;
        BlockEntity front = level.getBlockEntity(frontPos);
        if (!(front instanceof ConveyerBeltBaseBlockEntity nextBelt)
            || !ConveyorGeometry.connects(worldPosition, getBlockState(), frontPos, nextBelt.getBlockState())) return false;

        ItemStack tail = belt[SEGMENTS - 1];
        if (tail.isEmpty()) return false;

        ItemStack moving = tail.copy();
        boolean ok = nextBelt.tryInsertToHead(moving);
        if (!ok) return false;

        int moved = tail.getCount() - moving.getCount();
        if (moved <= 0) return false;

        tail.shrink(moved);
        if (tail.isEmpty()) belt[SEGMENTS - 1] = ItemStack.EMPTY;

        nextBelt.incomingHeadGameTime = lastMoveGameTime;
        nextBelt.sync();
        return true;
    }




private boolean tryPullFromAbove() {
    if (level == null) return false;

    BlockPos inputPos = worldPosition.above();
    if (level.getBlockEntity(inputPos) instanceof ConveyerBeltBaseBlockEntity) return false;
    return tryPullFrom(inputPos, Direction.DOWN, Direction.UP);
}

private boolean tryPullFromBehind(Direction back, Direction facing) {
    return tryPullFrom(ConveyorGeometry.port(worldPosition, getBlockState(), false), back.getOpposite(), back);
}

private boolean tryPullFrom(BlockPos inputPos, Direction primarySide, Direction fallbackSide) {
    if (level == null) return false;

    if (!level.hasChunkAt(inputPos) || level.getBlockEntity(inputPos) instanceof ConveyerBeltBaseBlockEntity) return false;
    if (!hasGapAtHead()) return false;

    ItemStack head = belt[0];
    int headSpace = head.isEmpty() ? 64 : (head.getMaxStackSize() - head.getCount());
    if (headSpace <= 0) return false;

    int toPullMax = Math.min(ConveyerBeltMode.tierAt(getBlockState()).batchSize(), headSpace);
    if (toPullMax <= 0) return false;

    
    IItemHandler input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, primarySide);
    if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
    if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, fallbackSide);
    if (input == null) return false;

    for (int slot = 0; slot < input.getSlots(); slot++) {

        
        ItemStack peek = input.extractItem(slot, 1, true);
        if (peek.isEmpty() || !matchesFilter(peek)) continue;

        if (!head.isEmpty() && !ItemStack.isSameItemSameComponents(head, peek)) continue;

        int remaining = Math.min(toPullMax, peek.getMaxStackSize());
        ItemStack pulledTotal = ItemStack.EMPTY;

        while (remaining > 0) {
            
            ItemStack sim = input.extractItem(slot, remaining, true);
            if (sim.isEmpty() || !matchesFilter(sim)) break;

            if (!head.isEmpty() && !ItemStack.isSameItemSameComponents(head, sim)) break;
            if (!pulledTotal.isEmpty() && !ItemStack.isSameItemSameComponents(pulledTotal, sim)) break;

            ItemStack got = input.extractItem(slot, sim.getCount(), false);
            if (got.isEmpty()) break;

            if (pulledTotal.isEmpty()) {
                pulledTotal = got.copy();
            } else {
                pulledTotal.grow(got.getCount());
            }

            remaining -= got.getCount();

            
            if (got.getCount() <= 0) break;
        }

        if (pulledTotal.isEmpty()) continue;

        belt[0] = insertIntoSegment(belt[0], pulledTotal);
        incomingHeadGameTime = lastMoveGameTime;
        return true; 
    }

    return false;
}


    @Nullable
    private IItemHandler findFrontInventory(Direction facing, Direction back) {
        ItemStack tail = belt[SEGMENTS - 1];
        if (level == null || tail.isEmpty()) return null;

        BlockPos outPos = ConveyorGeometry.neighbor(level, worldPosition, getBlockState(), true);
        if (!level.hasChunkAt(outPos) || level.getBlockEntity(outPos) instanceof ConveyerBeltBaseBlockEntity) return null;

        
        IItemHandler out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, facing.getOpposite());
        if (out == null) out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, null);
        if (out == null) out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, facing);
        return out;
    }

    private boolean tryPushToFront(IItemHandler out) {
        ItemStack tail = belt[SEGMENTS - 1];
        if (!matchesFilter(tail)) return false;
        ItemStack remainder = ItemHandlerHelper.insertItem(out, tail, false);
        if (remainder.isEmpty()) {
            belt[SEGMENTS - 1] = ItemStack.EMPTY;
            return true;
        }
        if (remainder.getCount() != tail.getCount()) {
            belt[SEGMENTS - 1] = remainder;
            return true;
        }
        return false; 
    }

    private boolean tryDropOffFront(Direction facing) {
        if (level == null) return false;

        BlockPos frontPos = ConveyorGeometry.neighbor(level, worldPosition, getBlockState(), true);
        if (!level.hasChunkAt(frontPos) || level.getBlockEntity(frontPos) instanceof ConveyerBeltBaseBlockEntity) return false;

        ItemStack tail = belt[SEGMENTS - 1];
        if (tail.isEmpty()) return false;

        ItemStack dropped = tail.copy();
        belt[SEGMENTS - 1] = ItemStack.EMPTY;

        double spawnX = worldPosition.getX() + 0.5D + facing.getStepX() * 0.55D;
        double spawnY = worldPosition.getY() + 0.65D + ConveyorGeometry.outputHeight(getBlockState());
        double spawnZ = worldPosition.getZ() + 0.5D + facing.getStepZ() * 0.55D;
        ItemEntity entity = new ItemEntity(level, spawnX, spawnY, spawnZ, dropped);
        entity.setDeltaMovement(facing.getStepX() * 0.08D, 0.02D, facing.getStepZ() * 0.08D);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
        return true;
    }

    private static boolean canAccept(ItemStack seg, int amount) {
        if (seg.isEmpty()) return true;
        return seg.getCount() + amount <= seg.getMaxStackSize();
    }

    private static ItemStack insertIntoSegment(ItemStack seg, ItemStack incoming) {
        if (incoming.isEmpty()) return seg;
        if (seg.isEmpty()) return incoming.copy();
        if (!ItemStack.isSameItemSameComponents(seg, incoming)) return seg;

        int space = seg.getMaxStackSize() - seg.getCount();
        int move = Math.min(space, incoming.getCount());
        seg.grow(move);
        incoming.shrink(move);
        return seg;
    }

    protected void sync() {
        setChanged();
        if (level != null) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, 3);
        }
    }


    @Override public int getContainerSize() { return SEGMENTS; }

    @Override public boolean isEmpty() {
        for (ItemStack s : belt) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int index) {
        if (index < 0 || index >= SEGMENTS) return ItemStack.EMPTY;
        return belt[index];
    }

    @Override public ItemStack removeItem(int index, int count) {
        if (index < 0 || index >= SEGMENTS || count <= 0) return ItemStack.EMPTY;
        ItemStack cur = belt[index];
        if (cur.isEmpty()) return ItemStack.EMPTY;

        ItemStack split = cur.split(count);
        if (cur.isEmpty()) {
            belt[index] = ItemStack.EMPTY;
            if (index == 0) incomingHeadGameTime = Long.MIN_VALUE;
        }
        if (!split.isEmpty()) sync();
        return split;
    }

    @Override public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= SEGMENTS) return ItemStack.EMPTY;
        ItemStack cur = belt[index];
        belt[index] = ItemStack.EMPTY;
        if (index == 0) incomingHeadGameTime = Long.MIN_VALUE;
        return cur;
    }

    @Override public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= SEGMENTS) return;
        belt[index] = stack;
        if (index == 0) {
            incomingHeadGameTime = stack.isEmpty() || level == null ? Long.MIN_VALUE : level.getGameTime();
        }
        sync();
    }

    @Override public void clearContent() {
        for (int i = 0; i < SEGMENTS; i++) belt[i] = ItemStack.EMPTY;
        incomingHeadGameTime = Long.MIN_VALUE;
        sync();
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        return index == 0;
    }

    @Override public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, SEGMENTS).toArray();
    }

    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) {
        return canPlaceItem(index, stack);
    }

    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) {
        return index == SEGMENTS - 1;
    }

    public Component getDisplayName() {
        return Component.literal("Conveyor Belt");
    }

@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);

    CompoundTag beltTag = new CompoundTag();
    for (int i = 0; i < SEGMENTS; i++) {
        if (!belt[i].isEmpty()) beltTag.put("s" + i, belt[i].save(lookup));
    }
    tag.put("Belt", beltTag);
    CompoundTag filterTag = new CompoundTag();
    for (int i = 0; i < FILTER_SLOTS; i++) {
        if (!filters.get(i).isEmpty()) filterTag.put("s" + i, filters.get(i).save(lookup));
    }
    tag.put("InventoryFilters", filterTag);
    CompoundTag tagFilters = new CompoundTag();
    for (int i = 0; i < FILTER_SLOTS; i++) {
        if (filterTags[i] != null) tagFilters.putString("s" + i, filterTags[i].toString());
    }
    tag.put("InventoryFilterTags", tagFilters);
    tag.putBoolean("FilterBlacklist", filterBlacklist);

    tag.putDouble("LastMove", lastMoveGameTime);
    if (incomingHeadGameTime != Long.MIN_VALUE) {
        tag.putDouble(INCOMING_HEAD_TIME_TAG, incomingHeadGameTime);
    }
    if (splinePrevPos != null) {
        tag.putLong(PREV_POS_TAG, splinePrevPos.asLong());
    }
    if (splineNextPos != null) {
        tag.putLong(NEXT_POS_TAG, splineNextPos.asLong());
    }
    if (manualMode) {
        tag.putBoolean(MANUAL_MODE_TAG, true);
    }
}


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        for (int i = 0; i < SEGMENTS; i++) belt[i] = ItemStack.EMPTY;
        filterBlacklist = tag.getBoolean("FilterBlacklist");
        CompoundTag filterTag = tag.getCompound("InventoryFilters");
        CompoundTag tagFilters = tag.getCompound("InventoryFilterTags");
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = ItemStack.parseOptional(lookup, filterTag.getCompound("s" + i));
            filters.set(i, filter.isEmpty() ? ItemStack.EMPTY : new ItemStack(filter.getItem()));
            String tagId = tagFilters.getString("s" + i);
            filterTags[i] = filter.isEmpty() || tagId.isEmpty() ? null : net.minecraft.resources.ResourceLocation.tryParse(tagId);
        }

        if (tag.contains("Belt")) {
            CompoundTag beltTag = tag.getCompound("Belt");
            for (int i = 0; i < SEGMENTS; i++) {
                String key = "s" + i;
                if (beltTag.contains(key)) {
                    belt[i] = ItemStack.parseOptional(lookup, beltTag.getCompound(key));
                }
            }
        }
    if (tag.contains("LastMove"))
        lastMoveGameTime = tag.getDouble("LastMove");
    incomingHeadGameTime = tag.contains(INCOMING_HEAD_TIME_TAG) ? tag.getDouble(INCOMING_HEAD_TIME_TAG) : Long.MIN_VALUE;
    splinePrevPos = tag.contains(PREV_POS_TAG) ? BlockPos.of(tag.getLong(PREV_POS_TAG)) : null;
    splineNextPos = tag.contains(NEXT_POS_TAG) ? BlockPos.of(tag.getLong(NEXT_POS_TAG)) : null;
    manualMode = tag.getBoolean(MANUAL_MODE_TAG);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        return saveWithFullMetadata(lookup);
    }
}
