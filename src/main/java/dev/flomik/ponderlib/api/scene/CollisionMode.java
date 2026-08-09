package dev.flomik.ponderlib.api.scene;

/** Controls whether a scripted entity movement is allowed to pass through block collision. */
public enum CollisionMode {

    /**
     * Places the entity directly at every sampled point, temporarily disabling its physics and
     * gravity. Use this for authored paths that must enter or cross otherwise-solid geometry.
     */
    IGNORE,

    /**
     * Applies each desired displacement through vanilla's collision-aware
     * {@code Entity#move(MoverType, Vec3)} path. A block can therefore shorten or completely stop
     * the movement, so reaching the requested target is not guaranteed.
     */
    RESPECT
}
