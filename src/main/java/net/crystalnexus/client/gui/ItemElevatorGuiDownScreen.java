package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.ItemElevatorGuiDownMenu;
import net.crystalnexus.network.ItemElevatorGuiDownButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class ItemElevatorGuiDownScreen extends AbstractContainerScreen<ItemElevatorGuiDownMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	Button button_toggle_direction;

	public ItemElevatorGuiDownScreen(ItemElevatorGuiDownMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 144;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/item_elevator_gui_down.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/arrow_down.png"), this.leftPos + 53, this.topPos + -13, 0, 0, 16, 16, 16, 16);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.item_elevator_gui_down.label_item_elevator"), 74, -9, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		button_toggle_direction = Button.builder(Component.translatable("gui.crystalnexus.item_elevator_gui_down.button_toggle_direction"), e -> {
			int x = ItemElevatorGuiDownScreen.this.x;
			int y = ItemElevatorGuiDownScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new ItemElevatorGuiDownButtonMessage(0, x, y, z));
				ItemElevatorGuiDownButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 33, this.topPos + 35, 108, 20).build();
		this.addRenderableWidget(button_toggle_direction);
	}
}