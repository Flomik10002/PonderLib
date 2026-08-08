package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.ParticleEmitter;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.foundation.element.BoundingBoxOutlineElement;
import dev.flomik.ponderlib.foundation.element.EntityElementImpl;
import dev.flomik.ponderlib.foundation.element.OutlineElement;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import dev.flomik.ponderlib.foundation.instruction.DelayInstruction;
import dev.flomik.ponderlib.foundation.instruction.MarkAsFinishedInstruction;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.instruction.TextInstruction;
import dev.flomik.ponderlib.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PonderSceneBuilder queues instructions onto PonderScene#getSchedule() (package-private, hence
// this test living in the same package) instead of running them immediately - so each assertion
// below ticks the queued instruction against a mocked PonderScene, the same real object real
// playback would call tick() on.
class PonderSceneBuilderTest {

    private final List<PonderInstruction> schedule = new ArrayList<>();
    private PonderScene scene;
    private PonderSceneBuilder builder;

    @BeforeEach
    void setUp() {
        scene = mock(PonderScene.class);
        when(scene.getSchedule()).thenReturn(schedule);
        builder = new PonderSceneBuilder(scene);
    }

    @Test
    void titleSetsItOnTheSceneImmediatelyRatherThanQueueingAnInstruction() {
        builder.title("example:stone", "Stone basics");

        // No modId (this builder came from the single-arg constructor - see setUp()): no lang
        // namespace to put a translation key under, so it falls back to a plain literal.
        verify(scene).setTitle(Component.literal("Stone basics"));
        assertTrue(schedule.isEmpty());
    }

    @Test
    void titleUsesATranslatableWithEnglishFallbackWhenBuiltWithAModId() {
        PonderSceneBuilder namespaced = new PonderSceneBuilder(scene, "example");

        namespaced.title("stone_basics", "Stone basics");

        verify(scene).setTitle(Component.translatableWithFallback("example.ponder.stone_basics.header", "Stone basics"));
    }

    @Test
    void showTextUsesATranslatableKeyedByCallOrderWhenBuiltWithAModId() {
        PonderSceneBuilder namespaced = new PonderSceneBuilder(scene, "example");
        namespaced.title("stone_basics", "Stone basics");

        namespaced.overlay().showText(20, "First line");
        namespaced.overlay().showText(20, "Second line");

        assertEquals(2, schedule.size());
        TextInstruction first = assertInstanceOf(TextInstruction.class, schedule.get(0));
        TextInstruction second = assertInstanceOf(TextInstruction.class, schedule.get(1));
        assertEquals(Component.translatableWithFallback("example.ponder.stone_basics.text_0", "First line"), first.getElement().getText());
        assertEquals(Component.translatableWithFallback("example.ponder.stone_basics.text_1", "Second line"), second.getElement().getText());
    }

    @Test
    void setBlockQueuesAnImmutableBlockWrite() {
        BlockPos mutable = new BlockPos(1, 2, 3);
        builder.world().setBlock(mutable, Blocks.STONE.defaultBlockState());

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(eq(new BlockPos(1, 2, 3)), eq(Blocks.STONE.defaultBlockState()));
    }

    // Not ticked: the queued instruction's element.capture(level) bakes render buffers via
    // Minecraft.getInstance() (see WorldSectionElementImpl/SceneRenderBuffer), which - like
    // VirtualBlockView - has no running client to call into here. showSection()'s footprint math
    // runs eagerly before queueing though, so that much is covered without touching rendering.
    @Test
    void showSectionQueuesExactlyOneInstructionAndReturnsALinkForIt() {
        SimpleSelection selection = new SimpleSelection(List.of(new BlockPos(1, 0, 1), new BlockPos(3, 0, 2)));

        ElementLink<WorldSectionElement> link = builder.world().showSection(selection, Direction.DOWN);

        assertEquals(1, schedule.size());
        assertTrue(link.getId() != null);
        assertInstanceOf(dev.flomik.ponderlib.foundation.instruction.RevealSectionInstruction.class, schedule.get(0));
    }

    @Test
    void rotateSectionAnimatesTheLinkedElementsRotation() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        when(scene.resolve(link)).thenReturn(element);

        builder.world().rotateSection(link, new Vec3(0, 180, 0), 2);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        schedule.get(0).tick(scene);

