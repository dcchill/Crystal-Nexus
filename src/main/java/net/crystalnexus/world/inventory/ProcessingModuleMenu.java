package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.block.entity.ProcessingModuleBlockEntity;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ProcessingModuleMenu extends AbstractContainerMenu {
    public static final int MATERIAL_X = 44;
    public static final int WORK_SLOT_Y = 36;
    private final ProcessingModuleBlockEntity module;
    private final ContainerData data;
    private final BlockPos pos;
    private final boolean materialSlot;

    public ProcessingModuleMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, findModule(inventory, buffer.readBlockPos()), new SimpleContainerData(2));
    }

    public ProcessingModuleMenu(int id, Inventory inventory, ProcessingModuleBlockEntity module, ContainerData data) {
        super(CrystalnexusModMenus.PROCESSING_MODULE.get(), id);
        this.module = module;
        this.data = data;
        this.pos = module == null ? BlockPos.ZERO : module.getBlockPos();
        this.materialSlot = module != null && module.hasMaterialSlot();
        if (materialSlot) addSlot(new Slot(module, 0, MATERIAL_X, WORK_SLOT_Y));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }

    private static ProcessingModuleBlockEntity findModule(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof ProcessingModuleBlockEntity module ? module : null;
    }

    public boolean hasMaterialSlot() { return materialSlot; }
    public int beltItemX() { return materialSlot ? 116 : 80; }
    public BlockPos guiPosition() { return pos; }
    public String machineFallbackId() {
        return module == null ? "processing_press_module"
            : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(module.getBlockState().getBlock()).getPath();
    }
    public int progress() { return data.get(0); }
    public int maxProgress() { return Math.max(1, data.get(1)); }
    public ItemStack heldItem() {
        if (module == null || module.getLevel() == null || !(module.getLevel().getBlockEntity(pos.below()) instanceof ConveyerBeltBaseBlockEntity belt))
            return ItemStack.EMPTY;
        ItemStack processing = belt.getSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT);
        if (!processing.isEmpty()) return processing.copy();
        for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++) if (!belt.getSegment(i).isEmpty()) return belt.getSegment(i).copy();
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) { return module != null && module.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (module == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = materialSlot ? 1 : 0;
        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (materialSlot && !moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }
}
