package dev.flomik.ponderlib.foundation.registration;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class PonderTagRegistry {
    private final Map<ResourceLocation, PonderTag> tags = new LinkedHashMap<>();
    private final Map<ResourceLocation, LinkedHashSet<ResourceLocation>> componentsByTag = new LinkedHashMap<>();
    private final Map<ResourceLocation, LinkedHashSet<ResourceLocation>> tagsByComponent = new LinkedHashMap<>();
    public void register(PonderTag tag) { if (tags.putIfAbsent(tag.id(), tag) != null) throw new IllegalArgumentException("Duplicate Ponder tag " + tag.id()); }
    public void registerLegacyIfAbsent(ResourceLocation id) { tags.putIfAbsent(id, new RegisteredPonderTag(id, Component.literal(id.getPath()), Component.empty(), ItemStack.EMPTY, ItemStack.EMPTY, true)); }
    public void addComponent(ResourceLocation tag, ResourceLocation component) {
        componentsByTag.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(component);
        tagsByComponent.computeIfAbsent(component, ignored -> new LinkedHashSet<>()).add(tag);
    }
    public PonderTag get(ResourceLocation id) { return tags.get(id); }
    public Collection<PonderTag> getAll() { return List.copyOf(tags.values()); }
    public List<ResourceLocation> getComponents(ResourceLocation tag) { return new ArrayList<>(componentsByTag.getOrDefault(tag, new LinkedHashSet<>())); }
    public List<PonderTag> getTags(ResourceLocation component) {
        List<PonderTag> result = new ArrayList<>();
        for (ResourceLocation id : tagsByComponent.getOrDefault(component, new LinkedHashSet<>())) { PonderTag tag = tags.get(id); if (tag != null) result.add(tag); }
        return result;
    }
}
