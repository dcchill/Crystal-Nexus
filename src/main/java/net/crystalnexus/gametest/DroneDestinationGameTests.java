package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.drone.*;
import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.init.*;
import net.crystalnexus.item.*;
import net.crystalnexus.world.inventory.DroneMenu;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("crystalnexus_drones")
@PrefixGameTestTemplate(false)
public final class DroneDestinationGameTests {
    private static FakePlayer player(GameTestHelper h){return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"card-test"));}
    private static LUNADroneEntity drone(GameTestHelper h){var d=CrystalnexusModEntities.UNIVERSAL_DRONE.get().create(h.getLevel());d.setPos(h.absoluteVec(new Vec3(3.5,4.5,3.5)));d.owner=UUID.randomUUID();d.setHome();return d;}
    private static ItemStack card(GameTestHelper h,BlockPos pos,Direction face){
        ItemStack stack=new ItemStack(CrystalnexusModItems.LINK_CARD.get());CustomData.update(DataComponents.CUSTOM_DATA,stack,p->{p.putDouble("linkX",pos.getX());p.putDouble("linkY",pos.getY());p.putDouble("linkZ",pos.getZ());p.putString("linkDimension",h.getLevel().dimension().location().toString());p.putString("linkFace",face.getName());p.putString("linkBlock","minecraft:chest");});return stack;
    }
    private static int[] xyz(BlockPos p){return new int[]{p.getX(),p.getY(),p.getZ()};}
    @GameTest(template="empty",timeoutTicks=40)
    public static void simultaneousStorageTransfersConserveItems(GameTestHelper h){
        var d=drone(h);BlockPos chest=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        var storage=(net.minecraft.world.Container)h.getLevel().getBlockEntity(chest);storage.setItem(0,new ItemStack(Items.IRON_INGOT,6));d.destinationCards.setStackInSlot(0,card(h,chest,Direction.WEST));
        var g=d.graph();for(int id:new int[]{1,2}){var pickup=DroneNodeSettings.create(DroneNode.EXTRACT_ITEMS,id,0,0);pickup.parameters.putInt("destinationSlot",0);pickup.parameters.putInt("amount",4);g.nodes.add(pickup);g.edges.add(new AssemblyGraph.Edge(0,0,id,0));}
        int energy=d.battery.getEnergyStored();d.program.start(g);d.runtime.tick();d.runtime.tick();
        h.assertTrue(!d.program.running&&storage.isEmpty()&&d.inventory.getStackInSlot(0).getCount()==6&&d.battery.getEnergyStored()==energy-40,"Both transfers execute in the same tick, see actual remaining stock, and conserve items and energy");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void parallelTransfersSurviveRecallEventAndReloadWithoutReplay(GameTestHelper h){
        var d=drone(h);BlockPos chest=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        var storage=(net.minecraft.world.Container)h.getLevel().getBlockEntity(chest);storage.setItem(0,new ItemStack(Items.IRON_INGOT,16));d.destinationCards.setStackInSlot(0,card(h,chest,Direction.WEST));
        var g=d.graph();for(int id:new int[]{1,3}){var pickup=DroneNodeSettings.create(DroneNode.EXTRACT_ITEMS,id,0,0);pickup.parameters.putInt("destinationSlot",0);pickup.parameters.putInt("amount",4);g.nodes.add(pickup);}
        var wait=DroneNode.WAIT.create(2,0,0);wait.parameters.putInt("ticks",3);g.nodes.add(wait);
        var event=DroneNode.ON_RECALL_COMPLETE.create(4,0,0);var eventWait=DroneNode.WAIT.create(5,0,0);eventWait.parameters.putInt("ticks",2);g.nodes.add(event);g.nodes.add(eventWait);
        var longWait=DroneNode.WAIT.create(6,0,0);longWait.parameters.putInt("ticks",10);g.nodes.add(longWait);
        for(int id:new int[]{1,2})g.edges.add(new AssemblyGraph.Edge(0,0,id,0));g.edges.add(new AssemblyGraph.Edge(1,0,6,0));g.edges.add(new AssemblyGraph.Edge(2,0,3,0));g.edges.add(new AssemblyGraph.Edge(4,0,5,0));
        h.assertTrue(DroneProgram.validate(g,d.mode).isEmpty(),"Multiple Next destinations validate");d.program.start(g);d.runtime.tick();d.runtime.tick();
        h.assertTrue(d.program.node==2&&d.inventory.getStackInSlot(0).getCount()==4&&java.util.Arrays.equals(d.program.activeNodes(),new int[]{2,6}),"One transfer completes while both independent waits remain active");
        h.assertTrue(d.runtime.recall(d.owner,true,false).isEmpty(),"Recall interrupts the forked program");d.runtime.tick();
        h.assertTrue(d.program.node==4&&java.util.Arrays.equals(d.program.activeNodes(),new int[]{4}),"Recall event has an isolated queue");
        for(int i=0;i<4;i++)d.runtime.tick();h.assertTrue(storage.getItem(0).getCount()==12,"Recall event does not execute interrupted queued transfers");
        var copy=drone(h);copy.loadDrone(d.saveDrone());h.assertTrue(copy.runtime.resume()&&copy.program.node==2,"Reloaded program resumes the interrupted branch");
        for(int i=0;i<16;i++)copy.runtime.tick();
        h.assertTrue(!copy.program.running&&storage.getItem(0).getCount()==8&&copy.inventory.getStackInSlot(0).getCount()==8,"Concurrent task state resumes and each transfer executes exactly once after recall and reload");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void builderRetrievesLinkedStorageMaterialsThenBuilds(GameTestHelper h){
        var d=drone(h);d.mode=DroneMode.BUILDER;
        BlockPos chest=h.absolutePos(new BlockPos(4,4,3)),site=h.absolutePos(new BlockPos(4,4,4));
        h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());var storage=(net.minecraft.world.Container)h.getLevel().getBlockEntity(chest);
        storage.setItem(0,new ItemStack(Items.DIAMOND,7));storage.setItem(1,new ItemStack(Items.COBBLESTONE,16));
        h.getLevel().setBlockAndUpdate(site,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(site.below(),Blocks.STONE.defaultBlockState());
        d.destinationCards.setStackInSlot(0,card(h,chest,Direction.WEST));d.destinationCards.setStackInSlot(1,card(h,site,Direction.UP));
        var pickup=DroneNodeSettings.create(DroneNode.EXTRACT_ITEMS,1,0,0);pickup.parameters.putInt("destinationSlot",0);pickup.parameters.putInt("amount",4);pickup.parameters.putString("filter","minecraft:cobblestone");
        var place=DroneNodeSettings.create(DroneNode.PLACE_BLOCK,2,0,0);place.parameters.putInt("destinationSlot",1);place.parameters.putString("filter","minecraft:cobblestone");
        var g=d.graph();g.nodes.add(pickup);g.nodes.add(place);g.edges.add(new AssemblyGraph.Edge(0,0,1,0));g.edges.add(new AssemblyGraph.Edge(1,0,2,0));
        h.assertTrue(DroneProgram.validate(g,DroneMode.BUILDER).isEmpty(),"Builder graph accepts existing storage pickup node");
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> deny=e->{if(e.getPos().equals(chest))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try{d.program.start(g);d.runtime.tick();d.runtime.tick();h.assertTrue(!d.program.running&&d.status.contains("protection")&&storage.getItem(1).getCount()==16&&d.inventory.getStackInSlot(0).isEmpty(),"Builder storage pickup respects protection without moving items");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
        int energy=d.battery.getEnergyStored();d.program.start(g);for(int i=0;i<3;i++)d.runtime.tick();
        h.assertTrue(!d.program.running&&h.getLevel().getBlockState(site).is(Blocks.COBBLESTONE),"Builder runtime retrieves material and executes placement next");
        h.assertTrue(storage.getItem(0).getCount()==7&&storage.getItem(1).getCount()==12&&d.inventory.getStackInSlot(0).getCount()==3,"Filter and amount preserve unrelated items and consume exactly one building block");
        h.assertTrue(d.battery.getEnergyStored()==energy-120,"Pickup and placement consume normal energy");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void cardsLinkBothHandsAndRenderingDoesNotMutate(GameTestHelper h){
        var p=player(h);BlockPos pos=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        var item=(LinkCardItem)CrystalnexusModItems.LINK_CARD.get();var main=new ItemStack(CrystalnexusModItems.LINK_CARD.get());var off=new ItemStack(CrystalnexusModItems.LINK_CARD.get());p.setItemInHand(InteractionHand.MAIN_HAND,main);p.setItemInHand(InteractionHand.OFF_HAND,off);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.WEST,pos,false);item.onItemUseFirst(off,new UseOnContext(p,InteractionHand.OFF_HAND,hit));
        h.assertTrue(LinkCardData.read(main)==null&&LinkCardData.read(off).pos().equals(pos)&&LinkCardData.read(off).face()==Direction.WEST,"Offhand linking updates only the actual held card and records its face");
        item.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));var before=main.get(DataComponents.CUSTOM_DATA).copyTag();
        var tooltip=new ArrayList<net.minecraft.network.chat.Component>();for(int i=0;i<3;i++){h.assertTrue(item.isFoil(main),"Linked cards glint");item.appendHoverText(main,null,tooltip,TooltipFlag.NORMAL);}
        h.assertTrue(main.get(DataComponents.CUSTOM_DATA).copyTag().equals(before),"Glint and tooltip reads never relink cards");
        h.assertTrue(before.getDouble("linkX")==pos.getX()&&before.getDouble("linkY")==pos.getY()&&before.getDouble("linkZ")==pos.getZ(),"Old coordinate fields remain compatible with existing machines");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void hiddenSlotsAndOwnershipProtectDestinationCards(GameTestHelper h){
        var p=player(h);var d=drone(h);d.owner=p.getUUID();var m=new DroneMenu(1,p.getInventory(),d);p.getInventory().setItem(9,new ItemStack(CrystalnexusModItems.LINK_CARD.get(),3));
        m.clicked(18,0,ClickType.QUICK_MOVE,p);h.assertTrue(d.destinationCards.getStackInSlot(0).isEmpty()&&p.getInventory().getItem(9).getCount()==3,"Program tab ignores hidden-slot clicks and shift-click transfers");
        h.assertTrue(m.slots.stream().noneMatch(s->s.isActive()),"Every inventory slot is hidden on Program");m.cargoTab=true;m.quickMoveStack(p,18);
        h.assertTrue(d.destinationCards.getStackInSlot(0).getCount()==1&&p.getInventory().getItem(9).getCount()==2,"Cards shift-click into dedicated slots one per slot");
        p.getInventory().setItem(10,new ItemStack(Items.IRON_INGOT,5));m.quickMoveStack(p,19);h.assertTrue(d.inventory.getStackInSlot(0).getCount()==5&&!m.slots.get(9).mayPlace(new ItemStack(Items.IRON_INGOT)),"Normal cargo never enters destination slots");
        var stranger=player(h);h.assertTrue(m.quickMoveStack(stranger,9).isEmpty()&&d.destinationCards.getStackInSlot(0).getCount()==1,"Non-owner cannot take installed cards");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void activeDestinationSurvivesReplacementRecallAndReload(GameTestHelper h){
        var d=drone(h);BlockPos first=h.absolutePos(new BlockPos(9,4,3)),second=first.offset(0,0,3);d.destinationCards.setStackInSlot(0,card(h,first,Direction.NORTH));
        var node=DroneNodeSettings.create(DroneNode.GO_TO_BLOCK,1,0,0);node.parameters.putInt("destinationSlot",0);d.graph().nodes.add(node);d.graph().edges.add(new AssemblyGraph.Edge(0,0,1,0));d.program.start(d.graph());d.program.node=1;
        var resolved=DroneDestinations.resolve(d,node.parameters);h.assertTrue(Arrays.equals(resolved.getIntArray("pos"),xyz(first))&&resolved.getString("side").equals("north"),"Task resolves the card position and face");
        d.destinationCards.setStackInSlot(0,card(h,second,Direction.SOUTH));h.assertTrue(Arrays.equals(DroneDestinations.resolve(d,node.parameters).getIntArray("pos"),xyz(first)),"Replacing a card does not redirect current flight");
        h.assertTrue(d.runtime.recall(d.owner,true,false).isEmpty(),"Recall interrupts the task");d.runtime.tick();d.destinationCards.setStackInSlot(0,ItemStack.EMPTY);
        var copy=drone(h);copy.loadDrone(d.saveDrone());h.assertTrue(copy.runtime.resume(),"Interrupted task remains valid after card removal and reload");
        h.assertTrue(Arrays.equals(DroneDestinations.resolve(copy,copy.graph().node(1).parameters).getIntArray("pos"),xyz(first)),"Saved binding survives resumption without the physical card");
        copy.program.advance(copy.graph(),0);copy.destinationCards.setStackInSlot(0,card(h,second,Direction.SOUTH));
        h.assertTrue(Arrays.equals(DroneDestinations.resolve(copy,node.parameters).getIntArray("pos"),xyz(second)),"Next task uses the replacement card");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void cardReplacementAfterPickupChangesOnlyTheNextDelivery(GameTestHelper h){
        var d=drone(h);BlockPos source=h.absolutePos(new BlockPos(4,4,3)),oldTarget=h.absolutePos(new BlockPos(3,4,4)),nextTarget=h.absolutePos(new BlockPos(4,4,4));
        for(BlockPos pos:List.of(source,oldTarget,nextTarget))h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        var input=(net.minecraft.world.Container)h.getLevel().getBlockEntity(source);input.setItem(0,new ItemStack(Items.IRON_INGOT,32));d.destinationCards.setStackInSlot(0,card(h,source,Direction.UP));d.destinationCards.setStackInSlot(1,card(h,oldTarget,Direction.UP));
        var pickup=DroneNodeSettings.create(DroneNode.EXTRACT_ITEMS,1,0,0);pickup.parameters.putInt("destinationSlot",0);pickup.parameters.putInt("amount",32);var delivery=DroneNodeSettings.create(DroneNode.INSERT_ITEMS,2,0,0);delivery.parameters.putInt("destinationSlot",1);
        d.graph().nodes.add(pickup);d.graph().nodes.add(delivery);d.graph().edges.add(new AssemblyGraph.Edge(1,0,2,0));d.program.node=1;d.program.running=true;d.runtime.tick();
        h.assertTrue(input.getItem(0).isEmpty()&&d.inventory.getStackInSlot(0).getCount()==32,"Pickup commits before the next instruction");d.destinationCards.setStackInSlot(1,card(h,nextTarget,Direction.UP));d.runtime.tick();
        h.assertTrue(((net.minecraft.world.Container)h.getLevel().getBlockEntity(oldTarget)).isEmpty()&&((net.minecraft.world.Container)h.getLevel().getBlockEntity(nextTarget)).getItem(0).getCount()==32&&d.inventory.getStackInSlot(0).isEmpty(),"Next delivery uses the replacement without duplicating cargo");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void cornersOriginsAndPatrolLegsResolveCards(GameTestHelper h){
        var d=drone(h);BlockPos first=d.blockPosition(),second=first.offset(6,0,0),third=first.offset(6,0,3);
        d.destinationCards.setStackInSlot(0,card(h,first,Direction.UP));d.destinationCards.setStackInSlot(1,card(h,second,Direction.UP));
        var area=new CompoundTag();area.putInt("destinationSlot",0);area.putInt("endDestinationSlot",1);var result=DroneDestinations.resolve(d,area);
        h.assertTrue(Arrays.equals(result.getIntArray("pos"),xyz(first))&&Arrays.equals(result.getIntArray("end"),xyz(second)),"Area corners use two cards");DroneDestinations.clear(d.program.memory);
        var origin=new CompoundTag();origin.putInt("destinationSlot",1);h.assertTrue(Arrays.equals(DroneDestinations.resolve(d,origin).getIntArray("pos"),xyz(second)),"Blueprint origin uses a card");DroneDestinations.clear(d.program.memory);
        var patrol=DroneNodeSettings.create(DroneNode.PATROL,1,0,0);patrol.parameters.putIntArray("waypointSlots",new int[]{0,1});patrol.parameters.putDouble("range",1);d.runtime.actions.step(DroneNode.PATROL,patrol);d.runtime.actions.step(DroneNode.PATROL,patrol);
        d.destinationCards.setStackInSlot(1,card(h,third,Direction.UP));d.runtime.actions.step(DroneNode.PATROL,patrol);
        h.assertTrue(Arrays.equals(d.program.memory.getCompound("boundWaypoint").getIntArray("pos"),xyz(second)),"Active patrol leg keeps its original waypoint");
        d.setPos(Vec3.atCenterOf(second));d.runtime.actions.step(DroneNode.PATROL,patrol);d.destinationCards.setStackInSlot(0,card(h,third,Direction.UP));d.runtime.actions.step(DroneNode.PATROL,patrol);
        h.assertTrue(Arrays.equals(d.program.memory.getCompound("boundWaypoint").getIntArray("pos"),xyz(third)),"Next patrol leg reads the current installed card");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void invalidCardsAndUnloadedDestinationsFailWithoutLoading(GameTestHelper h){
        var d=drone(h);var parameters=new CompoundTag();parameters.putInt("destinationSlot",0);String error="";
        try{DroneDestinations.resolve(d,parameters);}catch(IllegalArgumentException e){error=e.getMessage();}h.assertTrue(error.contains("no card"),"Missing card reports a clear failure");
        var legacy=card(h,d.blockPosition(),Direction.UP);CustomData.update(DataComponents.CUSTOM_DATA,legacy,p->p.remove("linkDimension"));d.destinationCards.setStackInSlot(0,legacy);
        try{DroneDestinations.resolve(d,parameters);}catch(IllegalArgumentException e){error=e.getMessage();}h.assertTrue(error.contains("Relink")&&LinkCardData.read(legacy)!=null,"Legacy cards retain coordinates but require relinking for drones");
        var other=card(h,d.blockPosition(),Direction.UP);CustomData.update(DataComponents.CUSTOM_DATA,other,p->p.putString("linkDimension","minecraft:the_nether"));d.destinationCards.setStackInSlot(0,other);
        try{DroneDestinations.resolve(d,parameters);}catch(IllegalArgumentException e){error=e.getMessage();}h.assertTrue(error.contains("another dimension"),"Cross-dimension card is rejected");
        d.destinationCards.setStackInSlot(0,card(h,new BlockPos(13000000,80,13000000),Direction.UP));var node=DroneNodeSettings.create(DroneNode.GO_TO_BLOCK,1,0,0);node.parameters.putInt("destinationSlot",0);int chunks=h.getLevel().getChunkSource().getLoadedChunksCount();
        h.assertTrue(d.runtime.actions.step(DroneNode.GO_TO_BLOCK,node)==2&&chunks==h.getLevel().getChunkSource().getLoadedChunksCount(),"Unloaded card destination fails without force-loading chunks");
        var old=DroneNode.GO_TO_POSITION.create(2,0,0);h.assertTrue(DroneDestinations.resolve(d,old.parameters).equals(old.parameters),"Existing coordinate programs retain their behavior");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void destinationCardsPersistInPackedDroneDeployment(GameTestHelper h){
        var p=player(h);var d=drone(h);d.owner=p.getUUID();var linked=card(h,d.blockPosition(),Direction.WEST);d.destinationCards.setStackInSlot(3,linked);d.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,7));
        var copy=drone(h);copy.loadDrone(d.saveDrone());for(int i=0;i<6;i++)copy.cycleMode();h.assertTrue(copy.destinationCards.getStackInSlot(3).getCount()==1&&LinkCardData.read(copy.destinationCards.getStackInSlot(3)).face()==Direction.WEST,"Card bank survives reload and all six modes");
        var packed=new ItemStack(CrystalnexusModItems.UNIVERSAL_DRONE.get());CustomData.update(DataComponents.CUSTOM_DATA,packed,t->t.put("Drone",d.saveDrone()));p.setItemInHand(InteractionHand.MAIN_HAND,packed);p.getAbilities().instabuild=true;
        BlockPos base=h.absolutePos(new BlockPos(6,2,6));h.getLevel().setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());var hit=new BlockHitResult(Vec3.atCenterOf(base),Direction.UP,base,false);
        var outcome=((UniversalDroneItem)packed.getItem()).useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));
        var spawned=h.getLevel().getEntitiesOfClass(LUNADroneEntity.class,new AABB(base.above()).inflate(1));
        h.assertTrue(outcome.consumesAction()&&packed.isEmpty()&&spawned.size()==1&&spawned.getFirst().destinationCards.getStackInSlot(3).getCount()==1&&spawned.getFirst().inventory.getStackInSlot(0).getCount()==7,"Packed drone restores cards and cargo once, including creative deployment");for(var drone:spawned)drone.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void cardBuilderPlacesAtTheExactLinkedPosition(GameTestHelper h){
        var d=drone(h);BlockPos pos=h.absolutePos(new BlockPos(5,5,5));h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
        d.destinationCards.setStackInSlot(0,card(h,pos,Direction.UP));d.inventory.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,3));
        var place=DroneNodeSettings.create(DroneNode.PLACE_BLOCK,1,0,0);place.parameters.putInt("destinationSlot",0);place.parameters.putString("filter","minecraft:cobblestone");
        h.assertTrue(d.runtime.actions.step(DroneNode.PLACE_BLOCK,place)==0&&h.getLevel().getBlockState(pos).is(Blocks.COBBLESTONE)&&h.getLevel().getBlockState(pos.below()).isAir()&&d.inventory.getStackInSlot(0).getCount()==2,"Builder consumes one material at the linked position, including when the block below is air");h.succeed();
    }
}
