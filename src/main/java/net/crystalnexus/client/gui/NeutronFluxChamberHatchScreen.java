package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.NeutronFluxChamberHatchBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.world.inventory.NeutronFluxChamberHatchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class NeutronFluxChamberHatchScreen extends HolographicMachineScreen<NeutronFluxChamberHatchMenu> {
	private static final int TANK_X = 116, TANK_Y = 29, TANK_WIDTH = 16, TANK_HEIGHT = 42;

	public NeutronFluxChamberHatchScreen(NeutronFluxChamberHatchMenu menu, Inventory inventory, Component title) {
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
		NeutronFluxChamberHatchBlockEntity hatch = menu.hatch();
		if (hatch != null) machineTank(graphics, hatch.getFluidOutputTank().getFluid(),
			NeutronFluxChamberHatchBlockEntity.FLUID_CAPACITY, leftPos + TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);

		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(graphics);
    }

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "neutron_flux_chamber_hatch"; }
}
