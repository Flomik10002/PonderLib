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
     * {@link #showOutline}, with a {@code slot} argument accepted for parity with real Create's own
     * signature (there, an opaque per-block marker for highlighting one specific inventory slot
     * region instead of the whole block). This library only ever outlines whole positions — {@code
     * slot} is accepted and ignored.
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
     * {@link #showOutline(PonderPalette, Object, Selection, int)}'s outline-following-a-moving-
     * target counterpart: outlines one arbitrary {@link AABB} instead of a block-grid {@link
     * Selection} — for highlighting something that isn't aligned to whole blocks, e.g. an entity's
     * own hitbox. Re-queue this each tick with a freshly read box to actually "chase" a moving
     * target; a single call just outlines wherever the box was at that moment, same as any other
     * {@code FadeInOutInstruction}-backed overlay.
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
