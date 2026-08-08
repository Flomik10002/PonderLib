package dev.flomik.ponderlib.api.registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
public interface PonderTagRegistrationHelper {
    PonderTagBuilder registerTag(ResourceLocation id);
    default PonderTagBuilder registerTag(String path) { return registerTag(asLocation(path)); }
    void addTagToComponent(ResourceLocation component, ResourceLocation tag);
    default void addTagToComponent(ItemLike component, ResourceLocation tag) {
        addTagToComponent(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(component.asItem()), tag);
    }
    default void addToTag(ResourceLocation tag, ResourceLocation... components) { for (ResourceLocation c : components) addTagToComponent(c, tag); }
    default void addToTag(ResourceLocation tag, ItemLike... components) { for (ItemLike c : components) addTagToComponent(c, tag); }
    ResourceLocation asLocation(String path);
}
