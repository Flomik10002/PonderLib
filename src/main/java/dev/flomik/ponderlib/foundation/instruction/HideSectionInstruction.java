package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.Direction;

import java.util.function.Function;

/**
 * The reverse of {@link RevealSectionInstruction}: fades an already-shown {@link
 * WorldSectionElementImpl} out over {@code fadeOutTicks}, sliding out towards {@code fadeOutTo}'s
 * normal, then marks it invisible. Non-blocking, same as the reveal it mirrors - the timeline keeps
 * advancing while a section fades out.
 * <p>
 * {@code resolver} runs on the very first tick, not at construction time — resolving which element
 * to fade (by {@link dev.flomik.ponderlib.api.element.ElementLink}, or by matching a {@code
 * Selection}'s positions against whatever's currently visible) is inherently a runtime question, the
 * same reason {@link AnimateElementInstruction} resolves its own link lazily in {@code firstTick}
 * rather than at schedule-build time. A resolver that finds nothing (returns {@code null}) makes the
 * whole instruction a no-op for its entire duration, never fading anything.
 */
public class HideSectionInstruction extends TickingInstruction {

    private final Function<PonderScene, WorldSectionElementImpl> resolver;
    private final Direction fadeOutTo;
    private WorldSectionElementImpl element;

    public HideSectionInstruction(Function<PonderScene, WorldSectionElementImpl> resolver, Direction fadeOutTo, int fadeOutTicks) {
        super(false, Math.max(fadeOutTicks, 1));
        this.resolver = resolver;
        this.fadeOutTo = fadeOutTo;
    }

    @Override
    protected void firstTick(PonderScene scene) {
        element = resolver.apply(scene);
        if (element != null) {
            element.setFadeFromDirection(fadeOutTo);
        }
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        if (element == null) {
            return;
        }
        float remainingFraction = remainingTicks / (float) totalTicks;
        element.setFade(remainingFraction * remainingFraction);
        if (remainingTicks == 0) {
            element.setVisible(false);
        }
    }
}
