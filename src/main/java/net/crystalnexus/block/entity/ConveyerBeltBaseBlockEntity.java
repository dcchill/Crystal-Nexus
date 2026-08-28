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

    public static final int SEGMENTS = 4;
    protected final ItemStack[] belt = new ItemStack[SEGMENTS];
	private static final int GAP_SEGMENTS = 1; // 1 = one empty slot between items (recommended)
    private long lastMoveGameTime = 0L;
    private long incomingHeadGameTime = Long.MIN_VALUE;
    private final ConveyerBeltRenderClock clientRenderClock = new ConveyerBeltRenderClock();
    private boolean manualMode;
    @Nullable
    private BlockPos splinePrevPos;
    @Nullable
    private BlockPos splineNextPos;

public float getRenderProgress(int segment, float partialTick) {
    if (level == null) return 0f;
    long startTime = level.isClientSide
            ? clientRenderClock.startTime(segment, lastMoveGameTime, incomingHeadGameTime, level.getGameTime())
            : ConveyerBeltMovement.renderStartTime(segment, lastMoveGameTime, incomingHeadGameTime);
    float dt = (float)((level.getGameTime() - startTime) + partialTick);
    float p = dt / (float) ticksPerMove();
    if (p < 0f) p = 0f;
    if (p > 1f) p = 1f;
    return p;
}

