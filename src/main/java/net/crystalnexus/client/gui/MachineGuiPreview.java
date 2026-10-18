package net.crystalnexus.client.gui;

import net.crystalnexus.processing.MachineTier;
import net.crystalnexus.world.inventory.MachineGuiLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import java.nio.file.Files;
import java.nio.file.Path;

/** Developer-only real-renderer screenshots: gradlew runClient -PguiPreview. Never opens a world. */
@EventBusSubscriber(modid = "crystalnexus", value = Dist.CLIENT)
public final class MachineGuiPreview {
    private static int ticks;
    private static boolean started;
    private MachineGuiPreview() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String output = System.getProperty("crystalnexus.guiPreview");
        if (output == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            started = true;
            mc.options.guiScale().set(1);
            mc.resizeDisplay();
            mc.setScreen(new Gallery(false));
        }
        if (!(mc.screen instanceof Gallery) || ++ticks < 40) return;
        try {
            Files.createDirectories(Path.of(output));
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(Path.of(output, ticks == 40 ? "tier-themes.png" : "complex-layouts.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot write GUI preview", exception);
        }
        if (ticks == 40) { mc.setScreen(new Gallery(true)); ticks = -40; }
        else mc.stop();
    }

    private static final class Gallery extends Screen {
        private final boolean complex;
        private int frames;
        Gallery(boolean complex) { super(Component.literal("Machine GUI renderer previews")); this.complex = complex; }
        @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFF030812);
            float scale = Math.min(width / 760f, height / (complex ? 560f : 740f));
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1);
            g.drawString(font, complex ? "COMPLEX LAYOUT FIXTURES / live Minecraft renderer" : "MATERIAL TIERS / live Minecraft renderer", 20, 10, 0xECF5FF, false);
            if (!complex) for (int tier = 0; tier < 9; tier++) {
                int x = 44 + tier % 3 * 244, y = 76 + tier / 3 * 230;
                PreviewMenu menu = new PreviewMenu(false, MachineTier.forLevel(tier).upgradeSlots());
                draw(g, menu, x, y, 176, 166, tier, "Refinery");
                for (int i = 0; i < 2; i++) tank(g, x + MachineGuiLayout.TWO_TANK_X[i], y + MachineGuiLayout.TANK_Y, 16, 34, tier, (tier % 3) * 2000);
            } else {
                PreviewMenu fluid = new PreviewMenu(true, 5);
                draw(g, fluid, 44, 76, 176, 166, 3, "Fluid Chemical Reaction Chamber");
                for (int i = 0; i < 3; i++) {
                    int x = 44 + MachineGuiLayout.THREE_TANK_X[i];
                    tank(g, x, 76 + MachineGuiLayout.TANK_Y, 16, 34, 3, i * 2000);
                    MachineGuiTheme.frame(g, x, 76 + MachineGuiLayout.PURGE_Y, 16, 12, MachineGuiTheme.accent(3));
                    g.drawString(font, "x", x + 5, 87, 0xFFFFFF, false);
                }
                PreviewMenu plasma = new PreviewMenu();
                for (int row = 0; row < 5; row++) for (int col = 0; col < 9; col++) plasma.machineSlot(8 + col * 18, 28 + row * 18);
                plasma.inventory(142);
                draw(g, plasma, 330, 76, 356, 225, 6, "Plasma Generator");
                tank(g, 515, 202, 18, 82, 6, 3200);
                g.drawString(font, "Output: 120,000 FE/t", 511, 108, 0x8BD8FF, false);
                g.drawString(font, "Demand: 64 mB/t", 511, 124, 0xD4ABFF, false);
                g.drawString(font, "Argon", 542, 204, 0xD4ABFF, false);
                PreviewMenu assembler = new PreviewMenu();
                assembler.machineSlot(43, 35); assembler.machineSlot(115, 35); assembler.inventory(84);
                draw(g, assembler, 44, 350, 176, 166, 1, "Parts Assembler");
                for (int i = 0; i < 3; i++) { var r = MachineGuiLayout.assemblerMode(i); MachineGuiTheme.frame(g, 44 + r.x(), 350 + r.y(), 18, 18, MachineGuiTheme.accent(1)); }
                g.drawString(font, "Empty / half / full tanks; maximum upgrade bays", 300, 370, 0xB9D7ED, false);
                g.drawString(font, "Fixtures verify rendering; they are not gameplay tests.", 300, 390, 0xB9D7ED, false);
            }
            g.pose().popPose();
            frames++;
        }
        private void draw(GuiGraphics g, PreviewMenu menu, int x, int y, int w, int h, int tier, String label) {
            MachineGuiTheme.background(g, menu, null, null, Component.literal(label), x, y, w, h, tier);
            var r = MachineGuiLayout.progress(w, 0, 1);
            MachineGuiTheme.bar(g, x + r.x(), y + r.y(), r.width(), r.height(), 0.65, MachineGuiTheme.accent(tier));
            for (var slot : menu.slots) if (slot.hasItem()) g.renderItem(slot.getItem(), x + slot.x, y + slot.y);
        }
        private void tank(GuiGraphics g, int x, int y, int w, int h, int tier, int amount) {
            MachineGuiTheme.frame(g, x - 1, y - 1, w + 2, h + 2, MachineGuiTheme.accent(tier));
            FluidTankRenderer.drawClamped(g, new FluidStack(Fluids.WATER, amount), 4000, x, y, w, h);
        }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class PreviewMenu extends AbstractContainerMenu {
        private final SimpleContainer machine = new SimpleContainer(128);
        private int nextSlot;
        PreviewMenu() { super(null, 0); }
        PreviewMenu(boolean threeTanks, int upgrades) {
            this();
            for (int x : threeTanks ? MachineGuiLayout.THREE_TANK_X : MachineGuiLayout.TWO_TANK_X) machineSlot(x, MachineGuiLayout.TANK_SLOT_Y);
            for (int i = 0; i < upgrades; i++) machineSlot(180, 8 + i * 32);
            inventory(84);
            machine.setItem(0, new ItemStack(Items.IRON_INGOT));
        }
        void machineSlot(int x, int y) { addSlot(new Slot(machine, nextSlot++, x, y)); }
        void inventory(int y) {
            Inventory inventory = new Inventory(null);
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, y + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, y + 58));
        }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    }
}
