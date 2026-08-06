package dev.flomik.ponderlib.api.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * A {@link PonderElement} rendered inside the 3D scene itself, as opposed to a flat overlay.
 */
public interface PonderSceneElement extends PonderElement {

    /**
     * @param poseStack   already transformed by the scene's own camera - draw in scene (block)
     *                    space, not screen space
     * @param partialTick how far between the previous and current tick this frame falls (0..1)
     */
    void render(PoseStack poseStack, MultiBufferSource buffer, float partialTick);
}
