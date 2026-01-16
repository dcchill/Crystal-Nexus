package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.item.ZeroPointCoreItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ZeroPointLaserRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof ZeroPointCoreItem)) return;

        renderBeam(event, player);
    }

    private static void renderBeam(RenderLevelStageEvent event, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        Vec3 look = player.getLookAngle();
        Vec3 startW = getBeamStartWorld(player, partialTick, look); // ✅ better in first person

        double range = 32.0;
        Vec3 endW = startW.add(look.scale(range));

        HitResult hit = player.level().clip(new ClipContext(
                startW, endW,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 hitW = hit.getLocation();

        // Camera-relative positions
        Vec3 camW = event.getCamera().getPosition();
        Vec3 start = startW.subtract(camW);
        Vec3 end = hitW.subtract(camW);

        Vector3f forward = new Vector3f(
                (float) (end.x - start.x),
                (float) (end.y - start.y),
                (float) (end.z - start.z)
        );
        if (forward.lengthSquared() < 1.0e-6f) return;
        forward.normalize();

        // Vector from beam -> camera (camera space origin is 0,0,0)
        Vector3f toCam = new Vector3f((float) -start.x, (float) -start.y, (float) -start.z);
        if (toCam.lengthSquared() < 1.0e-6f) toCam.set(0, 1, 0);
        toCam.normalize();

        // Camera-facing basis
        Vector3f side = new Vector3f(forward).cross(toCam);
        if (side.lengthSquared() < 1.0e-6f) side = new Vector3f(forward).cross(new Vector3f(0, 1, 0));
        side.normalize();
        Vector3f up = new Vector3f(side).cross(forward).normalize();

        Vector3f s = new Vector3f((float) start.x, (float) start.y, (float) start.z);
        Vector3f e = new Vector3f((float) end.x, (float) end.y, (float) end.z);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        Matrix4f mat = poseStack.last().pose();

        // ===== Look settings =====
        // Core + glow layers
        int r = 255, g = 0, b = 0;

        float coreW = 0.045f;
        float glowW = 0.090f;

        int coreA = 255;
        int glowA = 90;

        // Muzzle segment in first-person (thicker near camera)
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float muzzleLen = firstPerson ? 1.25f : 0.0f; // blocks
        float muzzleExtra = firstPerson ? 0.06f : 0.0f;

        // Compute muzzle end (short segment)
        Vector3f muzzleEnd = new Vector3f(e);
        if (muzzleLen > 0.0f) {
            Vector3f dir = new Vector3f(e).sub(s);
            float len = dir.length();
            if (len > 1.0e-4f) {
                dir.div(len);
                float seg = Math.min(muzzleLen, len);
                muzzleEnd = new Vector3f(s).add(dir.mul(seg));
            }
        }

        // Draw muzzle (glow + core, fatter)
        if (muzzleLen > 0.0f) {
            drawBeamTube(vc, mat, s, muzzleEnd, side, up, glowW + muzzleExtra, r, g, b, glowA);
            drawBeamTube(vc, mat, s, muzzleEnd, side, up, coreW + muzzleExtra * 0.6f, r, g, b, coreA);
        }

        // Draw main beam (glow + core)
        drawBeamTube(vc, mat, s, e, side, up, glowW, r, g, b, glowA);
        drawBeamTube(vc, mat, s, e, side, up, coreW, r, g, b, coreA);

        buffer.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    /**
     * In first-person, offset the beam start so it looks like it comes from the held item.
     * In third-person, start at eye.
     */
private static Vec3 getBeamStartWorld(LocalPlayer player, float partialTick, Vec3 look) {
    Minecraft mc = Minecraft.getInstance();
    Vec3 eye = player.getEyePosition(partialTick);

    // Right vector from look
    Vec3 up = new Vec3(0, 1, 0);
    Vec3 right = look.cross(up);
    if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
    right = right.normalize();

    // Which side should the beam come from?
    // Main hand: based on main arm. Offhand: opposite.
    double mainHandSide = (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1.0 : -1.0;
    boolean usingOffhand = player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND;
    double sideSign = usingOffhand ? -mainHandSide : mainHandSide;

    // Strong offsets so it DOES NOT look like it starts at the face
    boolean firstPerson = mc.options.getCameraType().isFirstPerson();

    double side   = (firstPerson ? 0.65 : 0.35) * sideSign; // right/left
    double down   = (firstPerson ? -0.28 : -0.15);          // lower a bit
    double forward= (firstPerson ? 0.85 : 0.40);            // push out

    return eye
            .add(right.scale(side))
            .add(0, down, 0)
            .add(look.scale(forward));
}



    /** Draws a “tube-ish” beam using two crossed ribbons (billboarded) */
    private static void drawBeamTube(VertexConsumer vc, Matrix4f mat,
                                     Vector3f s, Vector3f e,
                                     Vector3f side, Vector3f up,
                                     float halfWidth,
                                     int r, int g, int b, int a) {

        Vector3f sideW = new Vector3f(side).mul(halfWidth);
        Vector3f upW = new Vector3f(up).mul(halfWidth);

        // Ribbon 1 (side)
        quad(vc, mat,
                new Vector3f(s).add(sideW),
                new Vector3f(s).sub(sideW),
                new Vector3f(e).sub(sideW),
                new Vector3f(e).add(sideW),
                r, g, b, a);

        // Ribbon 2 (up)
        quad(vc, mat,
                new Vector3f(s).add(upW),
                new Vector3f(s).sub(upW),
                new Vector3f(e).sub(upW),
                new Vector3f(e).add(upW),
                r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f mat, Vector3f p, int r, int g, int b, int a) {
        Vector4f t = new Vector4f(p.x, p.y, p.z, 1.0f).mul(mat);
        vc.addVertex(t.x, t.y, t.z).setColor(r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                             int r, int g, int bl, int al) {
        v(vc, mat, a, r, g, bl, al);
        v(vc, mat, b, r, g, bl, al);
        v(vc, mat, c, r, g, bl, al);
        v(vc, mat, d, r, g, bl, al);
    }
}
