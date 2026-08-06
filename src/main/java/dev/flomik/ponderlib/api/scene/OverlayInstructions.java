package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.Pointing;
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
     * {@link #showOutline} and {@link #showText} together, sharing a duration — the usual way a
     * scene points something out: the outline marks where, the returned text window explains what.
     * The text window points at the selection's centre by default, and {@code .colored(...)} on the
     * returned builder recolours the outline along with the text.
     */
    TextElementBuilder showOutlineWithText(Selection selection, int duration, String text);

    /**
     * Shows an input hint at {@code sceneSpace} for {@code duration} ticks — a small window whose tail
     * points at that spot, saying which mouse input to use there and optionally with which item. The
     * other main teaching device next to {@link #showText}: text explains, this demonstrates.
     *
     * @param direction which way the window's tail points (i.e. which side of the anchor it sits on)
     */
    InputElementBuilder showControls(Vec3 sceneSpace, Pointing direction, int duration);
}
