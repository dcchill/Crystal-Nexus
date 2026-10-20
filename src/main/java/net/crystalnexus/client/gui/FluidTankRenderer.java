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
import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import java.util.ArrayList;
import java.util.List;

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
		var r = MachineGuiWidgets.tank(graphics, fluid, capacity, x, y, width, height, true);
		drawFill(graphics, fluid, capacity, r, MachineGuiWidgets.hasTankFrame());
	}

	/** Opt-in bounded renderer for holographic tanks; legacy/custom screens keep their existing rendering. */
	public static void drawClamped(GuiGraphics graphics, FluidStack fluid, int capacity,
			int x, int y, int width, int height) {
		var r = MachineGuiWidgets.tank(graphics, fluid, capacity, x, y, width, height, false);
		drawFill(graphics, fluid, capacity, r, true);
	}

	/** The tank coordinates include the inner bevel; fluid belongs only in the dark rounded interior. */
	static List<Rect> roundedFillClips(Rect tank, int fill) {
		int x = tank.x() + 1, y = tank.y() + 1, width = tank.width() - 2, height = tank.height() - 2;
		if (width <= 0 || height <= 0 || fill <= 0) return List.of();
		int radius = Math.clamp(MachineGuiTheme.frameRadius(tank.width() + 2, tank.height() + 2) - 2, 0, Math.min(width, height) / 2);
		int bottom = y + height, top = bottom - Math.min(fill, height);
		List<Rect> clips = new ArrayList<>();
		for (int row = 0; row < radius; row++) {
			int inset = MachineGuiTheme.cornerInset(radius, row);
			if (y + row >= top) clips.add(new Rect(x + inset, y + row, width - inset * 2, 1));
			if (bottom - row - 1 >= top) clips.add(new Rect(x + inset, bottom - row - 1, width - inset * 2, 1));
		}
		int middleTop = Math.max(top, y + radius), middleBottom = bottom - radius;
		if (middleBottom > middleTop) clips.add(new Rect(x, middleTop, width, middleBottom - middleTop));
		return clips;
	}

	private static void drawFill(GuiGraphics graphics, FluidStack fluid, int capacity, Rect tank, boolean rounded) {
		int inset = rounded ? 1 : 0;
		int x = tank.x() + inset, y = tank.y() + inset, width = tank.width() - inset * 2, height = tank.height() - inset * 2;
		if (fluid.isEmpty() || capacity <= 0 || width <= 0 || height <= 0) return;
		int fill = (int) Math.clamp((long) fluid.getAmount() * height / capacity, 1, height);
		int top = y + height - fill;
		List<Rect> clips = rounded ? roundedFillClips(tank, fill) : List.of(new Rect(x, top, width, fill));
		IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
		ResourceLocation still = extension.getStillTexture(fluid);
		if (still == null) return;
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
		int tint = extension.getTintColor(fluid);
		float alpha = ((tint >>> 24) & 255) / 255f;
		RenderSystem.setShaderColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f,
			(tint & 255) / 255f, alpha == 0 ? 1 : alpha);
		try {
			for (Rect clip : clips) {
				graphics.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
				try {
					for (int drawY = top + (clip.y() - top) / 16 * 16; drawY < clip.bottom(); drawY += 16)
						for (int drawX = x + (clip.x() - x) / 16 * 16; drawX < clip.right(); drawX += 16)
							graphics.blit(drawX, drawY, 0, 16, 16, sprite);
				} finally { graphics.disableScissor(); }
			}
		} finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
	}
}
