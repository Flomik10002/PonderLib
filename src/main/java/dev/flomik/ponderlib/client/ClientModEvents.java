package dev.flomik.ponderlib.client;

import dev.flomik.ponderlib.Ponderlib;
import dev.flomik.ponderlib.foundation.PonderIndex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * {@link ClientEvents}'s two lifecycle-only triggers, split into their own mod-bus subscriber:
 * {@code RegisterKeyMappingsEvent}/{@code FMLLoadCompleteEvent} both fire on the mod event bus,
 * while everything else in {@link ClientEvents} fires on the game (forge) event bus - one
 * {@code @Mod.EventBusSubscriber} class can only target one bus at a time.
 */
@Mod.EventBusSubscriber(modid = Ponderlib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        PonderIndex.registerAll();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientEvents.PONDER_KEY);
        event.register(ClientEvents.PONDER_INDEX_KEY);
    }
}
