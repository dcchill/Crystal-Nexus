package net.crystalnexus.gametest;

import net.crystalnexus.CrystalnexusMod;
import net.crystalnexus.block.ConveyerBeltBlock;
import net.crystalnexus.block.ConveyorGeometry;
import net.crystalnexus.block.ConveyerBeltMode;
import net.crystalnexus.util.ConveyorPlacement;
import net.crystalnexus.block.entity.ConveyerBeltBaseBlockEntity;
import net.crystalnexus.init.CrystalnexusModBlocks;
import net.crystalnexus.procedures.ConveyerBeltNeighbourBlockChangesProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_conveyors")
@PrefixGameTestTemplate(false)
public final class ConveyerBeltGameTests {
    private ConveyerBeltGameTests() {}

    @GameTest(template = "zero_point")
    public static void tagFiltersMatchPersistAndValidateMenuActions(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var planks = net.minecraft.tags.ItemTags.PLANKS.location();
        belt.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        belt.setFilter(1, new ItemStack(Items.DIAMOND));
        helper.assertTrue(!belt.matchesFilter(new ItemStack(Items.BIRCH_PLANKS)), "Exact filters must remain exact");
        helper.assertTrue(belt.setFilterTag(0, planks), "Sample item's tags must be selectable");
        helper.assertTrue(belt.matchesFilter(new ItemStack(Items.BIRCH_PLANKS)) && belt.matchesFilter(new ItemStack(Items.DIAMOND))
            && !belt.matchesFilter(new ItemStack(Items.EMERALD)), "Tag and exact filters must match with OR semantics");
        belt.setFilterBlacklist(true);
        helper.assertTrue(!belt.matchesFilter(new ItemStack(Items.BIRCH_PLANKS)) && !belt.matchesFilter(new ItemStack(Items.DIAMOND))
            && belt.matchesFilter(new ItemStack(Items.EMERALD)), "Blacklist must invert item and tag matches");
        for (int mode = 0; mode < 3; mode++) {
            ConveyerBeltMode.cycle(level, pos);
            belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(planks.equals(belt.getFilterTag(0)), "Mode changes must preserve tag filters");
        }
        var saved = belt.saveWithFullMetadata(level.registryAccess());
        belt.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(planks.equals(belt.getFilterTag(0)), "Save/load must preserve tag filters");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        var menu = new net.crystalnexus.world.inventory.ConveyorFilterMenu(7, player.getInventory(), pos);
        player.containerMenu = menu;
        helper.assertTrue(planks.equals(menu.getFilterTag(0)), "Ghost slots must synchronize tag identity");
        helper.assertTrue(!menu.selectTag(player, 8, 0, null) && !menu.selectTag(player, 7, -1, planks)
            && !menu.selectTag(player, 7, 9, planks) && !menu.selectTag(player, 7, 2, planks)
            && !menu.selectTag(player, 7, 1, planks), "Stale IDs, invalid slots and nonmember tags must be rejected");
        helper.assertTrue(menu.selectTag(player, 7, 0, null) && belt.getFilterTag(0) == null,
            "Exact item selection must clear the tag");
        helper.assertTrue(menu.selectTag(player, 7, 0, planks), "Valid menu actions must set the tag");
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        helper.assertTrue(!menu.selectTag(player, 7, 0, null), "Distant tag changes must be rejected");
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!menu.selectTag(player, 7, 0, null), "Closed menu actions must be rejected");
        var unknown = net.minecraft.resources.ResourceLocation.parse("crystalnexus:missing_filter_test_tag");
        saved.getCompound("InventoryFilterTags").putString("s0", unknown.toString());
        belt.loadWithComponents(saved, level.registryAccess());
        belt.setFilterBlacklist(false);
        helper.assertTrue(unknown.equals(belt.getFilterTag(0)) && !belt.matchesFilter(new ItemStack(Items.OAK_PLANKS)),
            "Missing tags must remain configured and match nothing");
        saved.remove("InventoryFilterTags");
        belt.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(belt.getFilterTag(0) == null, "Old saves must load exact filters");
        belt.setFilterTag(0, planks);
        belt.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(belt.getFilterTag(0) == null, "Replacing a sample must clear its tag");
        belt.setFilterTag(0, planks);
        belt.setFilter(0, ItemStack.EMPTY);
        belt.setFilter(1, ItemStack.EMPTY);
        helper.assertTrue(belt.getFilterTag(0) == null && belt.matchesFilter(new ItemStack(Items.EMERALD)),
            "Clearing all slots must restore unrestricted transfers");
        for (var tag : new net.minecraft.resources.ResourceLocation[]{null, planks}) {
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
            try {
                var request = new net.crystalnexus.network.ConveyorFilterTagSelect(7, 0, tag);
                net.crystalnexus.network.ConveyorFilterTagSelect.STREAM_CODEC.encode(buffer, request);
                helper.assertTrue(request.equals(net.crystalnexus.network.ConveyorFilterTagSelect.STREAM_CODEC.decode(buffer)),
                    "Tag selection payload must round-trip exact and tag choices");
            } finally { buffer.release(); }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 80)
    public static void tagFiltersControlInventoryTransfers(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(pos.above(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        belt.setFilterTag(0, net.minecraft.tags.ItemTags.PLANKS.location());
        var source = (net.minecraft.world.Container) level.getBlockEntity(pos.above());
        var destination = (net.minecraft.world.Container) level.getBlockEntity(pos.east());
        source.setItem(0, new ItemStack(Items.EMERALD, 5));
        source.setItem(1, new ItemStack(Items.BIRCH_PLANKS, 7));
        helper.runAfterDelay(30, () -> {
            int delivered = 0;
            for (int slot = 0; slot < destination.getContainerSize(); slot++) delivered += destination.getItem(slot).getCount();
            helper.assertTrue(source.getItem(0).getCount() == 5 && source.getItem(1).isEmpty() && delivered == 7,
                "Tag filters must pull and push different items sharing the selected tag");
            belt.setItem(3, new ItemStack(Items.EMERALD, 4));
            helper.assertTrue(!belt.canAdvanceForRender(3), "Rejected items must not animate inventory advancement");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(belt.getItem(3).getCount() == 4, "Tag-rejected items must wait without dropping");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void outputBatchesFollowTierAndItemStackLimit(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        ConveyorPlacement.placing = true;
        try {
            for (int tier = 0; tier < 3; tier++) for (var item : new net.minecraft.world.item.Item[]{Items.DIAMOND, Items.EGG}) {
                level.removeBlock(pos, false);
                level.removeBlock(pos.west(), false);
                level.setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get().defaultBlockState()
                    .setValue(ConveyerBeltBlock.FACING, Direction.EAST)
                    .setValue(net.crystalnexus.block.ConveyerBeltOutputBlock.TIER, tier));
                var source = (net.minecraft.world.Container) level.getBlockEntity(pos.west());
                int stackLimit = new ItemStack(item).getMaxStackSize();
                for (int slot = 0; slot < 64 / stackLimit; slot++) source.setItem(slot, new ItemStack(item, stackLimit));
                var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
                var saved = belt.saveWithFullMetadata(level.registryAccess());
                saved.putLong("LastMove", level.getGameTime() - (tier == 0 ? 2 : 1));
                belt.loadWithComponents(saved, level.registryAccess());
                belt.serverTick();
                int expected = Math.min(16 << tier, stackLimit);
                int carried = 0;
                for (int slot = 0; slot < 4; slot++) carried += belt.getItem(slot).getCount();
                int remaining = 0;
                for (int slot = 0; slot < source.getContainerSize(); slot++) remaining += source.getItem(slot).getCount();
                helper.assertTrue(carried == expected && remaining == 64 - expected,
                    "Output must extract the tier batch while respecting the item's stack limit: tier " + tier + " " + item);
            }
        } finally { ConveyorPlacement.placing = false; }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void everyTierAndModeMovesTwiceAsFast(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var tiers = new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
            CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get()};
        ConveyorPlacement.placing = true;
        try {
            for (int tier = 0; tier < tiers.length; tier++) {
                for (Block block : new Block[]{tiers[tier], CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(),
                        CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()}) {
                    level.removeBlock(pos, false);
                    level.removeBlock(pos.east(), false);
                    var state = block.defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
                    if (state.hasProperty(net.crystalnexus.block.ConveyerBeltInputBlock.TIER))
                        state = state.setValue(net.crystalnexus.block.ConveyerBeltInputBlock.TIER, tier);
                    level.setBlockAndUpdate(pos, state);
                    level.setBlockAndUpdate(pos.east(), tiers[tier].defaultBlockState()
                        .setValue(ConveyerBeltBlock.FACING, Direction.EAST));
                    var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
                    var next = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos.east());
                    belt.setItem(0, new ItemStack(Items.DIAMOND, 7));
                    var saved = belt.saveWithFullMetadata(level.registryAccess());
                    // Old saves used integer timestamps; they must still load correctly.
                    saved.putLong("LastMove", level.getGameTime() - (tier == 0 ? 2 : 1));
                    saved.putLong("IncomingHeadTime", level.getGameTime() - (tier == 0 ? 2 : 1));
                    belt.loadWithComponents(saved, level.registryAccess());
                    belt.serverTick();
                    var destination = belt.getItem(tier == 2 ? 2 : 1);
                    helper.assertTrue(destination.is(Items.DIAMOND) && destination.getCount() == 7,
                        "Movement must take 2, 1 or 0.5 ticks per segment for tier " + tier + " and " + block);
                    belt.serverTick();
                    helper.assertTrue(destination.getCount() == 7,
                        "Repeating a tick must not duplicate or lose items");
                    int total = 0;
                    for (int i = 0; i < 4; i++) total += belt.getItem(i).getCount() + next.getItem(i).getCount();
                    helper.assertTrue(total == 7, "Sub-tick movement must conserve every item");
                }
            }
        } finally { ConveyorPlacement.placing = false; }
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 100)
    public static void entitiesEnterAndClimbRampFromFlatBelt(GameTestHelper helper) {
        var level = helper.getLevel();
        var start = helper.absolutePos(new BlockPos(1, 3, 1));
        var passengers = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        var flat = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        ConveyorPlacement.placing = true;
        try {
            for (int track = 0; track < 2; track++) {
                var base = start.south(track * 3);
                var positions = new java.util.ArrayList<BlockPos>();
                for (int i = 0; i < 8; i++) {
                    var pos = base.east(i).above(i >= 2 ? 1 : 0);
                    positions.add(pos);
                    for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(pos.above(dy), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pos, flat.setValue(ConveyorGeometry.SLOPE,
                        i == 1 ? ConveyorGeometry.Slope.UP : ConveyorGeometry.Slope.FLAT));
                }
                for (int i = 0; i < positions.size(); i++) {
                    var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(positions.get(i));
                    belt.setSplineConnections(i == 0 ? null : positions.get(i - 1),
                        i + 1 == positions.size() ? null : positions.get(i + 1));
                }
                net.minecraft.world.entity.Entity entity;
                if (track == 0) {
                    var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                    pig.setNoAi(true);
                    entity = pig;
                } else {
                    entity = new net.minecraft.world.entity.decoration.ArmorStand(level, 0, 0, 0);
                }
                entity.setPos(base.getX() + .5, base.getY() + .5, base.getZ() + .5);
                level.addFreshEntity(entity);
                passengers.add(entity);
            }
        } finally { ConveyorPlacement.placing = false; }
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var rampPos = start.east();
        var ramp = level.getBlockState(rampPos);
        player.setPos(rampPos.getX() + .5, rampPos.getY() + 1, rampPos.getZ() + .5);
        double support = ConveyorGeometry.supportHeight(ramp, player.getBoundingBox().move(-rampPos.getX(), -rampPos.getY(), -rampPos.getZ()));
        player.setPos(player.getX(), rampPos.getY() + support, player.getZ());
        level.addFreshEntity(player);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(rampPos)).serverTick();
        helper.assertTrue(player.getDeltaMovement().x > 0 && player.getDeltaMovement().y > .08,
            "Uphill player velocity must lift against gravity");
        player.discard();
        helper.runAfterDelay(45, () -> {
            for (var entity : passengers) {
                helper.assertTrue(entity.getX() > start.getX() + 2.3 && entity.getY() >= start.getY() + 1.45,
                    "Entity must travel from flat belt to the raised landing: " + entity.getType() + " at " + entity.position());
                entity.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void nonItemEntitiesFollowEveryBeltSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) for (var slope : ConveyorGeometry.Slope.values())
                    for (var curve : ConveyerBeltBlock.CurveShape.values()) {
                if (slope != ConveyorGeometry.Slope.FLAT && curve != ConveyerBeltBlock.CurveShape.STRAIGHT) continue;
                var state = block.defaultBlockState().setValue(ConveyerBeltBlock.FACING, facing)
                    .setValue(ConveyorGeometry.SLOPE, slope).setValue(ConveyerBeltBlock.CURVE, curve);
                level.setBlockAndUpdate(pos, state);
                var surface = ConveyorGeometry.point(state, .5).add(pos.getX(), pos.getY(), pos.getZ());
                var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, surface.x, surface.y, surface.z);
                double support = ConveyorGeometry.supportHeight(state, stand.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()));
                stand.setPos(surface.x, pos.getY() + support, surface.z);
                double startY = stand.getY();
                stand.setNoGravity(true);
                level.addFreshEntity(stand);
                var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
                belt.serverTick();
                double t = ConveyorGeometry.progress(state, stand.getX() - pos.getX(), stand.getZ() - pos.getZ());
                helper.assertTrue(t > .5, "Non-item entities must move along " + block + " " + facing + " " + slope + " " + curve);
                if (slope == ConveyorGeometry.Slope.UP) helper.assertTrue(stand.getY() > startY, "Entities must climb ramps");
                if (slope == ConveyorGeometry.Slope.DOWN) helper.assertTrue(stand.getY() < startY, "Entities must descend ramps");
                var moved = stand.position();
                belt.serverTick();
                helper.assertTrue(stand.position().equals(moved), "Overlapping belt ticks must not move an entity twice");
                stand.discard();
            }
        }
        var flat = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, flat);
        var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 3, 2));
        pig.setNoAi(true);
        pig.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos)).serverTick();
        helper.assertTrue(pig.getX() > pos.getX() + .5, "Conveyors must also carry living mobs");
        pig.discard();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        level.addFreshEntity(player);
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().x > 0 && player.hurtMarked,
            "Player belt velocity must be synchronized to the client");
        player.getPersistentData().remove("crystalnexusConveyorMoveTick");
        player.setDeltaMovement(0, .42, 0);
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().y == .42 && player.getDeltaMovement().x == 0,
            "Jumping players must be able to leave the conveyor");
        player.setDeltaMovement(0, 0, 0);
        player.getAbilities().flying = true;
        belt.serverTick();
        helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0, "Flying players must not be pulled onto conveyors");
        player.discard();
        var ramp = flat.setValue(ConveyorGeometry.SLOPE, ConveyorGeometry.Slope.UP);
        level.setBlockAndUpdate(pos, ramp);
        level.setBlockAndUpdate(pos.east().above(), ramp);
        var crossing = new net.minecraft.world.entity.decoration.ArmorStand(level, pos.getX() + .95, pos.getY() + 1, pos.getZ() + .5);
        var bounds = crossing.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ());
        double support = Math.max(ConveyorGeometry.supportHeight(ramp, bounds),
            1 + ConveyorGeometry.supportHeight(ramp, bounds.move(-1, -1, 0)));
        crossing.setPos(crossing.getX(), pos.getY() + support, crossing.getZ());
        level.addFreshEntity(crossing);
        ((ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos)).serverTick();
        helper.assertTrue(crossing.getX() > pos.getX() + 1 && crossing.getY() > pos.getY() + support,
            "Wide entities must cross smoothly between consecutive ramps");
        crossing.discard();
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void collisionShapesAreReused(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get(),
                CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get(), CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get()}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                if (block instanceof ConveyerBeltBlock) state = state.setValue(ConveyerBeltBlock.BLOCKSTATE, 2);
                var first = state.getShape(level, pos, CollisionContext.empty());
                long start = System.nanoTime();
                for (int query = 0; query < 200; query++) state.getShape(level, pos, CollisionContext.empty());
                CrystalnexusMod.LOGGER.info("Conveyor shape benchmark: {} {} / 200 queries: {} ms", block, facing,
                    (System.nanoTime() - start) / 1_000_000.0);
                helper.assertTrue(first == state.getShape(level, pos, CollisionContext.empty()),
                    "Repeated collision queries must reuse the prebuilt shape for " + block + " " + facing);
                if (block instanceof ConveyerBeltBlock) {
                    helper.assertTrue(first.max(Direction.Axis.Y) == 1.0, "Stacked belt supports must retain their full height");
                    var unstacked = state.setValue(ConveyerBeltBlock.BLOCKSTATE, 1);
                    helper.assertTrue(unstacked.getShape(level, pos, CollisionContext.empty()).max(Direction.Axis.Y) == 0.5,
                        "An uncovered belt must retain its low profile");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void breakingStackedBeltsPreservesNeighborsAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (Block block : new Block[]{CrystalnexusModBlocks.CONVEYER_BELT.get(),
                CrystalnexusModBlocks.TITANIUM_CONVEYER_BELT.get(), CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get()}) {
            for (int repeat = 0; repeat < 8; repeat++) {
                for (int height = 0; height < 6; height++) level.setBlockAndUpdate(base.above(height), block.defaultBlockState());
                BlockPos target = base.above(3);
                var broken = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(target);
                for (int slot = 0; slot < 4; slot++) broken.setItem(slot, new ItemStack(Items.DIAMOND, 16));
                var below = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(target.below());
                below.setItem(0, new ItemStack(Items.EMERALD, 3));
                helper.assertTrue(level.getBlockState(target.below()).getValue(ConveyerBeltBlock.BLOCKSTATE) == 2,
                    "The lower belt must have stacked supports before breaking");
                level.destroyBlock(target, false);
                helper.assertTrue(level.getBlockState(target).isAir()
                        && level.getBlockEntity(target.below()) == below && below.getItem(0).getCount() == 3,
                    "Breaking one belt must leave neighboring belts and their contents intact");
                helper.assertTrue(level.getBlockState(target.below()).getValue(ConveyerBeltBlock.BLOCKSTATE) == 1,
                    "Breaking the upper belt must remove the lower belt's supports");
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8));
                int diamonds = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
                helper.assertTrue(diamonds == 64, "Broken belt contents must drop exactly once");
                drops.forEach(ItemEntity::discard);
                for (int height = 0; height < 6; height++) level.removeBlock(base.above(height), false);
                level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(8)).forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void coverUpdatesOnlyAffectBelts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.BIOMATIC_SIMULATOR.get().defaultBlockState());
        var original = level.getBlockState(pos);
        ConveyerBeltNeighbourBlockChangesProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(level.getBlockState(pos) == original, "A stale conveyor callback must not change an unrelated machine");
        level.setBlockAndUpdate(pos, CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(level.getBlockState(pos).getValue(ConveyerBeltBlock.BLOCKSTATE) == 2, "Any cover must enable supports");
        level.removeBlock(pos.above(), false);
        helper.assertTrue(level.getBlockState(pos).getValue(ConveyerBeltBlock.BLOCKSTATE) == 1, "Removing the cover must remove supports");
        helper.succeed();
    }
    @GameTest(template = "zero_point", timeoutTicks = 220)
    public static void mixedTiersCarryItemsAroundCornerAndUpRamp(GameTestHelper helper) {
        var level=helper.getLevel();
        var start=helper.absolutePos(new BlockPos(1,3,1));
        var route=ConveyorGeometry.plan(start,start.offset(4,1,3),true);
        ConveyorPlacement.placing=true;
        try {
            for(int i=0;i<route.size();i++) {
                var segment=route.get(i);
                level.setBlockAndUpdate(segment.pos().above(),Blocks.AIR.defaultBlockState());
                var block=i%2==0?CrystalnexusModBlocks.CONVEYER_BELT.get():CrystalnexusModBlocks.METEORITE_CONVEYER_BELT.get();
                level.setBlockAndUpdate(segment.pos(),segment.state(block.defaultBlockState()));
            }
            for(int i=0;i<route.size();i++) {
                var belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(route.get(i).pos());
                belt.setSplineConnections(i==0?null:route.get(i-1).pos(),i+1==route.size()?null:route.get(i+1).pos());
            }
        } finally {ConveyorPlacement.placing=false;}
        var end=route.getLast();
        var chest=end.pos().relative(end.facing());
        level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        ((ConveyerBeltBaseBlockEntity)level.getBlockEntity(start)).setItem(0,new ItemStack(Items.DIAMOND,7));
        helper.runAfterDelay(180,()->{
            var inventory=(net.minecraft.world.Container)level.getBlockEntity(chest);
            int total=0;
            for(int i=0;i<inventory.getContainerSize();i++) if(inventory.getItem(i).is(Items.DIAMOND)) total+=inventory.getItem(i).getCount();
            helper.assertTrue(total==7,"Curves, ramps and tier transitions must deliver every item exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void geometryPortsSaveAndModeChangesPreserveRamp(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        var state=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING,Direction.EAST).setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.DOWN);
        level.setBlockAndUpdate(pos,state);
        var next=pos.east();level.setBlockAndUpdate(next,CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING,Direction.EAST));
        var belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
        belt.setSplineConnections(pos.west().above(),next);belt.setItem(2,new ItemStack(Items.EMERALD,9));
        var saved=belt.saveWithFullMetadata(level.registryAccess());
        belt.loadWithComponents(saved,level.registryAccess());
        helper.assertTrue(next.equals(belt.getSplineNextPos()),"Valid links must survive reload");
        for(int i=0;i<3;i++) {
            ConveyerBeltMode.cycle(level,pos);
            belt=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
            helper.assertTrue(belt.getItem(2).getCount()==9 && level.getBlockState(pos).getValue(ConveyorGeometry.SLOPE)==ConveyorGeometry.Slope.DOWN,
                "Mode changes must preserve slope and contents");
        }
        level.removeBlock(next,false);
        helper.assertTrue(belt.getSplineNextPos()==null,"Removed links must be ignored");
        var flat=state.setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.FLAT);
        helper.assertTrue(!ConveyorGeometry.connects(pos,flat,pos.north(),flat),"Parallel adjacent lanes must not connect sideways");
        helper.assertTrue(!ConveyorGeometry.connects(pos,flat,pos.east(),flat.setValue(ConveyerBeltBlock.FACING,Direction.WEST)),"Opposing belts must not connect");
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 100)
    public static void downhillTransferWaitsForJamAndDoesNotLeakToParallelLane(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        var flat=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING,Direction.EAST);
        level.setBlockAndUpdate(pos,flat.setValue(ConveyorGeometry.SLOPE,ConveyorGeometry.Slope.DOWN));
        level.setBlockAndUpdate(pos.east(),flat);
        level.setBlockAndUpdate(pos.east(2),flat.setValue(ConveyerBeltBlock.FACING,Direction.WEST));
        level.setBlockAndUpdate(pos.north(),flat);
        var source=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos);
        var jam=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos.east());
        for(int slot=0;slot<4;slot++) jam.setItem(slot,new ItemStack(Items.EMERALD,1));
        source.setItem(3,new ItemStack(Items.DIAMOND,5));
        helper.runAfterDelay(12,()->{
            helper.assertTrue(source.getItem(3).getCount()==5,"A jam must hold the upstream tail");
            var parallel=(ConveyerBeltBaseBlockEntity)level.getBlockEntity(pos.north());
            helper.assertTrue(parallel.isEmpty(),"Parallel belt must remain empty");
            jam.clearContent();
        });
        helper.runAfterDelay(32,()->{
            helper.assertTrue(source.isEmpty() && jam.getItem(3).is(Items.DIAMOND) && jam.getItem(3).getCount()==5,
                "Downhill transfer must resume after the jam clears");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point")
    public static void surfacePointsMeetAndKeepUniformArcSpacing(GameTestHelper helper) {
        var base=CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState();
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            for(var curve:ConveyerBeltBlock.CurveShape.values()) {
                var state=base.setValue(ConveyerBeltBlock.FACING,facing).setValue(ConveyerBeltBlock.CURVE,curve);
                var first=ConveyorGeometry.point(state,0);var last=ConveyorGeometry.point(state,1);
                var in=ConveyorGeometry.input(state);
                helper.assertTrue(first.distanceTo(new net.minecraft.world.phys.Vec3(.5+in.getStepX()*.5,.5,.5+in.getStepZ()*.5))<1e-8,
                    "Curve input must meet the adjacent block edge");
                helper.assertTrue(last.distanceTo(new net.minecraft.world.phys.Vec3(.5+facing.getStepX()*.5,.5,.5+facing.getStepZ()*.5))<1e-8,
                    "Curve output must meet the adjacent block edge");
                double spacing=first.distanceTo(ConveyorGeometry.point(state,.25));
                for(int n=1;n<4;n++) helper.assertTrue(Math.abs(spacing-ConveyorGeometry.point(state,n*.25).distanceTo(ConveyorGeometry.point(state,(n+1)*.25)))<1e-8,
                    "Each transport slot must occupy the same arc length");
                for (double t : new double[]{0, .25, .5, .75, 1}) {
                    var point = ConveyorGeometry.point(state, t);
                    helper.assertTrue(Math.abs(t - ConveyorGeometry.progress(state, point.x, point.z)) < 1e-8,
                        "Entity progress must match the belt surface in every direction");
                }
            }
        }
        helper.succeed();
    }


    @GameTest(template = "zero_point")
    public static void filterMenuPersistsAcrossModesAndPickup(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        var menu = new net.crystalnexus.world.inventory.ConveyorFilterMenu(1, player.getInventory(), pos);
        var carried = new ItemStack(Items.DIAMOND, 17);
        carried.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Sample"));
        menu.setCarried(carried);
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.clicked(8, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(carried.getCount() == 17 && belt.getFilter(0).getCount() == 1
            && belt.matchesFilter(new ItemStack(Items.DIAMOND)) && !belt.matchesFilter(new ItemStack(Items.EMERALD)),
            "Ghost filters must copy item identity without consuming items");
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        helper.assertTrue(belt.getFilter(0).is(Items.DIAMOND), "Unsupported ghost clicks must not alter filters");
        menu.clicked(0, 1, net.minecraft.world.inventory.ClickType.PICKUP, player);
        helper.assertTrue(belt.getFilter(0).isEmpty() && belt.getFilter(8).is(Items.DIAMOND), "Right-click clears only one slot");
        helper.assertTrue(!menu.clickMenuButton(player, 99), "Invalid buttons must be rejected");
        for (int n = 0; n < 3; n++) {
            helper.assertTrue(menu.clickMenuButton(player, 0) && menu.stillValid(player), "Menu must survive filter mode changes");
            helper.assertTrue(level.getBlockState(pos).equals(state), "Filter button must not change block mode");
            helper.assertTrue(belt.isFilterBlacklist() == (n % 2 == 0)
                && belt.matchesFilter(new ItemStack(Items.DIAMOND)) == (n % 2 != 0)
                && belt.matchesFilter(new ItemStack(Items.EMERALD)) == (n % 2 == 0),
                "Whitelist accepts listed items; blacklist accepts unlisted items");
            belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(belt.getFilter(8).is(Items.DIAMOND), "Mode changes must preserve filters");
        }
        var saved = belt.saveWithFullMetadata(level.registryAccess());
        belt.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(belt.getFilter(8).is(Items.DIAMOND), "Save/load must preserve filters");
        helper.assertTrue(belt.isFilterBlacklist(), "Save/load must preserve blacklist mode");
        belt.setFilter(7, new ItemStack(Items.OAK_PLANKS));
        belt.setFilterTag(7, net.minecraft.tags.ItemTags.PLANKS.location());
        var wrench = new ItemStack(net.crystalnexus.init.CrystalnexusModItems.CRYSTAL_WRENCH.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wrench);
        player.setShiftKeyDown(true);
        wrench.getItem().onItemUseFirst(wrench, new net.minecraft.world.item.context.UseOnContext(player,
            net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        helper.assertTrue(!menu.stillValid(player), "Removed conveyor must invalidate menu");
        ItemStack packed = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
            .map(ItemEntity::getItem).filter(item -> item.is(state.getBlock().asItem())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!packed.isEmpty(), "Wrench pickup must return the conveyor");
        level.setBlockAndUpdate(pos, state);
        state.getBlock().setPlacedBy(level, pos, state, player, packed);
        belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(belt.isEmpty() && belt.getFilter(8).is(Items.DIAMOND) && belt.isFilterBlacklist(), "Empty filtered belt must survive pickup and placement");
        helper.assertTrue(net.minecraft.tags.ItemTags.PLANKS.location().equals(belt.getFilterTag(7)), "Tag filters must survive wrench pickup and placement");
        belt.loadWithComponents(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        helper.assertTrue(belt.matchesFilter(new ItemStack(Items.EMERALD)), "Older saves must allow all items");
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        helper.assertTrue(!menu.clickMenuButton(player, 0), "Distant menu actions must be rejected");
        helper.succeed();
    }

    @GameTest(template = "zero_point", timeoutTicks = 100)
    public static void filtersSelectInventoryPullsAndHoldRejectedPushes(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = CrystalnexusModBlocks.CONVEYER_BELT_INPUT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(pos.above(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
        var source = (net.minecraft.world.Container) level.getBlockEntity(pos.above());
        var destination = (net.minecraft.world.Container) level.getBlockEntity(pos.east());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setFilter(0, new ItemStack(Items.DIAMOND));
        belt.setFilter(8, new ItemStack(Items.GOLD_INGOT));
        source.setItem(0, new ItemStack(Items.EMERALD, 5));
        source.setItem(1, new ItemStack(Items.DIAMOND, 3));
        source.setItem(2, new ItemStack(Items.GOLD_INGOT, 2));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(source.getItem(0).getCount() == 5 && source.getItem(1).isEmpty() && source.getItem(2).isEmpty(),
                "Pulls must skip rejected inventory slots and accept every listed type");
            int delivered = 0;
            for (int slot = 0; slot < destination.getContainerSize(); slot++) delivered += destination.getItem(slot).getCount();
            helper.assertTrue(delivered == 5, "Allowed items must reach the inventory");
            belt.setItem(3, new ItemStack(Items.EMERALD, 4));
            helper.assertTrue(!belt.canAdvanceForRender(3), "Rejected push must not animate advancement");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(belt.getItem(3).getCount() == 4, "Rejected push must hold items without dropping");
            belt.setFilter(0, ItemStack.EMPTY);
            belt.setFilter(8, ItemStack.EMPTY);
            source.clearContent();
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(belt.isEmpty(), "Empty filters must restore unrestricted transfers");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point", timeoutTicks = 60)
    public static void filtersDoNotRestrictBeltTransfers(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState().setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(pos.east(), state);
        level.setBlockAndUpdate(pos.east(2), state.setValue(ConveyerBeltBlock.FACING, Direction.WEST));
        var source = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        var target = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos.east());
        source.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        target.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        source.setFilterTag(0, net.minecraft.tags.ItemTags.PLANKS.location());
        target.setFilterTag(0, net.minecraft.tags.ItemTags.PLANKS.location());
        source.setItem(3, new ItemStack(Items.DIAMOND, 7));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(source.isEmpty() && target.getItem(3).is(Items.DIAMOND) && target.getItem(3).getCount() == 7,
                "Filters must not restrict belt-to-belt transport");
            helper.succeed();
        });
    }

    @GameTest(template = "zero_point", timeoutTicks = 60)
    public static void filtersRestrictBehindInventoryPulls(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        var state = CrystalnexusModBlocks.CONVEYER_BELT_OUTPUT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), CrystalnexusModBlocks.CONVEYER_BELT.get().defaultBlockState()
            .setValue(ConveyerBeltBlock.FACING, Direction.WEST));
        var source = (net.minecraft.world.Container) level.getBlockEntity(pos.west());
        var belt = (ConveyerBeltBaseBlockEntity) level.getBlockEntity(pos);
        belt.setFilter(0, new ItemStack(Items.OAK_PLANKS));
        belt.setFilterTag(0, net.minecraft.tags.ItemTags.PLANKS.location());
        source.setItem(0, new ItemStack(Items.EMERALD, 5));
        source.setItem(1, new ItemStack(Items.BIRCH_PLANKS, 3));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(source.getItem(0).getCount() == 5 && source.getItem(1).isEmpty()
                && belt.getItem(3).is(Items.BIRCH_PLANKS) && belt.getItem(3).getCount() == 3,
                "Behind inventory pulls must skip rejected items and preserve every matching item");
            helper.succeed();
        });
    }
}
