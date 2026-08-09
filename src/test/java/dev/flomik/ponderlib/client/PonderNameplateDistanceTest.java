package dev.flomik.ponderlib.client;

import dev.flomik.ponderlib.foundation.PonderLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PonderNameplateDistanceTest {

    @Test
    void onlyExplicitlyVisiblePonderEntitiesBypassDistanceCulling() {
        Entity entity = mock(Entity.class);
        when(entity.level()).thenReturn(mock(PonderLevel.class));
        when(entity.isCustomNameVisible()).thenReturn(true);
        assertTrue(PonderNameplateDistance.shouldBypass(entity));

        when(entity.isCustomNameVisible()).thenReturn(false);
        assertFalse(PonderNameplateDistance.shouldBypass(entity));

        when(entity.level()).thenReturn(mock(Level.class));
        when(entity.isCustomNameVisible()).thenReturn(true);
        assertFalse(PonderNameplateDistance.shouldBypass(entity));
    }
}
