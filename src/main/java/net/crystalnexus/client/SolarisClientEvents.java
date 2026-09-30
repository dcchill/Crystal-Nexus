package net.crystalnexus.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.SolarisItem;
import net.crystalnexus.network.SolarisRadiusMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public final class SolarisClientEvents {
    private SolarisClientEvents() { }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !minecraft.player.isShiftKeyDown()
                || !minecraft.player.getMainHandItem().is(CrystalnexusModItems.SOLARIS.get())
                || event.getScrollDeltaY() == 0) return;
        PacketDistributor.sendToServer(new SolarisRadiusMessage(event.getScrollDeltaY() > 0 ? 1 : -1));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.getMainHandItem().is(CrystalnexusModItems.SOLARIS.get())) return;
        int radius = SolarisItem.radius(minecraft.player.getMainHandItem());
        if (radius == 0) return;

        BlockPos center = event.getTarget().getBlockPos();
        Vec3 look = minecraft.player.getLookAngle();
        Direction.Axis axis = Direction.getNearest(look.x, look.y, look.z).getAxis();
        Vec3 camera = event.getCamera().getPosition();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        var pose = event.getPoseStack().last();
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                BlockPos pos = switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                if (!minecraft.level.hasChunkAt(pos) || !minecraft.level.mayInteract(minecraft.player, pos)) continue;
                var state = minecraft.level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(minecraft.level, pos) < 0) continue;
                state.getShape(minecraft.level, pos, CollisionContext.of(minecraft.player)).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
                    float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length == 0) return;
                    dx /= length; dy /= length; dz /= length;
                    lines.addVertex(pose, (float) (pos.getX() + x1 - camera.x), (float) (pos.getY() + y1 - camera.y), (float) (pos.getZ() + z1 - camera.z))
                        .setColor(0.0F, 0.0F, 0.0F, 0.4F).setNormal(pose, dx, dy, dz);
                    lines.addVertex(pose, (float) (pos.getX() + x2 - camera.x), (float) (pos.getY() + y2 - camera.y), (float) (pos.getZ() + z2 - camera.z))
                        .setColor(0.0F, 0.0F, 0.0F, 0.4F).setNormal(pose, dx, dy, dz);
                });
            }
        }
        event.setCanceled(true);
    }
}
