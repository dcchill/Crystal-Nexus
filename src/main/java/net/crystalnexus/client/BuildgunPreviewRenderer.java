package net.crystalnexus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.crystalnexus.item.BuildGunItem;
import net.crystalnexus.schematic.BuildgunSchematicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class BuildgunPreviewRenderer {
	private static final Map<String, ClientSchematic> CACHE = new HashMap<>();

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) {
			return;
		}

		ItemStack stack = mc.player.getMainHandItem();
		if (!(stack.getItem() instanceof BuildGunItem)) {
			stack = mc.player.getOffhandItem();
		}
		if (!(stack.getItem() instanceof BuildGunItem)) {
			return;
		}

		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (BuildGunItem.isVisualActive(stack)) {
			renderPlacement(event, mc, level, stack, tag);
		}
	}

	private static void renderPlacement(RenderLevelStageEvent event, Minecraft mc, Level level, ItemStack stack, CompoundTag tag) {
		String schematicName = tag.getString("buildgunSelectedSchematic");
		if (schematicName.isBlank()) {
			return;
		}

		ClientSchematic schematic = CACHE.computeIfAbsent(schematicName, name -> load(level, name));
		if (schematic.blocks.isEmpty()) {
			return;
		}

		int distance = Mth.clamp(tag.getInt("buildgunPlacementDistance"), -64, 64);
		int rotation = Math.floorMod(tag.getInt("buildgunPlacementRotation"), 4);
		BlockPos origin = placementOrigin(mc, distance);
		List<ClientBlock> blocks = schematic.rotated(rotation, origin);
		if (blocks.isEmpty() && tag.getBoolean("buildgunPlacementActive")) {
			return;
		}
		AABB bounds = blocks.isEmpty() ? new AABB(origin) : bounds(blocks);

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		if (tag.getBoolean("buildgunPlacementActive")) {
			Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
			BlockRenderDispatcher renderer = mc.getBlockRenderer();
			RenderType ghostType = Sheets.translucentCullBlockSheet();

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.depthMask(false);
			for (ClientBlock block : blocks) {
				if (!level.hasChunkAt(block.pos) || block.state.isAir() || block.state.is(Blocks.STRUCTURE_VOID)) {
					continue;
				}
				poseStack.pushPose();
				poseStack.translate(block.pos.getX() - cam.x, block.pos.getY() - cam.y, block.pos.getZ() - cam.z);
				BakedModel model = renderer.getBlockModel(block.state);
				int light = LevelRenderer.getLightColor(level, block.pos);
				renderer.getModelRenderer().renderModel(
						poseStack.last(),
						new AlphaVertexConsumer(buffers.getBuffer(ghostType), 0.38f),
						block.state,
						model,
						1.0f, 1.0f, 1.0f,
						light,
						net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY
				);
				poseStack.popPose();
			}
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();

			renderWireBox(event, mc, bounds.inflate(0.02), 0.15f, 0.85f, 1.0f, 1.0f);
		}
		renderBeam(event, mc, stack, bounds.getCenter());
		buffers.endBatch();
	}

	private static void renderWireBox(RenderLevelStageEvent event, Minecraft mc, AABB box, float r, float g, float b, float a) {
		PoseStack poseStack = event.getPoseStack();
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), box.move(-cam.x, -cam.y, -cam.z), r, g, b, a);
		buffers.endBatch();
	}

	private static void renderBeam(RenderLevelStageEvent event, Minecraft mc, ItemStack stack, Vec3 target) {
		Vec3 start = beamStart(mc, stack);
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		Vector3f s = new Vector3f((float) (start.x - cam.x), (float) (start.y - cam.y), (float) (start.z - cam.z));
		Vector3f e = new Vector3f((float) (target.x - cam.x), (float) (target.y - cam.y), (float) (target.z - cam.z));
		Vector3f forward = new Vector3f(e).sub(s);
		if (forward.lengthSquared() < 1.0e-6f) {
			return;
		}
		forward.normalize();
		Vector3f toCam = new Vector3f(-s.x, -s.y, -s.z);
		if (toCam.lengthSquared() < 1.0e-6f) {
			toCam.set(0.0f, 1.0f, 0.0f);
		}
		toCam.normalize();
		Vector3f side = new Vector3f(forward).cross(toCam);
		if (side.lengthSquared() < 1.0e-6f) {
			side = new Vector3f(forward).cross(new Vector3f(0.0f, 1.0f, 0.0f));
		}
		side.normalize();
		Vector3f up = new Vector3f(side).cross(forward).normalize();
		PoseStack poseStack = event.getPoseStack();
		VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lightning());
		poseStack.pushPose();
		Matrix4f mat = poseStack.last().pose();
		drawBeamTube(consumer, mat, s, e, side, up, 0.12f, 0x3C, 0x44, 0x9A, 96);
		drawBeamTube(consumer, mat, s, e, side, up, 0.055f, 0x3C, 0x44, 0x9A, 255);
		poseStack.popPose();
	}

	private static AABB bounds(List<ClientBlock> blocks) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (ClientBlock block : blocks) {
			minX = Math.min(minX, block.pos.getX());
			minY = Math.min(minY, block.pos.getY());
			minZ = Math.min(minZ, block.pos.getZ());
			maxX = Math.max(maxX, block.pos.getX());
			maxY = Math.max(maxY, block.pos.getY());
			maxZ = Math.max(maxZ, block.pos.getZ());
		}
		return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
	}

	private static Vec3 beamStart(Minecraft mc, ItemStack stack) {
		Vec3 eye = mc.player.getEyePosition();
		Vec3 look = mc.player.getLookAngle();
		Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 1.0E-5D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		boolean offhand = mc.player.getOffhandItem() == stack;
		double side = offhand ? -0.22D : 0.22D;
		return eye.add(look.scale(0.65D)).add(right.scale(side)).add(0.0D, -0.18D, 0.0D);
	}

	private static BlockPos placementOrigin(Minecraft mc, int distance) {
		return BlockPos.containing(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(distance)));
	}

	private static ClientSchematic load(Level level, String name) {
		try {
			if (!name.toLowerCase(Locale.ROOT).endsWith(".nbt") || !Path.of(name).getFileName().toString().equals(name)) {
				return new ClientSchematic(List.of());
			}
			Path file = BuildgunSchematicManager.schematicDirectory().resolve(name).normalize();
			if (!file.startsWith(BuildgunSchematicManager.schematicDirectory())) {
				return new ClientSchematic(List.of());
			}
			CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
			List<BlockState> palette = new ArrayList<>(paletteTag.size());
			for (int i = 0; i < paletteTag.size(); i++) {
				palette.add(NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), paletteTag.getCompound(i)));
			}

			ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
			List<ClientBlock> blocks = new ArrayList<>(blocksTag.size());
			for (int i = 0; i < blocksTag.size(); i++) {
				CompoundTag blockTag = blocksTag.getCompound(i);
				ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
				BlockState state = palette.get(blockTag.getInt("state"));
				if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
					blocks.add(new ClientBlock(new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2)), state));
				}
			}
			blocks.sort(Comparator.comparingInt((ClientBlock block) -> block.pos.getY()).thenComparingInt(block -> block.pos.getZ()).thenComparingInt(block -> block.pos.getX()));
			return new ClientSchematic(blocks);
		} catch (IOException | RuntimeException exception) {
			return new ClientSchematic(List.of());
		}
	}

	private record ClientSchematic(List<ClientBlock> blocks) {
		private List<ClientBlock> rotated(int rotation, BlockPos origin) {
			List<ClientBlock> rotated = new ArrayList<>(blocks.size());
			int quarterTurns = Math.floorMod(rotation, 4);
			for (ClientBlock block : blocks) {
				BlockPos pos = switch (quarterTurns) {
					case 1 -> new BlockPos(-block.pos.getZ(), block.pos.getY(), block.pos.getX());
					case 2 -> new BlockPos(-block.pos.getX(), block.pos.getY(), -block.pos.getZ());
					case 3 -> new BlockPos(block.pos.getZ(), block.pos.getY(), -block.pos.getX());
					default -> block.pos;
				};
				BlockState state = block.state;
				for (int i = 0; i < quarterTurns; i++) {
					state = state.rotate(Rotation.CLOCKWISE_90);
				}
				rotated.add(new ClientBlock(origin.offset(pos), state));
			}
			return rotated;
		}
	}

	private record ClientBlock(BlockPos pos, BlockState state) {
	}

	private static class AlphaVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float alpha;

		private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
			this.delegate = delegate;
			this.alpha = alpha;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			delegate.setColor(r, g, b, Math.max(0, Math.min(255, (int) (a * alpha))));
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			delegate.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			delegate.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			delegate.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			delegate.setNormal(x, y, z);
			return this;
		}
	}

	private static void drawBeamTube(VertexConsumer vc, Matrix4f mat, Vector3f s, Vector3f e, Vector3f side, Vector3f up, float halfWidth, int r, int g, int b, int a) {
		Vector3f sideW = new Vector3f(side).mul(halfWidth);
		Vector3f upW = new Vector3f(up).mul(halfWidth);
		quad(vc, mat, new Vector3f(s).add(sideW), new Vector3f(s).sub(sideW), new Vector3f(e).sub(sideW), new Vector3f(e).add(sideW), r, g, b, a);
		quad(vc, mat, new Vector3f(s).add(upW), new Vector3f(s).sub(upW), new Vector3f(e).sub(upW), new Vector3f(e).add(upW), r, g, b, a);
	}

	private static void quad(VertexConsumer vc, Matrix4f mat, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int r, int g, int bl, int al) {
		vertex(vc, mat, a, r, g, bl, al);
		vertex(vc, mat, b, r, g, bl, al);
		vertex(vc, mat, c, r, g, bl, al);
		vertex(vc, mat, d, r, g, bl, al);
	}

	private static void vertex(VertexConsumer vc, Matrix4f mat, Vector3f p, int r, int g, int b, int a) {
		Vector4f t = new Vector4f(p.x, p.y, p.z, 1.0f).mul(mat);
		vc.addVertex(t.x, t.y, t.z).setColor(r, g, b, a);
	}
}
