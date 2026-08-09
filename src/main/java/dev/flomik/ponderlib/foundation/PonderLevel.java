package dev.flomik.ponderlib.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A minimal fake {@link Level} for a scene's own blocks/block entities/entities: delegate every
 * generic query (registry access, light engine, recipe manager, ...) to a real, currently-loaded
 * {@code Level} borrowed just for that, and only actually implement the handful of methods scenes
 * care about (block/block-entity/entity storage, flat shading/brightness, plains-biome tint).
 * <p>
 * {@link #entities} is this level's OWN list, entirely separate from {@link #getEntities()} (the
 * required {@link LevelEntityGetter} override below, which real gameplay code — collision, AI,
 * `getEntitiesOfClass` — would query, and which stays a no-op {@link DummyLevelEntityGetter}: no
 * scene has ever needed real entity queries against itself). Entities are added ({@link
 * #addFreshEntity}), ticked ({@link #tickEntities}) and rendered ({@link #renderEntities}) directly
 * by name from {@code PonderScene}/{@code PonderUI}, the same way {@link #blocks}/{@link
 * #blockEntities} already are.
 */
public class PonderLevel extends Level {

    private final Level real;
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final Map<BlockPos, CompoundTag> originalBlockEntityData = new HashMap<>();
    private final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private final LevelEntityGetter<Entity> entityGetter = new DummyLevelEntityGetter<>();
    @Nullable
    private PonderParticleSink particleSink;
    @Nullable
    private BiConsumer<BlockPos, BlockState> blockStateSink;

    public PonderLevel(Level real) {
        super((WritableLevelData) real.getLevelData(), real.dimension(), real.registryAccess(),
            real.dimensionTypeRegistration(), real::getProfiler, real.isClientSide, real.isDebug(), 0, 0);
        this.real = real;
    }

    /**
     * Seeds a block directly, bypassing normal update/neighbor-notification machinery — scenes
     * aren't a real, ticking world, there's nothing to notify.
     */
    public void setBlockDirect(BlockPos pos, BlockState state) {
        BlockPos immutable = pos.immutable();
        storeBlockState(blocks, blockEntities, immutable, state);
        if (!blockEntities.containsKey(immutable) && state.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity blockEntity = entityBlock.newBlockEntity(immutable, state);
            if (blockEntity != null) {
                setBlockEntityDirect(immutable, blockEntity);
            }
        }
        notifyBlockStateChanged(immutable, state);
    }

    /**
     * Keeps a virtual world's block map and an already-instantiated block entity in agreement.
     * Vanilla normally performs this synchronization as part of its chunk mutation path; this
     * stripped-down level owns no chunks, so its direct map write has to do it explicitly.
     */
    static void storeBlockState(Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> blockEntities,
                                BlockPos pos, BlockState state) {
        BlockPos immutable = pos.immutable();
        blocks.put(immutable, state);
        BlockEntity blockEntity = blockEntities.get(immutable);
        if (blockEntity != null) {
            if (blockEntity.getType().isValid(state)) {
                blockEntity.setBlockState(state);
            } else {
                blockEntity.setRemoved();
                blockEntities.remove(immutable);
            }
        }
    }

    /**
     * Registers a block entity for {@code pos} (already constructed/loaded by the caller — see
     * {@code PonderScene#compile}) and gives it this level, so the vanilla renderer/ticker
     * machinery (which checks {@code BlockEntity#hasLevel}) accepts it.
     */
    public void setBlockEntityDirect(BlockPos pos, BlockEntity blockEntity) {
        blockEntity.setLevel(this);
        BlockPos immutable = pos.immutable();
        BlockState state = blocks.get(immutable);
        if (state == null || !blockEntity.getType().isValid(state)) {
            blockEntity.setRemoved();
            return;
        }
        blockEntity.setBlockState(state);
        blockEntity.clearRemoved();
        BlockEntity previous = blockEntities.put(immutable, blockEntity);
        if (previous != null && previous != blockEntity) {
            previous.setRemoved();
        }
        notifyBlockStateChanged(immutable, state);
    }

    public void setBlockStateSink(@Nullable BiConsumer<BlockPos, BlockState> blockStateSink) {
        this.blockStateSink = blockStateSink;
    }

    private void notifyBlockStateChanged(BlockPos pos, BlockState state) {
        if (blockStateSink != null) {
            blockStateSink.accept(pos, state);
        }
    }

    /**
     * Snapshots every currently-registered block entity's saved data, so it can be restored later
     * (see {@link #resetWorld()}). Called once, right after a scene's schematic finishes
     * loading (see {@code PonderScene#compile}) - before any storyboard instruction has had a
     * chance to mutate a block entity's own state (e.g. a chest's {@code openCount}).
     */
    public void createBackup() {
        originalBlockEntityData.clear();
        blockEntities.forEach((pos, blockEntity) -> originalBlockEntityData.put(pos, blockEntity.saveWithFullMetadata(registryAccess())));
        originalBlocks.clear();
        originalBlocks.putAll(blocks);
    }

    /**
     * Resets every position in {@code positions} back to whatever {@link #createBackup} last
     * snapshotted there (air, for a position with no schematic-original entry) - the backing of
     * {@code WorldInstructions#restoreBlocks}. Block-state listeners propagate the restored state
     * and block entity into any section that is already visible.
     */
    public void restoreBlocks(Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) {
            BlockPos immutable = pos.immutable();
            BlockState originalState = originalBlocks.getOrDefault(immutable, Blocks.AIR.defaultBlockState());
            setBlockDirect(immutable, originalState);
            CompoundTag originalData = originalBlockEntityData.get(immutable);
            if (originalData != null) {
                BlockEntity fresh = BlockEntity.loadStatic(immutable, originalState, originalData, registryAccess());
                if (fresh != null) {
                    setBlockEntityDirect(immutable, fresh);
                }
            }
        }
    }

    /**
     * Restores both block states and block entities to the snapshot captured by
     * {@link #createBackup()}. Replacing only the block entities is insufficient: a replay after
     * {@code destroyBlock} or {@code modifyBlock} would otherwise start from the previous run's
     * mutated block map.
     */
    public void resetWorld() {
        blockEntities.values().forEach(BlockEntity::setRemoved);
        blockEntities.clear();
        restoreBlockMap(blocks, originalBlocks);
        for (Map.Entry<BlockPos, CompoundTag> entry : originalBlockEntityData.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = blocks.get(pos);
            if (state == null) {
                continue;
            }
            BlockEntity fresh = BlockEntity.loadStatic(pos, state, entry.getValue(), registryAccess());
            if (fresh != null) {
                setBlockEntityDirect(pos, fresh);
            }
        }
    }

    static void restoreBlockMap(Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockState> originalBlocks) {
        blocks.clear();
        blocks.putAll(originalBlocks);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        setBlockEntityDirect(blockEntity.getBlockPos(), blockEntity);
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        BlockEntity removed = blockEntities.remove(immutable);
        if (removed != null) {
            removed.setRemoved();
            notifyBlockStateChanged(immutable, getBlockState(immutable));
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        setBlockDirect(pos, newState);
        return true;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public ChunkSource getChunkSource() {
        return real.getChunkSource();
    }

    /**
     * Collision queries must read the scene's virtual block map, not the unrelated real-world
     * chunk source delegated by {@link #getChunkSource()}. This mirrors upstream PonderLevel and is
     * especially important for collision-aware scripted entity movement.
     */
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return real.getLightEngine();
    }

    @Override
    public int getMaxLocalRawBrightness(BlockPos pos) {
        return 15;
    }

    /**
     * Flat full brightness for both light layers, regardless of position. Without this override,
     * {@code BlockAndTintGetter}'s default implementation would fall through to {@link
     * #getLightEngine()}, which delegates to the REAL level's light engine - querying real-world
     * light at whatever arbitrary small coordinates a scene entity happens to sit at, which has no
     * relationship to the scene at all. This is what makes {@link #renderEntities}' {@code
     * getPackedLightCoords} call resolve to a sane, scene-appropriate value instead of that.
     */
    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        // Same reasoning as VirtualBlockView.getShade: flat, unshaded, real light comes from
        // RenderSystem.setupLevelDiffuseLighting at render time instead.
        return 1.0F;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return plainsBiome();
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        // No real biome data for a fake scene - tint (grass/leaves/water color) resolves as if
        // every block were standing in a plains biome, same as the real SchematicLevel.
        return plainsBiome();
    }

    private Holder<Biome> plainsBiome() {
        return real.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    }

    @Override
    public RegistryAccess registryAccess() {
        return real.registryAccess();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return real.potionBrewing();
    }

    @Override
    public RecipeManager getRecipeManager() {
        return real.getRecipeManager();
    }

    @Override
    public Scoreboard getScoreboard() {
        return real.getScoreboard();
    }

    @Override
    public TickRateManager tickRateManager() {
        return real.tickRateManager();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return real.enabledFeatures();
    }

    @Override
    public String gatherChunkSourceStats() {
        return real.gatherChunkSourceStats();
    }

    // Everything below is either irrelevant to a scene (real persistence, sound, maps) or a
    // required override with no meaningful behavior here - stubbed to no-ops.

    @Override
    public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public void playSeededSound(@Nullable Player player, double x, double y, double z, Holder<SoundEvent> sound,
                                 SoundSource source, float volume, float pitch, long seed) {
    }

    @Override
    public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> sound, SoundSource source,
                                 float volume, float pitch, long seed) {
    }

    @Override
    public void playSound(@Nullable Player player, double x, double y, double z, SoundEvent sound,
                           SoundSource source, float volume, float pitch) {
    }

    /**
     * Installs where particles spawned inside this scene go — see {@link PonderParticleSink} for why
     * this is an interface rather than the concrete client-side class. Called once by
     * {@link PonderScene}'s constructor; a level with no sink installed (e.g. one built directly by a
     * GameTest) silently drops particles, exactly like the vanilla {@link Level#addParticle} no-op
     * stub this overrides.
     */
    public void setParticleSink(@Nullable PonderParticleSink particleSink) {
        this.particleSink = particleSink;
    }

    // Vanilla's Level#addParticle is an empty stub that only ClientLevel actually implements (it
    // routes into Minecraft's own ParticleEngine, whose particles all live in the REAL world and
    // would be positioned/rendered there, not in a scene). Routing it into the scene's own pool
    // instead is what makes any ordinary `level.addParticle(...)` inside a scene work - including a
    // block's own Block#animateTick (a lit furnace's smoke/flame, see
    // element.WorldSectionElementImpl#tick), which no storyboard has to know about.
    @Override
    public void addParticle(ParticleOptions options, double x, double y, double z, double mx, double my, double mz) {
        if (particleSink != null) {
            particleSink.addParticle(options, x, y, z, mx, my, mz);
        }
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions options, double x, double y, double z, double mx, double my, double mz) {
        addParticle(options, x, y, z, mx, my, mz);
    }

    @Override
    public void playSound(@Nullable Player player, Entity entity, SoundEvent sound, SoundSource source,
                           float volume, float pitch) {
    }

    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Override
    @Nullable
    public MapItemSavedData getMapData(MapId id) {
        return null;
    }

    @Override
    public void setMapData(MapId id, MapItemSavedData data) {
    }

    @Override
    public MapId getFreeMapId() {
        return real.getFreeMapId();
    }

    /**
     * Starts tracking an entity a storyboard creates (see {@code WorldInstructions#createEntity}).
     * That entity can come from any mod, including its item stack (an item frame or armor stand's
     * displayed item), so {@link #withUnsafeComponentsDiscarded} first strips everything off that
     * stack except the handful of components meaningful to just look at ({@code
     * ENCHANTMENTS}/{@code POTION_CONTENTS}/{@code DAMAGE}/{@code CUSTOM_NAME}) - an arbitrary
     * mod's item can otherwise carry a component that assumes it's attached to a real block
     * entity/world, which a scene is not. Past that, tracking is all this needs to do - {@link
     * #tickEntities}/{@link #renderEntities} pick the entity up from here every frame, the same way
     * {@link #blocks} is picked up by block rendering.
     */
    @Override
    public boolean addFreshEntity(Entity entity) {
        if (entity instanceof ItemFrame itemFrame) {
            itemFrame.setItem(withUnsafeComponentsDiscarded(itemFrame.getItem()));
        }
        if (entity instanceof ArmorStand armorStand) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                armorStand.setItemSlot(slot, withUnsafeComponentsDiscarded(armorStand.getItemBySlot(slot)));
            }
        }
        return entities.add(entity);
    }

    /**
     * Strips every data component off {@code stack} except the small allowlist checked in {@link
     * #isUnsafeItemComponent} - a no-op if the stack carries no components at all.
     */
    private static ItemStack withUnsafeComponentsDiscarded(ItemStack stack) {
        if (stack.getComponentsPatch().isEmpty()) {
            return stack;
        }
        ItemStack copy = stack.copy();
        stack.getComponents().stream()
            .filter(PonderLevel::isUnsafeItemComponent)
            .map(TypedDataComponent::type)
            .forEach(copy::remove);
        return copy;
    }

    private static boolean isUnsafeItemComponent(TypedDataComponent<?> component) {
        return isUnsafeItemComponent(component.type());
    }

    private static boolean isUnsafeItemComponent(DataComponentType<?> component) {
        if (component.equals(DataComponents.ENCHANTMENTS)) {
            return false;
        }
        if (component.equals(DataComponents.POTION_CONTENTS)) {
            return false;
        }
        if (component.equals(DataComponents.DAMAGE)) {
            return false;
        }
        return !component.equals(DataComponents.CUSTOM_NAME);
    }

    /**
     * This scene's own live entities - used directly by {@code PonderScene#forEachWorldEntity} and
     * {@code foundation.ui.PonderUI}'s render loop. Named distinctly from {@link #getEntities()}
     * below, an unrelated required override that stays a no-op stub.
     */
    public List<Entity> getSceneEntities() {
        return entities;
    }

    /**
     * Drops every entity a storyboard has created so far - called from {@code PonderScene#begin()}
     * on every (re)start/replay, the same moment {@link #resetWorld()} runs.
     * Schematics carry no entities of their own, only blocks/block entities, so a scene's entity
     * list is always empty right after loading - a plain clear is enough to get back there.
     */
    public void clearEntities() {
        entities.clear();
    }

    /**
     * One tick of every entity in this scene: standard per-tick bookkeeping (old-position tracking,
     * {@code entity.tick()}), discarding anything that fell below {@code y = -0.5} (out of the
     * scene's own small bounds, with nothing below it to land on) and removing anything no longer
     * alive. This scene's particles are ticked separately (see {@code PonderScene#tick}), owned by
     * {@code PonderScene} rather than by this level. Called from {@code PonderScene#tick()} once per
     * scene tick.
     */
    public void tickEntities() {
        for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext(); ) {
            Entity entity = iterator.next();

            entity.tickCount++;
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
            entity.tick();

            if (entity.getY() <= -0.5) {
                entity.discard();
            }
            if (!entity.isAlive()) {
                iterator.remove();
            }
        }
    }

    /**
     * Renders every entity in this scene through the real {@link EntityRenderDispatcher} - the same
     * one vanilla uses for every entity in a real world, so a scene entity looks and animates
     * exactly like it would there, including its light: {@code dispatcher.getRenderer(entity)
     * .getPackedLightCoords(entity, pt)} resolves through {@link #getBrightness} above, rather than
     * a hardcoded light value at the call site here.
     * <p>
     * Vanilla entity rendering is normally camera-relative (an entity's world position minus the
     * camera's own position). This scene's camera never moves away from the origin, so that
     * subtraction would always be a no-op - this uses the entity's own interpolated position
     * directly instead, the same number without the detour.
     * <p>
     * Called from {@code foundation.ui.PonderUI#renderScene}, inside the same {@code
     * applySceneTransform}'d pose stack as the scene's blocks - {@code poseStack}/{@code buffer} are
     * exactly what that method is already holding, so an entity ends up positioned in the scene the
     * same way a block does, with no separate transform of its own.
     */
    public void renderEntities(PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        for (Entity entity : entities) {
            if (entity.tickCount == 0) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            }
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            int light = dispatcher.getRenderer(entity).getPackedLightCoords(entity, partialTick);
            dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, buffer, light);
        }
    }

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
    }

    @Override
    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {
    }

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, Vec3 pos) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) {
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return entityGetter;
    }

    // Copied rather than delegated to the real level: other mods can override vanilla's
    // LevelHeightAccessor defaults in ways that would make naive delegation return the REAL
    // level's height bounds instead of this fake level's own.
    @Override
    public int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    @Override
    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    @Override
    public int getMinSection() {
        return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
    }

    @Override
    public int getMaxSection() {
        return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight();
    }

    @Override
    public int getSectionIndex(int y) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(y));
    }

    @Override
    public int getSectionIndexFromSectionY(int sectionY) {
        return sectionY - this.getMinSection();
    }

    @Override
    public int getSectionYFromSectionIndex(int sectionIndex) {
        return sectionIndex + this.getMinSection();
    }

    @Override
    public void setDayTimeFraction(float fraction) {
    }

    @Override
    public float getDayTimeFraction() {
        return 0;
    }

    @Override
    public float getDayTimePerTick() {
        return 0;
    }

    @Override
    public void setDayTimePerTick(float dayTimePerTick) {
    }

    private static final class DummyLevelEntityGetter<T extends EntityAccess> implements LevelEntityGetter<T> {

        @Override
        public T get(int id) {
            return null;
        }

        @Override
        public T get(UUID id) {
            return null;
        }

        @Override
        public Iterable<T> getAll() {
            return Collections.emptyList();
        }

        @Override
        public <U extends T> void get(EntityTypeTest<T, U> test, AbortableIterationConsumer<U> consumer) {
        }

        @Override
        public void get(AABB area, Consumer<T> consumer) {
        }

        @Override
        public <U extends T> void get(EntityTypeTest<T, U> test, AABB area, AbortableIterationConsumer<U> consumer) {
        }
    }
}
