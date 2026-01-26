package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class SmartSplitterBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

	// Slot layout
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FILTER_LEFT = 1;
	public static final int SLOT_FILTER_FORWARD = 2;
	public static final int SLOT_FILTER_RIGHT = 3;
	public static final int SLOT_OVERFLOW_BUFFER = 4; // was "overflow filter", now buffer

	private NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);

	public SmartSplitterBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.SMART_SPLITTER.get(), position, state);
	}

	/* -------------------------
	   Saving / sync
	   ------------------------- */

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	/* -------------------------
	   Container basics
	   ------------------------- */

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("smart_splitter");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		// If you want your custom GUI, keep this as whatever MCreator uses OR leave it;
		// your block opens SmartSplitterGUIMenu directly, so this isn't critical.
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Smart Splitter");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return true;
	}

	/* -------------------------
	   Smart routing tick
	   ------------------------- */

	/**
	 * Call this from your block ticker.
	 */
	public static void tick(Level level, BlockPos pos, BlockState state, SmartSplitterBlockEntity be) {
		if (level.isClientSide) return;

		ItemStack in = be.getItem(SLOT_INPUT);
		if (in.isEmpty()) return;

		Direction facing = getHorizontalFacing(state);
		if (facing == null) return;

		Direction leftDir = facing.getCounterClockWise();
		Direction rightDir = facing.getClockWise();
		Direction forwardDir = facing;

		ItemStack leftFilter = be.getItem(SLOT_FILTER_LEFT);
		ItemStack fwdFilter = be.getItem(SLOT_FILTER_FORWARD);
		ItemStack rightFilter = be.getItem(SLOT_FILTER_RIGHT);

		// Attempt to route exactly 1 item
		ItemStack one = in.copy();
		one.setCount(1);

		boolean moved = false;

		// 1) Strict filter matches first (priority: left -> forward -> right)
		if (!moved && matchesFilter(one, leftFilter)) {
			moved = tryInsertIntoNeighbor(level, pos, leftDir, one);
		}
		if (!moved && matchesFilter(one, fwdFilter)) {
			moved = tryInsertIntoNeighbor(level, pos, forwardDir, one);
		}
		if (!moved && matchesFilter(one, rightFilter)) {
			moved = tryInsertIntoNeighbor(level, pos, rightDir, one);
		}

		// 2) If not matched anywhere, empty filter slots act as "ANY (unmatched)"
		if (!moved) {
			if (!moved && leftFilter.isEmpty()) {
				moved = tryInsertIntoNeighbor(level, pos, leftDir, one);
			}
			if (!moved && fwdFilter.isEmpty()) {
				moved = tryInsertIntoNeighbor(level, pos, forwardDir, one);
			}
			if (!moved && rightFilter.isEmpty()) {
				moved = tryInsertIntoNeighbor(level, pos, rightDir, one);
			}
		}

		// 3) If still not moved, push into slot 4 (overflow buffer)
		if (!moved) {
			moved = tryMoveOneIntoOverflowBuffer(be, one);
		}

		if (moved) {
			be.removeItem(SLOT_INPUT, 1);
			be.setChanged();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private static boolean matchesFilter(ItemStack item, ItemStack filter) {
		if (filter.isEmpty()) return false;
		return ItemStack.isSameItemSameComponents(item, filter);
	}

	private static boolean tryInsertIntoNeighbor(Level level, BlockPos pos, Direction dir, ItemStack stack) {
		BlockPos neighborPos = pos.relative(dir);

		// NeoForge 1.21.x: returns IItemHandler or null
		IItemHandler handler = level.getCapability(
				Capabilities.ItemHandler.BLOCK,
				neighborPos,
				dir.getOpposite()
		);

		if (handler == null) return false;

		ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack, false);
		return remainder.isEmpty();
	}

	private static boolean tryMoveOneIntoOverflowBuffer(SmartSplitterBlockEntity be, ItemStack one) {
		ItemStack overflow = be.getItem(SLOT_OVERFLOW_BUFFER);

		if (overflow.isEmpty()) {
			be.setItem(SLOT_OVERFLOW_BUFFER, one.copy());
			return true;
		}

		if (!ItemStack.isSameItemSameComponents(overflow, one)) return false;
		if (overflow.getCount() >= overflow.getMaxStackSize()) return false;

		overflow.grow(1);
		be.setItem(SLOT_OVERFLOW_BUFFER, overflow);
		return true;
	}

	/**
	 * Generic way to get the block's horizontal facing without referencing SmartSplitterBlock.FACING.
	 */
	@Nullable
	private static Direction getHorizontalFacing(BlockState state) {
		for (var prop : state.getProperties()) {
			if (prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dp) {
				Direction d = state.getValue(dp);
				if (d != Direction.UP && d != Direction.DOWN) return d;
			}
		}
		return null;
	}

	/* -------------------------
	   Sided IO rules (WorldlyContainer)
	   ------------------------- */

	@Override
	public int[] getSlotsForFace(Direction side) {
		// Back = input, Top = filters, Bottom = overflow buffer extraction
		if (this.level == null) return new int[]{SLOT_INPUT};

		Direction facing = getHorizontalFacing(this.getBlockState());
		if (facing == null) return new int[]{SLOT_INPUT};

		Direction back = facing.getOpposite();

		if (side == Direction.UP) {
			// Filters only
			return new int[]{SLOT_FILTER_LEFT, SLOT_FILTER_FORWARD, SLOT_FILTER_RIGHT};
		}
		if (side == back) {
			// Input only
			return new int[]{SLOT_INPUT};
		}
		if (side == Direction.DOWN) {
			// Overflow buffer output
			return new int[]{SLOT_OVERFLOW_BUFFER};
		}
		return new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		if (direction == null || this.level == null) return false;

		Direction facing = getHorizontalFacing(this.getBlockState());
		if (facing == null) return false;

		Direction back = facing.getOpposite();

		// Insert to INPUT from back
		if (direction == back) return index == SLOT_INPUT;

		// Set filters from top (do NOT allow automation to insert into overflow buffer)
		if (direction == Direction.UP) {
			return index == SLOT_FILTER_LEFT
					|| index == SLOT_FILTER_FORWARD
					|| index == SLOT_FILTER_RIGHT;
		}

		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		// Only allow automation to extract overflow buffer from the bottom
		return index == SLOT_OVERFLOW_BUFFER && direction == Direction.DOWN;
	}
}
