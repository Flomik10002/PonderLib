package dev.flomik.ponderlib;

import com.mojang.logging.LogUtils;
import dev.flomik.ponderlib.datagen.PonderLangProvider;
import dev.flomik.ponderlib.datagen.PonderSchematicProvider;
import dev.flomik.ponderlib.datagen.PonderTestStructureProvider;
import dev.flomik.ponderlib.demo.DemoPonderPlugin;
import dev.flomik.ponderlib.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
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

        // Self-demonstration only - a mod depending on PonderLib should never end up with these
        // scenes in its own, shipped scene index.
        if (!FMLEnvironment.production) {
            PonderIndex.addPlugin(new DemoPonderPlugin());
        }
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderTestStructureProvider(event.getGenerator().getPackOutput()));
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderSchematicProvider(event.getGenerator().getPackOutput()));
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderLangProvider(event.getGenerator().getPackOutput()));
    }
}
