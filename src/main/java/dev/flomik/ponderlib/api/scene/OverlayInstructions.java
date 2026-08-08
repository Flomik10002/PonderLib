package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface OverlayInstructions {

    /**
     * Shows a text window for {@code duration} ticks. Non-blocking — the scene's timeline keeps
     * advancing while the text is up, and the window eases in/out around that duration (see
     * {@code foundation.instruction.FadeInOutInstruction}).
     * <p>
     * The returned {@link TextElementBuilder} is optional: ignore it and the window behaves as an
     * unattached one near the middle of the screen; call {@code .pointAt(...)} on it to anchor the
     * window to a place in the scene and draw a leader line out to it.
     */
    TextElementBuilder showText(int duration, String text);

    /**
     * Highlights {@code selection} for {@code duration} ticks, in {@code palette}'s colour — the
     * storyboard's way of saying "this block, here". Non-blocking, and eased in/out like
     * {@link #showText}.
     */
    void showOutline(PonderPalette palette, Selection selection, int duration);

    /**
     * {@link #showOutline}, keyed by an arbitrary {@code slot} object identifying "this outline" as
     * the same logical thing across separate calls. A repeated call with the same {@code slot}
     * (anywhere later in the same storyboard) retargets that existing outline to the new {@code
     * selection} instead of spawning a second one competing for the same highlight — the way to move
     * one outline to follow something across several beats without leaking a new element per call.
     * The first call for a given {@code slot} behaves exactly like the 3-argument overload.
     */
    void showOutline(PonderPalette palette, Object slot, Selection selection, int duration);

    /**
     * {@link #showOutline} and {@link #showText} together, sharing a duration — the usual way a
     * scene points something out: the outline marks where, the returned text window explains what.
     * The text window points at the selection's centre by default, and {@code .colored(...)} on the
     * returned builder recolours the outline along with the text.
     */
    TextElementBuilder showOutlineWithText(Selection selection, int duration, String text);

    /**
     * {@link #showOutline(PonderPalette, Object, Selection, int)}'s counterpart for something that
     * isn't aligned to whole blocks — an arbitrary {@link AABB} (an entity's own hitbox, say) instead
     * of a block-grid {@link Selection} — with the exact same {@code slot} retargeting behaviour:
     * call it again later in the storyboard with the same {@code slot} and a freshly read box to
     * make the one outline "chase" wherever its target has moved to since, instead of spawning a new
     * outline at each beat.
     */
    void chaseBoundingBoxOutline(PonderPalette color, Object slot, AABB boundingBox, int duration);

    /**
     * A line between two scene-space points, in {@code color} — for pointing out a distance/range or
     * a before/after threshold rather than a single block (see {@link #showOutline} for that). Eased
     * in/out like every other overlay.
     */
    void showLine(PonderPalette color, Vec3 start, Vec3 end, int duration);

    /**
     * {@link #showLine}, drawn thicker — for a line meant to read as the main subject of its beat
     * rather than a secondary annotation next to a text window.
     */
    void showBigLine(PonderPalette color, Vec3 start, Vec3 end, int duration);

    /**
     * Shows an input hint at {@code sceneSpace} for {@code duration} ticks — a small window whose tail
     * points at that spot, saying which mouse input to use there and optionally with which item. The
     * other main teaching device next to {@link #showText}: text explains, this demonstrates.
     *
     * @param direction which way the window's tail points (i.e. which side of the anchor it sits on)
     */
    InputElementBuilder showControls(Vec3 sceneSpace, Pointing direction, int duration);

    /**
     * A floating "scroll here" icon centered on {@code pos}'s {@code side} face — the lighter-weight
     * counterpart to {@link #showControls} for a block that's configured by scrolling rather than
     * clicking, with no speech box/tail of its own.
     */
    void showCenteredScrollInput(BlockPos pos, Direction side, int duration);

    /**
     * {@link #showCenteredScrollInput}, anchored to an arbitrary scene-space point instead of a
     * block face.
     */
    void showScrollInput(Vec3 location, Direction side, int duration);

    /**
     * {@link #showCenteredScrollInput}, for the common case of a block (a repeater's delay, say)
     * that's always scrolled from directly above.
     */
    void showRepeaterScrollInput(BlockPos pos, int duration);

    /**
     * A floating "filter slot here" icon at {@code location} — the same lightweight, boxless hint as
     * {@link #showScrollInput}, with this library's own filter-funnel glyph instead of the scroll
     * wheel.
     */
    void showFilterSlotInput(Vec3 location, int duration);

    /**
     * {@link #showFilterSlotInput(Vec3, int)}, offset towards {@code side} — for a filter slot on a
     * specific face of a block rather than centered on a bare point.
     */
    void showFilterSlotInput(Vec3 location, Direction side, int duration);
}
