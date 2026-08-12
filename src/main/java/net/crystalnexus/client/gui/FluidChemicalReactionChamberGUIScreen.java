package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.block.entity.FluidChemicalReactionChamberBlockEntity;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.network.FluidChemicalReactionChamberPurgeMessage;
import net.crystalnexus.world.inventory.FluidChemicalReactionChamberGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class FluidChemicalReactionChamberGUIScreen extends AbstractContainerScreen<FluidChemicalReactionChamberGUIMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/fluid_chemical_reaction_chamber_gui.png");
    private static final int[] TANK_X = {28, 52, 115};
    private static final int TANK_HEIGHT = 34;

    public FluidChemicalReactionChamberGUIScreen(FluidChemicalReactionChamberGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        FluidChemicalReactionChamberBlockEntity chamber = menu.chamber();
        if (chamber != null) {
            if (isHovering(-21, 9, 24, 24, mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.literal("FE: " + chamber.getEnergyStorage().getEnergyStored()), mouseX, mouseY);
                return;
            }
            for (int i = 0; i < TANK_X.length; i++) {
                if (isHovering(TANK_X[i], 26, 16, TANK_HEIGHT, mouseX, mouseY)) {
                    FluidStack fluid = chamber.getTank(i).getFluid();
                    Component name = fluid.isEmpty() ? Component.literal("Empty") : fluid.getHoverName();
                    graphics.renderComponentTooltip(font,
                        List.of(name, Component.literal(fluid.getAmount() + " / 4000 mB")), mouseX, mouseY);
                    return;
                }
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        FluidChemicalReactionChamberBlockEntity chamber = menu.chamber();
        if (chamber != null) for (int i = 0; i < TANK_X.length; i++) drawTank(graphics, chamber.getTank(i).getFluid(), TANK_X[i], 26);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), leftPos + 50, topPos - 15, 0, 0, 126, 18, 126, 18);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png"), leftPos + 173, topPos, 0, 0, 32, 32, 32, 32);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/battery_addon.png"), leftPos - 33, topPos - 1, 0, 0, 48, 48, 48, 48);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/progressbar.png"), leftPos + 76, topPos + 27, 0,
            Mth.clamp((int) ProgressDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320), 32, 32, 32, 352);
        graphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/batterylevelsmall.png"), leftPos - 25, topPos + 5, 0,
            Mth.clamp((int) EnergyDisplayProcedure.execute(menu.entity.level(), menu.x, menu.y, menu.z) * 32, 0, 320), 32, 32, 32, 352);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private void drawTank(GuiGraphics graphics, FluidStack fluid, int x, int y) {
        if (fluid.isEmpty()) return;
        int height = Math.max(1, fluid.getAmount() * TANK_HEIGHT / FluidChemicalReactionChamberBlockEntity.TANK_CAPACITY);
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extension.getStillTexture(fluid);
        if (still == null) return;
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(still.getNamespace(), "textures/" + still.getPath() + ".png");
        int tint = extension.getTintColor(fluid);
        RenderSystem.setShaderColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f,
            ((tint >>> 24) & 255) == 0 ? 1 : ((tint >>> 24) & 255) / 255f);
        int screenX = leftPos + x;
        int bottom = topPos + y + TANK_HEIGHT;
        graphics.enableScissor(screenX, bottom - height, screenX + 16, bottom);
        for (int drawY = bottom - height; drawY < bottom; drawY += 16)
            graphics.blit(texture, screenX, drawY, 0, 0, 16, 16, 16, 16);
        graphics.disableScissor();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.crystalnexus.fluid_chemical_reaction_chamber"), 44, -11, -12829636, false);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < TANK_X.length; i++) {
            int tank = i;
            addRenderableWidget(Button.builder(Component.literal("X"), button ->
                PacketDistributor.sendToServer(new FluidChemicalReactionChamberPurgeMessage(tank,
                    new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z))))
                .bounds(leftPos + TANK_X[i], topPos + 15, 16, 10).build());
        }
    }
}
