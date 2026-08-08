package dev.flomik.ponderlib.foundation.registration;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public record RegisteredPonderTag(ResourceLocation id, Component title, Component description, ItemStack icon,
                                  ItemStack mainItem, boolean isIndexed) implements PonderTag {
    public RegisteredPonderTag { icon = icon.copy(); mainItem = mainItem.copy(); }
}
