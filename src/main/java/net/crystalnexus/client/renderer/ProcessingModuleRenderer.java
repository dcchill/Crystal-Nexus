package net.crystalnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ProcessingModuleBlock;
import net.crystalnexus.block.ProcessingModuleAnimation;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.block.entity.ProcessingModuleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ProcessingModuleRenderer implements BlockEntityRenderer<ProcessingModuleBlockEntity> {
    public static final ModelResourceLocation PRESS_HEAD = ModelResourceLocation.standalone(
        ResourceLocation.parse("crystalnexus:block/processing_press_head"));
    public static final ModelResourceLocation LASER_HEAD = ModelResourceLocation.standalone(
        ResourceLocation.parse("crystalnexus:block/processing_laser_head"));
    public static final ModelResourceLocation DRILL_HEAD = ModelResourceLocation.standalone(
        ResourceLocation.parse("crystalnexus:block/processing_drill_head"));
    public static final ModelResourceLocation NANOFABRICATOR_HEAD = ModelResourceLocation.standalone(
        ResourceLocation.parse("crystalnexus:block/nanofabricator_head"));
    public static final ModelResourceLocation WELDER_HEAD = ModelResourceLocation.standalone(
        ResourceLocation.parse("crystalnexus:block/processing_welder_head"));

    public ProcessingModuleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(ProcessingModuleBlockEntity module, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay) {
        if (module.getLevel() == null || !(module.getLevel().getBlockEntity(module.getBlockPos().below())
                instanceof ConveyerBeltBaseBlockEntity belt)) return;
        light = net.minecraft.client.renderer.LevelRenderer.getLightColor(module.getLevel(), belt.getBlockPos());
        Vec3 point = ConveyorGeometry.point(belt.getBlockState(), .5);
        boolean held = belt.isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT);
        boolean running = held && module.isWorking();
        float animationProgress = module.animationProgress(partialTick);
        float progress = held ? animationProgress : 0;
        boolean press = module.operation() == ProcessingModuleBlock.Operation.PRESS;
        boolean drill = module.operation() == ProcessingModuleBlock.Operation.DRILL;
        boolean nano = module.operation() == ProcessingModuleBlock.Operation.NANOFABRICATE;
        boolean weld = module.operation() == ProcessingModuleBlock.Operation.WELD;
        double stroke = weld ? ProcessingModuleAnimation.weldingStroke(progress)
            : drill ? ProcessingModuleAnimation.cncStroke(progress)
            : press ? ProcessingModuleAnimation.pressStroke(progress) : 0;
        double headY = press || drill || weld ? -.06 - stroke * Math.max(0, .94 - point.y - (weld ? .065 : drill ? .06 : .085)) : 0;
        double sweep = nano ? ProcessingModuleAnimation.fabricationSweep(progress) : 0;
        var facing = module.getBlockState().getValue(ProcessingModuleBlock.FACING);
        double scanX = sweep * facing.getStepZ(), scanZ = sweep * facing.getStepX();
        if (drill) {
            scanX = ProcessingModuleAnimation.cncOffsetX(progress);
            scanZ = ProcessingModuleAnimation.cncOffsetZ(progress);
        }
        if (nano) headY = -ProcessingModuleAnimation.fabricationStroke(progress) * Math.max(0, .66 - point.y);

        var minecraft = Minecraft.getInstance();
        poses.pushPose();
        poses.translate(point.x + scanX, headY, point.z + scanZ);
        if (drill) poses.mulPose(Axis.YP.rotationDegrees(ProcessingModuleAnimation.drillRotation(progress)));
        poses.translate(-.5, 0, -.5);
        var head = switch (module.operation()) {
            case PRESS -> PRESS_HEAD;
            case ENGRAVE -> LASER_HEAD;
            case DRILL -> DRILL_HEAD;
            case NANOFABRICATE -> NANOFABRICATOR_HEAD;
            case WELD -> WELDER_HEAD;
        };
        var model = minecraft.getModelManager().getModel(head);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(poses.last(), buffers.getBuffer(RenderType.solid()),
            module.getBlockState(), model, 1, 1, 1, light, overlay);
        poses.popPose();

        if (nano) {
            fabrication(module, poses, buffers, point, headY, scanX, scanZ, progress, running, partialTick);
            return;
        }

        if (!running) return;
        if (weld) {
            welding(module, poses, buffers, point, progress);
        } else if (press && !module.material().isEmpty()) {
            poses.pushPose();
            poses.translate(point.x, headY - .015, point.z);
            poses.mulPose(Axis.XP.rotationDegrees(90));
            poses.scale(.25F, .25F, .25F);
            minecraft.getItemRenderer().renderStatic(module.material(), ItemDisplayContext.FIXED, light, overlay,
                poses, buffers, module.getLevel(), (int) module.getBlockPos().asLong());
            poses.popPose();
        } else if (module.operation() == ProcessingModuleBlock.Operation.ENGRAVE) {
            double phase = progress * Math.PI * 2;
            Vec3 target = new Vec3(point.x + Math.sin(phase * 3) * .07, point.y - 1 + .065,
                point.z + Math.cos(phase * 2) * .07);
            Vec3 emitter = new Vec3(point.x, -.19, point.z);
            VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
            beam(vertices, poses.last().pose(), emitter, target, .025, 70, 180, 255, 95);
            beam(vertices, poses.last().pose(), emitter, target, .006, 190, 245, 255, 255);
        }
    }

    private static void welding(ProcessingModuleBlockEntity module, PoseStack poses, MultiBufferSource buffers,
            Vec3 point, float progress) {
        float glow = ProcessingModuleAnimation.weldingGlow(progress);
        if (glow <= 0) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poses.last().pose();
        double y = point.y - 1 + .068;
        beam(vertices, matrix, point.x, y, point.z, point.x, y + .015, point.z,
            .018, 205, 235, 255, (int) (255 * glow));
        double seed = (module.getBlockPos().asLong() & 255) * .1;
        for (int i = 0; i < 14; i++) {
            double cycle = progress * 24 + i * .618;
            double age = cycle - Math.floor(cycle);
            double angle = seed + i * 2.4;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double radius = .025 + age * .20;
            double height = y + .015 + Math.sin(age * Math.PI) * .11;
            double x = point.x + dx * radius, z = point.z + dz * radius;
            beam(vertices, matrix, x, height, z, x + dx * .04, height + .025, z + dz * .04,
                .007, 255, 185, 75, (int) (255 * glow * (1 - age)));
        }
    }

    private static void fabrication(ProcessingModuleBlockEntity module, PoseStack poses, MultiBufferSource buffers,
            Vec3 point, double headY, double scanX, double scanZ, float progress, boolean running, float partialTick) {
        double time = module.getLevel().getGameTime() + (double) partialTick;
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poses.last().pose();
        double x = point.x + scanX, z = point.z + scanZ;
        // A handful of rendered motes are naturally frustum-culled with the machine; no particle entities or packets.
        int indicator = module.currentRecipe() == null ? (int) (100 + 35 * Math.sin(time * .08)) : 160;
        beam(vertices, matrix, x, headY - .191, z, x, headY - .187, z, .035, 80, 225, 255, indicator);
        float strength = running ? ProcessingModuleAnimation.fabricationBeam(progress) : 0;
        if (strength <= 0) return;
        double targetY = point.y - 1 + .065;
        beam(vertices, matrix, x, headY - .19, z, x, targetY, z, .018 + .008 * strength,
            30, 150, 255, (int) (65 * strength));
        beam(vertices, matrix, x, headY - .19, z, x, targetY, z, .004 + .002 * strength,
            165, 245, 255, (int) Math.min(255, 180 * strength));
        double seed = (module.getBlockPos().asLong() & 255) * .1;
        for (int i = 0; i < 6; i++) {
            double phase = progress * Math.PI * 18 + i * Math.PI / 3 + seed;
            double radius = .035 + (i % 3) * .022;
            double px = x + Math.cos(phase) * radius, pz = z + Math.sin(phase) * radius;
            double py = targetY + .012 + .018 * (1 + Math.sin(phase * 1.7));
            beam(vertices, matrix, px, py, pz, px, py + .009, pz, .004,
                130, 235, 255, (int) Math.min(255, 150 * strength));
        }
    }

    private static void beam(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to,
            double width, int r, int g, int b, int alpha) {
        beam(vertices, matrix, from.x, from.y, from.z, to.x, to.y, to.z, width, r, g, b, alpha);
    }

    private static void beam(VertexConsumer vertices, Matrix4f matrix, double x, double y, double z,
            double tx, double ty, double tz, double width, int r, int g, int b, int alpha) {
        for (int axis = 0; axis < 2; axis++) {
            double dx = axis == 0 ? width : 0, dz = axis == 1 ? width : 0;
            vertex(vertices, matrix, x - dx, y, z - dz, r, g, b, alpha);
            vertex(vertices, matrix, x + dx, y, z + dz, r, g, b, alpha);
            vertex(vertices, matrix, tx + dx, ty, tz + dz, r, g, b, alpha);
            vertex(vertices, matrix, tx - dx, ty, tz - dz, r, g, b, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, double x, double y, double z, int r, int g, int b, int alpha) {
        vertices.addVertex(matrix, (float) x, (float) y, (float) z).setColor(r, g, b, alpha);
    }

    @Override public AABB getRenderBoundingBox(ProcessingModuleBlockEntity module) {
        return new AABB(module.getBlockPos()).expandTowards(0, -1, 0);
    }
}
