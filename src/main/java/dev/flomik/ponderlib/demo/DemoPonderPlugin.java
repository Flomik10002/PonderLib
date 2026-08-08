package dev.flomik.ponderlib.demo;

import dev.flomik.ponderlib.api.registration.PonderPlugin;
import dev.flomik.ponderlib.api.registration.PonderSceneRegistrationHelper;
import dev.flomik.ponderlib.api.registration.PonderTagRegistrationHelper;
import dev.flomik.ponderlib.api.scene.SceneBuilder;
import dev.flomik.ponderlib.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;

import java.util.Set;

/**
 * PonderLib registering scenes against its own {@link #MODID}, playing the role of its own first
 * "consumer" - exactly the same {@code PonderPlugin}/{@code helper.addStoryBoard} path a real
 * dependent mod would use. Registered only outside a production environment (see {@code
 * Ponderlib}'s own constructor) - these scenes exist to prove and demonstrate the API, not to ship
 * to players of whatever mod happens to depend on this library.
 */
public class DemoPonderPlugin implements PonderPlugin {

    public static final String MODID = "ponderlib";
    static final ResourceLocation BASICS = new ResourceLocation(MODID, "basics");
    static final ResourceLocation STORAGE = new ResourceLocation(MODID, "storage_test");
    static final ResourceLocation WORKSTATIONS = new ResourceLocation(MODID, "workstations_test");

    @Override
    public String getModId() {
        return MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper helper) {
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks"),
            "oak_planks/floor", DemoPonderPlugin::oakPlanksScene,
            Set.of(BASICS), Set.of(), Set.of());
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "chest"),
            "chest/floor", DemoPonderPlugin::chestScene,
            Set.of(BASICS, STORAGE, WORKSTATIONS), Set.of(), Set.of());
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
            "furnace/unlit", DemoPonderPlugin::furnaceScene,
            Set.of(BASICS, WORKSTATIONS), Set.of(), Set.of());
    }
    @Override public void registerTags(PonderTagRegistrationHelper helper) {
        helper.registerTag(BASICS).title("PonderLib basics")
            .description("Small scenes demonstrating the library's core features")
            .icon(Blocks.CHEST).addToIndex().register();
        helper.addToTag(BASICS, Blocks.OAK_PLANKS, Blocks.CHEST, Blocks.FURNACE);
        helper.registerTag(STORAGE).title("Storage and item transport")
            .description("A deliberately mixed test category: only the chest has a scene, while the other entries verify missing-scene cards and item tooltips")
            .icon(Blocks.BARREL).addToIndex().register();
        helper.addToTag(STORAGE, Blocks.CHEST, Blocks.BARREL, Blocks.HOPPER, Blocks.SHULKER_BOX);
        helper.registerTag(WORKSTATIONS).title("Furnaces and workstations")
            .description("Overlaps with the other categories so chest and furnace display several independent tag buttons in the scene sidebar")
            .icon(Blocks.BLAST_FURNACE).addToIndex().register();
        helper.addToTag(WORKSTATIONS, Blocks.CHEST, Blocks.FURNACE, Blocks.CRAFTING_TABLE,
            Blocks.BLAST_FURNACE, Blocks.SMOKER);
    }

    private static void oakPlanksScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("oak_planks_floor", "A minimal Ponder scene");
        scene.configureBasePlate(0, 0, 3);
        scene.showBasePlate();
        scene.idle(5);

        var blockPos = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().position(blockPos), Direction.UP);
        scene.idle(10);
        scene.addKeyframe("Showing a block");
        scene.overlay().showText(60, "This is the simplest possible Ponder scene")
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
        scene.effects().playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .6F, 1F);
        scene.effects().indicateSuccess(blockPos);
        scene.idle(45);

        scene.markAsFinished();
    }
}
