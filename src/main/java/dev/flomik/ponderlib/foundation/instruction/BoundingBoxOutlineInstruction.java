package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.BoundingBoxOutlineElement;
import net.minecraft.world.phys.AABB;

/**
 * Shows a {@link BoundingBoxOutlineElement} for {@code duration} ticks, eased in and out like
 * {@link OutlineInstruction} — including the same slotted-retarget mechanism (see {@link
 * OutlineInstruction}'s javadoc) for {@code OverlayInstructions#chaseBoundingBoxOutline}'s {@code
 * slot}.
 */
public class BoundingBoxOutlineInstruction extends FadeInOutInstruction {

    private final BoundingBoxOutlineElement element;
    private final AABB retarget;

    public BoundingBoxOutlineInstruction(BoundingBoxOutlineElement element, int duration) {
        this(element, duration, null);
    }

    public BoundingBoxOutlineInstruction(BoundingBoxOutlineElement element, int duration, AABB retarget) {
        super(duration);
        this.element = element;
        this.retarget = retarget;
    }

    @Override
    protected void show(PonderScene scene) {
        if (retarget != null) {
            element.setBox(retarget);
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
