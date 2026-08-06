package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.OutlineElement;

/**
 * Shows an {@link OutlineElement} for {@code duration} ticks, easing it in and out around that — the
 * same {@link FadeInOutInstruction} treatment text windows get, so an outline and the text explaining
 * it appear and disappear together instead of one snapping.
 */
public class OutlineInstruction extends FadeInOutInstruction {

    private final OutlineElement element;

    public OutlineInstruction(OutlineElement element, int duration) {
        super(duration);
        this.element = element;
    }

    public OutlineElement getElement() {
        return element;
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
