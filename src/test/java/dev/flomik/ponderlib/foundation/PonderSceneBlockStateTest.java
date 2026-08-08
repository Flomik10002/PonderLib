package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.PonderElement;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PonderSceneBlockStateTest {

    @Test
    void stateChangesUpdateOnlyVisibleSectionsContainingThePosition() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BlockState air = Blocks.AIR.defaultBlockState();
        WorldSectionElementImpl visible = section(true, pos);
        WorldSectionElementImpl hidden = section(false, pos);
        WorldSectionElementImpl elsewhere = section(true, pos.above());

        PonderScene.updateVisibleSections(List.<PonderElement>of(visible, hidden, elsewhere), pos, air);

        verify(visible).setBlockState(pos, air);
        verify(hidden, never()).setBlockState(pos, air);
        verify(elsewhere, never()).setBlockState(pos, air);
    }

    private static WorldSectionElementImpl section(boolean visible, BlockPos pos) {
        WorldSectionElementImpl section = mock(WorldSectionElementImpl.class);
        when(section.isVisible()).thenReturn(visible);
        when(section.getBlockPositions()).thenReturn(Set.of(pos));
        return section;
    }
}
