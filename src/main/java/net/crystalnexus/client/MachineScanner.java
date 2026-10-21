package net.crystalnexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.client.gui.MachineGuiTheme;
import net.crystalnexus.item.CrystalWrenchItem;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import net.crystalnexus.network.MachineInspectionNetwork;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineInspection;
import net.crystalnexus.util.WrenchLinePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class MachineScanner {
    private MachineScanner() {}
    private static BlockPos target;
    private static LUNADroneEntity droneTarget;
    private static ResourceLocation dimension, block;
    private static long tick, lastRequest = -5, sequence, accepted = -1, received;
    private static MachineInspection snapshot;
    private static Object currentLevel;
    private static Vector3f projected;

    private static Object aimed(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return null;
        boolean wrench = false;
        for (var stack : new net.minecraft.world.item.ItemStack[]{mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
            if (!(stack.getItem() instanceof CrystalWrenchItem)) continue;
            wrench = true;
            if (!net.crystalnexus.util.ConveyorPlacement.preview(stack).isEmpty()) return null;
            var preview = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompound(WrenchLinePlacement.PREVIEW_KEY);
            if (stack == mc.player.getMainHandItem() && mc.player.getOffhandItem().getItem() instanceof net.minecraft.world.item.BlockItem item
                    && !preview.isEmpty() && preview.getString("dimension").equals(mc.level.dimension().location().toString())
                    && preview.getString("block").equals(BuiltInRegistries.BLOCK.getKey(item.getBlock()).toString())) return null;
        }
        if (!wrench) return null;
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LUNADroneEntity drone)
            return drone.isAlive() && mc.player.getEyePosition().distanceTo(hit.getLocation()) < mc.player.entityInteractionRange() ? drone : null;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        var pos = hit.getBlockPos();
        if (mc.player.getEyePosition().distanceTo(hit.getLocation()) > mc.player.blockInteractionRange() || !mc.level.hasChunkAt(pos)) return null;
        return MachineInspection.canInspect(mc.level, pos) ? pos : null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        tick++;
        MachineInspectionNetwork.receiver = MachineScanner::receive;
        MachineInspectionNetwork.droneReceiver = MachineScanner::receive;
        var mc = Minecraft.getInstance();
        Object next = aimed(mc);
        if (currentLevel != mc.level) { currentLevel = mc.level; clear(); }
        if (next == null) { clear(); return; }
        var nextDimension = mc.level.dimension().location();
        if (next instanceof LUNADroneEntity drone) {
            if (drone != droneTarget || !nextDimension.equals(dimension)) {
                clear(); droneTarget = drone; dimension = nextDimension;
            }
        } else {
            var pos = (BlockPos) next;
            var nextBlock = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock());
            if (!pos.equals(target) || !nextDimension.equals(dimension) || !nextBlock.equals(block)) {
                clear(); target = pos.immutable(); dimension = nextDimension; block = nextBlock;
            }
        }
        if (!MachineInspectionNetwork.fresh(tick, received)) snapshot = null;
        if (tick - lastRequest >= 5) {
            lastRequest = tick;
            if (droneTarget != null)
                PacketDistributor.sendToServer(new MachineInspectionNetwork.DroneRequest(dimension, droneTarget.getId(), droneTarget.getUUID(), ++sequence));
            else PacketDistributor.sendToServer(new MachineInspectionNetwork.Request(dimension, target, ++sequence));
        }
    }
    private static void clear() { target = null; droneTarget = null; dimension = null; block = null; snapshot = null; projected = null; accepted = sequence; }
    private static void receive(MachineInspectionNetwork.Response response) {
        if (target == null || !response.matches(dimension, target, block, accepted, sequence)) return;
        accepted = response.sequence(); received = tick; snapshot = response.snapshot();
    }
    private static void receive(MachineInspectionNetwork.DroneResponse response) {
        var mc = Minecraft.getInstance();
        if (droneTarget == null || !droneTarget.isAlive() || mc.level == null || mc.level != currentLevel
                || mc.level.getEntity(droneTarget.getId()) != droneTarget
                || aimed(mc) != droneTarget
                || !response.matches(dimension, droneTarget.getId(), droneTarget.getUUID(), accepted, sequence)) return;
        accepted = response.sequence(); received = tick; snapshot = response.snapshot();
    }
    public static boolean showingDrone(LUNADroneEntity drone) {
        var mc = Minecraft.getInstance();
        return drone == droneTarget && snapshot != null && MachineInspectionNetwork.fresh(tick, received)
            && mc.level == currentLevel && aimed(mc) == drone;
    }
    @SubscribeEvent public static void projectTarget(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        projected = null;
        if (snapshot == null || !MachineInspectionNetwork.fresh(tick, received)) return;
        var mc = Minecraft.getInstance();
        Object next = aimed(mc);
        if (droneTarget != null ? next != droneTarget : target == null || !target.equals(next)) return;
        if (mc.level != currentLevel) return;
        Vec3 anchor = droneTarget == null ? Vec3.atBottomCenterOf(target).add(0, 1.35, 0)
            : droneTarget.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false))
                .add(0, droneTarget.getBoundingBox().maxY - droneTarget.getY() + .35, 0);
        var relative = anchor.subtract(event.getCamera().getPosition());
        projected = project(event.getProjectionMatrix(), event.getModelViewMatrix(),
            new Vector3f((float)relative.x, (float)relative.y, (float)relative.z),
            mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
    static Vector3f project(Matrix4f projection, Matrix4f view, Vector3f relative, int width, int height) {
        var clip = new Matrix4f(projection).mul(view).transform(new Vector4f(relative, 1));
        if (clip.w <= 0 || clip.z < -clip.w || clip.z > clip.w) return null;
        return new Vector3f((clip.x / clip.w + 1) * width / 2,
            (1 - clip.y / clip.w) * height / 2, projection.m11() * height / (2 * clip.w));
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        if (projected == null || snapshot == null || !MachineInspectionNetwork.fresh(tick, received)) return;
        var mc = Minecraft.getInstance();
        Object next = aimed(mc);
        if (mc.level != currentLevel || (droneTarget != null ? next != droneTarget : target == null || !target.equals(next))) return;
        var g = event.getGuiGraphics();
        var pose = g.pose();
        int height = height(snapshot), color = MachineGuiTheme.accent(snapshot.tier());
        float scale = Math.min(.01f, 1.6f / height) * projected.z;
        // Render after world depth is cleared so every part of the popup stays in front of blocks.
        g.flush();
        pose.pushPose();
        pose.translate(projected.x, projected.y, 400);
        pose.scale(scale, scale, 1);
        pose.translate(-110, -height, 0);
        try {
            MachineGuiTheme.frame(g, 0, 0, 220, height, color);
            g.fill(109, height, 111, height + 25, 0xCC000000 | color);
            g.fill(104, height + 25, 116, height + 27, 0xCC000000 | color);
            draw(g, snapshot, color);
            g.flush();
        } finally {
            pose.popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
    private static int height(MachineInspection s) {
        return 48 + (s.wrenchStatus().isEmpty() ? 0 : s.wrenchStatus().stream().mapToInt(line -> wrappedStatus(line).size() * 12).sum() + 2) + (s.energy() ? 24 : 0) + (s.progress() >= 0 ? 22 : 0)
            + (s.inventory() ? 16 + Math.max(1, (s.items().size() + 3) / 4) * 24 + (s.extraItems() > 0 ? 12 : 0) : 0)
            + (s.fluids() ? 14 + s.tanks().size() * 30 + (s.extraTanks() > 0 ? 12 : 0) : 0);
    }
    private static List<FormattedCharSequence> wrappedStatus(Component text) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(text, 204);
        if (lines.size() <= 3) return lines;
        // Keep long runtime errors readable without letting a popup grow indefinitely.
        var last = new StringBuilder();
        lines.get(2).accept((index, style, codePoint) -> { last.appendCodePoint(codePoint); return true; });
        return List.of(lines.get(0), lines.get(1), Component.literal(
            font.plainSubstrByWidth(last.toString(), 204 - font.width("...")) + "...").getVisualOrderText());
    }
    private static void text(GuiGraphics g, Component text, int y, int color) {
        var font = Minecraft.getInstance().font;
        font.drawInBatch(font.plainSubstrByWidth(text.getString(), 204), 8, y, color, false,
            g.pose().last().pose(), g.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
    }
    private static void draw(GuiGraphics g, MachineInspection s, int color) {
        var font = Minecraft.getInstance().font;
        text(g, s.name(), 8, 0xEFF5FF);
        text(g, s.tier() < 0 ? Component.translatable(droneTarget != null ? "gui.crystalnexus.scanner.drone"
            : !s.energy() && !s.inventory() && !s.fluids() && s.progress() < 0 ? "gui.crystalnexus.scanner.wrench" : "gui.crystalnexus.scanner.local") : Component.translatable(
            "gui.crystalnexus.machine.material_tier", s.tier() + 1, MachineTier.forLevel(s.tier()).displayName()), 20, color);
        int y = 36;
        for (var line : s.wrenchStatus()) {
            for (var wrapped : wrappedStatus(line)) {
                font.drawInBatch(wrapped, 8, y, 0x97A9BB, false, g.pose().last().pose(),
                    g.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                y += 12;
            }
        }
        if (!s.wrenchStatus().isEmpty()) y += 2;
        if (s.energy()) {
            text(g, Component.translatable("gui.crystalnexus.scanner.energy", s.storedEnergy(), s.energyCapacity()), y, 0xD5E0ED);
            MachineGuiTheme.bar(g, 8, y + 11, 204, 7, MachineInspection.fraction(s.storedEnergy(), s.energyCapacity()), color); y += 24;
        }
        if (s.progress() >= 0) {
            text(g, Component.translatable("gui.crystalnexus.scanner.progress", (int)(s.progress() * 100)), y, 0xD5E0ED);
            MachineGuiTheme.bar(g, 8, y + 11, 204, 6, s.progress(), color); y += 22;
        }
        if (s.inventory()) {
            text(g, Component.translatable("gui.crystalnexus.scanner.items"), y, color); y += 14;
            if (s.items().isEmpty()) text(g, Component.translatable("gui.crystalnexus.scanner.empty"), y + 4, 0x97A9BB);
            for (int i = 0; i < s.items().size(); i++) {
                var item = s.items().get(i); int x = 8 + (i % 4) * 51, row = y + (i / 4) * 24;
                g.renderItem(item.icon(), x, row);
                g.pose().pushPose();
                g.pose().translate(x + 18, row + 5, 200);
                float countScale = Math.min(.75f, 30f / Math.max(1, font.width(Integer.toString(item.count()))));
                g.pose().scale(countScale, countScale, 1);
                font.drawInBatch(Integer.toString(item.count()), 0, 0, 0xEFF5FF, false,
                    g.pose().last().pose(), g.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
                g.pose().popPose();
            }
            y += Math.max(1, (s.items().size() + 3) / 4) * 24;
            if (s.extraItems() > 0) { text(g, Component.translatable("gui.crystalnexus.scanner.more_items", s.extraItems()), y, 0x97A9BB); y += 12; }
        }
        if (s.fluids()) {
            text(g, Component.translatable("gui.crystalnexus.scanner.fluids"), y, color); y += 14;
            for (var tank : s.tanks()) {
                text(g, tank.name(), y, 0xD5E0ED);
                text(g, Component.translatable("gui.crystalnexus.scanner.tank", tank.amount(), tank.capacity()), y + 10, 0x97A9BB);
                MachineGuiTheme.bar(g, 8, y + 21, 204, 5, MachineInspection.fraction(tank.amount(), tank.capacity()), color); y += 30;
            }
            if (s.extraTanks() > 0) text(g, Component.translatable("gui.crystalnexus.scanner.more_tanks", s.extraTanks()), y, 0x97A9BB);
        }
        int sweep = (int)((System.currentTimeMillis() / 60) % 204);
        g.fill(8 + sweep, 31, Math.min(212, 22 + sweep), 32, 0xAA000000 | color);
    }
}
