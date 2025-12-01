package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.MultiblockGuiPage4Menu;
import net.crystalnexus.procedures.MultiblockGuiPage1WhileThisGUIIsOpenTickProcedure;
import net.crystalnexus.network.MultiblockGuiPage4ButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class MultiblockGuiPage4Screen extends AbstractContainerScreen<MultiblockGuiPage4Menu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	Button button_empty;
	Button button_empty1;
	ImageButton imagebutton_tab_dark;
	ImageButton imagebutton_tab_dark1;
	ImageButton imagebutton_tab_dark2;
	ImageButton imagebutton_tab_dark3;

	public MultiblockGuiPage4Screen(MultiblockGuiPage4Menu container, Inventory inventory, Component text) {
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
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/multiblock_gui_page_1_overlay.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 330, 166, 330, 166);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/tab_orepro.png"), this.leftPos + 252, this.topPos + -23, 0, 0, 32, 26, 32, 26);
		guiGraphics.blit(ResourceLocation.parse("crystalnexus:textures/screens/oreprosprite.png"), this.leftPos + 6, this.topPos + 7, 0, Mth.clamp((int) MultiblockGuiPage1WhileThisGUIIsOpenTickProcedure.execute(world, x, y, z) * 128, 0, 256), 128,
				128, 128, 384);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_blutonium_reactor"), 141, 7, -3407668, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_uses_water_and_blutonium_ingots"), 141, 25, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_can_smelt_four_stacks_at_once"), 141, 43, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_the_energy_input_can_be_placed"), 141, 52, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_any_side_in_the_middle_row_of_bl"), 141, 61, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_automatically_combines_nuggets"), 141, 79, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.crystalnexus.multiblock_gui_page_4.label_into_ingots"), 141, 88, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		button_empty = Button.builder(Component.translatable("gui.crystalnexus.multiblock_gui_page_4.button_empty"), e -> {
			int x = MultiblockGuiPage4Screen.this.x;
			int y = MultiblockGuiPage4Screen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(0, x, y, z));
				MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 96, this.topPos + 142, 30, 20).build();
		this.addRenderableWidget(button_empty);
		button_empty1 = Button.builder(Component.translatable("gui.crystalnexus.multiblock_gui_page_4.button_empty1"), e -> {
			int x = MultiblockGuiPage4Screen.this.x;
			int y = MultiblockGuiPage4Screen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(1, x, y, z));
				MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + 15, this.topPos + 142, 30, 20).build();
		this.addRenderableWidget(button_empty1);
		imagebutton_tab_dark = new ImageButton(this.leftPos + 285, this.topPos + -23, 32, 26, new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab.png")),
				e -> {
					int x = MultiblockGuiPage4Screen.this.x;
					int y = MultiblockGuiPage4Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(2, x, y, z));
						MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 2, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark);
		imagebutton_tab_dark1 = new ImageButton(this.leftPos + 219, this.topPos + -23, 32, 26,
				new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_ultimasmelter_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab_ultimasmelter.png")), e -> {
					int x = MultiblockGuiPage4Screen.this.x;
					int y = MultiblockGuiPage4Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(3, x, y, z));
						MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 3, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
				guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addRenderableWidget(imagebutton_tab_dark1);
		imagebutton_tab_dark2 = new ImageButton(this.leftPos + 186, this.topPos + -23, 32, 26,
				new WidgetSprites(ResourceLocation.parse("crystalnexus:textures/screens/tab_reaction_dark.png"), ResourceLocation.parse("crystalnexus:textures/screens/tab_reaction.png")), e -> {
					int x = MultiblockGuiPage4Screen.this.x;
					int y = MultiblockGuiPage4Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(4, x, y, z));
						MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 4, x, y, z);
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
					int x = MultiblockGuiPage4Screen.this.x;
					int y = MultiblockGuiPage4Screen.this.y;
					if (true) {
						PacketDistributor.sendToServer(new MultiblockGuiPage4ButtonMessage(5, x, y, z));
						MultiblockGuiPage4ButtonMessage.handleButtonAction(entity, 5, x, y, z);
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