package net.crystalnexus.events;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.item.PrisonCubeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Vanilla display entities synchronize the entire seal with nearby clients. */
@EventBusSubscriber(modid = CrystalnexusMod.MODID)
public final class PrisonCubeCapture {
	private static final List<Capture> ACTIVE = new ArrayList<>();
	private PrisonCubeCapture() {}

	public static void start(ItemStack stack, Player player, Entity target) {
		if (ACTIVE.stream().anyMatch(c -> c.stack == stack || c.target == target || c.player == player)) return;
		if (target instanceof Display) return;
		ACTIVE.add(new Capture((ServerLevel) player.level(), stack, player, target));
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		ACTIVE.removeIf(capture -> {
			if (capture.tick()) return false;
			capture.cleanup();
			return true;
		});
	}

	@SubscribeEvent
	public static void restrain(EntityTickEvent.Pre event) {
		if (!ACTIVE.isEmpty() && !event.getEntity().level().isClientSide && ACTIVE.stream().anyMatch(c -> c.target == event.getEntity() && !c.sealed)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void stop(ServerStoppingEvent event) {
		ACTIVE.forEach(Capture::cleanup);
		ACTIVE.clear();
	}

	private static final class Capture {
		final ServerLevel level;
		final ItemStack stack;
		final Player player;
		final Entity target;
		final Vec3 origin, center, forward, right;
		final float radius;
		final List<Piece> pieces = new ArrayList<>();
		final Piece wall, seal;
		final Piece[] cubes = new Piece[8];
		final Piece[][] tendrils = new Piece[8][3];
		int age;
		boolean sealed;

		Capture(ServerLevel level, ItemStack stack, Player player, Entity target) {
			this.level = level;
			this.stack = stack;
			this.player = player;
			this.target = target;
			origin = target.position();
			center = origin.add(0, target.getBbHeight() * 0.5, 0);
			Vec3 direction = center.subtract(player.position()).multiply(1, 0, 1).normalize();
			forward = direction.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : direction;
			right = new Vec3(forward.z, 0, -forward.x);
			radius = Math.max(1.3f, Math.max(target.getBbWidth(), target.getBbHeight()) * 0.65f);
			wall = piece(1);
			seal = piece(0);
			for (int i = 0; i < cubes.length; i++) {
				cubes[i] = piece(0);
				for (int j = 0; j < 3; j++) tendrils[i][j] = piece(2);
			}
			animate();
			pieces.forEach(level::addFreshEntity);
			level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 0.55f);
		}

		Piece piece(int model) {
			ItemStack visual = new ItemStack(stack.getItem());
			if (model != 0) visual.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(model));
			Piece piece = new Piece(level, center, visual);
			pieces.add(piece);
			return piece;
		}

		boolean tick() {
			if (!sealed && (!target.isAlive() || target.level() != level || !player.isAlive()
					|| player.isRemoved() || player.level() != level || player.distanceToSqr(target) > 256
					|| (player.getMainHandItem() != stack && player.getOffhandItem() != stack)
					|| stack.isEmpty() || PrisonCubeItem.hasStoredEntity(stack) || !level.hasChunkAt(target.blockPosition()))) return false;
			age++;
			if (!sealed) {
				target.setPos(origin);
				target.setDeltaMovement(Vec3.ZERO);
				target.hurtMarked = true;
			}
			if (age == 32 || age == 58) {
				level.playSound(null, target.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1f, 0.55f);
			}
			if (age == 88) {
				PrisonCubeItem.finishCapture(stack, target);
				sealed = true;
				level.playSound(null, target.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1.2f, 0.5f);
			}
			if (age % 2 == 0) animate();
			return age < 108;
		}

		void animate() {
			float opening = progress(age, 0, 20);
			float surround = progress(age, 20, 44);
			float bind = progress(age, 38, 58);
			float close = progress(age, 64, 88);
			float vanish = progress(age, 98, 108);
			float yaw = (float) Math.atan2(forward.x, forward.z);
			Vec3 wallPos = forward.scale(radius + 0.45);
			float wallSize = radius * 2.3f * opening * (1 - progress(age, 28, 46));
			wall.pose(new Matrix4f().translation(vector(wallPos)).rotateY(yaw).scale(wallSize, wallSize, Math.max(0.01f, wallSize)));
			for (int i = 0; i < cubes.length; i++) {
				float x = (i & 1) == 0 ? -1 : 1;
				float y = (i & 2) == 0 ? -1 : 1;
				float z = (i & 4) == 0 ? -1 : 1;
				Vec3 from = wallPos.add(right.scale(x * radius * 0.85)).add(0, y * radius * 0.85, 0);
				Vec3 around = right.scale(x * radius).add(0, y * radius * 0.85, 0).add(forward.scale(z * radius));
				Vec3 pos = from.lerp(around, surround).scale(1 - close);
				float pulse = 1 + 0.035f * (float) Math.sin(age * 0.35 + i);
				float size = (0.12f + opening * 0.85f) * pulse * (1 - close);
				// The existing cube occupies half a block and is centered two pixels above the item origin.
				cubes[i].pose(new Matrix4f().translation(vector(pos)).rotateY(yaw + x * surround * 0.3f * (1 - close))
						.scale(size * 2).translate(0, -0.125f, 0));
				Vec3 contact = right.scale(x * target.getBbWidth() * 0.2).add(0, y * target.getBbHeight() * 0.18, 0);
				Vec3 end = pos.lerp(contact, bind);
				Vec3 previous = pos;
				for (int j = 0; j < 3; j++) {
					float t = (j + 1) / 3f;
					Vec3 next = pos.lerp(end, t).add(0, Math.sin(t * Math.PI) * 0.18 * Math.sin(age * 0.22 + i) * (1 - close), 0);
					Vec3 delta = next.subtract(previous);
					float thickness = (0.18f - j * 0.035f) * bind * (1 - close);
					Quaternionf rotation = delta.lengthSqr() < 0.000001 ? new Quaternionf()
							: new Quaternionf().rotationTo(new Vector3f(0, 1, 0), vector(delta.normalize()));
					tendrils[i][j].pose(new Matrix4f().translation(vector(previous.lerp(next, 0.5))).rotate(rotation)
							.scale(thickness, (float) delta.length() + thickness * 0.3f, thickness));
					previous = next;
				}
			}
			// Enclose the still-visible target before storage, then contract to a small sealed cube.
			float shell = progress(age, 76, 86) * (radius * 4.8f);
			shell = (shell + (0.85f - shell) * progress(age, 88, 98)) * (1 - vanish);
			seal.pose(new Matrix4f().rotateY(yaw).scale(shell).translate(0, -0.125f, 0));
		}

		void cleanup() { pieces.forEach(Entity::discard); }
	}

	static float progress(int age, int start, int end) {
		float t = Math.clamp((age - start) / (float) (end - start), 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static Vector3f vector(Vec3 v) { return new Vector3f((float) v.x, (float) v.y, (float) v.z); }

	/** Ephemeral vanilla displays must never be left in a saved chunk after a restart. */
	private static final class Piece extends Display.ItemDisplay {
		final CompoundTag visual = new CompoundTag();
		Piece(ServerLevel level, Vec3 center, ItemStack stack) {
			super(EntityType.ITEM_DISPLAY, level);
			setPos(center);
			visual.put("item", stack.save(level.registryAccess()));
			visual.putInt("interpolation_duration", 2);
			visual.putFloat("view_range", 1.5f);
		}

		void pose(Matrix4f matrix) {
			visual.put("transformation", net.minecraft.util.ExtraCodecs.MATRIX4F.encodeStart(NbtOps.INSTANCE, matrix).getOrThrow());
			visual.putInt("start_interpolation", 0);
			readAdditionalSaveData(visual);
		}

		@Override
		public boolean shouldBeSaved() { return false; }
	}
}
