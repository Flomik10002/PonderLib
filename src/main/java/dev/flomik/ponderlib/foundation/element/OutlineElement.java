package dev.flomik.ponderlib.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.element.PonderSceneElement;
import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.render.SceneOutline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Highlights a {@link Selection} inside a scene — the storyboard-facing counterpart to identify
 * mode's hover highlight, and the other half of "explain this thing" next to {@code
 * TextWindowElement}'s leader line (that one says WHAT, this one says WHERE). Drawn with {@link
 * SceneOutline}, faded in/out by {@code foundation.instruction.OutlineInstruction} the same way
 * text windows are.
 * <p>
 * Draws one box per selected position rather than a single silhouette around the whole selection -
 * identical to a merged outline for the common case of pointing at a single block, visibly busier
 * for a large flat selection like a floor (a 3x3 floor gets nine boxes, not one rectangle).
 */
public class OutlineElement implements PonderSceneElement {

    // Matches the identify-mode hover highlight's thickness, so a storyboard outline and a hover
    // outline read as the same kind of mark.
    private static final float THICKNESS = 0.05F;

    private final Selection selection;
    private PonderPalette palette = PonderPalette.WHITE;
    private boolean visible;
    private float fade;

    public OutlineElement(Selection selection) {
        this.selection = selection;
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
        // show up as brightness rather than opacity.
        int shade = Math.round(0xFF * Mth.clamp(fade, 0F, 1F));
        int rgb = palette.getColor();
        int argb = 0xFF000000
            | (scale((rgb >> 16) & 0xFF, shade) << 16)
            | (scale((rgb >> 8) & 0xFF, shade) << 8)
            | scale(rgb & 0xFF, shade);

        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        var consumer = buffer.getBuffer(SceneOutline.RENDER_TYPE);
        for (BlockPos pos : selection) {
            SceneOutline.render(consumer, pose,
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                THICKNESS, argb);
        }
    }

    private static int scale(int channel, int shade) {
        return channel * shade / 0xFF;
    }
}
