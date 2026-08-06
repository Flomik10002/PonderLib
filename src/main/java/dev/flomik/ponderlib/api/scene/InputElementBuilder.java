package dev.flomik.ponderlib.api.scene;

import net.minecraft.world.item.ItemStack;

/**
 * Configures one input hint — the little window a scene puts next to a block to say "right-click
 * here". Obtained from {@link OverlayInstructions#showControls}; every method is optional and
 * returns {@code this}.
 */
public interface InputElementBuilder {

    /**
     * Accepts and discards everything — useful for an {@link OverlayInstructions} implementation
     * that only cares that {@code showControls} was called, not how the hint would look. Returning
     * {@code null} instead would break any storyboard that chains onto the result.
     */
    InputElementBuilder NO_OP = new InputElementBuilder() {
        @Override
        public InputElementBuilder withItem(ItemStack stack) {
            return this;
        }

        @Override
        public InputElementBuilder leftClick() {
            return this;
        }

        @Override
        public InputElementBuilder rightClick() {
            return this;
        }

        @Override
        public InputElementBuilder scroll() {
            return this;
        }

        @Override
        public InputElementBuilder whileSneaking() {
            return this;
        }

        @Override
        public InputElementBuilder whileCTRL() {
            return this;
        }
    };

    /**
     * Shows an item alongside the input icon — "use THIS here".
     */
    InputElementBuilder withItem(ItemStack stack);

    InputElementBuilder leftClick();

    InputElementBuilder rightClick();

    InputElementBuilder scroll();

    /**
     * Adds a "Sneak" qualifier, for actions that need the sneak key held.
     */
    InputElementBuilder whileSneaking();

    /**
     * Adds a "Ctrl" qualifier.
     */
    InputElementBuilder whileCTRL();
}
