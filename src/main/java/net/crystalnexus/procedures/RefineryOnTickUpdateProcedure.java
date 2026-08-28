package net.crystalnexus.procedures;

import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.RefiningRecipe;
import net.crystalnexus.processing.MaterialProcessingCatalog;
import net.crystalnexus.util.MachineUpgradeHelper;
import net.crystalnexus.processing.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class RefineryOnTickUpdateProcedure {
    private static final int ENERGY_PER_OPERATION = 4096;
    private RefineryOnTickUpdateProcedure() {}

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RefineryBlockEntity refinery)) return;
        emptyContainer(refinery);
        ItemStack upgrade = refinery.getItem(2);
        MachineTier machineTier = MachineTier.from(level.getBlockState(pos));
        double cookTime = machineTier.processingTime(MachineUpgradeHelper.cookTime(upgrade,
            upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 75
                : upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 50 : 100));
        int energyCost = machineTier.energyCost(MachineUpgradeHelper.energyCost(upgrade, ENERGY_PER_OPERATION));
        RefiningRecipe recipe = findRecipe(level, refinery, machineTier);
        ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.output();
        if (recipe == null || output.isEmpty() || refinery.getEnergyStorage().getEnergyStored() < energyCost
                || !FluidChemicalReactionChamberOnTickUpdateProcedure.canStackOutput(refinery.getItem(1), output)) {
            setActive(level, pos, false);
            refinery.getPersistentData().putDouble("maxProgress", cookTime);
            sync(level, pos, refinery);
            return;
        }
        setActive(level, pos, true);
        refinery.getPersistentData().putDouble("maxProgress", cookTime);
        double progress = refinery.getPersistentData().getDouble("progress") + 1;
        refinery.getPersistentData().putDouble("progress", progress);
        if (progress < cookTime) { sync(level, pos, refinery); return; }

        refinery.getTank(0).drain(recipe.input().amount(), IFluidHandler.FluidAction.EXECUTE);
        recipe.itemInput().ifPresent(ingredient -> refinery.getItem(0).shrink(1));
        ItemStack produced = output.copy();
        produced.setCount(refinery.getItem(1).getCount() + output.getCount());
        refinery.setItem(1, produced);
        refinery.getEnergyStorage().extractEnergy(energyCost, false);
        refinery.getPersistentData().putDouble("progress", 0);
        sync(level, pos, refinery);
    }

    private static RefiningRecipe findRecipe(ServerLevel level, RefineryBlockEntity refinery, MachineTier machineTier) {
        FluidStack input = refinery.getTank(0).getFluid();
        for (var holder : level.getRecipeManager().getAllRecipesFor(RefiningRecipe.Type.INSTANCE))
            if (holder.value().input().matches(input)
                    && holder.value().itemInput().map(ingredient -> ingredient.test(refinery.getItem(0))).orElse(true))
                return machineTier.supports(holder.value().minimumMachineTier()) ? holder.value() : null;
        if (input.getAmount() < MaterialProcessingCatalog.SLURRY_AMOUNT) return null;
        return MaterialProcessingCatalog.slurryMaterial(input).flatMap(MaterialProcessingCatalog.get(level)::byId)
            .filter(material -> !material.profile().disabledStages().contains("refining"))
            .filter(material -> machineTier.supports(material.profile().minimumMachineTier()))
            .map(material -> new RefiningRecipe(
                new net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe.FluidAmount(
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(input.getFluid()),
                    MaterialProcessingCatalog.SLURRY_AMOUNT, java.util.Optional.of(material.id())),
                java.util.Optional.of(material.dust("crystalnexus", material.profile().advancedMultiplier())),
                java.util.Optional.empty(), material.profile().minimumMachineTier())).orElse(null);
    }

    private static void emptyContainer(RefineryBlockEntity refinery) {
        ItemStack stack = refinery.getItem(0);
        if (stack.getCount() != 1) return;
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return;
        FluidStack offered = item.drain(RefineryBlockEntity.TANK_CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        int accepted = refinery.getTank(0).fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        refinery.getTank(0).fill(item.drain(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        refinery.setItem(0, item.getContainer());
    }

    private static void setActive(ServerLevel level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        int value = active ? 2 : 1;
        if (state.hasProperty(ChemicalReactionChamberBlock.BLOCKSTATE)
                && state.getValue(ChemicalReactionChamberBlock.BLOCKSTATE) != value)
            level.setBlock(pos, state.setValue(ChemicalReactionChamberBlock.BLOCKSTATE, value), 3);
    }
    private static void sync(ServerLevel level, BlockPos pos, RefineryBlockEntity refinery) {
        refinery.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }
}
