package net.crystalnexus.drone;

import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.item.LinkCardData;
import net.minecraft.nbt.CompoundTag;

/** Resolved cards belong to the active task, including through recall and reload. */
public final class DroneDestinations {
    private DroneDestinations() {}
    public static CompoundTag resolve(LUNADroneEntity drone,CompoundTag parameters) {
        CompoundTag result=parameters.copy();
        for(String key:new String[]{"destinationSlot","endDestinationSlot"}) {
            if(!parameters.contains(key))continue;
            String boundKey=key.equals("destinationSlot")?"boundDestination":"boundEnd";
            CompoundTag bound=drone.program.memory.getCompound(boundKey);
            if(bound.isEmpty()) {bound=card(drone,parameters.getInt(key));drone.program.memory.put(boundKey,bound.copy());}
            if(!bound.getString("dimension").equals(drone.level().dimension().location().toString()))throw new IllegalArgumentException("Destination is in another dimension");
            result.putIntArray(key.equals("destinationSlot")?"pos":"end",bound.getIntArray("pos"));
            if(key.equals("destinationSlot")){result.putBoolean("useSelected",false);if(!result.contains("side"))result.putString("side",bound.getString("face"));}
        }
        return result;
    }
    public static CompoundTag card(LUNADroneEntity drone,int slot) {
        if(slot<0||slot>=9)throw new IllegalArgumentException("Choose an installed destination card");
        var stack=drone.destinationCards.getStackInSlot(slot);
        if(stack.isEmpty()||!stack.is(net.crystalnexus.init.CrystalnexusModItems.LINK_CARD.get()))throw new IllegalArgumentException("Destination "+(slot+1)+" has no card installed");
        var link=LinkCardData.read(stack);
        if(link==null)throw new IllegalArgumentException("Destination "+(slot+1)+" is unlinked");
        if(link.dimension().isEmpty())throw new IllegalArgumentException("Relink this card to record its dimension");
        if(!link.dimension().equals(drone.level().dimension().location().toString()))throw new IllegalArgumentException("Destination "+(slot+1)+" is in another dimension");
        CompoundTag p=new CompoundTag();p.putIntArray("pos",new int[]{link.pos().getX(),link.pos().getY(),link.pos().getZ()});
        p.putString("dimension",link.dimension());p.putString("face",link.face().getName());return p;
    }
    public static void clear(CompoundTag memory){memory.remove("boundDestination");memory.remove("boundEnd");memory.remove("boundWaypoint");}
}
