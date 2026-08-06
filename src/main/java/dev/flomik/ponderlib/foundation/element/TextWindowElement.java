package dev.flomik.ponderlib.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.element.PonderOverlayElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.ui.PonderUI;
import dev.flomik.ponderlib.render.PonderBoxElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A text window anchored to a point in the scene:
 * <ul>
 * <li>{@link #setPointAt} anchors the window to a point in the SCENE — projected to the screen
 * through the scene's own camera matrix ({@link PonderUI#sceneToScreen}) — and draws a leader line
 * from that point to the window, whose length is multiplied by the fade so it grows out of the
 * block.
 * <li>The window sits on the right ({@code width * lerp(yDiff², 6/8, 5/8)}), vertically level with
 * whatever it points at, and {@link #setNearScene} pulls it in closer.
 * <li>Text wraps to {@code min(width - targetX, 180)} inside a framed {@link PonderBoxElement}, sized
 * to the wrapped text rather than assuming one line.
 * <li>Colour comes from a {@link PonderPalette}, mixed halfway toward {@code 0xffffdd} to brighten
 * it, with alpha scaled by the fade.
 * </ul>
 */
public class TextWindowElement implements PonderOverlayElement {

    private static final int BORDER_TOP = 0x607A6000;
    private static final int BORDER_BOT = 0x207A6000;
    private static final int BACKGROUND = 0xFF000000;
    private static final int TEXT_MIX_TOWARD = 0xFFFFDD;
    private static final int MAX_TEXT_WIDTH = 180;

    private Component text = Component.empty();
    private boolean visible;
    private float fade;

    @Nullable
    private Vec3 pointAt;
    private boolean nearScene;
    private int independentY;
    private PonderPalette palette = PonderPalette.WHITE;

    public void setText(Component text) {
        this.text = text;
    }

    public Component getText() {
        return text;
    }

    /**
     * Anchors this window to {@code scenePos} (scene/block space) and enables the leader line.
     */
    public void setPointAt(@Nullable Vec3 scenePos) {
        this.pointAt = scenePos;
    }

    /**
     * Pulls the window in towards whatever it points at instead of letting it sit at its default
     * right-hand column.
     */
    public void setNearScene(boolean nearScene) {
        this.nearScene = nearScene;
    }

    /**
     * Vertical placement (0..200) used only when this window points at nothing.
     */
    public void setIndependentY(int independentY) {
        this.independentY = independentY;
    }

    public void setPalette(PonderPalette palette) {
        this.palette = palette;
    }

    public PonderPalette getPalette() {
        return palette;
    }

    public void setFade(float fade) {
        this.fade = fade;
    }

    public float getFade() {
        return fade;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(PonderScene scene, GuiGraphics graphics, float partialTick) {
        if (!visible || !(Minecraft.getInstance().screen instanceof PonderUI screen)) {
            return;
        }
        // Bails below 1/16 rather than at 0 - under that the text is invisible anyway and the
        // leader line would just be a stub.
        if (fade < 1 / 16F) {
            return;
        }

        Font font = screen.getFont();
        Vec2 anchor = pointAt != null
            ? screen.sceneToScreen(scene, pointAt)
            : new Vec2(screen.width / 2F, (screen.height - 200) / 2F + independentY - 8);

        // The further the anchor sits from the vertical middle, the further left the column moves
        // (6/8 of the width at the middle, 5/8 at the extremes).
        float yDiff = (screen.height / 2F - anchor.y - 10) / 100F;
        int targetX = (int) (screen.width * Mth.lerp(yDiff * yDiff, 6F / 8, 5F / 8));
        if (nearScene) {
            targetX = (int) Math.min(targetX, anchor.x + 50);
        }
        int boxY = (int) anchor.y;

        int wrapWidth = Math.max(1, Math.min(screen.width - targetX, MAX_TEXT_WIDTH));
        List<FormattedText> lines = font.getSplitter().splitLines(text, wrapWidth, Style.EMPTY);
        int boxWidth = 0;
        for (FormattedText line : lines) {
            boxWidth = Math.max(boxWidth, font.width(line));
        }
        int boxHeight = font.wordWrapHeight(text.getString(), Math.max(boxWidth, 1));

        int textColor = mix(palette.getColor(), TEXT_MIX_TOWARD, 0.5F);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, boxY, 400);

        new PonderBoxElement()
            .withBackground(BACKGROUND)
            .gradientBorder(BORDER_TOP, BORDER_BOT)
            .at(targetX - 10, 3, -101)
            .withBounds(boxWidth, boxHeight - 1)
            .withAlpha(fade)
            .render(graphics);

        if (pointAt != null) {
            // Grows out of the anchor towards the box as the fade comes up: a 1px bright line with a
            // 1px darker one under it, drawn by scaling a unit-wide quad.
            poseStack.pushPose();
            poseStack.translate(anchor.x, 0, 0);
            poseStack.scale((targetX - anchor.x) * fade, 1, 1);
            int bright = withAlpha(textColor, fade);
            graphics.fillGradient(0, 0, 1, 1, -100, bright, bright);
            graphics.fillGradient(0, 1, 1, 2, -100, withAlpha(0x494949, fade), withAlpha(0x393939, fade));
            poseStack.popPose();
        }

        poseStack.translate(0, 0, 400);
        int lineColor = withAlpha(textColor, fade);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i).getString(), targetX - 10, 3 + 9 * i, lineColor, false);
        }
        poseStack.popPose();
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * Vanilla's font renderer treats alpha below 5 as "fully opaque" rather than nearly invisible,
     * so this floors the alpha at 5 instead of letting a plain multiplication reach 0.
     */
    private static int withAlpha(int rgb, float fade) {
        int a = Math.max(0x05, Math.round(0xFF * Mth.clamp(fade, 0F, 1F)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
