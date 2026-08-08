package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.api.Pointing;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputWindowElementTest {

    @Test
    void dropUsesTheCurrentKeyNameInItsControlLabel() {
        assertEquals("Drop [G]", InputWindowElement.dropControlFor(Component.literal("G")).getString());
    }

    @Test
    void theLastSelectedControlKindWins() {
        InputWindowElement element = new InputWindowElement(Vec3.ZERO, Pointing.DOWN);

        element.drop();
        assertTrue(element.isDrop());

        element.rightClick();
        assertFalse(element.isDrop());
    }
}
