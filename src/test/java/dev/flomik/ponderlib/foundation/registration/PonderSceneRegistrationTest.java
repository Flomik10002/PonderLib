package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PonderSceneRegistrationTest {

    @Test
    void helperBuildsNamespacedSchematicLocationAndRegistersEntry() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        DefaultPonderSceneRegistrationHelper helper = new DefaultPonderSceneRegistrationHelper("example", registry);
        ResourceLocation component = ResourceLocation.withDefaultNamespace("stone");
        PonderStoryBoard board = (scene, util) -> { };

        StoryBoardEntry entry = helper.addStoryBoard(component, "machines/stone", board);

        assertEquals("example", helper.getModId());
        assertEquals(ResourceLocation.fromNamespaceAndPath("example", "local"), helper.asLocation("local"));
        assertEquals(component, entry.getComponent());
        assertEquals(ResourceLocation.fromNamespaceAndPath("example", "ponder/machines/stone.nbt"), entry.getSchematicLocation());
        assertSame(board, entry.getBoard());
        assertTrue(registry.doScenesExistForId(component));
        assertEquals(1, registry.getScenes(component).size());
    }

    @Test
    void registryPreservesRegistrationOrderAndExposesAnEmptyViewForUnknownComponents() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        ResourceLocation component = ResourceLocation.withDefaultNamespace("stone");
        PonderStoryBoard first = (scene, util) -> { };
        PonderStoryBoard second = (scene, util) -> { };
        registry.register(new PonderStoryBoardEntry(component, ResourceLocation.parse("test:first"), first));
        registry.register(new PonderStoryBoardEntry(component, ResourceLocation.parse("test:second"), second));

        assertEquals(java.util.List.of(first, second), registry.getScenes(component).stream().map(StoryBoardEntry::getBoard).toList());
        ResourceLocation missing = ResourceLocation.parse("test:missing");
        assertFalse(registry.doScenesExistForId(missing));
        assertEquals(new ArrayList<>(), new ArrayList<>(registry.getScenes(missing)));
    }
}
