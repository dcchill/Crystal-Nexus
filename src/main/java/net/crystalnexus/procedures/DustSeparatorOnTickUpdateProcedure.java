package net.crystalnexus.procedures;

import net.crystalnexus.block.entity.DustSeparatorBlockEntity;
import net.crystalnexus.jei_recipes.DustSeperationRecipe;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.config.CrystalnexusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class DustSeparatorOnTickUpdateProcedure {
    private static final int ENERGY_PER_OPERATION = 4096;

    private DustSeparatorOnTickUpdateProcedure() {}

    public static void execute(LevelAccessor world, double x, double y, double z) {
        var machine = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
        net.crystalnexus.util.MachineUpgradeHelper.processParallel(machine, net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(net.crystalnexus.util.MachineUpgradeHelper.upgrades(machine, 2, 4)), () -> executeSingle(world, x, y, z));
    }

    private static boolean executeSingle(LevelAccessor world, double x, double y, double z) {
        boolean completed = false;
        if (world.isClientSide()) return false;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!(world instanceof Level level) || !(level.getBlockEntity(pos) instanceof DustSeparatorBlockEntity separator))
            return false;
        if (level.isClientSide()) return false;

        var upgrade = MachineUpgradeHelper.upgrades(separator, 2, 4);
        MachineTier machineTier = MachineTier.from(level.getBlockState(pos));
        double baseCookTime = MachineUpgradeHelper.processingTime(upgrade, 100, 75, 50);
        double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade, baseCookTime));
        int energyCost = machineTier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
        separator.getPersistentData().putDouble("maxProgress", cookTime);

        Match match = findMatch(level, separator, machineTier);
        if (match == null) {
            separator.getPersistentData().putDouble("progress", 0);
            separator.getPersistentData().remove("pendingSecondary");
            setActive(level, pos, false);
            sync(level, pos, separator);
            return false;
        }
        if (separator.getEnergyStorage().getEnergyStored() < Math.min(energyCost, CrystalnexusConfig.MACHINES.DUST_SEPARATOR.maxExtract())) {
            setActive(level, pos, false);
            sync(level, pos, separator);
            return false;
        }

        boolean pendingSecondary = separator.getPersistentData().contains("pendingSecondary")
            ? separator.getPersistentData().getBoolean("pendingSecondary")
            : match.secondaryChance() > 0 && level.random.nextFloat() < match.secondaryChance();
        separator.getPersistentData().putBoolean("pendingSecondary", pendingSecondary);
        ItemStack primary = (match.primary()).copy();
        ItemStack secondary = (match.secondary()).copy();
        if (!net.crystalnexus.util.MachineItemStorage.fits(separator, 1, primary)
            || pendingSecondary && !net.crystalnexus.util.MachineItemStorage.fits(separator, 3, secondary)) {
            setActive(level, pos, false);
            sync(level, pos, separator);
            return false;
        }

        setActive(level, pos, true);
        double progress = separator.getPersistentData().getDouble("progress") + 1;
        separator.getPersistentData().putDouble("progress", progress);
        if (progress < cookTime) {
            sync(level, pos, separator);
            return false;
        }

        separator.setItem(1, merged(separator.getItem(1), primary));
        if (pendingSecondary && !match.secondary().isEmpty())
            separator.setItem(3, merged(separator.getItem(3), secondary));
        separator.getItem(0).shrink(match.itemCount());
        separator.getEnergyStorage().extractEnergy(energyCost, false);
        separator.getPersistentData().putDouble("progress", 0);
        completed = true;
        separator.getPersistentData().remove("pendingSecondary");
        sync(level, pos, separator);
        return completed;
    }

    private static Match findMatch(Level level, DustSeparatorBlockEntity separator, MachineTier machineTier) {
        ItemStack input = separator.getItem(0);
        for (RecipeHolder<DustSeperationRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(DustSeperationRecipe.Type.INSTANCE)) {
            DustSeperationRecipe recipe = holder.value();
            boolean itemMatches = recipe.fluidInput().isEmpty() && !recipe.getIngredients().isEmpty()
                && recipe.getIngredients().getFirst().test(input) && input.getCount() >= recipe.inputCount();
            if (itemMatches) {
                if (!machineTier.supports(recipe.minimumMachineTier())) return null;
                ItemStack output = tieredOutput(recipe.getResultItem(level.registryAccess()), machineTier);
                if (!output.isEmpty()) return new Match(output, recipe.secondaryOutput(), recipe.secondaryChance(),
                    recipe.inputCount());
            }
        }

        MaterialProcessingCatalog.Snapshot catalog = MaterialProcessingCatalog.get(level);
        return catalog.dust(input)
            .filter(material -> !material.profile().disabledStages().contains("separation"))
            .filter(material -> machineTier.supports(material.profile().minimumMachineTier()))
                .map(material -> material.nugget(BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace(),
                MaterialProcessingCatalog.nuggetsPerDust(machineTier)))
            .filter(output -> !output.isEmpty())
            .map(output -> new Match(output, ItemStack.EMPTY, 0, 1)).orElse(null);
    }

    private static ItemStack tieredOutput(ItemStack output, MachineTier tier) {
        if (!output.isEmpty() && BuiltInRegistries.ITEM.getKey(output.getItem()).getPath().endsWith("_nugget")) {
            ItemStack adjusted = output.copy();
            adjusted.setCount(MaterialProcessingCatalog.nuggetsPerDust(tier));
            return adjusted;
        }
        return output;
    }


    private static ItemStack merged(ItemStack current, ItemStack output) {
        ItemStack result = output.copy();
        result.setCount(current.getCount() + output.getCount());
        return result;
    }

    private static void setActive(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty property) {
            int value = active ? 2 : 1;
            if (property.getPossibleValues().contains(value) && state.getValue(property) != value)
                level.setBlock(pos, state.setValue(property, value), 3);
        }
    }

    private static void sync(Level level, BlockPos pos, BlockEntity entity) {
        entity.setChanged();
        if (entity.getPersistentData().getDouble("progress") == 0 || level.getGameTime() % 5 == 0)
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }

    public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof DustSeparatorBlockEntity separator
            ? separator.getEnergyStorage().getEnergyStored() : 0;
    }

    private record Match(ItemStack primary, ItemStack secondary, float secondaryChance,
                         int itemCount) {}
}
