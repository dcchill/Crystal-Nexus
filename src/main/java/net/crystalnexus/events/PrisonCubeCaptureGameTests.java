package net.crystalnexus.events;

import com.mojang.authlib.GameProfile;
import net.crystalnexus.init.CrystalnexusModItems;
import net.crystalnexus.item.PrisonCubeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("crystalnexus")
@PrefixGameTestTemplate(false)
public final class PrisonCubeCaptureGameTests {
	@GameTest(template = "zero_point", timeoutTicks = 130)
	public static void storesOnlyAfterClosingAndCleansUp(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack stack = player.getMainHandItem();
		var mob = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
		var position = mob.position();
		mob.setCustomName(net.minecraft.network.chat.Component.literal("Sealed pig"));
		((PrisonCubeItem) stack.getItem()).capture(stack, player, mob);
		// A second interaction must neither duplicate visuals nor finish the capture early.
		((PrisonCubeItem) stack.getItem()).capture(stack, player, mob);
		var visuals = helper.getLevel().getEntitiesOfClass(Display.ItemDisplay.class, mob.getBoundingBox().inflate(0.5));
		helper.assertTrue(visuals.size() == 34, "One capture must spawn one set of wall, cubes, tendrils and seal");
		helper.assertTrue(visuals.stream().noneMatch(Display::shouldBeSaved), "Capture visuals must not persist in saved chunks");
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(mob.isAlive() && !PrisonCubeItem.hasStoredEntity(stack), "The mob must remain outside the item during binding");
			helper.assertTrue(mob.position().distanceToSqr(position) < 0.01, "Tendrils must hold the mob in place");
		});
		helper.runAfterDelay(112, () -> {
			helper.assertTrue(mob.isRemoved() && PrisonCubeItem.hasStoredEntity(stack, EntityType.getKey(EntityType.PIG)), "Closing must store the mob exactly once");
			helper.assertTrue(visuals.stream().allMatch(Display::isRemoved), "The completed animation must remove every display");
			helper.succeed();
		});
	}

	@GameTest(template = "zero_point", timeoutTicks = 50)
	public static void switchingItemsCancelsWithoutConsumingMob(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack stack = player.getMainHandItem();
		var mob = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 2));
		((PrisonCubeItem) stack.getItem()).capture(stack, player, mob);
		var visuals = helper.getLevel().getEntitiesOfClass(Display.ItemDisplay.class, mob.getBoundingBox().inflate(0.5));
		helper.runAfterDelay(10, () -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(mob.isAlive() && !mob.isNoAi() && !PrisonCubeItem.hasStoredEntity(stack), "Cancellation must leave a live, unsealed mob with its AI intact");
			helper.assertTrue(visuals.stream().allMatch(Display::isRemoved), "Cancellation must remove every display");
			helper.succeed();
		});
	}

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new GameProfile(UUID.randomUUID(), "prison-test"), ClientInformation.createDefault());
		player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 0)));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrystalnexusModItems.PRISON_CUBE.get()));
		return player;
	}
}
