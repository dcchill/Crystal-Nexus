package net.crystalnexus.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.crystalnexus.world.inventory.MasticatorMenu;
import net.crystalnexus.block.entity.MasticatorBlockEntity;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MasticatorScreen extends AbstractContainerScreen<MasticatorMenu> {
	private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/gene_splicer_gui.png");
	private static final ResourceLocation FLESH_PROGRESS_BAR = ResourceLocation.parse("crystalnexus:textures/screens/flesh_progress_bar.png");

	public MasticatorScreen(MasticatorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		if (menu.getSlot(0).container instanceof MasticatorBlockEntity machine) {
			graphics.fill(leftPos + 26, topPos + 35, leftPos + 42, topPos + 51, 0xff181818);
			FluidTankRenderer.draw(graphics, machine.getBloodTank().getFluid(), MasticatorBlockEntity.BLOOD_TANK_CAPACITY,
					leftPos + 26, topPos + 35, 16, 16);
		}
        MachineUpgradeSlots.render(graphics, menu, leftPos, topPos);
		int frame = Mth.clamp(menu.progress() * 10 / menu.maxProgress(), 0, 10);
		graphics.blit(FLESH_PROGRESS_BAR, leftPos + 99, topPos + 26, 0, frame * 32, 32, 32, 32, 352);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (menu.getSlot(0).container instanceof MasticatorBlockEntity machine
				&& isHovering(26, 35, 16, 16, mouseX, mouseY)) {
			graphics.renderComponentTooltip(font, List.of(Component.translatable("fluid.crystalnexus.blood"),
					Component.literal(machine.getBloodTank().getFluidAmount() + " / "
							+ MasticatorBlockEntity.BLOOD_TANK_CAPACITY + " mB")), mouseX, mouseY);
		} else renderTooltip(graphics, mouseX, mouseY);
	}
}
