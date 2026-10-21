package net.crystalnexus.entity;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.drone.*;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.world.inventory.DroneMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

public class LUNADroneEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DISPLAY = SynchedEntityData.defineId(LUNADroneEntity.class, EntityDataSerializers.STRING);
    public final AnimationState animationState0 = new AnimationState(), animationState2 = new AnimationState(), animationState4 = new AnimationState();
    public final ItemStackHandler inventory = new ItemStackHandler(9);
    public final ItemStackHandler destinationCards = new ItemStackHandler(9) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return stack.is(CrystalnexusModItems.LINK_CARD.get()); }
    };
    public final FluidTank tank = new FluidTank(16000);
    public final FluidTank recoveryTank = new FluidTank(16000);
    public final EnergyStorage battery = new EnergyStorage(200000, 2000, 200000, 200000);
    public final EnumMap<DroneMode, AssemblyGraph> programs = new EnumMap<>(DroneMode.class);
    public final DroneProgram program = new DroneProgram();
    public final Set<UUID> authorized = new HashSet<>();
    public final DroneRuntime runtime = new DroneRuntime(this);
    public UUID owner;
    public DroneMode mode = DroneMode.TRANSPORT;
    public BlockPos home;
    public String homeDimension = "";
    public String status = "Idle";

    public LUNADroneEntity(EntityType<LUNADroneEntity> type, Level world) {
        super(type, world);
        setPersistenceRequired(); setNoGravity(true);
        moveControl = new FlyingMoveControl(this, 10, true);
        for (DroneMode m : DroneMode.values()) { AssemblyGraph g = new AssemblyGraph(); g.nodes.add(DroneNode.START.create(0, 24, 24)); programs.put(m, g); }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(DISPLAY, "TRANSPORT | Idle | 100%"); }
    public String displayStatus() { return entityData.get(DISPLAY); }
    public AssemblyGraph graph() { return programs.get(mode); }
    public boolean ownedBy(Player player) { return owner != null && owner.equals(player.getUUID()); }
    public boolean permitted(UUID player) { return player != null && (player.equals(owner) || authorized.contains(player)); }
    public void setOwner(Player player) { owner = player.getUUID(); setHome(); if (level() instanceof ServerLevel l) DroneRegistry.get(l.getServer()).track(this); }
    public void setHome() { home = blockPosition(); homeDimension = level().dimension().location().toString(); }
    public void hover() { getNavigation().stop(); moveControl.setWantedPosition(getX(), getY(), getZ(), 0); setDeltaMovement(Vec3.ZERO); }
    public void cycleMode() { runtime.cancel(); program.stop(); mode = mode.next(); program.elapsed = 0; program.memory = new CompoundTag(); status = "Mode: " + mode; }
    public void open(ServerPlayer player) {
        if (!ownedBy(player)) { player.sendSystemMessage(Component.literal("You do not own this drone.")); return; }
        if (player.level() != level()) { player.sendSystemMessage(Component.literal("Drone is in another dimension.")); return; }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DroneMenu(id, inv, this), Component.literal("Universal Drone")), buf -> { buf.writeUUID(getUUID()); buf.writeNbt(view()); });
    }
    public CompoundTag view() {
        CompoundTag p = new CompoundTag(); p.putString("mode", mode.name()); p.putString("status", status);
        p.putIntArray("position",new int[]{getBlockX(),getBlockY(),getBlockZ()});
        p.putInt("energy", battery.getEnergyStored()); p.putInt("node", program.node); p.putBoolean("running", program.running);p.putIntArray("activeNodes",program.activeNodes());
        p.putBoolean("recalling", runtime.isRecalling()); p.putString("target", runtime.destination());
        p.put("cards",destinationCards.serializeNBT(registryAccess()));
        ListTag players=new ListTag();
        var allowed=new HashSet<>(authorized); if(owner!=null)allowed.add(owner);
        for(UUID id:allowed){CompoundTag entry=new CompoundTag();entry.putString("id",id.equals(owner)?"owner":id.toString());
            var online=level().getServer().getPlayerList().getPlayer(id);var cache=level().getServer().getProfileCache();
            String name=online!=null?online.getGameProfile().getName():cache==null?"Player "+id.toString().substring(0,8):cache.get(id).map(com.mojang.authlib.GameProfile::getName).orElse("Player "+id.toString().substring(0,8));
            entry.putString("name",id.equals(owner)?"Owner ("+name+")":name);players.add(entry);}
        p.put("players",players);
        p.put("graph", graph().save(registryAccess())); p.put("tank", tank.writeToNBT(registryAccess(), new CompoundTag()));
        return p;
    }
    @Override protected PathNavigation createNavigation(Level world) {
        return new FlyingPathNavigation(this, world) {
            @Override protected Path createPath(Set<BlockPos> targets, int padding, boolean above, int reach, float range) {
                // PathNavigationRegion loads its whole search box; reject before it can request missing chunks.
                int radius = (int)Math.min(24, range) + padding;
                BlockPos c = blockPosition();
                for (int x = (c.getX()-radius)>>4; x <= (c.getX()+radius)>>4; x++)
                    for (int z = (c.getZ()-radius)>>4; z <= (c.getZ()+radius)>>4; z++)
                        if (!world.hasChunk(x, z)) return null;
                return super.createPath(targets, padding, above, reach, Math.min(24, range));
            }
        };
    }
    @Override protected void registerGoals() {}
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override protected void checkFallDamage(double y, boolean grounded, BlockState state, BlockPos pos) {}
    @Override public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) { moveRelative(0.1F, input); move(MoverType.SELF, getDeltaMovement()); setDeltaMovement(getDeltaMovement().scale(0.8)); }
        else super.travel(input);
        calculateEntityAnimation(false);
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        if (owner == null) setOwner(player);
        if (!ownedBy(player)) { server.sendSystemMessage(Component.literal("You do not own this drone.")); return InteractionResult.FAIL; }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(CrystalnexusModItems.CRYSTAL_WRENCH.get()) && player.isShiftKeyDown()) { cycleMode(); }
        else if (held.is(CrystalnexusModItems.DRONE_CONTROLLER.get())) {
            net.crystalnexus.item.DroneControllerItem.bind(held, getUUID()); open(server);
        } else {
            var energy = held.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energy != null && player.isShiftKeyDown()) {
                int amount = battery.receiveEnergy(energy.extractEnergy(2000, true), true);
                battery.receiveEnergy(energy.extractEnergy(amount, false), false);
                server.sendSystemMessage(Component.literal("Drone battery: " + battery.getEnergyStored() + " FE"));
            } else open(server);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void tick() {
        super.tick(); setNoGravity(true);
        setBoundingBox(getType().getDimensions().makeBoundingBox(getX(), getY(), getZ()).move(0, 0.4, 0));
        if (level().isClientSide) { animationState0.animateWhen(true, tickCount); animationState4.animateWhen(true, tickCount); return; }
        runtime.tick();
        if (tickCount % 20 == 0) {
            DroneRegistry.get(((ServerLevel)level()).getServer()).track(this);
            entityData.set(DISPLAY, mode + " | " + status + " | " + battery.getEnergyStored()/2000 + "%");
        }
    }
    public CompoundTag saveDrone() {
        CompoundTag p = new CompoundTag(); if (owner != null) p.putUUID("owner", owner);
        ListTag auth = new ListTag(); for (UUID id : authorized) auth.add(StringTag.valueOf(id.toString())); p.put("authorized", auth);
        p.putString("mode", mode.name()); if (home != null) p.putLong("home", home.asLong()); p.putString("homeDimension", homeDimension);
        p.put("destinationCards",destinationCards.serializeNBT(registryAccess()));
        p.put("recoveryTank", recoveryTank.writeToNBT(registryAccess(), new CompoundTag()));
        p.putInt("energy", battery.getEnergyStored()); p.put("inventory", inventory.serializeNBT(registryAccess())); p.put("tank", tank.writeToNBT(registryAccess(), new CompoundTag()));
        CompoundTag graphs = new CompoundTag(); programs.forEach((m,g) -> graphs.put(m.name(), g.save(registryAccess()))); p.put("programs", graphs);
        p.put("execution", program.save()); p.put("recall", runtime.save()); p.putString("status", status); return p;
    }
    public void loadDrone(CompoundTag p) {
        owner = p.hasUUID("owner") ? p.getUUID("owner") : null; authorized.clear();
        for (Tag raw : p.getList("authorized", Tag.TAG_STRING)) try { authorized.add(UUID.fromString(raw.getAsString())); } catch (IllegalArgumentException ignored) {}
        mode = DroneMode.read(p.getString("mode")); home = p.contains("home") ? BlockPos.of(p.getLong("home")) : null; homeDimension = p.getString("homeDimension");
        battery.extractEnergy(battery.getEnergyStored(), false); battery.receiveEnergy(Math.max(0, Math.min(200000, p.getInt("energy"))), false);
        // Receiving is rate limited; restore persisted charge in bounded increments.
        int remaining = Math.max(0, Math.min(200000, p.getInt("energy"))) - battery.getEnergyStored();
        while (remaining > 0) remaining -= battery.receiveEnergy(remaining, false);
        if(p.contains("destinationCards"))destinationCards.deserializeNBT(registryAccess(),p.getCompound("destinationCards"));
        recoveryTank.readFromNBT(registryAccess(), p.getCompound("recoveryTank"));
        inventory.deserializeNBT(registryAccess(), p.getCompound("inventory")); tank.readFromNBT(registryAccess(), p.getCompound("tank"));
        for (DroneMode m : DroneMode.values()) if (p.getCompound("programs").contains(m.name())) {
            try { AssemblyGraph g = AssemblyGraph.load(p.getCompound("programs").getCompound(m.name()), registryAccess());
                if (DroneProgram.validate(g, m).isEmpty()) programs.put(m, g);
            } catch (IllegalArgumentException e) { status = "Invalid saved program; stopped"; }
        }
        status=p.getString("status");
        try{program.load(p.getCompound("execution"));}catch(IllegalArgumentException e){program.stop();status="Invalid saved tasks; stopped";}
        runtime.load(p.getCompound("recall"));
        if (graph().node(program.node) == null) program.running = false;
    }
    @Override public void addAdditionalSaveData(CompoundTag p) { super.addAdditionalSaveData(p); p.put("Drone", saveDrone()); }
    @Override public void readAdditionalSaveData(CompoundTag p) { super.readAdditionalSaveData(p); if (p.contains("Drone")) loadDrone(p.getCompound("Drone")); }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        runtime.cancel(); program.running = false;
        ItemStack packed = new ItemStack(CrystalnexusModItems.UNIVERSAL_DRONE.get());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, packed, p -> p.put("Drone", saveDrone()));
        spawnAtLocation(packed); DroneRegistry.get(level.getServer()).forget(getUUID());
        for (int i=0;i<inventory.getSlots();i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        tank.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        for(int i=0;i<9;i++)destinationCards.setStackInSlot(i,ItemStack.EMPTY);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.FLYING_SPEED, .3)
            .add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 24);
    }
}
