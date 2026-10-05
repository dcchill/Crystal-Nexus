package net.crystalnexus.procedures;

import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.config.CrystalnexusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.energy.IEnergyStorage;

import net.crystalnexus.block.entity.CrystalCrusherBlockEntity;

public class CrystalCrusherOnTickUpdateProcedure {
	private static final int ENERGY_PER_OPERATION = 4096;
	private static final int MAX_OUTPUT = 16;

	public static void execute(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level) || level.isClientSide()) return;
        var profiler = net.crystalnexus.commands.NexusDebugCommand.profiler(level);
        profiler.push("crystalnexus_crusher_processing");
        try { process(level, x, y, z); }
        finally { profiler.pop(); }
    }

    private static void process(Level world, double x, double y, double z) {
        Level level = world;
		BlockPos pos = BlockPos.containing(x, y, z);
		if (!(world.getBlockEntity(pos) instanceof CrystalCrusherBlockEntity crusher)) return;
        IEnergyStorage energy = crusher.getEnergyStorage();
		setMachineState(world, pos, net.crystalnexus.util.MachineAnimationHelper.shouldIdle(world, pos, crusher.getPersistentData().getDouble("progress")) ? 1 : 2);

		var upgrade = MachineUpgradeHelper.upgrades(crusher, 2, 3);
		MachineTier machineTier = MachineTier.from(world.getBlockState(pos));
		double baseCookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);
		double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseCookTime));
		int energyCost = machineTier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
		crusher.machineSync.setDouble("maxProgress", cookTime);

		ItemStack input = crusher.getItem(0);
        var assigned = net.crystalnexus.assembly.AssemblyLineMachine.assignedRecipe(crusher);
		ItemStack result;
		if (assigned == null) {
			result = net.crystalnexus.util.CrushingRecipeSupport.findResult(level, input, machineTier);
		} else {
			result = level.getRecipeManager().byKey(assigned)
				.filter(h -> h.value() instanceof net.crystalnexus.jei_recipes.OreCrushingJeiRecipe r
					&& machineTier.supports(r.minimumMachineTier()) && !r.getIngredients().isEmpty()
					&& r.getIngredients().getFirst().test(input))
				.map(h -> h.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
		}
		int outputCount = Math.min(MAX_OUTPUT, result.getCount());
		ItemStack currentOutput = crusher.getItem(1);
		boolean outputFits = outputCount > 0
				&& (currentOutput.isEmpty() || ItemStack.isSameItemSameComponents(currentOutput, result))
				&& currentOutput.getCount() + outputCount <= result.getMaxStackSize();

		int requiredEnergy = assigned != null ? energyCost : Math.min(energyCost, CrystalnexusConfig.MACHINES.CRYSTAL_CRUSHER.maxExtract());
		if (result.isEmpty() || energy.getEnergyStored() < requiredEnergy || !outputFits)
			return;

		double progress = crusher.getPersistentData().getDouble("progress") + 1;
		crusher.machineSync.setDouble("progress", progress);
		if (world instanceof ServerLevel serverLevel && net.crystalnexus.util.MachineSync.isUpdateTick(serverLevel.getGameTime(), pos))
			serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
					x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);

		if (progress >= cookTime) {
            int crafts = Math.min(MachineUpgradeHelper.parallelCraftCount(upgrade), input.getCount());
            crafts = Math.min(crafts, (result.getMaxStackSize() - currentOutput.getCount()) / outputCount);
            crafts = Math.min(crafts, energy.getEnergyStored() / requiredEnergy);
            var profiler = net.crystalnexus.commands.NexusDebugCommand.profiler(level);
            profiler.push("crystalnexus_inventory");
            try {
			ItemStack produced = result.copy();
			produced.setCount(currentOutput.getCount() + outputCount * crafts);
			crusher.setItem(1, produced);
			ItemStack remainingInput = crusher.getItem(0).copy();
			remainingInput.shrink(crafts);
			crusher.setItem(0, remainingInput);
			crusher.machineSync.setDouble("progress", 0);
            } finally { profiler.pop(); }

            if (assigned != null) net.crystalnexus.assembly.AssemblyLineMachine.consumeAssignedEnergy(energy, energyCost * crafts);
            else for (int craft = 0; craft < crafts; craft++) energy.extractEnergy(energyCost, false);
		}

    }

	private static void setMachineState(LevelAccessor world, BlockPos pos, int value) {
		BlockState state = world.getBlockState(pos);
		if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property
				&& property.getPossibleValues().contains(value) && state.getValue(property) != value)
			world.setBlock(pos, state.setValue(property, value), 2);
	}

	public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
		if (level instanceof ILevelExtension extension) {
			IEnergyStorage energy = extension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
			if (energy != null)
				return energy.getEnergyStored();
		}
		return 0;
	}
}
