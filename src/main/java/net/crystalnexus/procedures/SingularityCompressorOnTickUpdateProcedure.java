package net.crystalnexus.procedures;

import net.crystalnexus.item.GeneratedSingularityItem;
import net.crystalnexus.item.ResourceCometItem;
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
        if (!(world instanceof Level level) || level.isClientSide()) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockEntity entity = level.getBlockEntity(pos);
        IItemHandlerModifiable inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) instanceof IItemHandlerModifiable handler ? handler : null;
        if (entity == null || inventory == null || !inventory.getStackInSlot(1).isEmpty()) return;

        CompoundTag data = entity.getPersistentData();
        data.putDouble("maxItem", GeneratedSingularityItem.COST);
        data.putDouble("maxProgress", TIME);
        ItemStack input = inventory.getStackInSlot(0);
        if (data.getDouble("item") < GeneratedSingularityItem.COST && ResourceCometItem.isMaterial(input)
                && MachineAnimationHelper.shouldIdle(level, pos, data.getDouble("progress"))) {
            String id = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
            if (data.getDouble("item") == 0) data.putString("setItem", id);
            if (id.equals(data.getString("setItem"))) {
                data.putDouble("item", data.getDouble("item") + 1);
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
                        ItemStack material = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
                        ItemStack result = SingularityCompressionRecipe.resultFor(level, material);
                        if (!result.isEmpty()) {
                            energy.extractEnergy(ENERGY, false);
                            inventory.setStackInSlot(1, result);
                            data.putDouble("item", 0);
                            data.putDouble("progress", 0);
                            data.remove("setItem");
                        }
                    }
                }
            }
        }
        entity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }
}
