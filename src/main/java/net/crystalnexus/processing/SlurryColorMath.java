package net.crystalnexus.processing;

public final class SlurryColorMath {
    private SlurryColorMath() {}

    public static int averageAbgr(int[] pixels, int fallback) {
        long red = 0, green = 0, blue = 0, weight = 0;
        for (int pixel : pixels) {
            int alpha = pixel >>> 24;
            red += (long) (pixel & 255) * alpha;
            green += (long) (pixel >>> 8 & 255) * alpha;
            blue += (long) (pixel >>> 16 & 255) * alpha;
            weight += alpha;
        }
        if (weight == 0) return fallback;
        int r = (int) (red / weight), g = (int) (green / weight), b = (int) (blue / weight);
        int luminance = (r * 77 + g * 150 + b * 29) >> 8;
        r = clamp(luminance + (r - luminance) * 125 / 100);
        g = clamp(luminance + (g - luminance) * 125 / 100);
        b = clamp(luminance + (b - luminance) * 125 / 100);
        r += (255 - r) * 8 / 100;
        g += (255 - g) * 8 / 100;
        b += (255 - b) * 8 / 100;
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    private static int clamp(int channel) { return Math.max(0, Math.min(255, channel)); }
}
