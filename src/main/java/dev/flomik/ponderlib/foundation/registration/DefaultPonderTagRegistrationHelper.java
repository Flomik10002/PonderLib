package dev.flomik.ponderlib.foundation.registration;

import dev.flomik.ponderlib.api.registration.PonderTag;
import dev.flomik.ponderlib.api.registration.PonderTagBuilder;
import dev.flomik.ponderlib.api.registration.PonderTagRegistrationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class DefaultPonderTagRegistrationHelper implements PonderTagRegistrationHelper {
    private final String modId;
    private final PonderTagRegistry registry;

    public DefaultPonderTagRegistrationHelper(String modId, PonderTagRegistry registry) {
        this.modId = modId;
        this.registry = registry;
    }

    @Override public PonderTagBuilder registerTag(ResourceLocation id) { return new Builder(id); }
    @Override public void addTagToComponent(ResourceLocation component, ResourceLocation tag) { registry.addComponent(tag, component); }
    @Override public ResourceLocation asLocation(String path) { return ResourceLocation.fromNamespaceAndPath(modId, path); }

    private final class Builder implements PonderTagBuilder {
        private final ResourceLocation id;
        private Component title;
        private Component description = Component.empty();
        private ItemStackProvider icon = () -> ItemStack.EMPTY;
        private ItemStackProvider mainItem = () -> ItemStack.EMPTY;
        private boolean indexed;

        private Builder(ResourceLocation id) {
            this.id = id;
            this.title = Component.literal(id.getPath());
        }

        @Override public PonderTagBuilder title(Component value) { title = value; return this; }
        @Override public PonderTagBuilder title(String fallback) {
            title = Component.translatableWithFallback(id.getNamespace() + ".ponder.tag." + id.getPath() + ".title", fallback);
            return this;
        }
        @Override public PonderTagBuilder description(Component value) { description = value; return this; }
        @Override public PonderTagBuilder description(String fallback) {
            description = Component.translatableWithFallback(id.getNamespace() + ".ponder.tag." + id.getPath() + ".description", fallback);
            return this;
        }
        @Override public PonderTagBuilder icon(ItemStackProvider value) { icon = value; return this; }
        @Override public PonderTagBuilder mainItem(ItemStackProvider value) { mainItem = value; return this; }
        @Override public PonderTagBuilder addToIndex() { indexed = true; return this; }

        @Override public PonderTag register() {
            ItemStack iconStack = icon.get();
            ItemStack mainStack = mainItem.get();
            if (iconStack.isEmpty()) iconStack = mainStack;
            if (mainStack.isEmpty()) mainStack = iconStack;
            PonderTag tag = new RegisteredPonderTag(id, title, description, iconStack, mainStack, indexed);
            registry.register(tag);
            return tag;
        }
    }
}
