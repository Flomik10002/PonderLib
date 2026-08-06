package dev.flomik.ponderlib;

import com.mojang.logging.LogUtils;
import dev.flomik.ponderlib.datagen.PonderLangProvider;
import dev.flomik.ponderlib.datagen.PonderTestStructureProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

@Mod(Ponderlib.MODID)
public class Ponderlib {

    public static final String MODID = "ponderlib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ponderlib(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("PonderLib initializing");
        modEventBus.addListener(this::gatherData);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderTestStructureProvider(event.getGenerator().getPackOutput()));
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderLangProvider(event.getGenerator().getPackOutput()));
    }
}
