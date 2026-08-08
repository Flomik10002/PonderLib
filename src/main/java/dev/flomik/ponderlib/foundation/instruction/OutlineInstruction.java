package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.OutlineElement;

/**
 * Shows an {@link OutlineElement} for {@code duration} ticks, easing it in and out around that — the
 * same {@link FadeInOutInstruction} treatment text windows get, so an outline and the text explaining
 * it appear and disappear together instead of one snapping.
 */
public class OutlineInstruction extends FadeInOutInstruction {

    private final OutlineElement element;
    // Non-null only for a slotted (OverlayInstructions#showOutline with an Object slot) instance:
    // several OutlineInstructions can then share the SAME element across separate points in the
    // schedule, each retargeting it to its own selection when its own turn to show() comes up,
    // instead of each slot-call spawning its own independently-fading competing outline.
    private final Selection retarget;

    public OutlineInstruction(OutlineElement element, int duration) {
        this(element, duration, null);
    }

    public OutlineInstruction(OutlineElement element, int duration, Selection retarget) {
        super(duration);
        this.element = element;
        this.retarget = retarget;
    }

    public OutlineElement getElement() {
        return element;
    }

    @Override
    protected void show(PonderScene scene) {
        if (retarget != null) {
            element.setSelection(retarget);
        }
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
