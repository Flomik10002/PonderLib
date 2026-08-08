package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.InputIconElement;

/**
 * Shows an {@link InputIconElement} for {@code duration} ticks, eased in and out like {@link
 * InputWindowInstruction}.
 */
public class InputIconInstruction extends FadeInOutInstruction {

    private final InputIconElement element;

    public InputIconInstruction(InputIconElement element, int duration) {
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
