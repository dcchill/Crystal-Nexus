package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.crystalnexus.world.inventory.CircuitPressGUIMenu;
import net.crystalnexus.block.entity.CircuitPressBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.procedures.CircuitPressOnTickUpdateProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public class CircuitPressGUIScreen extends AbstractContainerScreen<CircuitPressGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public CircuitPressGUIScreen(CircuitPressGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/circuit_press_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		boolean customTooltipShown = false;
		if (mouseX > leftPos + -22 && mouseX < leftPos + 2 && mouseY > topPos + 9 && mouseY < topPos + 33) {
			String hoverText = CircuitPressOnTickUpdateProcedure.execute(world, x, y, z);
			if (hoverText != null) {
				guiGraphics.renderComponentTooltip(font, Arrays.stream(hoverText.split("\n")).map(Component::literal).collect(Collectors.toList()), mouseX, mouseY);
			}
			customTooltipShown = true;
		}
		if (!customTooltipShown)
			if (isHovering(115, 16, 16, 34, mouseX, mouseY)) {
				CircuitPressBlockEntity press = menu.press();
				FluidStack fluid = press == null ? FluidStack.EMPTY : press.getNitrogenTank().getFluid();
				guiGraphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(), Component.literal(fluid.getAmount() + " / 4000 mB")), mouseX, mouseY);
			} else this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/crystal_purifier_gui_addon.png"), this.leftPos + 65, this.topPos + 33, 0, 0, 44, 33, 44, 33);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), this.leftPos + -33, this.topPos + -1, 0, 0, 48, 48, 48, 48);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), this.leftPos + 173, this.topPos + 0, 0, 0, 32, 32, 32, 32);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), this.leftPos + -25, this.topPos + 5, 0, Mth.clamp((int) EnergyDisplayProcedure.execute(world, x, y, z) * 32, 0, 320), 32, 32, 32, 352);
		CircuitPressBlockEntity press = menu.press();
		double maxProgress = press == null ? 0 : press.getPersistentData().getDouble("maxProgress");
		double progress = press == null ? 0 : press.getPersistentData().getDouble("progress");
		int frame = maxProgress <= 0 ? 0 : Mth.clamp((int) Math.ceil(progress / maxProgress * 10), 0, 10);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbarinvert.png"), this.leftPos + 71, this.topPos + 14, 0, frame * 32, 32, 32, 32, 352);
		if (press != null) drawTank(guiGraphics, press.getNitrogenTank().getFluid(), 115, 16);
		RenderSystem.disableBlend();
	}

	private void drawTank(GuiGraphics graphics, FluidStack fluid, int x, int y) {
		if (fluid.isEmpty()) return;
		int height = Math.max(1, fluid.getAmount() * 34 / CircuitPressBlockEntity.TANK_CAPACITY);
		IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = extension.getStillTexture(fluid);
		if (still == null) return;
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
		int tint = extension.getTintColor(fluid);
		RenderSystem.setShaderColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f, 1);
		int screenX = leftPos + x, bottom = topPos + y + 34;
		graphics.enableScissor(screenX, bottom - height, screenX + 16, bottom);
		for (int drawY = bottom - height; drawY < bottom; drawY += 16)
			graphics.blit(screenX, drawY, 0, 16, 16, sprite);
		graphics.disableScissor();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.circuit_press_gui.label_proc_get_block_name_for_gui"), 73, -9, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
	}
}
