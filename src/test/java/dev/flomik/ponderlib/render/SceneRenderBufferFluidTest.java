package dev.flomik.ponderlib.render;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SceneRenderBufferFluidTest {
    @Test void nonEmptyFluidRequiresDedicatedFluidPass(){BlockState state=mock(BlockState.class);FluidState fluid=mock(FluidState.class);when(state.getFluidState()).thenReturn(fluid);when(fluid.isEmpty()).thenReturn(false);assertTrue(SceneRenderBuffer.needsFluidPass(state));when(fluid.isEmpty()).thenReturn(true);assertFalse(SceneRenderBuffer.needsFluidPass(state));}
}
