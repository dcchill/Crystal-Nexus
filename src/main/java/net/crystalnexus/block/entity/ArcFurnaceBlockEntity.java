package net.crystalnexus.block.entity;

import io.netty.buffer.Unpooled;
import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.procedures.ArcFurnaceOnTickUpdateProcedure;
import net.crystalnexus.multiblock.MultiblockPortTarget;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.block.ArcFurnaceBlock;
import net.crystalnexus.block.HeatingCoreBlock;
import net.crystalnexus.world.inventory.ArcFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class ArcFurnaceBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, MultiblockPortTarget {
	private static final int VALIDATION_INTERVAL = 20;
	private static final int MAX_HEATING_LAYERS = 7;
	private static final int MAX_ENERGY_INPUTS = 1;
	private static final int MAX_ITEM_INPUTS = 2;
	private static final int MAX_ITEM_OUTPUTS = 1;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);
	private final List<BlockPos> energyInputs = new ArrayList<>();
	private final List<BlockPos> itemInputs = new ArrayList<>();
	private final List<BlockPos> itemOutputs = new ArrayList<>();
	private final List<BlockPos> heatingCores = new ArrayList<>();
	private boolean formed;
	private int heatingLayerCount;
	private int validationDelay;

	public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.ARC_FURNACE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ArcFurnaceBlockEntity blockEntity) {
		if (!(level instanceof ServerLevel serverLevel))
			return;
		ArcFurnaceOnTickUpdateProcedure.execute(serverLevel, pos);
	}

	@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.loadAdditional(tag, provider);
		if (!tryLoadLootTable(tag)) stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, provider);
		if (tag.get("energyStorage") instanceof IntTag energy) energyStorage.deserializeNBT(provider, energy);
	}

	@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.saveAdditional(tag, provider);
		if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, stacks, provider);
		tag.put("energyStorage", energyStorage.serializeNBT(provider));
	}

	@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithFullMetadata(provider); }
	@Override public int getContainerSize() { return stacks.size(); }
	@Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
	@Override public Component getDefaultName() {
		return Component.translatable(recipeTier() == 1 ? "block.crystalnexus.azurine_blast_furnace" : "block.crystalnexus.arc_furnace");
	}
	@Override public Component getDisplayName() { return getDefaultName(); }
	@Override protected NonNullList<ItemStack> getItems() { return stacks; }
	@Override protected void setItems(NonNullList<ItemStack> stacks) { this.stacks = stacks; }
	@Override public int getMaxStackSize() { return 64; }
	@Override public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new ArcFurnaceMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot != 2; }
	@Override public int[] getSlotsForFace(Direction side) { return IntStream.range(0, getContainerSize()).toArray(); }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 2; }

	private final EnergyStorage energyStorage = new EnergyStorage(
		MachineTier.from(getBlockState()).minimumCapacity(CrystalnexusConfig.MACHINES.ARC_FURNACE.capacity(), 4096), CrystalnexusConfig.MACHINES.ARC_FURNACE.maxReceive(),
		CrystalnexusConfig.MACHINES.ARC_FURNACE.maxExtract(), 0) {
		@Override public int receiveEnergy(int amount, boolean simulate) {
			int received = super.receiveEnergy(amount, simulate); if (!simulate) changed(); return received;
		}
		@Override public int extractEnergy(int amount, boolean simulate) {
			int extracted = super.extractEnergy(amount, simulate); if (!simulate) changed(); return extracted;
		}
		private void changed() {
			setChanged();
			if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
		}
	};

	public EnergyStorage getEnergyStorage() { return energyStorage; }
	public boolean isFormed() { return formed; }
	public int heatingLayerCount() { return heatingLayerCount; }
	public int recipeTier() {
		return getBlockState().getBlock() instanceof ArcFurnaceBlock furnace ? furnace.recipeTier() : 2;
	}

	private Block heatingCoreBlock() {
		return recipeTier() == 1 ? CrystalnexusModBlocks.AZURINE_HEATING_CORE.get() : CrystalnexusModBlocks.HEATING_CORE.get();
	}

	private Block casingBlock() {
		return recipeTier() == 1 ? CrystalnexusModBlocks.TITANIUM_BLOCK.get() : CrystalnexusModBlocks.TITANIUM_CARBIDE_BLOCK.get();
	}

	public boolean validateStructureNow() {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		validateStructure(serverLevel);
		validationDelay = VALIDATION_INTERVAL;
		return formed;
	}

	public boolean prepareForProcessing(ServerLevel level) {
		if (validationDelay-- <= 0) {
			validateStructure(level);
			validationDelay = VALIDATION_INTERVAL;
		}
		if (!formed) return false;
		flushOutput();
		pullInputs();
		return true;
	}

	public int availableEnergy() {
		return energyStorage.getEnergyStored();
	}

	public int extractEnergy(int amount, boolean simulate) {
		return energyStorage.extractEnergy(amount, simulate);
	}

	public boolean consumeEnergy(int amount) {
		if (energyStorage.getEnergyStored() < amount) return false;
		int remaining = amount;
		while (remaining > 0) {
			int extracted = energyStorage.extractEnergy(remaining, false);
			if (extracted <= 0) return false;
			remaining -= extracted;
		}
		return true;
	}

	public void setHeatingCoresActive(boolean active) {
		if (level == null) return;
		for (BlockPos corePos : heatingCores) {
			BlockState state = level.getBlockState(corePos);
			if (state.is(heatingCoreBlock()) && state.getValue(HeatingCoreBlock.LIT) != active)
				level.setBlock(corePos, state.setValue(HeatingCoreBlock.LIT, active), 3);
		}
	}

	public void onControllerRemoved() {
		setHeatingCoresActive(false);
		if (level != null) for (BlockPos pos : energyInputs)
			if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
		energyInputs.clear();
		itemInputs.clear();
		itemOutputs.clear();
		heatingCores.clear();
		formed = false;
		heatingLayerCount = 0;
	}

	private void validateStructure(ServerLevel level) {
		boolean correctController = getBlockState().is(recipeTier() == 1
			? CrystalnexusModBlocks.AZURINE_BLAST_FURNACE.get() : CrystalnexusModBlocks.ARC_FURNACE.get());
		List<BlockPos> previousEnergyInputs = List.copyOf(energyInputs);
		List<BlockPos> previousHeatingCores = List.copyOf(heatingCores);
		energyInputs.clear();
		itemInputs.clear();
		itemOutputs.clear();
		heatingCores.clear();
		int layers = correctController ? scanLayers(level) : 0;
		if (layers == 0) {
			energyInputs.clear();
			itemInputs.clear();
			itemOutputs.clear();
			heatingCores.clear();
		}
		for (BlockPos old : previousEnergyInputs) if (!energyInputs.contains(old)
			&& level.getBlockEntity(old) instanceof MachineEnergyInputBlockEntity input) input.unbindController(worldPosition);
		energyInputs.removeIf(pos -> {
			if (level.getBlockEntity(pos) instanceof MachineEnergyInputBlockEntity input) {
				input.bindController(worldPosition);
				return false;
			}
			return true;
		});
		boolean nextFormed = layers > 0 && !energyInputs.isEmpty()
			&& itemInputs.stream().allMatch(pos -> level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity)
			&& itemOutputs.stream().allMatch(pos -> level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity);
		if (!nextFormed) for (BlockPos pos : previousHeatingCores) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof HeatingCoreBlock && state.getValue(HeatingCoreBlock.LIT))
				level.setBlock(pos, state.setValue(HeatingCoreBlock.LIT, false), 3);
		}
		heatingLayerCount = nextFormed ? layers : 0;
		if (formed != nextFormed) {
			formed = nextFormed;
			sync();
		} else formed = nextFormed;
	}

	private int scanLayers(ServerLevel level) {
		if (!scanCasingLayer(level, 0, true)) return 0;
		int layers = 0;
		while (layers < MAX_HEATING_LAYERS && scanHeatingLayer(level, layers + 1)) layers++;
		return layers > 0 && scanCasingLayer(level, layers + 1, false) ? layers : 0;
	}

	private boolean scanCasingLayer(ServerLevel level, int y, boolean bottom) {
		for (int depth = 0; depth < 3; depth++) for (int side = -1; side <= 1; side++) {
			BlockPos pos = structurePos(side, depth, y);
			if (bottom && depth == 0 && side == 0) {
				if (!pos.equals(worldPosition)) return false;
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(casingBlock())) continue;
			if (state.is(CrystalnexusModBlocks.MACHINE_ENERGY_INPUT.get())) energyInputs.add(pos);
			else if (state.is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_INPUT.get())) itemInputs.add(pos);
			else if (state.is(CrystalnexusModBlocks.MULTIBLOCK_ITEM_OUTPUT.get())) itemOutputs.add(pos);
			else return false;
			if (energyInputs.size() > MAX_ENERGY_INPUTS || itemInputs.size() > MAX_ITEM_INPUTS
				|| itemOutputs.size() > MAX_ITEM_OUTPUTS) return false;
		}
		return true;
	}

	private boolean scanHeatingLayer(ServerLevel level, int y) {
		for (int depth = 0; depth < 3; depth++) for (int side = -1; side <= 1; side++) {
			BlockPos pos = structurePos(side, depth, y);
			if (depth == 1 && side == 0) {
				if (!level.getBlockState(pos).isAir()) return false;
			} else {
				if (!level.getBlockState(pos).is(heatingCoreBlock())) return false;
				heatingCores.add(pos);
			}
		}
		return true;
	}

	private BlockPos structurePos(int side, int depth, int y) {
		Direction inward = getBlockState().getValue(ArcFurnaceBlock.FACING).getOpposite();
		return worldPosition.above(y).relative(inward, depth).relative(inward.getClockWise(), side);
	}

	private void pullInputs() {
		for (BlockPos pos : itemInputs) {
			if (!(level.getBlockEntity(pos) instanceof MultiblockItemInputBlockEntity input)) continue;
			for (int sourceSlot = 0; sourceSlot < input.getContainerSize(); sourceSlot++) {
				ItemStack source = input.getItem(sourceSlot);
				for (int targetSlot = 0; targetSlot < 2 && !source.isEmpty(); targetSlot++) {
					ItemStack target = stacks.get(targetSlot);
					if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source)) continue;
					int room = target.isEmpty() ? source.getMaxStackSize() : target.getMaxStackSize() - target.getCount();
					int moved = Math.min(room, source.getCount());
					if (moved <= 0) continue;
					if (target.isEmpty()) stacks.set(targetSlot, source.copyWithCount(moved)); else target.grow(moved);
					source.shrink(moved);
				}
			}
			input.setChanged();
		}
		setChanged();
	}

	@Override public EnergyStorage multiblockEnergyInput() { return energyStorage; }

	private void flushOutput() {
		ItemStack source = stacks.get(2);
		for (BlockPos pos : itemOutputs) {
			if (source.isEmpty()) break;
			if (!(level.getBlockEntity(pos) instanceof MultiblockItemOutputBlockEntity output)) continue;
			for (int slot = 0; slot < output.getContainerSize() && !source.isEmpty(); slot++) {
				ItemStack target = output.getItem(slot);
				if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, source)) continue;
				int room = target.isEmpty() ? source.getMaxStackSize() : target.getMaxStackSize() - target.getCount();
				int moved = Math.min(room, source.getCount());
				if (moved <= 0) continue;
				if (target.isEmpty()) output.setItem(slot, source.copyWithCount(moved)); else target.grow(moved);
				source.shrink(moved);
			}
			output.setChanged();
		}
		if (source.isEmpty()) stacks.set(2, ItemStack.EMPTY);
		setChanged();
	}

	private void sync() {
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
	}
}
