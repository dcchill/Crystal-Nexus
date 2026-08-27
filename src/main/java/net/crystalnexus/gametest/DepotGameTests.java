package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.crystalnexus.data.DepotSavedData;
import net.crystalnexus.block.CraftingUpgradeBlock;
import net.crystalnexus.block.DepotCliBlock;
import net.crystalnexus.block.DepotCableBlock;
import net.crystalnexus.block.entity.CraftingUpgradeBlockEntity;
import net.crystalnexus.block.entity.DepotCliBlockEntity;
import net.crystalnexus.block.entity.DepotControllerBlockEntity;
import net.crystalnexus.block.entity.DepotCableBlockEntity;
import net.crystalnexus.block.entity.DepotCableConnectionConfig;
import net.crystalnexus.cli.DepotCliCommandContext;
import net.crystalnexus.cli.DepotCliCommandRegistry;
import net.crystalnexus.cli.DepotCraftingService;
import net.crystalnexus.cli.DepotProcessingService;
import net.crystalnexus.cli.DepotItemResolver;
import net.crystalnexus.cli.DepotJeiRecipeCache;
import net.crystalnexus.cli.DepotSendItemService;
import net.crystalnexus.cli.ProgramActionResult;
import net.crystalnexus.automation.DepotProgram;
import net.crystalnexus.automation.DepotProgramRuntime;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.jei_recipes.PurificationRecipe;
import net.crystalnexus.world.inventory.DepotCliMenu;
import net.crystalnexus.world.inventory.DepotMenu;
import net.crystalnexus.world.inventory.CraftingProcessorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class DepotGameTests {
    private DepotGameTests() {
    }

    @GameTest(template = "zero_point")
    public static void cableMachineRouting(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "depot-cable-test"), ClientInformation.createDefault());
        BlockPos controllerPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos furnacePos = new BlockPos(2, 1, 1);
        BlockPos overflowPos = new BlockPos(2, 3, 1);
        helper.setBlock(controllerPos, CrystalnexusModBlocks.DEPOT_CONTROLLER.get());
        helper.setBlock(cablePos, CrystalnexusModBlocks.DEPOT_CABLE.get());
        helper.setBlock(furnacePos, Blocks.FURNACE);
        helper.setBlock(overflowPos, Blocks.CHEST);

        DepotControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        controller.setOwner(player.getUUID());
        controller.getEnergyStorage().receiveEnergy(1_000, false);
        DepotSavedData depot = DepotSavedData.get(helper.getLevel(), player.getUUID());
        depot.setController(helper.getLevel(), helper.absolutePos(controllerPos));

        DepotCableBlockEntity cable = helper.getBlockEntity(cablePos);
        cable.refreshConnections();
        DepotCableConnectionConfig furnaceConfig = cable.connection(net.minecraft.core.Direction.DOWN);
        DepotCableConnectionConfig overflowConfig = cable.connection(net.minecraft.core.Direction.UP);
        helper.assertTrue(furnaceConfig != null && overflowConfig != null,
                "Each cable face touching an inventory must own a separate configuration");
        furnaceConfig.setFilterMode(DepotCableConnectionConfig.FilterMode.ALLOW_LISTED);
        furnaceConfig.setPriority(10);
        furnaceConfig.setFilter(0, new ItemStack(Items.RAW_IRON));
        overflowConfig.setFilterMode(DepotCableConnectionConfig.FilterMode.ALLOW_LISTED);
        overflowConfig.setPriority(-10);
        overflowConfig.setFilter(0, new ItemStack(Items.RAW_IRON));
        cable.update(net.minecraft.core.Direction.DOWN, furnaceConfig.priority(), furnaceConfig.filterMode(), furnaceConfig.itemFilters());
        cable.update(net.minecraft.core.Direction.UP, overflowConfig.priority(), overflowConfig.filterMode(), overflowConfig.itemFilters());

        CompoundTag cableTag = cable.saveWithFullMetadata(helper.getLevel().registryAccess());
        DepotCableBlockEntity restored = new DepotCableBlockEntity(helper.absolutePos(cablePos), helper.getBlockState(cablePos));
        restored.loadWithComponents(cableTag, helper.getLevel().registryAccess());
        helper.assertTrue(restored.connection(net.minecraft.core.Direction.DOWN).priority() == 10
                        && restored.connection(net.minecraft.core.Direction.DOWN).accepts(new ItemStack(Items.RAW_IRON))
                        && !restored.connection(net.minecraft.core.Direction.DOWN).accepts(new ItemStack(Items.COBBLESTONE)),
                "Cable face priority and exact-item filters must survive block-entity save/load");

        ResourceLocation rawIron = ResourceLocation.parse("minecraft:raw_iron");
        ResourceLocation cobblestone = ResourceLocation.parse("minecraft:cobblestone");
        depot.deposit(cobblestone, 1);
        DepotSendItemService.Result rejected = DepotSendItemService.send(player, depot, cobblestone, 1);
        helper.assertTrue(rejected.status() == ProgramActionResult.WAITING && depot.getCount(cobblestone) == 1,
                "A rejected Send Item action must wait without extracting or reinserting the item");
        depot.remove(cobblestone, 1);

        FurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
        ChestBlockEntity overflow = helper.getBlockEntity(overflowPos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 63));
        depot.deposit(rawIron, 8);
        DepotSendItemService.Result routed = DepotSendItemService.send(player, depot, rawIron, 8);
        helper.assertTrue(routed.sent() == 8 && furnace.getItem(0).getCount() == 64
                        && overflow.getItem(0).is(Items.RAW_IRON) && overflow.getItem(0).getCount() == 7
                        && depot.getCount(rawIron) == 0,
                "Send Item routing mismatch: sent=" + routed.sent()
                        + ", furnace=" + furnace.getItem(0).getCount()
                        + ", overflow=" + overflow.getItem(0).getCount()
                        + ", depot=" + depot.getCount(rawIron));
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void depotProgramming(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "depot-program-test"), ClientInformation.createDefault());
        BlockPos controllerPos = new BlockPos(3, 2, 3);
        BlockPos processorPos = new BlockPos(3, 2, 2);
        BlockPos importCablePos = new BlockPos(4, 2, 3);
        BlockPos sourcePos = new BlockPos(4, 1, 3);
        BlockPos outputCablePos = new BlockPos(2, 2, 3);
        BlockPos destinationPos = new BlockPos(2, 1, 3);
        helper.setBlock(controllerPos, CrystalnexusModBlocks.DEPOT_CONTROLLER.get());
        helper.setBlock(processorPos, CrystalnexusModBlocks.CRAFTING_UPGRADE.get());
        helper.setBlock(importCablePos, CrystalnexusModBlocks.DEPOT_CABLE.get().defaultBlockState()
                .setValue(DepotCableBlock.MODE, net.crystalnexus.block.DepotCableMode.IMPORT));
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(outputCablePos, CrystalnexusModBlocks.DEPOT_CABLE.get());
        helper.setBlock(destinationPos, Blocks.CHEST);

        DepotControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        controller.setOwner(player.getUUID());
        controller.getEnergyStorage().receiveEnergy(1_000, false);
        DepotSavedData depot = DepotSavedData.get(helper.getLevel(), player.getUUID());
        depot.setController(helper.getLevel(), helper.absolutePos(controllerPos));
        ResourceLocation rawIron = ResourceLocation.parse("minecraft:raw_iron");

        DepotCableBlockEntity outputCable = helper.getBlockEntity(outputCablePos);
        outputCable.refreshConnections();
        DepotCableConnectionConfig output = outputCable.connection(net.minecraft.core.Direction.DOWN);
        output.setFilterMode(DepotCableConnectionConfig.FilterMode.ALLOW_LISTED);
        output.setFilter(0, new ItemStack(Items.RAW_IRON));
        outputCable.update(net.minecraft.core.Direction.DOWN, 10, output.filterMode(), output.itemFilters());

        DepotProgram importProgram = new DepotProgram(UUID.randomUUID(), "Ore Processing", true,
                new DepotProgram.ProgramTrigger(DepotProgram.TriggerType.ITEM_ADDED, rawIron),
                List.of(new DepotProgram.ProgramCondition(DepotProgram.ConditionType.COUNT_AT_LEAST, rawIron, 1)),
                List.of(new DepotProgram.ProgramAction(DepotProgram.ActionType.SEND_ITEM, rawIron, 64)));
        depot.putProgram(importProgram);
        DepotSavedData restored = DepotSavedData.load(depot.save(new CompoundTag(), helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertTrue(restored.getPrograms().size() == 1
                        && restored.getPrograms().getFirst().id().equals(importProgram.id())
                        && restored.getPrograms().getFirst().conditions().size() == 1,
                "Programs and their reusable WHEN/IF/DO model must survive world persistence");

        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity destination = helper.getBlockEntity(destinationPos);
        source.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        DepotCableBlock importCable = (DepotCableBlock) helper.getBlockState(importCablePos).getBlock();
        importCable.tick(helper.getBlockState(importCablePos), helper.getLevel(), helper.absolutePos(importCablePos),
                net.minecraft.util.RandomSource.create());
        int importActions = DepotProgramRuntime.process(player, depot);
        helper.assertTrue(importActions == 1 && source.getItem(0).isEmpty()
                        && destination.getItem(0).is(Items.RAW_IRON) && destination.getItem(0).getCount() == 8
                        && depot.getCount(rawIron) == 0,
                "IMPORT program mismatch: actions=" + importActions
                        + ", source=" + source.getItem(0).getCount()
                        + ", destination=" + destination.getItem(0).getCount()
                        + ", depot=" + depot.getCount(rawIron)
                        + ", endpoints=" + net.crystalnexus.util.DepotNetwork.machineEndpoints(player).size());

        depot.removeProgram(importProgram.id());
        DepotProgram loopProgram = new DepotProgram(UUID.randomUUID(), "Bounded Loop", true,
                new DepotProgram.ProgramTrigger(DepotProgram.TriggerType.INVENTORY_CHANGED, null), List.of(),
                List.of(new DepotProgram.ProgramAction(DepotProgram.ActionType.SEND_ITEM, rawIron, 1)));
        depot.putProgram(loopProgram);
        depot.deposit(rawIron, 5);
        int loopActions = DepotProgramRuntime.process(player, depot);
        helper.assertTrue(loopActions == 1 && depot.getCount(rawIron) == 4
                        && destination.getItem(0).getCount() == 9,
                "A program must execute at most once in its immediate mutation transaction");

        depot.removeProgram(loopProgram.id());
        depot.remove(rawIron, Long.MAX_VALUE);
        ResourceLocation nuggets = ResourceLocation.parse("minecraft:iron_nugget");
        ResourceLocation ingot = ResourceLocation.parse("minecraft:iron_ingot");
        DepotProgram craftProgram = new DepotProgram(UUID.randomUUID(), "Ingot Stock", true,
                new DepotProgram.ProgramTrigger(DepotProgram.TriggerType.ITEM_ADDED, nuggets), List.of(),
                List.of(new DepotProgram.ProgramAction(DepotProgram.ActionType.CRAFT, ingot, 1)));
        depot.putProgram(craftProgram);
        depot.deposit(nuggets, 9);
        int craftActions = DepotProgramRuntime.process(player, depot);
        int repeatedActions = DepotProgramRuntime.process(player, depot);
        helper.assertTrue(craftActions == 1 && repeatedActions == 0 && depot.getCraftingJobs().size() == 1,
                "Craft programs must queue through existing crafting once, without polling or duplicate jobs");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void storageSafety(GameTestHelper helper) {
        DepotSavedData depot = new DepotSavedData();
        for (int i = 0; i < 100; i++) depot.addUpgrade();
        int saturatedLevel = depot.getUpgradeLevel();
        for (int i = 0; i < 100; i++) depot.addUpgrade();
        helper.assertTrue(saturatedLevel > 0 && depot.getUpgradeLevel() == saturatedLevel,
                "Excess depot upgrades must stop without wrapping or resetting");
        helper.assertTrue(depot.deposit(ResourceLocation.parse("crystalnexus:depot_uplink"), 1) == 0,
                "Depot uplinks must never enter depot storage");

        ResourceLocation persistedOutput = ResourceLocation.parse("minecraft:iron_ingot");
        ResourceLocation persistedInput = ResourceLocation.parse("minecraft:raw_iron");
        ResourceLocation persistedByproduct = ResourceLocation.parse("minecraft:iron_nugget");
        ResourceLocation persistedMachine = ResourceLocation.parse("minecraft:furnace");
        Map<ResourceLocation, Long> persistedOutputs = Map.of(persistedOutput, 1L, persistedByproduct, 2L);
        depot.setProcessingPattern(persistedOutput, 1, Map.of(persistedInput, 1L), persistedOutputs);
        depot.setPreferredMachine(persistedOutput, persistedMachine);
        depot.deposit(persistedInput, 1);
        DepotSavedData.CraftingJob persistedJob = depot.startCraftingJob(persistedOutput, 1, 1, 3,
                Map.of(persistedInput, 1L), persistedOutputs,
                List.of(new DepotSavedData.CraftingStep(persistedOutput, 1, 1,
                        List.of(new DepotSavedData.SlotEntry(persistedInput, 1L)), persistedOutputs, true)));
        ResourceLocation secondInput = ResourceLocation.parse("minecraft:gold_nugget");
        ResourceLocation secondOutput = ResourceLocation.parse("minecraft:gold_ingot");
        depot.deposit(secondInput, 9);
        DepotSavedData.CraftingJob secondJob = depot.startCraftingJob(secondOutput, 1, 4, 9,
                Map.of(secondInput, 9L), Map.of(secondOutput, 1L),
                List.of(new DepotSavedData.CraftingStep(secondOutput, 1, 4,
                        List.of(new DepotSavedData.SlotEntry(secondInput, 9L)), Map.of(secondOutput, 1L))));
        depot.updateProcessingTask(new DepotSavedData.ProcessingTask(ResourceLocation.parse("minecraft:overworld"),
                BlockPos.ZERO, List.of(), persistedOutputs), Map.of(persistedInput, 1L), Map.of());
        DepotSavedData restored = DepotSavedData.load(
                depot.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(persistedJob != null && secondJob != null && restored.getCraftingJobs().size() == 2
                        && restored.getCraftingJob() != null
                        && restored.getCraftingJob().currentStep().processing()
                        && restored.getProcessingTask() != null
                        && persistedMachine.equals(restored.getPreferredMachine(persistedOutput))
                        && restored.getProcessingPattern(persistedOutput).outputs()
                        .getOrDefault(persistedByproduct, 0L) == 2,
                "Concurrent jobs, processing patterns, byproducts, active machine steps, and tasks must survive save/load");
        restored.cancelCraftingJob(secondJob.id());
        helper.assertTrue(restored.getCraftingJobs().size() == 1 && restored.getCount(secondInput) == 9,
                "Cancelling one concurrent job must preserve the other job and return only its materials");
        restored.cancelCraftingJob(restored.getCraftingJob().id());
        helper.assertTrue(restored.getProcessingTask() == null,
                "Cancelling a processing job must stop tracking its machine without undoing dispatched inputs");

        DepotItemResolver.Result exact = DepotItemResolver.registry("minecraft:stone");
        helper.assertTrue(exact.found() && exact.match().id().equals(ResourceLocation.parse("minecraft:stone")),
                "Exact registry ids must resolve exactly");
        DepotItemResolver.Result ambiguous = DepotItemResolver.registry("planks");
        helper.assertTrue(!ambiguous.found() && !ambiguous.ambiguous().isEmpty(),
                "Ambiguous names must not resolve to an arbitrary item");
        DepotItemResolver.Result displayName = DepotItemResolver.registry("Iron Ingot");
        helper.assertTrue(displayName.found() && displayName.match().id().equals(ResourceLocation.parse("minecraft:iron_ingot")),
                "Display names must resolve without requiring registry identifiers");

        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "depot-test-player"), ClientInformation.createDefault());
        BlockPos controllerPos = new BlockPos(1, 2, 1);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos cliPos = new BlockPos(3, 2, 1);
        BlockPos downloaderPos = new BlockPos(2, 2, 0);
        BlockPos absoluteCliPos = helper.absolutePos(cliPos);
        player.setPos(absoluteCliPos.getX() + 0.5, absoluteCliPos.getY(), absoluteCliPos.getZ() + 0.5);
        helper.setBlock(controllerPos, CrystalnexusModBlocks.DEPOT_CONTROLLER.get());
        helper.setBlock(cablePos, CrystalnexusModBlocks.DEPOT_CABLE.get());
        helper.setBlock(cliPos, CrystalnexusModBlocks.DEPOT_CLI.get());
        helper.setBlock(downloaderPos, CrystalnexusModBlocks.DEPOT_DOWNLOADER.get());
        helper.assertTrue(helper.getBlockState(cablePos).getValue(DepotCableBlock.NORTH),
                "Depot cables must visually connect to depot downloaders");
        DepotControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
        DepotCliBlockEntity cli = helper.getBlockEntity(cliPos);
        controller.setOwner(player.getUUID());
        controller.getEnergyStorage().receiveEnergy(1_000, false);
        cli.setOwner(player.getUUID());

        DepotSavedData playerDepot = DepotSavedData.get(helper.getLevel(), player.getUUID());
        playerDepot.setController(helper.getLevel(), helper.absolutePos(controllerPos));
        DepotCliMenu menu = new DepotCliMenu(1, player.getInventory(), absoluteCliPos);
        DepotCliCommandContext context = new DepotCliCommandContext(player, menu, playerDepot);
        helper.assertTrue(menu.isConnected(player), "The owner's cabled CLI must connect to the powered controller");
        helper.assertTrue(CrystalnexusModBlocks.DEPOT_CLI.get().getLightEmission(
                        CrystalnexusModBlocks.DEPOT_CLI.get().defaultBlockState().setValue(DepotCliBlock.CONNECTED, true),
                        helper.getLevel(), helper.absolutePos(cliPos)) > 0,
                "An active Depot CLI must emit light");
        FriendlyByteBuf downloaderData = new FriendlyByteBuf(Unpooled.buffer());
        downloaderData.writeBlockPos(helper.absolutePos(downloaderPos)).writeBoolean(false).writeBoolean(false);
        DepotMenu downloaderMenu = new DepotMenu(2, player.getInventory(), downloaderData);
        helper.assertTrue(downloaderMenu.canAccessDepot(player),
                "A downloader cabled to the player's powered controller must work");
        helper.assertTrue(DepotSavedData.get(helper.getLevel(), java.util.UUID.randomUUID()) != playerDepot,
                "Different players must use different depot saved data");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
                        .map(RecipeHolder::value).filter(PurificationRecipe.class::isInstance)
                        .map(PurificationRecipe.class::cast).anyMatch(PurificationRecipe::isSpecial),
                "Crystal Nexus recipes must remain discoverable by JEI without entering the vanilla recipe book");

        ResourceLocation logs = ResourceLocation.parse("minecraft:oak_log");
        ResourceLocation planks = ResourceLocation.parse("minecraft:oak_planks");
        playerDepot.deposit(logs, 16);
        DepotCliCommandRegistry.INSTANCE.execute(context, "craft minecraft:oak_planks 64");
        helper.assertTrue(playerDepot.getCount(logs) == 16 && playerDepot.getCount(planks) == 0,
                "Crafting without a connected upgrade must not consume ingredients");
        BlockPos processorPos = new BlockPos(2, 2, 2);
        BlockPos corePos = new BlockPos(2, 3, 1);
        helper.setBlock(processorPos, CrystalnexusModBlocks.CRAFTING_UPGRADE.get());
        helper.setBlock(corePos, CrystalnexusModBlocks.CRAFTING_CORE.get());
        helper.assertTrue(net.crystalnexus.util.DepotNetwork.craftingProcessorCount(player) == 1,
                "Crafting Cores must add job lanes without incorrectly increasing crafting speed");
        helper.assertTrue(net.crystalnexus.util.DepotNetwork.craftingJobCapacity(player) == 2,
                "A connected Crafting Core must add one concurrent crafting process");
        DepotCraftingService.Preview plankPreview = DepotCraftingService.preview(player, playerDepot, Items.OAK_PLANKS, 64);
        helper.assertTrue(plankPreview.success() && plankPreview.startable()
                        && plankPreview.nodes().stream().anyMatch(node -> node.itemId().equals(logs)
                        && node.source() == DepotCraftingService.PreviewSource.STORED)
                        && playerDepot.getCount(logs) == 16 && playerDepot.getCraftingJob() == null,
                "Visual crafting previews must show stored dependencies without reserving or changing them");
        ResourceLocation glowstoneDust = ResourceLocation.parse("minecraft:glowstone_dust");
        playerDepot.deposit(glowstoneDust, 1_000);
        DepotCraftingService.Preview lampPreview = DepotCraftingService.preview(
                player, playerDepot, Items.REDSTONE_LAMP, 1);
        helper.assertTrue(!lampPreview.success()
                        && lampPreview.details().stream().anyMatch(detail -> detail.toLowerCase().contains("redstone"))
                        && lampPreview.details().stream().noneMatch(detail -> detail.toLowerCase().contains("glowstone dust"))
                        && lampPreview.nodes().stream().filter(node -> node.itemId().equals(Items.REDSTONE.builtInRegistryHolder().key().location()))
                        .anyMatch(node -> node.required() == 4 && node.source() == DepotCraftingService.PreviewSource.MISSING)
                        && lampPreview.nodes().stream().anyMatch(node -> node.itemId().equals(glowstoneDust)
                        && node.source() == DepotCraftingService.PreviewSource.STORED),
                "A failed branch must aggregate repeated slots and report the later missing ingredient, not an ingredient already stored in the depot: "
                        + lampPreview.details());
        playerDepot.remove(glowstoneDust, Long.MAX_VALUE);
        DepotCliCommandRegistry.INSTANCE.execute(context, "machine balance on");
        helper.assertTrue(playerDepot.isMachineLoadBalancing(),
                "Machine load balancing must be configurable and persist in depot data");
        DepotCliCommandRegistry.INSTANCE.execute(context, "craft minecraft:oak_planks 64");
        helper.assertTrue(playerDepot.getCount(logs) == 0 && playerDepot.getCount(planks) == 0
                        && playerDepot.getCraftingJob() != null,
                "CLI crafting must reserve ingredients and queue output instead of completing immediately");
        DepotSavedData.CraftingJob plankJob = playerDepot.getCraftingJob();
        helper.assertTrue(plankJob.steps().size() == 64 && plankJob.totalWork() == 320
                        && plankJob.currentStep().outputAmount() == 1 && plankJob.currentStep().work() == 5,
                "Large jobs must expose one output item at a time without changing their total speed");
        CraftingProcessorMenu processorMenu = new CraftingProcessorMenu(3, player.getInventory(),
                helper.absolutePos(processorPos));
        helper.assertTrue(processorMenu.getJobId() == plankJob.id() && processorMenu.getStepCount() == 64
                        && processorMenu.getStepAmount() == 1,
                "The Crafting Processor menu must report the live job and current craft");
        long oneProcessorTicks = DepotCraftingService.estimatedTicks(playerDepot.getCraftingJob().remainingWork(), 1);
        long twoProcessorTicks = DepotCraftingService.estimatedTicks(playerDepot.getCraftingJob().remainingWork(), 2);
        helper.assertTrue(twoProcessorTicks < oneProcessorTicks,
                "Each additional Crafting Processor must reduce the remaining craft time");
        for (int i = 0; i < 5; i++) playerDepot.advanceCraftingJob(1);
        helper.assertTrue(playerDepot.getCount(planks) == 1
                        && playerDepot.getCraftingJob().workingItems().getOrDefault(planks, 0L) == 0,
                "Each completed target item must immediately appear in depot storage");
        DepotCliCommandRegistry.INSTANCE.execute(context, "craft minecraft:oak_button 1");
        helper.assertTrue(playerDepot.getCraftingJobs().size() == 2,
                "A Crafting Core must allow a second job to start while the first is active");
        DepotSavedData.CraftingJob buttonJob = playerDepot.getCraftingJobs().get(1);
        playerDepot.cancelCraftingJob(buttonJob.id());
        helper.assertTrue(playerDepot.getCraftingJobs().size() == 1 && playerDepot.getCraftingJob().id() == plankJob.id(),
                "Cancelling a concurrent job must leave the other crafting process active");
        for (int i = 0; i < 15; i++) playerDepot.advanceCraftingJob(1);
        helper.assertTrue(playerDepot.getCount(planks) == 4
                        && playerDepot.getCraftingJob().workingItems().getOrDefault(logs, 0L) == 15
                        && playerDepot.getCraftingJob().workingItems().getOrDefault(planks, 0L) == 0
                        && processorMenu.getStepIndex() == 4,
                "Each completed item must update depot storage and processor progress immediately");
        playerDepot.cancelCraftingJob(plankJob.id());
        helper.assertTrue(playerDepot.getCraftingJob() == null && playerDepot.getCount(logs) == 15
                        && playerDepot.getCount(planks) == 4,
                "Cancelling must keep completed crafts and return only the current workspace");
        DepotCliCommandRegistry.INSTANCE.execute(context, "craft minecraft:oak_planks 60");
        finishCraft(playerDepot, 1);
        helper.assertTrue(playerDepot.getCount(planks) == 64,
                "Queued CLI output must enter depot storage only after its work completes");

        BlockPos machinePos = new BlockPos(2, 3, 1);
        helper.setBlock(machinePos, Blocks.FURNACE);
        helper.assertTrue(helper.getBlockState(cablePos).getValue(DepotCableBlock.UP)
                        && net.crystalnexus.util.DepotNetwork.processingMachines(player).stream()
                        .anyMatch(endpoint -> endpoint.pos().equals(helper.absolutePos(machinePos))),
                "Depot cables must connect to and discover adjacent item-handler machines");
        FurnaceBlockEntity furnace = helper.getBlockEntity(machinePos);
        furnace.setItem(1, new ItemStack(Items.COAL));
        ResourceLocation syncedRecipeId = ResourceLocation.parse("crystalnexus:jei/test_iron_processing");
        DepotJeiRecipeCache.accept(player, 1, true, List.of(new DepotJeiRecipeCache.Recipe(
                syncedRecipeId, ResourceLocation.parse("mekanism:enriching"), "Enriching",
                List.of(new DepotJeiRecipeCache.Slot(List.of(
                        new DepotJeiRecipeCache.StackRef(ResourceLocation.parse("minecraft:raw_iron"), 1),
                        new DepotJeiRecipeCache.StackRef(ResourceLocation.parse("minecraft:iron_ore"), 1)))),
                List.of(new DepotJeiRecipeCache.StackRef(ResourceLocation.parse("minecraft:iron_ingot"), 1)),
                List.of(ResourceLocation.parse("minecraft:furnace")))));
        List<DepotCraftingService.RecipeChoice> ironChoices = DepotCraftingService.recipeChoices(player, Items.IRON_INGOT);
        int syncedChoice = java.util.stream.IntStream.range(0, ironChoices.size())
                .filter(index -> ironChoices.get(index).id().equals(syncedRecipeId)).findFirst().orElseThrow() + 1;
        var listedRecipes = DepotCliCommandRegistry.INSTANCE.execute(context, "recipe list minecraft:iron_ingot");
        ResourceLocation rawIron = ResourceLocation.parse("minecraft:raw_iron");
        ResourceLocation ironIngot = ResourceLocation.parse("minecraft:iron_ingot");
        ResourceLocation ironNugget = ResourceLocation.parse("minecraft:iron_nugget");
        playerDepot.deposit(ironNugget, 9);
        playerDepot.deposit(rawIron, 1);
        DepotCraftingService.Result defaultCrafting = DepotCraftingService.craft(player, playerDepot, Items.IRON_INGOT, 1);
        helper.assertTrue(defaultCrafting.success() && !defaultCrafting.job().currentStep().processing(),
                "Vanilla crafting must win over smelting and synced JEI machine recipes by default");
        finishCraft(playerDepot, 1);
        helper.assertTrue(playerDepot.getCount(ironNugget) == 0 && playerDepot.getCount(rawIron) == 1
                        && playerDepot.getCount(ironIngot) == 1,
                "Default vanilla crafting must not consume the smeltable raw iron");
        playerDepot.remove(ironIngot, Long.MAX_VALUE);
        DepotCraftingService.Preview manualMachinePreview = DepotCraftingService.preview(player, playerDepot, Items.IRON_INGOT, 1);
        helper.assertTrue(!manualMachinePreview.success() && manualMachinePreview.nodes().getFirst().alternatives()
                        .stream().anyMatch(DepotCraftingService.RecipeChoice::processing)
                        && playerDepot.getCount(rawIron) == 1,
                "The visual planner must expose machine routes without selecting them automatically");
        DepotCraftingService.Result defaultSmelting = DepotCraftingService.craft(player, playerDepot, Items.IRON_INGOT, 1);
        helper.assertTrue(defaultSmelting.success() && defaultSmelting.job().currentStep().processing()
                        && defaultSmelting.job().currentStep().machineTypes().equals(List.of(ResourceLocation.parse("minecraft:furnace"))),
                "Vanilla smelting must win over synced JEI machine recipes and target a furnace: success="
                        + defaultSmelting.success() + ", details=" + defaultSmelting.details() + ", step="
                        + (defaultSmelting.job() == null ? null : defaultSmelting.job().currentStep()));
        runFurnaceJob(helper, player, playerDepot, machinePos, furnace);
        helper.assertTrue(playerDepot.getCraftingJob() == null && playerDepot.getCount(ironIngot) == 1,
                "Vanilla smelting must insert into the furnace and extract its output");
        playerDepot.deposit(rawIron, 1);
        DepotCliCommandRegistry.INSTANCE.execute(context, "smelt minecraft:raw_iron 1");
        helper.assertTrue(playerDepot.getCraftingJob() != null && playerDepot.getCraftingJob().currentStep().processing()
                        && playerDepot.getCraftingJob().currentStep().inputs().equals(List.of(new DepotSavedData.SlotEntry(rawIron, 1)))
                        && playerDepot.getCraftingJob().currentStep().machineTypes().contains(ResourceLocation.parse("minecraft:furnace")),
                "The smelt command must queue a direct single-item furnace step");
        playerDepot.cancelCraftingJob(playerDepot.getCraftingJob().id());
        playerDepot.remove(ironIngot, Long.MAX_VALUE);
        DepotCliCommandRegistry.INSTANCE.execute(context, "recipe prefer minecraft:iron_ingot " + syncedChoice);
        DepotCliCommandRegistry.INSTANCE.execute(context, "machine prefer minecraft:iron_ingot 1");
        helper.assertTrue(listedRecipes.lines().stream().anyMatch(line -> line.contains(syncedChoice + ". [Enriching]"))
                        && syncedRecipeId.equals(playerDepot.getPreferredRecipe(ironIngot))
                        && ResourceLocation.parse("minecraft:furnace").equals(
                                playerDepot.getPreferredMachine(ironIngot)),
                "JEI recipes and catalyst machines must be selectable by friendly numbers instead of recipe ids");
        helper.assertTrue(DepotCraftingService.recipesFor(player, Items.IRON_INGOT).stream()
                        .anyMatch(DepotCraftingService.AvailableRecipe::processing),
                "The depot must discover machine recipes from the server recipe data shown by JEI");
        helper.assertTrue(DepotCraftingService.recipesFor(player, Items.IRON_INGOT).stream()
                        .anyMatch(candidate -> candidate.id().toString().contains("smelting")),
                "The depot must discover vanilla smelting recipes for iron ingots: "
                        + DepotCraftingService.recipesFor(player, Items.IRON_INGOT).stream()
                        .limit(8).map(candidate -> candidate.id().toString()).toList());
        DepotCraftingService.Result processOnly = DepotCraftingService.process(player, playerDepot, Items.IRON_INGOT, 1);
        helper.assertTrue(processOnly.success() && processOnly.job().currentStep().processing(),
                "The process command must require an external-machine final step");
        playerDepot.cancelCraftingJob(processOnly.job().id());
        helper.assertTrue(DepotCraftingService.recipesFor(player,
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse("crystalnexus:coal_singularity"))).stream()
                        .anyMatch(candidate -> candidate.recipe() instanceof net.crystalnexus.jei_recipes.CrystalNexusRecipe recipe
                                && recipe.getInputCount(0) == 10_368),
                "Automatic processing must preserve custom JEI recipe ingredient counts");
        playerDepot.deposit(rawIron, 2);
        DepotCraftingService.Result machineCraft = DepotCraftingService.craft(player, playerDepot, Items.IRON_INGOT, 2);
        helper.assertTrue(machineCraft.success() && machineCraft.job().currentStep().processing()
                        && machineCraft.job().currentStep().machineTypes().equals(List.of(ResourceLocation.parse("minecraft:furnace"))),
                "A synced JEI layout must become an asynchronous step restricted to its machine catalyst: success="
                        + machineCraft.success() + ", details=" + machineCraft.details());
        runFurnaceJob(helper, player, playerDepot, machinePos, furnace);
        helper.assertTrue(playerDepot.getCraftingJob() != null
                        && playerDepot.getCount(ironIngot) == 1,
                "Each machine-crafted target item must update depot storage before the job finishes");
        runFurnaceJob(helper, player, playerDepot, machinePos, furnace);
        helper.assertTrue(playerDepot.getCraftingJob() == null
                        && playerDepot.getCount(ironIngot) == 2,
                "The depot must insert machine inputs and extract the completed furnace output");

        playerDepot.remove(ironIngot, Long.MAX_VALUE);
        playerDepot.deposit(rawIron, 1);
        playerDepot.deposit(ResourceLocation.parse("minecraft:cobblestone"), 4);
        playerDepot.deposit(ResourceLocation.parse("minecraft:redstone"), 1);
        DepotCraftingService.Result processedDependency = DepotCraftingService.craft(player, playerDepot, Items.PISTON, 1);
        helper.assertTrue(processedDependency.success() && processedDependency.job().steps().stream()
                        .anyMatch(DepotSavedData.CraftingStep::processing),
                "Recursive crafting must use processing patterns for machine-made dependencies");
        runFurnaceJob(helper, player, playerDepot, machinePos, furnace);
        finishCraft(playerDepot, 1);
        helper.assertTrue(playerDepot.getCount(ResourceLocation.parse("minecraft:piston")) == 1,
                "A dependent crafting recipe must continue after its machine output returns to the depot job");
        playerDepot.remove(ResourceLocation.parse("minecraft:piston"), Long.MAX_VALUE);
        playerDepot.remove(ResourceLocation.parse("minecraft:iron_ingot"), Long.MAX_VALUE);
        playerDepot.remove(ResourceLocation.parse("minecraft:cobblestone"), Long.MAX_VALUE);
        playerDepot.remove(ResourceLocation.parse("minecraft:redstone"), Long.MAX_VALUE);

        ResourceLocation redstone = ResourceLocation.parse("minecraft:redstone");
        ResourceLocation cobblestone = ResourceLocation.parse("minecraft:cobblestone");
        ResourceLocation ironBlock = ResourceLocation.parse("minecraft:iron_block");
        ResourceLocation piston = ResourceLocation.parse("minecraft:piston");
        playerDepot.deposit(cobblestone, 4);
        playerDepot.deposit(redstone, 1);
        playerDepot.deposit(ironBlock, 1);
        DepotCraftingService.Result recursiveCraft = DepotCraftingService.craft(player, playerDepot, Items.PISTON, 1);
        helper.assertTrue(recursiveCraft.success() && playerDepot.getCount(piston) == 0
                        && playerDepot.getCount(ironBlock) == 0 && recursiveCraft.job().steps().size() > 1
                        && recursiveCraft.job().currentStep().outputId().equals(ironIngot),
                "Recursive crafting must turn an iron block into the ingot needed for a piston");
        CraftingUpgradeBlockEntity processor = helper.getBlockEntity(processorPos);
        for (int i = 0; i < 20; i++) CraftingUpgradeBlockEntity.tick(helper.getLevel(),
                helper.absolutePos(processorPos), helper.getBlockState(processorPos), processor);
        helper.assertTrue(helper.getBlockState(processorPos).getValue(CraftingUpgradeBlock.ACTIVE)
                        && CrystalnexusModBlocks.CRAFTING_UPGRADE.get().getLightEmission(
                        helper.getBlockState(processorPos), helper.getLevel(), helper.absolutePos(processorPos)) > 0,
                "A Crafting Processor handling a job must emit light");
        for (int i = 0; i < 20; i++) playerDepot.advanceCraftingJob(1);
        helper.assertTrue(playerDepot.getCraftingJob() != null
                        && playerDepot.getCraftingJob().workingItems().getOrDefault(ironIngot, 0L) == 9
                        && playerDepot.getCraftingJob().currentStep().outputId().equals(piston),
                "Prerequisite items must be crafted before the dependent recipe begins");
        finishCraft(playerDepot, 1);
        helper.assertTrue(playerDepot.getCount(piston) == 1 && playerDepot.getCount(ironIngot) == 8,
                "Recursive craft products must appear when the more-complex timed job completes");

        playerDepot.remove(piston, Long.MAX_VALUE);
        playerDepot.remove(ironIngot, Long.MAX_VALUE);
        playerDepot.deposit(cobblestone, 4);
        playerDepot.deposit(redstone, 1);
        playerDepot.deposit(ironBlock, 1);
        playerDepot.deposit(ironNugget, 9);
        RecipeHolder<CraftingRecipe> nuggetRecipe = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(holder -> holder.value().getResultItem(helper.getLevel().registryAccess()).is(Items.IRON_INGOT))
                .filter(holder -> holder.value().getIngredients().stream().flatMap(ingredient -> java.util.Arrays.stream(ingredient.getItems()))
                        .anyMatch(stack -> stack.is(Items.IRON_NUGGET)))
                .findFirst().orElseThrow();
        List<DepotCraftingService.RecipeChoice> ingotChoices = DepotCraftingService.recipeChoices(player, Items.IRON_INGOT);
        int nuggetChoice = java.util.stream.IntStream.range(0, ingotChoices.size())
                .filter(index -> ingotChoices.get(index).id().equals(nuggetRecipe.id())).findFirst().orElseThrow() + 1;
        DepotCliCommandRegistry.INSTANCE.execute(context,
                "recipe prefer minecraft:iron_ingot " + nuggetChoice);
        DepotCraftingService.Result preferredCraft = DepotCraftingService.craft(player, playerDepot, Items.PISTON, 1);
        helper.assertTrue(preferredCraft.success() && nuggetRecipe.id().equals(playerDepot.getPreferredRecipe(ironIngot))
                        && playerDepot.getCount(ironNugget) == 0 && playerDepot.getCount(ironBlock) == 1,
                "A preferred intermediate recipe must win across an advanced recursive craft");
        finishCraft(playerDepot, 1);

        playerDepot.remove(planks, Long.MAX_VALUE);
        playerDepot.remove(cobblestone, Long.MAX_VALUE);
        playerDepot.remove(redstone, Long.MAX_VALUE);
        playerDepot.remove(ironBlock, Long.MAX_VALUE);
        playerDepot.remove(ironIngot, Long.MAX_VALUE);
        DepotCraftingService.Result missingCraft = DepotCraftingService.craft(player, playerDepot, Items.PISTON, 1);
        helper.assertTrue(!missingCraft.success() && missingCraft.details().stream().skip(1)
                        .noneMatch(line -> line.trim().startsWith("#"))
                        && missingCraft.details().stream().noneMatch(line -> line.startsWith("Circular recipe path:")),
                "Missing tag ingredients must list concrete alternatives without reporting abandoned cyclic routes: "
                        + missingCraft.details());

        DepotMenu craftingMenu = new DepotMenu(2, player.getInventory(), true, true);
        helper.assertTrue(craftingMenu.slots.size() == DepotMenu.PAGE_SIZE + 36,
                "Crafting Processors must not add a crafting grid to the Depot Uplink");

        ResourceLocation stone = ResourceLocation.parse("minecraft:stone");
        playerDepot.deposit(stone, 64);
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, new ItemStack(Items.DIRT, 64));
        }
        DepotCliCommandRegistry.INSTANCE.execute(context, "take minecraft:stone 64");
        helper.assertTrue(playerDepot.getCount(stone) == 64,
                "A full player inventory must return all extraction overflow to storage");

        player.getInventory().items.set(player.getInventory().selected,
                new ItemStack(CrystalnexusModItems.DEPOT_UPLINK.get()));
        DepotCliCommandRegistry.INSTANCE.execute(context, "deposit held");
        helper.assertTrue(player.getMainHandItem().is(CrystalnexusModItems.DEPOT_UPLINK.get()),
                "A rejected uplink deposit must remain in the player's hand");

        cli.setOwner(java.util.UUID.randomUUID());
        DepotCliCommandRegistry.INSTANCE.execute(context, "take minecraft:stone 1");
        helper.assertTrue(playerDepot.getCount(stone) == 64,
                "Losing permission while open must prevent extraction");
        cli.setOwner(player.getUUID());
        helper.getLevel().removeBlock(helper.absolutePos(cliPos), false);
        DepotCliCommandRegistry.INSTANCE.execute(context, "take minecraft:stone 1");
        helper.assertTrue(playerDepot.getCount(stone) == 64 && !menu.stillValid(player),
                "Breaking an open CLI must not extract or duplicate items");

        helper.setBlock(cliPos, CrystalnexusModBlocks.DEPOT_CLI.get());
        DepotCliBlockEntity disconnectedCli = helper.getBlockEntity(cliPos);
        disconnectedCli.setOwner(player.getUUID());
        helper.setBlock(cablePos, Blocks.AIR);
        helper.assertTrue(!downloaderMenu.canAccessDepot(player),
                "A downloader must stop working when disconnected from the depot network");
        DepotCliMenu disconnectedMenu = new DepotCliMenu(2, player.getInventory(), absoluteCliPos);
        DepotCliCommandContext disconnectedContext = new DepotCliCommandContext(player, disconnectedMenu, playerDepot);
        DepotCliCommandRegistry.INSTANCE.execute(disconnectedContext, "take minecraft:stone 1");
        helper.assertTrue(!disconnectedMenu.isConnected(player) && playerDepot.getCount(stone) == 64,
                "A disconnected CLI must fail without changing storage");
        helper.succeed();
    }

    private static void finishCraft(DepotSavedData depot, int processors) {
        int guard = 100_000;
        while (depot.getCraftingJob() != null && guard-- > 0) depot.advanceCraftingJob(processors);
        if (depot.getCraftingJob() != null) throw new IllegalStateException("Crafting job did not complete");
    }

    private static void runFurnaceJob(GameTestHelper helper, ServerPlayer player, DepotSavedData depot,
            BlockPos relativePos, FurnaceBlockEntity furnace) {
        int stepIndex = depot.getCraftingJob().currentStepIndex();
        DepotProcessingService.tick(player, depot);
        for (int i = 0; i < 220; i++) {
            AbstractFurnaceBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(relativePos),
                    helper.getBlockState(relativePos), furnace);
        }
        ItemStack producedBeforePolling = furnace.getItem(2).copy();
        for (int i = 0; i < 10 && depot.getCraftingJob() != null
                && depot.getCraftingJob().currentStepIndex() == stepIndex; i++) DepotProcessingService.tick(player, depot);
        if (depot.getCraftingJob() != null && depot.getCraftingJob().currentStepIndex() == stepIndex) {
            throw new IllegalStateException("Furnace processing step did not complete: input=" + furnace.getItem(0)
                    + ", fuel=" + furnace.getItem(1) + ", output=" + furnace.getItem(2)
                    + ", producedBeforePolling=" + producedBeforePolling + ", task=" + depot.getProcessingTask()
                    + ", working=" + depot.getCraftingJob().workingItems());
        }
    }
}
