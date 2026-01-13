package net.crystalnexus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.crystalnexus.init.CrystalnexusModBlocks;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public abstract class ConveyerBeltBaseBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int SEGMENTS = 4;
    protected final ItemStack[] belt = new ItemStack[SEGMENTS];
	private static final int GAP_SEGMENTS = 1; // 1 = one empty slot between items (recommended)
    // movement speed control
    private int moveCooldown = 1;

    // Lower = faster, Higher = slower (runs once every N ticks)
    private static final int TICKS_PER_MOVE = 4;
    private long lastMoveGameTime = 0L;
// 0..1 progress between discrete moves (client uses this for smooth rendering)
private float renderProgress = 0f;

public float getRenderProgress(float partialTick) {
    if (level == null) return 0f;
    float dt = (float)((level.getGameTime() - lastMoveGameTime) + partialTick);
    float p = dt / (float) TICKS_PER_MOVE;
    if (p < 0f) p = 0f;
    if (p > 1f) p = 1f;
    return p;
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

    // ======== Belt Simulation (server) ========
public void serverTick() {
    if (level == null) return;
    if (level.isClientSide) return;

    if (++moveCooldown < TICKS_PER_MOVE) return;
    moveCooldown = 0;

    // mark the moment this belt step starts (used for smooth rendering)
    lastMoveGameTime = level.getGameTime();

    BlockState state = getBlockState();
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction back = facing.getOpposite();

        boolean changed = false;

        // 1) move segment contents forward within THIS belt block
        for (int i = SEGMENTS - 2; i >= 0; i--) {
            ItemStack cur = belt[i];
            if (cur.isEmpty()) continue;

            ItemStack next = belt[i + 1];
            if (next.isEmpty()) {
                belt[i + 1] = cur;
                belt[i] = ItemStack.EMPTY;
                changed = true;
            } else if (ItemStack.isSameItemSameComponents(next, cur) && next.getCount() < next.getMaxStackSize()) {
                int space = next.getMaxStackSize() - next.getCount();
                int move = Math.min(space, cur.getCount());
                next.grow(move);
                cur.shrink(move);
                if (cur.isEmpty()) belt[i] = ItemStack.EMPTY;
                changed = true;
            }
        }

        // 2) try to hand off tail into the next belt (belt chains)
        changed |= tryMoveToNextBelt(facing);

        // 3) output belt pulls from inventory behind into segment 0
        if (isOutputBelt()) changed |= tryPullFromBehind(back, facing);

        // 4) input belt pushes into inventory in front ONLY if there isn't a belt in front
        if (isInputBelt()) {
            BlockPos frontPos = worldPosition.relative(facing);
            if (!(level.getBlockEntity(frontPos) instanceof ConveyerBeltBaseBlockEntity)) {
                changed |= tryPushToFront(facing, back);
            }
        }

        if (changed) sync();
    }

    protected boolean isOutputBelt() {
        return getBlockState().getBlock() == CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get();
    }

    protected boolean isInputBelt() {
        return getBlockState().getBlock() == CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get();
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

        ItemStack head = belt[0];

        if (head.isEmpty()) {
            belt[0] = moving.copy();
            moving.setCount(0);
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(head, moving)) return false;

        int space = head.getMaxStackSize() - head.getCount();
        if (space <= 0) return false;

        int move = Math.min(space, moving.getCount());
        head.grow(move);
        moving.shrink(move);
        return move > 0;
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

    private boolean tryPullFromBehind(Direction back, Direction facing) {
        if (level == null) return false;

        if (!canAccept(belt[0], 1)) return false;
		// Enforce spacing: only pull if head has a gap
		if (!hasGapAtHead()) return false;

        BlockPos inputPos = worldPosition.relative(back);

        // Try common sides + null (more compatible with different inventories)
        IItemHandler input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, facing);
        if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        if (input == null) input = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, back);
        if (input == null) return false;

        for (int slot = 0; slot < input.getSlots(); slot++) {
            ItemStack sim = input.extractItem(slot, 1, true);
            if (sim.isEmpty()) continue;

            if (!belt[0].isEmpty() && !ItemStack.isSameItemSameComponents(belt[0], sim)) continue;

            ItemStack pulled = input.extractItem(slot, 1, false);
            if (pulled.isEmpty()) continue;

            belt[0] = insertIntoSegment(belt[0], pulled);
            return true; // one per tick
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
        if (cur.isEmpty()) belt[index] = ItemStack.EMPTY;
        if (!split.isEmpty()) sync();
        return split;
    }

    @Override public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= SEGMENTS) return ItemStack.EMPTY;
        ItemStack cur = belt[index];
        belt[index] = ItemStack.EMPTY;
        return cur;
    }

    @Override public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= SEGMENTS) return;
        belt[index] = stack;
        sync();
    }

    @Override public void clearContent() {
        for (int i = 0; i < SEGMENTS; i++) belt[i] = ItemStack.EMPTY;
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
