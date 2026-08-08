package dev.flomik.ponderlib.api.validation;

import dev.flomik.ponderlib.api.registration.PonderTag;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.foundation.PonderIndex;
import dev.flomik.ponderlib.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static dev.flomik.ponderlib.api.validation.PonderDiagnostic.Severity.ERROR;
import static dev.flomik.ponderlib.api.validation.PonderDiagnostic.Severity.WARNING;

/** Read-only diagnostics for registrations, compilation and exact timeline duration. */
public final class PonderDoctor {
    private static final int MAX_RUNTIME_TICKS = 20 * 60 * 30;

    private PonderDoctor() {}

    public static List<PonderDiagnostic> validateAll() {
        List<PonderDiagnostic> result = new ArrayList<>();
        for (PonderTag tag : PonderIndex.getTags().getAll()) validateTag(tag, result);
        for (StoryBoardEntry entry : PonderIndex.getScenes().getAllEntries()) validateScene(entry, result);
        return List.copyOf(result);
    }

    private static void validateTag(PonderTag tag, List<PonderDiagnostic> out) {
        if (tag.title().getString().isBlank()) out.add(diagnostic(ERROR, "tag.empty_title", tag.id(), "Tag title is empty"));
        if (tag.description().getString().isBlank()) out.add(diagnostic(WARNING, "tag.empty_description", tag.id(), "Tag description is empty"));
        if (tag.icon().isEmpty()) out.add(diagnostic(WARNING, "tag.empty_icon", tag.id(), "Tag icon is empty"));
        List<ResourceLocation> components = PonderIndex.getTags().getComponents(tag.id());
        if (components.isEmpty()) out.add(diagnostic(WARNING, "tag.empty", tag.id(), "Tag has no components"));
        for (ResourceLocation component : components) {
            if (!PonderIndex.getScenes().doScenesExistForId(component)) {
                out.add(diagnostic(WARNING, "tag.component_without_scenes", component,
                    "Component belongs to " + tag.id() + " but has no Ponder scenes"));
            }
        }
    }

    private static void validateScene(StoryBoardEntry entry, List<PonderDiagnostic> out) {
        ResourceLocation id = entry.getSchematicLocation();
        if (Minecraft.getInstance().getResourceManager().getResource(id).isEmpty()) {
            out.add(diagnostic(ERROR, "scene.missing_schematic", id, "Schematic resource does not exist"));
            return;
        }
        try {
            PonderScene scene = PonderScene.compile(entry);
            if (scene.getTitle().getString().isBlank()) {
                out.add(diagnostic(WARNING, "scene.empty_title", id, "Scene title is empty"));
            }
            int declared = scene.getTotalTime();
            int measured = scene.measureRuntimeTicks(MAX_RUNTIME_TICKS);
            if (measured < 0) {
                out.add(diagnostic(ERROR, "scene.never_finishes", id,
                    "Scene did not finish within " + MAX_RUNTIME_TICKS + " ticks"));
            } else if (measured != declared) {
                out.add(diagnostic(ERROR, "scene.bad_duration", id,
                    "Timeline reports " + declared + " ticks but playback needs " + measured));
            }
            for (int i = 0; i < scene.getKeyframeCount(); i++) {
                int time = scene.getKeyframeTime(i);
                if (time < 0 || time > declared) {
                    out.add(diagnostic(ERROR, "scene.keyframe_outside_timeline", id,
                        "Keyframe " + i + " is at " + time + " but duration is " + declared));
                }
            }
        } catch (RuntimeException exception) {
            out.add(diagnostic(ERROR, "scene.compile_failed", id,
                exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }
    }

    private static PonderDiagnostic diagnostic(PonderDiagnostic.Severity severity, String code,
                                                ResourceLocation subject, String message) {
        return new PonderDiagnostic(severity, code, subject, message);
    }
}
