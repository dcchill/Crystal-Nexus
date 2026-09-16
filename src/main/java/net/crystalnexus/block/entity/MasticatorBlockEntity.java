package net.crystalnexus.block.entity;

import net.crystalnexus.block.MasticatorBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.jei_recipes.GeneSplicingRecipe;
import net.crystalnexus.item.PrisonCubeItem;
import net.crystalnexus.world.inventory.MasticatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class MasticatorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private static final int PROCESSING_TICKS = (int) MachineTier.TITANIUM.processingTime(100);
	private static final int MAX_OUTPUT = 16;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);
	private int progress;
	private final ContainerData data = new ContainerData() {
		@Override public int get(int index) {
			return index == 0 ? progress : PROCESSING_TICKS;
		}

		@Override public void set(int index, int value) {
			if (index == 0) progress = value;
		}

		@Override public int getCount() { return 2; }
	};

	public MasticatorBlockEntity(BlockPos pos, BlockState state) {
		super(CrystalnexusModBlockEntities.MASTICATOR.get(), pos, state);
	}

	public ContainerData data() {
		return data;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MasticatorBlockEntity blockEntity) {
		if (level.isClientSide) {
			return;
		}

		ItemStack biomass = blockEntity.getItem(0);
		ItemStack input = blockEntity.getItem(1);
		GeneSplicingRecipe recipe = level.getRecipeManager().getAllRecipesFor(GeneSplicingRecipe.Type.INSTANCE).stream()
				.map(holder -> holder.value())
				.filter(candidate -> candidate.matches(biomass, input))
				.findFirst().orElse(null);
		ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
		ItemStack egg = recipe == null ? ItemStack.EMPTY : recipe.spawnEgg();
		boolean running = recipe != null && (!egg.isEmpty() || !result.isEmpty())
				&& outputFits(blockEntity.getItem(2), egg) && outputFits(blockEntity.getItem(3), result);

		setRunning(level, pos, state, running);
		if (!running) {
			if (blockEntity.progress != 0) {
				blockEntity.progress = 0;
				blockEntity.setChanged();
			}
			return;
		}

		boolean completes = MasticatorCycle.completes(blockEntity.progress, PROCESSING_TICKS);
		blockEntity.progress = MasticatorCycle.advance(blockEntity.progress, PROCESSING_TICKS);
		if (completes) {
			biomass.shrink(recipe.fuelCount());
			PrisonCubeItem.clearStoredEntity(input);
			blockEntity.addOutput(2, egg);
			blockEntity.addOutput(3, result);
		}
		blockEntity.setChanged();
	}

	private static boolean outputFits(ItemStack output, ItemStack result) {
		return result.isEmpty() || ((output.isEmpty() || ItemStack.isSameItemSameComponents(output, result))
				&& output.getCount() + Math.min(MAX_OUTPUT, result.getCount()) <= result.getMaxStackSize());
	}

	private void addOutput(int slot, ItemStack result) {
		if (result.isEmpty()) return;
		int count = Math.min(MAX_OUTPUT, result.getCount());
		if (getItem(slot).isEmpty()) setItem(slot, result.copyWithCount(count));
		else getItem(slot).grow(count);
	}

	private static void setRunning(Level level, BlockPos pos, BlockState state, boolean running) {
		int value = running ? 2 : 1;
		if (state.getValue(MasticatorBlock.BLOCKSTATE) != value) {
			level.setBlock(pos, state.setValue(MasticatorBlock.BLOCKSTATE, value), Block.UPDATE_ALL);
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.crystalnexus.gene_splicer");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new MasticatorMenu(id, inventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, stacks, registries);
		progress = tag.getInt("Progress");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, stacks, registries);
		tag.putInt("Progress", progress);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 ? stack.is(CrystalnexusModItems.BIOMASS.get()) : slot == 1;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 2 || slot == 3;
	}
}
