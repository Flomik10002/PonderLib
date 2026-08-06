package dev.flomik.ponderlib.render;

import dev.flomik.ponderlib.api.Pointing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * A framed panel with a small tail on one side, so it reads as belonging to whatever it points at.
 * Used by the "next up" teaser and by the input-hint windows.
 */
public final class PonderSpeechBox {

    private static final int TAIL = 4;

    private PonderSpeechBox() {
    }

    /**
     * @param x,y     top-left of the panel's content box
     * @param pointing which side the tail sticks out of
     * @param alpha   0..1, fades the whole thing including the tail
     */
    public static void render(GuiGraphics graphics, int x, int y, int width, int height,
                              Pointing pointing, int z, int background, int borderTop, int borderBot, float alpha) {
        new PonderBoxElement()
            .withBackground(background)
            .gradientBorder(borderTop, borderBot)
            .at(x, y, z)
            .withBounds(width, height)
            .withAlpha(alpha)
            .render(graphics);

        int a = Math.round(0xFF * Mth.clamp(alpha, 0F, 1F));
        if (a <= 0) {
            return;
        }
        int tailColor = (a << 24) | (background & 0xFFFFFF);
        int centreX = x + width / 2;
        int centreY = y + height / 2;
        // Drawn one step in front of the frame: the frame goes out immediate-mode, these fills are
        // batched, so at the same z they would lose the depth test against it.
        int tailZ = z + 10;

        for (int i = 0; i < TAIL; i++) {
            switch (pointing) {
                case DOWN -> graphics.fill(centreX - TAIL + i, y + height + i,
                    centreX + TAIL - i, y + height + i + 1, tailZ, tailColor);
                case UP -> graphics.fill(centreX - TAIL + i, y - i - 1,
                    centreX + TAIL - i, y - i, tailZ, tailColor);
                case RIGHT -> graphics.fill(x + width + i, centreY - TAIL + i,
                    x + width + i + 1, centreY + TAIL - i, tailZ, tailColor);
                case LEFT -> graphics.fill(x - i - 1, centreY - TAIL + i,
                    x - i, centreY + TAIL - i, tailZ, tailColor);
            }
        }
    }
}
