package dev.flomik.ponderlib.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.element.PonderSceneElement;
import dev.flomik.ponderlib.render.SceneOutline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A thin line between two scene-space points — {@code OverlayInstructions#showLine}/{@code
 * #showBigLine}'s element, drawn with the same edge-box technique {@link OutlineElement} uses (see
 * {@link SceneOutline#line}) instead of a GL line, for the same reason: a GL line's width doesn't
 * scale with the scene's own zoom.
 */
public class LineElement implements PonderSceneElement {

    private final Vec3 start;
    private final Vec3 end;
    private final float thickness;
    private PonderPalette palette = PonderPalette.WHITE;
    private boolean visible;
    private float fade;

    public LineElement(Vec3 start, Vec3 end, float thickness) {
        this.start = start;
        this.end = end;
        this.thickness = thickness;
    }

    public void setPalette(PonderPalette palette) {
        this.palette = palette;
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
    public void render(PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        if (!visible || fade < 1 / 16F) {
            return;
        }
        // NO_TRANSPARENCY on the outline render type means alpha is not blended, so the fade has to
        // show up as brightness rather than opacity - same reasoning as OutlineElement's own render.
        int shade = Math.round(0xFF * Mth.clamp(fade, 0F, 1F));
        int rgb = palette.getColor();
        int argb = 0xFF000000
            | (scale((rgb >> 16) & 0xFF, shade) << 16)
            | (scale((rgb >> 8) & 0xFF, shade) << 8)
            | scale(rgb & 0xFF, shade);

        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        var consumer = buffer.getBuffer(SceneOutline.RENDER_TYPE);
        SceneOutline.line(consumer, pose, start, end, thickness, argb);
    }

    private static int scale(int channel, int shade) {
        return channel * shade / 0xFF;
    }
}
