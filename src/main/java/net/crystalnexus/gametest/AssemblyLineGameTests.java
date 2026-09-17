package net.crystalnexus.gametest;

import net.crystalnexus.assembly.*;
import net.crystalnexus.block.entity.*;
import net.crystalnexus.init.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("crystalnexus_assembly")
@PrefixGameTestTemplate(false)
public final class AssemblyLineGameTests {
    private static AssemblyLineControllerBlockEntity build(GameTestHelper h, int x, int y, int z) {
        for (BlockPos p : BlockPos.betweenClosed(1, 1, 1, x, y, z)) {
            boolean shell = p.getX()==1 || p.getY()==1 || p.getZ()==1 || p.getX()==x || p.getY()==y || p.getZ()==z;
            h.setBlock(p, shell ? CrystalnexusModBlocks.ASSEMBLY_LINE_CASING.get() : Blocks.AIR);
        }
        BlockPos controller = new BlockPos(2, 2, 1); h.setBlock(controller, CrystalnexusModBlocks.ASSEMBLY_LINE_CONTROLLER.get());
        var be = (AssemblyLineControllerBlockEntity) h.getBlockEntity(controller); be.rescan(); return be;
    }
    private static int count(AssemblyLineControllerBlockEntity c, Item item, int start, int end) {
        int n=0; for (int i=start;i<end;i++) if (c.inventory.getStackInSlot(i).is(item)) n+=c.inventory.getStackInSlot(i).getCount(); return n;
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void structureAndPlannerValidation(GameTestHelper h) {
        var c=build(h, 7, 5, 9); h.assertTrue(c.formed(), "Variable rectangular enclosure must form");
        h.setBlock(new BlockPos(1,1,1), Blocks.STONE); c.rescan(); h.assertTrue(!c.formed(), "Ordinary shell corner must fail");
        h.setBlock(new BlockPos(1,1,1), CrystalnexusModBlocks.ASSEMBLY_LINE_CASING.get());
        h.setBlock(new BlockPos(4,2,4), Blocks.STONE); c.rescan(); h.assertTrue(!c.formed(), "Unsupported interior must fail");
        h.setBlock(new BlockPos(4,2,4), CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        h.setBlock(new BlockPos(3,2,1), CrystalnexusModBlocks.ASSEMBLY_LINE_CONTROLLER.get()); c.rescan(); h.assertTrue(!c.formed(), "Second controller must fail");
        h.setBlock(new BlockPos(3,2,1), CrystalnexusModBlocks.ASSEMBLY_LINE_CASING.get()); c.rescan();
        var workers=List.of(AssemblyLineMachine.at(h.getLevel(), h.absolutePos(new BlockPos(4,2,4))));
        var missing=ProductionPlan.build(h.getLevel(), new ItemStack(CrystalnexusModItems.RAW_CARBON.get(),4), List.of(), workers);
        h.assertTrue(missing.error.contains("Missing Materials"), "Missing inputs must be reported");
        var plan=ProductionPlan.build(h.getLevel(), new ItemStack(CrystalnexusModItems.RAW_CARBON.get(),4), List.of(new ItemStack(Items.COAL,2)), workers);
        h.assertTrue(plan.error.isEmpty() && plan.tasks.size()==2, "Planner must select available coal route over charcoal");
        var noMachine=ProductionPlan.build(h.getLevel(), new ItemStack(CrystalnexusModItems.RAW_CARBON.get()), List.of(new ItemStack(Items.COAL)), List.of());
        h.assertTrue(noMachine.error.contains("Missing Machine"), "Missing workers must be reported");
        var request=new ProductionPlan(new ItemStack(CrystalnexusModItems.RAW_CARBON.get(),4096));
        h.assertTrue(ProductionPlan.load(request.save(h.getLevel().registryAccess()),h.getLevel().registryAccess()).requested.getCount()==4096, "Large request must survive NBT round trip");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=1000)
    public static void parallelCrushersPauseAndReload(GameTestHelper h) {
        var c=build(h,7,5,7); List<BlockPos> positions=List.of(new BlockPos(2,2,3),new BlockPos(3,2,3),new BlockPos(4,2,3),new BlockPos(5,2,3));
        for (BlockPos p:positions) h.setBlock(p,CrystalnexusModBlocks.CRYSTAL_CRUSHER.get());
        c.rescan(); c.inventory.setStackInSlot(0,new ItemStack(Items.COAL,20)); c.energy.receiveEnergy(200000,false);
        c.enqueue(new ItemStack(CrystalnexusModItems.RAW_CARBON.get(),40));
        h.runAfterDelay(20,()->{
            h.assertTrue(c.activePlan()!=null && c.activePlan().tasks.stream().filter(t->t.machine!=null).count()==4,"Four crushers must run concurrently");
            for(BlockPos p:positions) {
                var port=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,h.absolutePos(p),null);
                h.assertTrue(port.insertItem(0,new ItemStack(Items.COAL),false).getCount()==1,"Assigned worker must reject automation input");
            }
            h.setBlock(new BlockPos(1,1,1),Blocks.AIR);
            h.assertTrue(!c.formed(),"Breaking casing must invalidate immediately");
            int progress=AssemblyLineMachine.at(h.getLevel(),h.absolutePos(positions.getFirst())).progress();
            h.runAfterDelay(10,()->{
                h.assertTrue(AssemblyLineMachine.at(h.getLevel(),h.absolutePos(positions.getFirst())).progress()==progress,"Broken shell must pause real worker progress");
                var registries=h.getLevel().registryAccess(); CompoundTag saved=c.saveWithFullMetadata(registries);
                for(BlockPos p:positions) { var worker=h.getBlockEntity(p); worker.loadWithComponents(worker.saveWithFullMetadata(registries),registries); }
                c.loadWithComponents(saved,registries); h.setBlock(new BlockPos(1,1,1),CrystalnexusModBlocks.ASSEMBLY_LINE_CASING.get()); c.retry();
            });
        });
        h.succeedWhen(()->{
            h.assertTrue(count(c,CrystalnexusModItems.RAW_CARBON.get(),27,36)==40,"All 20 operations must finish after repair/reload: "+c.status());
            h.assertTrue(c.activePlan()==null,"Completed tasks must retire");
            h.assertTrue(count(c,Items.COAL,0,27)==0,"Exactly 20 coal must be consumed");
            int remaining=c.energy.getEnergyStored();
            for(BlockPos p:positions) remaining+=((CrystalCrusherBlockEntity)h.getBlockEntity(p)).getEnergyStorage().getEnergyStored();
            h.assertTrue(remaining==200000-20*4096,"Workers must consume normal recipe FE exactly once");
        });
    }
    @GameTest(template="empty", timeoutTicks=1000)
    public static void circuitDependenciesAndOutputBackpressure(GameTestHelper h) {
        var c=build(h,5,5,5); h.setBlock(new BlockPos(2,2,3),CrystalnexusModBlocks.CIRCUIT_PRESS.get()); h.setBlock(new BlockPos(3,2,3),CrystalnexusModBlocks.CIRCUIT_PRESS.get()); c.rescan();
        c.inventory.setStackInSlot(0,new ItemStack(CrystalnexusModItems.SILICON.get(),4)); c.inventory.setStackInSlot(1,new ItemStack(Items.REDSTONE,4)); c.inventory.setStackInSlot(2,new ItemStack(Items.COPPER_INGOT,4));
        for(int i=27;i<36;i++) c.inventory.setStackInSlot(i,new ItemStack(Items.STONE,64));
        c.energy.receiveEnergy(100000,false); c.enqueue(new ItemStack(CrystalnexusModItems.ACCELERATION_UPGRADE.get(),4));
        h.runAfterDelay(600,()->{
            h.assertTrue(c.activePlan()!=null && c.activePlan().complete(),"Dependency graph must finish using real circuit presses: "+c.status());
            h.assertTrue(c.status().contains("Output storage full"),"Full output must pause final transfer");
            h.assertTrue(count(c,CrystalnexusModItems.BLANK_CHIP.get(),27,36)==0,"Intermediate chips must not become final outputs");
            c.inventory.setStackInSlot(27,ItemStack.EMPTY);
        });
        h.succeedWhen(()->h.assertTrue(count(c,CrystalnexusModItems.ACCELERATION_UPGRADE.get(),27,36)==4,"Requested upgrade chain must finish without item loss: "+c.status()));
    }
    @GameTest(template="empty", timeoutTicks=500)
    public static void partsAssemblerAndMissingMachine(GameTestHelper h) {
        var c=build(h,5,5,5); BlockPos worker=new BlockPos(2,2,3); h.setBlock(worker,CrystalnexusModBlocks.PARTS_ASSEMBLER.get()); c.rescan();
        c.inventory.setStackInSlot(0,new ItemStack(Items.COPPER_INGOT,2)); c.energy.receiveEnergy(10000,false); c.enqueue(new ItemStack(CrystalnexusModItems.COPPER_SHEET.get(),2));
        h.runAfterDelay(20,()->{
            var be=h.getBlockEntity(worker); var saved=be.saveWithFullMetadata(h.getLevel().registryAccess());
            h.setBlock(worker,Blocks.AIR); c.rescan();
            h.assertTrue(c.activePlan()!=null,"Removing a worker must preserve the job");
            h.runAfterDelay(10,()->{
                h.setBlock(worker,CrystalnexusModBlocks.PARTS_ASSEMBLER.get());
                h.getBlockEntity(worker).loadWithComponents(saved,h.getLevel().registryAccess()); c.retry();
            });
        });
        h.succeedWhen(()->h.assertTrue(count(c,CrystalnexusModItems.COPPER_SHEET.get(),27,36)==2,"Parts worker must resume its persisted assignment: "+c.status()));
    }
}
