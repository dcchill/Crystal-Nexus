package net.crystalnexus.procedures;

import net.crystalnexus.block.ChemicalReactionChamberBlock;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.FluidChemicalReactionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class FluidChemicalReactionChamberOnTickUpdateProcedure {
    private static final int ENERGY_PER_REACTION = 4096;
    private FluidChemicalReactionChamberOnTickUpdateProcedure() {}

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity chamber)) return;
        transferContainers(chamber);

        ItemStack upgrade = chamber.getItem(3);
        double cookTime = upgrade.is(CrystalnexusModItems.ACCELERATION_UPGRADE.get()) ? 75
            : upgrade.is(CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get()) ? 50 : 100;
        double outputMultiplier = 1;
        if (upgrade.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = upgrade.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data == null ? null : data.copyTag();
            if (tag != null) {
                if (tag.contains("cook_mult")) cookTime *= Math.max(.05, Math.min(10, tag.getDouble("cook_mult")));
                if (tag.contains("output_mult")) outputMultiplier = Math.max(0, Math.min(10, tag.getDouble("output_mult")));
            }
        }
        cookTime = Math.max(1, cookTime);
        RecipeMatch match = findRecipe(level, chamber);
        if (match == null || chamber.getEnergyStorage().getEnergyStored() < ENERGY_PER_REACTION) {
            setActive(level, pos, false);
            if (level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity current) {
                current.getPersistentData().putDouble("maxProgress", cookTime);
                sync(level, pos, current);
            }
            return;
        }
        FluidChemicalReactionRecipe recipe = match.recipe();

        FluidStack fluidOutput = recipe.fluidOutput().map(FluidChemicalReactionRecipe.FluidAmount::stack).orElse(FluidStack.EMPTY);
        if (!fluidOutput.isEmpty())
            fluidOutput.setAmount(Math.max(1, (int) Math.floor(fluidOutput.getAmount() * outputMultiplier)));
        ItemStack itemOutput = recipe.itemOutput().orElse(ItemStack.EMPTY);
        if (!itemOutput.isEmpty())
            itemOutput.setCount(Math.max(1, (int) Math.floor(itemOutput.getCount() * outputMultiplier)));
        if ((!fluidOutput.isEmpty() && chamber.getTank(2).fill(fluidOutput, IFluidHandler.FluidAction.SIMULATE) != fluidOutput.getAmount())
                || (!itemOutput.isEmpty() && !canStackOutput(chamber.getItem(2), itemOutput))) {
            setActive(level, pos, false);
            return;
        }

        setActive(level, pos, true);
        if (!(level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity activeChamber)) return;
        activeChamber.getPersistentData().putDouble("maxProgress", cookTime);
        double progress = activeChamber.getPersistentData().getDouble("progress");
        progress++;
        activeChamber.getPersistentData().putDouble("progress", progress);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
            1, .25, 0, .25, 0);
        if (progress < cookTime) {
            sync(level, pos, activeChamber);
            return;
        }

        for (int input = 0; input < 2; input++) {
            int recipeInput = input;
            recipe.fluidInput(input).ifPresent(fluid -> activeChamber.getTank(match.fluidSlot(recipeInput))
                .drain(fluid.amount(), IFluidHandler.FluidAction.EXECUTE));
            recipe.itemInput(input).ifPresent(item -> activeChamber.getItem(match.itemSlot(recipeInput)).shrink(1));
        }
        if (!fluidOutput.isEmpty()) activeChamber.getTank(2).fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        if (!itemOutput.isEmpty()) {
            ItemStack produced = itemOutput.copy();
            produced.setCount(activeChamber.getItem(2).getCount() + itemOutput.getCount());
            activeChamber.setItem(2, produced);
        }
        activeChamber.getEnergyStorage().extractEnergy(ENERGY_PER_REACTION, false);
        activeChamber.getPersistentData().putDouble("progress", 0);
        setActive(level, pos, false);
        if (level.getBlockEntity(pos) instanceof FluidChemicalReactionChamberBlockEntity current) sync(level, pos, current);
    }

    private static RecipeMatch findRecipe(ServerLevel level, FluidChemicalReactionChamberBlockEntity chamber) {
        for (var holder : level.getRecipeManager().getAllRecipesFor(FluidChemicalReactionRecipe.Type.INSTANCE)) {
            RecipeMatch match = match(holder.value(), chamber);
            if (match != null) return match;
        }
        return null;
    }

    private static RecipeMatch match(FluidChemicalReactionRecipe recipe, FluidChemicalReactionChamberBlockEntity chamber) {
        boolean fluidsStraight = fluidMatches(recipe, 0, chamber, 0) && fluidMatches(recipe, 1, chamber, 1);
        boolean fluidsSwapped = fluidMatches(recipe, 0, chamber, 1) && fluidMatches(recipe, 1, chamber, 0);
        if (!fluidsStraight && !fluidsSwapped) return null;

        boolean itemsStraight = itemMatches(recipe, 0, chamber, 0) && itemMatches(recipe, 1, chamber, 1);
        boolean itemsSwapped = itemMatches(recipe, 0, chamber, 1) && itemMatches(recipe, 1, chamber, 0);
        if (!itemsStraight && !itemsSwapped) return null;

        return new RecipeMatch(recipe, fluidsStraight ? 0 : 1, fluidsStraight ? 1 : 0,
            itemsStraight ? 0 : 1, itemsStraight ? 1 : 0);
    }

    private static boolean fluidMatches(FluidChemicalReactionRecipe recipe, int input,
                                        FluidChemicalReactionChamberBlockEntity chamber, int tank) {
        FluidStack stored = chamber.getTank(tank).getFluid();
        var required = recipe.fluidInput(input);
        return required.isEmpty() ? stored.isEmpty()
            : !stored.isEmpty() && stored.is(required.get().stack().getFluid())
                && stored.getAmount() >= required.get().amount();
    }

    private static boolean itemMatches(FluidChemicalReactionRecipe recipe, int input,
                                       FluidChemicalReactionChamberBlockEntity chamber, int slot) {
        return recipe.itemInput(input).map(ingredient -> ingredient.test(chamber.getItem(slot))).orElse(true);
    }

    public static boolean canStackOutput(ItemStack current, ItemStack output) {
        return !output.isEmpty()
            && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, output))
            && current.getCount() + output.getCount() <= output.getMaxStackSize();
    }

    private record RecipeMatch(FluidChemicalReactionRecipe recipe, int fluid0, int fluid1, int item0, int item1) {
        int fluidSlot(int input) { return input == 0 ? fluid0 : fluid1; }
        int itemSlot(int input) { return input == 0 ? item0 : item1; }
    }

    private static void transferContainers(FluidChemicalReactionChamberBlockEntity chamber) {
        for (int slot = 0; slot < 2; slot++) emptyContainer(chamber, slot);
        fillContainer(chamber);
    }

    private static void emptyContainer(FluidChemicalReactionChamberBlockEntity chamber, int slot) {
        ItemStack stack = chamber.getItem(slot);
        if (stack.getCount() != 1) return;
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return;
        FluidStack offered = item.drain(FluidChemicalReactionChamberBlockEntity.TANK_CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        int accepted = chamber.getTank(slot).fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drained = item.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        chamber.getTank(slot).fill(drained, IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(slot, item.getContainer());
    }

    private static void fillContainer(FluidChemicalReactionChamberBlockEntity chamber) {
        ItemStack stack = chamber.getItem(2);
        if (stack.getCount() != 1 || chamber.getTank(2).isEmpty()) return;
        IFluidHandlerItem item = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (item == null) return;
        FluidStack available = chamber.getTank(2).drain(FluidChemicalReactionChamberBlockEntity.TANK_CAPACITY, IFluidHandler.FluidAction.SIMULATE);
        int accepted = item.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drained = chamber.getTank(2).drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        item.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        chamber.setItem(2, item.getContainer());
    }

    private static void setActive(ServerLevel level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        int value = active ? 2 : 1;
        if (state.hasProperty(ChemicalReactionChamberBlock.BLOCKSTATE) && state.getValue(ChemicalReactionChamberBlock.BLOCKSTATE) != value)
            level.setBlock(pos, state.setValue(ChemicalReactionChamberBlock.BLOCKSTATE, value), 3);
    }

    private static void sync(ServerLevel level, BlockPos pos, FluidChemicalReactionChamberBlockEntity chamber) {
        chamber.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }
}
