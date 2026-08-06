package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;

/**
 * Shows something, eases its fade in over {@link #FADE_TIME} ticks, holds it for the requested
 * duration, then eases back out and hides it — so the caller's {@code duration} is the
 * fully-visible time and the instruction quietly runs for {@code duration + 2 * FADE_TIME}.
 */
public abstract class FadeInOutInstruction extends TickingInstruction {

    protected static final int FADE_TIME = 5;

    protected FadeInOutInstruction(int duration) {
        super(false, duration + 2 * FADE_TIME);
    }

    protected abstract void show(PonderScene scene);

    protected abstract void hide(PonderScene scene);

    protected abstract void applyFade(PonderScene scene, float fade);

    @Override
    protected void firstTick(PonderScene scene) {
        super.firstTick(scene);
        show(scene);
        applyFade(scene, 0);
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        int elapsed = totalTicks - remainingTicks;

        if (elapsed < FADE_TIME) {
            float fade = elapsed / (float) FADE_TIME;
            applyFade(scene, fade * fade);
        } else if (remainingTicks < FADE_TIME) {
            float fade = remainingTicks / (float) FADE_TIME;
            applyFade(scene, fade * fade);
        } else {
            applyFade(scene, 1);
        }

        if (remainingTicks == 0) {
            applyFade(scene, 0);
            hide(scene);
        }
    }
}
