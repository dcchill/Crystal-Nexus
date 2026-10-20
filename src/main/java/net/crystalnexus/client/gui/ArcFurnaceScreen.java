package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.world.inventory.ArcFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class ArcFurnaceScreen extends HolographicMachineScreen<ArcFurnaceMenu> {

	public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderMachine(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		machineBackground(graphics);

		machineProgress(graphics, MachineGuiWidgets.progress(menu.entity.level(), new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z)));

		RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(graphics);
    }

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "arc_furnace"; }
}
