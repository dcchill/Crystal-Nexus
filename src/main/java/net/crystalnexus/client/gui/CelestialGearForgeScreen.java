package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.CelestialGearForgeMenu;
import net.crystalnexus.block.entity.CelestialGearForgeBlockEntity;
import net.crystalnexus.recipe.CelestialGearForgeEnchanting;
import net.crystalnexus.item.GradientItemName;
import net.crystalnexus.init.CrystalnexusModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CelestialGearForgeScreen extends AbstractContainerScreen<CelestialGearForgeMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("crystalnexus", "textures/screens/gear_forge.png");
	private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath("crystalnexus", "textures/screens/progressbar.png");
	private Button previous, next;
	public CelestialGearForgeScreen(CelestialGearForgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 193;
	}

	@Override protected void init() {
		super.init();
		previous = addRenderableWidget(selectionButton("<", 99, 0));
		next = addRenderableWidget(selectionButton(">", 148, 1));
	}

	private Button selectionButton(String label, int x, int id) {
		return Button.builder(Component.literal(label), button -> {
			if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}).bounds(leftPos + x, topPos + 59, 20, 18)
			.tooltip(Tooltip.create(Component.translatable("gui.crystalnexus.celestial_gear_forge." + (id == 0 ? "previous" : "next")))).build();
	}

	@Override protected void containerTick() {
		super.containerTick();
		previous.active = next.active = !combining() && CelestialGearForgeBlockEntity.enchantmentChoices(menu.getSlot(0).getItem(),
			menu.getSlot(CelestialGearForgeBlockEntity.STAR_SLOT).getItem()).size() > 1;
	}

	private boolean combining() {
		for (int slot = 1; slot < CelestialGearForgeBlockEntity.STAR_SLOT; slot++) if (menu.getSlot(slot).hasItem()) return true;
		return false;
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 92, imageWidth, 181);
		graphics.fill(leftPos, topPos + 92, leftPos + imageWidth, topPos + 104, 0xff0b0031);
		graphics.blit(TEXTURE, leftPos, topPos + 104, 0, 92, imageWidth, 89, imageWidth, 181);
		int frame = Mth.clamp(Mth.ceil(menu.progress() * 10.0F / Math.max(1, menu.duration())), 0, 10);
		graphics.blit(PROGRESS, leftPos + 100, topPos + 29, 0, frame * 32, 32, 32, 32, 352);
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		var input = menu.getSlot(0).getItem();
		var star = menu.getSlot(CelestialGearForgeBlockEntity.STAR_SLOT).getItem();
		var choices = CelestialGearForgeBlockEntity.enchantmentChoices(input, star);
		int index = menu.selectedEnchantmentIndex();
		if (combining()) {
			boolean booksOnly = java.util.stream.IntStream.range(1, CelestialGearForgeBlockEntity.STAR_SLOT).mapToObj(slot -> menu.getSlot(slot).getItem())
				.allMatch(stack -> stack.isEmpty() || stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK));
			graphics.drawCenteredString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge." + (booksOnly ? "combining" : "crafting")), 132, 11, 0xfff2e8d5);
		} else if (index >= 0 && index < choices.size()) {
			var selected = choices.get(index);
			int current = EnchantmentHelper.getEnchantmentsForCrafting(input).getLevel(selected);
			String name = font.plainSubstrByWidth(selected.value().description().getString(), 72);
			boolean zeroStar = star.is(CrystalnexusModItems.ZERO_STAR.get());
			graphics.drawCenteredString(font, zeroStar ? GradientItemName.rainbow(Component.literal(name)) : Component.literal(name), 132, 11, 0xfff2e8d5);
			int nextLevel = (int) Math.min(CelestialGearForgeEnchanting.levelCap(star), (long) current + CelestialGearForgeBlockEntity.starBoost(star));
			Component levels = Component.translatable("gui.crystalnexus.celestial_gear_forge.levels", current, nextLevel);
			graphics.drawCenteredString(font, zeroStar ? GradientItemName.rainbow(levels) : levels, 132, 24, 0xffffc647);
		}
		graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge.stored", menu.energyStored()), 8, 80, 0xffded5c4, false);
		boolean needsPower = menu.energyPerTick() > 0 && menu.energyStored() < menu.energyPerTick();
		if (!menu.formed() || needsPower) graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge."
			+ (menu.formed() ? "needs_power" : "incomplete_short")), 105, 80, 0xffff7777, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge.power_rate", menu.energyPerTick()), 8, 94, needsPower ? 0xffff7777 : 0xffded5c4, false);
		graphics.drawString(font, Component.translatable("gui.crystalnexus.celestial_gear_forge.book_limit",
			CelestialGearForgeEnchanting.bookLimit(menu.getSlot(CelestialGearForgeBlockEntity.STAR_SLOT).getItem())), 122, 94, 0xffded5c4, false);
	}

	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);
		if (isHovering(8, 80, 160, 24, mouseX, mouseY))
			graphics.renderTooltip(font, java.util.List.of(Component.translatable("gui.crystalnexus.celestial_gear_forge.energy_capacity", menu.energyStored(), Integer.MAX_VALUE),
				Component.translatable("gui.crystalnexus.celestial_gear_forge.energy_rule"),
				Component.translatable("gui.crystalnexus.celestial_gear_forge.energy_total", (long) menu.energyPerTick() * menu.duration())), java.util.Optional.empty(), mouseX, mouseY);
	}
}
