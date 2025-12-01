/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.GameRules;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CrystalnexusModGameRules {
	public static GameRules.Key<GameRules.BooleanValue> DISABLE_MELTDOWNS;

	@SubscribeEvent
	public static void registerGameRules(FMLCommonSetupEvent event) {
		DISABLE_MELTDOWNS = GameRules.register("disableMeltdowns", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
	}
}