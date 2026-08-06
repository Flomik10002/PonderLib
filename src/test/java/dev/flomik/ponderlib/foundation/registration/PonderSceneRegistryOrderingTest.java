package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PonderSceneRegistryOrderingTest {

    private static final ResourceLocation COMPONENT = ResourceLocation.withDefaultNamespace("stone");
    private static final PonderStoryBoard NOOP = (scene, util) -> {
    };

    @Test
    void entriesWithNoOrderingConstraintsKeepRegistrationOrder() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        StoryBoardEntry first = entry("first");
        StoryBoardEntry second = entry("second");
        registry.register(first);
        registry.register(second);

        assertEquals(List.of(first, second), registry.getScenes(COMPONENT));
    }

    @Test
    void orderBeforeMovesAnEntryAheadOfTheOneItReferences() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        // Registered first-then-second, but "second" declares it must come before "first".
        StoryBoardEntry first = entry("first");
        StoryBoardEntry second = new PonderStoryBoardEntry(COMPONENT, location("second"), NOOP,
            Set.of(), Set.of(location("first")), Set.of());
        registry.register(first);
        registry.register(second);

        assertEquals(List.of(second, first), registry.getScenes(COMPONENT));
    }

    @Test
    void orderAfterMovesAnEntryBehindTheOneItReferences() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        // Registered first-then-second, but "first" declares it must come after "second".
        StoryBoardEntry first = new PonderStoryBoardEntry(COMPONENT, location("first"), NOOP,
            Set.of(), Set.of(), Set.of(location("second")));
        StoryBoardEntry second = entry("second");
        registry.register(first);
        registry.register(second);

        assertEquals(List.of(second, first), registry.getScenes(COMPONENT));
    }

    @Test
    void referencesToEntriesOutsideThisComponentAreIgnoredRatherThanCrashing() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        StoryBoardEntry entry = new PonderStoryBoardEntry(COMPONENT, location("only"), NOOP,
            Set.of(), Set.of(ResourceLocation.withDefaultNamespace("ponder/never_registered.nbt")), Set.of());
        registry.register(entry);

        assertEquals(List.of(entry), registry.getScenes(COMPONENT));
    }

    @Test
    void aCycleFallsBackToIncludingEveryEntryInsteadOfCrashingOrDroppingAny() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        StoryBoardEntry first = new PonderStoryBoardEntry(COMPONENT, location("first"), NOOP,
            Set.of(), Set.of(location("second")), Set.of());
        StoryBoardEntry second = new PonderStoryBoardEntry(COMPONENT, location("second"), NOOP,
            Set.of(), Set.of(location("first")), Set.of());
        registry.register(first);
        registry.register(second);

        List<StoryBoardEntry> scenes = registry.getScenes(COMPONENT);
        assertEquals(2, scenes.size());
        assertTrue(scenes.containsAll(List.of(first, second)));
    }

    @Test
    void getScenesByTagFindsEntriesAcrossComponentsCarryingThatTag() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        ResourceLocation basics = ResourceLocation.withDefaultNamespace("basics");
        StoryBoardEntry tagged = new PonderStoryBoardEntry(COMPONENT, location("tagged"), NOOP, Set.of(basics), Set.of(), Set.of());
        StoryBoardEntry untagged = entry("untagged");
        registry.register(tagged);
        registry.register(untagged);

        assertEquals(List.of(tagged), registry.getScenesByTag(basics));
    }

    @Test
    void getAllTagsReturnsEveryDistinctTagSortedByNamespaceThenPath() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        ResourceLocation zTag = ResourceLocation.fromNamespaceAndPath("zzz_mod", "basics");
        ResourceLocation aTag = ResourceLocation.fromNamespaceAndPath("aaa_mod", "basics");
        ResourceLocation duplicateAcrossEntries = ResourceLocation.fromNamespaceAndPath("aaa_mod", "advanced");
        StoryBoardEntry first = new PonderStoryBoardEntry(COMPONENT, location("first"), NOOP,
            Set.of(zTag, duplicateAcrossEntries), Set.of(), Set.of());
        StoryBoardEntry second = new PonderStoryBoardEntry(COMPONENT, location("second"), NOOP,
            Set.of(aTag, duplicateAcrossEntries), Set.of(), Set.of());
        registry.register(first);
        registry.register(second);

        // aaa_mod entries first (namespace sorts before zzz_mod), "advanced" before "basics"
        // within that namespace, and the tag shared by both entries appears only once.
        assertEquals(List.of(duplicateAcrossEntries, aTag, zTag), registry.getAllTags());
    }

    @Test
    void getAllTagsIsEmptyWhenNoEntryCarriesAnyTag() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        registry.register(entry("untagged"));

        assertEquals(List.of(), registry.getAllTags());
    }

    private static StoryBoardEntry entry(String path) {
        return new PonderStoryBoardEntry(COMPONENT, location(path), NOOP);
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath("ponderlib_test", "ponder/" + path + ".nbt");
    }
}
