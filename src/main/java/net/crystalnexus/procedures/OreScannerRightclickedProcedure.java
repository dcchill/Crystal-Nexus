package net.crystalnexus.procedures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.crystalnexus.network.payload.S2C_OreScanResult;
import net.crystalnexus.util.OreTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;

public class OreScannerRightclickedProcedure {

	// tuning
	private static final int RADIUS = 32;
	private static final int MAX_FOUND = 1200;
	private static final int DURATION_TICKS = 100;

	// FE cost per scan
	private static final int FE_COST = 5120;

	// persistent filter key on the player
	private static final String FILTER_TAG = "crystalnexus_ore_scanner_filter";

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null) return;
		if (!(world instanceof Level level)) return;
		if (level.isClientSide) return;
		if (!(entity instanceof ServerPlayer sp)) return;

		// ✅ SHIFT = set/clear filter (NO FE cost)
		if (sp.isShiftKeyDown()) {
			handleFilterPick(level, sp);
			return;
		}

		// ✅ Normal use = costs FE
		if (!consumeFeFromInventoryTwoPass(sp, FE_COST)) {
			sp.displayClientMessage(Component.literal("Not enough FE (" + FE_COST + ")"), true);
			level.playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
			return;
		}

		String filter = sp.getPersistentData().getString(FILTER_TAG);

		List<BlockPos> found = scanForOresSphereNearestFirst(level, sp.blockPosition(), RADIUS, MAX_FOUND, filter);

		PacketDistributor.sendToPlayer(sp, new S2C_OreScanResult(found, DURATION_TICKS));
		level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.2f);
	}

	// ---------------- FILTER PICK ----------------

	// Shift+RightClick:
	// - If looking at an ore block: set filter to its ore token (iron/copper/tin/etc.)
	// - Otherwise: clear filter (ALL)
	private static void handleFilterPick(Level level, ServerPlayer sp) {
		HitResult hit = rayTrace(level, sp, 6.0);

		if (hit.getType() != HitResult.Type.BLOCK) {
			sp.getPersistentData().remove(FILTER_TAG);
			sp.displayClientMessage(Component.literal("Ore Scanner filter: ALL"), true);
			level.playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
			return;
		}

		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		BlockState state = level.getBlockState(pos);

		if (!isOreBlock(state)) {
			sp.getPersistentData().remove(FILTER_TAG);
			sp.displayClientMessage(Component.literal("Ore Scanner filter: ALL (not an ore)"), true);
			level.playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 0.9f);
			return;
		}

		String token = oreTokenFromState(state);
		if (token.isEmpty()) {
			sp.getPersistentData().remove(FILTER_TAG);
			sp.displayClientMessage(Component.literal("Ore Scanner filter: ALL"), true);
		} else {
			sp.getPersistentData().putString(FILTER_TAG, token);
			sp.displayClientMessage(Component.literal("Ore Scanner filter: " + token), true);
		}

		level.playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.1f);
	}

	private static HitResult rayTrace(Level level, ServerPlayer sp, double distance) {
		Vec3 eye = sp.getEyePosition();
		Vec3 look = sp.getLookAngle();
		Vec3 end = eye.add(look.x * distance, look.y * distance, look.z * distance);

		ClipContext ctx = new ClipContext(
				eye, end,
				ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE,
				sp
		);
		return level.clip(ctx);
	}

	// ---------------- ENERGY DRAIN (TWO-PASS) ----------------

	// ✅ Pass 1: simulate total extractable FE
	// ✅ Pass 2: actually drain if enough
	private static boolean consumeFeFromInventoryTwoPass(ServerPlayer sp, int amount) {
		int available = 0;

		for (int slot = 0; slot < sp.getInventory().getContainerSize(); slot++) {
			ItemStack stack = sp.getInventory().getItem(slot);
			if (stack.isEmpty()) continue;

			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null) continue;

			int can = es.extractEnergy(amount - available, true);
			if (can > 0) {
				available += can;
				if (available >= amount) break;
			}
		}

		if (available < amount) return false; // not enough FE -> no drain, no scan

		int remaining = amount;
		for (int slot = 0; slot < sp.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = sp.getInventory().getItem(slot);
			if (stack.isEmpty()) continue;

			IEnergyStorage es = stack.getCapability(Capabilities.EnergyStorage.ITEM);
			if (es == null) continue;

			int extracted = es.extractEnergy(remaining, false);
			remaining -= extracted;
		}

		return remaining <= 0;
	}

	// ---------------- SCAN ----------------

	private static List<BlockPos> scanForOresSphereNearestFirst(Level level, BlockPos center, int radius, int maxFound, String filter) {
		final int r2 = radius * radius;

		int minX = center.getX() - radius;
		int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
		int minZ = center.getZ() - radius;

		int maxX = center.getX() + radius;
		int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius);
		int maxZ = center.getZ() + radius;

		List<PosWithDist> candidates = new ArrayList<>(Math.min(maxFound * 2, 4096));
		BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

		for (int y = minY; y <= maxY; y++) {
			int dy = y - center.getY();
			int dy2 = dy * dy;

			for (int x = minX; x <= maxX; x++) {
				int dx = x - center.getX();
				int dx2 = dx * dx;

				int base = dx2 + dy2;
				if (base > r2) continue;

				for (int z = minZ; z <= maxZ; z++) {
					int dz = z - center.getZ();
					int dist2 = base + (dz * dz);
					if (dist2 > r2) continue; // sphere boundary

					mp.set(x, y, z);
					BlockState state = level.getBlockState(mp);
					if (state.isAir()) continue;

					if (!isOreBlock(state)) continue;
					if (!matchesFilter(state, filter)) continue;

					candidates.add(new PosWithDist(mp.immutable(), dist2));

					// cap candidate growth to avoid huge lag in ore-dense areas
					if (candidates.size() >= maxFound * 6) break;
				}
			}
		}

		candidates.sort(Comparator.comparingInt(p -> p.dist2));

		List<BlockPos> out = new ArrayList<>(Math.min(maxFound, candidates.size()));
		for (int i = 0; i < candidates.size() && out.size() < maxFound; i++) {
			out.add(candidates.get(i).pos);
		}
		return out;
	}

	private static boolean isOreBlock(BlockState state) {
		if (state.is(OreTags.C_ORES)) return true;
		var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		return key != null && key.getPath().contains("ore");
	}

	private static boolean matchesFilter(BlockState state, String filter) {
		if (filter == null || filter.isEmpty()) return true;
		return oreTokenFromState(state).equals(filter);
	}

	// Convert block id like:
	// minecraft:iron_ore -> iron
	// minecraft:deepslate_iron_ore -> iron
	// mod:tin_ore -> tin
	private static String oreTokenFromState(BlockState state) {
		var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (key == null) return "";

		String path = key.getPath(); // e.g. deepslate_copper_ore
		if (!path.contains("ore")) return "";

		path = path.replace("deepslate_", "");
		path = path.replace("nether_", "");
		path = path.replace("end_", "");

		if (path.endsWith("_ore")) {
			return path.substring(0, path.length() - 4); // remove "_ore"
		}

		int idx = path.indexOf("_ore");
		if (idx > 0) return path.substring(0, idx);

		return "";
	}

	private static class PosWithDist {
		final BlockPos pos;
		final int dist2;
		PosWithDist(BlockPos pos, int dist2) { this.pos = pos; this.dist2 = dist2; }
	}
}
