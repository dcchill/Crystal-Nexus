package net.crystalnexus.client.gui;

import net.crystalnexus.item.StructureTrackerItem;
import net.crystalnexus.network.StructureTrackerSelectMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class StructureTrackerScreen extends Screen {
	private final List<String> structures;
	private final String selected;
	private StructureList list;

	public StructureTrackerScreen(List<String> structures, String selected) {
		super(Component.translatable("gui.crystalnexus.structure_tracker.title"));
		this.structures = structures;
		this.selected = selected;
	}

	@Override
	protected void init() {
		EditBox search = new EditBox(this.font, this.width / 2 - 130, 30, 260, 20, Component.translatable("gui.crystalnexus.structure_tracker.search"));
		search.setHint(Component.translatable("gui.crystalnexus.structure_tracker.search"));
		search.setResponder(query -> this.list.filter(query));
		this.addRenderableWidget(search);

		this.list = new StructureList();
		this.addRenderableWidget(this.list);
		this.setInitialFocus(search);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(0, 0, this.width, this.height, 0xD0101116);
		guiGraphics.fill(this.width / 2 - 140, 8, this.width / 2 + 140, this.height - 8, 0xF0181B22);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
	}

	private class StructureList extends ObjectSelectionList<StructureEntry> {
		private StructureList() {
			super(StructureTrackerScreen.this.minecraft, 260, StructureTrackerScreen.this.height - 70, 56, 24);
			this.setX(StructureTrackerScreen.this.width / 2 - 130);
			this.filter("");
		}

		private void filter(String query) {
			String normalized = query.toLowerCase();
			this.clearEntries();
			for (String structure : StructureTrackerScreen.this.structures) {
				if (structure.toLowerCase().contains(normalized)) {
					this.addEntry(new StructureEntry(structure));
				}
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

	private class StructureEntry extends ObjectSelectionList.Entry<StructureEntry> {
		private final String structure;

		private StructureEntry(String structure) {
			this.structure = structure;
		}

		@Override
		public Component getNarration() {
			return Component.literal(this.structure);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button != 0) {
				return false;
			}
			PacketDistributor.sendToServer(new StructureTrackerSelectMessage(this.structure));
			StructureTrackerScreen.this.onClose();
			return true;
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			boolean current = this.structure.equals(StructureTrackerScreen.this.selected);
			guiGraphics.fill(left, top, left + width - 8, top + height, current ? 0x80507840 : hovering ? 0x60303A46 : 0x40202832);
			ResourceLocation id = ResourceLocation.tryParse(this.structure);
			String label = id == null ? this.structure : StructureTrackerItem.displayName(id);
			guiGraphics.drawString(StructureTrackerScreen.this.font, label, left + 4, top + 3, current ? 0xA8FFB0 : 0xFFFFFF, false);
			guiGraphics.drawString(StructureTrackerScreen.this.font, this.structure, left + 4, top + 13, 0x8A9BA8, false);
		}
	}
}
