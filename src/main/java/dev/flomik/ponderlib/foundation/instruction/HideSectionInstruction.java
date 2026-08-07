package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.Direction;

/**
 * The reverse of {@link RevealSectionInstruction}: fades an already-shown {@link
 * WorldSectionElementImpl} out over {@code fadeOutTicks}, sliding out towards {@code fadeOutTo}'s
 * normal, then marks it invisible. Non-blocking, same as the reveal it mirrors - the timeline keeps
 * advancing while a section fades out.
 */
public class HideSectionInstruction extends TickingInstruction {

    private final WorldSectionElementImpl element;
    private final Direction fadeOutTo;

    public HideSectionInstruction(WorldSectionElementImpl element, Direction fadeOutTo, int fadeOutTicks) {
        super(false, Math.max(fadeOutTicks, 1));
        this.element = element;
        this.fadeOutTo = fadeOutTo;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        element.setFadeFromDirection(fadeOutTo);
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        float remainingFraction = remainingTicks / (float) totalTicks;
        element.setFade(remainingFraction * remainingFraction);
        if (remainingTicks == 0) {
            element.setVisible(false);
        }
    }
}
