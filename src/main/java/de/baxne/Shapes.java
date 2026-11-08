package de.baxne;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Zeichnet die Crosshair-Formen. */
public final class Shapes {
    public static final String[] NAMES = {"Aus", "Plus", "X", "Quadrat", "Raute", "Kreis", "Punkt", "Dreieck"};
    private Shapes() {}

    public static void draw(GuiGraphicsExtractor g, int shape, int cx, int cy, int s, int c) {
        switch (shape) {
            case 1 -> { g.fill(cx - s, cy - 1, cx + s + 1, cy + 1, c); g.fill(cx - 1, cy - s, cx + 1, cy + s + 1, c); }
            case 2 -> { for (int i = -s; i <= s; i++) { g.fill(cx + i, cy + i, cx + i + 2, cy + i + 2, c); g.fill(cx + i, cy - i - 1, cx + i + 2, cy - i + 1, c); } }
            case 3 -> { g.fill(cx - s, cy - s, cx + s + 1, cy - s + 1, c); g.fill(cx - s, cy + s, cx + s + 1, cy + s + 1, c);
                        g.fill(cx - s, cy - s, cx - s + 1, cy + s + 1, c); g.fill(cx + s, cy - s, cx + s + 1, cy + s + 1, c); }
            case 4 -> { for (int i = 0; i <= s; i++) { g.fill(cx - s + i, cy - i, cx - s + i + 1, cy - i + 1, c); g.fill(cx + s - i, cy - i, cx + s - i + 1, cy - i + 1, c);
                                                       g.fill(cx - s + i, cy + i, cx - s + i + 1, cy + i + 1, c); g.fill(cx + s - i, cy + i, cx + s - i + 1, cy + i + 1, c); } }
            case 5 -> { for (int a = 0; a < 72; a++) { double r = Math.toRadians(a * 5);
                        int x = cx + (int) Math.round(Math.cos(r) * s), y = cy + (int) Math.round(Math.sin(r) * s); g.fill(x, y, x + 1, y + 1, c); } }
            case 6 -> g.fill(cx - 2, cy - 2, cx + 2, cy + 2, c);
            case 7 -> { for (int i = 0; i <= s * 2; i++) { int half = i / 2; g.fill(cx - half, cy - s + i, cx + half + 1, cy - s + i + 1, c); } }
            default -> {}
        }
    }
}
