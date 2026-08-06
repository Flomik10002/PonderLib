package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.InputWindowElement;

/**
 * Shows an {@link InputWindowElement} for {@code duration} ticks, eased in and out like the text and
 * outline instructions — an input hint that popped in instantly would read as a glitch next to them.
 */
public class InputWindowInstruction extends FadeInOutInstruction {

    private final InputWindowElement element;

    public InputWindowInstruction(InputWindowElement element, int duration) {
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
