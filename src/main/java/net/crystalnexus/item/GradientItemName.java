package net.crystalnexus.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.Color;

public final class GradientItemName {
    private GradientItemName() { }

    public enum Palette {
        IRON(0xE8E8E8, 0xA0A0A0),
        AZURINE(0xAFDAFC, 0x6F97E7),
        OBSIDRAX(0x79557F, 0x633A6D),
        FERROSTEEL(0xD6B6B7, 0xA15D58),
        METEORITE_ALLOY(0xFFD252, 0xF78327),
        CHLOROPHYTE(0x37EF00, 0x31D500),
        INVERTIUM(0xFF580B, 0xDA3300),
        ANCIENT_CRYSTAL(0xBC75DE, 0x8D4AB7),
        CARBON_FIBER(0x3D3C3D, 0x2B2C2B),
        ENGINEERED_FLESH(0xFFA6B9, 0xB8253E),
        BLUTONIUM(0x675CFF, 0x5448F2),
        EE_MATTER(0x0A8555, 0x06D39C);

        private final int light;
        private final int dark;

        Palette(int light, int dark) {
            this.light = light;
            this.dark = dark;
        }

        public int midpoint() { return blend(light, dark, 0.5F); }
    }

    static Component yellowToOrange(Component name) {
        return gradient(name, 0xFFE450, 0xFF8A00);
    }

    public static MutableComponent gradient(Component name, Palette palette) {
        return gradient(name, palette.light, palette.dark);
    }

    public static MutableComponent rainbow(Component name) {
        int[] chars = name.getString().codePoints().toArray();
        float phase = (System.currentTimeMillis() % 4000L) / 4000.0F;
        MutableComponent result = Component.empty();
        for (int i = 0; i < chars.length; i++) {
            int color = Color.HSBtoRGB((phase + (float) i / Math.max(1, chars.length)) % 1.0F, 0.85F, 1.0F) & 0xFFFFFF;
            result.append(Component.literal(new String(Character.toChars(chars[i]))).withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    private static MutableComponent gradient(Component name, int light, int dark) {
        int[] chars = name.getString().codePoints().toArray();
        MutableComponent result = Component.empty();
        for (int i = 0; i < chars.length; i++) {
            int color = blend(light, dark, chars.length <= 1 ? 0 : (float) i / (chars.length - 1));
            result.append(Component.literal(new String(Character.toChars(chars[i]))).withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    private static int blend(int light, int dark, float progress) {
        int red = Math.round(((light >> 16) & 255) * (1 - progress) + ((dark >> 16) & 255) * progress);
        int green = Math.round(((light >> 8) & 255) * (1 - progress) + ((dark >> 8) & 255) * progress);
        int blue = Math.round((light & 255) * (1 - progress) + (dark & 255) * progress);
        return red << 16 | green << 8 | blue;
    }
}
