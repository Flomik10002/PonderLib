package dev.flomik.ponderlib.client;

import dev.flomik.ponderlib.foundation.PonderIndex;
import dev.flomik.ponderlib.foundation.PonderTooltipHandler;
import dev.flomik.ponderlib.foundation.ui.PonderIndexScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Two triggers into a scene: hold the Ponder key while hovering an item's tooltip that has a
 * registered scene ({@code foundation.PonderTooltipHandler#tick}), or hold it while looking at a
 * placed block with one (see {@code PonderTooltipHandler#tickWorldLookup}'s javadoc for how that's
 * disambiguated from the tooltip-hover trigger). A third, dedicated keybind opens the full scene
 * index/browser ({@code foundation.ui.PonderIndexScreen}).
 */
@EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    private static final KeyMapping PONDER_KEY =
        new KeyMapping("key.ponderlib.ponder", GLFW.GLFW_KEY_P, "key.categories.ponderlib");
    private static final KeyMapping PONDER_INDEX_KEY =
        new KeyMapping("key.ponderlib.index", GLFW.GLFW_KEY_O, "key.categories.ponderlib");

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        PonderIndex.registerAll();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PONDER_KEY);
        event.register(PONDER_INDEX_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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
