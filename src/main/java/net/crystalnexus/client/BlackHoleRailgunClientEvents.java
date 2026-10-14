package net.crystalnexus.client;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.BlackHoleRpgItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public final class BlackHoleRailgunClientEvents {
    private static final List<Particle> CHARGE_SPARKS = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof BlackHoleRpgItem)) {
            CHARGE_SPARKS.forEach(Particle::remove);
            CHARGE_SPARKS.clear();
            return;
        }
        boolean scoped = isScoped();
        if (scoped) {
            CHARGE_SPARKS.forEach(Particle::remove);
            CHARGE_SPARKS.clear();
        }
        CHARGE_SPARKS.removeIf(particle -> !particle.isAlive());
        if (player.getTicksUsingItem() % 4 != 0) return;
        float charge = BlackHoleRpgItem.charge(player.getTicksUsingItem());
        int handSide = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (player.getUsedItemHand() == InteractionHand.OFF_HAND) handSide = -handSide;
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6 + player.getTicksUsingItem() * 0.2;
            Vec3 offset = scoped ? chargeScopeOrbitOffset(player.getLookAngle(), charge, angle)
                    : chargeParticleOffset(player.getLookAngle(), charge, angle, handSide, firstPerson);
            Vec3 point = player.getEyePosition().add(offset);
            var particle = minecraft.particleEngine.createParticle(ParticleTypes.END_ROD,
                    point.x, point.y, point.z, 0, 0, 0);
            if (particle != null) {
                particle.scale(0.3f);
                particle.setLifetime(8);
                CHARGE_SPARKS.add(particle);
            }
        }
    }

    private static Vec3 chargeScopeOrbitOffset(Vec3 forward, float charge, double angle) {
        Vec3 axis = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = forward.cross(axis).normalize();
        Vec3 up = side.cross(forward).normalize();
        double radius = 0.22 + charge * 0.28;
        return forward.scale(4).add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }

    static Vec3 chargeParticleOffset(Vec3 forward, float charge, double angle, int handSide, boolean firstPerson) {
        Vec3 axis = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = forward.cross(axis).normalize();
        Vec3 up = side.cross(forward).normalize();
        double radius = 0.08 + charge * 0.08;
        double screenSide = firstPerson ? handSide * 0.32 : -handSide * 0.32;
        double belowSightline = firstPerson ? -0.05 : -0.75;
        return forward.scale(0.95).add(side.scale(screenSide + handSide * Math.cos(angle) * radius))
                .add(up.scale(belowSightline + Math.sin(angle) * radius));
    }

    private static boolean holdingRailgun() {
        var minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.screen == null && !minecraft.player.isSpectator()
                && minecraft.player.getMainHandItem().is(CrystalnexusModItems.BLACK_HOLE_RPG.get());
    }

    public static boolean isScoped() {
        var minecraft = Minecraft.getInstance();
        return holdingRailgun() && minecraft.isWindowActive() && minecraft.options.getCameraType().isFirstPerson()
                && minecraft.options.keyAttack.isDown();
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!holdingRailgun()) return;
        if (event.isAttack()) event.setCanceled(true);
        if (event.isAttack() || event.isUseItem()) event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (isScoped() && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (isScoped() && event.usedConfiguredFov()) {
            event.setFOV(Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(event.getFOV()) / 2) / 4)));
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isScoped()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!isScoped()) {
            renderCharge(event);
            return;
        }
        var graphics = event.getGuiGraphics();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int cx = width / 2, cy = height / 2, radius = Math.min(width, height) * 2 / 5;
        for (int y = 0; y < height; y++) {
            int dy = y - cy;
            int half = Math.abs(dy) < radius ? (int) Math.sqrt(radius * radius - dy * dy) : 0;
            graphics.fill(0, y, cx - half, y + 1, 0xFF080510);
            graphics.fill(cx + half, y, width, y + 1, 0xFF080510);
        }
        int color = 0xFFC080FF;
        graphics.fill(cx - radius, cy, cx - 5, cy + 1, color);
        graphics.fill(cx + 5, cy, cx + radius, cy + 1, color);
        graphics.fill(cx, cy - radius, cx + 1, cy - 5, color);
        graphics.fill(cx, cy + 5, cx + 1, cy + radius, color);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
        for (int i = -3; i <= 3; i++) {
            if (i == 0) continue;
            int offset = i * radius / 5;
            graphics.fill(cx + offset, cy - 3, cx + offset + 1, cy + 4, color);
            graphics.fill(cx - 3, cy + offset, cx + 4, cy + offset + 1, color);
        }
        renderCharge(event);
    }

    private static void renderCharge(RenderGuiEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof BlackHoleRpgItem)) return;
        float charge = BlackHoleRpgItem.charge(player.getTicksUsingItem());
        var graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2 - 50;
        int y = isScoped() ? graphics.guiHeight() * 3 / 4 : graphics.guiHeight() - 45;
        int color = charge >= 1 ? 0xFFFFFFFF : 0xFFC080FF;
        graphics.fill(x - 1, y - 1, x + 101, y + 7, 0xCC080510);
        graphics.fill(x, y, x + Math.round(charge * 100), y + 6, color);
        String label = charge >= 1 ? "FULL CHARGE - RELEASE" : "CHARGING " + Math.round(charge * 100) + "%";
        graphics.drawCenteredString(minecraft.font, label, x + 50, y - 12, color);
    }
}
