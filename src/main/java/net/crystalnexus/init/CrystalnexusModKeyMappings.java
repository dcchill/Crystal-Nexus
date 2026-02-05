/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.crystalnexus.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.crystalnexus.network.JetpackJumpMessage;
import net.crystalnexus.network.HoverpackToggleMessage;
import net.crystalnexus.network.HoverpackRiseMessage;
import net.crystalnexus.network.HoverpackRightMessage;
import net.crystalnexus.network.HoverpackLeftMessage;
import net.crystalnexus.network.HoverpackForwardMessage;
import net.crystalnexus.network.HoverpackBackwardMessage;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class CrystalnexusModKeyMappings {
	public static final KeyMapping JETPACK_JUMP = new KeyMapping("key.crystalnexus.jetpack_jump", GLFW.GLFW_KEY_SPACE, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new JetpackJumpMessage(0, 0));
				JetpackJumpMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				JETPACK_JUMP_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - JETPACK_JUMP_LASTPRESS);
				PacketDistributor.sendToServer(new JetpackJumpMessage(1, dt));
				JetpackJumpMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_RISE = new KeyMapping("key.crystalnexus.hoverpack_rise", GLFW.GLFW_KEY_SPACE, "key.categories.hoverpack") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackRiseMessage(0, 0));
				HoverpackRiseMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				HOVERPACK_RISE_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - HOVERPACK_RISE_LASTPRESS);
				PacketDistributor.sendToServer(new HoverpackRiseMessage(1, dt));
				HoverpackRiseMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_FORWARD = new KeyMapping("key.crystalnexus.hoverpack_forward", GLFW.GLFW_KEY_W, "key.categories.hoverpack") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackForwardMessage(0, 0));
				HoverpackForwardMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				HOVERPACK_FORWARD_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - HOVERPACK_FORWARD_LASTPRESS);
				PacketDistributor.sendToServer(new HoverpackForwardMessage(1, dt));
				HoverpackForwardMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_BACKWARD = new KeyMapping("key.crystalnexus.hoverpack_backward", GLFW.GLFW_KEY_S, "key.categories.hoverpack") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackBackwardMessage(0, 0));
				HoverpackBackwardMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				HOVERPACK_BACKWARD_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - HOVERPACK_BACKWARD_LASTPRESS);
				PacketDistributor.sendToServer(new HoverpackBackwardMessage(1, dt));
				HoverpackBackwardMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_LEFT = new KeyMapping("key.crystalnexus.hoverpack_left", GLFW.GLFW_KEY_A, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackLeftMessage(0, 0));
				HoverpackLeftMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				HOVERPACK_LEFT_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - HOVERPACK_LEFT_LASTPRESS);
				PacketDistributor.sendToServer(new HoverpackLeftMessage(1, dt));
				HoverpackLeftMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_RIGHT = new KeyMapping("key.crystalnexus.hoverpack_right", GLFW.GLFW_KEY_D, "key.categories.hoverpack") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackRightMessage(0, 0));
				HoverpackRightMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				HOVERPACK_RIGHT_LASTPRESS = System.currentTimeMillis();
			} else if (isDownOld != isDown && !isDown) {
				int dt = (int) (System.currentTimeMillis() - HOVERPACK_RIGHT_LASTPRESS);
				PacketDistributor.sendToServer(new HoverpackRightMessage(1, dt));
				HoverpackRightMessage.pressAction(Minecraft.getInstance().player, 1, dt);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping HOVERPACK_TOGGLE = new KeyMapping("key.crystalnexus.hoverpack_toggle", GLFW.GLFW_KEY_R, "key.categories.hoverpack") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new HoverpackToggleMessage(0, 0));
				HoverpackToggleMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	private static long JETPACK_JUMP_LASTPRESS = 0;
	private static long HOVERPACK_RISE_LASTPRESS = 0;
	private static long HOVERPACK_FORWARD_LASTPRESS = 0;
	private static long HOVERPACK_BACKWARD_LASTPRESS = 0;
	private static long HOVERPACK_LEFT_LASTPRESS = 0;
	private static long HOVERPACK_RIGHT_LASTPRESS = 0;

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(JETPACK_JUMP);
		event.register(HOVERPACK_RISE);
		event.register(HOVERPACK_FORWARD);
		event.register(HOVERPACK_BACKWARD);
		event.register(HOVERPACK_LEFT);
		event.register(HOVERPACK_RIGHT);
		event.register(HOVERPACK_TOGGLE);
	}

	@EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				JETPACK_JUMP.consumeClick();
				HOVERPACK_RISE.consumeClick();
				HOVERPACK_FORWARD.consumeClick();
				HOVERPACK_BACKWARD.consumeClick();
				HOVERPACK_LEFT.consumeClick();
				HOVERPACK_RIGHT.consumeClick();
				HOVERPACK_TOGGLE.consumeClick();
			}
		}
	}
}