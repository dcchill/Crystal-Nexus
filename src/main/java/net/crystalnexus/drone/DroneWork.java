package net.crystalnexus.drone;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.entity.LUNADroneEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import java.util.Locale;

final class DroneWork {
    final DroneActions a;
    final LUNADroneEntity d;
    DroneWork(DroneActions actions) { a = actions; d = a.d; }
    private CompoundTag memory() { return a.memory(); }
    private Direction side(CompoundTag p) { return p.contains("side") ? Direction.valueOf(p.getString("side").toUpperCase(Locale.ROOT)) : Direction.UP; }
    boolean inventoryFull() { for (int i=0;i<d.inventory.getSlots();i++) { ItemStack s=d.inventory.getStackInSlot(i); if (s.isEmpty() || s.getCount()<Math.min(s.getMaxStackSize(),d.inventory.getSlotLimit(i))) return false; } return true; }
    boolean inventoryEmpty() { for (int i=0;i<d.inventory.getSlots();i++) if (!d.inventory.getStackInSlot(i).isEmpty()) return false; return true; }
    boolean matchesItem(ItemStack stack, String filter) {
        if (stack.isEmpty()) return false;
        if (filter.isEmpty()) return true;
        if (filter.startsWith("#")) return stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(filter.substring(1))));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(filter);
    }
    boolean matchesBlock(BlockPos pos, String filter) {
        var state = a.r.level().getBlockState(pos); if (state.isAir()) return false;
        if (filter.isEmpty()) return true;
        if (filter.startsWith("#")) return state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(filter.substring(1))));
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(filter);
    }
    private boolean componentsMatch(ItemStack stack, CompoundTag p) {
        CompoundTag wanted = p.contains("components") ? p.getCompound("components") : memory().getCompound("components");
        if (wanted.isEmpty()) return true;
        Tag saved = stack.save(a.r.level().registryAccess());
        return saved instanceof CompoundTag tag && NbtUtils.compareNbt(wanted, tag.get("components"), true);
    }
    IItemHandler itemHandler(CompoundTag p) { return a.r.level().getCapability(Capabilities.ItemHandler.BLOCK, a.pos(p), side(p)); }
    private IFluidHandler fluidHandler(CompoundTag p) { return a.r.level().getCapability(Capabilities.FluidHandler.BLOCK, a.pos(p), side(p)); }
    boolean canWork(int energy) { return d.battery.getEnergyStored() >= energy && d.owner != null; }
    boolean mayAccess(BlockPos pos, Direction face) {
        var player=actor();
        if(!a.r.level().mayInteract(player,pos))return false;
        var event=net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player,InteractionHand.MAIN_HAND,pos,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
        return !event.isCanceled() && event.getUseBlock()!=net.neoforged.neoforge.common.util.TriState.FALSE;
    }
    int transferItems(CompoundTag p, boolean extract) {
        BlockPos pos = a.pos(p); if (!a.loaded(pos)) return a.failure("Inventory chunk is unloaded");
        if (!a.near(pos)) return a.navigate(Vec3.atCenterOf(pos).add(0,1,0), 3, p, false) == 2 ? 2 : -1;
        IItemHandler external = itemHandler(p); if (external == null) return a.failure("No inventory on the selected face");
        if (!mayAccess(pos,side(p))) return a.failure("Inventory access is denied by protection");
        if (!canWork(20)) return a.failure("Insufficient battery");
        IItemHandler source = extract ? external : d.inventory, dest = extract ? d.inventory : external;
        for (int i=0;i<source.getSlots();i++) {
            ItemStack preview = source.extractItem(i, a.amount(p), true);
            if (!matchesItem(preview,a.filter(p)) || !componentsMatch(preview,p)) continue;
            int accepted = preview.getCount() - ItemHandlerHelper.insertItemStacked(dest, preview.copy(), true).getCount();
            if (accepted == 0) continue;
            ItemStack taken = source.extractItem(i, accepted, false);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(dest, taken, false);
            if (!remainder.isEmpty()) { remainder = source.insertItem(i, remainder, false); stash(remainder); }
            d.battery.extractEnergy(20, false); return 0;
        }
        return a.failure("No matching items or destination is full");
    }
    private void stash(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(d.inventory, stack, false);
        if (!remainder.isEmpty()) d.spawnAtLocation(remainder);
    }
    int collect(CompoundTag p) {
        if (!canWork(10)) return a.failure("Insufficient battery");
        for (ItemEntity item : a.r.level().getEntitiesOfClass(ItemEntity.class, d.getBoundingBox().inflate(Math.min(3,a.range(p))))) {
            if (item.hasPickUpDelay() || !matchesItem(item.getItem(),a.filter(p))) continue;
            ItemStack left = ItemHandlerHelper.insertItemStacked(d.inventory, item.getItem().copy(), false);
            if (left.getCount() == item.getItem().getCount()) continue;
            if (left.isEmpty()) item.discard(); else item.setItem(left);
            d.battery.extractEnergy(10,false); return 0;
        }
        return 1;
    }
    int drop(CompoundTag p) {
        for (int i=0;i<d.inventory.getSlots();i++) if (matchesItem(d.inventory.getStackInSlot(i), a.filter(p))) {
            ItemStack stack = d.inventory.extractItem(i, a.amount(p), false); d.spawnAtLocation(stack); return 0;
        }
        return 1;
    }
    boolean hasDeliverySpace(ServerPlayer player) {
        var dest = new PlayerMainInvWrapper(player.getInventory());
        for (int i=0;i<d.inventory.getSlots();i++) { ItemStack stack=d.inventory.getStackInSlot(i); if (!stack.isEmpty() && ItemHandlerHelper.insertItemStacked(dest,stack.copy(),true).getCount() < stack.getCount()) return true; }
        return false;
    }
    int deliver(CompoundTag p) {
        ServerPlayer player = a.r.player(a.recipient(p));
        if (player == null) return a.failure("Recipient is offline or unauthorized; items retained");
        if (player.level() != d.level()) return a.failure("Recipient is in another dimension; items retained");
        if (d.distanceToSqr(player) > 16) return a.navigate(player.position().add(0,1,0), 2.5, p, false) == 2 ? 2 : -1;
        if (!canWork(20)) return a.failure("Insufficient battery");
        var dest = new PlayerMainInvWrapper(player.getInventory()); boolean moved=false;
        for (int i=0;i<d.inventory.getSlots();i++) {
            ItemStack stack = d.inventory.getStackInSlot(i); if (!matchesItem(stack,a.filter(p))) continue;
            ItemStack left = ItemHandlerHelper.insertItemStacked(dest, stack.copy(), false);
            if (left.getCount() != stack.getCount()) { moved=true; d.inventory.setStackInSlot(i,left); }
        }
        if (moved) { d.battery.extractEnergy(20,false); player.getInventory().setChanged(); return 0; }
        return a.failure("Recipient inventory is full or no matching items; undelivered items retained");
    }
    private boolean matchesFluid(FluidStack fluid, String filter) {
        if (fluid.isEmpty()) return false;
        if (filter.isEmpty()) return true;
        if (filter.startsWith("#")) return fluid.is(TagKey.create(Registries.FLUID,ResourceLocation.parse(filter.substring(1))));
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(filter);
    }
    boolean fluidAvailable(CompoundTag p) {
        if (!a.loaded(a.pos(p)) || !a.near(a.pos(p))) return false;
        IFluidHandler handler = fluidHandler(p);
        if (handler == null) return false;
        for (int i=0;i<handler.getTanks();i++) if (matchesFluid(handler.getFluidInTank(i),a.filter(p))) return true;
        return false;
    }
    int transferFluid(CompoundTag p, boolean extract) {
        BlockPos pos=a.pos(p); if (!a.loaded(pos)) return a.failure("Fluid tank chunk is unloaded");
        if (!a.near(pos)) return a.navigate(Vec3.atCenterOf(pos).add(0,1,0),3,p,false) == 2 ? 2 : -1;
        IFluidHandler external=fluidHandler(p); if (external==null) return a.failure("No fluid tank on selected face");
        if (!mayAccess(pos,side(p))) return a.failure("Fluid tank access is denied by protection");
        if (!canWork(20)) return a.failure("Insufficient battery");
        if (extract && !d.recoveryTank.isEmpty()) return a.failure("Insert retained recovery fluid before extracting more");
        IFluidHandler source=extract?external:!d.recoveryTank.isEmpty()?d.recoveryTank:d.tank, dest=extract?d.tank:external;
        for (int i=0;i<source.getTanks();i++) {
            FluidStack requested=source.getFluidInTank(i).copy(); if (!matchesFluid(requested,a.filter(p))) continue;
            requested.setAmount(Math.min(a.amount(p),requested.getAmount()));
            FluidStack preview=source.drain(requested,IFluidHandler.FluidAction.SIMULATE);
            int accepted=dest.fill(preview,IFluidHandler.FluidAction.SIMULATE); if (accepted<=0) continue;
            preview.setAmount(Math.min(accepted,preview.getAmount()));
            FluidStack taken=source.drain(preview,IFluidHandler.FluidAction.EXECUTE);
            int inserted=dest.fill(taken,IFluidHandler.FluidAction.EXECUTE);
            if (inserted<taken.getAmount()) {
                FluidStack remaining=taken.copy(); remaining.shrink(inserted);
                remaining.shrink(source.fill(remaining,IFluidHandler.FluidAction.EXECUTE));
                if (!remaining.isEmpty()) {
                    // Retain a handler's uncommitted remainder independently of mutable program state.
                    d.recoveryTank.setFluid(remaining);
                    return a.failure("Fluid handler changed capacity; remainder retained for recovery");
                }
            }
            d.battery.extractEnergy(20,false); return 0;
        }
        return a.failure("No matching fluid or destination tank is full");
    }
    int scan(CompoundTag p, String type) {
        BlockPos center=a.pos(p); int radius=Math.min(16,Math.max(1,(int)a.range(p))), width=radius*2+1;
        int cursor=memory().getInt("scan"), total=width*width*width;
        BlockPos best = memory().contains("scanBest") ? BlockPos.of(memory().getLong("scanBest")) : null;
        for (int budget=0;budget<64 && cursor<total;budget++,cursor++) {
            BlockPos candidate=center.offset(cursor%width-radius,(cursor/width)%width-radius,cursor/(width*width)-radius);
            if (!a.loaded(candidate)) continue;
            boolean match=switch(type) { case "item" -> a.r.level().getCapability(Capabilities.ItemHandler.BLOCK,candidate,side(p))!=null; case "fluid" -> a.r.level().getCapability(Capabilities.FluidHandler.BLOCK,candidate,side(p))!=null; default -> matchesBlock(candidate,a.filter(p)); };
            if (match && (best == null || candidate.distSqr(d.blockPosition()) < best.distSqr(d.blockPosition()))) best = candidate;
        }
        if (best != null) memory().putLong("scanBest", best.asLong());
        memory().putInt("scan",cursor);
        if (cursor < total) return -1;
        memory().remove("scanBest");
        if (best == null) return 1;
        memory().putLong("position",best.asLong()); return 0;
    }
    BlockPos end(CompoundTag p) {
        if (p.contains("end")) { int[] v=p.getIntArray("end"); if(v.length!=3) throw new IllegalArgumentException("end requires three coordinates"); return new BlockPos(v[0],v[1],v[2]); }
        return memory().contains("areaEnd") ? BlockPos.of(memory().getLong("areaEnd")) : a.pos(p);
    }
    int area(CompoundTag p, boolean build) {
        BlockPos start=!p.contains("pos") && memory().contains("areaStart")?BlockPos.of(memory().getLong("areaStart")):a.pos(p), end=end(p);
        int x=Math.min(start.getX(),end.getX()),y=Math.min(start.getY(),end.getY()),z=Math.min(start.getZ(),end.getZ());
        long sx=Math.abs((long)start.getX()-end.getX())+1,sy=Math.abs((long)start.getY()-end.getY())+1,sz=Math.abs((long)start.getZ()-end.getZ())+1;
        if (sx>4096 || sy>4096 || sz>4096 || sx*sy*sz>4096) return a.failure("Area is limited to 4096 blocks");
        int index=memory().getInt("areaIndex"); if(index>=sx*sy*sz) {memory().remove("areaIndex");return 0;}
        BlockPos next=new BlockPos(x+(int)(index%sx),y+(int)((index/sx)%sy),z+(int)(index/(sx*sy)));
        if (!a.loaded(next)) return a.failure("Work area enters an unloaded chunk");
        boolean skip=build ? !a.r.level().getBlockState(next).canBeReplaced() : !matchesBlock(next,a.filter(p));
        int result=skip?0:build?place(next,p):mine(next,p,false);
        if(result==0 || result==1) {memory().putInt("areaIndex",index+1);memory().remove("work");a.r.resetNavigation();return -1;}
        return result;
    }
    FakePlayer actor() {
        if(d.owner==null) throw new IllegalArgumentException("Drone has no owner");
        var cache=a.r.level().getServer().getProfileCache();
        var profile=cache==null?new GameProfile(d.owner,"DroneOwner"):cache.get(d.owner).orElse(new GameProfile(d.owner,"DroneOwner"));
        FakePlayer player=FakePlayerFactory.get(a.r.level(),profile); player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(d.getX(),d.getY(),d.getZ()); return player;
    }
    int mine(BlockPos pos, CompoundTag p, boolean replacing) {
        if(!a.loaded(pos)) return a.failure("Mining chunk is unloaded");
        if(d.home!=null && d.homeDimension.equals(d.level().dimension().location().toString()) && (pos.equals(d.home) || pos.equals(d.home.below()))) return a.failure("Refusing to mine home infrastructure");
        var state=a.r.level().getBlockState(pos); if(state.isAir()) return 0;
        if(state.getDestroySpeed(a.r.level(),pos)<0) return memory().getBoolean("skipUnbreakable")?1:a.failure("Block is unbreakable");
        if(!replacing && !matchesBlock(pos,a.filter(p))) return 1;
        if(inventoryFull()) return a.failure("Drone inventory is full; block retained");
        if(!a.near(pos)) return a.navigate(Vec3.atCenterOf(pos).add(0,1,0),3,p,false)==2?2:-1;
        if(!canWork(100)) return a.failure("Insufficient battery to mine");
        FakePlayer player=actor();
        if(!a.r.level().mayInteract(player,pos)) return a.failure("Mining is not permitted here");
        int tool=-1;
        for(int i=0;i<d.inventory.getSlots();i++){
            ItemStack candidate=d.inventory.getStackInSlot(i);
            if(!(candidate.getItem() instanceof PickaxeItem)&&!candidate.is(net.minecraft.tags.ItemTags.PICKAXES))continue;
            if(tool<0||!d.inventory.getStackInSlot(tool).isCorrectToolForDrops(state)&&candidate.isCorrectToolForDrops(state))tool=i;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,tool>=0?d.inventory.getStackInSlot(tool).copy():ItemStack.EMPTY);
        try {
            if(state.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(state)) return a.failure("A suitable mining tool is required in the drone inventory");
            if(!player.gameMode.destroyBlock(pos)) return a.failure("Block breaking denied by protection or game rules");
            if(tool>=0) d.inventory.setStackInSlot(tool,player.getMainHandItem().copy());
            d.battery.extractEnergy(100,false);memory().remove("work");collect(p);return 0;
        } finally {player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
    }
    int material(CompoundTag p) {for(int i=0;i<d.inventory.getSlots();i++) if(d.inventory.getStackInSlot(i).getItem() instanceof BlockItem && matchesItem(d.inventory.getStackInSlot(i),a.filter(p))) return i;return -1;}
    int place(BlockPos pos, CompoundTag p) {
        if(!a.loaded(pos)) return a.failure("Building chunk is unloaded");
        if(!a.r.level().getBlockState(pos).canBeReplaced()) return a.failure("Destination block is occupied");
        int slot=material(p); if(slot<0) return a.failure("Required building material is missing");
        if(!a.near(pos)) return a.navigate(Vec3.atCenterOf(pos).add(0,1,0),3,p,false)==2?2:-1;
        if(!canWork(100)) return a.failure("Insufficient battery to build");
        FakePlayer player=actor(); if(!a.r.level().mayInteract(player,pos)) return a.failure("Building is not permitted here");
        player.setItemInHand(InteractionHand.MAIN_HAND,d.inventory.getStackInSlot(slot).copy());
        try {
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var result=player.gameMode.useItemOn(player,a.r.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            d.inventory.setStackInSlot(slot,player.getMainHandItem().copy());
            if(!result.consumesAction() || a.r.level().getBlockState(pos).canBeReplaced()) return a.failure("Placement rejected by protection or placement rules");
            d.battery.extractEnergy(100,false);return 0;
        } finally {player.stopUsingItem();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
    }
    int blueprint(CompoundTag p) {
        ListTag blocks=p.getList("blocks",Tag.TAG_COMPOUND); if(blocks.isEmpty() || blocks.size()>128) return a.failure("Blueprint requires 1 to 128 blocks");
        for(Tag t:blocks) {CompoundTag b=(CompoundTag)t; int[] v=b.getIntArray("offset"); ResourceLocation id=ResourceLocation.tryParse(b.getString("item"));
            if(v.length!=3 || Math.abs((long)v[0])>64 || Math.abs((long)v[1])>64 || Math.abs((long)v[2])>64 || id==null || !(BuiltInRegistries.ITEM.get(id) instanceof BlockItem)) return a.failure("Invalid blueprint block or offset");}
        memory().put("blueprint",blocks.copy());memory().putLong("origin",a.pos(p).asLong());memory().putInt("blueprintIndex",0);return 0;
    }
    int nextBlueprint() {
        ListTag blocks=memory().getList("blueprint",Tag.TAG_COMPOUND);int index=memory().getInt("blueprintIndex");if(index>=blocks.size()) return 1;
        CompoundTag b=blocks.getCompound(index);int[] v=b.getIntArray("offset");
        memory().putLong("position",BlockPos.of(memory().getLong("origin")).offset(v[0],v[1],v[2]).asLong());memory().putString("filter",b.getString("item"));memory().putInt("blueprintIndex",index+1);return 0;
    }
}
