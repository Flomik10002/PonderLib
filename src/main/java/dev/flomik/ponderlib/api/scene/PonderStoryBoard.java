package dev.flomik.ponderlib.api.scene;

/**
 * A scene's behavior: a plain method reference that only enqueues instructions onto the scene's
 * schedule via {@code scene}. Nothing executes until playback tick time.
 */
@FunctionalInterface
public interface PonderStoryBoard {

    void program(SceneBuilder scene, SceneBuildingUtil util);
}
