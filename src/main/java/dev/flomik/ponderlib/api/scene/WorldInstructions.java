package dev.flomik.ponderlib.api.scene;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface WorldInstructions {

    /**
     * Places a block in the scene's virtual world, for {@link #showSection} to later pick up.
     */
    void setBlock(BlockPos pos, BlockState state);

    /**
     * {@link #setBlock(BlockPos, BlockState)}, optionally spawning the same break-style particle
     * burst {@link #destroyBlock} uses — for a placement that should visually call attention to
     * itself once a later {@link #showSection} reveals it.
     */
    void setBlock(BlockPos pos, BlockState state, boolean spawnParticles);

    /**
     * {@link #setBlock(BlockPos, BlockState, boolean)} over every position in {@code selection} at
     * once.
     */
    void setBlocks(Selection selection, BlockState state, boolean spawnParticles);

    /**
     * Like {@link #setBlocks}, but skips any position whose currently displayed block is air —
     * "replace what's already there" rather than "set every position regardless".
     */
    void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles);

    /**
     * Removes a block (sets it to air), spawning a break-particle burst first — the visible,
     * one-shot counterpart to quietly clearing a position with {@code setBlock(pos,
     * Blocks.AIR.defaultBlockState())}.
     */
    void destroyBlock(BlockPos pos);

    /**
     * Resets every position in {@code selection} back to whatever the schematic originally had there
     * (or air, for a scene with no schematic) — undoes {@link #setBlock}/{@link #destroyBlock}. Only
     * affects the scene's virtual world; a section that already captured the overridden state via
     * {@link #showSection} keeps showing it until revealed again.
     */
    void restoreBlocks(Selection selection);

    /**
     * Applies {@code stateFunc} to whatever block is currently displayed at {@code pos} — reading
     * through an already-revealed section's own captured state if there is one, falling back to the
     * scene's virtual world otherwise — and pushes the result back to both, so the change is visible
     * immediately if the position is already shown, and future {@link #showSection} calls keep
     * seeing it too. Optionally spawns a break-particle burst, same as {@link #destroyBlock}.
     */
    void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles);

    /**
     * {@link #modifyBlock} over every position in {@code selection}.
     */
    void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles);

    /**
     * Advances {@code property} to its next possible value (vanilla {@code BlockState#cycle}), e.g.
     * a furnace's {@code LIT} flipping or a threshold switch's level bumping up one notch. A no-op
     * if the displayed block doesn't have {@code property} at all.
     */
    void cycleBlockProperty(BlockPos pos, Property<?> property);

    /**
     * Flips whichever redstone-signal property the blocks in {@code selection} actually have —
     * {@code POWERED} for a block that's simply on/off (an observer, a non-persistent button), or
     * between 0 and 15 for a block using an analog {@code POWER} level (redstone dust). A no-op on a
     * position with neither.
     */
    void toggleRedstonePower(Selection selection);

    /**
     * Makes a selection of previously placed blocks visible and returns a link so later
     * instructions (see {@link #rotateSection}/{@link #moveSection}) can animate it. Fades in over
     * a fixed duration, sliding in half a block from {@code direction}'s normal as it does.
     */
    ElementLink<WorldSectionElement> showSection(Selection selection, Direction direction);

    /**
     * {@link #showSection} under the name real Create's own API uses for the version that returns a
     * link — every section PonderLib shows already comes back with its own {@link ElementLink}, so
     * this is the exact same call under a second name, for a storyboard ported from real Create
     * where the plain, linkless {@code showSection} and this one are different things.
     */
    ElementLink<WorldSectionElement> showIndependentSection(Selection selection, Direction direction);

    /**
     * {@link #showIndependentSection}, fully visible from the moment it's scheduled instead of
     * fading in — for content that should just already be there with no reveal beat of its own.
     */
    ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection);

    /**
     * Folds newly captured blocks into an already-shown section (resolved via {@code link}) instead
     * of creating a new element — the positions render as part of the same section from then on,
     * including sharing its current animated rotation/offset. The merge itself is instant (no fade
     * of its own beyond the fade the target section is already at).
     */
    void showSectionAndMerge(Selection selection, Direction fadeInDirection, ElementLink<WorldSectionElement> link);

    /**
     * Pulls {@code selection}'s positions out of whichever already-shown section(s) currently
     * contain them, into a brand-new section of their own, and returns a link to it — the way to
     * detach one part of an already-revealed section so it can be moved/rotated/hidden
     * independently of the rest (see {@link #moveSection}/{@link #hideIndependentSection}). The
     * result starts fully visible, no fade of its own.
     */
    ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection);

    /**
     * Fades out and hides whichever currently-shown section's positions exactly match {@code
     * selection} — the reverse of {@link #showSection}. A no-op if no visible section's positions
     * match.
     */
    void hideSection(Selection selection, Direction fadeOutDirection);

    /**
     * {@link #hideSection}, by {@link ElementLink} instead of matching positions — always resolves
     * to exactly the section that link points at.
     */
    void hideIndependentSection(ElementLink<WorldSectionElement> link, Direction fadeOutDirection);

    /**
     * Rotates {@code link}'s section by {@code eulerDegrees} (relative to its current rotation),
     * linearly over {@code duration} ticks, around the center of its selection (or {@link
     * #configureCenterOfRotation}'s override, if set).
     */
    void rotateSection(ElementLink<WorldSectionElement> link, Vec3 eulerDegrees, int duration);

    /**
     * Overrides the point {@code link}'s section rotates around (see {@link #rotateSection}) — by
     * default that's the selection's own center; this pins it to an arbitrary scene-space point
     * instead, for a section that should swing around a hinge/axle rather than spin in place.
     */
    void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor);

    /**
     * Accepted for parity with real Create's own {@code WorldInstructions} — this library has no
     * parent/child section hierarchy (contraption-style movement is out of scope, see {@code
     * docs/PLAN.md}), so there is nothing here for a stabilization anchor to counteract yet; the
     * value is stored but has no observable effect on its own.
     */
    void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor);

    /**
     * Moves {@code link}'s section by {@code offset} (relative to its current offset), linearly
     * over {@code duration} ticks.
     */
    void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration);

    /**
     * Advances a per-position mining-progress counter (0..9, vanilla's own crack-stage range) by
     * one stage per call, wrapping back to 0 once it would pass the last stage, and spawns a small
     * particle cue each time — the visual language of a player actively mining a block. Doesn't
     * render vanilla's own crack-texture overlay (that needs hooking a Forge/vanilla rendering
     * internal this library doesn't otherwise touch); the particle cue is the observable effect.
     */
    void incrementBlockBreakingProgress(BlockPos pos);

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

    /**
     * The registry lookups needed to serialize/deserialize a {@link BlockEntity} or {@link
     * ItemStack} to NBT (see {@code ItemStack#saveOptional}) — needed to build the {@link
     * CompoundTag} payload {@link #modifyBlockEntityNBT} hands back.
     */
    HolderLookup.Provider getHolderLookupProvider();

    /**
     * Runs {@code consumer} against a full NBT snapshot of every block entity in {@code selection}
     * that's an instance of {@code beType}, then writes the (possibly mutated) tag back to it — the
     * way to reach a field with no dedicated setter (e.g. a Deployer's held item), the same
     * round-trip a real block entity's own save/load does. Equivalent to calling the 4-argument
     * overload with {@code reDrawBlocks = true}.
     */
    void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType, Consumer<CompoundTag> consumer);

    /**
     * {@link #modifyBlockEntityNBT(Selection, Class, Consumer)}, restricted to one position instead
     * of a {@link Selection}, handing back the live block entity object instead of an NBT
     * round-trip — for a field that already has a normal Java setter.
     */
    <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType, Consumer<T> consumer);

    /**
     * {@link #modifyBlockEntityNBT(Selection, Class, Consumer)}, with {@code reDrawBlocks} accepted
     * for parity with real Create's own signature. This library always renders a block entity from
     * its live object every frame (see {@code foundation.element.WorldSectionElementImpl#render}),
     * so there is no separate re-bake step to skip — the flag has no observable effect either way.
     */
    void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType, Consumer<CompoundTag> consumer,
                               boolean reDrawBlocks);
}
