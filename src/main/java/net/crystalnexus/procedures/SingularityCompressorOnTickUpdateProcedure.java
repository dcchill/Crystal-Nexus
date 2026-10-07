package net.crystalnexus.procedures;

import net.crystalnexus.item.GeneratedSingularityItem;
import net.crystalnexus.jei_recipes.SingularityCompressionRecipe;
import net.crystalnexus.util.MachineAnimationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class SingularityCompressorOnTickUpdateProcedure {
    private static final int ENERGY = 1_024_000;
    private static final int TIME = 300;

    public static void execute(LevelAccessor world, double x, double y, double z) {
        BlockEntity machine = world.getBlockEntity(BlockPos.containing(x, y, z));
        int steps = machine instanceof net.minecraft.world.Container inventory && inventory.getContainerSize() > 2
                ? net.crystalnexus.util.MachineUpgradeHelper.parallelCraftCount(inventory.getItem(2)) : 1;
        for (int step = 0; step < steps; step++) executeSingle(world, x, y, z);
    }

    private static void executeSingle(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level) || level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockEntity entity = level.getBlockEntity(pos);
        IItemHandlerModifiable inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable handler ? handler : null;
        if (entity == null || inventory == null || !inventory.getStackInSlot(1).isEmpty()) return;

        CompoundTag data = entity.getPersistentData();
        data.putDouble("maxItem", GeneratedSingularityItem.COST);
        data.putDouble("maxProgress", TIME);
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack material = SingularityCompressionRecipe.compressionMaterial(input);
        if (data.getDouble("item") < GeneratedSingularityItem.COST && !SingularityCompressionRecipe.resultFor(level, input).isEmpty()
                && MachineAnimationHelper.shouldIdle(level, pos, data.getDouble("progress"))) {
            String id = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
            ResourceLocation previousId = ResourceLocation.tryParse(data.getString("setItem"));
            if (previousId != null && BuiltInRegistries.ITEM.containsKey(previousId))
                data.putString("setItem", BuiltInRegistries.ITEM.getKey(SingularityCompressionRecipe.compressionMaterial(
                        BuiltInRegistries.ITEM.get(previousId).getDefaultInstance()).getItem()).toString());
            if (data.getDouble("item") == 0) data.putString("setItem", id);
            if (id.equals(data.getString("setItem"))) {
                data.putDouble("item", data.getDouble("item") + SingularityCompressionRecipe.materialValue(input));
                ItemStack remaining = input.copy();
                remaining.shrink(1);
                inventory.setStackInSlot(0, remaining);
            }
        }

        if (data.getDouble("item") >= GeneratedSingularityItem.COST) {
            IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (energy != null && energy.extractEnergy(ENERGY, true) == ENERGY) {
                if (data.getDouble("progress") < TIME) data.putDouble("progress", data.getDouble("progress") + 1);
                else {
                    ResourceLocation id = ResourceLocation.tryParse(data.getString("setItem"));
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                        ItemStack result = SingularityCompressionRecipe.resultFor(level,
                                SingularityCompressionRecipe.compressionMaterial(BuiltInRegistries.ITEM.get(id).getDefaultInstance()));
                        result = (result).copy();
                        if (!result.isEmpty() && result.getCount() <= result.getMaxStackSize()) {
                            energy.extractEnergy(ENERGY, false);
                            inventory.setStackInSlot(1, result);
                            data.putDouble("item", data.getDouble("item") - GeneratedSingularityItem.COST);
                            data.putDouble("progress", 0);
                            if (data.getDouble("item") == 0) data.remove("setItem");
                        }
                    }
                }
            }
        }
        entity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }
}
