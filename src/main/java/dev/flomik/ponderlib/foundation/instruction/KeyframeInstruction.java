package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;

/**
 * A pure timeline marker - does nothing on tick, just records a scrubber tick mark (see
 * {@code PonderScene#markKeyframe}) at wherever it sits in the schedule when scheduled.
 */
public class KeyframeInstruction extends PonderInstruction {

    public static final KeyframeInstruction IMMEDIATE = new KeyframeInstruction(false);
    public static final KeyframeInstruction DELAYED = new KeyframeInstruction(true);

    private final boolean delayed;

    private KeyframeInstruction(boolean delayed) {
        this.delayed = delayed;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void tick(PonderScene scene) {
    }

    @Override
    public void onScheduled(PonderScene scene) {
        scene.markKeyframe(delayed ? 6 : 0);
    }
}
