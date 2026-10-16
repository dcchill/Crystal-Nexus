package net.crystalnexus.item;

import net.crystalnexus.init.CrystalnexusModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class UniversalDroneItem extends Item {
    public UniversalDroneItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext c) {
        if (!(c.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var player=c.getPlayer(); if(player==null)return InteractionResult.FAIL;
        var pos=c.getClickedPos().relative(c.getClickedFace());if(!level.hasChunkAt(pos) || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand()))return InteractionResult.FAIL;
        var drone=CrystalnexusModEntities.UNIVERSAL_DRONE.get().create(level);if(drone==null)return InteractionResult.FAIL;
        var saved=c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(saved.contains("Drone")) {
            if(saved.getCompound("Drone").hasUUID("owner") && !saved.getCompound("Drone").getUUID("owner").equals(player.getUUID())) {player.sendSystemMessage(Component.literal("Only the owner can deploy this drone."));return InteractionResult.FAIL;}
            drone.loadDrone(saved.getCompound("Drone"));
        }
        drone.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,player.getYRot(),0);
        if(!level.noCollision(drone) || !level.getWorldBorder().isWithinBounds(pos))return InteractionResult.FAIL;
        if(drone.owner==null)drone.setOwner(player);
        if(!level.addFreshEntity(drone))return InteractionResult.FAIL;
        // Packed contents must be consumed even in creative mode to avoid cloning inventories.
        if(saved.contains("Drone") || !player.getAbilities().instabuild)c.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
