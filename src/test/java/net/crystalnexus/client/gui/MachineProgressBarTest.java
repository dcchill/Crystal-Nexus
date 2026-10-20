package net.crystalnexus.client.gui;

import net.crystalnexus.item.GradientItemName;
import net.crystalnexus.item.GradientItemName.Palette;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MachineProgressBarTest {
    @Test
    void spriteFramesFillBothOrientationsAndClampAtTheirEnds() {
        assertEquals(0, MachineProgressBar.filledPixels(-32, 30));
        assertEquals(0, MachineProgressBar.filledPixels(0, 30));
        assertEquals(15, MachineProgressBar.filledPixels(160, 30));
        assertEquals(30, MachineProgressBar.filledPixels(320, 30));
        assertEquals(30, MachineProgressBar.filledPixels(352, 30));
    }

    @Test
    void fillColorsMatchTheNameGradientAtBothEndsAndTheMiddle() {
        for (Palette palette : Palette.values()) {
            var name = GradientItemName.gradient(Component.literal("ABC"), palette).getSiblings();
            assertEquals(name.get(0).getStyle().getColor().getValue(), palette.colorAt(0));
            assertEquals(name.get(1).getStyle().getColor().getValue(), palette.colorAt(0.5F));
            assertEquals(name.get(2).getStyle().getColor().getValue(), palette.colorAt(1));
        }
    }
}
