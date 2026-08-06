package dev.flomik.ponderlib.render;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws a small icon from an ASCII pixel mask ({@code '#'} is a lit pixel, anything else
 * transparent) - plain code, no image assets. Shared by the scene screen's buttons and its
 * input-hint windows so both draw icons the same way.
 */
public final class PonderIconMask {

    private PonderIconMask() {
    }

    public static int width(String[] mask) {
        int width = 0;
        for (String row : mask) {
            width = Math.max(width, row.length());
        }
        return width;
    }

    public static int height(String[] mask) {
        return mask.length;
    }

    /**
     * @param z     needed whenever the icon sits over something drawn immediate-mode: batched fills at
     *              the default z lose the depth test against it and vanish entirely.
     * @param color 0xAARRGGBB
     */
    public static void render(GuiGraphics graphics, String[] mask, int x, int y, int z, int color) {
        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, z, color);
                }
            }
        }
    }
}
