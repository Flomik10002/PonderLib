package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.api.PonderUIColors;
import dev.flomik.ponderlib.render.PonderBoxElement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * One of the scene screen's buttons: a framed box that brightens on hover, an optional toggle
 * "flash" state for buttons that represent something being on, a callback, and an optional
 * keyboard shortcut whose key name is drawn under the icon — that label is the whole reason
 * identify mode is discoverable at all rather than being a key nobody knows about.
 * <p>
 * Icons are drawn from {@link Icon}'s pixel masks rather than sprite images — plain code, no
 * image files, which keeps this project's "ships no art at all" licensing position intact.
 */
public class PonderButton extends AbstractWidget {

    public static final int SIZE = 20;

    /**
     * 8x8 pixel masks, drawn centred in the button. {@code '#'} is a lit pixel, anything else is
     * transparent. Deliberately readable as ASCII art so they can be edited by eye.
     */
    public enum Icon {
        CLOSE(
            "##    ##",
            "###  ###",
            " ###### ",
            "  ####  ",
            "  ####  ",
            " ###### ",
            "###  ###",
            "##    ##"),
        LEFT(
            "    #   ",
            "   ##   ",
            "  ###   ",
            " ####   ",
            " ####   ",
            "  ###   ",
            "   ##   ",
            "    #   "),
        RIGHT(
            "   #    ",
            "   ##   ",
            "   ###  ",
            "   #### ",
            "   #### ",
            "   ###  ",
            "   ##   ",
            "   #    "),
        // Rewind-to-start: a stop bar the triangle runs into.
        REPLAY(
            "#   #   ",
            "#  ##   ",
            "# ###   ",
            "#####   ",
            "#####   ",
            "# ###   ",
            "#  ##   ",
            "#   #   "),
        // Magnifier, for inspecting blocks.
        IDENTIFY(
            " ####   ",
            "#    #  ",
            "#    #  ",
            "#    #  ",
            "#    #  ",
            " ####   ",
            "     ## ",
            "      ##"),
        // Clock, for the slowed-down reading pace.
        SLOW(
            " ####   ",
            "#    #  ",
            "#  # #  ",
            "#  # #  ",
            "#  ###  ",
            "#    #  ",
            " ####   ",
            "        ");

        private static final int PIXELS = 8;

        private final String[] mask;

        Icon(String... mask) {
            this.mask = mask;
        }
    }

    // Colours live in api.PonderUIColors so a mod can match its own palette instead of being stuck
    // with hardcoded constants - the idle border is the same pair the scrubber frame uses.
    // A 5-tick ramp, applied per frame, same as the scrubber's fill.
    private static final float FADE_CHASE = 1F / 5;
    // The frame is drawn immediate-mode (see PonderBoxElement) at z=600, while the icon and the
    // shortcut label go through the BATCHED gui render type and are flushed later. At the default
    // z=0 they would lose the depth test against the frame's opaque background and vanish entirely,
    // leaving empty frames - same z-order pitfall as the scrubber's fill.
    private static final int FRAME_Z = 600;
    private static final int CONTENT_Z = 610;

    private final Icon icon;
    private final Runnable callback;
    @Nullable
    private KeyMapping shortcut;
    private boolean flashing;
    private float fade;

    public PonderButton(int x, int y, Icon icon, Component label, Runnable callback) {
        super(x, y, SIZE, SIZE, label);
        this.icon = icon;
        this.callback = callback;
        // Hand-drawn 8x8 icons can only carry so much meaning - a hover tooltip is what actually
        // makes each button self-describing.
        setTooltip(Tooltip.create(label));
    }

    /**
     * Marks this button as also triggerable by {@code shortcut}, and draws that key's name under the
     * icon. The button does NOT consume the key itself (the screen already handles it), this only
     * advertises it.
     */
    public PonderButton withShortcut(KeyMapping shortcut) {
        this.shortcut = shortcut;
        return this;
    }

    /**
     * Whether this button's feature is currently ON, used for the toggles (identify mode, slow
     * mode) so the button itself shows the state.
     */
    public void setFlashing(boolean flashing) {
        this.flashing = flashing;
    }

    public void tick() {
        float target = isHovered() || flashing ? 1F : 0F;
        fade += (target - fade) * FADE_CHASE;
        if (Math.abs(target - fade) < 1 / 512F) {
            fade = target;
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        callback.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean lit = isHovered() || flashing;
        new PonderBoxElement()
            .withBackground(PonderUIColors.buttonBackground())
            .gradientBorder(
                lerpColor(PonderUIColors.frameBorderTop(), PonderUIColors.buttonHoverBorderTop(), fade),
                lerpColor(PonderUIColors.frameBorderBottom(), PonderUIColors.buttonHoverBorderBottom(), fade))
            // Offset by the border inset so the frame lands exactly on the widget's own clickable
            // 20x20 box: PonderBoxElement inflates by borderOffset + 1 = 3 on every side.
            .at(getX() + 3, getY() + 3, FRAME_Z)
            .withBounds(SIZE - 6, SIZE - 6)
            .render(graphics);

        drawIcon(graphics, lit ? PonderUIColors.buttonIconLit() : PonderUIColors.buttonIconDim());

        if (shortcut != null && fade > 0.1F) {
            // The key name is drawn under the icon, faded in with the button. drawString has no z
            // parameter, so it gets in front of the frame by translating the pose.
            Font font = Minecraft.getInstance().font;
            String key = shortcut.getTranslatedKeyMessage().getString();
            int alpha = Math.max(0x05, Math.round(0xFF * Mth.clamp(fade, 0F, 1F)));
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, CONTENT_Z);
            graphics.drawString(font, key,
                getX() + width / 2 - font.width(key) / 2,
                getY() + height - 2, (alpha << 24) | 0xBBBBBB, false);
            graphics.pose().popPose();
        }
    }

    private void drawIcon(GuiGraphics graphics, int color) {
        int originX = getX() + (width - Icon.PIXELS) / 2;
        int originY = getY() + (height - Icon.PIXELS) / 2;
        for (int row = 0; row < icon.mask.length; row++) {
            String line = icon.mask[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(originX + col, originY + row, originX + col + 1, originY + row + 1, CONTENT_Z, color);
                }
            }
        }
    }

    private static int lerpColor(int from, int to, float t) {
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Mth.clamp(Math.round(from + (to - from) * t), 0, 0xFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
