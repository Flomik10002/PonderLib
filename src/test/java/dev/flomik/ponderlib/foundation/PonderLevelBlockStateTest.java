package dev.flomik.ponderlib.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PonderLevelBlockStateTest {

    @Test
    void storingAStateAlsoUpdatesTheExistingBlockEntity() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BlockState state = Blocks.CHEST.defaultBlockState();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockEntity blockEntity = mock(BlockEntity.class);
        BlockEntityType<?> type = mock(BlockEntityType.class);
        doReturn(type).when(blockEntity).getType();
        when(type.isValid(state)).thenReturn(true);
        Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        blockEntities.put(pos, blockEntity);

        PonderLevel.storeBlockState(blocks, blockEntities, pos, state);

        assertEquals(state, blocks.get(pos));
        verify(blockEntity).setBlockState(state);
    }

    @Test
    void storingAnIncompatibleStateRemovesTheStaleBlockEntity() {
        BlockPos pos = new BlockPos(1, 2, 3);
        BlockState state = Blocks.AIR.defaultBlockState();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockEntity blockEntity = mock(BlockEntity.class);
        BlockEntityType<?> type = mock(BlockEntityType.class);
        doReturn(type).when(blockEntity).getType();
        when(type.isValid(state)).thenReturn(false);
        Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        blockEntities.put(pos, blockEntity);

        PonderLevel.storeBlockState(blocks, blockEntities, pos, state);

        assertEquals(state, blocks.get(pos));
        assertFalse(blockEntities.containsKey(pos));
        verify(blockEntity).setRemoved();
        verify(blockEntity, never()).setBlockState(state);
    }

    @Test
    void restoringTheBlockMapAlsoRemovesPositionsCreatedByThePreviousPlayback() {
        BlockPos originalPos = new BlockPos(1, 2, 3);
        BlockPos transientPos = new BlockPos(4, 5, 6);
        Map<BlockPos, BlockState> original = Map.of(originalPos, Blocks.STONE.defaultBlockState());
        Map<BlockPos, BlockState> blocks = new HashMap<>(original);
        blocks.put(transientPos, Blocks.DIRT.defaultBlockState());

        PonderLevel.restoreBlockMap(blocks, original);

        assertEquals(original, blocks);
    }
}
