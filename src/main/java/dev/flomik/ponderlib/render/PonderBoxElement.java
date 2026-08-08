package dev.flomik.ponderlib.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The framed panel drawn behind text windows and the progress bar - the frame is most of what
 * makes those two read as a coherent UI rather than as bare text on the screen.
 * <p>
 * Geometry: a solid background quad inflated by {@code borderOffset + 1} on every side, four 1px
 * "outer" bars one step further out in the background colour, and a 1px inner border that
 * gradients from {@code borderTop} down to {@code borderBot} (top edge and the top of both side
 * edges take the top colour, bottom edge and the bottom of the sides take the bottom one).
 * Drawn immediate-mode through {@link BufferUploader} with the position-colour shader, since these
 * are GUI overlays drawn after the scene's own batch has been flushed, so they don't interleave
 * with it.
 */
public class PonderBoxElement {

    private float x;
    private float y;
    private float z;
    private int width = 16;
    private int height = 16;
    private int borderOffset = 2;
    private int background = 0xFF000000;
    private int borderTop = 0x40FFEEDD;
    private int borderBot = 0x20FFEEDD;
    private float alpha = 1F;

    public PonderBoxElement at(int x, int y, int z) {
        return at((float) x, (float) y, (float) z);
    }

    public PonderBoxElement at(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public PonderBoxElement withBounds(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * @param background 0xAARRGGBB
     */
    public PonderBoxElement withBackground(int background) {
        this.background = background;
        return this;
    }

    /**
     * @param top 0xAARRGGBB drawn along the top edge and the upper half of the sides
     * @param bot 0xAARRGGBB drawn along the bottom edge and the lower half of the sides
     */
    public PonderBoxElement gradientBorder(int top, int bot) {
        this.borderTop = top;
        this.borderBot = bot;
        return this;
    }

    public PonderBoxElement withBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
        return this;
    }

    /**
     * Scales every colour's alpha, so a whole box can fade in/out with the element it belongs to.
     */
    public PonderBoxElement withAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public void render(GuiGraphics graphics) {
        if (alpha <= 0) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f model = graphics.pose().last().pose();
        int f = borderOffset;
        int bg = scaleAlpha(background, alpha);
        int top = scaleAlpha(borderTop, alpha);
        int bot = scaleAlpha(borderBot, alpha);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder b = tesselator.getBuilder();

        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Four 1px bars just outside the panel, then the panel itself (which also fills behind the
        // inner border drawn in the second batch below).
        quad(b, model, x - f - 1, y - f - 2, x + f + 1 + width, y - f - 1, bg);
        quad(b, model, x - f - 2, y - f - 1, x - f - 1, y + f + 1 + height, bg);
        quad(b, model, x - f - 1, y + f + 1 + height, x + f + 1 + width, y + f + 2 + height, bg);
        quad(b, model, x + f + 1 + width, y - f - 1, x + f + 2 + width, y + f + 1 + height, bg);
        quad(b, model, x - f - 1, y - f - 1, x + f + 1 + width, y + f + 1 + height, bg);
        BufferUploader.drawWithShader(b.end());

        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Inner 1px border: top/bottom bars include the corners, the side bars don't - and the sides
        // carry the gradient from top colour to bottom colour.
        quad(b, model, x - f - 1, y - f - 1, x + f + 1 + width, y - f, top);
        vGradient(b, model, x - f - 1, y - f, x - f, y + f + height, top, bot);
        quad(b, model, x - f - 1, y + f + height, x + f + 1 + width, y + f + 1 + height, bot);
        vGradient(b, model, x + f + width, y - f, x + f + 1 + width, y + f + height, top, bot);
        BufferUploader.drawWithShader(b.end());

        RenderSystem.disableBlend();
    }

    private void quad(BufferBuilder b, Matrix4f model, float x1, float y1, float x2, float y2, int color) {
        vGradient(b, model, x1, y1, x2, y2, color, color);
    }

    private void vGradient(BufferBuilder b, Matrix4f model, float x1, float y1, float x2, float y2, int topColor, int botColor) {
        vertex(b, model, x1, y1, topColor);
        vertex(b, model, x1, y2, botColor);
        vertex(b, model, x2, y2, botColor);
        vertex(b, model, x2, y1, topColor);
    }

    private void vertex(BufferBuilder b, Matrix4f model, float px, float py, int color) {
        Vector4f pos = new Vector4f(px, py, z, 1F).mul(model);
        b.vertex(pos.x(), pos.y(), pos.z())
            .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
            .endVertex();
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0F, 1F));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
