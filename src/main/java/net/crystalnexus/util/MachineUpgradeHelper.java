package net.crystalnexus.util;

import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.processing.MachineTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import java.util.ArrayList;
import java.util.List;

public final class MachineUpgradeHelper {
	private static final double MIN_MULTIPLIER = 0.05;
	private static final double MAX_MULTIPLIER = 10.0;
	
	private static final double[] STACK_WEIGHTS = {
		0, 1, 2, 2.875, 3.75, 4.5, 5.25, 5.875, 6.5,
		7, 7.5, 7.875, 8.25, 8.5625, 8.875, 9.125, 9.375
	};

	private MachineUpgradeHelper() {
	}

    public static boolean isMachineUpgrade(ItemStack stack) {
        return stack.is(ItemTags.create(ResourceLocation.parse("crystalnexus:machine_upgrades")));
    }

    public static int upgradeStackLimit(ItemStack stack) {
        if (isZeroChip(stack) || stack.is(CrystalnexusModItems.RARE_SSD.get())
                || stack.is(CrystalnexusModItems.EPIC_SSD.get())
                || stack.is(CrystalnexusModItems.REACTOR_UPGRADE.get())
                || stack.is(CrystalnexusModItems.REACTOR_UPGRADE_PERMAFROST.get())) return 1;
        if (isParallelizationChip(stack)) return 4;
        return isStackableUpgrade(stack) ? 16 : stack.getMaxStackSize();
    }

    public static int upgradeSlots(BlockState state) {
        if (state.getBlock() instanceof net.crystalnexus.block.MegaChemicalReactionChamberBlock
                || state.getBlock() instanceof net.crystalnexus.block.CryogenicFlashFreezerHatchBlock) return 3;
        if (state.getBlock() instanceof net.crystalnexus.block.MatterTransmutationTableBlock
                || state.getBlock() instanceof net.crystalnexus.block.ParticleAcceleratorControllerBlock) return 4;
        return MachineTier.from(state).upgradeSlots();
    }

    public static boolean acceptsUpgrade(BlockState state, int ordinal, ItemStack stack) {
        return ordinal >= 0 && ordinal < upgradeSlots(state)
            && isMachineUpgrade(stack);
    }

    // Extra slots are appended so existing saves keep their input/output indices.
    public static List<ItemStack> upgrades(BlockEntity machine, int firstSlot, int extraStart) {
        List<ItemStack> upgrades = new ArrayList<>();
        if (machine instanceof Container inventory) {
            int count = upgradeSlots(machine.getBlockState());
            for (int i = 0; i < count; i++) {
                int slot = i == 0 ? firstSlot : extraStart + i - 1;
                if (slot < inventory.getContainerSize()) {
                    ItemStack upgrade = inventory.getItem(slot);
                    if (isZeroChip(upgrade) && (machine.getBlockState().getBlock() instanceof net.crystalnexus.block.MatterTransmutationTableBlock
                            || machine.getBlockState().getBlock() instanceof net.crystalnexus.block.CraftingFactoryBlock)) continue;
                    upgrades.add(upgrade);
                }
            }
        }
        return upgrades;
    }

    public static double processingTime(List<ItemStack> upgrades, double base, double basic, double carbon) {
        double speed = 1.0;
        for (ItemStack upgrade : upgrades) speed += base / processingTime(upgrade, base, basic, carbon) - 1.0;
        return base / speed;
    }

    public static double cookTime(List<ItemStack> upgrades, double base) {
        for (ItemStack upgrade : upgrades) base *= cookMultiplier(upgrade);
        return Math.max(1.0, base);
    }

    public static int energyCost(List<ItemStack> upgrades, int baseCost) {
        double efficiency = 1.0;
        for (ItemStack upgrade : upgrades) efficiency += feEfficiency(upgrade) - 1.0;
        return Math.max(1, (int) Math.ceil(baseCost / Math.max(MIN_MULTIPLIER, efficiency)));
    }

    public static int energyCost(BlockState state, List<ItemStack> upgrades, int baseCost) {
        return MachineTier.from(state).energyCost(energyCost(upgrades, baseCost));
    }

