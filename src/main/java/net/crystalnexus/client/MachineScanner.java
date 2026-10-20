package net.crystalnexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.crystalnexus.client.gui.MachineGuiTheme;
import net.crystalnexus.item.CrystalWrenchItem;
import net.crystalnexus.network.MachineInspectionNetwork;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.util.MachineInspection;
import net.crystalnexus.util.WrenchLinePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
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
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class MachineScanner {
    private MachineScanner() {}
    private static BlockPos target;
    private static ResourceLocation dimension, block;
    private static long tick, lastRequest = -5, sequence, accepted = -1, received;
    private static MachineInspection snapshot;
    private static Object currentLevel;

    private static BlockPos aimed(Minecraft mc) {
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
        if (!wrench || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        var pos = hit.getBlockPos();
        if (mc.player.getEyePosition().distanceTo(hit.getLocation()) > mc.player.blockInteractionRange() || !mc.level.hasChunkAt(pos)) return null;
        return mc.level.getBlockEntity(pos) != null && BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock())
            .getNamespace().equals("crystalnexus") ? pos : null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        tick++;
        MachineInspectionNetwork.receiver = MachineScanner::receive;
        var mc = Minecraft.getInstance();
        BlockPos next = aimed(mc);
        if (currentLevel != mc.level) { currentLevel = mc.level; clear(); }
        if (next == null) { clear(); return; }
        var nextDimension = mc.level.dimension().location();
        var nextBlock = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(next).getBlock());
        if (!next.equals(target) || !nextDimension.equals(dimension) || !nextBlock.equals(block)) {
            clear(); target = next.immutable(); dimension = nextDimension; block = nextBlock;
        }
        if (!MachineInspectionNetwork.fresh(tick, received)) snapshot = null;
        if (tick - lastRequest >= 5) {
            lastRequest = tick;
            PacketDistributor.sendToServer(new MachineInspectionNetwork.Request(dimension, target, ++sequence));
        }
    }
    private static void clear() { target = null; dimension = null; block = null; snapshot = null; accepted = sequence; }
    private static void receive(MachineInspectionNetwork.Response response) {
        if (target == null || !response.matches(dimension, target, block, accepted, sequence)) return;
        accepted = response.sequence(); received = tick; snapshot = response.snapshot();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || snapshot == null || !MachineInspectionNetwork.fresh(tick, received)) return;
        var mc = Minecraft.getInstance();
        if (target == null || !target.equals(aimed(mc))) return;
        var camera = event.getCamera();
        var cameraPos = camera.getPosition();
        var g = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
            @Override public void fill(int x1, int y1, int x2, int y2, int color) {
                super.fill(PanelRenderType.PANEL, x1, y1, x2, y2, color);
            }
        };
        var pose = g.pose();
        pose.pushPose();
        pose.mulPose(event.getPoseStack().last().pose());
        pose.translate(target.getX() + .5 - cameraPos.x, target.getY() + 1.35 - cameraPos.y, target.getZ() + .5 - cameraPos.z);
        pose.mulPose(camera.rotation());
        int height = height(snapshot), color = MachineGuiTheme.accent(snapshot.tier());
        float scale = Math.min(.01f, 1.6f / height);
        pose.scale(scale, -scale, .00001f);
        pose.translate(-110, -height, 0);
        try {
            RenderSystem.enableDepthTest();
            MachineGuiTheme.frame(g, 0, 0, 220, height, color);
            g.fill(109, height, 111, height + 25, 0xCC000000 | color);
            g.fill(104, height + 25, 116, height + 27, 0xCC000000 | color);
            draw(g, snapshot, color);
            RenderSystem.disableDepthTest();
            g.flush();
        } finally {
            pose.popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            if (mc.level.effects().constantAmbientLight()) Lighting.setupNetherLevel(); else Lighting.setupLevel();
        }
    }
    private static int height(MachineInspection s) {
        return 48 + (s.energy() ? 24 : 0) + (s.progress() >= 0 ? 22 : 0)
            + (s.inventory() ? 16 + Math.max(1, (s.items().size() + 3) / 4) * 24 + (s.extraItems() > 0 ? 12 : 0) : 0)
            + (s.fluids() ? 14 + s.tanks().size() * 30 + (s.extraTanks() > 0 ? 12 : 0) : 0);
    }
    private static void text(GuiGraphics g, Component text, int y, int color) {
        var font = Minecraft.getInstance().font;
        font.drawInBatch(font.plainSubstrByWidth(text.getString(), 204), 8, y, color, false,
            g.pose().last().pose(), g.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
    }
    private static void draw(GuiGraphics g, MachineInspection s, int color) {
        var font = Minecraft.getInstance().font;
        text(g, s.name(), 8, 0xEFF5FF);
        text(g, s.tier() < 0 ? Component.translatable("gui.crystalnexus.scanner.local") : Component.translatable(
            "gui.crystalnexus.machine.material_tier", s.tier() + 1, MachineTier.forLevel(s.tier()).displayName()), 20, color);
        int y = 36;
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
    private static final class PanelRenderType extends RenderType {
        private PanelRenderType() { super("unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }
        private static final RenderType PANEL = create("crystalnexus_hologram", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false, CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    }
}
