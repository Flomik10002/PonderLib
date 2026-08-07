package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.BoundingBoxOutlineElement;

/**
 * Shows a {@link BoundingBoxOutlineElement} for {@code duration} ticks, eased in and out like
 * {@link OutlineInstruction}.
 */
public class BoundingBoxOutlineInstruction extends FadeInOutInstruction {

    private final BoundingBoxOutlineElement element;

    public BoundingBoxOutlineInstruction(BoundingBoxOutlineElement element, int duration) {
        super(duration);
        this.element = element;
    }

    @Override
    protected void show(PonderScene scene) {
        scene.addElement(element);
        element.setVisible(true);
    }

    @Override
    protected void hide(PonderScene scene) {
        element.setVisible(false);
    }

    @Override
    protected void applyFade(PonderScene scene, float fade) {
        element.setFade(fade);
    }
}
