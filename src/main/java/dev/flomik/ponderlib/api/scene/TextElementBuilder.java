package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.PonderPalette;
import net.minecraft.world.phys.Vec3;

/**
 * Configures one text window before it's queued. Obtained from {@link
 * OverlayInstructions#showText(int, String)}; every method is optional and returns {@code this}, so
 * the plain {@code showText(duration, text)} call on its own already behaves as a complete,
 * unattached window near the middle of the screen.
 */
public interface TextElementBuilder {

    /**
     * A builder that accepts and discards everything — useful for an {@link OverlayInstructions}
     * implementation that only cares that {@code showText} was called and not how the window would
     * look, e.g. a datagen provider that runs a storyboard just to harvest its default lang strings
     * without building a real scene. Returning {@code null} from {@code showText} instead would
     * break any storyboard that chains onto the result.
     */
    TextElementBuilder NO_OP = new TextElementBuilder() {
        @Override
        public TextElementBuilder pointAt(Vec3 scenePos) {
            return this;
        }

        @Override
        public TextElementBuilder placeNearTarget() {
            return this;
        }

        @Override
        public TextElementBuilder independent(int y) {
            return this;
        }

        @Override
        public TextElementBuilder colored(PonderPalette palette) {
            return this;
        }

        @Override
        public TextElementBuilder attachKeyFrame() {
            return this;
        }
    };

    /**
     * Anchors the window to a point in scene (block) space: the window lines up vertically with that
     * point and a leader line grows from it towards the text. Without this the window is
     * "independent" — see {@link #independent(int)}.
     */
    TextElementBuilder pointAt(Vec3 scenePos);

    /**
     * Pulls the window in towards whatever {@link #pointAt} targets, instead of leaving it in its
     * default right-hand column.
     */
    TextElementBuilder placeNearTarget();

    /**
     * Vertical placement (0..200 from the top of the scene area) for a window that points at nothing.
     * Ignored once {@link #pointAt} is set.
     */
    TextElementBuilder independent(int y);

    TextElementBuilder colored(PonderPalette palette);

    /**
     * Also drops a keyframe on the scrubber here, so this text is something the viewer can seek
     * back to.
     */
    TextElementBuilder attachKeyFrame();
}
