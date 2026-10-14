package net.crystalnexus.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.crystalnexus.entity.BlackHoleProjectileEntity;
import net.crystalnexus.init.CrystalnexusModEntities;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.BatteryData;
import net.crystalnexus.item.BlackHoleRpgItem;
import net.crystalnexus.item.ToolEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("crystalnexus_rpg")
@PrefixGameTestTemplate(false)
public final class BlackHoleRpgGameTests {
    @GameTest(template = "zero_point")
    public static void sharedEnergyAndCharging(GameTestHelper helper) {
        ToolEnergyGameTests.toolsStoreEnergyAndBatteriesChargeInventory(helper);
    }

    @GameTest(template = "zero_point")
    public static void energyAndFiring(GameTestHelper helper) {
        ServerPlayer player = player(helper, 0);
        var item = CrystalnexusModItems.BLACK_HOLE_RPG.get();
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL, "Empty RPG cannot fire");
        BatteryData.setEnergy(stack, ToolEnergy.CAPACITY, ToolEnergy.CAPACITY);
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(BatteryData.getEnergy(stack) == 75_000 && player.getCooldowns().isOnCooldown(item), "One shot spends 25,000 FE and starts cooldown");
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(BatteryData.getEnergy(stack) == 75_000, "Cooldown prevents a second shot");
        for (int i = 0; i < BlackHoleRpgItem.COOLDOWN_TICKS; i++) player.getCooldowns().tick();
        helper.assertTrue(!player.getCooldowns().isOnCooldown(item), "Cooldown expires at 60 ticks");
        BatteryData.setEnergy(stack, 24_999, ToolEnergy.CAPACITY);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL
                && BatteryData.getEnergy(stack) == 24_999, "Insufficient charge does not partially drain FE");
        player.setGameMode(GameType.CREATIVE);
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(BatteryData.getEnergy(stack) == 24_999, "Creative firing is free");
        var shots = helper.getLevel().getEntitiesOfClass(BlackHoleProjectileEntity.class, player.getBoundingBox().inflate(5));
        helper.assertTrue(shots.isEmpty(), "Railgun fires instant beams without spawning black holes");
        shots.forEach(BlackHoleProjectileEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void flightHarvestAndProtection(GameTestHelper helper) {
        ServerPlayer player = player(helper, 160);
        var level = helper.getLevel();
        var shot = shot(player);
        BlockPos center = shot.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        shot.setDeltaMovement(1, 0, 0);
        Vec3 start = shot.position();
        shot.tick();
        long remaining = BlockPos.betweenClosedStream(center.offset(-2, -2, -2), center.offset(2, 2, 2))
                .filter(pos -> level.getBlockState(pos).is(Blocks.DIRT)).count();
        helper.assertTrue(remaining == 125 - 32 && !shot.isRemoved() && shot.position().equals(start.add(1, 0, 0)), "Flies through walls and breaks at most 32 blocks");
        helper.assertTrue(level.getBlockState(center.east()).isAir() && level.getBlockState(center.offset(-2, -2, -2)).is(Blocks.DIRT), "Nearest blocks are harvested first");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(8));
        helper.assertTrue(drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == 32, "Every destroyed dirt block drops exactly once");
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.BEDROCK, Blocks.CHEST, Blocks.WATER,
                net.crystalnexus.init.CrystalnexusModBlocks.COMET_FORGE_CONTROLLER.get()}) {
            level.setBlockAndUpdate(center, block.defaultBlockState());
            helper.assertTrue(!shot.breakBlock(center, true) && level.getBlockState(center).is(block), "Protected blocks survive suction");
        }
        level.setBlockAndUpdate(center, Blocks.DIRT.defaultBlockState());
        Consumer<BlockEvent.BreakEvent> cancel = event -> { if (event.getPos().equals(center)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { helper.assertTrue(!shot.breakBlock(center, true), "Cancelled breaks preserve blocks"); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        player.setGameMode(GameType.ADVENTURE);
        helper.assertTrue(!shot.breakBlock(center, true), "Adventure players cannot destroy terrain");
        player.setGameMode(GameType.SURVIVAL);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) level.removeBlock(pos, false);
        var mob = EntityType.ZOMBIE.create(level);
        mob.setNoAi(true);
        mob.setPos(shot.position().add(0, 0, 4));
        level.addFreshEntity(mob);
        shot.setDeltaMovement(Vec3.ZERO);
        shot.tick();
        helper.assertTrue(mob.getDeltaMovement().z < 0 && !shot.isRemoved(), "Nearby mobs are pulled toward the travelling hole");
        helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "Shooter is not pulled");
        mob.discard();
        shot.discard();
        drops.forEach(ItemEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void collisionExplosionAndSavedLifetime(GameTestHelper helper) {
        ServerPlayer player = player(helper, 320);
        var level = helper.getLevel();
        var shot = shot(player);
        BlockPos center = shot.blockPosition();
        level.setBlockAndUpdate(center.below(), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(center.north(), Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(center.north());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var drop = new ItemEntity(level, shot.getX(), shot.getY(), shot.getZ(), new ItemStack(Items.DIAMOND));
        level.addFreshEntity(drop);
        var mob = EntityType.ZOMBIE.create(level);
        mob.setNoAi(true);
        mob.setPos(shot.position().add(0.7, -0.3, 0));
        level.addFreshEntity(mob);
        int[] explosions = {0};
        Consumer<ExplosionEvent.Detonate> count = event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof BlackHoleProjectileEntity) explosions[0]++;
        };
        NeoForge.EVENT_BUS.addListener(count);
        try {
            shot.setDeltaMovement(1, 0, 0);
            shot.tick();
            shot.tick();
            helper.assertTrue(shot.isRemoved() && explosions[0] == 1, "Swept mob contact detonates exactly once");
            helper.assertTrue(mob.getHealth() < mob.getMaxHealth(), "Explosion damages the hit mob");
            helper.assertTrue(!drop.isRemoved(), "Existing loose items survive the explosion");
            helper.assertTrue(level.getBlockState(center.below()).isAir(), "Explosion destroys eligible terrain");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(8)).stream()
                    .anyMatch(item -> item.getItem().is(Items.DIRT)), "Explosion preserves terrain loot");
            helper.assertTrue(chest.getItem(0).getCount() == 3 && level.getBlockState(center.north()).is(Blocks.CHEST), "Explosion preserves containers and contents");
            mob.discard();
            var saved = shot(player);
            saved.setDeltaMovement(Vec3.ZERO);
            for (int i = 0; i < 119; i++) saved.tick();
            helper.assertTrue(!saved.isRemoved(), "Projectile lives for 119 ticks");
            CompoundTag tag = new CompoundTag();
            saved.saveWithoutId(tag);
            saved.discard();
            var restored = shot(player);
            restored.load(tag);
            restored.setOwner(player);
            restored.tick();
            helper.assertTrue(restored.isRemoved() && explosions[0] == 2, "Reload preserves remaining flight time");
            drop.discard();
        } finally { NeoForge.EVENT_BUS.unregister(count); }
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void beamDamagesAtLongRangeAndStopsAtWalls(GameTestHelper helper) {
        ServerPlayer player = player(helper, 640);
        var level = helper.getLevel();
        player.setYRot(0);
        player.setXRot(0);
        player.setGameMode(GameType.CREATIVE);
        for (int z = 0; z <= 112; z += 16) level.getChunkAt(player.blockPosition().offset(0, 0, z));
        var target = EntityType.IRON_GOLEM.create(level);
        target.setNoAi(true);
        target.setPos(player.getEyePosition().add(0, -1, 96));
        level.addFreshEntity(target);
        var item = CrystalnexusModItems.BLACK_HOLE_RPG.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(target.getHealth() == 0, "Beam deals 100 damage instantly at 96 blocks without falloff");
        target.discard();
        for (int i = 0; i < BlackHoleRpgItem.COOLDOWN_TICKS; i++) player.getCooldowns().tick();
        var blocked = EntityType.ZOMBIE.create(level);
        blocked.setNoAi(true);
        blocked.setPos(player.getEyePosition().add(0, -1, 32));
        level.addFreshEntity(blocked);
        BlockPos wall = BlockPos.containing(player.getEyePosition().add(0, 0, 16));
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(blocked.getHealth() == blocked.getMaxHealth(), "Solid wall stops damage");
        helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE), "Beam preserves terrain");
        level.removeBlock(wall, false);
        blocked.discard();
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void beamHasMaximumRangeAndDoesNotLoadChunks(GameTestHelper helper) {
        ServerPlayer player = player(helper, 960);
        var level = helper.getLevel();
        player.setYRot(0);
        player.setXRot(0);
        player.setGameMode(GameType.CREATIVE);
        for (int z = 0; z <= 288; z += 16) level.getChunkAt(player.blockPosition().offset(0, 0, z));
        var target = EntityType.ZOMBIE.create(level);
        target.setNoAi(true);
        target.setPos(player.getEyePosition().add(0, -1, BlackHoleRpgItem.RANGE + 2));
        level.addFreshEntity(target);
        var item = CrystalnexusModItems.BLACK_HOLE_RPG.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(target.getHealth() == target.getMaxHealth(), "Beam cannot hit beyond 256 blocks");
        target.discard();
        for (int i = 0; i < BlackHoleRpgItem.COOLDOWN_TICKS; i++) player.getCooldowns().tick();
        player.setPos(20_000_000, 100, 20_000_000);
        BlockPos unloaded = player.blockPosition().south();
        helper.assertTrue(!level.hasChunkAt(unloaded), "Beam destination starts unloaded");
        fire(player, BlackHoleRpgItem.MIN_CHARGE_TICKS);
        helper.assertTrue(!level.hasChunkAt(unloaded), "Beam does not load chunks");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void unloadedFlightDoesNotLoadChunks(GameTestHelper helper) {
        var shot = shot(player(helper, 480));
        shot.setPos(20_000_000, 100, 20_000_000);
        shot.setDeltaMovement(1, 0, 0);
        BlockPos destination = shot.blockPosition().east();
        helper.assertTrue(!helper.getLevel().hasChunkAt(destination), "Destination starts unloaded");
        shot.tick();
        helper.assertTrue(shot.isRemoved() && !helper.getLevel().hasChunkAt(destination), "Unloaded flight discards without loading terrain");
        helper.succeed();
    }

    @GameTest(template = "zero_point")
    public static void chargeReleaseAndEnergyValidation(GameTestHelper helper) {
        ServerPlayer player = player(helper, 1280);
        var item = CrystalnexusModItems.BLACK_HOLE_RPG.get();
        ItemStack stack = new ItemStack(item);
        BatteryData.setEnergy(stack, ToolEnergy.CAPACITY, ToolEnergy.CAPACITY);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = item.use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem() && !result.getResult().shouldSwing(), "Holding starts charging without swinging");
        helper.assertTrue(BatteryData.getEnergy(stack) == ToolEnergy.CAPACITY && !player.getCooldowns().isOnCooldown(item),
                "Starting charge does not fire or spend energy");
        item.releaseUsing(stack, player.level(), player, item.getUseDuration(stack, player) - 4);
        player.stopUsingItem();
        helper.assertTrue(BatteryData.getEnergy(stack) == ToolEnergy.CAPACITY && !player.getCooldowns().isOnCooldown(item),
                "Releasing too early cancels for free");
        helper.assertTrue(BlackHoleRpgItem.charge(-10) == 0 && BlackHoleRpgItem.charge(5) == 0
                && BlackHoleRpgItem.charge(60) == 1 && BlackHoleRpgItem.charge(600) == 1, "Charge clamps at both limits");
        helper.assertTrue(BlackHoleRpgItem.damage(0) == 100 && BlackHoleRpgItem.damage(1) == 400, "Charge scales damage from 100 to 400");
        item.use(player.level(), player, InteractionHand.MAIN_HAND);
        BatteryData.setEnergy(stack, 0, ToolEnergy.CAPACITY);
        item.releaseUsing(stack, player.level(), player, item.getUseDuration(stack, player) - 60);
        player.stopUsingItem();
        helper.assertTrue(!player.getCooldowns().isOnCooldown(item), "Losing energy during charge prevents firing");
        BatteryData.setEnergy(stack, ToolEnergy.CAPACITY, ToolEnergy.CAPACITY);
        var target = EntityType.WARDEN.create(helper.getLevel());
        target.setNoAi(true);
        player.setYRot(0);
        player.setXRot(0);
        target.setPos(player.getEyePosition().add(0, -1, 10));
        helper.getLevel().addFreshEntity(target);
        fire(player, BlackHoleRpgItem.FULL_CHARGE_TICKS);
        helper.assertTrue(target.getHealth() == 100, "Full charge deals 400 damage");
        helper.assertTrue(BatteryData.getEnergy(stack) == 75_000, "Full charge spends exactly one shot's energy");
        target.discard();
        helper.succeed();
    }

    private static void fire(ServerPlayer player, int ticks) {
        var stack = player.getMainHandItem();
        var item = stack.getItem();
        if (!item.use(player.level(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()) return;
        item.releaseUsing(stack, player.level(), player, item.getUseDuration(stack, player) - ticks);
        player.stopUsingItem();
    }

    private static ServerPlayer player(GameTestHelper helper, int offset) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(offset, 60, 0));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z);
        var player = new net.neoforged.neoforge.common.util.FakePlayer(level, new GameProfile(UUID.randomUUID(), "rpg-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        return player;
    }

    private static BlackHoleProjectileEntity shot(ServerPlayer player) {
        var shot = new BlackHoleProjectileEntity(CrystalnexusModEntities.BLACK_HOLE_PROJECTILE.get(), player.level());
        shot.setOwner(player);
        shot.setPos(player.position().add(0, 0, 2));
        return shot;
    }
}
