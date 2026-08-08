package dev.flomik.ponderlib.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PonderLevelBlockStateTest {

    @Test
    void storingAStateAlsoUpdatesTheExistingBlockEntity() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BlockState state = Blocks.CHEST.defaultBlockState();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockEntity blockEntity = mock(BlockEntity.class);
        Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        blockEntities.put(pos, blockEntity);

        PonderLevel.storeBlockState(blocks, blockEntities, pos, state);

        assertEquals(state, blocks.get(pos));
        verify(blockEntity).setBlockState(state);
    }
}
