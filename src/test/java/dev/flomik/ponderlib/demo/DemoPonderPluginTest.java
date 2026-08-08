package dev.flomik.ponderlib.demo;

import dev.flomik.ponderlib.foundation.registration.DefaultPonderSceneRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPonderPluginTest {

    @Test
    void registersExactlyThreeScenesAgainstTheirOwnVanillaComponents() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        DefaultPonderSceneRegistrationHelper helper = new DefaultPonderSceneRegistrationHelper(DemoPonderPlugin.MODID, registry);

        new DemoPonderPlugin().registerScenes(helper);

        ResourceLocation oakPlanks = ResourceLocation.withDefaultNamespace("oak_planks");
        ResourceLocation chest = ResourceLocation.withDefaultNamespace("chest");
        ResourceLocation furnace = ResourceLocation.withDefaultNamespace("furnace");

        assertTrue(registry.doScenesExistForId(oakPlanks));
        assertTrue(registry.doScenesExistForId(chest));
        assertTrue(registry.doScenesExistForId(furnace));

        assertEquals(1, registry.getScenes(oakPlanks).size());
        assertEquals(1, registry.getScenes(chest).size());
        assertEquals(1, registry.getScenes(furnace).size());
    }

    @Test
    void schematicPathsMatchWhatPonderSchematicProviderActuallyGenerates() {
        PonderSceneRegistry registry = new PonderSceneRegistry();
        DefaultPonderSceneRegistrationHelper helper = new DefaultPonderSceneRegistrationHelper(DemoPonderPlugin.MODID, registry);

        new DemoPonderPlugin().registerScenes(helper);

        List<ResourceLocation> schematics = List.of(
            registry.getScenes(ResourceLocation.withDefaultNamespace("oak_planks")).get(0).getSchematicLocation(),
            registry.getScenes(ResourceLocation.withDefaultNamespace("chest")).get(0).getSchematicLocation(),
            registry.getScenes(ResourceLocation.withDefaultNamespace("furnace")).get(0).getSchematicLocation()
        );

        assertEquals(List.of(
            ResourceLocation.fromNamespaceAndPath("ponderlib", "ponder/oak_planks/floor.nbt"),
            ResourceLocation.fromNamespaceAndPath("ponderlib", "ponder/chest/floor.nbt"),
            ResourceLocation.fromNamespaceAndPath("ponderlib", "ponder/furnace/unlit.nbt")
        ), schematics);
    }
}
