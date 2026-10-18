package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.RefineryBlockEntity;
import net.crystalnexus.network.FluidChemicalReactionChamberPurgeMessage;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.world.inventory.RefineryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class RefineryScreen extends HolographicMachineScreen<RefineryMenu> {
    private static final int[] TANK_X = net.crystalnexus.world.inventory.MachineGuiLayout.TWO_TANK_X;
    private static final int TANK_HEIGHT = net.crystalnexus.world.inventory.MachineGuiLayout.TANK_HEIGHT;
    public RefineryScreen(RefineryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 166;
    }
    @Override protected void renderMachine(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderMachine(graphics, mouseX, mouseY, partialTick);
        RefineryBlockEntity refinery = menu.refinery();
        if (refinery != null) {
            
            for (int i = 0; i < TANK_X.length; i++) if (isHovering(TANK_X[i], net.crystalnexus.world.inventory.MachineGuiLayout.TANK_Y, 16, TANK_HEIGHT, mouseX, mouseY)) {
                FluidStack fluid = refinery.getTank(i).getFluid();
                graphics.renderComponentTooltip(font, List.of(fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName(),
                    Component.literal(fluid.getAmount() + " / 4000 mB")), mouseX, mouseY); return;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        machineBackground(graphics);
        RefineryBlockEntity refinery = menu.refinery();
        if (refinery != null) for (int i = 0; i < TANK_X.length; i++)
            machineTank(graphics, refinery.getTank(i).getFluid(), RefineryBlockEntity.TANK_CAPACITY,
                leftPos + TANK_X[i], topPos + net.crystalnexus.world.inventory.MachineGuiLayout.TANK_Y, 16, TANK_HEIGHT);

        machineProgress(graphics, (Mth.clamp((int) ProgressDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320)) / 320.0);

        RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
    @Override protected void init() {
        super.init();
        if (menu.refinery() == null) return;
        for (int i = 0; i < TANK_X.length; i++) {
            int tank = i;
            addRenderableWidget(Button.builder(Component.literal("X"), button -> PacketDistributor.sendToServer(
                new FluidChemicalReactionChamberPurgeMessage(tank, new BlockPos(menu.x, menu.y, menu.z))))
                .bounds(leftPos + TANK_X[i], topPos + net.crystalnexus.world.inventory.MachineGuiLayout.PURGE_Y, 16, net.crystalnexus.world.inventory.MachineGuiLayout.PURGE_HEIGHT).build());
        }
    }
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "refinery"; }
}
