package dev.flomik.ponderlib.datagen;

import dev.flomik.ponderlib.Ponderlib;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Generates {@code assets/ponderlib/lang/en_us.json} — this mod's own keybinding strings. Run via
 * {@code ./gradlew runData}. There's deliberately no hand-authored copy under
 * {@code src/main/resources}: the two would otherwise silently race for the same output path once
 * this one's generated copy is merged in via the {@code sourceSets.main.resources} srcDir in
 * build.gradle.
 */
public class PonderLangProvider extends LanguageProvider {

    public PonderLangProvider(PackOutput output) {
        super(output, Ponderlib.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("key.categories.ponderlib", "PonderLib");
        add("key.ponderlib.ponder", "Ponder");
        add("key.ponderlib.index", "Open Ponder Index");
    }
}
