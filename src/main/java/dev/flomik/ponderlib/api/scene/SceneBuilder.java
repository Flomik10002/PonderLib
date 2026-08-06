package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;

import java.util.function.Consumer;

/**
 * The imperative "enqueue an instruction" API a {@link PonderStoryBoard} programs against.
 * <p>
 * The concrete implementation ({@code PonderSceneBuilder}) is a non-final class on purpose: a mod
 * can subclass it to layer its own domain-specific instructions (its own {@code WorldInstructions}/
 * {@code OverlayInstructions} implementations calling back into the protected scene) on top of this
 * engine, rather than being limited to exactly this vocabulary.
 */
public interface SceneBuilder {

    WorldInstructions world();

    OverlayInstructions overlay();

    EffectInstructions effects();

    PonderScene getScene();

    /**
     * Assigns a title for this scene, shown in the UI.
     */
    void title(String sceneId, String title);

    /**
     * Tells the UI which part of the schematic is the base plate - the flat horizontal footprint
     * the ground-contact shadow hugs (see {@code PonderUI#renderBasePlateShadowAndFlash}). Encouraged
     * whenever a scene has parts sticking out beyond its floor, or shows more than one section:
     * without it, the footprint is instead re-derived from whichever section {@link
     * WorldInstructions#showSection} last revealed, which silently shrinks/moves if a later
     * section is smaller than the floor.
     *
     * @param xOffset       block spaces between the base plate and the schematic origin on X
     * @param zOffset       block spaces between the base plate and the schematic origin on Z
     * @param basePlateSize length in blocks of the (assumed square) base plate
     */
    void configureBasePlate(int xOffset, int zOffset, int basePlateSize);

    /**
     * Reveals the area configured via {@link #configureBasePlate} as its own section - a common
     * scene opener (floor first, then whatever sits on it). Fades in exactly like any other
     * {@link WorldInstructions#showSection} call.
     */
    void showBasePlate();

    /**
     * Marks this exact point in the schedule as a scrubber tick mark - see {@code
     * foundation.ui.PonderUI}'s timeline, which snaps clicks to the nearest one instead of seeking
     * to an arbitrary tick. Splits a scene into visually distinct chapters, same idea as chapter
     * markers on a video's scrub bar.
     */
    void addKeyframe();

    /**
     * Same as {@link #addKeyframe()}, but marks a point 6 ticks later - for right after a
     * blocking wait, when the very first frames of whatever comes next haven't visually "landed"
     * yet (e.g. a section still sliding/fading in) and snapping exactly there would look abrupt.
     */
    void addLazyKeyframe();

    void addInstruction(PonderInstruction instruction);

    void addInstruction(Consumer<PonderScene> callback);

    /**
     * Before running the upcoming instructions, wait for a duration to let previous actions play
     * out.
     */
    void idle(int ticks);

    /**
     * Marks the scene as finished once playback reaches this instruction.
     */
    void markAsFinished();
}
