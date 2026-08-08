package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

/**
 * UI accents shared by the tag screens: Catnip's streak geometry and PonderLib's own code-drawn
 * back-navigation echo. Every caller supplies its owning plugin's colour scheme.
 */
final class PonderStreak {

    static final int BACK_ECHO_Z = 390;
    private static final int ECHO_SIZE = 2;
    private static final int ECHO_WIDTH = 8;
    private static final int ECHO_HEIGHT = 16;
    private static final int ECHO_SPACING = 10;
    private static final int[][] ECHO_PIXELS = {
        {6, 0}, {4, 2}, {2, 4}, {0, 6},
        {0, 8}, {2, 10}, {4, 12}, {6, 14}
    };
    private static final float[] ECHO_ALPHA = {1F, .55F, .25F};

    private PonderStreak() {
    }

    static void render(GuiGraphics graphics, float angle, int x, int y, int breadth, int length, int color) {
        if (length <= 0) {
            return;
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90));

        int halfBreadth = breadth / 2;
        int split1 = (int) (.5 * length);
        int split2 = (int) (.75 * length);
        int c1 = scaleAlpha(color, .625F);
        int c2 = scaleAlpha(color, .5F);
        int c3 = scaleAlpha(color, .0625F);
        int c4 = color & 0x00FFFFFF;

        graphics.fillGradient(-halfBreadth, 0, halfBreadth, split1, c1, c2);
        graphics.fillGradient(-halfBreadth, split1, halfBreadth, split2, c2, c3);
        graphics.fillGradient(-halfBreadth, split2, halfBreadth, length, c3, c4);
        poseStack.popPose();
    }

    /**
     * PonderLib's own back-navigation texture: three hollow pixel chevrons behind the button. It is
     * deliberately code-drawn so every owning plugin's colour scheme remains intact.
     */
    static void backEcho(GuiGraphics graphics, int buttonX, int y, int z, int height, float phase,
                         int topColor, int bottomColor) {
        int topY = y + (height - ECHO_HEIGHT) / 2;
        int shift = backEchoShift(phase);

        for (int echo = 0; echo < ECHO_ALPHA.length; echo++) {
            int left = buttonX - ECHO_WIDTH + 1 - echo * ECHO_SPACING + shift;
            for (int[] pixel : ECHO_PIXELS) {
                int pixelY = pixel[1];
                float gradient = (pixelY + ECHO_SIZE / 2F) / ECHO_HEIGHT;
                int color = scaleAlpha(mix(topColor, bottomColor, gradient), ECHO_ALPHA[echo]);
                graphics.fill(left + pixel[0], topY + pixelY,
                    left + pixel[0] + ECHO_SIZE, topY + pixelY + ECHO_SIZE, z, color);
            }
        }
    }

    /** Pixel shift for a 0..1 entrance phase: -8 at the start, eased to rest at 0. */
    static int backEchoShift(float phase) {
        float clamped = Math.max(0F, Math.min(1F, phase));
        float remaining = 1F - clamped;
        float eased = 1F - remaining * remaining * remaining;
        return Math.round(-8F + 8F * eased);
    }

    private static int mix(int from, int to, float t) {
        int a = mixChannel(from >>> 24, to >>> 24, t);
        int r = mixChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = mixChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = mixChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int mixChannel(int from, int to, float t) {
        // Catnip's Color.mixColors truncates every interpolated channel.
        return (int) (from + (to - from) * t);
    }

    private static int scaleAlpha(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * factor);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
