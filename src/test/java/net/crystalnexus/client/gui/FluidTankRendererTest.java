package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FluidTankRendererTest {
    @Test void fullTankLeavesBothBevelLayersAndRoundedCornersVisible() {
        Rect tank = new Rect(10, 20, 34, 52);
        var clips = FluidTankRenderer.roundedFillClips(tank, 50);
        assertTrue(clips.contains(new Rect(12, 21, 30, 1)));
        assertTrue(clips.contains(new Rect(12, 70, 30, 1)));
        assertTrue(clips.contains(new Rect(11, 22, 32, 48)));
    }

    @Test void partialAndFullFillsMatchTheRoundedInteriorWithoutOverlappingBands() {
        for (Rect tank : List.of(new Rect(10, 20, 16, 34), new Rect(10, 20, 34, 52), new Rect(0, 0, 80, 100))) {
            int width = tank.width() - 2, height = tank.height() - 2;
            int radius = MachineGuiTheme.frameRadius(tank.width() + 2, tank.height() + 2) - 2;
            for (int fill : new int[]{0, 1, height / 2, height, height + 10}) {
                var pixels = new HashSet<String>();
                for (Rect clip : FluidTankRenderer.roundedFillClips(tank, fill))
                    for (int y = clip.y(); y < clip.bottom(); y++) for (int x = clip.x(); x < clip.right(); x++)
                        assertTrue(pixels.add(x + "," + y), "Clipping bands must not overdraw each other");
                for (int y = tank.y(); y < tank.bottom(); y++) for (int x = tank.x(); x < tank.right(); x++) {
                    int row = y - tank.y() - 1;
                    int corner = Math.min(row, height - row - 1);
                    int inset = corner >= 0 && corner < radius ? MachineGuiTheme.cornerInset(radius, corner) : 0;
                    boolean expected = row >= 0 && row < height && row >= height - fill
                        && x >= tank.x() + 1 + inset && x < tank.x() + 1 + width - inset;
                    assertEquals(expected, pixels.contains(x + "," + y), "Fluid must match the visible rounded interior");
                }
            }
        }
        assertTrue(FluidTankRenderer.roundedFillClips(new Rect(0, 0, 2, 10), 10).isEmpty());
    }
}
