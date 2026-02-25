package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import net.crystalnexus.init.CrystalnexusModItems;

public class ItemChargerOnTickUpdateProcedure {

    public static String execute(LevelAccessor world, double x, double y, double z) {

        if (!(world instanceof Level level)) return "";

        BlockPos pos = BlockPos.containing(x, y, z);

        boolean isCharging = false;

        IEnergyStorage blockEnergy =
                level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.UP);

        IItemHandler itemHandler =
                level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);

        if (blockEnergy == null || itemHandler == null)
            return "0 FE/t";

        // ----------------------------------
        //  SLOT 2 = Upgrade Logic
        // ----------------------------------

        int baseInput = 512;
        double inputMult = 1.0;

        ItemStack upgradeStack = itemHandler.getStackInSlot(2);

        if (!upgradeStack.isEmpty()) {

            if (upgradeStack.getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get())
                inputMult = 1.5;

            else if (upgradeStack.getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())
                inputMult = 3.0;
        }

        CompoundTag data = null;

        if (!upgradeStack.isEmpty() && upgradeStack.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = upgradeStack.get(DataComponents.CUSTOM_DATA);
            if (cd != null)
                data = cd.copyTag();
        }

        if (data != null && data.contains("cook_mult")) {
            double cookMult = data.getDouble("cook_mult");
            if (cookMult > 0)
                inputMult = 1.0 / cookMult;
        }

        inputMult = Math.max(0.05, Math.min(inputMult, 20.0));
        int maxTransferPerTick = (int) Math.floor(baseInput * inputMult);

        // ----------------------------------
        //  Charging Logic (SERVER ONLY)
        // ----------------------------------

        if (!level.isClientSide()) {

            ItemStack stack = itemHandler.getStackInSlot(0);

            if (!stack.isEmpty() && blockEnergy.getEnergyStored() > 0) {

                IEnergyStorage itemEnergy =
                        stack.getCapability(Capabilities.EnergyStorage.ITEM);

                if (itemEnergy != null &&
                    itemEnergy.getEnergyStored() < itemEnergy.getMaxEnergyStored()) {

                    int energyAvailable = blockEnergy.getEnergyStored();
                    int energyToSend = Math.min(maxTransferPerTick, energyAvailable);

                    int accepted = itemEnergy.receiveEnergy(energyToSend, true);

                    if (accepted > 0) {
                        int extracted = blockEnergy.extractEnergy(accepted, false);
                        itemEnergy.receiveEnergy(extracted, false);
                        isCharging = true;

                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) be.setChanged();
                    }
                }

                if (itemEnergy != null &&
                    itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored()) {

                    ItemStack outputStack = itemHandler.getStackInSlot(1);

                    if (outputStack.isEmpty()) {
                        ItemStack extracted = itemHandler.extractItem(0, 1, false);
                        itemHandler.insertItem(1, extracted, false);
                    }
                }
            }

            // Update blockstate server-side
            if (isCharging)
                setBlockState(world, pos, 2);
            else
                setBlockState(world, pos, 1);
        }

        // ----------------------------------
        // Return FE/t for GUI
        // ----------------------------------

        return maxTransferPerTick + " FE/t";
    }

    private static void setBlockState(LevelAccessor world, BlockPos pos, int value) {
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty prop
                && prop.getPossibleValues().contains(value)) {
            world.setBlock(pos, bs.setValue(prop, value), 3);
        }
    }
}