    public static int parallelCraftCount(List<ItemStack> upgrades) {
        long additive = 0, multiplier = 1;
        for (ItemStack upgrade : upgrades) {
            if (isParallelizationChip(upgrade)) additive = Math.min(Integer.MAX_VALUE, additive + 2L * upgrade.getCount());
            if (isZeroChip(upgrade)) multiplier = Math.min(Integer.MAX_VALUE, multiplier * 2);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, additive) * multiplier);
    }

    public static int parallelEnergyCapacity(BlockState state, int configuredCapacity, int energyPerCraft) {
        int slots = upgradeSlots(state);
        // Four parallel chips give eight crafts; each remaining Zero Chip doubles the batch.
        int maxCrafts = slots == 0 ? 1 : 8 << (slots - 1);
        return (int) Math.min(Integer.MAX_VALUE, Math.max((long) configuredCapacity, (long) energyPerCraft * maxCrafts));
    }

    public static boolean isZeroChip(ItemStack stack) {
        return stack.is(CrystalnexusModItems.ZERO_CHIP.get());
    }

    public static boolean isOutputUpgrade(ItemStack stack) {
        return isParallelizationChip(stack) || isZeroChip(stack);
    }

    public static void processParallel(BlockEntity machine, int crafts, java.util.function.BooleanSupplier cycle) {
        if (!cycle.getAsBoolean() || machine == null) return;
        // Reuse each machine's recipe/resource checks for every additional operation.
        for (int craft = 1; craft < crafts; craft++) {
            var data = machine.getPersistentData();
            data.putDouble("progress", data.getDouble("maxProgress"));
            boolean completed;
            try { completed = cycle.getAsBoolean(); }
            finally { data.putDouble("progress", 0); machine.setChanged(); }
            if (!completed) break;
        }
    }

	public static boolean isStackableUpgrade(ItemStack stack) {
		return isZeroChip(stack) || stack.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.RANGE_UPGRADE.get())
				|| stack.is(CrystalnexusModItems.CARBON_RANGE_UPGRADE.get());
	}

	public static boolean isParallelizationChip(ItemStack stack) {
		return stack.is(CrystalnexusModItems.PARALLELIZATION_CHIP.get());
	}

	public static int parallelCraftCount(ItemStack upgrade) {
		return parallelCraftCount(List.of(upgrade));
	}

	public static double stackWeight(ItemStack upgrade) {
		return weightForCount(upgrade.getCount());
	}

	static double weightForCount(int count) {
		return STACK_WEIGHTS[Math.clamp(count, 0, 16)];
	}

	public static double scaledEffect(ItemStack upgrade, double base, double singleUpgrade) {
		return base + (singleUpgrade - base) * stackWeight(upgrade);
	}

	public static double processingTime(ItemStack upgrade, double base, double basic, double carbon) {
		double single = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? basic
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? carbon : base;
		return scaledProcessingTime(base, single, stackWeight(upgrade));
	}

	static double scaledProcessingTime(double base, double single, double weight) {
		return base / (1.0 + (base / single - 1.0) * weight);
	}

	public static double generatorCycleTime(ItemStack upgrade, double base, double basic, double carbon) {
		double single = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? basic
				: upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? carbon : base;
		return scaledEffect(upgrade, base, single);
	}

	public static double feEfficiency(ItemStack upgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, 1.5);
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, 2.0);
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("fe_efficiency")
				? Math.clamp(data.getDouble("fe_efficiency"), MIN_MULTIPLIER, MAX_MULTIPLIER)
				: 1.0;
	}

	public static int energyCost(ItemStack upgrade, int baseCost) {
		return Math.max(1, (int) Math.ceil(baseCost / feEfficiency(upgrade)));
	}

	public static int energyCost(BlockState state, ItemStack upgrade, int baseCost) {
		return MachineTier.from(state).energyCost(energyCost(upgrade, baseCost));
	}

	public static double generatorEfficiency(ItemStack upgrade, double basicUpgrade, double carbonUpgrade) {
		if (upgrade.is(CrystalnexusModItems.FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, basicUpgrade);
		if (upgrade.is(CrystalnexusModItems.CARBON_FE_EFFICIENCY_UPGRADE.get()))
			return scaledEffect(upgrade, 1.0, carbonUpgrade);
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("fe_efficiency")
				? Math.clamp(data.getDouble("fe_efficiency"), MIN_MULTIPLIER, MAX_MULTIPLIER)
				: 1.0;
	}

	public static double cookTime(ItemStack upgrade, double baseCookTime) {
		return Math.max(1.0, baseCookTime * cookMultiplier(upgrade));
	}

	public static double cookMultiplier(ItemStack upgrade) {
		CompoundTag data = customData(upgrade);
		return data != null && data.contains("cook_mult") ? clampCookMultiplier(data.getDouble("cook_mult")) : 1.0;
	}

	public static double generatorSpeed(ItemStack upgrade) {
		return 1.0 / cookMultiplier(upgrade);
	}

	static double clampCookMultiplier(double multiplier) {
		return Math.clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
	}

	private static CompoundTag customData(ItemStack upgrade) {
		CustomData data = upgrade.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}
}
