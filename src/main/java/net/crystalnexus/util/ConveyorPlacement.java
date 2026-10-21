package net.crystalnexus.util;

import net.crystalnexus.block.*;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.CommonHooks;
import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid = "crystalnexus")
public final class ConveyorPlacement {
    public static final String KEY = "crystalnexusConveyorRoute";
    public static boolean placing;
    private ConveyorPlacement() {}
    public static boolean eligible(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && ConveyorGeometry.isBelt(item.getBlock().defaultBlockState());
    }
    public static BlockPos endpoint(Level level, Player player, ItemStack belts, BlockHitResult hit) {
        return ConveyorGeometry.isBelt(level.getBlockState(hit.getBlockPos())) ? hit.getBlockPos()
            : new BlockPlaceContext(level,player,InteractionHand.OFF_HAND,belts,hit).getClickedPos();
    }
    public static CompoundTag preview(ItemStack wrench) {
        return wrench.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY);
    }
    public static void clear(ItemStack wrench) {
        CustomData.update(DataComponents.CUSTOM_DATA,wrench,tag -> tag.remove(KEY));
    }
    public static boolean cancel(Player player) {
        ItemStack wrench = player.getMainHandItem();
        if (!(wrench.getItem() instanceof net.crystalnexus.item.CrystalWrenchItem) || preview(wrench).isEmpty()) return false;
        clear(wrench);
        message(player, "Conveyor placement cancelled");
        return true;
    }
    public static InteractionResult use(UseOnContext ctx) {
        Player player=ctx.getPlayer();
        if(player==null || !eligible(player.getOffhandItem())) return InteractionResult.PASS;
        Level level=ctx.getLevel();
        if(level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack wrench=ctx.getItemInHand(), belts=player.getOffhandItem();
        CompoundTag data=preview(wrench);
        BlockPos end=endpoint(level,player,belts,new BlockHitResult(ctx.getClickLocation(),ctx.getClickedFace(),ctx.getClickedPos(),ctx.isInside()));
        String dimension=level.dimension().location().toString();
        String item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(belts.getItem()).toString();
        if(!data.isEmpty() && (!dimension.equals(data.getString("dimension")) || !item.equals(data.getString("item")))) {
            clear(wrench); message(player,"Conveyor route cancelled: dimension or belt changed"); return InteractionResult.FAIL;
        }
        if(data.isEmpty()) {
            data.putLong("start",end.asLong()); data.putBoolean("xFirst",player.getDirection().getAxis()==Direction.Axis.X);
            data.putString("dimension",dimension); data.putString("item",item);
            write(wrench,data); message(player,"Conveyor node set. Click next node to build; sneak-use switches bend; left-click cancels");
            return InteractionResult.SUCCESS;
        }
        if(player.isShiftKeyDown()) {
            data.putBoolean("xFirst",!data.getBoolean("xFirst"));write(wrench,data);
            message(player,data.getBoolean("xFirst")?"Conveyor: X-first":"Conveyor: Z-first");return InteractionResult.SUCCESS;
        }
        BlockPos start=BlockPos.of(data.getLong("start"));
        if(start.equals(end)) {clear(wrench);message(player,"Conveyor route cancelled");return InteractionResult.SUCCESS;}
        try {
            List<ConveyorGeometry.Segment> route=plan(level,start,end,data.getBoolean("xFirst"));
            String problem=validate(level,player,belts,route);
            if(problem!=null) throw new IllegalArgumentException(problem);
            List<ConveyorGeometry.Segment> fresh=route.stream().filter(s -> !ConveyorGeometry.isBelt(level.getBlockState(s.pos()))).toList();
            int needed=fresh.size();
            if(!player.isCreative() && available(player,belts)<needed) throw new IllegalArgumentException("Need "+needed+" belts; have "+available(player,belts));
            Map<BlockPos,BlockState> old=new LinkedHashMap<>();
            Map<BlockPos,CompoundTag> oldData=new HashMap<>();
            placing=true;
            try {
                for(var segment:fresh) {
                    BlockPos pos=segment.pos();old.put(pos,level.getBlockState(pos));
                    if(level.getBlockEntity(pos)!=null) oldData.put(pos,level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
                    var result=CommonHooks.onPlaceItemIntoWorld(new Context(level,player,belts.copyWithCount(1),segment));
                    if(!result.consumesAction() || !ConveyorGeometry.isBelt(level.getBlockState(pos)))
                        throw new IllegalArgumentException("Placement denied at "+pos.toShortString());
                }
                for(int i=0;i<route.size();i++) {
                    var segment=route.get(i);
                    if(!old.containsKey(segment.pos())) continue;
                    level.setBlock(segment.pos(),segment.state(level.getBlockState(segment.pos())),Block.UPDATE_ALL);
                    if(level.getBlockEntity(segment.pos()) instanceof ConveyerBeltBaseBlockEntity belt)
                        belt.setSplineConnections(i>0?route.get(i-1).pos():null,i+1<route.size()?route.get(i+1).pos():null);
                }
            } catch(RuntimeException error) {
                var positions=new ArrayList<>(old.keySet());Collections.reverse(positions);
                for(BlockPos pos:positions) {
                    // The source items have not been consumed; rollback must not drop their contents.
                    if(level.getBlockEntity(pos) instanceof ConveyerBeltBaseBlockEntity belt) belt.clearContent();
                    level.setBlock(pos,old.get(pos),Block.UPDATE_ALL);
                    if(oldData.containsKey(pos) && level.getBlockEntity(pos)!=null)
                        level.getBlockEntity(pos).loadWithComponents(oldData.get(pos),level.registryAccess());
                }
                throw error;
            } finally { placing=false; }
            if(!player.isCreative()) consume(player,belts,needed);
            data.putLong("start",end.asLong());write(wrench,data);
            message(player,"Placed "+needed+" conveyor belts. Click next node to continue; left-click cancels");return InteractionResult.SUCCESS;
        } catch(IllegalArgumentException error) { message(player,error.getMessage());return InteractionResult.FAIL; }
    }
    public static List<ConveyorGeometry.Segment> plan(Level level, BlockPos start, BlockPos end, boolean xFirst) {
        if (!level.hasChunkAt(start) || !level.hasChunkAt(end)) throw new IllegalArgumentException("Endpoints must be in loaded chunks");
        BlockState first=level.getBlockState(start), last=level.getBlockState(end);
        boolean snapStart=ConveyorGeometry.isBelt(first), snapEnd=ConveyorGeometry.isBelt(last);
        var route=new ArrayList<>(ConveyorGeometry.plan(
            snapStart?start.above(ConveyorGeometry.outputHeight(first)):start,
            snapEnd?end.above(ConveyorGeometry.inputHeight(last)):end,xFirst));
        if(snapStart) route.set(0,new ConveyorGeometry.Segment(start,ConveyorGeometry.output(first),
            first.getValue(ConveyerBeltBlock.CURVE),first.getValue(ConveyorGeometry.SLOPE)));
        if(snapEnd) route.set(route.size()-1,new ConveyorGeometry.Segment(end,ConveyorGeometry.output(last),
            last.getValue(ConveyerBeltBlock.CURVE),last.getValue(ConveyorGeometry.SLOPE)));
        return List.copyOf(route);
    }

    public static String validate(Level level,Player player,ItemStack belts,List<ConveyorGeometry.Segment> route) {
        BlockState base=((BlockItem)belts.getItem()).getBlock().defaultBlockState();
        int fresh=0;
        for(int i=0;i<route.size();i++) {
            var segment=route.get(i);BlockPos pos=segment.pos();
            if(!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above()) || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) return "Route outside loaded/buildable area";
            BlockState old=level.getBlockState(pos), state=segment.state(base);
            if(ConveyorGeometry.isBelt(old)) {
                if(i!=0 && i!=route.size()-1) return "Route crosses an existing belt";
                var adjacent=route.get(i==0?1:i-1);
                boolean matches=i==0?ConveyorGeometry.connects(pos,old,adjacent.pos(),adjacent.state(base)):
                    ConveyorGeometry.connects(adjacent.pos(),adjacent.state(base),pos,old);
                if(!matches) return "Endpoint direction or height does not match";
                BlockPos occupied=ConveyorGeometry.neighbor(level,pos,old,i==0);
                if(ConveyorGeometry.isBelt(level.getBlockState(occupied))) return "Endpoint already connected";
                continue;
            }
            fresh++;
            Context context=new Context(level,player,belts.copyWithCount(1),segment);
            if(!old.canBeReplaced(context) || !player.mayUseItemAt(pos,Direction.UP,belts)
                || !state.canSurvive(level,pos) || !level.isUnobstructed(state,pos,CollisionContext.of(player))) return "Route blocked at "+pos.toShortString();
            if(segment.slope()!=ConveyorGeometry.Slope.FLAT && (!level.getBlockState(pos.above()).canBeReplaced()
                || level.isOutsideBuildHeight(pos.above()))) return "Ramp needs clearance above "+pos.toShortString();
        }
        return fresh>256?"Route exceeds 256 new belts":null;
    }
    public static int available(Player player,ItemStack template) {
        int total=player.getOffhandItem().getCount();
        for(ItemStack s:player.getInventory().items) if(ItemStack.isSameItemSameComponents(s,template)) total+=s.getCount();
        return total;
    }
    private static void consume(Player player,ItemStack belts,int count) {
        ItemStack template=belts.copyWithCount(1);
        int n=Math.min(count,belts.getCount());belts.shrink(n);count-=n;
        for(ItemStack s:player.getInventory().items) if(count>0 && ItemStack.isSameItemSameComponents(s,template)) {
            n=Math.min(count,s.getCount());s.shrink(n);count-=n;
        }
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player=event.getEntity();
        if(player.level().isClientSide) return;
        var stacks=new ArrayList<ItemStack>(player.getInventory().items);stacks.add(player.getOffhandItem());
        for(ItemStack stack:stacks) {
            if (!(stack.getItem() instanceof net.crystalnexus.item.CrystalWrenchItem)) continue;
            CompoundTag data=preview(stack);
            if(data.isEmpty()) continue;
            if(stack!=player.getMainHandItem() || !eligible(player.getOffhandItem())
                || !data.getString("dimension").equals(player.level().dimension().location().toString())
                || !data.getString("item").equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString())) clear(stack);
        }
    }
    private static void message(Player p,String text) {
        if(p instanceof net.minecraft.server.level.ServerPlayer server && server.connection==null) return;
        p.displayClientMessage(Component.literal(text),true);
    }
    private static void write(ItemStack wrench,CompoundTag data) {CustomData.update(DataComponents.CUSTOM_DATA,wrench,t -> t.put(KEY,data.copy()));}
    public static final class Context extends BlockPlaceContext {
        private final ConveyorGeometry.Segment segment;
        public Context(Level level,Player player,ItemStack stack,ConveyorGeometry.Segment segment) {
            super(level,player,InteractionHand.OFF_HAND,stack,new BlockHitResult(Vec3.atCenterOf(segment.pos()),Direction.UP,segment.pos(),false));
            this.segment=segment;
        }
        @Override public BlockPos getClickedPos() {return segment==null?super.getClickedPos():segment.pos();}
        @Override public Direction getHorizontalDirection() {return segment.facing();}
        public BlockState state(BlockState base) {return segment.state(base);}
    }
}
