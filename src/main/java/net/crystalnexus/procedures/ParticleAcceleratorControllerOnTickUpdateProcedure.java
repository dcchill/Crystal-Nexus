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
import java.util.stream.Collectors;

public class ParticleAcceleratorControllerOnTickUpdateProcedure {

	// ===== CONFIG =====
	private static final int MIN_LEN = 12;
	private static final int MAX_LEN = 64;

	// TOTAL FE drained per tick (split across all magnets)
	private static final int TOTAL_FE_PER_TICK = 5120;

	private static final double BASE_COOK_TIME = 200;

	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);

		// ---- SERVER ONLY ----
		if (world.isClientSide()) return;

		BlockEntity be = world.getBlockEntity(pos);
		if (be == null) return;

		// ---- Facing (MCreator standard) ----
		Direction forward = world.getBlockState(pos)
				.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);

		// =====================================================
		// Scan LINAC
		// =====================================================
		int len = 0;
		int magnetCount = 0;

		BlockPos cur = pos.relative(forward);
		while (len < MAX_LEN) {
			BlockState st = world.getBlockState(cur);

			if (st.getBlock() == CrystalnexusModBlocks.PARTICLE_ACCELERATOR_TUBE.get()) {
				len++;
			} else if (st.getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get()) {
				len++;
				magnetCount++;
			} else {
				break;
			}
			cur = cur.relative(forward);
		}

		boolean formed = len >= MIN_LEN;

		// ---- Debug / GUI data ----
		be.getPersistentData().putDouble("linacLen", len);
		be.getPersistentData().putDouble("magCount", magnetCount);
		be.getPersistentData().putDouble("formed", formed ? 1 : 0);
		be.getPersistentData().putDouble("maxProgress", BASE_COOK_TIME);

if (!formed) {
	be.getPersistentData().putDouble("progress", 0);
	be.getPersistentData().putDouble("reason", 1); // invalid structure
	sync(world, pos);
	return;
}

if (magnetCount < 3) { // <-- MIN MAGNETS HERE
	be.getPersistentData().putDouble("progress", 0);
	be.getPersistentData().putDouble("reason", 2); // not enough magnets
	sync(world, pos);
	return;
}


		// =====================================================
		// Inventory
		// =====================================================
		if (!(world instanceof ILevelExtension ext)) return;
		IItemHandler inv = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (inv == null) return;

		ItemStack input = inv.getStackInSlot(0);
		if (input.isEmpty()) {
			be.getPersistentData().putDouble("progress", 0);
			sync(world, pos);
			return;
		}

		// =====================================================
		// Recipe lookup (same pattern as your crusher)
		// =====================================================
		ItemStack result = ItemStack.EMPTY;

		if (world instanceof Level lvl) {
			List<AcceleratorJeiRecipe> recipes =
				lvl.getRecipeManager()
					.getAllRecipesFor(AcceleratorJeiRecipe.Type.INSTANCE)
					.stream().map(RecipeHolder::value)
					.collect(Collectors.toList());

			for (AcceleratorJeiRecipe r : recipes) {
				NonNullList<Ingredient> ing = r.getIngredients();
				if (!ing.isEmpty() && ing.get(0).test(input)) {
					result = r.getResultItem(null);
					break;
				}
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
		// POWER CHECK — ALL MAGNETS MUST PAY
		// =====================================================
		int per = TOTAL_FE_PER_TICK / magnetCount;
		int rem = TOTAL_FE_PER_TICK % magnetCount;

		be.getPersistentData().putDouble("needPerMag", per);

		// Pass 1: simulate
		cur = pos.relative(forward);
		int seen = 0;
		int minFE = Integer.MAX_VALUE;

		for (int i = 0; i < len; i++) {
			BlockState st = world.getBlockState(cur);
			if (st.getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get()) {
				int cost = per + (seen < rem ? 1 : 0);
				seen++;

				IEnergyStorage es = getEnergyStorageAnySide(world, cur);
				int stored = es == null ? 0 : es.getEnergyStored();
				minFE = Math.min(minFE, stored);

				if (es == null || es.extractEnergy(cost, true) < cost) {
					be.getPersistentData().putDouble("stalled", 1);
					be.getPersistentData().putDouble("stallNeed", cost);
					be.getPersistentData().putDouble("stallStored", stored);
					be.getPersistentData().putDouble("minMagFE", minFE == Integer.MAX_VALUE ? 0 : minFE);
					sync(world, pos);
					return;
				}
			}
			cur = cur.relative(forward);
		}

		// Pass 2: drain
		cur = pos.relative(forward);
		seen = 0;
		for (int i = 0; i < len; i++) {
			BlockState st = world.getBlockState(cur);
			if (st.getBlock() == CrystalnexusModBlocks.ELECTROMAGNET.get()) {
				int cost = per + (seen < rem ? 1 : 0);
				seen++;
				IEnergyStorage es = getEnergyStorageAnySide(world, cur);
				if (es != null) es.extractEnergy(cost, false);
			}
			cur = cur.relative(forward);
		}

		be.getPersistentData().putDouble("stalled", 0);
		be.getPersistentData().putDouble("minMagFE", minFE == Integer.MAX_VALUE ? 0 : minFE);

		// =====================================================
		// Progress
		// =====================================================
		double progress = be.getPersistentData().getDouble("progress");
		double speed = 1 + Math.min(magnetCount, 15);

		progress += speed;
		be.getPersistentData().putDouble("progress", progress);

		// =====================================================
		// Craft
		// =====================================================
		if (progress >= BASE_COOK_TIME) {
			if (inv instanceof IItemHandlerModifiable mod) {
				ItemStack newOut = result.copy();
				newOut.setCount(out.isEmpty() ? 1 : out.getCount() + 1);
				mod.setStackInSlot(1, newOut);

				ItemStack in2 = input.copy();
				in2.shrink(1);
				mod.setStackInSlot(0, in2);
			}
			be.getPersistentData().putDouble("progress", 0);
		}

		sync(world, pos);
	}

	// =====================================================
	// Helpers
	// =====================================================
	private static IEnergyStorage getEnergyStorageAnySide(LevelAccessor world, BlockPos pos) {
		if (!(world instanceof ILevelExtension ext)) return null;

		IEnergyStorage es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		if (es != null) return es;

		for (Direction d : Direction.values()) {
			es = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d);
			if (es != null) return es;
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
