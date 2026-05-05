package net.crystalnexus.client.blackhole;

import net.crystalnexus.CrystalnexusMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = CrystalnexusMod.MODID, value = Dist.CLIENT)
public class BlackHoleSoundEvents {
	@SubscribeEvent
	public static void onPlaySound(PlaySoundEvent event) {
		SoundInstance sound = event.getSound();
		Minecraft mc = Minecraft.getInstance();
		if (sound == null || mc.level == null || sound.getSource() != SoundSource.BLOCKS) {
			return;
		}

		Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
		long gameTime = mc.level.getGameTime();
		for (BlackHoleVisualState.Visual visual : BlackHoleVisualState.active(gameTime)) {
			if (visual.center().distanceToSqr(soundPos) <= visual.radius() * visual.radius()) {
				event.setSound(null);
				return;
			}
		}
	}
}