public boolean canAdvanceForRender(int segment) {
    if (level == null || segment < 0 || segment >= SEGMENTS || belt[segment].isEmpty()) return false;

    int occupiedSegments = 0;
    for (int i = 0; i < SEGMENTS; i++) {
        if (!belt[i].isEmpty()) occupiedSegments |= 1 << i;
    }

    BlockState state = getBlockState();
    if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;

    BlockEntity front = level.getBlockEntity(worldPosition.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    boolean tailCanAdvance = !(front instanceof ConveyerBeltBaseBlockEntity nextBelt) || nextBelt.belt[0].isEmpty();
    return ConveyerBeltMovement.canAdvance(occupiedSegments, segment, tailCanAdvance);
}

    protected ConveyerBeltBaseBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < SEGMENTS; i++) belt[i] = ItemStack.EMPTY;
    }

    // For renderer access
    public ItemStack getSegment(int idx) {
        if (idx < 0 || idx >= SEGMENTS) return ItemStack.EMPTY;
        return belt[idx];
    }

    @Nullable
    public BlockPos getSplinePrevPos() {
        return splinePrevPos;
    }

    @Nullable
    public BlockPos getSplineNextPos() {
        return splineNextPos;
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

    // ======== Belt Simulation (server) ========
public void serverTick() {
    if (level == null) return;
    if (level.isClientSide) return;

    long gameTime = level.getGameTime();
    if (gameTime - lastMoveGameTime < ticksPerMove()) return;

    // This timestamp drives both movement cadence and smooth client interpolation.
    lastMoveGameTime = gameTime;

    BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction back = facing.getOpposite();

        boolean changed = false;

        // 1) hand off an item that was already at the tail before this step.
        // Doing this after the loop let segment 2 move to 3 and immediately cross
        // into the next block, skipping the final quarter of the rendered path.
        boolean movedToNextBelt = tryMoveToNextBelt(facing);
        changed |= movedToNextBelt;

        // 2) move segment contents forward within THIS belt block
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

        // 3) every belt pulls from an inventory directly above into segment 0
        changed |= tryPullFromAbove();

        // 4) output belt pulls from inventory behind into segment 0
        if (isOutputBelt()) changed |= tryPullFromBehind(back, facing);

        // 5) input belt pushes into inventory in front ONLY if there isn't a belt in front
        boolean pushedToFront = false;
        if (isInputBelt()) {
            BlockPos frontPos = worldPosition.relative(facing);
            if (!(level.getBlockEntity(frontPos) instanceof ConveyerBeltBaseBlockEntity)) {
                pushedToFront = tryPushToFront(facing, back);
                changed |= pushedToFront;
            }
        }

        if (!movedToNextBelt) {
            changed |= tryDropOffFront(facing, pushedToFront);
        }

        if (changed) sync();
    }

    protected boolean isOutputBelt() {
        return getBlockState().getBlock() instanceof ConveyerBeltOutputBlock;
    }

    protected boolean isInputBelt() {
        return getBlockState().getBlock() instanceof ConveyerBeltInputBlock;
    }

    private int ticksPerMove() {
        return ConveyerBeltMode.tierAt(getBlockState()).ticksPerMove();
    }
private boolean hasGapAtHead() {
    // Require belt[0] empty and next few segments empty too
    for (int i = 0; i <= GAP_SEGMENTS; i++) {
        if (i >= SEGMENTS) break;
        if (!belt[i].isEmpty()) return false;
    }
    return true;
}


    // ======== Belt -> Belt handoff ========

    // Insert stack into THIS belt's segment 0 (used by belt->belt handoff)
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

    // Try to move items from our tail into the next belt's head
    private boolean tryMoveToNextBelt(Direction facing) {
        if (level == null) return false;

        BlockPos frontPos = worldPosition.relative(facing);
        BlockEntity front = level.getBlockEntity(frontPos);
        if (!(front instanceof ConveyerBeltBaseBlockEntity nextBelt)) return false;

        ItemStack tail = belt[SEGMENTS - 1];
        if (tail.isEmpty()) return false;

        ItemStack moving = tail.copy();
        boolean ok = nextBelt.tryInsertToHead(moving);
        if (!ok) return false;

        int moved = tail.getCount() - moving.getCount();
        if (moved <= 0) return false;

        tail.shrink(moved);
        if (tail.isEmpty()) belt[SEGMENTS - 1] = ItemStack.EMPTY;

        nextBelt.sync();
        return true;
    }

    // ======== Inventory interactions ========

private static final int PULL_MAX = 32;

private boolean tryPullFromAbove() {
    if (level == null) return false;

    BlockPos inputPos = worldPosition.above();
    if (level.getBlockEntity(inputPos) instanceof ConveyerBeltBaseBlockEntity) return false;
    return tryPullFrom(inputPos, Direction.DOWN, Direction.UP);
}

private boolean tryPullFromBehind(Direction back, Direction facing) {
    return tryPullFrom(worldPosition.relative(back), facing, back);
}

private boolean tryPullFrom(BlockPos inputPos, Direction primarySide, Direction fallbackSide) {
    if (level == null) return false;

    // Enforce spacing: only pull if head has a gap
    if (!hasGapAtHead()) return false;

    // If head has something, we can only pull more of the same thing
    ItemStack head = belt[0];
    int headSpace = head.isEmpty() ? 64 : (head.getMaxStackSize() - head.getCount());
    if (headSpace <= 0) return false;

    // Pull no more than: 32, or remaining space in the head stack
    int toPullMax = Math.min(PULL_MAX, headSpace);
    if (toPullMax <= 0) return false;

    // Try common sides + null (more compatible with different inventories)
    IItemHandler input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, primarySide);
    if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
    if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, fallbackSide);
    if (input == null) return false;

    for (int slot = 0; slot < input.getSlots(); slot++) {

        // Peek what item is available (some handlers only reveal 1 even if asked for more)
        ItemStack peek = input.extractItem(slot, 1, true);
        if (peek.isEmpty()) continue;

        // If belt head isn't empty, only accept exact same item+components
        if (!head.isEmpty() && !ItemStack.isSameItemSameComponents(head, peek)) continue;

        int remaining = toPullMax;
        ItemStack pulledTotal = ItemStack.EMPTY;

        while (remaining > 0) {
            // Try to extract remaining (handler might still only give 1)
            ItemStack sim = input.extractItem(slot, remaining, true);
            if (sim.isEmpty()) break;

            // Make sure it still matches (safety)
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

            // safety against weird 0-count behavior
            if (got.getCount() <= 0) break;
        }

        if (pulledTotal.isEmpty()) continue;

        // Insert into belt[0] (won’t overflow because we bounded by headSpace)
        belt[0] = insertIntoSegment(belt[0], pulledTotal);
        return true; // pulled up to 32 (or up to space), from one slot per step
    }

    return false;
}


    private boolean tryPushToFront(Direction facing, Direction back) {
        if (level == null) return false;

        ItemStack tail = belt[SEGMENTS - 1];
        if (tail.isEmpty()) return false;

        BlockPos outPos = worldPosition.relative(facing);

        // Try common sides + null (more compatible with different inventories)
        IItemHandler out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, back);
        if (out == null) out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, null);
        if (out == null) out = level.getCapability(Capabilities.ItemHandler.BLOCK, outPos, facing);
        if (out == null) return false;

        ItemStack remainder = ItemHandlerHelper.insertItem(out, tail, false);
        if (remainder.isEmpty()) {
            belt[SEGMENTS - 1] = ItemStack.EMPTY;
            return true;
        }
        if (remainder.getCount() != tail.getCount()) {
            belt[SEGMENTS - 1] = remainder;
            return true;
        }
        return false; // no space -> backs up
    }

    private boolean tryDropOffFront(Direction facing, boolean alreadyOutput) {
        if (level == null) return false;

        BlockPos frontPos = worldPosition.relative(facing);
        if (level.getBlockEntity(frontPos) instanceof ConveyerBeltBaseBlockEntity) return false;
        if (alreadyOutput) return false;

        ItemStack tail = belt[SEGMENTS - 1];
        if (tail.isEmpty()) return false;

        ItemStack dropped = tail.copy();
        belt[SEGMENTS - 1] = ItemStack.EMPTY;

        double spawnX = worldPosition.getX() + 0.5D + facing.getStepX() * 0.55D;
        double spawnY = worldPosition.getY() + 0.65D;
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
        // Strict behavior: only allow automation to insert at segment 0
        return index == 0;
    }

    @Override public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, SEGMENTS).toArray();
    }

    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction dir) {
        return canPlaceItem(index, stack);
    }

    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction dir) {
        // Strict behavior: only allow extraction from last segment
        return index == SEGMENTS - 1;
    }

    public Component getDisplayName() {
        return Component.literal("Conveyor Belt");
    }

    // ======== NBT + Client Sync ========
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);

    CompoundTag beltTag = new CompoundTag();
    for (int i = 0; i < SEGMENTS; i++) {
        if (!belt[i].isEmpty()) beltTag.put("s" + i, belt[i].save(lookup));
    }
    tag.put("Belt", beltTag);

    // ✅ ADD THIS LINE
    tag.putLong("LastMove", lastMoveGameTime);
    if (incomingHeadGameTime != Long.MIN_VALUE) {
        tag.putLong(INCOMING_HEAD_TIME_TAG, incomingHeadGameTime);
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

        if (tag.contains("Belt")) {
            CompoundTag beltTag = tag.getCompound("Belt");
            for (int i = 0; i < SEGMENTS; i++) {
                String key = "s" + i;
                if (beltTag.contains(key)) {
                    belt[i] = ItemStack.parseOptional(lookup, beltTag.getCompound(key));
                }
            }
        }
           // ✅ ADD THIS LINE
    if (tag.contains("LastMove"))
        lastMoveGameTime = tag.getLong("LastMove");
    incomingHeadGameTime = tag.contains(INCOMING_HEAD_TIME_TAG) ? tag.getLong(INCOMING_HEAD_TIME_TAG) : Long.MIN_VALUE;
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
