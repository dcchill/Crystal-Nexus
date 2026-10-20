package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.ItemElevatorGuiMenu;
import net.crystalnexus.network.ItemElevatorGuiButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class ItemElevatorGuiScreen extends HolographicMachineScreen<ItemElevatorGuiMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	Button button_toggle_direction;

	public ItemElevatorGuiScreen(ItemElevatorGuiMenu container, Inventory inventory, Component text) {
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


	@Override
	protected void renderMachine(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderMachine(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		machineBackground(guiGraphics);

		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/arrow_up.png"), this.leftPos + 53, this.topPos + 16, 0, 0, 16, 16, 16, 16);
		RenderSystem.disableBlend();

        MachineGuiWidgets.finishBackground(guiGraphics);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.item_elevator_gui.label_item_elevator"), 74, -9, 0xE3F3FF, false);
	}

	@Override
	public void init() {
		super.init();
		button_toggle_direction = Button.builder(Component.translatable("gui.crystalnexus.item_elevator_gui.button_toggle_direction"), e -> {
			int x = ItemElevatorGuiScreen.this.x;
			int y = ItemElevatorGuiScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new ItemElevatorGuiButtonMessage(0, x, y, z));
				ItemElevatorGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 33, this.topPos + 35, 108, 20).build();
		this.addRenderableWidget(button_toggle_direction);
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "item_elevator"; }
}
