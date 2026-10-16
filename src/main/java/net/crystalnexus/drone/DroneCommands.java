package net.crystalnexus.drone;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

@EventBusSubscriber(modid="crystalnexus")
public final class DroneCommands {
    public static int recall(ServerPlayer player,UUID id,boolean resume){
        var server=player.getServer();var directory=DroneRegistry.get(server);
        var entries=directory.entries.values().stream().filter(e->e.owner().equals(player.getUUID())&&(id==null||e.id().equals(id))).toList();
        if(entries.isEmpty()){player.sendSystemMessage(Component.literal("No owned drones found."));return 0;}
        int count=0;
        for(var entry:entries){
            String message;
            if(!entry.dimension().equals(player.level().dimension().location().toString()))message="Cannot recall: drone is in another dimension; cross-dimensional recall is unsupported";
            else {
                var drone=DroneRegistry.loaded(server,entry.id());
                if(drone==null)message="Cannot recall: drone chunk is unloaded; load its area and retry";
                else if(!drone.ownedBy(player))message="You do not own this drone";
                else {message=drone.runtime.recall(player.getUUID(),false,resume);if(message.isEmpty()){message="Recall started";count++;}}
            }
            player.sendSystemMessage(Component.literal(entry.id()+": "+message));
        }
        return count;
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("drone")
            .then(Commands.literal("recall").executes(c->recall(c.getSource().getPlayerOrException(),null,false))
                .then(Commands.argument("drone",UuidArgument.uuid()).executes(c->recall(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"drone"),false))
                    .then(Commands.literal("resume").executes(c->recall(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"drone"),true)))))
            .then(Commands.literal("recall_all").executes(c->recall(c.getSource().getPlayerOrException(),null,false))
                .then(Commands.literal("resume").executes(c->recall(c.getSource().getPlayerOrException(),null,true))))
            .then(Commands.literal("list").executes(c->{var p=c.getSource().getPlayerOrException();var entries=DroneRegistry.get(p.getServer()).entries.values().stream().filter(e->e.owner().equals(p.getUUID())).toList();for(var e:entries)p.sendSystemMessage(Component.literal(e.id()+" | "+e.dimension()+" | "+(DroneRegistry.loaded(p.getServer(),e.id())==null?"unloaded":"loaded")));return entries.size();}))
            .then(Commands.literal("open").then(Commands.argument("drone",UuidArgument.uuid()).executes(c->{var p=c.getSource().getPlayerOrException();var d=DroneRegistry.loaded(p.getServer(),UuidArgument.getUuid(c,"drone"));if(d==null){p.sendSystemMessage(Component.literal("Drone is unloaded or unavailable"));return 0;}d.open(p);return 1;})))
            .then(Commands.literal("resume").then(Commands.argument("drone",UuidArgument.uuid()).executes(c->{var p=c.getSource().getPlayerOrException();var d=DroneRegistry.loaded(p.getServer(),UuidArgument.getUuid(c,"drone"));if(d==null||!d.ownedBy(p)){p.sendSystemMessage(Component.literal("Owned drone is unavailable"));return 0;}boolean ok=d.runtime.resume();p.sendSystemMessage(Component.literal(d.status));return ok?1:0;})))
            .then(Commands.literal("authorize").then(Commands.argument("drone",UuidArgument.uuid()).then(Commands.argument("player",EntityArgument.player()).executes(c->authorize(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"drone"),EntityArgument.getPlayer(c,"player"),true)))))
            .then(Commands.literal("revoke").then(Commands.argument("drone",UuidArgument.uuid()).then(Commands.argument("player",EntityArgument.player()).executes(c->authorize(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"drone"),EntityArgument.getPlayer(c,"player"),false))))));
    }
    private static int authorize(ServerPlayer owner,UUID drone,ServerPlayer target,boolean permit){
        var d=DroneRegistry.loaded(owner.getServer(),drone);if(d==null||!d.ownedBy(owner)){owner.sendSystemMessage(Component.literal("Owned drone is unavailable"));return 0;}
        if(permit)d.authorized.add(target.getUUID());else d.authorized.remove(target.getUUID());
        owner.sendSystemMessage(Component.literal((permit?"Authorized ":"Revoked ")+target.getGameProfile().getName()+" ("+target.getUUID()+")"));return 1;
    }
}
