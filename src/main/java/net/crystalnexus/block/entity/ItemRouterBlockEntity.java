package net.crystalnexus.block.entity;

import net.crystalnexus.block.ItemRouterBlock;
import net.crystalnexus.init.CrystalnexusModBlockEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ItemRouterBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public enum Mode {
        ROUND_ROBIN, PRIORITY_FRONT, PRIORITY_LEFT, PRIORITY_RIGHT;

        public Component displayName() { return Component.translatable("gui.crystalnexus.router.mode." + name().toLowerCase(java.util.Locale.ROOT)); }

        public int port(int attempt, int nextPort) {
            if (this == ROUND_ROBIN) return (nextPort + attempt) % 3;
            int preferred = ordinal() - 1;
            if (attempt == 0) return preferred;
            int fallback = attempt - 1;
            return fallback >= preferred ? fallback + 1 : fallback;
        }
    }

    private NonNullList<ItemStack> stacks;
    private Mode mode = Mode.ROUND_ROBIN;
    private int nextPort;

    public ItemRouterBlockEntity(BlockPos pos, BlockState state) {
        super((((ItemRouterBlock) state.getBlock()).isMerger() ? CrystalnexusModBlockEntities.MERGER : CrystalnexusModBlockEntities.SPLITTER).get(), pos, state);
        stacks = NonNullList.withSize(isMerger() ? 3 : 1, ItemStack.EMPTY);
    }

    public boolean isMerger() { return ((ItemRouterBlock) getBlockState().getBlock()).isMerger(); }
    public Mode mode() { return mode; }
    public Mode cycleMode() {
        if (isMerger()) return Mode.ROUND_ROBIN;
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        nextPort = 0;
        setChanged();
        return mode;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(ItemRouterBlock.FACING);
        for (int attempt = 0; attempt < 3; attempt++) {
            int port = mode.port(attempt, nextPort);
            int slot = isMerger() ? port : 0;
            ItemStack source = getItem(slot);
            if (source.isEmpty()) continue;
            Direction output = isMerger() ? facing.getOpposite() : ItemRouterBlock.redPort(facing, port);
            BlockPos target = worldPosition.relative(output);
            if (!level.hasChunkAt(target)) continue;
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, output.getOpposite());
            if (handler == null) continue;
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, source.copy(), false);
            int moved = source.getCount() - remainder.getCount();
            if (moved <= 0) continue;
            removeItem(slot, moved);
            nextPort = (port + 1) % 3;
            setChanged();
            return;
        }
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, lookup);
        int savedMode = tag.getInt("RoutingMode");
        mode = !isMerger() && savedMode >= 0 && savedMode < Mode.values().length ? Mode.values()[savedMode] : Mode.ROUND_ROBIN;
        nextPort = Math.floorMod(tag.getInt("NextPort"), 3);
    }
    @Override public void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, stacks, lookup);
        if (!isMerger()) tag.putInt("RoutingMode", mode.ordinal());
        tag.putInt("NextPort", nextPort);
    }

    @Override public int getContainerSize() { return isMerger() ? 3 : 1; }
    @Override protected NonNullList<ItemStack> getItems() { return stacks; }
    @Override protected void setItems(NonNullList<ItemStack> items) { stacks = items; }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return null; }

    @Override public int[] getSlotsForFace(Direction side) {
        Direction facing = getBlockState().getValue(ItemRouterBlock.FACING);
        if (!isMerger()) return side == facing.getOpposite() ? new int[]{0} : new int[0];
        for (int port = 0; port < 3; port++) if (side == ItemRouterBlock.redPort(facing, port)) return new int[]{port};
        return new int[0];
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        int[] slots = getSlotsForFace(side);
        return slots.length == 1 && slots[0] == slot;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
}
