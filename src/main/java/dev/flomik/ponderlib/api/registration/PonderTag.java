package dev.flomik.ponderlib.api.registration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public interface PonderTag {
    ResourceLocation HIGHLIGHT_ALL = new ResourceLocation("ponderlib", "_all");

    ResourceLocation id(); Component title(); Component description(); ItemStack icon(); ItemStack mainItem(); boolean isIndexed();
}
