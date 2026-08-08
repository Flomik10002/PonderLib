package dev.flomik.ponderlib.api.element;

import net.minecraft.world.phys.Vec3;

/**
 * A {@link PonderSceneElement} that fades in/out rather than popping visible/invisible instantly —
 * {@link WorldSectionElement} is the only implementation. An instruction driving a fade (see {@code
 * foundation.instruction.HideSectionInstruction}/{@code RevealSectionInstruction}) calls {@link
 * #setFade} every tick as the value moves; {@link #forceApplyFade} is for a caller that wants the
 * element to already look fully settled at a fade level with no animation of its own (e.g. an
 * immediately-shown independent section).
 */
public interface AnimatedSceneElement extends PonderSceneElement {

    /**
     * @param fade 0 (fully hidden) .. 1 (fully shown)
     */
    void setFade(float fade);

    /**
     * Same effect as {@link #setFade}, for a caller that isn't mid-animation and wants this value to
     * apply immediately rather than being read tick-by-tick by an in-progress fade instruction.
     */
    void forceApplyFade(float fade);

    /**
     * The direction (scaled to a half-block offset) a fade slides in/out from — {@link Vec3#ZERO}
     * disables the slide (fade-only, no directional movement).
     */
    void setFadeVec(Vec3 fadeVec);
}
