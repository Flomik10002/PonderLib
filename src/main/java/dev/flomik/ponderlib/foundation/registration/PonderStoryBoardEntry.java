package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record PonderStoryBoardEntry(ResourceLocation component, ResourceLocation schematicLocation,
                                     PonderStoryBoard board, Set<ResourceLocation> tags,
                                     Set<ResourceLocation> orderBefore, Set<ResourceLocation> orderAfter)
    implements StoryBoardEntry {

    public PonderStoryBoardEntry(ResourceLocation component, ResourceLocation schematicLocation, PonderStoryBoard board) {
        this(component, schematicLocation, board, Set.of(), Set.of(), Set.of());
    }

    @Override
    public PonderStoryBoard getBoard() {
        return board;
    }

    @Override
    public ResourceLocation getSchematicLocation() {
        return schematicLocation;
    }

    @Override
    public ResourceLocation getComponent() {
        return component;
    }

    @Override
    public Set<ResourceLocation> getTags() {
        return tags;
    }

    @Override
    public Set<ResourceLocation> getOrderBefore() {
        return orderBefore;
    }

    @Override
    public Set<ResourceLocation> getOrderAfter() {
        return orderAfter;
    }
}
