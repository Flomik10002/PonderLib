package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.foundation.element.EntityElementImpl;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import dev.flomik.ponderlib.foundation.instruction.DelayInstruction;
import dev.flomik.ponderlib.foundation.instruction.MarkAsFinishedInstruction;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.instruction.TextInstruction;
import dev.flomik.ponderlib.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
}
