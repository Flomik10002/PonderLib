package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Reveals a {@link WorldSectionElementImpl} over {@code fadeInTicks} instead of popping it fully
 * visible instantly - captures its blocks/state from the scene's level, links it, and fades it in
 * (dim-to-full-bright light plus a small directional slide, see {@code WorldSectionElementImpl}).
 * Non-blocking - the timeline keeps advancing while a section fades in.
 */
public class RevealSectionInstruction extends TickingInstruction {

    private final WorldSectionElementImpl element;
    private final ElementLink<WorldSectionElement> link;
    private final Direction fadeInFrom;
    private final Vec3 focusPoint;
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    public RevealSectionInstruction(WorldSectionElementImpl element, ElementLink<WorldSectionElement> link,
                                     Direction fadeInFrom, int fadeInTicks, Vec3 focusPoint,
                                     double minX, double maxX, double minZ, double maxZ) {
        super(false, Math.max(fadeInTicks, 1));
        this.element = element;
        this.link = link;
        this.fadeInFrom = fadeInFrom;
        this.focusPoint = focusPoint;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        element.capture(scene.getLevel());
        scene.addElement(element);
        scene.linkElement(element, link);
        element.setVisible(true);
        element.setFade(0);
        element.setFadeFromDirection(fadeInFrom);
        // Same WorldSectionElementImpl instance is reused if this instruction reset()s and reveals
        // again (see PonderScene#begin()'s doc comment) - without resetting these too, a replayed
        // rotate/move (see AnimateElementInstruction) would compute its target from wherever the
        // element was left at the end of the previous playthrough instead of from neutral, so the
        // section would appear to jump to an already-animated pose and animate further from there.
        element.setAnimatedRotation(Vec3.ZERO);
        element.setAnimatedOffset(Vec3.ZERO);
        scene.setFocusPoint(focusPoint);
        scene.setBasePlateBounds(minX, maxX, minZ, maxZ);
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        float remainingFraction = remainingTicks / (float) totalTicks;
        element.setFade(1 - remainingFraction * remainingFraction);
    }
}
