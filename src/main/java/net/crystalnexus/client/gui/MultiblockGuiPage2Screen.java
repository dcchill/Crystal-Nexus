package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.MultiblockGuiPage2Menu;
import net.crystalnexus.network.MultiblockGuiPage2ButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class MultiblockGuiPage2Screen extends AbstractContainerScreen<MultiblockGuiPage2Menu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private final MultiblockStructurePreview structurePreview = new MultiblockStructurePreview("reaction");
	ImageButton imagebutton_tab_dark;
	ImageButton imagebutton_tab_dark1;
	ImageButton imagebutton_tab_dark2;
	ImageButton imagebutton_tab_dark3;

	public MultiblockGuiPage2Screen(MultiblockGuiPage2Menu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 320;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		this.structurePreview.renderHoverTooltip(guiGraphics, this.font, this.world.registryAccess(), this.leftPos, this.topPos, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/multiblock_gui_page_1_overlay.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 330, 166, 330, 166);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tab_reaction.png"), this.leftPos + 186, this.topPos + -23, 0, 0, 32, 26, 32, 26);
		this.structurePreview.render(guiGraphics, this.font, this.world.registryAccess(), this.leftPos, this.topPos, mouseX, mouseY);
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.structurePreview.mouseClicked(mouseX, mouseY, button, this.world.registryAccess(), this.leftPos, this.topPos)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.structurePreview.mouseDragged(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.structurePreview.mouseReleased(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (this.structurePreview.mouseScrolled(mouseX, mouseY, deltaY, this.world.registryAccess(), this.leftPos, this.topPos)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_blutonium_reactor"), 141, 7, -3407668, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_uses_water_and_blutonium_ingots"), 141, 25, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_the_fluid_input_and_energy_outpu"), 141, 43, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_use_the_energy_input_block_to_po"), 141, 61, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_power_this_machine_it_doesnt"), 141, 70, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_matter_what_side_the_input_is_on"), 141, 79, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_2.label_as_long_as_it_is_in_the_center_r"), 141, 88, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		imagebutton_tab_dark = new ImageButton(this.leftPos + 285, this.topPos + -23, 32, 26, new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab.png")),
				e -> {
					int x = MultiblockGuiPage2Screen.this.x;
					int y = MultiblockGuiPage2Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage2ButtonMessage(2, x, y, z));
						MultiblockGuiPage2ButtonMessage.handleButtonAction(entity, 2, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark);
		imagebutton_tab_dark1 = new ImageButton(this.leftPos + 252, this.topPos + -23, 32, 26,
				new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_orepro_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab_orepro.png")), e -> {
					int x = MultiblockGuiPage2Screen.this.x;
					int y = MultiblockGuiPage2Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage2ButtonMessage(3, x, y, z));
						MultiblockGuiPage2ButtonMessage.handleButtonAction(entity, 3, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark1);
		imagebutton_tab_dark2 = new ImageButton(this.leftPos + 219, this.topPos + -23, 32, 26,
				new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_ultimasmelter_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab_ultimasmelter.png")), e -> {
					int x = MultiblockGuiPage2Screen.this.x;
					int y = MultiblockGuiPage2Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage2ButtonMessage(4, x, y, z));
						MultiblockGuiPage2ButtonMessage.handleButtonAction(entity, 4, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark2);
		imagebutton_tab_dark3 = new ImageButton(this.leftPos + 153, this.topPos + -23, 32, 26,
				new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_reactor_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab_reactor.png")), e -> {
					int x = MultiblockGuiPage2Screen.this.x;
					int y = MultiblockGuiPage2Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage2ButtonMessage(5, x, y, z));
						MultiblockGuiPage2ButtonMessage.handleButtonAction(entity, 5, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark3);
	}
}
