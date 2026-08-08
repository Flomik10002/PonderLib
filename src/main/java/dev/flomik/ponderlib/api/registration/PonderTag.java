package dev.flomik.ponderlib.api.registration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A registered navigation category shown beside scenes and in the Ponder index. */
public interface PonderTag {
    ResourceLocation id();
    Component title();
    Component description();
    ItemStack icon();
    ItemStack mainItem();
    boolean isIndexed();
}
