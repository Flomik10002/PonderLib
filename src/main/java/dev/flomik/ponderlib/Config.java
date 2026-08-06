package dev.flomik.ponderlib;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_TOOLTIP_HINT = BUILDER
        .comment("Whether to show the \"Hold [key] to Ponder\" hint in item tooltips for items that have a registered scene.")
        .define("showTooltipHint", true);

    // Kept in config rather than being per-screen state: it's a reading-pace preference, so it
    // should survive closing the scene. The slow-mode button toggles this value directly.
    public static final ForgeConfigSpec.BooleanValue COMFY_READING = BUILDER
        .comment("Slow down a ponder scene whenever there is text on screen, to give more time to read it.")
        .define("comfyReading", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
