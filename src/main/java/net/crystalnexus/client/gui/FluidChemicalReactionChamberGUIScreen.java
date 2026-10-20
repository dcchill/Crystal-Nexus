package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.network.FluidChemicalReactionChamberPurgeMessage;
import net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class FluidChemicalReactionChamberGUIScreen extends HolographicMachineScreen<FluidChemicalReactionChamberGUIMenu> {
    private static final int[] TANK_X = net.crystalnexus.world.inventory.MachineGuiLayout.THREE_TANK_X;
    private static final int TANK_Y = net.crystalnexus.world.inventory.MachineGuiLayout.TANK_Y;
    private static final int TANK_HEIGHT = net.crystalnexus.world.inventory.MachineGuiLayout.TANK_HEIGHT;

    public FluidChemicalReactionChamberGUIScreen(FluidChemicalReactionChamberGUIMenu menu, Inventory inventory, Component title) {
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
        FluidChemicalReactionChamberBlockEntity chamber = menu.chamber();
        if (chamber != null) for (int i = 0; i < TANK_X.length; i++)
            machineTank(graphics, chamber.getTank(i).getFluid(), chamber.getTankCapacity(),
                leftPos + TANK_X[i], topPos + TANK_Y, 16, TANK_HEIGHT);

        machineProgress(graphics, MachineGuiWidgets.progress(menu.entity.level(), new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z)));

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(graphics);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override protected void init() {
        super.init();
        if (menu.chamber() == null) return;
        for (int i = 0; i < TANK_X.length; i++) {
            int tank = i;
            addRenderableWidget(Button.builder(Component.literal("X"), button ->
                PacketDistributor.sendToServer(new FluidChemicalReactionChamberPurgeMessage(tank,
                    new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z))))
                .bounds(leftPos + TANK_X[i] - 1, topPos + net.crystalnexus.world.inventory.MachineGuiLayout.PURGE_Y, 18, net.crystalnexus.world.inventory.MachineGuiLayout.PURGE_HEIGHT).build());
        }
    }
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "fluid_chemical_reaction_chamber"; }
}
