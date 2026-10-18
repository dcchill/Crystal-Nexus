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
		NeutronFluxChamberHatchBlockEntity hatch = menu.hatch();
		
		if (hatch != null && isHovering(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
			FluidStack fluid = hatch.getFluidOutputTank().getFluid();
			graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
				Component.literal(fluid.getAmount() + " / " + NeutronFluxChamberHatchBlockEntity.FLUID_CAPACITY + " mB")), mouseX, mouseY);
			return;
		}
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		machineBackground(graphics);

		// Match the JEI arrow, pointing from the Demon Core slot toward the flux tank.
		graphics.fill(leftPos + 76, topPos + 42, leftPos + 107, topPos + 44, 0xFF686868);
		for (int i = 0; i < 6; i++) {
			graphics.fill(leftPos + 107 - i, topPos + 37 + i,
				leftPos + 108 - i, topPos + 49 - i, 0xFF686868);
		}
		graphics.fill(leftPos + TANK_X - 1, topPos + TANK_Y - 1, leftPos + TANK_X + TANK_WIDTH + 1,
			topPos + TANK_Y + TANK_HEIGHT + 1, 0xff161022);
		NeutronFluxChamberHatchBlockEntity hatch = menu.hatch();
		if (hatch != null) machineTank(graphics, hatch.getFluidOutputTank().getFluid(),
			NeutronFluxChamberHatchBlockEntity.FLUID_CAPACITY, leftPos + TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);

		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}

	@Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "neutron_flux_chamber_hatch"; }
}
