package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidTankRenderer {
	private FluidTankRenderer() {}

	public static void drawBlockTank(GuiGraphics graphics, LevelAccessor level, BlockPos pos, int tank,
			int x, int y, int width, int height) {
		if (!(level instanceof ILevelExtension extension)) return;
		IFluidHandler handler = extension.getCapability(Capabilities.FluidHandler.BLOCK, pos, (Direction) null);
		if (handler == null || tank < 0 || tank >= handler.getTanks()) return;
		draw(graphics, handler.getFluidInTank(tank), handler.getTankCapacity(tank), x, y, width, height);
	}

	public static void draw(GuiGraphics graphics, FluidStack fluid, int capacity,
			int x, int y, int width, int height) {
		if (fluid.isEmpty() || capacity <= 0) return;
		int fill = Math.max(1, fluid.getAmount() * height / capacity);
		IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = extension.getStillTexture(fluid);
		if (still == null) return;
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
		int tint = extension.getTintColor(fluid);
		float alpha = ((tint >>> 24) & 255) / 255f;
		RenderSystem.setShaderColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f,
			(tint & 255) / 255f, alpha == 0 ? 1 : alpha);
		int bottom = y + height;
		graphics.enableScissor(x, bottom - fill, x + width, bottom);
		for (int drawY = bottom - fill; drawY < bottom; drawY += 16)
			for (int drawX = x; drawX < x + width; drawX += 16)
				graphics.blit(drawX, drawY, 0, 16, 16, sprite);
		graphics.disableScissor();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
