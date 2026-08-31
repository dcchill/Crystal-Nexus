package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.ModChunkTickets;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.util.QuarryChunkSelection;
import net.crystalnexus.world.inventory.HyperLaserQuarryMenu;
import net.crystalnexus.world.inventory.QuarryGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class QuarryBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int FE_PER_BLOCK = 1024;
	public static final int HYPER_BUFFER_SLOTS = 256;
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_MINING = 1;
	public static final int STATUS_NO_POWER = 2;
	public static final int STATUS_BUFFER_FULL = 3;
	public static final int STATUS_REDSTONE_STOPPED = 4;

	private static final int BLOCKS_PER_TICK = 1;
	private static final int COOLDOWN_TICKS = 2;
	private static final int BEAM_IDLE_TICKS = 5;
	private static final int SKIP_LIMIT_PER_TICK = 1024;
	private static final ItemStack VIRTUAL_TOOL = new ItemStack(Items.NETHERITE_PICKAXE);

	// Slots 0..8 are visible output, slot 9 is the SSD upgrade.
	private NonNullList<ItemStack> stacks = NonNullList.withSize(10, ItemStack.EMPTY);
	private final ItemStackHandler hiddenBuffer = new ItemStackHandler(HYPER_BUFFER_SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private List<Vec3i> chunkOrder;
	private int orderIndex;
	private int layerY = Integer.MIN_VALUE;
	private int cooldown;
	private int selectionWidth = 1;
	private int selectionDepth = 1;
	private List<QuarryChunkSelection.BlockOffset> hyperSliceOrder;
	private int status = STATUS_IDLE;
	private final Set<ChunkPos> ticketedChunks = new HashSet<>();

	@Nullable private BlockPos targetPos;
	private int beamIdleTimer;
	private final EnergyStorage energyStorage;

	private final ContainerData hyperData = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> selectionWidth;
				case 1 -> selectionDepth;
				case 2 -> layerY == Integer.MIN_VALUE ? worldPosition.getY() - 1 : layerY;
				case 3 -> getBufferedSlotsUsed();
				case 4 -> status;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) selectionWidth = QuarryChunkSelection.clampSize(value);
			else if (index == 1) selectionDepth = QuarryChunkSelection.clampSize(value);
			else if (index == 2) layerY = value;
			else if (index == 4) status = value;
		}

		@Override public int getCount() { return 5; }
	};

	public QuarryBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.QUARRY.get(), position, state);
		var power = state.is(CrystalnexusModBlocks.HYPER_LASER_QUARRY.get())
			? CrystalnexusConfig.MACHINES.HYPER_LASER_QUARRY
			: CrystalnexusConfig.MACHINES.QUARRY;
		this.energyStorage = new EnergyStorage(power.capacity(), power.maxReceive(), power.maxExtract(), 0) {
			@Override
			public int receiveEnergy(int maxReceive, boolean simulate) {
				int received = super.receiveEnergy(maxReceive, simulate);
				if (received > 0 && !simulate) sync();
				return received;
			}

			@Override
			public int extractEnergy(int maxExtract, boolean simulate) {
				int extracted = super.extractEnergy(maxExtract, simulate);
				if (extracted > 0 && !simulate) sync();
				return extracted;
			}
		};
	}

	public static void tick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
		if (level.isClientSide) return;
		if (quarry.isHyper()) quarry.tickHyper((ServerLevel) level);
		else quarry.tickNormal(level, pos);
	}

	private void tickNormal(Level level, BlockPos pos) {
		if (layerY == Integer.MIN_VALUE) {
			layerY = worldPosition.getY() - 1;
			orderIndex = 0;
			setChanged();
		}
		if (level.hasNeighborSignal(pos)) {
			tickBeamIdle();
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			tickBeamIdle();
			return;
		}
		if (chunkOrder == null) chunkOrder = buildOuterToInnerChunkOrder();

		int mined = 0;
		while (mined < BLOCKS_PER_TICK) {
			BlockPos scanPos = findNextMineableOrAdvance(level);
			if (scanPos == null) {
				tickBeamIdle();
				return;
			}
			markBeamActive();
			setTargetPos(scanPos);
			if (energyStorage.getEnergyStored() < FE_PER_BLOCK) return;
			BlockState state = level.getBlockState(scanPos);
			if (!isMineable(level, scanPos)) {
				advanceScan(level);
				return;
			}
			if (!tryMineAndOutput(level, scanPos, state)) return;
			advanceScan(level);
			mined++;
			cooldown = upgradedCooldown();
			sync();
			if (cooldown > 0) return;
		}
	}

	private void tickHyper(ServerLevel level) {
		drainHiddenBuffer(level);
		if (layerY == Integer.MIN_VALUE) layerY = worldPosition.getY() - 1;
		if (level.hasNeighborSignal(worldPosition)) {
			status = STATUS_REDSTONE_STOPPED;
			releaseTickets(level);
			tickBeamIdle();
			sync();
			return;
		}
		ensureTickets(level);
		if (cooldown > 0) {
			cooldown--;
			status = STATUS_IDLE;
			tickBeamIdle();
			return;
		}

		List<SliceEntry> slice = collectHyperSlice(level);
		if (slice.isEmpty()) {
			advanceHyperLayer(level);
			status = STATUS_IDLE;
			tickBeamIdle();
			sync();
			return;
		}
		BlockPos beamTarget = slice.getLast().pos();
		setTargetPos(beamTarget);
		markBeamActive();
		int energyCost = Math.multiplyExact(slice.size(), FE_PER_BLOCK);
		if (energyStorage.getEnergyStored() < energyCost) {
			status = STATUS_NO_POWER;
			sync();
			return;
		}
		List<ItemStack> drops = slice.stream().flatMap(entry -> entry.drops().stream()).toList();
		if (!bufferCanAccept(drops)) {
			status = STATUS_BUFFER_FULL;
			sync();
			return;
		}

		energyStorage.extractEnergy(energyCost, false);
		for (SliceEntry entry : slice)
			level.setBlock(entry.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		for (ItemStack drop : drops) insertIntoHandler(hiddenBuffer, drop.copy());
		status = STATUS_MINING;
		cooldown = upgradedCooldown();
		advanceHyperLayer(level);
		sync();
	}

	private List<SliceEntry> collectHyperSlice(ServerLevel level) {
		if (hyperSliceOrder == null)
			hyperSliceOrder = QuarryChunkSelection.blockOffsetsOuterToInner(selectionWidth, selectionDepth);
		ChunkPos center = new ChunkPos(worldPosition);
		List<SliceEntry> slice = new ArrayList<>();
		for (QuarryChunkSelection.BlockOffset offset : hyperSliceOrder) {
			BlockPos pos = new BlockPos(center.getMinBlockX() + offset.x(), layerY,
				center.getMinBlockZ() + offset.z());
			if (!isMineable(level, pos)) continue;
			BlockState state = level.getBlockState(pos);
			LootParams.Builder loot = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
				.withParameter(LootContextParams.TOOL, VIRTUAL_TOOL.copy());
			slice.add(new SliceEntry(pos, state.getDrops(loot)));
		}
		return slice;
	}

	private record SliceEntry(BlockPos pos, List<ItemStack> drops) {}

	private boolean bufferCanAccept(List<ItemStack> drops) {
		ItemStackHandler scratch = new ItemStackHandler(HYPER_BUFFER_SLOTS);
		for (int slot = 0; slot < HYPER_BUFFER_SLOTS; slot++) scratch.setStackInSlot(slot, hiddenBuffer.getStackInSlot(slot).copy());
		for (ItemStack drop : drops) if (!insertIntoHandler(scratch, drop.copy()).isEmpty()) return false;
		return true;
	}

	private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
		ItemStack remaining = stack;
		for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) remaining = handler.insertItem(slot, remaining, false);
		return remaining;
	}

	private void drainHiddenBuffer(Level level) {
		for (int slot = 0; slot < HYPER_BUFFER_SLOTS; slot++) {
			ItemStack buffered = hiddenBuffer.getStackInSlot(slot);
			if (buffered.isEmpty()) continue;
			ItemStack remaining = insertIntoOutput(buffered.copy(), false);
			remaining = insertToTopInventory(level, remaining, false);
			hiddenBuffer.setStackInSlot(slot, remaining);
		}
	}

	private void advanceHyperLayer(Level level) {
		layerY--;
		if (layerY < level.getMinBuildHeight()) layerY = worldPosition.getY() - 1;
	}

	public List<ChunkPos> selectedChunks() {
		return QuarryChunkSelection.chunks(new ChunkPos(worldPosition), selectionWidth, selectionDepth);
	}

	private void ensureTickets(ServerLevel level) {
		if (ModChunkTickets.HYPER_LASER_QUARRY == null) return;
		Set<ChunkPos> desired = new HashSet<>(selectedChunks());
		for (ChunkPos chunk : new HashSet<>(ticketedChunks)) {
			if (!desired.contains(chunk)) {
				ModChunkTickets.HYPER_LASER_QUARRY.forceChunk(level, worldPosition, chunk.x, chunk.z, false, true);
				ticketedChunks.remove(chunk);
			}
		}
		for (ChunkPos chunk : desired) {
			if (ticketedChunks.add(chunk)) ModChunkTickets.HYPER_LASER_QUARRY.forceChunk(level, worldPosition, chunk.x, chunk.z, true, true);
		}
	}

	private void releaseTickets(ServerLevel level) {
		if (ModChunkTickets.HYPER_LASER_QUARRY == null) return;
		Set<ChunkPos> chunks = new HashSet<>(ticketedChunks);
		if (isHyper()) chunks.addAll(selectedChunks());
		for (ChunkPos chunk : chunks) ModChunkTickets.HYPER_LASER_QUARRY.forceChunk(level, worldPosition, chunk.x, chunk.z, false, true);
		ticketedChunks.clear();
	}

	public void resizeSelection(int widthDelta, int depthDelta) {
		if (!isHyper()) return;
		int width = QuarryChunkSelection.clampSize(selectionWidth + widthDelta);
		int depth = QuarryChunkSelection.clampSize(selectionDepth + depthDelta);
		if (width == selectionWidth && depth == selectionDepth) return;
		if (level instanceof ServerLevel serverLevel) releaseTickets(serverLevel);
		selectionWidth = width;
		selectionDepth = depth;
		layerY = worldPosition.getY() - 1;
		hyperSliceOrder = null;
		status = STATUS_IDLE;
		sync();
	}

	@Nullable
	private BlockPos findNextMineableOrAdvance(Level level) {
		if (chunkOrder == null || chunkOrder.isEmpty()) return null;
		ChunkPos chunk = new ChunkPos(worldPosition);
		int minY = level.getMinBuildHeight();
		int startY = worldPosition.getY() - 1;
		if (layerY == Integer.MIN_VALUE || layerY > startY) layerY = startY;
		int skips = 0;
		for (int pass = 0; pass < 2; pass++) {
			while (layerY >= minY) {
				if (orderIndex >= chunkOrder.size()) {
					orderIndex = 0;
					layerY--;
					continue;
				}
				Vec3i local = chunkOrder.get(orderIndex);
				BlockPos candidate = new BlockPos(chunk.getMinBlockX() + local.getX(), layerY, chunk.getMinBlockZ() + local.getZ());
				if (isMineable(level, candidate)) return candidate;
				advanceScan(level);
				if (++skips >= SKIP_LIMIT_PER_TICK) return null;
			}
			layerY = startY;
			orderIndex = 0;
		}
		return null;
	}

	private void advanceScan(Level level) {
		if (chunkOrder == null || chunkOrder.isEmpty()) return;
		if (layerY == Integer.MIN_VALUE) layerY = worldPosition.getY() - 1;
		orderIndex++;
		if (orderIndex >= chunkOrder.size()) {
			orderIndex = 0;
			layerY--;
		}
		if (layerY < level.getMinBuildHeight()) {
			layerY = worldPosition.getY() - 1;
			orderIndex = 0;
		}
	}

	private boolean tryMineAndOutput(Level level, BlockPos pos, BlockState state) {
		if (!isMineable(level, pos)) return false;
		LootParams.Builder builder = new LootParams.Builder((ServerLevel) level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withParameter(LootContextParams.TOOL, VIRTUAL_TOOL.copy());
		List<ItemStack> drops = state.getDrops(builder);
		if (!canAcceptAllDrops(level, drops) || energyStorage.getEnergyStored() < FE_PER_BLOCK) return false;
		energyStorage.extractEnergy(FE_PER_BLOCK, false);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.levelEvent(2001, pos, Block.getId(state));
		for (ItemStack drop : drops) {
			ItemStack remaining = insertToTopInventory(level, drop.copy(), false);
			insertIntoOutput(remaining, false);
		}
		sync();
		return true;
	}

	private boolean canAcceptAllDrops(Level level, List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			ItemStack remaining = insertToTopInventory(level, drop.copy(), true);
			remaining = insertIntoOutput(remaining, true);
			if (!remaining.isEmpty()) return false;
		}
		return true;
	}

	private ItemStack insertToTopInventory(Level level, ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.above(), Direction.DOWN);
		if (handler == null) return stack;
		ItemStack remaining = stack;
		for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) remaining = handler.insertItem(slot, remaining, simulate);
		return remaining;
	}

	public static boolean isMineable(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) return false;
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty() || state.getDestroySpeed(level, pos) < 0) return false;
		return level.getBlockEntity(pos) == null;
	}

	private ItemStack insertIntoOutput(ItemStack input, boolean simulate) {
		if (input.isEmpty()) return ItemStack.EMPTY;
		ItemStack stack = input.copy();
		for (int i = 0; i <= 8; i++) {
			ItemStack slot = stacks.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int space = Math.min(slot.getMaxStackSize(), getMaxStackSize()) - slot.getCount();
				int move = Math.min(space, stack.getCount());
				if (move > 0) {
					if (!simulate) slot.grow(move);
					stack.shrink(move);
					if (stack.isEmpty()) return ItemStack.EMPTY;
				}
			}
		}
		for (int i = 0; i <= 8; i++) {
			if (stacks.get(i).isEmpty()) {
				int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getMaxStackSize()));
				if (!simulate) {
					ItemStack placed = stack.copy();
					placed.setCount(move);
					stacks.set(i, placed);
				}
				stack.shrink(move);
				if (stack.isEmpty()) return ItemStack.EMPTY;
			}
		}
		return stack;
	}

	private static List<Vec3i> buildOuterToInnerChunkOrder() {
		List<Vec3i> positions = new ArrayList<>(256);
		for (int ring = 0; ring < 8; ring++) {
			int min = ring;
			int max = 15 - ring;
			for (int x = min; x <= max; x++) positions.add(new Vec3i(x, 0, min));
			for (int z = min + 1; z <= max; z++) positions.add(new Vec3i(max, 0, z));
			for (int x = max - 1; x >= min; x--) positions.add(new Vec3i(x, 0, max));
			for (int z = max - 1; z >= min + 1; z--) positions.add(new Vec3i(min, 0, z));
		}
		return positions;
	}

	private int upgradedCooldown() {
		ItemStack upgrade = stacks.get(9);
		if (upgrade.isEmpty()) return COOLDOWN_TICKS;
		CompoundTag tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		double multiplier = tag.contains("cook_mult") ? tag.getDouble("cook_mult") : 1.0;
		return Math.max(0, (int) Math.round(COOLDOWN_TICKS * Mth.clamp(multiplier, 0.05, 10.0)));
	}

	private void markBeamActive() { beamIdleTimer = 0; }

	private void tickBeamIdle() {
		if (targetPos == null) {
			beamIdleTimer = 0;
			return;
		}
		if (++beamIdleTimer >= BEAM_IDLE_TICKS) {
			setTargetPos(null);
			beamIdleTimer = 0;
		}
	}

	public @Nullable BlockPos getTargetPos() { return targetPos; }

	private void setTargetPos(@Nullable BlockPos target) {
		if (java.util.Objects.equals(targetPos, target)) return;
		targetPos = target;
		sync();
		if (targetPos == null) clientCacheRemove(); else clientCacheSet(targetPos);
	}

	private void clientCacheSet(BlockPos target) {
		if (level == null || !level.isClientSide) return;
		try {
			Class<?> cache = Class.forName("net.crystalnexus.client.render.QuarryBeamClientCache");
			Method set = cache.getMethod("set", BlockPos.class, BlockPos.class);
			set.invoke(null, worldPosition, target);
		} catch (Throwable ignored) {
		}
	}

	private void clientCacheRemove() {
		if (level == null || !level.isClientSide) return;
		try {
			Class<?> cache = Class.forName("net.crystalnexus.client.render.QuarryBeamClientCache");
			Method remove = cache.getMethod("remove", BlockPos.class);
			remove.invoke(null, worldPosition);
		} catch (Throwable ignored) {
		}
	}

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public boolean isHyper() { return getBlockState().is(CrystalnexusModBlocks.HYPER_LASER_QUARRY.get()); }

	public int getBufferedSlotsUsed() {
		int used = 0;
		for (int i = 0; i < hiddenBuffer.getSlots(); i++) if (!hiddenBuffer.getStackInSlot(i).isEmpty()) used++;
		return used;
	}

	public ContainerData getHyperData() { return hyperData; }
	public EnergyStorage getEnergyStorage() { return energyStorage; }

	public void dropBufferedContents() {
		if (level == null || level.isClientSide) return;
		for (int slot = 0; slot < hiddenBuffer.getSlots(); slot++) {
			ItemStack stack = hiddenBuffer.getStackInSlot(slot);
			if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
			hiddenBuffer.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (targetPos == null) clientCacheRemove(); else clientCacheSet(targetPos);
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel serverLevel) releaseTickets(serverLevel);
		super.setRemoved();
		clientCacheRemove();
	}

	public AABB getRenderBoundingBox() {
		List<ChunkPos> chunks = isHyper() ? selectedChunks() : List.of(new ChunkPos(worldPosition));
		ChunkPos first = chunks.getFirst();
		ChunkPos last = chunks.getLast();
		int minY = level != null ? level.getMinBuildHeight() : -64;
		int maxY = level != null ? level.getMaxBuildHeight() : 320;
		return new AABB(first.getMinBlockX(), minY, first.getMinBlockZ(), last.getMaxBlockX() + 1, maxY, last.getMaxBlockZ() + 1);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
		if (tag.get("energyStorage") instanceof IntTag energyTag) energyStorage.deserializeNBT(registries, energyTag);
		if (tag.get("hyperBuffer") instanceof CompoundTag bufferTag) hiddenBuffer.deserializeNBT(registries, bufferTag);
		orderIndex = tag.getInt("orderIndex");
		layerY = tag.contains("layerY") ? tag.getInt("layerY") : Integer.MIN_VALUE;
		cooldown = tag.getInt("cooldown");
		beamIdleTimer = tag.getInt("beamIdleTimer");
		selectionWidth = tag.contains("selectionWidth") ? QuarryChunkSelection.clampSize(tag.getInt("selectionWidth")) : 1;
		selectionDepth = tag.contains("selectionDepth") ? QuarryChunkSelection.clampSize(tag.getInt("selectionDepth")) : 1;
		status = tag.getInt("hyperStatus");
		targetPos = tag.getBoolean("hasTargetPos") && tag.contains("targetPos") ? BlockPos.of(tag.getLong("targetPos")) : null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.put("energyStorage", energyStorage.serializeNBT(registries));
		tag.put("hyperBuffer", hiddenBuffer.serializeNBT(registries));
		tag.putInt("orderIndex", orderIndex);
		tag.putInt("layerY", layerY);
		tag.putInt("cooldown", cooldown);
		tag.putInt("beamIdleTimer", beamIdleTimer);
		tag.putInt("selectionWidth", selectionWidth);
		tag.putInt("selectionDepth", selectionDepth);
		tag.putInt("hyperStatus", status);
		tag.putBoolean("hasTargetPos", targetPos != null);
		if (targetPos != null) tag.putLong("targetPos", targetPos.asLong());
	}

	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithFullMetadata(registries); }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
	@Override protected Component getDefaultName() { return Component.translatable(isHyper() ? "block.crystalnexus.hyper_laser_quarry" : "block.crystalnexus.quarry"); }
	@Override public Component getDisplayName() { return getDefaultName(); }

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		if (isHyper()) return new HyperLaserQuarryMenu(id, inventory, this, hyperData);
		return new QuarryGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		if (index == 9) return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("cook_mult");
		return !isHyper();
	}

	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) { return canPlaceItem(index, stack); }
	@Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) { return index != 9; }

}
