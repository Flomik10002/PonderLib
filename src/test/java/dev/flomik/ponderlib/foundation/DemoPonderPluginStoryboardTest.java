package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.demo.DemoPonderPlugin;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.registration.DefaultPonderSceneRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Lives in foundation (not demo, where DemoPonderPlugin itself lives) because it needs
// PonderScene#getSchedule() - package-private the same way PonderSceneBuilderTest already relies
// on, and for the same reason (see that class' own comment).
class DemoPonderPluginStoryboardTest {

    // Not ticked - showSection's RevealSectionInstruction bakes render buffers via
    // Minecraft.getInstance() (see WorldSectionElementImpl/SceneRenderBuffer), unavailable in a bare
    // JUnit run. Programming the storyboard itself touches none of that: it only builds and queues
    // instructions, so this still catches a storyboard that throws while being written (a bad
    // util.select()/grid() call, a typo in a block-selection index, etc), and confirms it actually
    // queues something rather than silently doing nothing.
    @Test
    void everyDemoStoryboardProgramsWithoutThrowingAndQueuesMoreThanOneInstruction() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        DefaultPonderSceneRegistrationHelper helper = new DefaultPonderSceneRegistrationHelper(DemoPonderPlugin.MODID, registry);
        new DemoPonderPlugin().registerScenes(helper);

        for (ResourceLocation component : List.of(
            ResourceLocation.withDefaultNamespace("oak_planks"),
            ResourceLocation.withDefaultNamespace("chest"),
            ResourceLocation.withDefaultNamespace("furnace"))) {
            for (StoryBoardEntry entry : registry.getScenes(component)) {
                PonderScene scene = mock(PonderScene.class);
                List<PonderInstruction> schedule = new ArrayList<>();
                when(scene.getSchedule()).thenReturn(schedule);
                when(scene.getBasePlateMinX()).thenReturn(0.0);
                when(scene.getBasePlateMaxX()).thenReturn(3.0);
                when(scene.getBasePlateMinZ()).thenReturn(0.0);
                when(scene.getBasePlateMaxZ()).thenReturn(3.0);
                PonderSceneBuilder builder = new PonderSceneBuilder(scene, DemoPonderPlugin.MODID);

                entry.getBoard().program(builder, new SimpleSceneBuildingUtil());

                assertTrue(schedule.size() > 1, "a real scene queues more than a single instruction: " + component);
            }
        }
    }
}
