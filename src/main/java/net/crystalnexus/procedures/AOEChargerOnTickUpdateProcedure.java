package net.crystalnexus.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import net.crystalnexus.config.CrystalnexusConfig;
import net.crystalnexus.init.CrystalnexusModItems;

import java.util.List;

public class AOEChargerOnTickUpdateProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z) {

        if (!(world instanceof Level level) || level.isClientSide()) return;

        BlockPos pos = BlockPos.containing(x, y, z);
        boolean isCharging = false;

        IEnergyStorage blockEnergy =
                level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.UP);

        if (blockEnergy == null)
            return;

        IItemHandler itemHandler =
                level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);

        if (itemHandler == null)
            return;


        CrystalnexusConfig.AoeChargerValues config = CrystalnexusConfig.MACHINES.AOE_CHARGER_BEHAVIOR;
        Settings settings = settings(itemHandler.getStackInSlot(0));
        double range = settings.range();
        int maxTransferPerTick = settings.transfer();


        if (blockEnergy.getEnergyStored() > 0) {

            AABB area = new AABB(
                    x - range, y - range, z - range,
                    x + range, y + range, z + range
            );

            List<Player> players = level.getEntitiesOfClass(Player.class, area);

            for (Player player : players) {

                for (ItemStack stack : player.getInventory().items) {

                    if (blockEnergy.getEnergyStored() <= 0)
                        break;

                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage itemEnergy =
                            stack.getCapability(Capabilities.EnergyStorage.ITEM);

                    if (itemEnergy == null)
                        continue;

                    if (itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored())
                        continue;

                    int energyAvailable = blockEnergy.getEnergyStored();
                    int energyToSend = Math.min(maxTransferPerTick, energyAvailable);

                    int accepted = itemEnergy.receiveEnergy(energyToSend, true);

                    if (accepted > 0) {
                        int extracted = blockEnergy.extractEnergy(accepted, false);
                        itemEnergy.receiveEnergy(extracted, false);
                        isCharging = true;
                    }
                }
            }
        }

			
			if (isCharging) {
			
			    setBlockState(world, pos, 2);
			
			    if (!level.isClientSide() && level.random.nextDouble() < config.sparkChance()) {
			
			        if (level instanceof ServerLevel serverLevel) {
			
			            double px = x + 0.5 + (level.random.nextDouble() - 0.5);
			            double py = y + 1.0;
			            double pz = z + 0.5 + (level.random.nextDouble() - 0.5);
			
			            serverLevel.sendParticles(
			                    ParticleTypes.ELECTRIC_SPARK,
			                    px, py, pz,
			                    3,              
			                    0.1, 0.1, 0.1,  
			                    0.02            
			            );
			        }
			    }
			
			} else {
			    setBlockState(world, pos, 1);
			}



    }


    private record Settings(double range, int transfer) {}

    private static Settings settings(ItemStack upgrade) {
        CrystalnexusConfig.AoeChargerValues config = CrystalnexusConfig.MACHINES.AOE_CHARGER_BEHAVIOR;
        int baseInput = config.baseTransferPerTick();
        double inputMult = 1.0;
        double range = config.baseRange();



        if (!upgrade.isEmpty()) {

            if (upgrade.getItem() == CrystalnexusModItems.ACCELERATION_UPGRADE.get())
                inputMult = net.crystalnexus.util.MachineUpgradeHelper.scaledEffect(upgrade, 1.0, config.accelerationUpgradeMultiplier());

            else if (upgrade.getItem() == CrystalnexusModItems.CARBON_ACCELERATION_UPGRADE.get())
                inputMult = net.crystalnexus.util.MachineUpgradeHelper.scaledEffect(upgrade, 1.0, config.carbonAccelerationUpgradeMultiplier());

            else if (upgrade.getItem() == CrystalnexusModItems.RANGE_UPGRADE.get())
                range = net.crystalnexus.util.MachineUpgradeHelper.scaledEffect(upgrade, config.baseRange(), config.rangeUpgradeRange());

            else if (upgrade.getItem() == CrystalnexusModItems.CARBON_RANGE_UPGRADE.get())
                range = net.crystalnexus.util.MachineUpgradeHelper.scaledEffect(upgrade, config.baseRange(), config.carbonRangeUpgradeRange());
        }

        
        CompoundTag data = null;

        if (!upgrade.isEmpty() && upgrade.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = upgrade.get(DataComponents.CUSTOM_DATA);
            if (cd != null)
                data = cd.copyTag();
        }

        if (data != null && data.contains("cook_mult")) {
            double cookMult = data.getDouble("cook_mult");
            if (cookMult > 0)
                inputMult = 1.0 / cookMult;
        }

        inputMult = Math.max(config.minTransferMultiplier(), Math.min(inputMult, config.maxTransferMultiplier()));
        int maxTransferPerTick = (int) Math.floor(baseInput * inputMult);
        return new Settings(range, maxTransferPerTick);
    }

    public static String displayText(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof Level level)) return "";
        BlockPos pos = BlockPos.containing(x, y, z);
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.UP) == null) return "No Energy Capability";
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        if (inventory == null) return "No Inventory Capability";
        Settings settings = settings(inventory.getStackInSlot(0));
        return "Range: " + (int) settings.range() + " blocks | " + settings.transfer() + " FE/t";
    }

    private static void setBlockState(LevelAccessor world, BlockPos pos, int value) {
        BlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getStateDefinition().getProperty("blockstate") instanceof IntegerProperty prop
                && prop.getPossibleValues().contains(value)) {
            world.setBlock(pos, bs.setValue(prop, value), 3);
        }
    }
}
