package net.crystalnexus.procedures;

import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.crystalnexus.jei_recipes.AcceleratorJeiRecipe;
import net.crystalnexus.init.CrystalnexusModBlocks;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ParticleAcceleratorControllerOnTickUpdateProcedure {

	// ===== CONFIG =====
	private static final int MIN_LEN = 5;
	private static final int MAX_LEN = 64;

	private static final int MIN_MAGNETS = 1;

	// TOTAL FE drained per tick (split across all magnets)
	private static final int TOTAL_FE_PER_TICK = 5120;

	private static final double BASE_COOK_TIME = 3500;     // time at MIN_MAGNETS
	private static final double MIN_COOK_TIME  = 100;     // hard floor
	private static final double MAGNET_EFFICIENCY = 0.25;   // 1.0 = strong effect, 0.5 = weaker

	// Input slots (unordered)
	private static final int[] INPUT_SLOTS = new int[] {0, 2, 3, 4};

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);

		// ---- SERVER ONLY ----
		if (world.isClientSide()) return;

		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) return;

		Direction forward = world.getBlockState(pos)
			.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

		// =====================================================
		// Scan: linear OR ring (corners supported)
		// =====================================================
		PathScanResult scan = scanPath(world, pos, forward, MIN_LEN, MAX_LEN);

		int len = scan.len();
		int magnetCount = scan.magnetCount();
		boolean formed = scan.ok();
		boolean ringMode = scan.ringMode();
		ArrayList<BlockPos> magnets = scan.magnets();

		// ---- Store for GUI + renderer ----
		be.getPersistentData().putDouble("linacLen", len);
		be.getPersistentData().putDouble("magCount", magnetCount);
		be.getPersistentData().putDouble("formed", formed ? 1 : 0);
		be.getPersistentData().putDouble("ringMode", ringMode ? 1 : 0);
		be.getPersistentData().putDouble("minMagReq", MIN_MAGNETS);

		// Cook time shrinks with magnets (diminishing returns)
		double effectiveMagnets = Math.max(0, magnetCount - MIN_MAGNETS);
		double cookTime = BASE_COOK_TIME / (1.0 + (effectiveMagnets * MAGNET_EFFICIENCY));
		cookTime = Math.max(MIN_COOK_TIME, cookTime);
		be.getPersistentData().putDouble("maxProgress", cookTime);

		// reset default status flags each tick
		be.getPersistentData().putDouble("reason", 0);
		be.getPersistentData().putDouble("stalled", 0);
		be.getPersistentData().putDouble("stallNeed", 0);
		be.getPersistentData().putDouble("stallStored", 0);

		if (!formed) {
			be.getPersistentData().putDouble("progress", 0);
			be.getPersistentData().putDouble("reason", 1); // invalid structure
			sync(world, pos);
			return;
		}

		if (magnetCount < MIN_MAGNETS) {
			be.getPersistentData().putDouble("progress", 0);
			be.getPersistentData().putDouble("reason", 2); // not enough magnets
			sync(world, pos);
			return;
		}

		// =====================================================
		// Inventory (4 unordered inputs)
		// Slots: 0,2,3,4  Output: 1
		// =====================================================
		if (!(world instanceof ILevelExtension ext)) return;
		IItemHandler inv = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (inv == null) return;

		ArrayList<ItemStack> inputs = new ArrayList<>();
		for (int s : INPUT_SLOTS) {
			ItemStack st = inv.getStackInSlot(s);
			if (st.isEmpty()) {
				be.getPersistentData().putDouble("progress", 0);
				sync(world, pos);
				return;
			}
			inputs.add(st);
		}

		// =====================================================
		// Recipe lookup (4 ingredients, order-independent)
		// =====================================================
		ItemStack result = ItemStack.EMPTY;

		if (world instanceof Level lvl) {
			List<AcceleratorJeiRecipe> recipes =
				lvl.getRecipeManager()
					.getAllRecipesFor(AcceleratorJeiRecipe.Type.INSTANCE)
					.stream()
					.map(RecipeHolder::value)
					.collect(Collectors.toList());

			recipeLoop:
			for (AcceleratorJeiRecipe r : recipes) {
				NonNullList<Ingredient> ing = r.getIngredients();
				if (ing.size() != 4) continue;

				boolean[] used = new boolean[inputs.size()];

				for (Ingredient recipeIng : ing) {
					boolean matched = false;

					for (int i = 0; i < inputs.size(); i++) {
						if (used[i]) continue;
						if (recipeIng.test(inputs.get(i))) {
							used[i] = true;
							matched = true;
							break;
						}
					}

					if (!matched) continue recipeLoop;
				}

				result = r.getResultItem(null);
				break;
			}
		}

		if (result.isEmpty()) {
			be.getPersistentData().putDouble("progress", 0);
			sync(world, pos);
			return;
		}

		// =====================================================
		// Output slot check
		// =====================================================
		ItemStack out = inv.getStackInSlot(1);
		if (!out.isEmpty() && out.getItem() != result.getItem()) return;
		if (!out.isEmpty() && out.getCount() >= 64) return;

		// =====================================================
		// POWER: ALL MAGNETS MUST PAY
		// =====================================================
		int per = TOTAL_FE_PER_TICK / magnetCount;
		int rem = TOTAL_FE_PER_TICK % magnetCount;

		// simulate pass (must be extractable)
		for (int i = 0; i < magnets.size(); i++) {
			int cost = per + (i < rem ? 1 : 0);
			IEnergyStorage es = getDrainableEnergyStorage(world, magnets.get(i), cost);
			if (es == null) {
				be.getPersistentData().putDouble("stalled", 1);
				be.getPersistentData().putDouble("stallNeed", cost);
				be.getPersistentData().putDouble("stallStored", 0);
				sync(world, pos);
				return;
			}
		}

		// drain pass
		for (int i = 0; i < magnets.size(); i++) {
			int cost = per + (i < rem ? 1 : 0);
			IEnergyStorage es = getDrainableEnergyStorage(world, magnets.get(i), cost);
			if (es != null) es.extractEnergy(cost, false);
		}

		// =====================================================
		// Progress + Craft
		// =====================================================
		double progress = be.getPersistentData().getDouble("progress");
		cookTime = be.getPersistentData().getDouble("maxProgress");

		progress += 1.0;
		be.getPersistentData().putDouble("progress", progress);

		if (progress >= cookTime) {
			if (inv instanceof IItemHandlerModifiable mod) {
				ItemStack newOut = result.copy();
				newOut.setCount(out.isEmpty() ? 1 : out.getCount() + 1);
				mod.setStackInSlot(1, newOut);

				for (int s : INPUT_SLOTS) {
					ItemStack st = mod.getStackInSlot(s).copy();
					st.shrink(1);
					mod.setStackInSlot(s, st);
				}
			}
			be.getPersistentData().putDouble("progress", 0);
		}

		sync(world, pos);
	}

	// =====================================================
	// SCAN SUPPORT (linear OR ring, corners supported)
	// =====================================================
	private record PathScanResult(boolean ok, boolean ringMode, int len, int magnetCount, ArrayList<BlockPos> magnets) {}

	private static boolean isTubeOrMagnet(LevelAccessor world, BlockPos p) {
		BlockState st = world.getBlockState(p);
		return st.getBlock() == CrystalnexusModBlocks.PARTICLE_ACCELERATOR_TUBE.get()
			|| st.getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get();
	}

	private static boolean isMagnet(LevelAccessor world, BlockPos p) {
		return world.getBlockState(p).getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get();
	}

	private static PathScanResult scanPath(LevelAccessor world, BlockPos controllerPos, Direction facing, int minLen, int maxLen) {
		// ---- 1) Linear scan forward ----
		ArrayList<BlockPos> mags = new ArrayList<>();
		int len = 0;

		BlockPos cur = controllerPos.relative(facing);
		while (len < maxLen) {
			if (!isTubeOrMagnet(world, cur)) break;
			if (isMagnet(world, cur)) mags.add(cur.immutable());
			len++;
			cur = cur.relative(facing);
		}
		if (len >= minLen) {
			return new PathScanResult(true, false, len, mags.size(), mags);
		}

		// ---- 2) Ring scan (horizontal loop, corners supported) ----
		Direction[] candidates = new Direction[] {
			facing,
			facing.getClockWise(),
			facing.getCounterClockWise(),
			facing.getOpposite(),
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
		};

		BlockPos startSeg = null;
		Direction startDir = null;

		for (Direction d : candidates) {
			BlockPos p = controllerPos.relative(d);
			if (isTubeOrMagnet(world, p)) {
				startSeg = p;
				startDir = d;
				break;
			}
		}
		if (startSeg == null || startDir == null) {
			return new PathScanResult(false, false, 0, 0, new ArrayList<>());
		}

		HashSet<Long> visited = new HashSet<>();
		ArrayList<BlockPos> ringMags = new ArrayList<>();

		BlockPos pos = startSeg;
		Direction dir = startDir;

		for (int steps = 0; steps < maxLen; steps++) {
			if (pos.getY() != controllerPos.getY()) {
				return new PathScanResult(false, true, 0, 0, new ArrayList<>());
			}

			if (!isTubeOrMagnet(world, pos)) {
				return new PathScanResult(false, true, 0, 0, new ArrayList<>());
			}

			long key = pos.asLong();
			if (visited.contains(key)) {
				return new PathScanResult(false, true, 0, 0, new ArrayList<>());
			}
			visited.add(key);

			if (isMagnet(world, pos)) ringMags.add(pos.immutable());

			Direction[] horiz = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

			Direction nextDir = null;
			int options = 0;

			for (Direction d : horiz) {
				if (d == dir.getOpposite()) continue;

				BlockPos np = pos.relative(d);

				if (np.equals(controllerPos) && steps + 1 >= minLen) {
					nextDir = d;
					options = 1;
					break;
				}

				if (isTubeOrMagnet(world, np) && !visited.contains(np.asLong())) {
					nextDir = d;
					options++;
				}
			}

			if (options != 1 || nextDir == null) {
				return new PathScanResult(false, true, 0, 0, new ArrayList<>());
			}

			if (pos.relative(nextDir).equals(controllerPos)) {
				int finalLen = steps + 1;
				return new PathScanResult(true, true, finalLen, ringMags.size(), ringMags);
			}

			dir = nextDir;
			pos = pos.relative(nextDir);
		}

		return new PathScanResult(false, true, 0, 0, new ArrayList<>());
	}

	// =====================================================
	// Energy: pick a capability that can actually extract
	// =====================================================
	private static IEnergyStorage getDrainableEnergyStorage(LevelAccessor world, BlockPos pos, int cost) {
		if (!(world instanceof ILevelExtension ext)) return null;

		IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (es != null && es.canExtract() && es.extractEnergy(cost, true) >= cost) return es;

		for (Direction d : Direction.values()) {
			es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
			if (es != null && es.canExtract() && es.extractEnergy(cost, true) >= cost) return es;
		}

		return null;
	}

	private static void sync(LevelAccessor world, BlockPos pos) {
		if (world instanceof Level lvl) {
			BlockState bs = world.getBlockState(pos);
			lvl.sendBlockUpdated(pos, bs, bs, 3);
		}
	}
}
