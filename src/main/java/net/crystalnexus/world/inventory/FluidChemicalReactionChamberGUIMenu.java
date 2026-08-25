package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.minecraft.network.FriendlyByteBuf;

public class FluidChemicalReactionChamberGUIMenu extends AbstractContainerMenu {
    public final Player entity;
    public final int x, y, z;
    private final ContainerLevelAccess access;
    private final FluidChemicalReactionChamberBlockEntity chamber;

    public FluidChemicalReactionChamberGUIMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        super(CrystalnexusModMenus.FLUID_CHEMICAL_REACTION_CHAMBER_GUI.get(), id);
        entity = inventory.player;
        BlockPos pos = data.readBlockPos();
        x = pos.getX(); y = pos.getY(); z = pos.getZ();
        access = ContainerLevelAccess.create(entity.level(), pos);
        chamber = entity.level().getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity be ? be : null;
        InvWrapper items = new InvWrapper(chamber == null ? new net.minecraft.world.SimpleContainer(4) : chamber);

        addSlot(new SlotItemHandler(items, 0, 37, 54));
        addSlot(new SlotItemHandler(items, 1, 61, 54));
        addSlot(new SlotItemHandler(items, 2, 124, 54));
        addSlot(new SlotItemHandler(items, 3, 180, 8) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    public FluidChemicalReactionChamberBlockEntity chamber() { return chamber; }

    @Override public boolean stillValid(Player player) {
        return chamber != null && AbstractContainerMenu.stillValid(access, player, chamber.getBlockState().getBlock());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 4) {
            if (!moveItemStackTo(original, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 4, false)) {
            int inventoryEnd = 4 + 27;
            if (index < inventoryEnd ? !moveItemStackTo(original, inventoryEnd, slots.size(), true)
                                     : !moveItemStackTo(original, 4, inventoryEnd, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }
}
