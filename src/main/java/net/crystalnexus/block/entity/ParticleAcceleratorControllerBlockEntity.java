package net.crystalnexus.block.entity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.AcceleratorGuiMenu;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.crystalnexus.procedures.ParticleAcceleratorControllerOnTickUpdateProcedure;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class ParticleAcceleratorControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private static final int[] OUTPUT_SLOTS = new int[] {1, 5, 6, 7};
	private static final int[] INPUT_SLOTS = new int[] {0, 2, 3, 4};
	private NonNullList<ItemStack> stacks = NonNullList.withSize(8, ItemStack.EMPTY);

	public ParticleAcceleratorControllerBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.PARTICLE_ACCELERATOR_CONTROLLER.get(), position, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ParticleAcceleratorControllerBlockEntity blockEntity) {
		if (level.isClientSide())
			return;
		ParticleAcceleratorControllerOnTickUpdateProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
	}

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
		return Component.literal("particle_accelerator_controller");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new AcceleratorGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Particle Accelerator Controller");
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
		return !isOutputSlot(index);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.of(INPUT_SLOTS).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return !isOutputSlot(index) && this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		return isOutputSlot(index);
	}

	private static boolean isOutputSlot(int index) {
		for (int slot : OUTPUT_SLOTS) if (slot == index) return true;
		return false;
	}
}
