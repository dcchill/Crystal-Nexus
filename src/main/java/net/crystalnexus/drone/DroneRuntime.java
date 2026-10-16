package net.crystalnexus.drone;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.UUID;

public final class DroneRuntime {
    public final LUNADroneEntity drone;
    public final DroneFlight flight;
    public final DroneActions actions;
    private boolean recalling, holding, autoResume, recallHome, eventRunning;
    private UUID recallPlayer;
    private int recallTicks,navigationOwner=-1,executingNode=-1;
    public DroneRuntime(LUNADroneEntity drone) { this.drone = drone; flight = new DroneFlight(drone); actions = new DroneActions(this); }
    public boolean isRecalling() { return recalling; }
    public String destination() { return flight.destination(); }
    public ServerLevel level() { return (ServerLevel)drone.level(); }
    public ServerPlayer player(String value) {
        UUID id;
        try { id = value.isEmpty() || value.equals("owner") ? drone.owner : UUID.fromString(value); }
        catch (IllegalArgumentException e) { return null; }
        return drone.permitted(id) ? level().getServer().getPlayerList().getPlayer(id) : null;
    }
    public String recall(UUID target, boolean home, boolean resume) {
        if (drone.level().isClientSide) return "Recall must execute on the server";
        if (recalling) return "Drone is already recalling";
        if (home) {
            if (drone.home == null) return "No home position saved";
            if (!drone.homeDimension.equals(drone.level().dimension().location().toString())) return "Home is in another dimension; cross-dimensional recall is unsupported";
        } else {
            if (!drone.permitted(target)) return "Player is not authorized";
            ServerPlayer p = level().getServer().getPlayerList().getPlayer(target);
            if (p == null) return "Recall player is offline";
            if (p.level() != drone.level()) return "Drone and player are in different dimensions; cross-dimensional recall is unsupported";
        }
        Vec3 dest = home ? Vec3.atCenterOf(drone.home) : level().getServer().getPlayerList().getPlayer(target).position().add(0, 2, 0);
        if (!level().hasChunkAt(BlockPos.containing(dest))) return "Recall destination is unloaded";
        if (drone.battery.getEnergyStored() < Math.max(5, (int)(drone.position().distanceTo(dest) * 20))) return "Insufficient battery for recall; recharge the drone";
        drone.program.suspend(drone.mode, drone.graph().revision);
        recallPlayer = target; recallHome = home; autoResume = resume; recalling = true; holding = false; eventRunning = false; recallTicks = 0;
        resetAllNavigation(); drone.status = "Recalling"; return "";
    }
    public void cancel() { recalling = holding = eventRunning = false; resetAllNavigation(); }
    public boolean resume() {
        if (recalling) { drone.status = "Recall is still active"; return false; }
        if (!drone.program.resume(drone.mode, drone.graph())) { drone.status = "Previous task is no longer valid (mode or program changed)"; return false; }
        holding = eventRunning = false; resetAllNavigation(); drone.status = drone.program.running ? "Resumed" : "Previous task was paused"; return true;
    }
    private void resetAllNavigation(){navigationOwner=-1;flight.reset();}
    public void resetNavigation(){
        int task=executingNode>=0?executingNode:drone.program.node;
        if(task<0)task=-2;
        if(navigationOwner==-1||navigationOwner==task)resetAllNavigation();
    }
    public DroneFlight.Result navigate(Vec3 target,double distance,double speed,boolean persistent){
        int task=executingNode>=0?executingNode:drone.program.node;if(task<0)task=-2;
        if(navigationOwner!=-1&&navigationOwner!=task)return DroneFlight.Result.RUNNING;
        navigationOwner=task;var result=flight.tick(target,distance,speed,persistent);
        if(result!=DroneFlight.Result.RUNNING){String error=flight.error;resetAllNavigation();flight.error=error;}return result;
    }
    public void tick() {
        if(drone.level().isClientSide)return;
        charge();
        if(recalling){tickRecall();return;}
        if(holding&&!drone.program.running){tickHold();return;}
        if(!drone.program.running)return;
        for(var task:drone.program.beginTick()){
            if(!drone.program.running||recalling)break;
            if(!drone.program.activate(task))continue;
            var node=drone.graph().node(drone.program.node);
            if(node==null){fail("Active task no longer exists");break;}
            DroneNode kind=DroneNode.of(node);executingNode=node.id;
            try{
                if(!kind.available(drone.mode)){fail("Node is incompatible with current mode");break;}
                if(drone.battery.getEnergyStored()<2000&&drone.home!=null&&!eventRunning
                    &&kind!=DroneNode.RECALL_DRONE&&kind!=DroneNode.RECALL_TO_PLAYER&&kind!=DroneNode.RECALL_TO_HOME&&kind!=DroneNode.RETURN_HOME){
                    String error=recall(drone.owner,true,true);if(!error.isEmpty())fail("Low battery: "+error);break;
                }
                drone.status=kind.label();node.status="Running";drone.program.elapsed++;
                int output=actions.step(kind,node);
                if(output>=0){
                    node.status=output==2?"Failure":"Success";
                    drone.program.advance(drone.graph(),output);
                    if(drone.program.running)resetNavigation();else resetAllNavigation();
                    if(output==2&&!drone.program.running)drone.status=actions.error;
                }
            }catch(IllegalArgumentException e){fail("Invalid node settings: "+e.getMessage());}
            finally{executingNode=-1;}
        }
        drone.program.finishTick();
        if(eventRunning&&!drone.program.running&&!recalling){
            eventRunning=false;if(autoResume&&(!recallHome||drone.battery.getEnergyStored()>=10000))resume();
        }
    }
    private void tickRecall() {
        Vec3 destination;
        if (recallHome) destination = Vec3.atCenterOf(drone.home);
        else {
            ServerPlayer p = level().getServer().getPlayerList().getPlayer(recallPlayer);
            if (p == null) { recallFailure("Recall player went offline"); return; }
            if (!drone.permitted(p.getUUID())) { recallFailure("Recall permission was revoked"); return; }
            if (p.level() != drone.level()) { recallFailure("Recall player changed dimension; cross-dimensional recall is unsupported"); return; }
            destination = p.position().add(0, 2, 0);
        }
        if (++recallTicks > 2400) { recallFailure("Recall timed out while following the player"); return; }
        var result = flight.tick(destination, 2, 1, false);
        if (result == DroneFlight.Result.FAILURE) { recallFailure(flight.error); return; }
        if (result == DroneFlight.Result.SUCCESS) {
            recalling = false; holding = true; drone.status = "Recall complete; hovering";
            drone.program.memory.putBoolean("recall_complete", true); notifyOwner(drone.status);
            var event = drone.graph().nodes.stream().filter(n -> DroneNode.of(n) == DroneNode.ON_RECALL_COMPLETE).findFirst();
            if (event.isPresent()) {
                drone.program.event(event.get().id); eventRunning = true;
            } else if (autoResume && (!recallHome || drone.battery.getEnergyStored() >= 10000)) resume();
        }
    }
    private void tickHold() {
        if (recallHome) {
            if (autoResume && drone.battery.getEnergyStored() >= 10000) resume();
            return;
        }
        ServerPlayer p = level().getServer().getPlayerList().getPlayer(recallPlayer);
        if (p == null || p.level() != drone.level() || !drone.permitted(recallPlayer)) { holding = false; drone.hover(); drone.status = "Hover target unavailable; task preserved"; return; }
        if (flight.tick(p.position().add(0, 2, 0), 3, 1, true) == DroneFlight.Result.FAILURE) { holding = false; drone.status = flight.error; notifyOwner(drone.status); }
    }
    private void recallFailure(String error) {
        recalling = holding = false; resetAllNavigation(); drone.status = "Recall failed: " + error + "; previous task preserved"; notifyOwner(drone.status);
    }
    public void fail(String error) { drone.program.running = false; resetAllNavigation(); drone.status = error; notifyOwner(error); }
    private void notifyOwner(String message) { ServerPlayer p = drone.owner == null ? null : level().getServer().getPlayerList().getPlayer(drone.owner); if (p != null) p.sendSystemMessage(Component.literal(message)); }
    private void charge() {
        if (drone.tickCount % 10 != 0 || drone.home == null || !drone.homeDimension.equals(level().dimension().location().toString()) || drone.position().distanceToSqr(Vec3.atCenterOf(drone.home)) > 16) return;
        for (var pos : new BlockPos[]{drone.home, drone.home.below()}) {
            if (!level().hasChunkAt(pos)) continue;
            var source = level().getCapability(Capabilities.EnergyStorage.BLOCK, pos, net.minecraft.core.Direction.UP);
            if (source != null && drone.owner != null && actions.work.mayAccess(pos,net.minecraft.core.Direction.UP)) { int amount = drone.battery.receiveEnergy(source.extractEnergy(2000, true), true); drone.battery.receiveEnergy(source.extractEnergy(amount, false), false); break; }
        }
    }
    public CompoundTag save() {
        CompoundTag p = new CompoundTag(); p.putBoolean("active", recalling); p.putBoolean("holding", holding); p.putBoolean("home", recallHome); p.putBoolean("resume", autoResume); p.putBoolean("event", eventRunning); p.putInt("ticks", recallTicks);
        if (recallPlayer != null) p.putUUID("player", recallPlayer); return p;
    }
    public void load(CompoundTag p) {
        recalling = p.getBoolean("active"); holding = p.getBoolean("holding"); recallHome = p.getBoolean("home"); autoResume = p.getBoolean("resume"); eventRunning = p.getBoolean("event"); recallTicks = p.getInt("ticks"); recallPlayer = p.hasUUID("player") ? p.getUUID("player") : null;
        if (recallHome && drone.home == null) recalling = holding = false;
        resetAllNavigation();
    }
}
