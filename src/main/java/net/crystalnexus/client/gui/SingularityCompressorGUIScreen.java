package net.crystalnexus.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.crystalnexus.world.inventory.SingularityCompressorGUIMenu;
import net.crystalnexus.procedures.ProgressDisplayProcedure;
import net.crystalnexus.procedures.ItemDisplayProcedure;
import net.crystalnexus.procedures.EnergyDisplayProcedure;
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
		if (isHovering(6, 18, 32, 32, mouseX, mouseY)) {
			BlockEntity compressor = world.getBlockEntity(net.minecraft.core.BlockPos.containing(x, y, z));
			if (compressor != null) guiGraphics.renderTooltip(font, Component.translatable(
					"gui.crystalnexus.singularity_compressor_gui.items", (int) compressor.getPersistentData().getDouble("item"),
					(int) compressor.getPersistentData().getDouble("maxItem")), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		machineBackground(guiGraphics);

		machineProgress(guiGraphics, (Mth.clamp((int) ItemDisplayProcedure.execute(world, x, y, z) * 32, 0, 320)) / 320.0);
		machineProgress(guiGraphics, (Mth.clamp((int) ProgressDisplayProcedure.execute(world, x, y, z) * 32, 0, 320)) / 320.0);

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
    @Override protected int machineProgressCount() { return 2; }
    @Override protected String machineFallbackId() { return "singularity_compressor"; }
}
