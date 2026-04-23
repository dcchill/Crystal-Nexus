package net.crystalnexus.client;

import net.crystalnexus.item.BuildGunItem;
import net.crystalnexus.network.BuildgunUsageItemsMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class BuildgunUsageOverlay {
	private static final List<BuildgunUsageItemsMessage.Entry> ENTRIES = new ArrayList<>();
	private static long visibleUntilMs = 0L;

	public static void show(List<BuildgunUsageItemsMessage.Entry> entries) {
		ENTRIES.clear();
		ENTRIES.addAll(entries);
		visibleUntilMs = System.currentTimeMillis() + 2500L;
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		boolean activePlacement = isPlacementActive(minecraft);
		if (ENTRIES.isEmpty() || (!activePlacement && System.currentTimeMillis() > visibleUntilMs)) {
			return;
		}
		GuiGraphics guiGraphics = event.getGuiGraphics();
		int centerX = guiGraphics.guiWidth() / 2;
		int y = guiGraphics.guiHeight() - 74;
		guiGraphics.drawCenteredString(minecraft.font, Component.literal("Uses"), centerX, y - 12, 0xD7F0FF);
		int totalSlots = Math.min(ENTRIES.size(), 8) + (ENTRIES.size() > 8 ? 1 : 0);
		int startX = centerX - (totalSlots * 20 - 2) / 2;

		for (int i = 0; i < Math.min(ENTRIES.size(), 8); i++) {
			int x = startX + i * 20;
			guiGraphics.fill(x - 2, y - 2, x + 18, y + 18, 0xB0101116);
			ItemStack stack = new ItemStack(ENTRIES.get(i).item());
			guiGraphics.renderItem(stack, x, y);
			guiGraphics.renderItemDecorations(minecraft.font, stack, x, y, String.valueOf(ENTRIES.get(i).count()));
		}
		if (ENTRIES.size() > 8) {
			int x = startX + 8 * 20;
			guiGraphics.fill(x - 2, y - 2, x + 18, y + 18, 0xB0101116);
			guiGraphics.drawCenteredString(minecraft.font, "+" + (ENTRIES.size() - 8), x + 8, y + 5, 0xFFD966);
		}
	}

	private static boolean isPlacementActive(Minecraft minecraft) {
		if (minecraft.player == null) {
			return false;
		}
		ItemStack stack = minecraft.player.getMainHandItem();
		if (!(stack.getItem() instanceof BuildGunItem)) {
			stack = minecraft.player.getOffhandItem();
		}
		if (!(stack.getItem() instanceof BuildGunItem)) {
			return false;
		}
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("buildgunPlacementActive");
	}
}
