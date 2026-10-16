package net.crystalnexus.network;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.drone.*;
import net.crystalnexus.world.inventory.DroneMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DroneControl(int menu,String action,CompoundTag data) implements CustomPacketPayload {
    public static final Type<DroneControl> TYPE=new Type<>(ResourceLocation.parse("crystalnexus:drone_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DroneControl> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);b.writeUtf(p.action,24);b.writeNbt(p.data);},b->new DroneControl(b.readVarInt(),b.readUtf(24),b.readNbt()));
    @Override public Type<DroneControl> type(){return TYPE;}
    public static void handle(DroneControl p,IPayloadContext context){context.enqueueWork(()->{
        if(!(context.player().containerMenu instanceof DroneMenu m)||m.containerId!=p.menu||!m.stillValid(context.player())||m.drone==null||p.data==null||p.data.toString().length()>65536)return;
        var d=m.drone;
        if(p.action.equals("graph")||p.action.equals("graph_run")){m.submittedRevision=p.data.getInt("revision");m.editRequest=p.data.getInt("editRequest");m.editAccepted=false;}
        switch(p.action){
            case "tab" -> {m.cargoTab=p.data.getBoolean("cargo");return;}
            case "start" -> {String error=DroneProgram.validate(d.graph(),d.mode);if(!error.isEmpty()){d.status=error;break;}d.runtime.cancel();d.program.start(d.graph());d.status="Running";}
            case "pause" -> {d.program.running=false;d.hover();d.status="Paused";}
            case "continue" -> {if(d.runtime.isRecalling()){d.status="Recall is active";break;}if(d.graph().node(d.program.node)!=null){d.runtime.cancel();d.program.running=true;d.status="Running";}}
            case "stop" -> {d.runtime.cancel();d.program.stop();d.status="Stopped";}
            case "resume" -> d.runtime.resume();
            case "recall", "home" -> {String error=d.runtime.recall(context.player().getUUID(),p.action.equals("home"),p.data.getBoolean("resume"));if(!error.isEmpty())d.status=error;}
            case "recall_all" -> DroneCommands.recall((net.minecraft.server.level.ServerPlayer)context.player(),null,p.data.getBoolean("resume"));
            case "graph", "graph_run" -> {
                try {AssemblyGraph graph=AssemblyGraph.load(p.data,d.registryAccess());String error=DroneProgram.validate(graph,d.mode);
                    if(!error.isEmpty()){d.status=error;break;}if(p.data.getInt("revision")!=d.graph().revision){d.status="Program changed; reopen before editing";break;}
                    d.program.stop();d.runtime.cancel();graph.revision=d.graph().revision+1;d.programs.put(d.mode,graph);m.editAccepted=true;d.status="Program saved; previous task invalidated";
                    if(p.action.equals("graph_run")){d.runtime.cancel();d.program.start(graph);d.status="Running saved program";}
                }catch(IllegalArgumentException e){d.status="Invalid program: "+e.getMessage();}
            }
            default -> {return;}
        }
        if(p.action.equals("graph")||p.action.equals("graph_run")){m.editError=m.editAccepted?"":d.status;}
        context.player().sendSystemMessage(Component.literal(d.status));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer)context.player(),new DroneState(m.containerId,m.snapshot()));
    });}
}
