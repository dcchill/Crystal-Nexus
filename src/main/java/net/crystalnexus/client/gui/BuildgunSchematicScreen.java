package net.crystalnexus.client.gui;

import net.crystalnexus.network.BuildgunSelectSchematicMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class BuildgunSchematicScreen extends Screen {
	private final List<String> schematics;
	private SchematicList list;

	public BuildgunSchematicScreen(List<String> schematics) {
		super(Component.literal("Buildgun Schematics"));
		this.schematics = schematics;
	}

	@Override
	protected void init() {
		this.list = new SchematicList();
		this.addRenderableWidget(this.list);
		this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
				.bounds(this.width / 2 - 50, this.height - 30, 100, 20)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(0, 0, this.width, this.height, 0xD0101116);
		guiGraphics.fill(this.width / 2 - 140, 8, this.width / 2 + 140, this.height - 8, 0xF0181B22);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
		if (this.schematics.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.literal("No .nbt files in schematics"), this.width / 2, this.height / 2, 0xA0A0A0);
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private class SchematicList extends ObjectSelectionList<SchematicEntry> {
		private SchematicList() {
			super(BuildgunSchematicScreen.this.minecraft, 260, BuildgunSchematicScreen.this.height - 58, 28, 24);
			this.setX(BuildgunSchematicScreen.this.width / 2 - 130);
			for (String schematic : BuildgunSchematicScreen.this.schematics) {
				this.addEntry(new SchematicEntry(schematic));
			}
		}

		@Override
		public int getRowWidth() {
			return 248;
		}

		@Override
		protected int getScrollbarPosition() {
			return this.getX() + this.width - 6;
		}
	}

	private class SchematicEntry extends ObjectSelectionList.Entry<SchematicEntry> {
		private final String schematic;

		private SchematicEntry(String schematic) {
			this.schematic = schematic;
		}

		@Override
		public Component getNarration() {
			return Component.literal(this.schematic);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button == 0) {
				PacketDistributor.sendToServer(new BuildgunSelectSchematicMessage(this.schematic));
				BuildgunSchematicScreen.this.onClose();
				return true;
			}
			return false;
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			guiGraphics.fill(left, top, left + width - 8, top + height, hovering ? 0x60303A46 : 0x40202832);
			int color = hovering ? 0xFFFFFF : 0xC8D7E1;
			guiGraphics.drawString(BuildgunSchematicScreen.this.font, this.schematic, left + 4, top + 6, color, false);
		}
	}
}
