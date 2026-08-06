package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Function;

public interface WorldInstructions {

    /**
     * Places a block in the scene's virtual world, for {@link #showSection} to later pick up.
     */
    void setBlock(BlockPos pos, BlockState state);

    /**
     * Makes a selection of previously placed blocks visible and returns a link so later
     * instructions (see {@link #rotateSection}/{@link #moveSection}) can animate it. Fades in over
     * a fixed duration, sliding in half a block from {@code direction}'s normal as it does.
     */
    ElementLink<WorldSectionElement> showSection(Selection selection, Direction direction);

    /**
     * Rotates {@code link}'s section by {@code eulerDegrees} (relative to its current rotation),
     * linearly over {@code duration} ticks, around the center of its selection.
     */
    void rotateSection(ElementLink<WorldSectionElement> link, Vec3 eulerDegrees, int duration);

    /**
     * Moves {@code link}'s section by {@code offset} (relative to its current offset), linearly
     * over {@code duration} ticks.
     */
    void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration);

    /**
     * Spawns an entity built by {@code factory} (given this scene's own {@link Level}, matching
     * whatever constructor the entity type needs) and starts tracking it, returning a link so a
     * later instruction can look it up again via {@link #modifyEntity}. Ticked/rendered from the
     * moment it's created — no fade-in, unlike a section revealed with {@link #showSection}.
     */
    ElementLink<EntityElement> createEntity(Function<Level, Entity> factory);

    /**
     * {@link #createEntity} for the common case of a dropped item.
     *
     * @param location where the item entity starts, in scene (block) space
     * @param motion   its initial velocity
     * @param stack    the item it displays
     */
    ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack);

    /**
     * Runs {@code entityCallback} against the entity {@code link} points to, if it's still there —
     * see {@link EntityElement#ifPresent}.
     */
    void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallback);

    /**
     * Runs {@code entityCallback} against every entity of type {@code entityClass} currently in this
     * scene, no matter how it got there (created here, or already present some other way).
     */
    <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallback);

    /**
     * {@link #modifyEntities}, restricted to entities whose block position is inside {@code area}.
     */
    <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area, Consumer<T> entityCallback);
}