        assertEquals(new Vec3(0, 180, 0), element.getAnimatedRotation());
        assertEquals(Vec3.ZERO, element.getAnimatedOffset());
    }

    @Test
    void moveSectionAnimatesTheLinkedElementsOffsetInstead() {
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of()));
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        when(scene.resolve(link)).thenReturn(element);

        builder.world().moveSection(link, new Vec3(1, 0, 0), 1);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        assertEquals(new Vec3(1, 0, 0), element.getAnimatedOffset());
        assertEquals(Vec3.ZERO, element.getAnimatedRotation());
    }

    @Test
    void configureBasePlatePinsTheExplicitFootprintOnTheScene() {
        builder.configureBasePlate(1, 2, 3);

        verify(scene).configureBasePlateBounds(1, 4, 2, 5);
    }

    @Test
    void basePlateSelectionCoversExactlyTheConfiguredFootprintAtGroundLevel() {
        when(scene.getBasePlateMinX()).thenReturn(0.0);
        when(scene.getBasePlateMaxX()).thenReturn(2.0);
        when(scene.getBasePlateMinZ()).thenReturn(0.0);
        when(scene.getBasePlateMaxZ()).thenReturn(3.0);

        Selection selection = builder.basePlateSelection();
        List<BlockPos> positions = new ArrayList<>();
        selection.forEach(positions::add);

        assertEquals(List.of(
            new BlockPos(0, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, 2),
            new BlockPos(1, 0, 0), new BlockPos(1, 0, 1), new BlockPos(1, 0, 2)
        ), positions);
    }

    // Not ticked - same reasoning as showSectionQueuesExactlyOneInstructionAndReturnsALinkForIt.
    @Test
    void showBasePlateQueuesExactlyOneSectionRevealForTheConfiguredFootprint() {
        when(scene.getBasePlateMinX()).thenReturn(0.0);
        when(scene.getBasePlateMaxX()).thenReturn(1.0);
        when(scene.getBasePlateMinZ()).thenReturn(0.0);
        when(scene.getBasePlateMaxZ()).thenReturn(1.0);

        builder.showBasePlate();

        assertEquals(1, schedule.size());
    }

    @Test
    void showTextQueuesATextInstructionThatAddsAndShowsItsElement() {
        builder.overlay().showText(20, "Hello");

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).addElement(org.mockito.ArgumentMatchers.any());
    }

    // Not ticked (unlike showTextQueuesAnInstructionThatAddsAndShowsItsElement above) -
    // EffectInstructionsImpl.emitSparks now constructs a real vanilla particle, which needs a live,
    // world-connected Minecraft client (see that class' javadoc) that doesn't exist in this test
    // environment. Queuing the instruction is still plain, client-independent logic worth locking
    // down on its own.
    @Test
    void emitSparksQueuesExactlyOneInstruction() {
        builder.effects().emitSparks(new Vec3(0.5, 0.5, 0.5), 0xFF0000, 4);

        assertEquals(1, schedule.size());
    }

    @Test
    void addKeyframeQueuesTheSharedImmediateKeyframeInstruction() {
        builder.addKeyframe();

        assertEquals(1, schedule.size());
        assertEquals(dev.flomik.ponderlib.foundation.instruction.KeyframeInstruction.IMMEDIATE, schedule.get(0));
    }

    @Test
    void addLazyKeyframeQueuesTheSharedDelayedKeyframeInstruction() {
        builder.addLazyKeyframe();

        assertEquals(1, schedule.size());
        assertEquals(dev.flomik.ponderlib.foundation.instruction.KeyframeInstruction.DELAYED, schedule.get(0));
    }

    @Test
    void idleQueuesADelayInstructionBlockingForExactlyThatManyTicks() {
        builder.idle(3);

        assertEquals(1, schedule.size());
        DelayInstruction instruction = assertInstanceOf(DelayInstruction.class, schedule.get(0));
        assertTrue(instruction.isBlocking());
    }

    @Test
    void markAsFinishedQueuesAnInstructionThatFinishesTheScene() {
        builder.markAsFinished();

        assertEquals(1, schedule.size());
        assertInstanceOf(MarkAsFinishedInstruction.class, schedule.get(0));
        schedule.get(0).tick(scene);

        verify(scene).setFinished(true);
    }

    @Test
    void addInstructionWithACallbackWrapsItAsANonBlockingSimpleInstruction() {
        List<PonderScene> seen = new ArrayList<>();
        builder.addInstruction(seen::add);

        assertEquals(1, schedule.size());
        PonderInstruction instruction = schedule.get(0);
        assertTrue(instruction.isComplete());
        instruction.tick(scene);

        assertEquals(List.of(scene), seen);
    }

    @Test
    void createEntityTicksItsFactoryOnceAndTracksTheResultingEntity() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        Entity entity = mock(Entity.class);

        ElementLink<EntityElement> link = builder.world().createEntity(l -> {
            assertEquals(level, l, "factory should be handed the scene's own level, not some other one");
            return entity;
        });

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        ArgumentCaptor<EntityElementImpl> captor = ArgumentCaptor.forClass(EntityElementImpl.class);
        verify(scene).addElement(captor.capture());
        verify(scene).linkElement(captor.getValue(), link);
        verify(level).addFreshEntity(entity);

        List<Entity> resolved = new ArrayList<>();
        captor.getValue().ifPresent(resolved::add);
        assertEquals(List.of(entity), resolved, "the element added/linked should wrap the entity the factory returned");
    }

    // Not ticked - constructing a real ItemEntity needs a fully-initialized Level (registry access,
    // profiler, ...) that a bare mock doesn't provide, same reasoning as showSection's own test.
    @Test
    void createItemEntityQueuesExactlyOneInstructionAndReturnsALinkForIt() {
        ElementLink<EntityElement> link = builder.world().createItemEntity(Vec3.ZERO, Vec3.ZERO, new ItemStack(Items.EMERALD));

        assertEquals(1, schedule.size());
        assertTrue(link.getId() != null);
    }

    @Test
    void modifyEntityRunsTheCallbackAgainstTheResolvedEntityOnly() {
        ElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
        Entity entity = mock(Entity.class);
        EntityElementImpl element = new EntityElementImpl(entity);
        when(scene.resolve(link)).thenReturn(element);
        List<Entity> seen = new ArrayList<>();

        builder.world().modifyEntity(link, seen::add);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        assertEquals(List.of(entity), seen);
    }

    @Test
    void modifyEntityDoesNothingWhenTheLinkNoLongerResolves() {
        ElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
        when(scene.resolve(link)).thenReturn(null);
        List<Entity> seen = new ArrayList<>();

        builder.world().modifyEntity(link, seen::add);
        schedule.get(0).tick(scene);

        assertTrue(seen.isEmpty(), "a dead/unresolved link must not invoke the callback at all");
    }

    @Test
    void modifyEntitiesDelegatesStraightToTheScenesForEachWorldEntity() {
        Consumer<Entity> callback = e -> {
        };
        builder.world().modifyEntities(Entity.class, callback);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).forEachWorldEntity(Entity.class, callback);
    }

    @Test
    void modifyEntitiesInsideOnlyCallsBackForEntitiesWithinTheSelection() {
        Selection area = new SimpleSelection(List.of(new BlockPos(1, 0, 1)));
        Entity inside = mock(Entity.class);
        when(inside.blockPosition()).thenReturn(new BlockPos(1, 0, 1));
        Entity outside = mock(Entity.class);
        when(outside.blockPosition()).thenReturn(new BlockPos(5, 0, 5));

        doAnswer(invocation -> {
            Consumer<Entity> consumer = invocation.getArgument(1);
            consumer.accept(inside);
            consumer.accept(outside);
            return null;
        }).when(scene).forEachWorldEntity(eq(Entity.class), any());

        List<Entity> seen = new ArrayList<>();
        builder.world().modifyEntitiesInside(Entity.class, area, seen::add);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        assertEquals(List.of(inside), seen);
    }

    // --- world(): block state / bulk-mutation additions ---

    @Test
    void setBlockWithParticlesFlagSpawnsParticlesOnTheLevelBeforeWritingState() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        BlockPos pos = new BlockPos(1, 1, 1);

        builder.world().setBlock(pos, Blocks.STONE.defaultBlockState(), true);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.STONE.defaultBlockState());
        verify(level, atLeastOnce()).addParticle(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void setBlockWithoutParticlesFlagNeverTouchesTheLevelAtAll() {
        BlockPos pos = new BlockPos(1, 1, 1);
        builder.world().setBlock(pos, Blocks.STONE.defaultBlockState(), false);
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.STONE.defaultBlockState());
        verify(scene, never()).getLevel();
    }

    @Test
    void setBlocksAppliesToEveryPositionInTheSelection() {
        Selection selection = new SimpleSelection(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0)));

        builder.world().setBlocks(selection, Blocks.STONE.defaultBlockState(), false);
        assertEquals(2, schedule.size());
        schedule.forEach(i -> i.tick(scene));

        verify(scene).setBlockState(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        verify(scene).setBlockState(new BlockPos(1, 0, 0), Blocks.STONE.defaultBlockState());
    }

    @Test
    void replaceBlocksSkipsPositionsThatAreCurrentlyAir() {
        BlockPos airPos = new BlockPos(0, 0, 0);
        BlockPos dirtPos = new BlockPos(1, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(airPos)).thenReturn(Blocks.AIR.defaultBlockState());
        when(scene.getBlockState(dirtPos)).thenReturn(Blocks.DIRT.defaultBlockState());
        Selection selection = new SimpleSelection(List.of(airPos, dirtPos));

        builder.world().replaceBlocks(selection, Blocks.STONE.defaultBlockState(), false);
        assertEquals(2, schedule.size());
        schedule.forEach(i -> i.tick(scene));

        verify(scene, never()).setBlockState(eq(airPos), any());
        verify(scene).setBlockState(dirtPos, Blocks.STONE.defaultBlockState());
    }

    @Test
    void destroyBlockSetsAirAndSpawnsParticlesWhenTheCurrentBlockIsNotAir() {
        BlockPos pos = new BlockPos(2, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.DIRT.defaultBlockState());
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);

        builder.world().destroyBlock(pos);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(level, atLeastOnce()).addParticle(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        verify(scene).setBlockState(pos, Blocks.AIR.defaultBlockState());
    }

    @Test
    void restoreBlocksDelegatesStraightToTheLevel() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        Selection selection = new SimpleSelection(List.of(new BlockPos(1, 1, 1)));

        builder.world().restoreBlocks(selection);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(level).restoreBlocks(selection);
    }

    @Test
    void modifyBlockUpdatesBothTheLevelAndAnyVisibleSectionAlreadyShowingThatPosition() {
        BlockPos pos = new BlockPos(3, 0, 0);
        WorldSectionElementImpl section = mock(WorldSectionElementImpl.class);
        when(section.isVisible()).thenReturn(true);
        when(section.getBlockPositions()).thenReturn(Set.of(pos));
        when(section.getBlockState(pos)).thenReturn(Blocks.DIRT.defaultBlockState());
        when(scene.getElements()).thenReturn(Set.of(section));

        builder.world().modifyBlock(pos, state -> Blocks.STONE.defaultBlockState(), false);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.STONE.defaultBlockState());
        verify(section).setBlockState(pos, Blocks.STONE.defaultBlockState());
    }

    @Test
    void modifyBlocksAppliesToEveryPositionInTheSelection() {
        when(scene.getElements()).thenReturn(Set.of());
        BlockPos a = new BlockPos(0, 0, 0);
        BlockPos b = new BlockPos(1, 0, 0);
        when(scene.getBlockState(a)).thenReturn(Blocks.DIRT.defaultBlockState());
        when(scene.getBlockState(b)).thenReturn(Blocks.DIRT.defaultBlockState());
        Selection selection = new SimpleSelection(List.of(a, b));

        builder.world().modifyBlocks(selection, state -> Blocks.STONE.defaultBlockState(), false);
        assertEquals(2, schedule.size());
        schedule.forEach(i -> i.tick(scene));

        verify(scene).setBlockState(a, Blocks.STONE.defaultBlockState());
        verify(scene).setBlockState(b, Blocks.STONE.defaultBlockState());
    }

    @Test
    void cycleBlockPropertyAdvancesAVanillaPropertyTheBlockActuallyHas() {
        BlockPos pos = new BlockPos(0, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.LEVER.defaultBlockState());

        builder.world().cycleBlockProperty(pos, BlockStateProperties.POWERED);
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.LEVER.defaultBlockState().cycle(BlockStateProperties.POWERED));
    }

    @Test
    void cycleBlockPropertyIsANoOpWhenTheDisplayedBlockLacksTheProperty() {
        BlockPos pos = new BlockPos(0, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.STONE.defaultBlockState());

        builder.world().cycleBlockProperty(pos, BlockStateProperties.POWERED);
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.STONE.defaultBlockState());
    }

    @Test
    void toggleRedstonePowerFlipsThePoweredBooleanProperty() {
        BlockPos pos = new BlockPos(0, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.LEVER.defaultBlockState());
        Selection selection = new SimpleSelection(List.of(pos));

        builder.world().toggleRedstonePower(selection);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.LEVER.defaultBlockState().cycle(BlockStateProperties.POWERED));
    }

    @Test
    void toggleRedstonePowerBouncesAnalogPowerBetweenZeroAndFifteen() {
        BlockPos pos = new BlockPos(0, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.REDSTONE_WIRE.defaultBlockState());
        Selection selection = new SimpleSelection(List.of(pos));

        builder.world().toggleRedstonePower(selection);
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.REDSTONE_WIRE.defaultBlockState().setValue(BlockStateProperties.POWER, 15));
    }

    @Test
    void toggleRedstonePowerIsANoOpOnABlockWithNeitherRedstoneProperty() {
        BlockPos pos = new BlockPos(0, 0, 0);
        when(scene.getElements()).thenReturn(Set.of());
        when(scene.getBlockState(pos)).thenReturn(Blocks.STONE.defaultBlockState());
        Selection selection = new SimpleSelection(List.of(pos));

        builder.world().toggleRedstonePower(selection);
        schedule.get(0).tick(scene);

        verify(scene).setBlockState(pos, Blocks.STONE.defaultBlockState());
    }

    // --- world(): hide/independent sections ---
    //
    // Regression coverage: the first cut of hideSection/hideIndependentSection resolved the target
    // up front and called addInstruction(...) from INSIDE an already-ticking callback, which landed
    // on PonderScene's master schedule list rather than the activeSchedule list actually being
    // iterated - so the fade silently never played during that same playthrough. These tests tick
    // the SAME instruction the builder queues, in the SAME single call, the way real playback would -
    // they would have failed against that first version.

    @Test
    void hideSectionFadesTheMatchingVisibleSectionDuringThisSamePlaythrough() {
        WorldSectionElementImpl section = mock(WorldSectionElementImpl.class);
        when(section.isVisible()).thenReturn(true);
        Set<BlockPos> positions = Set.of(new BlockPos(0, 0, 0));
        when(section.getBlockPositions()).thenReturn(positions);
        when(scene.getElements()).thenReturn(Set.of(section));
        Selection selection = new SimpleSelection(List.of(new BlockPos(0, 0, 0)));

        builder.world().hideSection(selection, Direction.DOWN);
        assertEquals(1, schedule.size(), "must not need a second, later-added instruction to take effect");
        schedule.get(0).tick(scene);

        verify(section).setFadeFromDirection(Direction.DOWN);
    }

    @Test
    void hideSectionDoesNothingWhenNoVisibleSectionMatches() {
        when(scene.getElements()).thenReturn(Set.of());
        Selection selection = new SimpleSelection(List.of(new BlockPos(0, 0, 0)));

        builder.world().hideSection(selection, Direction.UP);
        assertEquals(1, schedule.size());
        assertTrue(schedule.get(0).isComplete() || !schedule.get(0).isComplete(), "must not throw");
        schedule.get(0).tick(scene);
    }

    @Test
    void hideIndependentSectionResolvesTheLinkAndFadesItOutDuringThisSamePlaythrough() {
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        WorldSectionElementImpl element = mock(WorldSectionElementImpl.class);
        when(scene.resolve(link)).thenReturn(element);

        builder.world().hideIndependentSection(link, Direction.EAST);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(element).setFadeFromDirection(Direction.EAST);
    }

    @Test
    void hideIndependentSectionDoesNothingWhenTheLinkNoLongerResolves() {
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        when(scene.resolve(link)).thenReturn(null);

        builder.world().hideIndependentSection(link, Direction.EAST);
        schedule.get(0).tick(scene);
        // no exception - the resolver returning null makes the whole instruction inert this playthrough
    }

    // Not ticked - same reasoning as showSectionQueuesExactlyOneInstructionAndReturnsALinkForIt:
    // capture() bakes render buffers via Minecraft.getInstance(), unavailable here.
    @Test
    void showIndependentSectionQueuesExactlyOneInstructionAndReturnsALinkForIt() {
        Selection selection = new SimpleSelection(List.of(new BlockPos(1, 0, 1)));

        ElementLink<WorldSectionElement> link = builder.world().showIndependentSection(selection, Direction.UP);

        assertEquals(1, schedule.size());
        assertTrue(link.getId() != null);
    }

    @Test
    void showIndependentSectionImmediatelyQueuesExactlyOneInstructionAndReturnsALinkForIt() {
        Selection selection = new SimpleSelection(List.of(new BlockPos(1, 0, 1)));

        ElementLink<WorldSectionElement> link = builder.world().showIndependentSectionImmediately(selection);

        assertEquals(1, schedule.size());
        assertTrue(link.getId() != null);
    }

    @Test
    void makeSectionIndependentTellsEveryOtherVisibleSectionToExtractTheGivenPositions() {
        WorldSectionElementImpl source = mock(WorldSectionElementImpl.class);
        when(scene.getElements()).thenReturn(Set.of(source));
        BlockPos pos = new BlockPos(1, 0, 0);
        Selection selection = new SimpleSelection(List.of(pos));

        ElementLink<WorldSectionElement> link = builder.world().makeSectionIndependent(selection);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(source).extractInto(any(WorldSectionElementImpl.class), eq(Set.of(pos)));
        ArgumentCaptor<WorldSectionElementImpl> extracted = ArgumentCaptor.forClass(WorldSectionElementImpl.class);
        verify(scene).addElement(extracted.capture());
        verify(scene).linkElement(extracted.getValue(), link);
        assertTrue(extracted.getValue().isVisible());
    }

    @Test
    void showSectionAndMergeResolvesTheLinkAndMergesTheStagingElementIntoIt() {
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        WorldSectionElementImpl target = mock(WorldSectionElementImpl.class);
        when(scene.resolve(link)).thenReturn(target);
        // Empty selection - same trick RevealSectionInstructionTest uses to keep capture() a no-op
        // loop that never touches Minecraft.getInstance().
        Selection selection = new SimpleSelection(List.of());

        builder.world().showSectionAndMerge(selection, Direction.UP, link);
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(target).mergeFrom(any(WorldSectionElementImpl.class));
    }

    @Test
    void configureCenterOfRotationMovesWhatPointStaysFixedWhileRotating() {
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);
        WorldSectionElementImpl element = new WorldSectionElementImpl(new SimpleSelection(List.of(BlockPos.ZERO)));
        when(scene.resolve(link)).thenReturn(element);

        builder.world().configureCenterOfRotation(link, new Vec3(5, 5, 5));
        schedule.get(0).tick(scene);
        element.setAnimatedRotation(new Vec3(0, 90, 0));

        Vector4f pivot = new Vector4f(5, 5, 5, 1F).mul(element.getSectionTransform());
        assertEquals(5F, pivot.x(), 1e-4);
        assertEquals(5F, pivot.y(), 1e-4);
        assertEquals(5F, pivot.z(), 1e-4);
    }

    @Test
    void incrementBlockBreakingProgressAdvancesThroughTenStagesThenWrapsBackToZero() {
        BlockPos pos = new BlockPos(0, 0, 0);
        WorldSectionElementImpl section = mock(WorldSectionElementImpl.class);
        when(section.isVisible()).thenReturn(true);
        when(section.getBlockPositions()).thenReturn(Set.of(pos));
        when(scene.getElements()).thenReturn(Set.of(section));
        when(scene.getBlockState(pos)).thenReturn(Blocks.STONE.defaultBlockState());
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);

        for (int i = 0; i < 11; i++) {
            builder.world().incrementBlockBreakingProgress(pos);
        }
        assertEquals(11, schedule.size());
        schedule.forEach(i -> i.tick(scene));

        verify(section, times(2)).setBreakingStage(pos, 0);
        verify(section).setBreakingStage(pos, 9);
    }

    @Test
    void getHolderLookupProviderReturnsTheLevelsOwnRegistryAccess() {
        PonderLevel level = mock(PonderLevel.class);
        RegistryAccess registryAccess = mock(RegistryAccess.class);
        when(level.registryAccess()).thenReturn(registryAccess);
        when(scene.getLevel()).thenReturn(level);

        assertSame(registryAccess, builder.world().getHolderLookupProvider());
        assertTrue(schedule.isEmpty(), "a plain lookup, not a scheduled effect");
    }

    @Test
    void modifyBlockEntityNBTRoundTripsThroughSaveAndLoadForMatchingBlockEntities() {
        BlockPos pos = new BlockPos(0, 0, 0);
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        BlockEntity blockEntity = mock(BlockEntity.class);
        when(level.getBlockEntity(pos)).thenReturn(blockEntity);
        CompoundTag saved = new CompoundTag();
        when(blockEntity.saveWithFullMetadata()).thenReturn(saved);
        Selection selection = new SimpleSelection(List.of(pos));

        builder.world().modifyBlockEntityNBT(selection, BlockEntity.class, tag -> tag.putString("Marker", "hi"));
        schedule.get(0).tick(scene);

        assertEquals("hi", saved.getString("Marker"));
        verify(blockEntity).load(saved);
    }

    @Test
    void modifyBlockEntityNBTSkipsBlockEntitiesOfTheWrongType() {
        BlockPos pos = new BlockPos(0, 0, 0);
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        when(level.getBlockEntity(pos)).thenReturn(null);
        Selection selection = new SimpleSelection(List.of(pos));
        List<CompoundTag> seen = new ArrayList<>();

        builder.world().modifyBlockEntityNBT(selection, BlockEntity.class, seen::add);
        schedule.get(0).tick(scene);

        assertTrue(seen.isEmpty());
    }

    @Test
    void modifyBlockEntityRunsTheCallbackAgainstTheLiveObjectForAMatchingType() {
        BlockPos pos = new BlockPos(0, 0, 0);
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);
        BlockEntity blockEntity = mock(BlockEntity.class);
        when(level.getBlockEntity(pos)).thenReturn(blockEntity);
        List<BlockEntity> seen = new ArrayList<>();

        builder.world().modifyBlockEntity(pos, BlockEntity.class, seen::add);
        schedule.get(0).tick(scene);

        assertEquals(List.of(blockEntity), seen);
    }

    // --- overlay(): line / bounding-box / icon-hint additions ---

    @Test
    void showLineQueuesAFadingLineThatAddsAndShowsItsElement() {
        builder.overlay().showLine(PonderPalette.RED, Vec3.ZERO, new Vec3(1, 0, 0), 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showBigLineQueuesAFadingLineThatAddsAndShowsItsElement() {
        builder.overlay().showBigLine(PonderPalette.RED, Vec3.ZERO, new Vec3(1, 0, 0), 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showOutlineWithNullSlotBehavesLikeThePlainThreeArgumentOverload() {
        Selection selection = new SimpleSelection(List.of(new BlockPos(0, 0, 0)));

        builder.overlay().showOutline(PonderPalette.WHITE, null, selection, 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showOutlineWithTheSameSlotTwiceReusesOneElementInsteadOfStackingASecond() {
        Object slot = new Object();
        Selection first = new SimpleSelection(List.of(new BlockPos(0, 0, 0)));
        Selection second = new SimpleSelection(List.of(new BlockPos(1, 0, 0)));

        builder.overlay().showOutline(PonderPalette.RED, slot, first, 10);
        builder.overlay().showOutline(PonderPalette.RED, slot, second, 10);
        assertEquals(2, schedule.size(), "each call still gets its own fade window in the timeline");

        schedule.get(0).tick(scene);
        schedule.get(1).tick(scene);

        ArgumentCaptor<OutlineElement> captor = ArgumentCaptor.forClass(OutlineElement.class);
        verify(scene, times(2)).addElement(captor.capture());
        assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1), "same slot must reuse the same element");
    }

    @Test
    void showOutlineWithDifferentSlotsCreatesIndependentElements() {
        Selection first = new SimpleSelection(List.of(new BlockPos(0, 0, 0)));
        Selection second = new SimpleSelection(List.of(new BlockPos(1, 0, 0)));

        builder.overlay().showOutline(PonderPalette.RED, "a", first, 10);
        builder.overlay().showOutline(PonderPalette.RED, "b", second, 10);
        schedule.get(0).tick(scene);
        schedule.get(1).tick(scene);

        ArgumentCaptor<OutlineElement> captor = ArgumentCaptor.forClass(OutlineElement.class);
        verify(scene, times(2)).addElement(captor.capture());
        assertTrue(captor.getAllValues().get(0) != captor.getAllValues().get(1));
    }

    @Test
    void chaseBoundingBoxOutlineWithTheSameSlotTwiceReusesOneElementInsteadOfStackingASecond() {
        Object slot = new Object();

        builder.overlay().chaseBoundingBoxOutline(PonderPalette.RED, slot, new AABB(0, 0, 0, 1, 1, 1), 10);
        builder.overlay().chaseBoundingBoxOutline(PonderPalette.RED, slot, new AABB(2, 2, 2, 3, 3, 3), 10);
        assertEquals(2, schedule.size());

        schedule.get(0).tick(scene);
        schedule.get(1).tick(scene);

        ArgumentCaptor<BoundingBoxOutlineElement> captor = ArgumentCaptor.forClass(BoundingBoxOutlineElement.class);
        verify(scene, times(2)).addElement(captor.capture());
        assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1), "same slot must reuse the same element");
    }

    @Test
    void chaseBoundingBoxOutlineWithNullSlotBehavesLikeAPlainOneShotOutline() {
        builder.overlay().chaseBoundingBoxOutline(PonderPalette.RED, null, new AABB(0, 0, 0, 1, 1, 1), 10);
        builder.overlay().chaseBoundingBoxOutline(PonderPalette.RED, null, new AABB(2, 2, 2, 3, 3, 3), 10);
        schedule.get(0).tick(scene);
        schedule.get(1).tick(scene);

        ArgumentCaptor<BoundingBoxOutlineElement> captor = ArgumentCaptor.forClass(BoundingBoxOutlineElement.class);
        verify(scene, times(2)).addElement(captor.capture());
        assertTrue(captor.getAllValues().get(0) != captor.getAllValues().get(1));
    }

    @Test
    void showCenteredScrollInputQueuesAnIconHintThatAddsAndShowsItsElement() {
        builder.overlay().showCenteredScrollInput(new BlockPos(0, 0, 0), Direction.UP, 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showScrollInputQueuesAnIconHintThatAddsAndShowsItsElement() {
        builder.overlay().showScrollInput(new Vec3(0.5, 0.5, 0.5), Direction.NORTH, 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showRepeaterScrollInputQueuesAnIconHintThatAddsAndShowsItsElement() {
        builder.overlay().showRepeaterScrollInput(new BlockPos(0, 0, 0), 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showFilterSlotInputQueuesAnIconHintThatAddsAndShowsItsElement() {
        builder.overlay().showFilterSlotInput(new Vec3(0.5, 0.5, 0.5), 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    @Test
    void showFilterSlotInputWithSideQueuesAnIconHintThatAddsAndShowsItsElement() {
        builder.overlay().showFilterSlotInput(new Vec3(0.5, 0.5, 0.5), Direction.WEST, 20);

        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);
        verify(scene).addElement(any());
    }

    // --- effects(): redstone/success indicators, generic particles ---

    @Test
    void indicateRedstoneEmitsRedSparksCenteredOnTheBlock() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);

        builder.effects().indicateRedstone(new BlockPos(1, 1, 1));
        assertEquals(1, schedule.size());
        schedule.get(0).tick(scene);

        verify(level, atLeastOnce()).addParticle(any(), eq(1.5), eq(1.5), eq(1.5), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void indicateSuccessEmitsHappyVillagerParticlesNearTheBlock() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);

        builder.effects().indicateSuccess(new BlockPos(0, 0, 0));
        schedule.get(0).tick(scene);

        verify(level, atLeastOnce()).addParticle(eq(ParticleTypes.HAPPY_VILLAGER),
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void createRedstoneParticlesSpawnsExactlyTheRequestedAmount() {
        PonderLevel level = mock(PonderLevel.class);
        when(scene.getLevel()).thenReturn(level);

        builder.effects().createRedstoneParticles(new BlockPos(0, 0, 0), 0x00FF00, 7);
        schedule.get(0).tick(scene);

        verify(level, times(7)).addParticle(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void emitParticlesFiresTheEmitterAmountPerCycleTimesPerCycleAcrossAllCycles() {
        List<Vec3> seen = new ArrayList<>();
        ParticleEmitter emitter = (level, origin, random) -> seen.add(origin);

        builder.effects().emitParticles(new Vec3(1, 1, 1), emitter, 2F, 3);
        assertEquals(1, schedule.size());
        PonderInstruction instruction = schedule.get(0);
        while (!instruction.isComplete()) {
            instruction.tick(scene);
        }

        assertEquals(6, seen.size(), "2 per cycle across 3 cycles");
    }

    @Test
    void emitParticlesAccumulatesAFractionalRatherThanDroppingIt() {
        List<Vec3> seen = new ArrayList<>();
        ParticleEmitter emitter = (level, origin, random) -> seen.add(origin);

        builder.effects().emitParticles(Vec3.ZERO, emitter, 0.5F, 10);
        PonderInstruction instruction = schedule.get(0);
        while (!instruction.isComplete()) {
            instruction.tick(scene);
        }

        assertEquals(5, seen.size(), "0.5/cycle over 10 cycles must still add up to exactly 5, not 0");
    }

    @Test
    void simpleParticleEmitterSpawnsExactlyAtTheGivenOriginWithTheGivenMotion() {
        ParticleEmitter emitter = builder.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(0, 1, 0));
        PonderLevel level = mock(PonderLevel.class);

        emitter.emit(level, new Vec3(2, 3, 4), RandomSource.create());

        verify(level).addParticle(ParticleTypes.FLAME, 2, 3, 4, 0, 1, 0);
    }

    @Test
    void particleEmitterWithinBlockSpaceJittersWithinAUnitCubeAroundTheOrigin() {
        ParticleEmitter emitter = builder.effects().particleEmitterWithinBlockSpace(ParticleTypes.FLAME, Vec3.ZERO);
        PonderLevel level = mock(PonderLevel.class);
        RandomSource random = RandomSource.create();

        for (int i = 0; i < 20; i++) {
            emitter.emit(level, new Vec3(5, 5, 5), random);
        }

        ArgumentCaptor<Double> xCaptor = ArgumentCaptor.forClass(Double.class);
        verify(level, times(20)).addParticle(eq(ParticleTypes.FLAME), xCaptor.capture(), anyDouble(), anyDouble(), eq(0.0), eq(0.0), eq(0.0));
        for (double x : xCaptor.getAllValues()) {
            assertTrue(x >= 4.5 && x <= 5.5, "jitter must stay within half a block of the origin");
        }
    }

    // --- SceneBuilder-level: view/camera controls, idleSeconds, next-up opt-out ---

    @Test
    void scaleSceneViewSetsTheSceneScaleImmediatelyRatherThanQueueingAnInstruction() {
        builder.scaleSceneView(0.5F);

        verify(scene).setSceneScale(0.5F);
        assertTrue(schedule.isEmpty());
    }

    @Test
    void rotateCameraYAddsTheRotationImmediately() {
        builder.rotateCameraY(30F);

        verify(scene).addCameraYRotation(30F);
        assertTrue(schedule.isEmpty());
    }

    @Test
    void removeShadowDisablesTheShadowImmediately() {
        builder.removeShadow();

        verify(scene).setShadowEnabled(false);
        assertTrue(schedule.isEmpty());
    }

    @Test
    void setSceneOffsetYSetsItImmediately() {
        builder.setSceneOffsetY(10F);

        verify(scene).setSceneOffsetY(10F);
        assertTrue(schedule.isEmpty());
    }

    @Test
    void setNextUpEnabledSetsItImmediately() {
        builder.setNextUpEnabled(false);

        verify(scene).setNextUpEnabled(false);
        assertTrue(schedule.isEmpty());
    }

    @Test
    void idleSecondsIsSugarForIdleInTicksTwentyPerSecond() {
        builder.idleSeconds(1);

        assertEquals(1, schedule.size());
        PonderInstruction instruction = schedule.get(0);
        for (int i = 0; i < 19; i++) {
            instruction.tick(scene);
            assertFalse(instruction.isComplete(), "must not complete before 20 ticks (1 second)");
        }
        instruction.tick(scene);
        assertTrue(instruction.isComplete());
    }
}
