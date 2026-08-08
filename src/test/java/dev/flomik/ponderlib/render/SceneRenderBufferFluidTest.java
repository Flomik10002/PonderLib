package dev.flomik.ponderlib.render;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SceneRenderBufferFluidTest {
    @Test void nonEmptyFluidRequiresTheDedicatedFluidPass() {
        BlockState state = mock(BlockState.class);
        FluidState fluid = mock(FluidState.class);
        when(state.getFluidState()).thenReturn(fluid);
        when(fluid.isEmpty()).thenReturn(false);
        assertTrue(SceneRenderBuffer.needsFluidPass(state));
        when(fluid.isEmpty()).thenReturn(true);
        assertFalse(SceneRenderBuffer.needsFluidPass(state));
    }
}
