package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.CircuitPressGUIMenu;
import net.crystalnexus.block.entity.CircuitPressBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;

public class CircuitPressGUIScreen extends HolographicMachineScreen<CircuitPressGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
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


	@Override
	protected void renderMachine(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderMachine(guiGraphics, mouseX, mouseY, partialTicks);
		
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
		machineBackground(guiGraphics);

		CircuitPressBlockEntity press = menu.press();
		double maxProgress = press == null ? 0 : press.getPersistentData().getDouble("maxProgress");
		double progress = press == null ? 0 : press.getPersistentData().getDouble("progress");
		int frame = maxProgress <= 0 ? 0 : Mth.clamp((int) Math.ceil(progress / maxProgress * 10), 0, 10);
		machineProgress(guiGraphics, (frame * 32) / 320.0);
		if (press != null) machineTank(guiGraphics, press.getNitrogenTank().getFluid(),
			CircuitPressBlockEntity.TANK_CAPACITY, leftPos + 115, topPos + 16, 16, 34);
		RenderSystem.disableBlend();
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
	}

	@Override
	public void init() {
		super.init();
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "circuit_press"; }
}
