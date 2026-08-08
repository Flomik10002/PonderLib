package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;

import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.PonderElement;

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
    <E extends PonderElement> ElementLink<E> addElement(Class<E> type, Supplier<? extends E> factory);
    <E extends PonderElement> void modifyElement(ElementLink<E> link, Consumer<E> action);
    void removeElement(ElementLink<? extends PonderElement> link);

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
    void addKeyframe(String title);

    /**
     * Same as {@link #addKeyframe()}, but marks a point 6 ticks later - for right after a
     * blocking wait, when the very first frames of whatever comes next haven't visually "landed"
     * yet (e.g. a section still sliding/fading in) and snapping exactly there would look abrupt.
     */
    void addLazyKeyframe();
    void addLazyKeyframe(String title);

    void addInstruction(PonderInstruction instruction);

    void addInstruction(Consumer<PonderScene> callback);

    /**
     * Before running the upcoming instructions, wait for a duration to let previous actions play
     * out.
     */
    void idle(int ticks);

    /**
     * {@link #idle(int)}, in seconds instead of ticks (20 ticks/second) — sugar for a duration that's
     * more naturally expressed as "a couple of seconds" than a specific tick count.
     */
    void idleSeconds(int seconds);

    /**
     * Marks the scene as finished once playback reaches this instruction.
     */
    void markAsFinished();

    /**
     * Scales the whole scene's rendered size relative to the UI around it — {@literal >}1 makes it
     * appear larger, {@literal <}1 smaller. Useful for a schematic that reads as too small/large at
     * this library's fixed default scale.
     */
    void scaleSceneView(float factor);

    /**
     * Pans this scene's camera around the vertical axis by {@code degrees}, on top of the fixed
     * isometric angle every scene otherwise shares — for a scene whose subject reads better from a
     * different facing than the default.
     */
    void rotateCameraY(float degrees);

    /**
     * Disables the ground-contact shadow skirt around this scene's base plate — for a scene that
     * deliberately has no meaningful floor to ground it (see {@link #configureBasePlate}).
     */
    void removeShadow();

    /**
     * Nudges this scene's vertical position within the UI — positive moves it up, negative down. For
     * a scene whose subject sits awkwardly high/low against the fixed default framing.
     */
    void setSceneOffsetY(float yOffset);

    /**
     * Controls whether the "next scene" teaser pops up once this scene finishes and another one
     * follows it (see {@code foundation.ui.PonderUI}'s own next-up box) — {@code true} by default.
     * Turn it off for a scene that's meant to stand alone even when it isn't the last in its group.
     */
    void setNextUpEnabled(boolean isEnabled);
}
