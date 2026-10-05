package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineUpgradeSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MachineUpgradeSlots {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("crystalnexus:textures/screens/upgradeslot.png");
    private MachineUpgradeSlots() {}

    public static void render(GuiGraphics graphics, AbstractContainerMenu menu, int left, int top) {
        for (var slot : menu.slots)
            if (slot instanceof MachineUpgradeSlot && slot.isActive())
                graphics.blit(TEXTURE, left + slot.x - 7, top + slot.y - 8, 0, 0, 32, 32, 32, 32);
    }
}
