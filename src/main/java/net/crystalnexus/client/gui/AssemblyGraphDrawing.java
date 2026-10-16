package net.crystalnexus.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Shared connection rendering for machine and drone graph editors. */
public final class AssemblyGraphDrawing {
    public static void cable(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int segments = Math.max(8, Math.min(30, Math.abs(x2 - x1) / 8));
        double control = Math.max(34, Math.abs(x2 - x1) * .45);
        int px = x1, py = y1;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments, u = 1 - t;
            int x = (int) Math.round(u*u*u*x1 + 3*u*u*t*(x1+control) + 3*u*t*t*(x2-control) + t*t*t*x2);
            int y = (int) Math.round(u*u*u*y1 + 3*u*u*t*y1 + 3*u*t*t*y2 + t*t*t*y2);
            segment(graphics, px, py, x, y, color); px = x; py = y;
        }
    }

    private static void segment(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2-x1), Math.abs(y2-y1));
        for (int i=0;i<=steps;i++) { int x=x1+(x2-x1)*i/Math.max(1,steps), y=y1+(y2-y1)*i/Math.max(1,steps); graphics.fill(x-1,y-1,x+2,y+2,color); }
    }

}
