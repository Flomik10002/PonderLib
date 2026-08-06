package dev.flomik.ponderlib.client;

import dev.flomik.ponderlib.foundation.PonderTooltipHandler;
import dev.flomik.ponderlib.foundation.ui.PonderIndexScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Two triggers into a scene: hold the Ponder key while hovering an item's tooltip that has a
 * registered scene ({@code foundation.PonderTooltipHandler#tick}), or hold it while looking at a
 * placed block with one (see {@code PonderTooltipHandler#tickWorldLookup}'s javadoc for how that's
 * disambiguated from the tooltip-hover trigger). A third, dedicated keybind opens the full scene
 * index/browser ({@code foundation.ui.PonderIndexScreen}) - registered by {@link ClientModEvents},
 * since {@code RegisterKeyMappingsEvent} fires on the mod bus rather than this class's forge bus.
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    static final KeyMapping PONDER_KEY =
        new KeyMapping("key.ponderlib.ponder", GLFW.GLFW_KEY_P, "key.categories.ponderlib");
    static final KeyMapping PONDER_INDEX_KEY =
        new KeyMapping("key.ponderlib.index", GLFW.GLFW_KEY_O, "key.categories.ponderlib");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        PonderTooltipHandler.tick(PONDER_KEY);
        PonderTooltipHandler.tickWorldLookup(PONDER_KEY);

        Minecraft mc = Minecraft.getInstance();
        while (PONDER_INDEX_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PonderIndexScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        PonderTooltipHandler.addToTooltip(event.getToolTip(), event.getItemStack(), PONDER_KEY);
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        PonderTooltipHandler.tooltipBorderColor(event.getItemStack())
            .ifPresent(color -> {
                event.setBorderStart(color);
                event.setBorderEnd(color);
            });
    }
}
