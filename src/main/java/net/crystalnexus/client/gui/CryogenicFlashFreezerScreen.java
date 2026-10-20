package net.crystalnexus.client.gui;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.CryogenicFlashFreezerBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.world.inventory.CryogenicFlashFreezerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CryogenicFlashFreezerScreen extends HolographicMachineScreen<CryogenicFlashFreezerMenu> {
	private static final int[] TANK_X = net.crystalnexus.world.inventory.MachineGuiLayout.TWO_TANK_X;

	public CryogenicFlashFreezerScreen(CryogenicFlashFreezerMenu menu, Inventory inventory, Component title) {
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
		CryogenicFlashFreezerBlockEntity freezer = menu.freezer();
		if (freezer != null) {
			for (int i = 0; i < TANK_X.length; i++) {
				var tank = net.crystalnexus.world.inventory.MachineGuiLayout.twoTank(i, false);
				machineTank(graphics, freezer.getTank(i).getFluid(), CryogenicFlashFreezerBlockEntity.TANK_CAPACITY,
					leftPos + tank.x(), topPos + tank.y(), tank.width(), tank.height());
			}
			double max = freezer.getPersistentData().getDouble("maxProgress");
			double progress = freezer.getPersistentData().getDouble("progress");
			machineProgress(graphics, net.crystalnexus.world.inventory.MachineGuiLayout.fraction(progress, max));
		}

		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(graphics);
    }

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "cryogenic_flash_freezer_hatch"; }
}
