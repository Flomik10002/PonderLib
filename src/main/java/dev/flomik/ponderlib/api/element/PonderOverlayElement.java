package dev.flomik.ponderlib.api.element;

import dev.flomik.ponderlib.foundation.PonderScene;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A {@link PonderElement} rendered as a flat 2D overlay on top of the scene.
 */
public interface PonderOverlayElement extends PonderElement {

    /**
     * @param partialTick how far between the previous and current tick this frame falls (0..1) —
     *                     use it to interpolate any per-tick animation smoothly between ticks
     */
    void render(PonderScene scene, GuiGraphics graphics, float partialTick);
}
