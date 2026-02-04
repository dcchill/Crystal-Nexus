package net.crystalnexus.block.entity;

import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.Mth;

import net.crystalnexus.world.inventory.QuarryGUIMenu;
import net.crystalnexus.init.CrystalnexusModBlockEntities;

import javax.annotation.Nullable;

import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

import io.netty.buffer.Unpooled;

public class QuarryBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

	// ----------------- CONFIG -----------------
	private static final int FE_PER_BLOCK = 128;
	private static final int BLOCKS_PER_TICK = 1;
	private static final int COOLDOWN_TICKS = 2;

	// Beam disappears after no blocks are found for this many ticks
	private static final int BEAM_IDLE_TICKS = 5;

	// How many air/unmineable positions we can skip per tick while searching
	private static final int SKIP_LIMIT_PER_TICK = 1024;

	// ----------------- INVENTORY -----------------
	// Slots: 0..8 output, 9 upgrade
	private NonNullList<ItemStack> stacks = NonNullList.withSize(10, ItemStack.EMPTY);

	// ----------------- QUARRY STATE -----------------
	private List<Vec3i> chunkOrder = null;
	private int orderIndex = 0;
	private int layerY = Integer.MIN_VALUE;
	private int cooldown = 0;

	// Laser target (synced to client)
	private BlockPos targetPos = null;

	// counts up while idle AND beam is currently visible
	private int beamIdleTimer = 0;

	public QuarryBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.QUARRY.get(), position, state);
	}

	// ================= TICK =================
	public static void tick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity be) {
		if (level.isClientSide) return;

		// init layer so it isn't MIN_VALUE forever
		if (be.layerY == Integer.MIN_VALUE) {
			be.layerY = be.worldPosition.getY() - 1;
			be.orderIndex = 0;
			be.setChanged();
		}

		// powered -> STOP, unpowered -> RUN
		if (level.hasNeighborSignal(pos)) {
			be.tickBeamIdle();
			return;
		}

		if (be.cooldown > 0) {
			be.cooldown--;
			be.tickBeamIdle();
			return;
		}

		if (be.chunkOrder == null) {
			be.chunkOrder = buildOuterToInnerChunkOrder();
			be.setChanged();
		}

		int mined = 0;

		while (mined < BLOCKS_PER_TICK) {

			// Find a mineable position, skipping air/unmineable but not skipping mineable blocks.
			BlockPos scanPos = be.findNextMineableOrAdvance(level);

			if (scanPos == null) {
				// No mineable blocks right now -> fade the beam after 20 ticks
				be.tickBeamIdle();
				return;
			}

			// We found something to aim at, so keep beam alive
			be.markBeamActive();
			be.setTargetPos(scanPos);

			// If stalled (no FE), do NOT advance pointer
			if (be.energyStorage.getEnergyStored() < FE_PER_BLOCK) {
				return;
			}

			BlockState bsMine = level.getBlockState(scanPos);

			// If it became invalid, advance ONE step and try next tick
			if (bsMine.isAir() || !isMineable(level, scanPos)) {
				be.advanceScan(level);
				return;
			}

			// Try mining; if outputs full, pause (do NOT advance pointer)
			boolean didMine = be.tryMineAndOutput(level, scanPos, bsMine);
			if (!didMine) {
				return;
			}

			// Mining succeeded -> NOW advance exactly one scan position
			be.advanceScan(level);

			mined++;

			// apply SSD cook_mult to cooldown
			double cookMult = be.getCookMultFromUpgrade();
			be.cooldown = Math.max(0, (int) Math.round(COOLDOWN_TICKS * cookMult));

			be.setChanged();
			level.sendBlockUpdated(be.worldPosition, be.getBlockState(), be.getBlockState(), 2);

			if (be.cooldown > 0) return;
		}
	}

	@Nullable
	private BlockPos findNextMineableOrAdvance(Level level) {
		if (chunkOrder == null || chunkOrder.isEmpty()) return null;

		ChunkPos cp = new ChunkPos(this.worldPosition);
		int minX = cp.getMinBlockX();
		int minZ = cp.getMinBlockZ();

		int minY = level.getMinBuildHeight();
		int startY = this.worldPosition.getY() - 1;

		if (layerY == Integer.MIN_VALUE || layerY > startY) layerY = startY;

		int skips = 0;

		// two passes: current scan, then full rescan from top
		for (int pass = 0; pass < 2; pass++) {

			while (layerY >= minY) {

				if (orderIndex >= chunkOrder.size()) {
					orderIndex = 0;
					layerY--;
					continue;
				}

				Vec3i local = chunkOrder.get(orderIndex);
				BlockPos candidate = new BlockPos(minX + local.getX(), layerY, minZ + local.getZ());

				if (isMineable(level, candidate)) {
					return candidate; // STOP on mineable
				}

				advanceScan(level);
				skips++;

				if (skips >= SKIP_LIMIT_PER_TICK) {
					return null; // keep scanning next tick
				}
			}

			// reached bottom -> loop back to top for rescan
			layerY = startY;
			orderIndex = 0;
		}

		return null;
	}

	private void advanceScan(Level level) {
		if (chunkOrder == null || chunkOrder.isEmpty()) return;

		int minY = level.getMinBuildHeight();
		int startY = this.worldPosition.getY() - 1;

		if (layerY == Integer.MIN_VALUE) layerY = startY;

		orderIndex++;

		if (orderIndex >= chunkOrder.size()) {
			orderIndex = 0;
			layerY--;
		}

		if (layerY < minY) {
			layerY = startY;
			orderIndex = 0;
		}
	}

	// ================= BEAM IDLE =================
	private void markBeamActive() {
		beamIdleTimer = 0;
	}

	private void tickBeamIdle() {
		// only count down if beam is currently visible
		if (targetPos == null) {
			beamIdleTimer = 0;
			return;
		}

		beamIdleTimer++;

		if (beamIdleTimer >= BEAM_IDLE_TICKS) {
			setTargetPos(null);  // IMPORTANT: this MUST sync to client
			beamIdleTimer = 0;
		}
	}

	private boolean tryMineAndOutput(Level level, BlockPos pos, BlockState state) {
		if (level.isClientSide) return false;

		LootParams.Builder builder = new LootParams.Builder((net.minecraft.server.level.ServerLevel) level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

		BlockEntity maybeBe = level.getBlockEntity(pos);
		if (maybeBe != null) builder.withOptionalParameter(LootContextParams.BLOCK_ENTITY, maybeBe);

		List<ItemStack> drops = state.getDrops(builder);

		if (!canAcceptAllDrops(level, drops)) return false;

		if (this.energyStorage.getEnergyStored() < FE_PER_BLOCK) return false;
		this.energyStorage.extractEnergy(FE_PER_BLOCK, false);

		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		level.levelEvent(2001, pos, Block.getId(state));

		for (ItemStack drop : drops) {
			ItemStack rem = insertToTopInventory(level, drop.copy(), false);
			rem = insertIntoOutput(rem, false);
		}

		this.setChanged();
		level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);

		return true;
	}

	private boolean canAcceptAllDrops(Level level, List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			ItemStack rem = insertToTopInventory(level, drop.copy(), true);
			rem = insertIntoOutput(rem, true);
			if (!rem.isEmpty()) return false;
		}
		return true;
	}

	private ItemStack insertToTopInventory(Level level, ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) return ItemStack.EMPTY;

		BlockPos abovePos = this.worldPosition.above();
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, abovePos, Direction.DOWN);
		if (handler == null) return stack;

		ItemStack remaining = stack;
		for (int i = 0; i < handler.getSlots(); i++) {
			remaining = handler.insertItem(i, remaining, simulate);
			if (remaining.isEmpty()) return ItemStack.EMPTY;
		}
		return remaining;
	}

	private static boolean isMineable(Level level, BlockPos pos) {
		BlockState bs = level.getBlockState(pos);
		if (bs.isAir()) return false;

		FluidState fs = bs.getFluidState();
		if (!fs.isEmpty()) return false;

		if (bs.getDestroySpeed(level, pos) < 0) return false;

		if (level.getBlockEntity(pos) != null) return false;

		return true;
	}

	private ItemStack insertIntoOutput(ItemStack stackIn, boolean simulate) {
		if (stackIn.isEmpty()) return ItemStack.EMPTY;

		ItemStack stack = stackIn.copy();

		for (int i = 0; i <= 8; i++) {
			ItemStack slot = stacks.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int max = Math.min(slot.getMaxStackSize(), this.getMaxStackSize());
				int space = max - slot.getCount();
				if (space > 0) {
					int move = Math.min(space, stack.getCount());
					if (!simulate) {
						slot.grow(move);
						stacks.set(i, slot);
					}
					stack.shrink(move);
					if (stack.isEmpty()) return ItemStack.EMPTY;
				}
			}
		}

		for (int i = 0; i <= 8; i++) {
			ItemStack slot = stacks.get(i);
			if (slot.isEmpty()) {
				int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), this.getMaxStackSize()));
				if (!simulate) {
					ItemStack put = stack.copy();
					put.setCount(move);
					stacks.set(i, put);
				}
				stack.shrink(move);
				if (stack.isEmpty()) return ItemStack.EMPTY;
			}
		}

		return stack;
	}

	private static List<Vec3i> buildOuterToInnerChunkOrder() {
		List<Vec3i> out = new ArrayList<>(256);

		for (int ring = 0; ring < 8; ring++) {
			int min = ring;
			int max = 15 - ring;

			for (int x = min; x <= max; x++) out.add(new Vec3i(x, 0, min));
			for (int z = min + 1; z <= max; z++) out.add(new Vec3i(max, 0, z));
			for (int x = max - 1; x >= min; x--) out.add(new Vec3i(x, 0, max));
			for (int z = max - 1; z >= min + 1; z--) out.add(new Vec3i(min, 0, z));
		}

		return out;
	}

	// ================= SSD UPGRADE =================
	private ItemStack getUpgradeStack() {
		return this.stacks.size() > 9 ? this.stacks.get(9) : ItemStack.EMPTY;
	}

	private double getCookMultFromUpgrade() {
		ItemStack up = getUpgradeStack();
		if (up.isEmpty()) return 1.0;

		CustomData cd = up.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag tag = cd.copyTag();

		double m = tag.contains("cook_mult") ? tag.getDouble("cook_mult") : 1.0;
		return Mth.clamp(m, 0.05, 10.0);
	}

	// ================= LASER TARGET =================
	public @Nullable BlockPos getTargetPos() {
		return targetPos;
	}

	// ---- CLIENT CACHE (reflection, works in MCreator) ----
	private void clientCacheSet(@Nullable BlockPos target) {
		if (this.level == null || !this.level.isClientSide) return;
		try {
			Class<?> c = Class.forName("net.crystalnexus.client.render.QuarryBeamClientCache");
			Method m = c.getMethod("set", BlockPos.class, BlockPos.class);
			m.invoke(null, this.worldPosition, target);
		} catch (Throwable ignored) {
		}
	}

	private void clientCacheRemove() {
		if (this.level == null || !this.level.isClientSide) return;
		try {
			Class<?> c = Class.forName("net.crystalnexus.client.render.QuarryBeamClientCache");
			Method m = c.getMethod("remove", BlockPos.class);
			m.invoke(null, this.worldPosition);
		} catch (Throwable ignored) {
		}
	}

	private void setTargetPos(@Nullable BlockPos newTarget) {
		if ((this.targetPos == null && newTarget == null) ||
			(this.targetPos != null && this.targetPos.equals(newTarget))) {
			return;
		}

		this.targetPos = newTarget;
		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
		}

		// IMPORTANT:
		// - if target is null, REMOVE the cache entry (many caches ignore set(null))
		if (this.targetPos == null) clientCacheRemove();
		else clientCacheSet(this.targetPos);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (this.targetPos == null) clientCacheRemove();
		else clientCacheSet(this.targetPos);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		clientCacheRemove();
	}

	public AABB getRenderBoundingBox() {
		ChunkPos cp = new ChunkPos(this.worldPosition);
		int minX = cp.getMinBlockX();
		int minZ = cp.getMinBlockZ();
		int maxX = minX + 15;
		int maxZ = minZ + 15;

		int minY = this.level != null ? this.level.getMinBuildHeight() : -64;
		int maxY = this.level != null ? this.level.getMaxBuildHeight() : 320;

		return new AABB(minX, minY, minZ, maxX + 1, maxY, maxZ + 1);
	}

	// ================= SAVE/LOAD (FIXED TARGET CLEAR) =================
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);

		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(lookupProvider, intTag);

		orderIndex = compound.getInt("orderIndex");
		layerY = compound.contains("layerY") ? compound.getInt("layerY") : Integer.MIN_VALUE;
		cooldown = compound.getInt("cooldown");
		beamIdleTimer = compound.getInt("beamIdleTimer");

		// ✅ THIS is the important part:
		boolean hasTarget = compound.getBoolean("hasTargetPos");
		if (hasTarget && compound.contains("targetPos")) {
			targetPos = BlockPos.of(compound.getLong("targetPos"));
		} else {
			targetPos = null; // <-- forces beam to stop on client
		}

		if (this.level != null && this.level.isClientSide) {
			if (this.targetPos == null) clientCacheRemove();
			else clientCacheSet(this.targetPos);
		}
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));

		compound.putInt("orderIndex", orderIndex);
		compound.putInt("layerY", layerY);
		compound.putInt("cooldown", cooldown);
		compound.putInt("beamIdleTimer", beamIdleTimer);

		// ✅ Always sync whether target exists
		compound.putBoolean("hasTargetPos", targetPos != null);
		if (targetPos != null) compound.putLong("targetPos", targetPos.asLong());
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	// ================= GUI / CONTAINER =================
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
		return Component.literal("quarry");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new QuarryGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Quarry");
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
		if (index == 9) {
			CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag tag = cd.copyTag();
			return tag.contains("cook_mult");
		}
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		if (index == 9) return false;
		return true;
	}

	// ================= ENERGY =================
	private final EnergyStorage energyStorage = new EnergyStorage(409600, 20480, 10240, 0) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int retval = super.receiveEnergy(maxReceive, simulate);
			if (!simulate) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			int retval = super.extractEnergy(maxExtract, simulate);
			if (!simulate) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}
	};

	public EnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	public void dropContents() {
	}
}
