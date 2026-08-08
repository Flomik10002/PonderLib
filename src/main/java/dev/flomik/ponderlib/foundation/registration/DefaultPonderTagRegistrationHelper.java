package dev.flomik.ponderlib.foundation.registration;
import dev.flomik.ponderlib.api.registration.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public final class DefaultPonderTagRegistrationHelper implements PonderTagRegistrationHelper {
    private final String modId; private final PonderTagRegistry registry;
    public DefaultPonderTagRegistrationHelper(String modId, PonderTagRegistry registry) { this.modId = modId; this.registry = registry; }
    @Override public PonderTagBuilder registerTag(ResourceLocation id) { return new Builder(id); }
    @Override public void addTagToComponent(ResourceLocation component, ResourceLocation tag) { registry.addComponent(tag, component); }
    @Override public ResourceLocation asLocation(String path) { return new ResourceLocation(modId, path); }
    private final class Builder implements PonderTagBuilder {
        private final ResourceLocation id; private Component title; private Component description = Component.empty();
        private ItemStackProvider icon = () -> ItemStack.EMPTY, mainItem = () -> ItemStack.EMPTY; private boolean indexed;
        private Builder(ResourceLocation id) { this.id = id; title = Component.literal(id.getPath()); }
        @Override public PonderTagBuilder title(Component v) { title=v; return this; }
        @Override public PonderTagBuilder title(String fallback) { title=Component.translatableWithFallback(id.getNamespace()+".ponder.tag."+id.getPath()+".title",fallback); return this; }
        @Override public PonderTagBuilder description(Component v) { description=v; return this; }
        @Override public PonderTagBuilder description(String fallback) { description=Component.translatableWithFallback(id.getNamespace()+".ponder.tag."+id.getPath()+".description",fallback); return this; }
        @Override public PonderTagBuilder icon(ItemStackProvider v) { icon=v; return this; }
        @Override public PonderTagBuilder mainItem(ItemStackProvider v) { mainItem=v; return this; }
        @Override public PonderTagBuilder addToIndex() { indexed=true; return this; }
        @Override public PonderTag register() { ItemStack i=icon.get(), m=mainItem.get(); if(i.isEmpty())i=m; if(m.isEmpty())m=i; PonderTag tag=new RegisteredPonderTag(id,title,description,i,m,indexed); registry.register(tag); return tag; }
    }
}
