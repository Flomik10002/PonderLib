package dev.flomik.ponderlib.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualBlockViewTest {

    @Test
    void suppliesHeadlessSafeWorldQueriesForItsConfiguredBlock() {
        BlockPos origin = new BlockPos(4, 8, -2);
        var state = Blocks.OAK_PLANKS.defaultBlockState();
        VirtualBlockView view = new VirtualBlockView(origin, state);

        assertEquals(state, view.getBlockState(origin));
        assertEquals(Blocks.AIR, view.getBlockState(origin.above()).getBlock());
        assertEquals(Fluids.EMPTY.defaultFluidState(), view.getFluidState(origin));
        assertEquals(15, view.getBrightness(LightLayer.BLOCK, origin));
        assertEquals(15, view.getRawBrightness(origin, 12));
        assertEquals(-64, view.getMinBuildHeight());
        assertEquals(384, view.getHeight());
        assertEquals(null, view.getBlockEntity(origin));
    }

    @Test
    void shadeIsFlatInEveryDirectionSinceDiffuseLightIsAppliedAtRenderTimeInstead() {
        VirtualBlockView view = new VirtualBlockView(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        for (Direction direction : Direction.values()) {
            assertEquals(1.0F, view.getShade(direction, true));
            assertEquals(1.0F, view.getShade(direction, false));
        }
    }

    @Test
    void hasNoLightEngineSinceBrightnessIsSuppliedDirectly() {
        VirtualBlockView view = new VirtualBlockView(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        assertThrows(UnsupportedOperationException.class, view::getLightEngine);
    }
}
