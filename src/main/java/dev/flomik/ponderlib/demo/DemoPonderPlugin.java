package dev.flomik.ponderlib.demo;

import dev.flomik.ponderlib.api.registration.PonderPlugin;
import dev.flomik.ponderlib.api.registration.PonderSceneRegistrationHelper;
import dev.flomik.ponderlib.api.scene.SceneBuilder;
import dev.flomik.ponderlib.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * PonderLib registering scenes against its own {@link #MODID}, playing the role of its own first
 * "consumer" - exactly the same {@code PonderPlugin}/{@code helper.addStoryBoard} path a real
 * dependent mod would use. Registered only outside a production environment (see {@code
 * Ponderlib}'s own constructor) - these scenes exist to prove and demonstrate the API, not to ship
 * to players of whatever mod happens to depend on this library.
 */
public class DemoPonderPlugin implements PonderPlugin {

    public static final String MODID = "ponderlib";

    @Override
    public String getModId() {
        return MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper helper) {
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks"),
            "oak_planks/floor", DemoPonderPlugin::oakPlanksScene);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "chest"),
            "chest/floor", DemoPonderPlugin::chestScene);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
            "furnace/unlit", DemoPonderPlugin::furnaceScene);
    }

    private static void oakPlanksScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("oak_planks_floor", "A minimal Ponder scene");
        scene.configureBasePlate(0, 0, 3);
        scene.showBasePlate();
        scene.idle(5);

        var blockPos = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().position(blockPos), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(60, "This is the simplest possible Ponder scene")
            .attachKeyFrame()
            .pointAt(util.vector().topOf(blockPos))
            .placeNearTarget();
        scene.idle(65);

        scene.markAsFinished();
    }

    private static void chestScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("chest_floor", "A block entity in a scene");
        scene.configureBasePlate(0, 0, 3);
        scene.showBasePlate();
        scene.idle(5);

        var blockPos = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().position(blockPos), Direction.UP);
        scene.idle(10);
        scene.overlay().showOutlineWithText(util.select().position(blockPos), 70,
                "Blocks with a block entity (like this chest) tick and render exactly like they do in a real world")
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(75);

        scene.markAsFinished();
    }

    private static void furnaceScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("furnace_unlit", "Effects in a scene");
        scene.configureBasePlate(0, 0, 3);
        scene.showBasePlate();
        scene.idle(5);

        var blockPos = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().position(blockPos), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(60, "A scene can call out an event with effects(), not just text")
            .attachKeyFrame()
            .pointAt(util.vector().topOf(blockPos))
            .placeNearTarget();
        scene.idle(20);
        scene.effects().indicateSuccess(blockPos);
        scene.idle(45);

        scene.markAsFinished();
    }
}
