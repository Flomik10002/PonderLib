package dev.flomik.ponderlib.api.scene;

/**
 * Maps a normalized animation progress value to its eased progress.
 * <p>
 * The built-in curves all map {@code 0 -> 0} and {@code 1 -> 1}. Storyboards may also pass a
 * lambda for a custom curve. Entity movement supplies an input clamped to {@code [0, 1]}, while
 * the curve's output is deliberately left unconstrained so overshooting curves remain possible.
 */
@FunctionalInterface
public interface Easing {

    /** Constant-speed motion. */
    Easing LINEAR = progress -> progress;

    /** Starts slowly and accelerates quadratically. */
    Easing QUAD_IN = progress -> progress * progress;

    /** Starts quickly and decelerates quadratically. */
    Easing QUAD_OUT = progress -> 1.0 - (1.0 - progress) * (1.0 - progress);

    /** Accelerates through the first half, then symmetrically decelerates through the second. */
    Easing QUAD_IN_OUT = progress -> progress < 0.5
        ? 2.0 * progress * progress
        : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;

    /**
     * Computes eased progress for the supplied normalized progress.
     *
     * @param progress animation progress in {@code [0, 1]}
     * @return eased progress; custom curves may return values outside {@code [0, 1]}
     */
    double ease(double progress);
}
