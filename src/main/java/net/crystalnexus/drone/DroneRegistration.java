package net.crystalnexus.drone;

import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.init.CrystalnexusModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.energy.IEnergyStorage;

@EventBusSubscriber(modid="crystalnexus",bus=EventBusSubscriber.Bus.MOD)
public final class DroneRegistration {
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(CrystalnexusModEntities.UNIVERSAL_DRONE.get(),LUNADroneEntity.createAttributes().build());}
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event){
        // Public automation may charge the drone, but cannot access its private cargo or extract its power.
        event.registerEntity(Capabilities.EnergyStorage.ENTITY,CrystalnexusModEntities.UNIVERSAL_DRONE.get(),(drone,side)->new IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){return drone.battery.receiveEnergy(amount,simulate);}
            public int extractEnergy(int amount,boolean simulate){return 0;}
            public int getEnergyStored(){return drone.battery.getEnergyStored();}
            public int getMaxEnergyStored(){return drone.battery.getMaxEnergyStored();}
            public boolean canExtract(){return false;}
            public boolean canReceive(){return true;}
        });
    }
}
