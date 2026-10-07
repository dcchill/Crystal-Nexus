package net.crystalnexus.world.inventory;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import net.crystalnexus.procedures.ExtractinatorGuiThisGUIIsOpenedProcedure;
import net.crystalnexus.network.ExtractinatorGuiSlotMessage;
import net.crystalnexus.init.CrystalnexusModMenus;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class ExtractinatorGuiMenu extends TieredMachineMenu implements CrystalnexusModMenus.MenuAccessor {
    private final int machineSlots;
	public final Map<String, Object> menuState = new HashMap<>() {
		@Override
		public Object put(String key, Object value) {
			if (!this.containsKey(key) && this.size() >= 15)
				return null;
			return super.put(key, value);
		}
	};
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;

	public ExtractinatorGuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(CrystalnexusModMenus.EXTRACTINATOR_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(13);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
		if (pos != null) {
			if (extraData.readableBytes() == 1) { 
				byte hand = extraData.readByte();
				ItemStack itemstack = hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem();
				this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem());
				IItemHandler cap = itemstack.getCapability(Capabilities.ItemHandler.ITEM);
				if (cap != null) {
					this.internal = cap;
					this.bound = true;
				}
			} else if (extraData.readableBytes() > 1) { 
				extraData.readByte(); 
				boundEntity = world.getEntity(extraData.readVarInt());
				if (boundEntity != null) {
					IItemHandler cap = boundEntity.getCapability(Capabilities.ItemHandler.ENTITY);
					if (cap != null) {
						this.internal = cap;
						this.bound = true;
					}
				}
			} else { 
				boundBlockEntity = this.world.getBlockEntity(pos);
				if (boundBlockEntity instanceof BaseContainerBlockEntity baseContainerBlockEntity) {
					this.internal = new MachineItemHandler(baseContainerBlockEntity);
					this.bound = true;
				}
			}
		}
		this.customSlots.put(0, this.addSlot(new MachineItemSlot(internal, 0, 79, 17) {
			private final int slot = 0;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(0, 1, stack.getCount());
			}
		}));
		this.customSlots.put(1, this.addSlot(new MachineItemSlot(internal, 1, 61, 44) {
			private final int slot = 1;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		var upgradeState = world.getBlockState(net.minecraft.core.BlockPos.containing(x, y, z));
        this.customSlots.put(7, this.addSlot(new MachineUpgradeSlot(internal, 7, upgradeState, 0)));
		this.customSlots.put(2, this.addSlot(new MachineItemSlot(internal, 2, 79, 44) {
			private final int slot = 2;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(3, this.addSlot(new MachineItemSlot(internal, 3, 97, 44) {
			private final int slot = 3;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(4, this.addSlot(new MachineItemSlot(internal, 4, 61, 62) {
			private final int slot = 4;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(5, this.addSlot(new MachineItemSlot(internal, 5, 79, 62) {
			private final int slot = 5;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
		this.customSlots.put(6, this.addSlot(new MachineItemSlot(internal, 6, 97, 62) {
			private final int slot = 6;
			private int x = ExtractinatorGuiMenu.this.x;
			private int y = ExtractinatorGuiMenu.this.y;

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		}));
        for (int i = 1; i < net.crystalnexus.processing.MachineTier.from(upgradeState).upgradeSlots(); i++)
            this.customSlots.put(8 + i - 1, this.addSlot(new MachineUpgradeSlot(internal, 8 + i - 1, upgradeState, i)));
        machineSlots = slots.size();
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, 0 + 8 + sj * 18, 0 + 84 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, 0 + 8 + si * 18, 0 + 142));
		ExtractinatorGuiThisGUIIsOpenedProcedure.execute(world, x, y, z);
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = (Slot) this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < machineSlots) {
				if (!this.moveItemStackTo(itemstack1, machineSlots, this.slots.size(), true))
					return ItemStack.EMPTY;
				slot.onQuickCraft(itemstack1, itemstack);
			} else if (net.crystalnexus.util.MachineUpgradeHelper.isMachineUpgrade(itemstack1)) {
                boolean moved = false;
                for (int i = 0; i < machineSlots && !itemstack1.isEmpty(); i++)
                    if (slots.get(i) instanceof MachineUpgradeSlot) moved |= moveItemStackTo(itemstack1, i, i + 1, false);
                if (!moved) return ItemStack.EMPTY;
			} else if (!this.moveItemStackTo(itemstack1, 0, machineSlots, false)) {
				if (index < machineSlots + 27) {
					if (!this.moveItemStackTo(itemstack1, machineSlots + 27, this.slots.size(), true))
						return ItemStack.EMPTY;
				} else {
					if (!this.moveItemStackTo(itemstack1, machineSlots, machineSlots + 27, false))
						return ItemStack.EMPTY;
				}
				return ItemStack.EMPTY;
			}
			if (itemstack1.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(playerIn, itemstack1);
		}
		return itemstack;
	}


	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
			if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
				for (int j = 0; j < internal.getSlots(); ++j) {
					playerIn.drop(internal.getStackInSlot(j), false);
					if (internal instanceof IItemHandlerModifiable ihm)
						ihm.setStackInSlot(j, ItemStack.EMPTY);
				}
			} else {
				for (int i = 0; i < internal.getSlots(); ++i) {
					playerIn.getInventory().placeItemBackInInventory(internal.getStackInSlot(i));
					if (internal instanceof IItemHandlerModifiable ihm)
						ihm.setStackInSlot(i, ItemStack.EMPTY);
				}
			}
		}
	}

	private void slotChanged(int slotid, int ctype, int meta) {
		if (this.world != null && this.world.isClientSide()) {
			PacketDistributor.sendToServer(new ExtractinatorGuiSlotMessage(slotid, x, y, z, ctype, meta));
			ExtractinatorGuiSlotMessage.handleSlotAction(entity, slotid, ctype, meta, x, y, z);
		}
	}

	@Override
	public Map<Integer, Slot> getSlots() {
		return Collections.unmodifiableMap(customSlots);
	}

	@Override
	public Map<String, Object> getMenuState() {
		return menuState;
	}
}