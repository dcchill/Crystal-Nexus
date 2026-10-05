package net.crystalnexus.procedures;

import net.crystalnexus.block.entity.EnergyExtractorBlockEntity;
import net.crystalnexus.jei_recipes.EnergyExtractionRecipe;
import net.crystalnexus.util.EeMatterEconomy;
import net.crystalnexus.util.MachineAnimationHelper;
import net.crystalnexus.util.MachineSync;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.capabilities.Capabilities;

public class EnergyExtractorOnTickUpdateProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        int crafts = machine instanceof net.minecraft.world.Container inventory && inventory.getContainerSize() > 1 ? net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(inventory.getItem(1)) : 1;
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, crafts, () -> executeSingle(world, x, y, z));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
        if (!(world instanceof Level level) || level.isClientSide()) return false;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!(level.getBlockEntity(pos) instanceof EnergyExtractorBlockEntity machine)) return false;
        var energy = machine.getEnergyStorage();
        var data = machine.getPersistentData();
        var state = machine.getBlockState();
        int animation = MachineAnimationHelper.shouldIdle(world, pos, data.getDouble("progress")) ? 1 : 2;
        if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property
                && property.getPossibleValues().contains(animation) && state.getValue(property) != animation)
            level.setBlock(pos, state.setValue(property, animation), 3);

        int energyBase = EeMatterEconomy.EXTRACTION_FE_PER_ITEM;
        double cookTime = MachineUpgradeHelper.processingTime(machine.getItem(1), 200, 175, 100);
        machine.machineSync.setDouble("maxProgress", cookTime);
        ItemStack input = machine.getItem(0);
        ItemStack result = ItemStack.EMPTY;
        for (var holder : level.getRecipeManager().getAllRecipesFor(EnergyExtractionRecipe.Type.INSTANCE)) {
            if (holder.value().getIngredients().get(0).test(input)) {
                result = holder.value().getResultItem(null);
                break;
            }
        }
        boolean particles = MachineSync.isUpdateTick(level.getGameTime(), pos);
        if (!result.isEmpty()) {
            ItemStack output = machine.getItem(2);
            if (energy.getMaxEnergyStored() - energy.getEnergyStored() < energyBase
                    || output.getCount() >= result.getMaxStackSize()
                    || !(output.isEmpty() || ItemStack.isSameItemSameComponents(output, result))) return false;
            double progress = data.getDouble("progress");
            if (progress < cookTime) {
                machine.machineSync.setDouble("progress", ++progress);
                if (particles && level instanceof ServerLevel server)
                    server.sendParticles(ParticleTypes.DRAGON_BREATH, x + 0.5, y + 0.5, z + 0.5, 1, 0.25, 0, 0.25, 0);
            }
            if (progress >= cookTime) {
                ItemStack produced = result.copy();
                produced.setCount(output.getCount() + 1);
                machine.setItem(2, produced);
                ItemStack remaining = input.copy();
                remaining.shrink(1);
                machine.setItem(0, remaining);
                if (energy.generateEnergy(energyBase, true) == energyBase)
                    energy.generateEnergy(energyBase, false);
                machine.machineSync.setDouble("progress", 0);
        completed = true;
            }
        } else {
            var battery = input.getCapability(Capabilities.EnergyStorage.ITEM);
            if (battery != null && battery.canExtract()) {
                int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
                int request = Math.min(Math.max(1, energyBase / 8), room);
                if (request > 0) {
                    int simulated = battery.extractEnergy(request, true);
                    int move = Math.min(simulated, energy.generateEnergy(simulated, true));
                    if (move > 0) {
                        int pulled = battery.extractEnergy(move, false);
                        if (pulled > 0) {
                            energy.generateEnergy(pulled, false);
                            machine.setItem(0, input);
                            if (particles && level instanceof ServerLevel server)
                                server.sendParticles(ParticleTypes.END_ROD, x + 0.5, y + 0.6, z + 0.5, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
            }
            machine.machineSync.setDouble("progress", 0);
        }

        return completed;
    }
}
