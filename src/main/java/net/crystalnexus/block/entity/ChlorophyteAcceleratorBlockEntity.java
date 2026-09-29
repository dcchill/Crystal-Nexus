package net.crystalnexus.block.entity;


import net.crystalnexus.config.CrystalnexusConfig;
import net.neoforged.neoforge.energy.EnergyStorage;

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
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;

import net.crystalnexus.init.CrystalnexusModBlockEntities;

import javax.annotation.Nullable;

import java.util.stream.IntStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ChlorophyteAcceleratorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private static final int RADIUS = 5;
	private static final int HEIGHT = 3;
	private static final int SCAN_VOLUME = (RADIUS * 2 + 1) * (RADIUS * 2 + 1) * HEIGHT;
	private static final int SCAN_PER_CYCLE = (SCAN_VOLUME + 19) / 20;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(0, ItemStack.EMPTY);
	private final Set<BlockPos> growableTargets = new LinkedHashSet<>();
	private int scanCursor;

	public ChlorophyteAcceleratorBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.CHLOROPHYTE_ACCELERATOR.get(), position, state);
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(lookupProvider, intTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("energyStorage", energyStorage.serializeNBT(lookupProvider));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

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
		return Component.literal("chlorophyte_accelerator");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Chlorophyte Growth Accelerator");
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
		return true;
	}

	private final EnergyStorage energyStorage = new EnergyStorage(CrystalnexusConfig.MACHINES.CHLOROPHYTE_ACCELERATOR.capacity(), CrystalnexusConfig.MACHINES.CHLOROPHYTE_ACCELERATOR.maxReceive(), CrystalnexusConfig.MACHINES.CHLOROPHYTE_ACCELERATOR.maxExtract(), 0) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int retval = super.receiveEnergy(maxReceive, simulate);
			if (!simulate) {
				setChanged();
			}
			return retval;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			int retval = super.extractEnergy(maxExtract, simulate);
			if (!simulate) {
				setChanged();
			}
			return retval;
		}
	};

	public EnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	public void runAccelerationCycle(ServerLevel level) {
		refreshTargetSlice(level);
		if (energyStorage.getEnergyStored() < 256) return;

		Iterator<BlockPos> targets = growableTargets.iterator();
		while (targets.hasNext()) {
			BlockPos target = targets.next();
			BlockState state = level.getBlockState(target);
			if (!isGrowable(state)) {
				targets.remove();
				continue;
			}
			Block block = state.getBlock();
			if (block instanceof CropBlock crop) {
				int age = state.getValue(CropBlock.AGE);
				if (level.random.nextFloat() < 0.25f) {
					level.setBlock(target, state.setValue(CropBlock.AGE, age + 1), 2);
					if (age + 1 >= crop.getMaxAge()) targets.remove();
				}
			} else if (block instanceof AmethystClusterBlock) {
				level.scheduleTick(target, block, 1 + level.random.nextInt(3));
			} else if (block instanceof NetherWartBlock) {
				int age = state.getValue(NetherWartBlock.AGE);
				if (level.random.nextFloat() < 0.25f) {
					level.setBlock(target, state.setValue(NetherWartBlock.AGE, age + 1), 2);
					if (age + 1 >= 3) targets.remove();
				}
			} else if (state.hasProperty(SugarCaneBlock.AGE)) {
				int age = state.getValue(SugarCaneBlock.AGE);
				if (level.random.nextFloat() < 0.25f) {
					level.setBlock(target, state.setValue(SugarCaneBlock.AGE, age + 1), 2);
					if (age + 1 >= 15) targets.remove();
				}
			} else if (state.hasProperty(CactusBlock.AGE)) {
				int age = state.getValue(CactusBlock.AGE);
				if (level.random.nextFloat() < 0.25f) {
					level.setBlock(target, state.setValue(CactusBlock.AGE, age + 1), 2);
					if (age + 1 >= 15) targets.remove();
				}
			}
		}
		energyStorage.extractEnergy(256, false);
	}

	private void refreshTargetSlice(ServerLevel level) {
		for (int scanned = 0; scanned < SCAN_PER_CYCLE; scanned++) {
			int index = scanCursor++;
			if (scanCursor >= SCAN_VOLUME) scanCursor = 0;
			int dy = index % HEIGHT - 1;
			int horizontal = index / HEIGHT;
			int dz = horizontal % (RADIUS * 2 + 1) - RADIUS;
			int dx = horizontal / (RADIUS * 2 + 1) - RADIUS;
			BlockPos target = worldPosition.offset(dx, dy, dz);
			if (isGrowable(level.getBlockState(target))) growableTargets.add(target.immutable());
			else growableTargets.remove(target);
		}
	}

	static boolean isGrowable(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.TORCHFLOWER_CROP) return false;
		if (block instanceof CropBlock crop)
			return state.hasProperty(CropBlock.AGE) && state.getValue(CropBlock.AGE) < crop.getMaxAge();
		if (block instanceof NetherWartBlock)
			return state.getValue(NetherWartBlock.AGE) < 3;
		if (block instanceof AmethystClusterBlock) return true;
		if (state.hasProperty(SugarCaneBlock.AGE)) return state.getValue(SugarCaneBlock.AGE) < 15;
		return state.hasProperty(CactusBlock.AGE) && state.getValue(CactusBlock.AGE) < 15;
	}

	public int cachedTargetCount() { return growableTargets.size(); }
}
