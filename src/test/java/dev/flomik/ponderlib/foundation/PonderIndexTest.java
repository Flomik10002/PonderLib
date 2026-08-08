package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.registration.PonderPlugin;
import dev.flomik.ponderlib.api.registration.PonderSceneRegistrationHelper;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// PonderIndex.registerAll() is a JVM-wide, run-exactly-once static gate with no reset hook, so
// this is deliberately the only test touching it, kept to a single method to stay self-contained
// regardless of what other test classes happen to run in the same JVM.
class PonderIndexTest {

    @Test
    void registersEachPluginExactlyOnceEvenAcrossRepeatedRegisterAllCalls() {
        AtomicInteger registrations = new AtomicInteger();
        ResourceLocation component = ResourceLocation.fromNamespaceAndPath("ponderlib_test", "probe");
        PonderPlugin plugin = new PonderPlugin() {
            @Override
            public String getModId() {
                return "ponderlib_test";
            }

            @Override
            public void registerScenes(PonderSceneRegistrationHelper helper) {
                registrations.incrementAndGet();
                helper.addStoryBoard(component, "probe", (scene, util) -> {
                }, Set.of(PonderTag.HIGHLIGHT_ALL), Set.of(), Set.of());
            }
        };

        PonderIndex.addPlugin(plugin);
        PonderIndex.registerAll();

        assertEquals(1, registrations.get());
        assertTrue(PonderIndex.getScenes().doScenesExistForId(component));
        assertNull(PonderIndex.getTags().get(PonderTag.HIGHLIGHT_ALL));
        assertTrue(PonderIndex.getTags().getComponents(PonderTag.HIGHLIGHT_ALL).isEmpty());
        assertTrue(PonderIndex.getTags().getTags(component).isEmpty());

        PonderIndex.registerAll();

        assertEquals(1, registrations.get());
    }
}
