package dev.flomik.ponderlib.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.element.PonderSceneElement;
import dev.flomik.ponderlib.render.SceneOutline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Outlines one arbitrary {@link AABB} rather than a block-grid {@code Selection} — {@code
 * OverlayInstructions#chaseBoundingBoxOutline}'s element, for highlighting something that isn't
 * aligned to whole blocks (an entity's own hitbox, say). The box itself is fixed for the life of
 * this element — "chase" describes what the caller re-queues it for (following a moving target
 * frame to frame via a fresh call), not an animation this element does on its own.
 */
public class BoundingBoxOutlineElement implements PonderSceneElement {

    private static final float THICKNESS = 0.05F;

    private final AABB box;
    private PonderPalette palette = PonderPalette.WHITE;
    private boolean visible;
    private float fade;

    public BoundingBoxOutlineElement(AABB box) {
        this.box = box;
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
        int shade = Math.round(0xFF * Mth.clamp(fade, 0F, 1F));
        int rgb = palette.getColor();
        int argb = 0xFF000000
            | (scale((rgb >> 16) & 0xFF, shade) << 16)
            | (scale((rgb >> 8) & 0xFF, shade) << 8)
            | scale(rgb & 0xFF, shade);

        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        var consumer = buffer.getBuffer(SceneOutline.RENDER_TYPE);
        SceneOutline.render(consumer, pose,
            (float) box.minX, (float) box.minY, (float) box.minZ,
            (float) box.maxX, (float) box.maxY, (float) box.maxZ,
            THICKNESS, argb);
    }

    private static int scale(int channel, int shade) {
        return channel * shade / 0xFF;
    }
}
