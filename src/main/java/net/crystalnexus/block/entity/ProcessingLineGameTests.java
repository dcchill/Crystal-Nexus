package net.crystalnexus.block.entity;

import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_processing")
@PrefixGameTestTemplate(false)
public final class ProcessingLineGameTests {
    private ProcessingLineGameTests() {}

    @GameTest(template = "zero_point")
    public static void orderedProcessingSurvivesSaveAndBeltTransfer(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pressPos = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos engraverPos = pressPos.east();
        var state = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING, net.minecraft.core.Direction.EAST);
        level.setBlockAndUpdate(pressPos, state);
        level.setBlockAndUpdate(engraverPos, state);
        level.setBlockAndUpdate(pressPos.above(), CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get().defaultBlockState());
        level.setBlockAndUpdate(engraverPos.above(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get().defaultBlockState());

        var pressBelt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pressPos);
        var press = (ProcessingModuleBlockEntity) level.getBlockEntity(pressPos.above());
        press.saveWithFullMetadata(level.registryAccess());
        press.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get(), 1));
        press.energy().receiveEnergy(10_000, false);
        pressBelt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get()));
        tickModule(pressBelt, 10);
        helper.assertTrue(press.data().get(0) == 10 && press.energyStored() == 800,
            "A powered press must advance over time and spend 20 FE per tick");
        helper.assertTrue(press.getItem(0).getCount() == 1, "The press must hold silicon until the timed step finishes");
        var persisted = pressBelt.saveWithFullMetadata(level.registryAccess());
        pressBelt.loadWithComponents(persisted, level.registryAccess());

        tickModule(pressBelt, 10);
        ItemStack inProgress = find(pressBelt, CrystalnexusModItems.BLANK_CHIP.get());
        helper.assertTrue(!inProgress.isEmpty(), "Press must retain the in-progress item on the belt");
        helper.assertTrue(press.getItem(0).isEmpty(), "The press must consume its supplied material");
        helper.runAfterDelay(20, () -> {
            var engraverBelt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(engraverPos);
            helper.assertTrue(pressBelt.isEmpty() && find(engraverBelt, CrystalnexusModItems.BLANK_CHIP.get()).getCount() == 1,
                "The pressed circuit must move to the engraver without loss or duplication");
            var engraver = (ProcessingModuleBlockEntity) level.getBlockEntity(engraverPos.above());
            engraver.energy().receiveEnergy(10_000, false);
            tickModule(engraverBelt, 20);
            helper.assertTrue(find(engraverBelt, CrystalnexusModItems.CARBON_CHIP.get()).getCount() == 1,
                "The engraver must finish the persisted press-then-engrave recipe after transfer");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void outOfOrderModuleLeavesInputUnchanged(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get()));
        belt.processProcessingModules();
        helper.assertTrue(find(belt, CrystalnexusModItems.BLANK_CHIP.get()).getCount() == 1,
            "An engraver before the required press must leave the input unchanged");
        helper.succeed();
    }

    private static ItemStack find(ConveyerBeltBaseBlockEntity belt, net.minecraft.world.item.Item item) {
        for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++)
            if (belt.getItem(i).is(item)) return belt.getItem(i);
        return ItemStack.EMPTY;
    }

    @GameTest(template = "zero_point")
    public static void emptyBeltDoesNotExtractPressMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get().defaultBlockState());
        var press = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        press.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get(), 2));
        helper.runAfterDelay(10, () -> {
            var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(belt.isEmpty() && press.getItem(0).getCount() == 2,
                "Conveyor inventory pulling must leave the press's held material in the press");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void extractionClearsOnlyProcessingData(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        for (var block : new net.minecraft.world.level.block.Block[] { CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get() }) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            belt.setItem(3, taggedCircuit(3));
            assertClean(helper, belt.removeItem(3, 1));
            helper.assertTrue(belt.getItem(3).getCount() == 2 && hasProgress(belt.getItem(3)),
                "Taking one item must preserve progress on the remaining belt items");
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                pos, net.minecraft.core.Direction.DOWN);
            assertClean(helper, handler.extractItem(3, 1, true));
            helper.assertTrue(belt.getItem(3).getCount() == 2 && hasProgress(belt.getItem(3)),
                "Simulating extraction must not change the belt item");
            assertClean(helper, handler.extractItem(3, 1, false));
            assertClean(helper, belt.removeItemNoUpdate(3));
            helper.assertTrue(belt.isEmpty(), "Extraction must preserve item counts");
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void brokenBeltDropsCleanItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setItem(3, taggedCircuit(2));
        level.destroyBlock(pos, false);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(1), entity -> entity.getItem().is(CrystalnexusModItems.BLANK_CHIP.get()));
        helper.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 2,
            "Breaking the belt must drop all carried items once");
        drops.forEach(entity -> assertClean(helper, entity.getItem()));
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void partialInventoryInsertionClearsOnlyOutgoingItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.east(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING, net.minecraft.core.Direction.EAST));
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setManualMode(true);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos.east());
        for (int i = 0; i < chest.getContainerSize(); i++)
            chest.setItem(i, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
        var stored = taggedCircuit(63);
        ConveyerBeltBaseBlockEntity.clearProcessingData(stored);
        chest.setItem(0, stored);
        belt.setItem(3, taggedCircuit(2));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(chest.getItem(0).getCount() == 64, "Outgoing items must stack into clean inventory items");
            assertClean(helper, chest.getItem(0));
            helper.assertTrue(belt.getItem(3).getCount() == 1 && hasProgress(belt.getItem(3)),
                "A rejected remainder must retain its recipe progress on the conveyor");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void playerPickupClearsProcessingData(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setItem(3, taggedCircuit(2));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
            net.minecraft.core.Direction.UP, pos, false);
        ((net.crystalnexus.block.ConveyerBeltBlock) state.getBlock()).useWithoutItem(state, level, pos, player, hit);
        assertClean(helper, player.getInventory().getItem(0));
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 1
                && belt.getItem(3).getCount() == 1 && hasProgress(belt.getItem(3)),
            "Picking up one circuit must preserve the other circuit's progress");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void beltEjectsCleanItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setItem(3, taggedCircuit(2));
        helper.runAfterDelay(10, () -> {
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3), entity -> entity.getItem().is(CrystalnexusModItems.BLANK_CHIP.get()));
            helper.assertTrue(belt.isEmpty() && drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 2,
                "Ejection must preserve the total item count");
            drops.forEach(entity -> assertClean(helper, entity.getItem()));
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point", timeoutTicks = 140)
    public static void fastBeltsHoldItemsForProcessingAnimations(GameTestHelper helper) {
        var level = helper.getLevel();
        var belts = new ConveyerBeltBaseBlockEntity[4];
        var modules = new ProcessingModuleBlockEntity[4];
        var seenWorking = new boolean[4];
        for (int i = 0; i < 4; i++) {
            var pos = helper.absolutePos(new BlockPos(2 + (i / 2) * 3, 3, 2 + (i % 2) * 3));
            level.setBlockAndUpdate(pos, CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get().defaultBlockState());
            level.setBlockAndUpdate(pos.above(), (switch (i) {
                case 0 -> CrystalnexusModBlocks.PROCESSING_PRESS_MODULE;
                case 1 -> CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE;
                case 2 -> CrystalnexusModBlocks.PROCESSING_DRILL_MODULE;
                default -> CrystalnexusModBlocks.NANOFABRICATOR;
            }).get().defaultBlockState());
            belts[i] = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            modules[i] = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
            modules[i].energy().receiveEnergy(i == 3 ? 320 : 40, false);
            var input = new ItemStack(i == 3 ? CrystalnexusModItems.SILICON_WAFER.get()
                : i == 2 ? CrystalnexusModItems.IRON_SHEET.get() : CrystalnexusModItems.BLANK_CHIP.get());
            if (i == 0) modules[i].setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get()));
            else if (i == 1) {
                var data = new net.minecraft.nbt.CompoundTag();
                data.putString("CrystalNexusProcessingRecipe", "crystalnexus:processed_carbon_chip");
                data.putInt("CrystalNexusProcessingStep", 1);
                input.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
            }
            belts[i].setItem(0, input);
        }
        for (int tick = 1; tick <= 105; tick++) {
            final int elapsed = tick;
            helper.runAfterDelay(tick, () -> {
                for (int i = 0; i < 4; i++) {
                    if (modules[i].isWorking()) {
                        seenWorking[i] = true;
                        helper.assertTrue(belts[i].isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT)
                                && belts[i].getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).getCount() == 1
                                && !belts[i].canAdvanceForRender(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
                            "The item must stay beneath its active processing module throughout the animation");
                        helper.assertTrue(modules[i].getUpdateTag(level.registryAccess()).getBoolean("working"),
                            "The client must receive the crafting animation state");
                    }
                    if (elapsed == 5) helper.assertTrue(!modules[i].isWorking()
                            && belts[i].isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
                        "Power loss must pause the animation while keeping the item under the module");
                    if (elapsed == 6) modules[i].energy().receiveEnergy(i == 3 ? 16_000 : 1_000, false);
                    if (elapsed == 105) helper.assertTrue(seenWorking[i] && !modules[i].isWorking() && belts[i].isEmpty(),
                        "The animation must finish and release its item after power returns");
                }
                if (elapsed == 105) helper.succeed();
            });
        }
    }

    @GameTest(template = "zero_point")
    public static void celestialWaferCompletesAllSixStepsWithDifferentSingularities(GameTestHelper helper) {
        var belts = waferLine(helper);
        var generated = net.crystalnexus.item.GeneratedSingularityItem.create(new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        helper.assertTrue(!generated.isEmpty(), "Generated singularity fixture must have a material");
        var energy = new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get());
        energy.setDamageValue(7);
        for (var singularity : java.util.List.of(new ItemStack(CrystalnexusModItems.IRON_SINGULARITY.get()), energy, generated)) {
            belts[0].clearContent();
            belts[1].clearContent();
            belts[0].setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.SILICON_WAFER.get()));
            ItemStack[] materials = {new ItemStack(CrystalnexusModItems.DARK_MATTER.get()),
                new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get()), singularity};
            for (int step = 0; step < 6; step++) {
                var belt = belts[step % 2];
                var module = (ProcessingModuleBlockEntity) helper.getLevel().getBlockEntity(belt.getBlockPos().above());
                if (step % 2 == 0) module.setItem(0, materials[step / 2].copy());
                module.energy().receiveEnergy(1_000, false);
                tickModule(belt, 20);
                var carried = belt.getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT);
                if (step % 2 == 0) helper.assertTrue(module.isEmpty(), "Each press must consume exactly its held ingredient");
                if (step < 5) {
                    helper.assertTrue(carried.is(CrystalnexusModItems.SILICON_WAFER.get()) && carried.getCount() == 1,
                        "Intermediate steps must keep the wafer on the belt without finishing early");
                    var saved = belt.saveWithFullMetadata(helper.getLevel().registryAccess());
                    belt.loadWithComponents(saved, helper.getLevel().registryAccess());
                    transferWafer(belt, ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, belts[(step + 1) % 2]);
                } else helper.assertTrue(carried.is(CrystalnexusModItems.CELESTIAL_SILICON_WAFER.get())
                        && carried.getCount() == 1 && !hasProgress(carried),
                    "Only the sixth operation must produce one finished celestial wafer");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void singularitiesProcessStackedWafersOneAtATime(GameTestHelper helper) {
        var belts = waferLine(helper);
        var press = (ProcessingModuleBlockEntity) helper.getLevel().getBlockEntity(belts[0].getBlockPos().above());
        var engraver = (ProcessingModuleBlockEntity) helper.getLevel().getBlockEntity(belts[1].getBlockPos().above());
        var wafers = new ItemStack(CrystalnexusModItems.SILICON_WAFER.get(), 2);
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("CrystalNexusProcessingRecipe", "crystalnexus:celestial_silicon_wafer");
        data.putInt("CrystalNexusProcessingStep", 4);
        wafers.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        belts[0].setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, wafers);
        press.setItem(0, new ItemStack(CrystalnexusModItems.IRON_SINGULARITY.get()));
        press.energy().receiveEnergy(1_000, false);
        belts[0].setItem(3, new ItemStack(net.minecraft.world.item.Items.STONE));
        tickModule(belts[0], 1);
        helper.assertTrue(press.energyStored() == 1_000 && !press.isEmpty(),
            "A blocked exit must not consume power or a singularity");
        belts[0].setItem(3, ItemStack.EMPTY);
        tickModule(belts[0], 10);
        var saved = belts[0].saveWithFullMetadata(helper.getLevel().registryAccess());
        belts[0].loadWithComponents(saved, helper.getLevel().registryAccess());
        tickModule(belts[0], 10);
        helper.assertTrue(press.isEmpty() && press.energyStored() == 600
                && belts[0].getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).getCount() == 1
                && belts[0].getItem(3).getCount() == 1,
            "One singularity must press one wafer, preserve its batch size through saving, and leave the remainder on the belt");
        transferWafer(belts[0], 3, belts[1]);
        engraver.energy().receiveEnergy(1_000, false);
        tickModule(belts[1], 20);
        var first = belts[1].removeItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, 1);
        press.setItem(0, new ItemStack(CrystalnexusModItems.ENERGY_SINGULARITY.get()));
        tickModule(belts[0], 20);
        helper.assertTrue(press.isEmpty() && press.energyStored() == 200, "A second wafer must consume a second singularity and its own power");
        transferWafer(belts[0], ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, belts[1]);
        tickModule(belts[1], 20);
        helper.assertTrue(first.is(CrystalnexusModItems.CELESTIAL_SILICON_WAFER.get()) && first.getCount() == 1
                && belts[1].getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).is(CrystalnexusModItems.CELESTIAL_SILICON_WAFER.get())
                && belts[1].getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).getCount() == 1 && belts[0].isEmpty(),
            "Two wafers and two singularities must yield exactly two finished wafers without losing the remainder");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void celestialMachineRecipesRequireCelestialWafers(GameTestHelper helper) {
        for (String id : java.util.List.of("comet_forge_controller", "gravitational_array_controller_recipe", "solar_simulator_controller_recipe")) {
            var recipe = helper.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("crystalnexus:" + id))
                .orElseThrow().value();
            helper.assertTrue(recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(new ItemStack(CrystalnexusModItems.CELESTIAL_SILICON_WAFER.get())))
                    && recipe.getIngredients().stream().noneMatch(ingredient -> ingredient.test(new ItemStack(CrystalnexusModItems.SILICON_WAFER.get()))),
                "Celestial machine recipes must require the new wafer: " + id);
        }
        helper.succeed();
    }

    private static ConveyerBeltBaseBlockEntity[] waferLine(GameTestHelper helper) {
        var belts = new ConveyerBeltBaseBlockEntity[2];
        for (int i = 0; i < 2; i++) {
            var pos = helper.absolutePos(new BlockPos(2 + i, 3, 2));
            helper.getLevel().setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pos.above(), (i == 0 ? CrystalnexusModBlocks.PROCESSING_PRESS_MODULE
                : CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE).get().defaultBlockState());
            belts[i] = (ConveyerBeltBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        }
        return belts;
    }

    private static void transferWafer(ConveyerBeltBaseBlockEntity source, int slot, ConveyerBeltBaseBlockEntity destination) {
        var moving = source.getItem(slot).copy();
        source.setItem(slot, ItemStack.EMPTY);
        destination.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, moving);
    }

    @GameTest(template = "zero_point")
    public static void precisionDrillCraftsAndItsRecipeSurvivesCodecAndWorldReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (net.crystalnexus.recipe.ProcessingLineRecipe) level.getRecipeManager()
            .byKey(net.minecraft.resources.ResourceLocation.parse("crystalnexus:processing_drilled_iron_bearing")).orElseThrow().value();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
        var codec = net.crystalnexus.recipe.ProcessingLineRecipe.Serializer.INSTANCE.codec().codec();
        var json = codec.encodeStart(ops, recipe).getOrThrow();
        helper.assertTrue(json.getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject()
                .get("module").getAsString().equals("drill")
                && codec.parse(ops, json).getOrThrow().steps().getFirst().operation() == net.crystalnexus.block.ProcessingModuleBlock.Operation.DRILL,
            "Drill steps must encode and decode as drill, not engraving");
        var invalid = json.deepCopy();
        invalid.getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject().add("ingredient",
            com.google.gson.JsonParser.parseString("{\"item\":\"minecraft:iron_ingot\"}"));
        helper.assertTrue(codec.parse(ops, invalid).error().isPresent(), "A drill must reject ingredients it has no slot to consume");

        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var engraver = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        engraver.energy().receiveEnergy(1_000, false);
        belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.IRON_SHEET.get()));
        tickModule(belt, 20);
        helper.assertTrue(find(belt, CrystalnexusModItems.IRON_SHEET.get()).getCount() == 1 && engraver.energyStored() == 1_000,
            "An engraver must not perform a drill recipe");

        var state = CrystalnexusModBlocks.PROCESSING_DRILL_MODULE.get().defaultBlockState();
        level.setBlockAndUpdate(pos.above(), state);
        var drill = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        helper.assertTrue(!drill.hasMaterialSlot() && new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state),
            "The drill must need no held material and be harvestable with a pickaxe");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new net.crystalnexus.world.inventory.ProcessingModuleMenu(1, player.getInventory(), drill, drill.data());
        helper.assertTrue(!menu.hasMaterialSlot() && menu.machineFallbackId().equals("processing_drill_module") && menu.beltItemX() == 80,
            "The drill must use its own GUI identity and the centered belt-item display");
        drill.energy().receiveEnergy(1_000, false);
        tickModule(belt, 10);
        helper.assertTrue(drill.isWorking() && drill.data().get(0) == 10 && drill.energyStored() == 800
                && find(belt, CrystalnexusModItems.IRON_SHEET.get()).getCount() == 1,
            "Drilling must hold the input for its timed operation and consume power");
        var savedBelt = belt.saveWithFullMetadata(level.registryAccess());
        var savedDrill = drill.saveWithFullMetadata(level.registryAccess());
        belt.loadWithComponents(savedBelt, level.registryAccess());
        drill.loadWithComponents(savedDrill, level.registryAccess());
        tickModule(belt, 10);
        var output = find(belt, CrystalnexusModItems.IRON_BEARING.get());
        helper.assertTrue(output.getCount() == 1 && !hasProgress(output) && drill.energyStored() == 600 && !drill.isWorking(),
            "A saved drill job must finish once with a clean output and the correct power cost");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void nanofabricatorSequencesPauseReloadAndSynchronize(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (net.crystalnexus.recipe.ProcessingLineRecipe) level.getRecipeManager()
            .byKey(net.minecraft.resources.ResourceLocation.parse("crystalnexus:nanofabricated_carbon_chip")).orElseThrow().value();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
        var serializer = net.crystalnexus.recipe.ProcessingLineRecipe.Serializer.INSTANCE;
        var json = serializer.codec().codec().encodeStart(ops, recipe).getOrThrow();
        var first = json.getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject();
        helper.assertTrue(first.get("module").getAsString().equals("nanofabricate"), "Nanofabrication needs its own operation name");
        first.remove("energy_per_tick");
        helper.assertTrue(serializer.codec().codec().parse(ops, json).getOrThrow().steps().getFirst().energyPerTick() == 160,
            "Nanofabrication must default to a higher FE draw while remaining recipe configurable");
        first.addProperty("energy_per_tick", 240);
        helper.assertTrue(serializer.codec().codec().parse(ops, json).getOrThrow().steps().getFirst().energyPerTick() == 240,
            "Data packs must be able to configure the FE cost");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            serializer.streamCodec().encode(buffer, recipe);
            var decoded = serializer.streamCodec().decode(buffer);
            helper.assertTrue(serializer.codec().codec().encodeStart(ops, decoded).getOrThrow()
                    .equals(serializer.codec().codec().encodeStart(ops, recipe).getOrThrow()),
                "All operation steps and timings must reach multiplayer clients unchanged");
        } finally { buffer.release(); }

        var belts = new ConveyerBeltBaseBlockEntity[3];
        var modules = new ProcessingModuleBlockEntity[3];
        var blocks = new net.minecraft.world.level.block.Block[] {CrystalnexusModBlocks.NANOFABRICATOR.get(),
            CrystalnexusModBlocks.PROCESSING_PRESS_MODULE.get(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get()};
        for (int i = 0; i < 3; i++) {
            var pos = helper.absolutePos(new BlockPos(2 + i, 3, 2));
            level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
            level.setBlockAndUpdate(pos.above(), blocks[i].defaultBlockState());
            belts[i] = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            modules[i] = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        }
        var nano = modules[0];
        var state = nano.getBlockState();
        helper.assertTrue(state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90)
                .getValue(net.crystalnexus.block.ProcessingModuleBlock.FACING) == net.minecraft.core.Direction.EAST
                && state.mirror(net.minecraft.world.level.block.Mirror.LEFT_RIGHT)
                .getValue(net.crystalnexus.block.ProcessingModuleBlock.FACING) == net.minecraft.core.Direction.SOUTH
                && state.canSurvive(level, nano.getBlockPos())
                && !state.canSurvive(level, nano.getBlockPos().above()),
            "The Nanofabricator must rotate, mirror, and require a conveyor underneath");
        helper.assertTrue(!nano.hasMaterialSlot() && nano.energy().getMaxEnergyStored() == 128_000
                && new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state),
            "The Nanofabricator must reuse shared energy/GUI handling and support pickaxe harvesting");
        var menu = new net.crystalnexus.world.inventory.ProcessingModuleMenu(1,
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), nano, nano.data());
        helper.assertTrue(menu.machineFallbackId().equals("nanofabricator") && menu.beltItemX() == 80,
            "Its shared GUI must identify the Nanofabricator and show the held belt item");

        nano.energy().receiveEnergy(160, false);
        belts[0].setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(net.minecraft.world.item.Items.DIRT));
        tickModule(belts[0], 5);
        helper.assertTrue(nano.energyStored() == 160 && !nano.isWorking(), "Invalid items must not use FE or animate");
        belts[0].setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.SILICON_WAFER.get()));
        tickModule(belts[0], 1);
        helper.assertTrue(nano.isWorking() && nano.data().get(0) == 1 && nano.energyStored() == 0,
            "Nanofabrication must spend its configured FE per actual processing tick");
        tickModule(belts[0], 5);
        helper.assertTrue(!nano.isWorking() && nano.data().get(0) == 1
                && belts[0].isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
            "Power loss must stop effects and hold the partially processed wafer without advancing");
        nano.energy().receiveEnergy(16_000, false);
        tickModule(belts[0], 39);
        var snapshot = nano.getUpdateTag(level.registryAccess());
        var savedBelt = belts[0].saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(snapshot.getBoolean("working") && snapshot.getInt("progress") == 40,
            "Multiplayer animation snapshots must carry actual activity, duration, and progress");
        var restored = new ProcessingModuleBlockEntity(nano.getBlockPos(), state);
        restored.loadWithComponents(snapshot, level.registryAccess());
        helper.assertTrue(restored.isWorking() && restored.data().get(0) == 40 && restored.data().get(1) == 80,
            "Clients must reconstruct the same animation stage from the server snapshot");
        nano.loadWithComponents(snapshot, level.registryAccess());
        belts[0].loadWithComponents(savedBelt, level.registryAccess());
        tickModule(belts[0], 40);
        helper.assertTrue(!nano.isWorking() && find(belts[0], CrystalnexusModItems.SILICON_WAFER.get()).getCount() == 1
                && nano.energyStored() == 3_360, "Nanofabrication must resume a saved cycle exactly once");
        transferWafer(belts[0], ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, belts[2]);
        modules[2].energy().receiveEnergy(1_000, false);
        tickModule(belts[2], 20);
        helper.assertTrue(modules[2].energyStored() == 1_000 && find(belts[2], CrystalnexusModItems.CARBON_CHIP.get()).isEmpty(),
            "An engraver must not skip the required carbon-fiber press step");
        transferWafer(belts[2], ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, belts[1]);
        modules[1].setItem(0, new ItemStack(CrystalnexusModItems.CARBON_FIBER.get()));
        modules[1].energy().receiveEnergy(1_000, false);
        tickModule(belts[1], 20);
        transferWafer(belts[1], ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, belts[2]);
        tickModule(belts[2], 20);
        ItemStack output = find(belts[2], CrystalnexusModItems.CARBON_CHIP.get());
        helper.assertTrue(output.getCount() == 1 && !hasProgress(output) && belts[0].isEmpty() && belts[1].isEmpty()
                && modules[1].material().isEmpty(), "The existing ordered sequence must complete with one clean output");
        belts[0].setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.SILICON_WAFER.get(), 64));
        nano.energy().receiveEnergy(16_000, false);
        tickModule(belts[0], 1);
        helper.assertTrue(nano.isWorking() && nano.energyStored() == 9_120,
            "The Hyper energy buffer must support the per-tick power draw of a full wafer stack");
        var removed = belts[0].removeItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, 64);
        tickModule(belts[0], 1);
        helper.assertTrue(removed.getCount() == 64 && !hasProgress(removed) && !nano.isWorking(),
            "Taking a processing wafer stack must clear its recipe data and stop the machine");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void welderRequiresMaterialAndResumesItsSavedSequence(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_WELDER_MODULE.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var welder = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        var menu = new net.crystalnexus.world.inventory.ProcessingModuleMenu(1,
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), welder, welder.data());
        helper.assertTrue(welder.hasMaterialSlot() && menu.hasMaterialSlot() && menu.beltItemX() == 116
                && menu.machineFallbackId().equals("processing_welder_module"),
            "The welder must use the press-style material slot and belt item display");
        helper.assertTrue(welder.energy().getMaxEnergyStored() == ProcessingModuleBlockEntity.ENERGY_CAPACITY
                && new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE).isCorrectToolForDrops(welder.getBlockState()),
            "The welder must use standard module power and pickaxe harvesting");
        welder.energy().receiveEnergy(200, false);
        belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get()));
        tickModule(belt, 5);
        helper.assertTrue(!welder.isWorking() && welder.data().get(0) == 0 && welder.energyStored() == 200,
            "A welding step requiring material must not spend power or advance with an empty slot");
        welder.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIRT));
        tickModule(belt, 5);
        helper.assertTrue(!welder.isWorking() && welder.data().get(0) == 0 && welder.energyStored() == 200
                && welder.material().getCount() == 1, "Wrong welding material must remain untouched");
        welder.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get()));
        tickModule(belt, 10);
        helper.assertTrue(welder.isWorking() && welder.data().get(0) == 10 && welder.material().getCount() == 1,
            "The welder must retain its material and belt item during the timed operation");
        tickModule(belt, 3);
        helper.assertTrue(!welder.isWorking() && welder.data().get(0) == 10
                && belt.isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
            "Power loss must stop welding effects and retain the item at the probe");
        welder.energy().receiveEnergy(1_000, false);
        tickModule(belt, 10);
        var savedBelt = belt.saveWithFullMetadata(level.registryAccess());
        var savedModule = welder.getUpdateTag(level.registryAccess());
        helper.assertTrue(savedModule.getBoolean("working") && savedModule.getInt("progress") == 20
                && savedModule.getInt("max_progress") == 40, "Animation snapshots must carry the actual welding stage");
        welder.loadWithComponents(savedModule, level.registryAccess());
        belt.loadWithComponents(savedBelt, level.registryAccess());
        tickModule(belt, 20);
        helper.assertTrue(welder.material().isEmpty() && !welder.isWorking() && welder.energyStored() == 400
                && find(belt, CrystalnexusModItems.BLANK_CHIP.get()).getCount() == 1,
            "A saved weld must finish once, consume one material, and spend exactly 800 FE");
        welder.energy().receiveEnergy(100, false);
        tickModule(belt, 10);
        helper.assertTrue(welder.energyStored() == 500 && !welder.isWorking(), "The welder must not execute the next engraver step");
        var next = pos.east();
        level.setBlockAndUpdate(next, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(next.above(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get().defaultBlockState());
        var engraverBelt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(next);
        var engraver = (ProcessingModuleBlockEntity) level.getBlockEntity(next.above());
        engraver.energy().receiveEnergy(1_000, false);
        transferWafer(belt, ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, engraverBelt);
        tickModule(engraverBelt, 20);
        var output = find(engraverBelt, CrystalnexusModItems.CARBON_CHIP.get());
        helper.assertTrue(output.getCount() == 1 && !hasProgress(output) && belt.isEmpty(),
            "The welding sequence must survive belt transfer and yield one clean carbon chip");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void weldingSupportsOptionalMaterialsAndFullStacks(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var id = net.minecraft.resources.ResourceLocation.parse("crystalnexus:welded_carbon_chip");
        var recipe = (net.crystalnexus.recipe.ProcessingLineRecipe) manager.byKey(id).orElseThrow().value();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
        var serializer = net.crystalnexus.recipe.ProcessingLineRecipe.Serializer.INSTANCE;
        var codec = serializer.codec().codec();
        var json = codec.encodeStart(ops, recipe).getOrThrow();
        helper.assertTrue(codec.parse(ops, json).getOrThrow().steps().getFirst().operation()
                == net.crystalnexus.block.ProcessingModuleBlock.Operation.WELD, "Weld steps must round-trip through JSON");
        for (String operation : new String[] {"press", "engrave", "drill", "nanofabricate"}) {
            var invalid = json.deepCopy();
            var step = invalid.getAsJsonObject().getAsJsonArray("steps").get(0).getAsJsonObject();
            step.addProperty("module", operation);
            if (operation.equals("press")) step.remove("ingredient");
            helper.assertTrue(codec.parse(ops, invalid).error().isPresent(),
                "Optional welding ingredients must not loosen other operations' ingredient validation");
        }
        var noMaterialJson = json.deepCopy();
        var steps = noMaterialJson.getAsJsonObject().getAsJsonArray("steps");
        steps.get(0).getAsJsonObject().remove("ingredient");
        steps.remove(1);
        var noMaterial = codec.parse(ops, noMaterialJson).getOrThrow();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            for (var candidate : java.util.List.of(recipe, noMaterial)) {
                serializer.streamCodec().encode(buffer, candidate);
                helper.assertTrue(codec.encodeStart(ops, serializer.streamCodec().decode(buffer)).getOrThrow()
                        .equals(codec.encodeStart(ops, candidate).getOrThrow()),
                    "Weld steps with and without materials must reach clients unchanged");
            }
        } finally { buffer.release(); }

        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_WELDER_MODULE.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var welder = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        var original = java.util.List.copyOf(manager.getRecipes());
        var replaced = new java.util.ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>(original);
        replaced.removeIf(holder -> holder.id().equals(id));
        replaced.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id, noMaterial));
        try {
            manager.replaceRecipes(replaced);
            for (int held : new int[] {0, 3}) {
                belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get()));
                welder.setItem(0, held == 0 ? ItemStack.EMPTY : new ItemStack(net.minecraft.world.item.Items.DIRT, held));
                welder.energy().receiveEnergy(1_000, false);
                tickModule(belt, 40);
                helper.assertTrue(find(belt, CrystalnexusModItems.CARBON_CHIP.get()).getCount() == 1
                        && welder.material().getCount() == held && !welder.isWorking(),
                    "Material-free welding must work with an empty slot or leave unrelated held items untouched");
                belt.clearContent();
            }
        } finally { manager.replaceRecipes(original); }
        belt.setItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get(), 64));
        welder.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get(), 63));
        welder.energy().receiveEnergy(1_000, false);
        welder.energy().receiveEnergy(1_000, false);
        int before = welder.energyStored();
        tickModule(belt, 1);
        helper.assertTrue(welder.energyStored() == before && !welder.isWorking(),
            "Stack welding must wait for a material per carried item without spending power");
        welder.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get(), 64));
        int received = 0;
        for (int tick = 0; tick < 40; tick++) {
            received += welder.energy().receiveEnergy(1_000, false);
            received += welder.energy().receiveEnergy(1_000, false);
            tickModule(belt, 1);
            if (tick < 39) helper.assertTrue(welder.material().getCount() == 64
                    && find(belt, CrystalnexusModItems.BLANK_CHIP.get()).getCount() == 64,
                "Full-stack welding must hold all items and materials until completion");
        }
        helper.assertTrue(welder.material().isEmpty() && welder.energyStored() == before + received - 40 * 20 * 64,
            "Full-stack welding must consume exactly 64 materials and scale power per carried item");
        var removed = belt.removeItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT, 64);
        tickModule(belt, 1);
        helper.assertTrue(removed.getCount() == 64 && !hasProgress(removed) && !welder.isWorking(),
            "Extracted welded circuits must lose processing data without item loss");
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 140)
    public static void fastWelderHoldsItemsThenTransfersThroughEngraverToInventory(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING, net.minecraft.core.Direction.EAST));
        level.setBlockAndUpdate(pos.above(), CrystalnexusModBlocks.PROCESSING_WELDER_MODULE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get().defaultBlockState()
            .setValue(net.crystalnexus.block.ConveyerBeltBlock.FACING, net.minecraft.core.Direction.EAST));
        level.setBlockAndUpdate(pos.east().above(), CrystalnexusModBlocks.PROCESSING_ENGRAVER_MODULE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(2), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var welder = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.above());
        var engraver = (ProcessingModuleBlockEntity) level.getBlockEntity(pos.east().above());
        welder.setItem(0, new ItemStack(CrystalnexusModItems.ENERGIZED_SILICON.get()));
        welder.energy().receiveEnergy(40, false);
        engraver.energy().receiveEnergy(1_000, false);
        belt.setItem(0, new ItemStack(CrystalnexusModItems.BLANK_CHIP.get()));
        for (int tick = 1; tick <= 110; tick++) {
            final int elapsed = tick;
            helper.runAfterDelay(tick, () -> {
                if (welder.isWorking()) helper.assertTrue(belt.isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT)
                        && belt.getItem(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT).getCount() == 1
                        && !belt.canAdvanceForRender(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
                    "A fast belt must keep the circuit at the welding probe during animation");
                if (elapsed == 5) helper.assertTrue(!welder.isWorking() && welder.material().getCount() == 1
                        && belt.isProcessingSegment(ConveyerBeltBaseBlockEntity.PROCESSING_SEGMENT),
                    "An unpowered welder must keep its material and centered circuit");
                if (elapsed == 6) welder.energy().receiveEnergy(1_000, false);
                if (elapsed == 110) {
                    var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(pos.east(2));
                    helper.assertTrue(chest.getItem(0).is(CrystalnexusModItems.CARBON_CHIP.get())
                            && chest.getItem(0).getCount() == 1 && !hasProgress(chest.getItem(0))
                            && belt.isEmpty() && welder.material().isEmpty() && !welder.isWorking(),
                        "A welded item must transfer to engraving and enter inventory once with clean data");
                    helper.succeed();
                }
            });
        }
    }

    private static ItemStack taggedCircuit(int count) {
        var stack = new ItemStack(CrystalnexusModItems.BLANK_CHIP.get(), count);
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("CrystalNexusProcessingRecipe", "crystalnexus:processed_carbon_chip");
        data.putInt("CrystalNexusProcessingStep", 1);
        data.putInt("CrystalNexusProcessingTime", 5);
        data.putBoolean("CrystalNexusProcessingActive", true);
        data.putLong("CrystalNexusProcessingLastTick", 1);
        data.putInt("CrystalNexusProcessingCount", 1);
        data.putString("unrelated", "preserved");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        return stack;
    }

    private static boolean hasProgress(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getAllKeys().stream()
            .anyMatch(key -> key.startsWith("CrystalNexusProcessing"));
    }

    private static void assertClean(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(!stack.isEmpty() && !hasProgress(stack), "Extracted items must have no processing data");
        helper.assertTrue(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("unrelated").equals("preserved"),
            "Removing processing data must preserve unrelated item data");
    }

    private static void tickModule(ConveyerBeltBaseBlockEntity belt, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            for (int i = 0; i < ConveyerBeltBaseBlockEntity.SEGMENTS; i++) {
                ItemStack stack = belt.getItem(i);
                if (stack.isEmpty()) continue;
                var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                data.remove("CrystalNexusProcessingLastTick");
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(data));
            }
            belt.processProcessingModules();
        }
    }
}
