package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.registration.PonderTag;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * One of the scene screen's buttons: a framed box that brightens on hover, an optional toggle
 * "flash" state for buttons that represent something being on, a callback, and an optional
 * keyboard shortcut whose key name is drawn under the icon — that label is the whole reason
 * identify mode is discoverable at all rather than being a key nobody knows about.
 * <p>
 * Icons are drawn either from {@link Icon}'s pixel masks (plain code, no image files, which keeps
 * this project's "ships no art at all" licensing position intact) or, via {@link #showingTag}, as a
 * real {@link ItemStack} rendered at 1.5x scale — the same frame, hover brighten and click handling,
 * just what {@code net.createmod.ponder.foundation.ui.PonderButton#showingTag} looks like in the
 * real Create/Ponder: a tag's own button is this exact widget, not a separate visual.
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

    // Colours come from whichever scene's own PonderColorScheme is active (see #colors) rather
    // than hardcoded constants - the idle border is the same pair the scrubber frame uses.
    // A 5-tick ramp, applied per frame, same as the scrubber's fill.
    private static final float FADE_CHASE = 1F / 5;
    // The frame is drawn immediate-mode (see PonderBoxElement) at z=600, while the icon and the
    // shortcut label go through the BATCHED gui render type and are flushed later. At the default
    // z=0 they would lose the depth test against the frame's opaque background and vanish entirely,
    // leaving empty frames - same z-order pitfall as the scrubber's fill.
    private static final int FRAME_Z = 600;
    private static final int CONTENT_Z = 610;

    // Scale a tag's real item icon is drawn at - Create's own PonderButton#showingTag uses this
    // exact factor (GuiGameElement.of(item).scale(1.5f)), which is what makes a tag button's icon
    // read as slightly larger than the plain 16x16 vanilla slot size instead of looking undersized
    // inside the same 20x20 frame the pixel-mask buttons use.
    private static final float ITEM_ICON_SCALE = 1.5F;
    private static final int ITEM_ICON_SIZE = 16;

    @Nullable
    private final Icon icon;
    @Nullable
    private final ItemStack itemIcon;
    private final Runnable callback;
    // A supplier, not a fixed value: a button is built once in PonderUI#init, but which scene (and
    // so whose PonderColorScheme) is active can change afterwards (paging, the cross-fade slide).
    private final Supplier<PonderColorScheme> colors;
    @Nullable
    private KeyMapping shortcut;
    private boolean flashing;
    private float fade;

    public PonderButton(int x, int y, Icon icon, Component label, Runnable callback, Supplier<PonderColorScheme> colors) {
        super(x, y, SIZE, SIZE, label);
        this.icon = icon;
        this.itemIcon = null;
        this.callback = callback;
        this.colors = colors;
        // Hand-drawn 8x8 icons can only carry so much meaning - a hover tooltip is what actually
        // makes each button self-describing.
        setTooltip(Tooltip.create(label));
    }

    private PonderButton(int x, int y, ItemStack itemIcon, Component label, Runnable callback, Supplier<PonderColorScheme> colors) {
        super(x, y, SIZE, SIZE, label);
        this.icon = null;
        this.itemIcon = itemIcon;
        this.callback = callback;
        this.colors = colors;
        setTooltip(Tooltip.create(label));
    }

    /**
     * A tag's own button - the exact widget {@code PonderTag} entries render as in real Create:
     * this frame, this hover brighten, {@code tag.icon()} drawn at 1.5x scale instead of a
     * pixel-mask {@link Icon}. The tooltip shows the description when the tag has one, the title
     * otherwise - same fallback {@code PonderTagButton} used before this became a plain
     * {@code PonderButton}.
     */
    public static PonderButton showingTag(int x, int y, PonderTag tag, Runnable callback, Supplier<PonderColorScheme> colors) {
        PonderButton button = new PonderButton(x, y, tag.icon(), tag.title(), callback, colors);
        if (!tag.description().getString().isBlank()) {
            button.setTooltip(Tooltip.create(tag.description()));
        }
        return button;
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
        PonderColorScheme scheme = colors.get();
        new PonderBoxElement()
            .withBackground(scheme.buttonBackground())
            .gradientBorder(
                lerpColor(scheme.frameBorderTop(), scheme.buttonHoverBorderTop(), fade),
                lerpColor(scheme.frameBorderBottom(), scheme.buttonHoverBorderBottom(), fade))
            // Offset by the border inset so the frame lands exactly on the widget's own clickable
            // 20x20 box: PonderBoxElement inflates by borderOffset + 1 = 3 on every side.
            .at(getX() + 3, getY() + 3, FRAME_Z)
            .withBounds(SIZE - 6, SIZE - 6)
            .render(graphics);

        if (icon != null) {
            drawIcon(graphics, lit ? scheme.buttonIconLit() : scheme.buttonIconDim());
        } else if (itemIcon != null && !itemIcon.isEmpty()) {
            // A real item's own texture already carries its colour - unlike the pixel masks above,
            // this is never tinted by buttonIconLit/Dim, only the frame around it brightens on hover.
            drawItemIcon(graphics);
        }

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

    /**
     * {@code itemIcon} at {@link #ITEM_ICON_SCALE}, centred in the button - vanilla's
     * {@code renderItem} always draws a fixed 16x16, so centring after scaling (rather than trying
     * to reproduce Create's own hardcoded {@code at(-4, -4)} offset, tuned for its own item-element
     * wrapper) is what keeps this centred regardless of {@link #SIZE} or the scale factor.
     */
    private void drawItemIcon(GuiGraphics graphics) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        float offset = (SIZE - ITEM_ICON_SIZE * ITEM_ICON_SCALE) / 2F;
        poseStack.translate(getX() + offset, getY() + offset, CONTENT_Z);
        poseStack.scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE, 1F);
        graphics.renderItem(itemIcon, 0, 0);
        poseStack.popPose();
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
