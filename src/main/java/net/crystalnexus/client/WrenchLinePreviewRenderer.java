package net.crystalnexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.item.CrystalWrenchItem;
import net.crystalnexus.util.WrenchLinePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class WrenchLinePreviewRenderer {
    private WrenchLinePreviewRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) return;

        ItemStack wrench = minecraft.player.getMainHandItem();
        ItemStack offhand = minecraft.player.getOffhandItem();
        if (!(wrench.getItem() instanceof CrystalWrenchItem)
            || !(offhand.getItem() instanceof BlockItem blockItem)
            || !(minecraft.hitResult instanceof BlockHitResult hit)
            || hit.getType() == HitResult.Type.MISS) return;

        CompoundTag preview = wrench.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getCompound(WrenchLinePlacement.PREVIEW_KEY);
        if (preview.isEmpty()
            || !preview.getString("dimension").equals(level.dimension().location().toString())
            || !preview.getString("block").equals(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString())) return;

        BlockPos start = BlockPos.of(preview.getLong("start"));
        BlockPos end = new BlockPlaceContext(level, minecraft.player, InteractionHand.OFF_HAND, offhand, hit).getClickedPos();
        if (start.equals(end)) return;

        List<BlockPos> path = WrenchLinePlacement.planPath(start, end,
            Direction.from3DDataValue(preview.getInt("direction")));
        int available = minecraft.player.isCreative() ? Integer.MAX_VALUE : available(minecraft.player.getInventory().items, offhand);
        renderPath(event, minecraft, level, blockItem, offhand, path, available);
    }

    private static void renderPath(RenderLevelStageEvent event, Minecraft minecraft, Level level,
                                   BlockItem blockItem, ItemStack offhand, List<BlockPos> path, int available) {
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Set<BlockPos> route = new HashSet<>(path);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        try {
            for (int i = 0; i < path.size(); i++) {
                BlockPos pos = path.get(i);
                if (!level.hasChunkAt(pos)) continue;
                boolean missing = i >= available;
                Direction travel = travelDirection(path, i);
                BlockPlaceContext context = new BlockPlaceContext(level, minecraft.player, InteractionHand.OFF_HAND,
                    offhand, new BlockHitResult(Vec3.atCenterOf(pos), travel.getOpposite(), pos, false));
                BlockState placedState = blockItem.getBlock().getStateForPlacement(context);
                BlockState state = connectedState(
                    placedState == null ? blockItem.getBlock().defaultBlockState() : placedState, route, pos);
                poses.pushPose();
                poses.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                BakedModel model = dispatcher.getBlockModel(state);
                dispatcher.getModelRenderer().renderModel(
                    poses.last(), new TintedAlphaVertexConsumer(
                        buffers.getBuffer(Sheets.translucentCullBlockSheet()), missing),
                    state, model, 1.0f, 1.0f, 1.0f,
                    LevelRenderer.getLightColor(level, pos), OverlayTexture.NO_OVERLAY
                );
                poses.popPose();

                AABB box = new AABB(pos).move(-camera.x, -camera.y, -camera.z);
                LevelRenderer.renderLineBox(poses, buffers.getBuffer(RenderType.lines()), box,
                    missing ? 1.0f : 0.18f, missing ? 0.12f : 0.85f,
                    missing ? 0.12f : 1.0f, 0.75f);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
        buffers.endBatch();
    }

    private static BlockState connectedState(BlockState state, Set<BlockPos> route, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (state.getBlock().getStateDefinition().getProperty(direction.getName()) instanceof BooleanProperty property) {
                state = state.setValue(property, route.contains(pos.relative(direction)));
            }
        }
        return state;
    }

    private static Direction travelDirection(List<BlockPos> path, int index) {
        BlockPos from = index + 1 < path.size() ? path.get(index) : path.get(index - 1);
        BlockPos to = index + 1 < path.size() ? path.get(index + 1) : path.get(index);
        return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private static int available(List<ItemStack> inventory, ItemStack template) {
        int count = template.getCount();
        for (ItemStack stack : inventory) {
            if (ItemStack.isSameItemSameComponents(stack, template)) count += stack.getCount();
        }
        return count;
    }

    private static final class TintedAlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final boolean red;

        private TintedAlphaVertexConsumer(VertexConsumer delegate, boolean red) {
            this.delegate = delegate;
            this.red = red;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(r, red ? g / 6 : g, red ? b / 6 : b, Math.min(255, (int) (a * 0.38f)));
            return this;
        }
    }
}
