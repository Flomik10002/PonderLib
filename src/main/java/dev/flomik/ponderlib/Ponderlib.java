package dev.flomik.ponderlib;

import com.mojang.logging.LogUtils;
import dev.flomik.ponderlib.datagen.PonderLangProvider;
import dev.flomik.ponderlib.datagen.PonderTestStructureProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Ponderlib.MODID)
public class Ponderlib {

    public static final String MODID = "ponderlib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ponderlib() {
        LOGGER.info("PonderLib initializing");
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::gatherData);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderTestStructureProvider(event.getGenerator().getPackOutput()));
        event.getGenerator()
            .addProvider(event.includeClient(), new PonderLangProvider(event.getGenerator().getPackOutput()));
    }
}
