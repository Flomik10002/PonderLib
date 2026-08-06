package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;

import java.util.function.Consumer;

public abstract class PonderInstruction {

    /**
     * Blocking instructions hold up the scene's timeline until complete; non-blocking ones fire
     * and let playback continue to the next instruction in the same tick.
     */
    public boolean isBlocking() {
        return false;
    }

    public void reset(PonderScene scene) {
    }

    /**
     * Called once when this instruction is (re-)placed onto the active schedule (see
     * {@link PonderScene#begin()}) - this, not a naive sum of every instruction's own duration, is
     * how {@link PonderScene#getTotalTime()} gets computed: only {@link
     * TickingInstruction#isBlocking() blocking} instructions actually extend the scene's serial
     * length (see its override) - non-blocking ones (fades, text, animations) run alongside
     * whatever comes next and would otherwise inflate the scrubber's range past what {@code
     * seekToTime} can ever actually reach.
     */
    public void onScheduled(PonderScene scene) {
    }

    public abstract boolean isComplete();

    public abstract void tick(PonderScene scene);

    public static PonderInstruction simple(Consumer<PonderScene> callback) {
        return new Simple(callback);
    }

    private static final class Simple extends PonderInstruction {

        private final Consumer<PonderScene> callback;

        private Simple(Consumer<PonderScene> callback) {
            this.callback = callback;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public void tick(PonderScene scene) {
            callback.accept(scene);
        }
    }
}
