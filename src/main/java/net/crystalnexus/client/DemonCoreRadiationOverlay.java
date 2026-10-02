package net.crystalnexus.client;

import java.util.Random;

import net.crystalnexus.item.DemonCoreItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class DemonCoreRadiationOverlay {
	private static final double GAZE_RANGE = 8.0;
	private static final float OPEN_INTENSITY = 0.28F;
	private static final float CLOSED_INTENSITY = 0.95F;
	private static final Random NOISE = new Random();
	private static float previousIntensity;
	private static float intensity;
	private static long noiseTick;
	private static Entity lastCamera;

	private DemonCoreRadiationOverlay() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Entity camera = minecraft.getCameraEntity();
		if (minecraft.player == null || minecraft.level == null || camera == null) {
			reset();
			return;
		}
		if (lastCamera != camera) {
			reset();
			lastCamera = camera;
		}
		if (minecraft.isPaused()) return;

		float target = Math.max(coreIntensity(minecraft.player.getMainHandItem()),
				coreIntensity(minecraft.player.getOffhandItem()));
		target = Math.max(target, gazeIntensity(minecraft, camera));
		previousIntensity = intensity;
		// Ease in quickly and leave a short, smooth radiation afterimage.
		intensity = Mth.lerp(target > intensity ? 0.3F : 0.15F, intensity, target);
		if (intensity < 0.002F) intensity = 0.0F;
		noiseTick++;
	}

	private static float coreIntensity(ItemStack stack) {
		if (!(stack.getItem() instanceof DemonCoreItem)) return 0.0F;
		return DemonCoreItem.isClosed(stack) ? CLOSED_INTENSITY : OPEN_INTENSITY;
	}

	private static float gazeIntensity(Minecraft minecraft, Entity camera) {
		Vec3 start = camera.getEyePosition();
		Vec3 end = start.add(camera.getViewVector(1.0F).scale(GAZE_RANGE));
		// Dropped items are not vanilla pickable entities, so use an explicit sight ray.
		var blockHit = minecraft.level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera));
		if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
		AABB search = new AABB(start, end).inflate(0.3);
		double nearestDistance = start.distanceToSqr(end);
		float result = 0.0F;
		for (Entity entity : minecraft.level.getEntities(camera, search,
				candidate -> !candidate.isSpectator() && (candidate instanceof ItemEntity
						|| candidate instanceof ItemFrame || candidate instanceof LivingEntity))) {
			var intersection = entity.getBoundingBox().inflate(0.15).clip(start, end);
			if (intersection.isEmpty()) continue;
			double distance = start.distanceToSqr(intersection.get());
			if (distance > nearestDistance) continue;
			nearestDistance = distance;
			if (entity instanceof ItemEntity dropped) {
				result = coreIntensity(dropped.getItem());
			} else if (entity instanceof ItemFrame frame) {
				result = coreIntensity(frame.getItem());
			} else if (entity instanceof LivingEntity living) {
				result = Math.max(coreIntensity(living.getMainHandItem()), coreIntensity(living.getOffhandItem()));
			}
		}
		return result;
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
				|| minecraft.options.hideGui) return;
		float strength = Mth.lerp(event.getPartialTick().getGameTimeDeltaPartialTick(false),
				previousIntensity, intensity) * minecraft.options.screenEffectScale().get().floatValue();
		if (strength < 0.002F) return;

		GuiGraphics graphics = event.getGuiGraphics();
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		if (width <= 0 || height <= 0) return;
		// Stable within each game tick: high FPS does not increase flickering/noise density.
		NOISE.setSeed(noiseTick * 341873128712L + 132897987541L);
		graphics.fill(0, 0, width, height, color(strength * 7, 0x167E8C));

		int scanOffset = (int) (noiseTick % 7);
		for (int y = scanOffset; y < height; y += 7) {
			graphics.fill(0, y, width, y + 1, color(strength * 18, 0x163B48));
		}
		int specks = (int) (strength * 230);
		for (int i = 0; i < specks; i++) {
			int x = NOISE.nextInt(width);
			int y = NOISE.nextInt(height);
			int length = 1 + NOISE.nextInt(3);
			int rgb = NOISE.nextBoolean() ? 0xA8F8DF : 0x64C9FF;
			graphics.fill(x, y, Math.min(width, x + length), y + 1,
					color(strength * (35 + NOISE.nextInt(85)), rgb));
		}
		// Low-opacity offset color bands suggest tearing without bright full-screen flashes.
		int bands = 1 + (int) (strength * 4);
		for (int i = 0; i < bands; i++) {
			int x = NOISE.nextInt(width);
			int y = NOISE.nextInt(height);
			int bandWidth = Math.max(1, width / 8 + NOISE.nextInt(Math.max(1, width / 3)));
			int bandHeight = 1 + NOISE.nextInt(3);
			int right = Math.min(width, x + bandWidth);
			graphics.fill(x, y, right, Math.min(height, y + bandHeight), color(strength * 42, 0x6CFFE0));
			graphics.fill(Math.max(0, x - 3), Math.min(height, y + bandHeight),
					Math.max(0, right - 3), Math.min(height, y + bandHeight + 1), color(strength * 32, 0x668EFF));
		}
	}

	private static int color(float alpha, int rgb) {
		return (Mth.clamp((int) alpha, 0, 255) << 24) | rgb;
	}

	private static void reset() {
		previousIntensity = 0.0F;
		intensity = 0.0F;
		noiseTick = 0L;
		lastCamera = null;
	}
}