package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.TextWindowElement;

/**
 * Non-blocking: the scene's timeline keeps advancing while this instruction's duration counts
 * down in the background (see {@link PonderScene#tick()}'s scheduler loop). Extends
 * {@link FadeInOutInstruction} so the text eases in and out instead of popping on and off — the
 * fade is also what animates the leader line growing out towards the text (see {@link
 * TextWindowElement}).
 */
public class TextInstruction extends FadeInOutInstruction {

    private final TextWindowElement element;

    public TextInstruction(TextWindowElement element, int duration) {
        super(duration);
        this.element = element;
    }

    public TextWindowElement getElement() {
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
