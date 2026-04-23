package net.crystalnexus.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.crystalnexus.world.inventory.BlueprintGUIMenu;
import net.crystalnexus.init.CrystalnexusModScreens;
import net.crystalnexus.network.BlueprintSaveMessage;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;

import com.mojang.blaze3d.systems.RenderSystem;

public class BlueprintGUIScreen extends AbstractContainerScreen<BlueprintGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	EditBox name;
	Button button_save;

	public BlueprintGUIScreen(BlueprintGUIMenu container, Inventory inventory, Component text) {
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
		if (elementType == 0 && elementState instanceof String stringState) {
			if (name.equals("name"))
				this.name.setValue(stringState);
		}
		menuStateUpdateActive = false;
	}

	private static final ResourceLocation texture = ResourceLocation.parse("crystalnexus:textures/screens/blueprint_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		name.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/nameaddon.png"), this.leftPos + 50, this.topPos + -15, 0, 0, 126, 18, 126, 18);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (name.isFocused())
			return name.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String nameValue = name.getValue();
		super.resize(minecraft, width, height);
		name.setValue(nameValue);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.blueprint_gui.label_blueprint_controller"), 55, -11, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		name = new EditBox(this.font, this.leftPos + 52, this.topPos + 40, 118, 18, Component.translatable("gui.crystalnexus.blueprint_gui.name"));
		name.setMaxLength(8192);
		name.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "name", content, false);
		});
		name.setHint(Component.translatable("gui.crystalnexus.blueprint_gui.name"));
		this.addWidget(this.name);
		button_save = Button.builder(Component.translatable("gui.crystalnexus.blueprint_gui.button_save"), e -> {
			PacketDistributor.sendToServer(new BlueprintSaveMessage(new BlockPos(this.x, this.y, this.z), name.getValue()));
		}).bounds(this.leftPos + 91, this.topPos + 61, 46, 20).build();
		this.addRenderableWidget(button_save);
	}
}
