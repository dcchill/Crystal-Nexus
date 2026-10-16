package net.crystalnexus.drone;

import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.Objects;

final class DroneCombat {
    private final DroneActions a;
    private final LUNADroneEntity d;
    DroneCombat(DroneActions actions) { a=actions;d=a.d; }
    ListTag filters() {if(!a.memory().contains("filters")) a.memory().put("filters",new ListTag());return a.memory().getList("filters",Tag.TAG_COMPOUND);}
    void add(DroneNode kind, CompoundTag p) {
        ListTag filters=filters();if(filters.size()>=32) throw new IllegalArgumentException("Too many targeting filters; use Stop Attacking to clear");
        CompoundTag filter=p.copy();filter.putString("kind",kind.name());filters.add(filter);
        if(kind==DroneNode.TARGET_TAMED_ENTITIES) a.memory().putBoolean("allowTamed",true);
    }
    void combine(DroneNode kind) {
        ListTag filters=filters();CompoundTag expr=new CompoundTag();expr.putString("kind",kind.name());
        expr.put("a",filters.remove(filters.size()-1));if(kind!=DroneNode.NOT) expr.put("b",filters.remove(filters.size()-1));filters.add(expr);
    }
    private boolean test(CompoundTag p, LivingEntity entity, int depth) {
        if(depth>32) return false;
        DroneNode kind=DroneNode.valueOf(p.getString("kind"));String id=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return switch(kind) {
            case AND -> test(p.getCompound("a"),entity,depth+1)&&test(p.getCompound("b"),entity,depth+1);
            case OR -> test(p.getCompound("a"),entity,depth+1)||test(p.getCompound("b"),entity,depth+1);
            case NOT -> !test(p.getCompound("a"),entity,depth+1);
            case TARGET_HOSTILE_MOBS -> entity instanceof Enemy;
            case TARGET_PASSIVE_MOBS -> entity instanceof net.minecraft.world.entity.animal.Animal;
            case TARGET_SPECIFIC_ENTITY -> id.equals(p.getString("filter"));
            case EXCLUDE_ENTITY -> !id.equals(p.getString("filter"));
            case TARGET_ENTITY_TAG -> entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(p.getString("filter").replaceFirst("^#",""))));
            case TARGET_WITHIN_RADIUS -> d.distanceTo(entity)<=a.range(p);
            case TARGET_ADULT_ENTITIES -> !entity.isBaby();
            case TARGET_BABY_ENTITIES -> entity.isBaby();
            case TARGET_TAMED_ENTITIES -> entity instanceof TamableAnimal animal && animal.isTame();
            case TARGET_UNTAMED_ENTITIES -> !(entity instanceof TamableAnimal animal) || !animal.isTame();
            case TARGET_LOW_HEALTH -> entity.getHealth()<=(p.contains("value")?p.getDouble("value"):5);
            default -> false;
        };
    }
    boolean safe(LivingEntity entity, CompoundTag p) {
        double maxRange=p.contains("maxRange")?p.getDouble("maxRange"):32;
        if(!Double.isFinite(maxRange) || maxRange<=0 || !entity.isAlive() || entity==d || entity instanceof Player || d.owner==null || entity.getUUID().equals(d.owner) || d.distanceTo(entity)>Math.min(64,maxRange)) return false;
        if(entity instanceof LUNADroneEntity other && Objects.equals(d.owner,other.owner)) return false;
        ServerPlayer owner=a.r.level().getServer().getPlayerList().getPlayer(d.owner);
        if(owner!=null && owner.isAlliedTo(entity)) return false;
        if(entity instanceof TamableAnimal animal && animal.isTame() && (!a.memory().getBoolean("allowTamed") || d.permitted(animal.getOwnerUUID()))) return false;
        if(p.contains("end")) {var first=a.pos(p);var last=a.work.end(p);var box=new AABB(Math.min(first.getX(),last.getX()),Math.min(first.getY(),last.getY()),Math.min(first.getZ(),last.getZ()),Math.max(first.getX(),last.getX())+1,Math.max(first.getY(),last.getY())+1,Math.max(first.getZ(),last.getZ())+1);if(!box.contains(entity.position()))return false;}
        return true;
    }
    int select(CompoundTag p, boolean wait) {
        double radius=Math.max(1,Math.min(32,a.range(p)));ListTag filters=filters();LivingEntity nearest=null;double best=Double.MAX_VALUE;
        for(LivingEntity entity:a.r.level().getEntitiesOfClass(LivingEntity.class,d.getBoundingBox().inflate(radius))) {
            if(!safe(entity,p)) continue;
            boolean match=filters.isEmpty()?(d.mode!=DroneMode.COMBAT || entity instanceof Enemy):true;
            if(!p.getString("filter").isEmpty()) {
                String id=p.getString("filter");
                match &= id.startsWith("#")?entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(id.substring(1)))):BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(id);
            }
            for(Tag raw:filters) match &= test((CompoundTag)raw,entity,0);
            if(!match)continue;double distance=d.distanceToSqr(entity);if(distance<best){best=distance;nearest=entity;}
        }
        if(nearest!=null){a.memory().putUUID("target",nearest.getUUID());return 0;}
        a.memory().remove("target");return wait?a.waitFor(false):1;
    }
    int attack(CompoundTag p, boolean untilDead) {
        LivingEntity entity=a.target();if(entity==null) return untilDead?0:a.failure("No living target selected");
        if(!safe(entity,p)) return a.failure("Target is protected, friendly or out of range");
        if(d.distanceToSqr(entity)>6.25 || !d.hasLineOfSight(entity)) return a.navigate(entity.position(),1.5,p,false)==2?2:-1;
        if(a.memory().getLong("attackAt")>a.r.level().getGameTime())return -1;
        if(!a.work.canWork(100))return a.failure("Insufficient battery to attack");
        var player=a.work.actor();
        if(!net.neoforged.neoforge.common.CommonHooks.onPlayerAttackTarget(player,entity))return a.failure("Attack denied by protection");
        if(!entity.hurt(d.damageSources().playerAttack(player),3))return a.failure("Damage was denied or target is invulnerable");
        d.battery.extractEnergy(100,false);a.memory().putLong("attackAt",a.r.level().getGameTime()+20);return untilDead && entity.isAlive()?-1:0;
    }
    int defend(CompoundTag p, boolean defendPlayer) {
        ServerPlayer player=defendPlayer?a.r.player(a.recipient(p)):null;
        if(defendPlayer && (player==null || player.level()!=d.level()))return a.failure("Defended player is unavailable");
        LivingEntity entity=a.target();
        if(entity==null || !safe(entity,p) || defendPlayer && !(entity instanceof Mob mob && mob.getTarget()==player)) {
            a.memory().remove("target");
            for(Mob mob:a.r.level().getEntitiesOfClass(Mob.class,d.getBoundingBox().inflate(Math.min(32,Math.max(1,a.range(p)))))) {
                if(!safe(mob,p) || (defendPlayer?mob.getTarget()!=player:!(mob instanceof Enemy)))continue;
                boolean matches=true;for(Tag raw:filters())matches &= test((CompoundTag)raw,mob,0);
                if(matches){a.memory().putUUID("target",mob.getUUID());break;}
            }
            entity=a.target();
        }
        if(entity!=null){int result=attack(p,true);return result==2?2:-1;}
        return defendPlayer?a.follow(p,true):-1;
    }
}
