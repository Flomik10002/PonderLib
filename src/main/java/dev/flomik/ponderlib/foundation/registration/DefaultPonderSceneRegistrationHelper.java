package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.PonderSceneRegistrationHelper;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class DefaultPonderSceneRegistrationHelper implements PonderSceneRegistrationHelper {

    private final String modId;
    private final PonderSceneRegistry registry;

    public DefaultPonderSceneRegistrationHelper(String modId, PonderSceneRegistry registry) {
        this.modId = modId;
        this.registry = registry;
    }

    @Override
    public String getModId() {
        return modId;
    }

    @Override
    public StoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath, PonderStoryBoard storyBoard,
                                          Set<ResourceLocation> tags, Set<ResourceLocation> orderBefore, Set<ResourceLocation> orderAfter) {
        ResourceLocation schematicLocation = ResourceLocation.fromNamespaceAndPath(modId, "ponder/" + schematicPath + ".nbt");
        PonderStoryBoardEntry entry = new PonderStoryBoardEntry(component, schematicLocation, storyBoard, tags, orderBefore, orderAfter);
        registry.register(entry);
        return entry;
    }
}
