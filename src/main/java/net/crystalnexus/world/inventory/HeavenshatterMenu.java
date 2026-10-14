package net.crystalnexus.world.inventory;

import net.crystalnexus.block.entity.HeavenshatterBeamControllerBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public final class HeavenshatterMenu extends AbstractContainerMenu {
    private final HeavenshatterBeamControllerBlockEntity controller;
    private final ContainerLevelAccess access;
    private long lastControls = Long.MIN_VALUE, lastSnapshot = Long.MIN_VALUE;
    private net.minecraft.nbt.CompoundTag sent;
    public net.minecraft.nbt.CompoundTag tracking = new net.minecraft.nbt.CompoundTag();
    public boolean acceptControls(long tick) {
        if (lastControls != Long.MIN_VALUE && tick - lastControls < 2) return false;
        lastControls = tick; return true;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (controller == null || !(controller.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        long tick = level.getGameTime();
        if (sent != null && tick - lastSnapshot < 4 && sent.getInt("stage") == controller.stage().ordinal()
            && sent.getBoolean("identified") == controller.trackingIdentified() && (sent.getInt("stability") >= 60) == controller.trackingStable()) return;
        var next = controller.trackingState();
        if (!next.equals(sent)) {
            for (var player : level.players()) if (player.containerMenu == this && stillValid(player))
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new net.crystalnexus.network.payload.S2C_CelestialTracking(containerId, next));
            sent = next;
        }
        lastSnapshot = tick;
    }

    public HeavenshatterMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data == null ? null : controllerAt(inventory, data.readBlockPos()));
    }

    public HeavenshatterMenu(int id, Inventory inventory, HeavenshatterBeamControllerBlockEntity controller) {
        super(CrystalnexusModMenus.HEAVENSHATTER.get(), id);
        this.controller = controller;
        access = ContainerLevelAccess.create(inventory.player.level(), controller == null ? BlockPos.ZERO : controller.getBlockPos());
    }

    private static HeavenshatterBeamControllerBlockEntity controllerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof HeavenshatterBeamControllerBlockEntity controller ? controller : null;
    }

    public HeavenshatterBeamControllerBlockEntity controller() { return controller; }
    @Override public boolean stillValid(Player player) {
        return controller != null && !controller.isRemoved()
            && stillValid(access, player, CrystalnexusModBlocks.HEAVENSHATTER_BEAM_CONTROLLER.get());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
