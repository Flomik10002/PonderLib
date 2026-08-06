package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.foundation.SimpleSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementStateTest {

    @Test
    void textWindowVisibilityCanBeControlledWithoutRendering() {
        TextWindowElement element = new TextWindowElement();
        assertFalse(element.isVisible());

        element.setText(Component.literal("Headless-safe text"));
        element.setVisible(true);
        assertTrue(element.isVisible());
    }

    @Test
    void worldSectionTracksIndependentRotationOffsetAndVisibility() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(
            new SimpleSelection(List.of(BlockPos.ZERO, new BlockPos(2, 0, 2)))
        );
        Vec3 rotation = new Vec3(10, 20, 30);
        Vec3 offset = new Vec3(-1, 2, 0.5);

        element.setAnimatedRotation(rotation);
        element.setAnimatedOffset(offset);
        element.setVisible(true);

        assertEquals(rotation, element.getAnimatedRotation());
        assertEquals(offset, element.getAnimatedOffset());
        assertTrue(element.isVisible());
        assertEquals(null, element.getBlockEntity(BlockPos.ZERO));
    }
}
