package dev.flomik.ponderlib.demo;

import dev.flomik.ponderlib.foundation.registration.DefaultPonderSceneRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderSceneRegistry;
import dev.flomik.ponderlib.foundation.registration.DefaultPonderTagRegistrationHelper;
import dev.flomik.ponderlib.foundation.registration.PonderTagRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        assertEquals(Set.of(DemoPonderPlugin.BASICS), registry.getScenes(oakPlanks).get(0).getTags());
        assertEquals(Set.of(DemoPonderPlugin.BASICS, DemoPonderPlugin.STORAGE, DemoPonderPlugin.WORKSTATIONS),
            registry.getScenes(chest).get(0).getTags());
        assertEquals(Set.of(DemoPonderPlugin.BASICS, DemoPonderPlugin.WORKSTATIONS),
            registry.getScenes(furnace).get(0).getTags());
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

    @Test void registersOverlappingTagButtonTestCategories() {
        PonderTagRegistry registry=new PonderTagRegistry();
        new DemoPonderPlugin().registerTags(new DefaultPonderTagRegistrationHelper(DemoPonderPlugin.MODID,registry));
        ResourceLocation chest=BuiltInRegistries.ITEM.getKey(Blocks.CHEST.asItem());
        ResourceLocation furnace=BuiltInRegistries.ITEM.getKey(Blocks.FURNACE.asItem());
        assertEquals(List.of(DemoPonderPlugin.BASICS,DemoPonderPlugin.STORAGE,DemoPonderPlugin.WORKSTATIONS),registry.getTags(chest).stream().map(tag->tag.id()).toList());
        assertEquals(List.of(DemoPonderPlugin.BASICS,DemoPonderPlugin.WORKSTATIONS),registry.getTags(furnace).stream().map(tag->tag.id()).toList());
        assertEquals(4,registry.getComponents(DemoPonderPlugin.STORAGE).size());
        assertEquals(5,registry.getComponents(DemoPonderPlugin.WORKSTATIONS).size());
    }
}
