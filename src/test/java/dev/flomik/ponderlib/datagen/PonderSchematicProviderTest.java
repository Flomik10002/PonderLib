package dev.flomik.ponderlib.datagen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// PonderSchematicProvider hand-builds the structure-template NBT (no live Level to populate a real
// StructureTemplate from at datagen time - see that class' own javadoc) - these tests exercise the
// actual contract that matters: real vanilla StructureTemplate#load can read what we wrote back out
// correctly, the same way foundation.registration.SchematicLoader will at real runtime.
class PonderSchematicProviderTest {

    @Test
    void checkerboardFloorAlternatesByPositionParity() {
        Map<BlockPos, BlockState> blocks = PonderSchematicProvider.withCheckerboardFloor(
            3, 3, new BlockPos(1, 1, 1), Blocks.OAK_PLANKS.defaultBlockState());

        assertEquals(Blocks.WHITE_CONCRETE.defaultBlockState(), blocks.get(new BlockPos(0, 0, 0)));
        assertEquals(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), blocks.get(new BlockPos(1, 0, 0)));
        assertEquals(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), blocks.get(new BlockPos(0, 0, 1)));
        assertEquals(Blocks.WHITE_CONCRETE.defaultBlockState(), blocks.get(new BlockPos(1, 0, 1)));
        assertEquals(Blocks.WHITE_CONCRETE.defaultBlockState(), blocks.get(new BlockPos(2, 0, 2)));
    }

    @Test
    void checkerboardFloorPlacesExactlyOneSubjectBlockAboveTheFloor() {
        BlockPos subjectPos = new BlockPos(1, 1, 1);
        Map<BlockPos, BlockState> blocks = PonderSchematicProvider.withCheckerboardFloor(
            3, 3, subjectPos, Blocks.CHEST.defaultBlockState());

        assertEquals(Blocks.CHEST.defaultBlockState(), blocks.get(subjectPos));
        assertEquals(10, blocks.size(), "3x3 floor (9) + 1 subject block");
    }

    @Test
    void checkerboardFloorHasExactlyTwoDistinctFloorColoursSoThePaletteIsThree() {
        Map<BlockPos, BlockState> blocks = PonderSchematicProvider.withCheckerboardFloor(
            3, 3, new BlockPos(1, 1, 1), Blocks.FURNACE.defaultBlockState());

        long distinctStates = blocks.values().stream().distinct().count();
        assertEquals(3, distinctStates, "white_concrete + light_gray_concrete + the subject block");
    }

    @Test
    void generatedNbtRoundTripsThroughRealVanillaStructureTemplateLoad() {
        Map<BlockPos, BlockState> blocks = PonderSchematicProvider.withCheckerboardFloor(
            3, 3, new BlockPos(1, 1, 1), Blocks.OAK_PLANKS.defaultBlockState());
        CompoundTag nbt = PonderSchematicProvider.buildStructureNbt(blocks, 3, 2, 3);

        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), nbt);

        assertEquals(3, template.getSize().getX());
        assertEquals(2, template.getSize().getY());
        assertEquals(3, template.getSize().getZ());

        List<StructureTemplate.StructureBlockInfo> loaded = blocksOf(template);
        assertEquals(10, loaded.size());

        Map<BlockPos, BlockState> loadedByPos = loaded.stream()
            .collect(java.util.stream.Collectors.toMap(StructureTemplate.StructureBlockInfo::pos, StructureTemplate.StructureBlockInfo::state));
        assertEquals(blocks, loadedByPos, "every position/state must survive the NBT round-trip unchanged");
    }

    // Same reflection SchematicLoader itself needs - StructureTemplate#filterBlocks has no
    // "give me everything" mode (see that class' own javadoc).
    @SuppressWarnings("unchecked")
    private static List<StructureTemplate.StructureBlockInfo> blocksOf(StructureTemplate template) {
        try {
            Field field = StructureTemplate.class.getDeclaredField("palettes");
            field.setAccessible(true);
            List<StructureTemplate.Palette> palettes = (List<StructureTemplate.Palette>) field.get(template);
            return new StructurePlaceSettings().getRandomPalette(palettes, BlockPos.ZERO).blocks();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
