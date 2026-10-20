package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import net.crystalnexus.world.inventory.FluidPackagerGUIMenu;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
import net.crystalnexus.network.FluidPackagerGUIButtonMessage;
import net.crystalnexus.init.CrystalnexusModScreens;

import java.util.stream.Collectors;
import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

public class FluidPackagerGUIScreen extends HolographicMachineScreen<FluidPackagerGUIMenu> implements CrystalnexusModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	Button button_dump;

	public FluidPackagerGUIScreen(FluidPackagerGUIMenu container, Inventory inventory, Component text) {
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
		machineBlockTank(guiGraphics, world, BlockPos.containing(x, y, z), 0,
			leftPos + 18, topPos + 14, 32, 56);
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
	}

	@Override
	public void init() {
		super.init();
		button_dump = Button.builder(Component.translatable("gui.crystalnexus.fluid_packager_gui.button_dump"), e -> {
			int x = FluidPackagerGUIScreen.this.x;
			int y = FluidPackagerGUIScreen.this.y;
			if (true) {
				PacketDistributor.sendToServer(new FluidPackagerGUIButtonMessage(0, x, y, z));
				FluidPackagerGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 60, this.topPos + 54, 46, 20).build();
		this.addRenderableWidget(button_dump);
	}
    @Override protected net.minecraft.core.BlockPos machinePosition() { return new net.minecraft.core.BlockPos(menu.x, menu.y, menu.z); }
    @Override protected String machineFallbackId() { return "fluid_packager"; }
}
