package net.crystalnexus.item;

import net.crystalnexus.data.SummonedMeteorSavedData;
import net.crystalnexus.multiblock.RecordedMeteor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.Map;
import java.util.WeakHashMap;

public final class MeteorScannerItem extends Item {
    public static final int SCAN_TICKS = 100, SCAN_COST = 5000, RANGE = 56;
    private record Scan(RecordedMeteor meteor, ItemStack scanner, ItemStack disk, ItemStack originalDisk) {}
    private final Map<Player, Scan> scans = new WeakHashMap<>();

    public MeteorScannerItem() { super(new Properties().stacksTo(1)); }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return ToolEnergy.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return 0x00FF00; }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return SCAN_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    private static RecordedMeteor target(ServerLevel level, Player player) {
        var start = player.getEyePosition();
        var hit = level.clip(new ClipContext(start, start.add(player.getLookAngle().scale(RANGE)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? SummonedMeteorSavedData.get(level).at(level, hit.getBlockPos()) : null;
    }

    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof net.minecraft.server.level.ServerPlayer player)
            || (player.getMainHandItem() != stack && player.getOffhandItem() != stack) || player.tickCount % 2 != 0) return;
        RecordedMeteor meteor = target(server, player);
        Scan scan = scans.get(player);
        int elapsed = scan != null && player.isUsingItem() && player.getUseItem() == scan.scanner()
            && meteor != null && meteor.id().equals(scan.meteor().id()) ? player.getTicksUsingItem() : -1;
        if (meteor == null) meteor = SummonedMeteorSavedData.get(server).nearest(server, player.getEyePosition(), RANGE);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new net.crystalnexus.network.payload.S2C_MeteorScannerTarget(
                meteor == null ? net.minecraft.core.BlockPos.ZERO : meteor.base(), meteor == null ? 0 : meteor.diameter(), elapsed));
    }

    private static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.crystalnexus.meteor_scanner." + key, args), true);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack scanner = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(scanner);
        if (level instanceof ServerLevel server) {
            scans.remove(player);
            ItemStack disk = player.getOffhandItem();
            if (!(disk.getItem() instanceof MeteorDataDiskItem)) {
                message(player, "disk_required");
                return InteractionResultHolder.fail(scanner);
            }
            RecordedMeteor meteor = target(server, player);
            if (meteor == null) {
                message(player, "target_required");
                return InteractionResultHolder.fail(scanner);
            }
            if (!ToolEnergy.consume(player, scanner, SCAN_COST, true)) {
                message(player, "no_energy", SCAN_COST);
                return InteractionResultHolder.fail(scanner);
            }
            scans.put(player, new Scan(meteor, scanner, disk, disk.copy()));
            message(player, "progress", 0);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(scanner);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        Scan scan = scans.get(player);
        if (scan == null) { player.stopUsingItem(); return; }
        RecordedMeteor target = target(server, player);
        if (player.getMainHandItem() != scan.scanner() || stack != scan.scanner()
            || player.getOffhandItem() != scan.disk() || !ItemStack.matches(scan.disk(), scan.originalDisk())
            || target == null || !target.id().equals(scan.meteor().id())) {
            cancel(player, "interrupted");
            return;
        }
        if (!ToolEnergy.consume(player, stack, SCAN_COST, true)) {
            cancel(player, "no_energy", SCAN_COST);
            return;
        }
        int elapsed = getUseDuration(stack, entity) - remaining + 1;
        if (elapsed >= SCAN_TICKS) {
            if (!ToolEnergy.consume(player, stack, SCAN_COST, false)) { cancel(player, "no_energy", SCAN_COST); return; }
            MeteorDataDiskItem.write(scan.disk(), scan.meteor());
            scans.remove(player);
            player.stopUsingItem();
            player.getInventory().setChanged();
            message(player, "complete");
        } else if (elapsed % 10 == 0) message(player, "progress", elapsed);
    }

    private void cancel(Player player, String key, Object... args) {
        scans.remove(player);
        player.stopUsingItem();
        message(player, key, args);
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide && entity instanceof Player player && scans.remove(player) != null) message(player, "interrupted");
    }

}
