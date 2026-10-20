package net.crystalnexus.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Clear baked control frames and redraw them at their logical inventory-grid positions. */
public final class MachineGuiLegacy {
    private MachineGuiLegacy() {}
    public static void slots(GuiGraphics g, AbstractContainerMenu menu, int left, int top,
            ResourceLocation texture, int textureWidth, int textureHeight) {
        int[][] legacy = switch (menu.getClass().getSimpleName()) {
            case "BioMGuiMenu", "BioSIMGuiMenu" -> new int[][]{{0,43,35},{1,115,35}};
            case "BioMCGuiMenu" -> new int[][]{{0,43,35},{1,115,35},{3,79,17}};
            case "MawMenu" -> new int[][]{{0,62,33},{1,80,33},{2,98,33}};
            case "MasticatorMenu" -> new int[][]{{1,80,35},{2,134,26},{3,134,44}};
            case "HemolyzerMenu", "HemochanterMenu" -> new int[][]{{0,80,35}};
            case "SolarEngineMenu" -> new int[][]{{0,80,37}};
            case "CometForgeMenu", "GravitationalArrayMenu" -> new int[][]{{0,53,37},{1,107,37},{2,80,9},{3,80,63},{4,80,37}};
            case "SolarSimulatorMenu" -> new int[][]{{0,53,37},{1,107,37},{2,80,11},{3,80,63},{4,80,37}};
            case "CelestialGearForgeMenu" -> new int[][]{{0,45,11},{1,70,13},{2,72,37},{3,70,60},{4,45,63},{5,21,60},{6,18,37},{7,21,13},{8,45,37},{9,134,37}};
            default -> new int[0][];
        };
        for (int[] old : legacy) {
            var slot = find(menu, old[0]);
            if (slot != null)
                g.blit(texture, left + old[1] - 3, top + old[2] - 3, 22, 22, 10f, 10f, 1, 1, textureWidth, textureHeight);
        }
        int[] tank = switch (menu.getClass().getSimpleName()) {
            case "CometForgeMenu", "GravitationalArrayMenu", "SolarEngineMenu" -> new int[]{7,29,16,34};
            case "HemolyzerMenu", "HemochanterMenu" -> new int[]{134,26,15,33};
            default -> null;
        };
        if (tank != null) g.blit(texture, left + tank[0] - 1, top + tank[1] - 1,
                tank[2] + 2, tank[3] + 2, 10f, 10f, 1, 1, textureWidth, textureHeight);
        String name = menu.getClass().getSimpleName();
        boolean biological = name.startsWith("Bio");
        boolean celestial = name.equals("CometForgeMenu") || name.equals("GravitationalArrayMenu")
                || name.equals("SolarSimulatorMenu") || name.equals("SolarEngineMenu") || name.equals("CelestialGearForgeMenu");
        int border = biological ? 0xFFE8E8E8 : celestial ? 0xFFFFDF59 : 0xFFAD6872;
        int interior = biological ? 0xFF888888 : celestial ? 0xFF180C39 : 0xFF211017;
        for (int[] old : legacy) {
            var slot = find(menu, old[0]);
            if (slot != null) {
                g.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, interior);
                g.renderOutline(left + slot.x - 1, top + slot.y - 1, 18, 18, border);
            }
        }
    }

    private static net.minecraft.world.inventory.Slot find(AbstractContainerMenu menu, int index) {
        for (var slot : menu.slots) if (!(slot.container instanceof Inventory) && slot.getSlotIndex() == index) return slot;
        return null;
    }
}
