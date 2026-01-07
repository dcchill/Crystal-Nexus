package net.crystalnexus.procedures;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCrafterOnTickProcedure {

    // Only compares item types, ignores NBT
    private static boolean stacksMatch(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem();
    }

    public static void execute(LevelAccessor world, double x, double y, double z) {
        boolean crafting = false; // starts as false

        if (!(world instanceof ILevelExtension ext)) return;
        BlockPos pos = BlockPos.containing(x, y, z);

        var cap = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (!(cap instanceof IItemHandlerModifiable inv)) return;

        // -----------------------------
        // Collect 3x3 crafting grid
        // -----------------------------
        ItemStack[] grid = new ItemStack[9];
        boolean empty = true;
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getStackInSlot(i).copy();
            if (!grid[i].isEmpty()) empty = false;
        }
        if (empty) {
            updateBlockState(world, pos, crafting);
            return;
        }

        if (!(world instanceof ServerLevel serverLevel)) return;

        ItemStack filter = inv.getStackInSlot(10);
        if (filter.isEmpty()) {
            updateBlockState(world, pos, crafting);
            return;
        }

        RecipeManager manager = serverLevel.getRecipeManager();
        List<RecipeHolder<CraftingRecipe>> recipes = manager.getAllRecipesFor(RecipeType.CRAFTING);

        CraftingRecipe matchedRecipe = null;
        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            CraftingRecipe r = holder.value();
            ItemStack result = r.getResultItem(serverLevel.registryAccess());
            if (!result.isEmpty() && stacksMatch(result, filter)) {
                matchedRecipe = r;
                break;
            }
        }
        if (matchedRecipe == null) {
            updateBlockState(world, pos, crafting);
            return;
        }

        IEnergyStorage energy = ext.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        boolean canCraftEnergy = energy == null || energy.getEnergyStored() >= 256;
        if (!canCraftEnergy) {
            updateBlockState(world, pos, crafting);
            return;
        }

        // -----------------------------
        // Shape-agnostic, amount-aware ingredient check
        // -----------------------------
        Map<ItemStack, Integer> required = new HashMap<>();
        for (Ingredient ing : matchedRecipe.getIngredients()) {
            ItemStack[] options = ing.getItems();
            if (options.length == 0) continue;
            ItemStack example = options[0];
            boolean exists = false;
            for (ItemStack key : required.keySet()) {
                if (stacksMatch(key, example)) {
                    required.put(key, required.get(key) + 1);
                    exists = true;
                    break;
                }
            }
            if (!exists) required.put(example, 1);
        }

        Map<ItemStack, Integer> available = new HashMap<>();
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) continue;
            for (ItemStack key : required.keySet()) {
                if (stacksMatch(stack, key)) {
                    available.put(key, available.getOrDefault(key, 0) + stack.getCount());
                }
            }
        }

        for (Map.Entry<ItemStack, Integer> req : required.entrySet()) {
            if (available.getOrDefault(req.getKey(), 0) < req.getValue()) {
                updateBlockState(world, pos, crafting);
                return; // Not enough items
            }
        }

        // -----------------------------
        // Check output slot has room BEFORE consuming ingredients
        // -----------------------------
        ItemStack result = matchedRecipe.getResultItem(serverLevel.registryAccess());
        ItemStack output = inv.getStackInSlot(9);
        int maxStack = Math.min(result.getMaxStackSize(), 127);

        if (!output.isEmpty()) {
            if (!stacksMatch(output, result)) {
                updateBlockState(world, pos, crafting);
                return; // output is different → cannot craft
            }
            if (output.getCount() + result.getCount() > maxStack) {
                updateBlockState(world, pos, crafting);
                return; // no room → cannot craft
            }
        }

        // -----------------------------
        // Consume ingredients
        // -----------------------------
        for (Map.Entry<ItemStack, Integer> req : required.entrySet()) {
            int remaining = req.getValue();
            for (int i = 0; i < 9; i++) {
                ItemStack slot = inv.getStackInSlot(i);
                if (!slot.isEmpty() && stacksMatch(slot, req.getKey())) {
                    int remove = Math.min(slot.getCount(), remaining);
                    slot.shrink(remove);
                    remaining -= remove;
                    if (remaining <= 0) break;
                }
            }
        }

        // -----------------------------
        // Insert output
        // -----------------------------
        if (output.isEmpty()) {
            ItemStack toInsert = result.copy();
            toInsert.setCount(Math.min(toInsert.getCount(), maxStack));
            inv.setStackInSlot(9, toInsert);
        } else {
            output.grow(result.getCount());
        }

        // -----------------------------
        // Drain energy
        // -----------------------------
        if (energy != null) energy.extractEnergy(256, false);

        // -----------------------------
        // Crafting happened
        // -----------------------------
        crafting = true;

        // -----------------------------
        // Update block state based on crafting status
        // -----------------------------
        updateBlockState(world, pos, crafting);
    }

    // -----------------------------
    // Block state update helper
    // -----------------------------
    private static void updateBlockState(LevelAccessor world, BlockPos pos, boolean crafting) {
        int value = crafting ? 2 : 1;
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty prop
                && prop.getPossibleValues().contains(value)) {
            world.setBlock(pos, bs.setValue(prop, value), 3);
        }
    }

    // -----------------------------
    // Helper methods
    // -----------------------------
    public static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
        if (world instanceof ILevelExtension ext) {
            IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (itemHandler != null) return itemHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }

    public static int getEnergyStored(LevelAccessor level, BlockPos pos, Direction direction) {
        if (level instanceof ILevelExtension levelExtension) {
            IEnergyStorage energyStorage = levelExtension.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
            if (energyStorage != null) return energyStorage.getEnergyStored();
        }
        return 0;
    }
}
