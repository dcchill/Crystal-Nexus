package net.crystalnexus.client.gui;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.CryogenicFlashFreezerBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.world.inventory.CryogenicFlashFreezerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CryogenicFlashFreezerScreen extends AbstractContainerScreen<CryogenicFlashFreezerMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/cryo_chamber_gui.png");
	private static final ResourceLocation PROGRESS = ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png");
	private static final int[] TANK_X = { 50, 113 };
	private static final int TANK_Y = 16;
	private static final int TANK_HEIGHT = 34;

	public CryogenicFlashFreezerScreen(CryogenicFlashFreezerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		CryogenicFlashFreezerBlockEntity freezer = menu.freezer();
		if (freezer != null) {
			if (isHovering(-21, 9, 24, 24, mouseX, mouseY)) {
				graphics.renderTooltip(font, Component.literal("FE: " + freezer.getEnergyStorage().getEnergyStored()), mouseX, mouseY);
				return;
			}
			for (int i = 0; i < TANK_X.length; i++) {
				if (!isHovering(TANK_X[i], TANK_Y, 16, TANK_HEIGHT, mouseX, mouseY)) continue;
				FluidStack fluid = freezer.getTank(i).getFluid();
				graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
					Component.literal(fluid.getAmount() + " / " + CryogenicFlashFreezerBlockEntity.TANK_CAPACITY + " mB")), mouseX, mouseY);
				return;
			}
		}
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		CryogenicFlashFreezerBlockEntity freezer = menu.freezer();
		if (freezer != null) {
			for (int i = 0; i < TANK_X.length; i++) drawTank(graphics, freezer.getTank(i).getFluid(), TANK_X[i], TANK_Y);
			double max = freezer.getPersistentData().getDouble("maxProgress");
			double progress = freezer.getPersistentData().getDouble("progress");
			int frame = max <= 0 ? 0 : Mth.clamp((int) Math.ceil(progress / max * 10), 0, 10);
			graphics.blit(PROGRESS, leftPos + 73, topPos + 32, 0, frame * 32, 32, 32, 32, 352);
		}
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), leftPos + 50, topPos - 15, 0, 0, 126, 18, 126, 18);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), leftPos + 173, topPos, 0, 0, 32, 32, 32, 32);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), leftPos - 33, topPos - 1, 0, 0, 48, 48, 48, 48);
		graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), leftPos - 25, topPos + 5, 0,
			Mth.clamp((int) EnergyDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320), 32, 32, 32, 352);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}

	private void drawTank(GuiGraphics graphics, FluidStack fluid, int x, int y) {
		if (fluid.isEmpty()) return;
		int height = Math.max(1, fluid.getAmount() * TANK_HEIGHT / CryogenicFlashFreezerBlockEntity.TANK_CAPACITY);
		IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = extension.getStillTexture(fluid);
		if (still == null) return;
		ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(still.getNamespace(), "textures/" + still.getPath() + ".png");
		int tint = extension.getTintColor(fluid);
		RenderSystem.setShaderColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f,
			((tint >>> 24) & 255) == 0 ? 1 : ((tint >>> 24) & 255) / 255f);
		int screenX = leftPos + x, bottom = topPos + y + TANK_HEIGHT;
		graphics.enableScissor(screenX, bottom - height, screenX + 16, bottom);
		for (int drawY = bottom - height; drawY < bottom; drawY += 16)
			graphics.blit(texture, screenX, drawY, 0, 0, 16, 16, 16, 16);
		graphics.disableScissor();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, Component.translatable("gui.crystalnexus.cryogenic_flash_freezer"), 35, -11, -12829636, false);
	}
}
