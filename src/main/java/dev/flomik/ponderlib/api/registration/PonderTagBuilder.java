package dev.flomik.ponderlib.api.registration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
public interface PonderTagBuilder {
    PonderTagBuilder title(Component title);
    default PonderTagBuilder title(String title) { return title(Component.literal(title)); }
    PonderTagBuilder description(Component description);
    default PonderTagBuilder description(String description) { return description(Component.literal(description)); }
    PonderTagBuilder icon(ItemStackProvider icon);
    default PonderTagBuilder icon(ItemLike icon) { return icon(() -> icon.asItem().getDefaultInstance()); }
    PonderTagBuilder mainItem(ItemStackProvider mainItem);
    default PonderTagBuilder mainItem(ItemLike item) { return mainItem(() -> item.asItem().getDefaultInstance()); }
    PonderTagBuilder addToIndex();
    PonderTag register();
    @FunctionalInterface interface ItemStackProvider { net.minecraft.world.item.ItemStack get(); }
}
