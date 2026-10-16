package net.crystalnexus.gametest;

import net.crystalnexus.assembly.AssemblyGraph;
import net.crystalnexus.drone.*;
import net.crystalnexus.entity.LUNADroneEntity;
import net.crystalnexus.init.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("crystalnexus_drones")
@PrefixGameTestTemplate(false)
public final class DroneGameTests {
    private static final String EMPTY="empty";
    private static LUNADroneEntity drone(GameTestHelper h){
        var d=CrystalnexusModEntities.UNIVERSAL_DRONE.get().create(h.getLevel());d.setPos(h.absoluteVec(new Vec3(3,4,3)));d.owner=UUID.randomUUID();d.setHome();return d;
    }
    private static void waitProgram(LUNADroneEntity d){
        var g=d.graph();g.nodes.add(DroneNode.WAIT.create(1,160,24));g.node(1).parameters.putInt("ticks",500);g.edges.add(new AssemblyGraph.Edge(0,0,1,0));d.program.start(g);d.program.node=1;d.program.elapsed=37;d.program.memory.putInt("checkpoint",123);
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void miningIsInstantAndUsesASufficientCargoPickaxe(GameTestHelper h){
        var d=drone(h);BlockPos pos=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());
        d.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND_AXE));d.inventory.setStackInSlot(1,new ItemStack(Items.WOODEN_PICKAXE));d.inventory.setStackInSlot(2,new ItemStack(Items.DIAMOND_PICKAXE));
        var mine=DroneNode.MINE_BLOCK.create(1,0,0);mine.parameters.putIntArray("pos",new int[]{pos.getX(),pos.getY(),pos.getZ()});
        h.assertTrue(d.runtime.actions.step(DroneNode.MINE_BLOCK,mine)==0&&h.getLevel().getBlockState(pos).isAir(),"One mining call breaks obsidian instantly");
        h.assertTrue(d.inventory.getStackInSlot(2).getDamageValue()==1&&d.inventory.getStackInSlot(1).getDamageValue()==0&&d.inventory.getStackInSlot(0).getDamageValue()==0,"Suitable pickaxe is selected after weaker tools and receives normal durability cost");
        int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.OBSIDIAN)).mapToInt(e->e.getItem().getCount()).sum();
        for(int i=0;i<d.inventory.getSlots();i++)if(d.inventory.getStackInSlot(i).is(Items.OBSIDIAN))drops+=d.inventory.getStackInSlot(i).getCount();
        h.assertTrue(drops==1,"Harvest rules and loot remain intact during instant mining");
        h.getLevel().setBlockAndUpdate(pos,Blocks.DIAMOND_ORE.defaultBlockState());d.inventory.setStackInSlot(2,new ItemStack(Items.STONE_PICKAXE));int energy=d.battery.getEnergyStored();
        h.assertTrue(d.runtime.actions.step(DroneNode.MINE_BLOCK,mine)==2&&h.getLevel().getBlockState(pos).is(Blocks.DIAMOND_ORE)&&d.battery.getEnergyStored()==energy,"Insufficient harvest level leaves the block and energy intact");
        h.getLevel().setBlockAndUpdate(pos,Blocks.BEDROCK.defaultBlockState());d.inventory.setStackInSlot(2,new ItemStack(Items.NETHERITE_PICKAXE));
        h.assertTrue(d.runtime.actions.step(DroneNode.MINE_BLOCK,mine)==2&&h.getLevel().getBlockState(pos).is(Blocks.BEDROCK),"Instant mining never breaks bedrock");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void parallelWaitsAdvanceInTheSameServerTicks(GameTestHelper h){
        var d=drone(h);var g=d.graph();for(int id:new int[]{1,2}){var wait=DroneNode.WAIT.create(id,0,0);wait.parameters.putInt("ticks",20);g.nodes.add(wait);g.edges.add(new AssemblyGraph.Edge(0,0,id,0));}
        d.program.start(g);d.runtime.tick();for(int i=0;i<5;i++)d.runtime.tick();
        var tasks=d.program.save().getList("tasks",10);h.assertTrue(tasks.size()==2&&tasks.getCompound(0).getInt("elapsed")==5&&tasks.getCompound(1).getInt("elapsed")==5,"Both waits advance every tick with independent timers");
        for(int i=0;i<15;i++)d.runtime.tick();h.assertTrue(!d.program.running,"Two twenty-tick waits complete together after twenty ticks, rather than forty");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void persistentFollowAndPickupLoopRunSimultaneously(GameTestHelper h){
        var d=drone(h);var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"parallel-owner"));d.setOwner(player);player.setPos(d.position().add(6,0,0));var online=onlinePlayers(h);online.put(player.getUUID(),player);
        try{
            var g=d.graph();var follow=DroneNodeSettings.create(DroneNode.FOLLOW_PLAYER,1,0,0);g.nodes.add(follow);g.nodes.add(DroneNodeSettings.create(DroneNode.COLLECT_DROPPED_ITEMS,2,0,0));var repeat=DroneNode.REPEAT.create(3,0,0);repeat.parameters.putInt("node",2);g.nodes.add(repeat);
            g.edges.add(new AssemblyGraph.Edge(0,0,1,0));g.edges.add(new AssemblyGraph.Edge(0,0,2,0));g.edges.add(new AssemblyGraph.Edge(2,0,3,0));d.program.start(g);
            for(int i=0;i<8;i++)d.runtime.tick();h.assertTrue(d.program.running&&java.util.Arrays.stream(d.program.activeNodes()).anyMatch(id->id==1)&&!d.runtime.destination().equals("None"),"Follow stays active while the pickup loop advances");
            var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),d.getX(),d.getY(),d.getZ(),new ItemStack(Items.IRON_INGOT,3));item.setNoPickUpDelay();h.getLevel().addFreshEntity(item);
            for(int i=0;i<8;i++)d.runtime.tick();h.assertTrue(!item.isAlive()&&d.inventory.getStackInSlot(0).getCount()==3&&d.program.running&&d.program.activeNodes().length==2&&!d.runtime.destination().equals("None"),"Pickup collects later drops without waiting for Follow to finish or resetting its route");
            g.node(2).block=DroneNode.GO_TO_ENTITY.id();for(int i=0;i<3;i++)d.runtime.tick();
            h.assertTrue(!d.program.running&&d.runtime.destination().equals("None")&&d.getNavigation().isDone(),"Unhandled failure in another branch stops the entire program and active flight");h.succeed();
        }finally{online.remove(player.getUUID());}
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void conflictingMovementWaitsForTheActiveRoute(GameTestHelper h){
        var d=drone(h);var first=DroneNode.GO_TO_POSITION.create(1,0,0);var second=DroneNode.GO_TO_POSITION.create(2,0,0);
        BlockPos a=h.absolutePos(new BlockPos(10,6,3)),b=h.absolutePos(new BlockPos(3,6,10));first.parameters.putIntArray("pos",new int[]{a.getX(),a.getY(),a.getZ()});second.parameters.putIntArray("pos",new int[]{b.getX(),b.getY(),b.getZ()});
        d.program.node=1;h.assertTrue(d.runtime.actions.step(DroneNode.GO_TO_POSITION,first)==-1,"First route starts");String destination=d.runtime.destination();
        d.program.node=2;h.assertTrue(d.runtime.actions.step(DroneNode.GO_TO_POSITION,second)==-1&&d.runtime.destination().equals(destination),"Second route does not overwrite the active path");d.runtime.resetNavigation();h.assertTrue(d.runtime.destination().equals(destination),"Unrelated task completion cannot cancel the active path");
        d.setPos(Vec3.atCenterOf(a));d.program.node=1;h.assertTrue(d.runtime.actions.step(DroneNode.GO_TO_POSITION,first)==0,"First route releases movement on arrival");
        d.program.node=2;h.assertTrue(d.runtime.actions.step(DroneNode.GO_TO_POSITION,second)==-1&&d.runtime.destination().equals(b.toShortString()),"Waiting route can start after arrival");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void collectRepeatSurvivesEmptyScansPickupDelayAndFullCargo(GameTestHelper h){
        for(var kind:new DroneNode[]{DroneNode.COLLECT_DROPPED_ITEMS,DroneNode.COLLECT_DROPS}){
            var d=drone(h);var g=d.graph();var collect=DroneNodeSettings.create(kind,1,0,0);collect.parameters.putString("filter","minecraft:iron_ingot");
            var wait=DroneNode.WAIT.create(2,0,0);wait.parameters.putInt("ticks",1);var repeat=DroneNode.REPEAT.create(3,0,0);repeat.parameters.putInt("node",1);
            g.nodes.add(collect);g.nodes.add(wait);g.nodes.add(repeat);g.edges.add(new AssemblyGraph.Edge(0,0,1,0));g.edges.add(new AssemblyGraph.Edge(1,0,2,0));g.edges.add(new AssemblyGraph.Edge(2,0,3,0));
            h.assertTrue(DroneProgram.validate(g,d.mode).isEmpty(),"Collection loop validates");d.program.start(g);
            var first=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),d.getX(),d.getY(),d.getZ(),new ItemStack(Items.IRON_INGOT,2));first.setNoPickUpDelay();h.getLevel().addFreshEntity(first);
            for(int i=0;i<16;i++)d.runtime.tick();
            h.assertTrue(!first.isAlive()&&d.inventory.getStackInSlot(0).getCount()==2&&d.program.running,"Loop collects initially and continues across empty scans");
            var later=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),d.getX(),d.getY(),d.getZ(),new ItemStack(Items.IRON_INGOT,3));later.setPickUpDelay(10);h.getLevel().addFreshEntity(later);
            for(int i=0;i<12;i++)d.runtime.tick();h.assertTrue(later.isAlive()&&d.program.running,"Pickup delay does not terminate the loop");
            later.setNoPickUpDelay();for(int i=0;i<12;i++)d.runtime.tick();h.assertTrue(!later.isAlive()&&d.inventory.getStackInSlot(0).getCount()==5,"Later drops are picked up without restarting");
            for(int i=0;i<9;i++)d.inventory.setStackInSlot(i,new ItemStack(Items.IRON_INGOT,64));
            var overflow=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),d.getX(),d.getY(),d.getZ(),new ItemStack(Items.IRON_INGOT,4));overflow.setNoPickUpDelay();h.getLevel().addFreshEntity(overflow);
            for(int i=0;i<12;i++)d.runtime.tick();h.assertTrue(overflow.isAlive()&&overflow.getItem().getCount()==4&&d.program.running,"Full cargo preserves drops and keeps looping");
            d.inventory.setStackInSlot(0,ItemStack.EMPTY);for(int i=0;i<12;i++)d.runtime.tick();h.assertTrue(!overflow.isAlive()&&d.inventory.getStackInSlot(0).getCount()==4,"Collection resumes when cargo space is available");
        }
        h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void modesProgramsAndContentsPersist(GameTestHelper h){
        var d=drone(h);UUID owner=d.owner;d.authorized.add(UUID.randomUUID());waitProgram(d);
        d.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,17));d.tank.fill(new FluidStack(Fluids.WATER,4000),IFluidHandler.FluidAction.EXECUTE);d.battery.extractEnergy(12345,false);
        d.runtime.recall(d.owner,true,false);CompoundTag saved=d.saveDrone();var copy=drone(h);copy.loadDrone(saved);
        h.assertTrue(owner.equals(copy.owner)&&copy.inventory.getStackInSlot(0).getCount()==17&&copy.tank.getFluidAmount()==4000&&copy.battery.getEnergyStored()==187655,"Owner, cargo, fluid and FE survive reload");
        h.assertTrue(copy.runtime.isRecalling()&&!copy.program.suspended.isEmpty(),"Recall and previous execution survive reload");
        h.assertTrue(copy.program.suspended.getInt("elapsed")==37&&copy.program.suspended.getCompound("memory").getInt("checkpoint")==123,"Execution checkpoint is preserved");
        copy.runtime.cancel();h.assertTrue(copy.runtime.resume()&&copy.program.node==1&&copy.program.elapsed==37,"Previous task resumes at its exact checkpoint");
        for(int i=0;i<6;i++)copy.cycleMode();h.assertTrue(copy.mode==DroneMode.TRANSPORT&&copy.graph().nodes.size()==2,"Six modes cycle and retain their graphs");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void recallFailurePermissionsAndInvalidResume(GameTestHelper h){
        var d=drone(h);waitProgram(d);String error=d.runtime.recall(UUID.randomUUID(),false,false);
        h.assertTrue(error.contains("authorized")&&d.program.running&&d.program.elapsed==37,"Unauthorized recall does not interrupt the task");
        d.homeDimension="minecraft:the_nether";error=d.runtime.recall(d.owner,true,false);h.assertTrue(error.contains("dimension")&&d.program.running,"Cross-dimension recall preserves the running program");
        d.homeDimension=d.level().dimension().location().toString();d.home=new BlockPos(13000000,80,13000000);int chunks=h.getLevel().getChunkSource().getLoadedChunksCount();error=d.runtime.recall(d.owner,true,false);
        h.assertTrue(error.contains("unloaded")&&chunks==h.getLevel().getChunkSource().getLoadedChunksCount(),"Unloaded recall does not load chunks");
        d.setHome();d.battery.extractEnergy(200000,false);error=d.runtime.recall(d.owner,true,false);h.assertTrue(error.contains("battery")&&d.program.running,"Insufficient energy is reported without losing state");
        d.battery.receiveEnergy(1000,false);h.assertTrue(d.runtime.recall(d.owner,true,false).isEmpty(),"Low battery recall overrides ordinary work threshold");
        d.runtime.tick();h.assertTrue(!d.runtime.isRecalling(),"Recall completes on arrival");d.graph().revision++;
        h.assertTrue(!d.runtime.resume()&&!d.program.suspended.isEmpty(),"Edited program cannot resume stale execution; checkpoint retained");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=400)
    public static void recallTracksMovingOwnerAndHovers(GameTestHelper h){
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"drone-test"));
        var online=onlinePlayers(h);online.put(player.getUUID(),player);player.setPos(h.absoluteVec(new Vec3(11,4,3)));
        var d=drone(h);d.owner=player.getUUID();waitProgram(d);d.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,32));h.getLevel().addFreshEntity(d);
        h.assertTrue(d.runtime.recall(player.getUUID(),false,false).isEmpty(),"Remote recall starts");
        h.assertTrue(!d.program.running&&!d.program.suspended.isEmpty(),"Active task is interrupted and preserved");
        h.runAfterDelay(15,()->player.setPos(h.absoluteVec(new Vec3(12,4,8))));
        h.runAfterDelay(160,()->{
            h.assertTrue(!d.runtime.isRecalling()&&d.position().distanceTo(player.position().add(0,2,0))<4,"Drone physically reaches the moving owner: "+d.status+" at "+d.position());
            h.assertTrue(d.inventory.getStackInSlot(0).getCount()==32&&!d.program.running,"Recall holds cargo and hovers without running the program");
            player.setPos(h.absoluteVec(new Vec3(8,4,8)));
        });
        h.runAfterDelay(260,()->{h.assertTrue(d.position().distanceTo(player.position().add(0,2,0))<4,"Hover follows owner after completion");h.assertTrue(d.runtime.resume()&&d.program.elapsed==37,"Explicit resume restores interrupted instruction");online.remove(player.getUUID());d.discard();h.succeed();});
    }
    @SuppressWarnings("unchecked")
    private static java.util.Map<UUID,ServerPlayer> onlinePlayers(GameTestHelper h){
        // Register a fake online recipient without invoking unrelated login-payload negotiation.
        try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);return (java.util.Map<UUID,ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void recallEventAndAutomaticResume(GameTestHelper h){
        var d=drone(h);waitProgram(d);var event=DroneNode.ON_RECALL_COMPLETE.create(2,0,0);var resume=DroneNode.RESUME_PREVIOUS_TASK.create(3,0,0);d.graph().nodes.add(event);d.graph().nodes.add(resume);d.graph().edges.add(new AssemblyGraph.Edge(2,0,3,0));
        h.assertTrue(d.runtime.recall(d.owner,true,false).isEmpty(),"Recall starts");d.runtime.tick();h.assertTrue(d.program.node==2,"On Recall Complete selects event entry");d.runtime.tick();d.runtime.tick();
        h.assertTrue(d.program.running&&d.program.node==1&&d.program.elapsed==37,"Resume Previous Task restores the interrupted task from event branch");
        d.graph().nodes.remove(event);d.graph().nodes.remove(resume);d.graph().edges.removeIf(e->e.from()==2);h.assertTrue(d.runtime.recall(d.owner,true,true).isEmpty(),"Second recall starts");d.runtime.tick();h.assertTrue(d.program.running&&d.program.suspended.isEmpty(),"Automatic resume is optional and works");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void transferAndFluidTransactionsPreserveContents(GameTestHelper h){
        var d=drone(h);BlockPos chest=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());var container=(net.minecraft.world.Container)h.getLevel().getBlockEntity(chest);container.setItem(0,new ItemStack(Items.IRON_INGOT,64));
        var extract=DroneNode.EXTRACT_ITEMS.create(1,0,0);extract.parameters.putIntArray("pos",new int[]{chest.getX(),chest.getY(),chest.getZ()});extract.parameters.putInt("amount",32);
        h.assertTrue(d.runtime.actions.step(DroneNode.EXTRACT_ITEMS,extract)==0&&container.getItem(0).getCount()==32&&d.inventory.getStackInSlot(0).getCount()==32,"Extract commits exactly one transfer");
        waitProgram(d);d.runtime.recall(d.owner,true,false);h.assertTrue(container.getItem(0).getCount()==32&&d.inventory.getStackInSlot(0).getCount()==32,"Recall cannot duplicate or replay the inventory transaction");
        var tankPos=h.absolutePos(new BlockPos(4,4,4));h.getLevel().setBlockAndUpdate(tankPos,CrystalnexusModBlocks.TANK.get().defaultBlockState());
        var external=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,tankPos,net.minecraft.core.Direction.UP);
        h.assertTrue(external!=null,"Existing tank exposes fluid capability");external.fill(new FluidStack(Fluids.WATER,6000),IFluidHandler.FluidAction.EXECUTE);
        var fluid=DroneNode.EXTRACT_FLUID.create(2,0,0);fluid.parameters.putIntArray("pos",new int[]{tankPos.getX(),tankPos.getY(),tankPos.getZ()});fluid.parameters.putInt("amount",4000);
        h.assertTrue(d.runtime.actions.step(DroneNode.EXTRACT_FLUID,fluid)==0&&d.tank.getFluidAmount()==4000&&external.getFluidInTank(0).getAmount()==2000,"Fluid extraction conserves amount");
        h.assertTrue(d.runtime.actions.step(DroneNode.INSERT_FLUID,fluid)==0&&d.tank.isEmpty()&&external.getFluidInTank(0).getAmount()==6000,"Fluid insertion conserves amount");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void miningBuilderAndCombatUseRealResources(GameTestHelper h){
        var d=drone(h);BlockPos pos=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());d.inventory.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,4));
        var place=DroneNode.PLACE_BLOCK.create(1,0,0);place.parameters.putIntArray("pos",new int[]{pos.getX(),pos.getY(),pos.getZ()});place.parameters.putString("filter","minecraft:cobblestone");
        h.assertTrue(d.runtime.actions.step(DroneNode.PLACE_BLOCK,place)==0&&d.inventory.getStackInSlot(0).getCount()==3&&h.getLevel().getBlockState(pos).is(Blocks.COBBLESTONE),"Builder consumes a real block");
        d.inventory.setStackInSlot(1,new ItemStack(Items.DIAMOND_PICKAXE));var mine=DroneNode.MINE_BLOCK.create(2,0,0);mine.parameters.putIntArray("pos",new int[]{pos.getX(),pos.getY(),pos.getZ()});int result=-1;
        for(int i=0;i<400&&result==-1;i++)result=d.runtime.actions.step(DroneNode.MINE_BLOCK,mine);
        h.assertTrue(result==0&&h.getLevel().getBlockState(pos).isAir(),"Mining respects progress and removes the block");
        var pig=h.spawn(EntityType.PIG,new BlockPos(4,4,4));var hostile=DroneNode.TARGET_HOSTILE_MOBS.create(3,0,0);d.runtime.actions.step(DroneNode.TARGET_HOSTILE_MOBS,hostile);var detect=DroneNode.DETECT_ENTITY.create(4,0,0);detect.parameters.putDouble("range",8);
        h.assertTrue(d.runtime.actions.step(DroneNode.DETECT_ENTITY,detect)==1,"Hostile filter excludes passive animals");
        d.runtime.actions.step(DroneNode.STOP_ATTACKING,DroneNode.STOP_ATTACKING.create(5,0,0));d.runtime.actions.step(DroneNode.TARGET_PASSIVE_MOBS,DroneNode.TARGET_PASSIVE_MOBS.create(6,0,0));
        h.assertTrue(d.runtime.actions.step(DroneNode.DETECT_ENTITY,detect)==0,"Passive filter selects animals");float health=pig.getHealth();
        h.assertTrue(d.runtime.actions.step(DroneNode.ATTACK_TARGET,DroneNode.ATTACK_TARGET.create(7,0,0))==0&&pig.getHealth()<health,"Basic melee damages permitted target");pig.discard();h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void controllerRecallAllIsOwnedAndModeIndependent(GameTestHelper h){
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"drone-controller"));
        var online=onlinePlayers(h);online.put(player.getUUID(),player);player.setPos(h.absoluteVec(new Vec3(3,4,3)));
        var owned=new java.util.ArrayList<LUNADroneEntity>();
        try{
            for(var mode:DroneMode.values()){var d=drone(h);d.setOwner(player);d.mode=mode;waitProgram(d);d.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,2));h.getLevel().addFreshEntity(d);owned.add(d);}
            var foreign=drone(h);waitProgram(foreign);h.getLevel().addFreshEntity(foreign);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CrystalnexusModItems.DRONE_CONTROLLER.get()));
            ((net.crystalnexus.item.DroneControllerItem)player.getMainHandItem().getItem()).use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            h.assertTrue(owned.stream().allMatch(d->d.runtime.isRecalling()&&!d.program.running&&d.inventory.getStackInSlot(0).getCount()==2),"Unbound controller recalls owned drones in all six modes without touching cargo");
            h.assertTrue(!foreign.runtime.isRecalling()&&foreign.program.running,"Recall all never interrupts another owner's drone");
            var menu=new net.crystalnexus.world.inventory.DroneMenu(1,player.getInventory(),foreign);h.assertTrue(!menu.stillValid(player),"Foreign drone inventory rejects owner mismatch");foreign.discard();h.succeed();
        }finally{online.remove(player.getUUID());for(var d:owned)d.discard();}
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void failedActiveRecallPreservesTaskAndCargo(GameTestHelper h){
        var d=drone(h);waitProgram(d);d.inventory.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,11));
        d.home=d.home.offset(6,0,0);h.assertTrue(d.runtime.recall(d.owner,true,true).isEmpty(),"Recall starts toward a loaded destination");
        d.battery.extractEnergy(200000,false);d.runtime.tick();
        h.assertTrue(!d.runtime.isRecalling()&&d.status.contains("Recall failed")&&d.status.contains("battery"),"Mid-route energy failure is reported");
        h.assertTrue(!d.program.running&&!d.program.suspended.isEmpty()&&d.inventory.getStackInSlot(0).getCount()==11,"Failed recall preserves exact interrupted task and inventory");
        h.assertTrue(d.runtime.resume()&&d.program.node==1&&d.program.elapsed==37,"Failure does not invalidate the old checkpoint");h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void blockProtectionDeniesMiningAndPlacement(GameTestHelper h){
        var d=drone(h);BlockPos pos=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());d.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND_PICKAXE));
        var node=DroneNode.MINE_BLOCK.create(1,0,0);node.parameters.putIntArray("pos",new int[]{pos.getX(),pos.getY(),pos.getZ()});
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> denyBreak=e->{if(e.getPos().equals(pos))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(denyBreak);
        try{int result=-1;for(int i=0;i<400&&result==-1;i++)result=d.runtime.actions.step(DroneNode.MINE_BLOCK,node);h.assertTrue(result==2&&h.getLevel().getBlockState(pos).is(Blocks.STONE),"Drone cannot bypass canceled break events");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(denyBreak);}
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());d.inventory.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,3));node.parameters.putString("filter","minecraft:cobblestone");
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> denyPlace=e->{if(e.getPos().equals(pos))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(denyPlace);
        try{h.assertTrue(d.runtime.actions.step(DroneNode.PLACE_BLOCK,node)==2&&h.getLevel().getBlockState(pos).isAir()&&d.inventory.getStackInSlot(0).getCount()==3,"Canceled placement preserves world and material");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(denyPlace);}h.succeed();
    }
    @GameTest(template=EMPTY,timeoutTicks=40)
    public static void storageAndCombatProtectionKeepResourcesIntact(GameTestHelper h){
        var d=drone(h);BlockPos pos=h.absolutePos(new BlockPos(4,4,3));h.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());var storage=(net.minecraft.world.Container)h.getLevel().getBlockEntity(pos);storage.setItem(0,new ItemStack(Items.IRON_INGOT,32));
        var node=DroneNode.EXTRACT_ITEMS.create(1,0,0);node.parameters.putIntArray("pos",new int[]{pos.getX(),pos.getY(),pos.getZ()});
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> deny=e->{if(e.getPos().equals(pos))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try{h.assertTrue(d.runtime.actions.step(DroneNode.EXTRACT_ITEMS,node)==2&&storage.getItem(0).getCount()==32&&d.inventory.getStackInSlot(0).isEmpty(),"Protected inventory cannot be extracted through capabilities");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);}
        var pig=h.spawn(EntityType.PIG,new BlockPos(3,4,4));d.program.memory.putUUID("target",pig.getUUID());float health=pig.getHealth();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> denyAttack=e->{if(e.getTarget()==pig)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(denyAttack);
        try{h.assertTrue(d.runtime.actions.step(DroneNode.ATTACK_TARGET,DroneNode.ATTACK_TARGET.create(2,0,0))==2&&pig.getHealth()==health,"Canceled attacks cannot damage protected entities");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(denyAttack);pig.discard();}h.succeed();
    }
}
