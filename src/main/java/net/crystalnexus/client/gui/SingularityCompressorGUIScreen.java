package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import net.crystalnexus.world.inventory.SingularityCompressorGUIMenu;
import net.crystalnexus.network.SingularityCompressorGUIButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class SingularityCompressorGUIScreen extends HolographicMachineScreen<SingularityCompressorGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	Button button_dump;

	public SingularityCompressorGUIScreen(SingularityCompressorGUIMenu container, Inventory inventory, Component text) {
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

		machineProgress(guiGraphics, MachineGuiWidgets.progress(world, new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z)));
		BlockEntity compressor = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
		var input = MachineGuiWidgets.slot(menu, 0);
		ResourceLocation selected = ghostItemId(compressor == null ? "" : compressor.getPersistentData().getString("setItem"), input.hasItem());
		if (selected != null && BuiltInRegistries.ITEM.containsKey(selected)) {
			ItemStack ghost = BuiltInRegistries.ITEM.get(selected).getDefaultInstance();
			if (!ghost.isEmpty()) {
				int slotX = leftPos + input.x, slotY = topPos + input.y;
				guiGraphics.renderFakeItem(ghost, slotX, slotY);
				guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), slotX, slotY, slotX + 16, slotY + 16, 0x880A1424);
				MachineGuiWidgets.value(MachineGuiWidgets.slotFrame(menu, 0, leftPos, topPos), ghost.getHoverName());
			}
		}

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
		BlockEntity compressor = world.getBlockEntity(machinePosition());
		int items = compressor == null ? 0 : (int) compressor.getPersistentData().getDouble("item");
		int capacity = compressor == null ? 0 : (int) compressor.getPersistentData().getDouble("maxItem");
		var input = MachineGuiWidgets.slot(menu, 0);
		MachineGuiWidgets.label(guiGraphics, font,
			Component.translatable("gui.crystalnexus.singularity_compressor_gui.items", items, capacity), input.x - 1, input.y - 14, 0xE3F3FF, false);
	}

	static ResourceLocation ghostItemId(String configuredItem, boolean inputOccupied) {
		if (inputOccupied || configuredItem.isBlank() || configuredItem.equals("none")) return null;
		return ResourceLocation.tryParse(configuredItem);
	}

	@Override
	public void init() {
		super.init();
		button_dump = Button.builder(Component.translatable("gui.crystalnexus.singularity_compressor_gui.button_dump"), e -> {
			int x = SingularityCompressorGUIScreen.this.x;
			int y = SingularityCompressorGUIScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new SingularityCompressorGUIButtonMessage(0, x, y, z));
				SingularityCompressorGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 65, this.topPos + 55, 46, 20).build();
		this.addRenderableWidget(button_dump);
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "singularity_compressor"; }
}
