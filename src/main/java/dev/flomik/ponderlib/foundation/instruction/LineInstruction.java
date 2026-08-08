package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.LineElement;

/**
 * Shows a {@link LineElement} for {@code duration} ticks, eased in and out like {@link
 * OutlineInstruction} — a line and the outline/text it's pointing alongside should fade together.
 */
public class LineInstruction extends FadeInOutInstruction {

    private final LineElement element;

    public LineInstruction(LineElement element, int duration) {
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
