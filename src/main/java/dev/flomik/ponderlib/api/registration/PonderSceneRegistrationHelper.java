package dev.flomik.ponderlib.api.registration;

import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public interface PonderSceneRegistrationHelper {

    String getModId();

    /**
     * @param component      what this scene is "about" — an item/block id it can be looked up by
     * @param schematicPath  resolved to {@code assets/<modid>/ponder/<schematicPath>.nbt}
     * @param storyBoard     the scene's behavior
     * @param tags           see {@link StoryBoardEntry#getTags()}
     * @param orderBefore    see {@link StoryBoardEntry#getOrderBefore()} — identify the referenced
     *                       entries by their own {@code schematicPath} resolved the same way (this
     *                       or another mod's), not the raw path string
     * @param orderAfter     see {@link StoryBoardEntry#getOrderAfter()}
     */
    StoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath, PonderStoryBoard storyBoard,
                                   Set<ResourceLocation> tags, Set<ResourceLocation> orderBefore, Set<ResourceLocation> orderAfter);

    /**
     * @param component      what this scene is "about" — an item/block id it can be looked up by
     * @param schematicPath  resolved to {@code assets/<modid>/ponder/<schematicPath>.nbt}
     * @param storyBoard     the scene's behavior
     */
    default StoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath, PonderStoryBoard storyBoard) {
        return addStoryBoard(component, schematicPath, storyBoard, Set.of(), Set.of(), Set.of());
    }

    default ResourceLocation asLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(getModId(), path);
    }
}
