package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;

/**
 * A pure timeline marker - does nothing on tick, just records a scrubber tick mark (see
 * {@code PonderScene#markKeyframe}) at wherever it sits in the schedule when scheduled.
 */
public class KeyframeInstruction extends PonderInstruction {

    public static final KeyframeInstruction IMMEDIATE = new KeyframeInstruction(false, null);
    public static final KeyframeInstruction DELAYED = new KeyframeInstruction(true, null);

    private final boolean delayed;
    private final String title;

    private KeyframeInstruction(boolean delayed, String title) {
        this.delayed = delayed;
        this.title = title;
    }

    public static KeyframeInstruction named(String title, boolean delayed) {
        return new KeyframeInstruction(delayed, title);
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
        if (title == null) scene.markKeyframe(delayed ? 6 : 0);
        else scene.markKeyframe(delayed ? 6 : 0, title);
    }
}
