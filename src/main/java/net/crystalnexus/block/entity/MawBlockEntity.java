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

import net.crystalnexus.world.inventory.MawMenu;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class MawBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);

	public MawBlockEntity(BlockPos position, BlockState state) {
		super(CrystalnexusModBlockEntities.MAW.get(), position, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MawBlockEntity blockEntity) {
		if (level.isClientSide())
			return;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(pos).expandTowards(0, 0.25, 0))) {
            if (!BlockPos.containing(mob.getX(), mob.getY() - 0.05, mob.getZ()).equals(pos)
                    || mob.getY() < pos.getY() + 0.95 || mob.getY() > pos.getY() + 1.1) continue;
            if (!mob.isAlive()) {
                mob.discard();
            } else if (level.getGameTime() % 10 == 0) {
                mob.hurt(level.damageSources().cactus(), 4.0f);
            }
        }
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
		return Component.translatable("block.crystalnexus.maw");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new MawMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.crystalnexus.maw");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

    private boolean collecting;

    public ItemStack collect(ItemStack stack) {
        collecting = true;
        try { return ItemHandlerHelper.insertItemStacked(new InvWrapper(this), stack, false); }
        finally { collecting = false; }
    }

    @Override public boolean canPlaceItem(int index, ItemStack stack) { return collecting; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2}; }
    @Override public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) { return true; }
}
