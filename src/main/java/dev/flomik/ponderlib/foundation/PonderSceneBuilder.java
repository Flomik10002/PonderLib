package dev.flomik.ponderlib.foundation;

import dev.flomik.ponderlib.api.element.ElementLink;
import dev.flomik.ponderlib.api.element.EntityElement;
import dev.flomik.ponderlib.api.element.WorldSectionElement;
import dev.flomik.ponderlib.api.scene.EffectInstructions;
import dev.flomik.ponderlib.api.PonderPalette;
import dev.flomik.ponderlib.api.Pointing;
import dev.flomik.ponderlib.api.scene.InputElementBuilder;
import dev.flomik.ponderlib.api.scene.OverlayInstructions;
import dev.flomik.ponderlib.api.scene.SceneBuilder;
import dev.flomik.ponderlib.api.scene.Selection;
import dev.flomik.ponderlib.api.scene.TextElementBuilder;
import dev.flomik.ponderlib.api.scene.WorldInstructions;
import dev.flomik.ponderlib.foundation.element.EntityElementImpl;
import dev.flomik.ponderlib.foundation.element.InputWindowElement;
import dev.flomik.ponderlib.foundation.element.OutlineElement;
import dev.flomik.ponderlib.foundation.element.TextWindowElement;
import dev.flomik.ponderlib.foundation.element.WorldSectionElementImpl;
import dev.flomik.ponderlib.foundation.instruction.AnimateElementInstruction;
import dev.flomik.ponderlib.foundation.instruction.DelayInstruction;
import dev.flomik.ponderlib.foundation.instruction.KeyframeInstruction;
import dev.flomik.ponderlib.foundation.instruction.InputWindowInstruction;
import dev.flomik.ponderlib.foundation.instruction.MarkAsFinishedInstruction;
import dev.flomik.ponderlib.foundation.instruction.OutlineInstruction;
import dev.flomik.ponderlib.foundation.instruction.PonderInstruction;
import dev.flomik.ponderlib.foundation.instruction.RevealSectionInstruction;
import dev.flomik.ponderlib.foundation.instruction.TextInstruction;
import dev.flomik.ponderlib.foundation.registration.PonderLocalization;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Non-final on purpose — see {@link SceneBuilder}'s javadoc. A mod can subclass this to layer its
 * own instruction vocabulary (its own {@code WorldInstructions}/{@code OverlayInstructions}
 * implementations calling back into the protected {@link #scene}) on top of this engine.
 */
public class PonderSceneBuilder implements SceneBuilder {

    /**
     * Ticks a revealed section takes to fade in (see {@link RevealSectionInstruction}) - about half
     * a second at 20 ticks/sec. {@code showSection} has no duration parameter in its public
     * signature, so this is a fixed default rather than something a storyboard can tune per call.
     */
    private static final int SECTION_FADE_TICKS = 20;

    protected final PonderScene scene;
    private final WorldInstructions worldInstructions = new WorldInstructionsImpl();
    private final OverlayInstructions overlayInstructions = new OverlayInstructionsImpl();
    private final EffectInstructions effectInstructions = new EffectInstructionsImpl();

    /**
     * {@code null} for scenes with no known registering mod (e.g. {@link PonderScene#compile(PonderStoryBoard)}'s
     * raw, unregistered path) - title/text then stay plain {@link Component#literal} with no
     * translation key, since there's no {@code <modid>.ponder....} namespace to put one under.
     */
    private final String modId;
    private String sceneId = "";
    private int textIndex;

    public PonderSceneBuilder(PonderScene scene) {
        this(scene, null);
    }

    public PonderSceneBuilder(PonderScene scene, String modId) {
        this.scene = scene;
        this.modId = modId;
    }

    /**
     * Builds a real, live-translatable component when this scene has a known mod namespace (real
     * play, via {@link PonderScene#compile(StoryBoardEntry)}), falling back to a fixed literal
     * otherwise - {@code translatableWithFallback} always displays correctly even before/without a
     * generated lang file, so this never needs the raw {@code fallback} text to also be baked in
     * separately (see {@code datagen.PonderLangProvider} for where the lang file itself comes from).
     */
    private Component component(String key, String fallback) {
        return key == null ? Component.literal(fallback) : Component.translatableWithFallback(key, fallback);
    }

    private String titleKey(String sceneId) {
        return modId == null ? null : PonderLocalization.keyForTitle(modId, sceneId);
    }

    private String textKey(String sceneId, int index) {
        return modId == null ? null : PonderLocalization.keyForText(modId, sceneId, index);
    }

    @Override
    public WorldInstructions world() {
        return worldInstructions;
    }

    @Override
    public OverlayInstructions overlay() {
        return overlayInstructions;
    }

    @Override
    public EffectInstructions effects() {
        return effectInstructions;
    }

    @Override
    public PonderScene getScene() {
        return scene;
    }

    @Override
    public void title(String sceneId, String title) {
        this.sceneId = sceneId;
        scene.setTitle(component(titleKey(sceneId), title));
    }

    @Override
    public void configureBasePlate(int xOffset, int zOffset, int basePlateSize) {
        scene.configureBasePlateBounds(xOffset, xOffset + basePlateSize, zOffset, zOffset + basePlateSize);
    }

    @Override
    public void showBasePlate() {
        createSection(basePlateSelection(), Direction.UP, true);
    }

    /**
     * Shared by {@link WorldInstructionsImpl#showSection} and {@link #showBasePlate}, which needs
     * to mark its own section as the base plate (see {@link WorldSectionElementImpl#setBasePlate})
     * before it's ever added to the scene - something the public {@link WorldInstructions#showSection}
     * signature has no way to express.
     */
    private ElementLink<WorldSectionElement> createSection(Selection selection, Direction direction, boolean basePlate) {
        WorldSectionElementImpl element = new WorldSectionElementImpl(selection);
        element.setBasePlate(basePlate);
        ElementLink<WorldSectionElement> link = new SimpleElementLink<>(WorldSectionElement.class);

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (BlockPos pos : selection) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX() + 1);
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }
        addInstruction(new RevealSectionInstruction(element, link, direction, SECTION_FADE_TICKS,
            selection.getCenter(), minX, maxX, minZ, maxZ));
        return link;
    }

    // Package-private (not private) so PonderSceneBuilderTest can verify the cuboid math directly,
    // the same reasoning as PonderScene#getSchedule()'s visibility.
    Selection basePlateSelection() {
        int minX = (int) scene.getBasePlateMinX();
        int maxX = (int) scene.getBasePlateMaxX();
        int minZ = (int) scene.getBasePlateMinZ();
        int maxZ = (int) scene.getBasePlateMaxZ();

        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                positions.add(new BlockPos(x, 0, z));
            }
        }
        return new SimpleSelection(positions);
    }

    @Override
    public void addKeyframe() {
        addInstruction(KeyframeInstruction.IMMEDIATE);
    }

    @Override
    public void addLazyKeyframe() {
        addInstruction(KeyframeInstruction.DELAYED);
    }

    @Override
    public void addInstruction(PonderInstruction instruction) {
        scene.getSchedule().add(instruction);
    }

    @Override
    public void addInstruction(Consumer<PonderScene> callback) {
        addInstruction(PonderInstruction.simple(callback));
    }

    @Override
    public void idle(int ticks) {
        addInstruction(new DelayInstruction(ticks));
    }

    @Override
    public void markAsFinished() {
        addInstruction(new MarkAsFinishedInstruction());
    }

    protected class WorldInstructionsImpl implements WorldInstructions {

        @Override
        public void setBlock(BlockPos pos, BlockState state) {
            BlockPos immutable = pos.immutable();
            addInstruction(s -> s.setBlockState(immutable, state));
        }

        @Override
        public ElementLink<WorldSectionElement> showSection(Selection selection, Direction direction) {
            return createSection(selection, direction, false);
        }

        @Override
        public void rotateSection(ElementLink<WorldSectionElement> link, Vec3 eulerDegrees, int duration) {
            addInstruction(new AnimateElementInstruction<>(link, eulerDegrees, duration,
                WorldSectionElement::setAnimatedRotation, WorldSectionElement::getAnimatedRotation));
        }

        @Override
        public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
            addInstruction(new AnimateElementInstruction<>(link, offset, duration,
                WorldSectionElement::setAnimatedOffset, WorldSectionElement::getAnimatedOffset));
        }

        @Override
        public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
            ElementLink<EntityElement> link = new SimpleElementLink<>(EntityElement.class);
            // A one-shot side effect, same shape as setBlock above - no dedicated Instruction class
            // needed (unlike showSection, which has to run every tick to animate a fade-in).
            addInstruction(s -> {
                Entity entity = factory.apply(s.getLevel());
                EntityElementImpl element = new EntityElementImpl(entity);
                s.addElement(element);
                s.linkElement(element, link);
                s.getLevel().addFreshEntity(entity);
            });
            return link;
        }

        @Override
        public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
            return createEntity(level -> {
                ItemEntity itemEntity = new ItemEntity(level, location.x, location.y, location.z, stack);
                itemEntity.setDeltaMovement(motion);
                return itemEntity;
            });
        }

        @Override
        public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallback) {
            addInstruction(s -> {
                EntityElement element = s.resolve(link);
                if (element != null) {
                    element.ifPresent(entityCallback);
                }
            });
        }

        @Override
        public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallback) {
            addInstruction(s -> s.forEachWorldEntity(entityClass, entityCallback));
        }

        @Override
        public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area, Consumer<T> entityCallback) {
            Set<BlockPos> positions = new HashSet<>();
            area.forEach(positions::add);
            addInstruction(s -> s.forEachWorldEntity(entityClass, entity -> {
                if (positions.contains(entity.blockPosition())) {
                    entityCallback.accept(entity);
                }
            }));
        }
    }

    protected class OverlayInstructionsImpl implements OverlayInstructions {

        @Override
        public TextElementBuilder showText(int duration, String text) {
            TextWindowElement element = new TextWindowElement();
            element.setText(component(textKey(sceneId, textIndex++), text));
            addInstruction(new TextInstruction(element, duration));
            // The element is configured through the returned builder AFTER the instruction is queued,
            // which is fine: nothing reads these fields until the instruction first ticks, long
            // after the storyboard has finished programming.
            return new TextElementBuilderImpl(element);
        }

        @Override
        public void showOutline(PonderPalette palette, Selection selection, int duration) {
            OutlineElement element = new OutlineElement(selection);
            element.setPalette(palette);
            addInstruction(new OutlineInstruction(element, duration));
        }

        @Override
        public TextElementBuilder showOutlineWithText(Selection selection, int duration, String text) {
            OutlineElement outline = new OutlineElement(selection);
            addInstruction(new OutlineInstruction(outline, duration));
            // The text defaults to pointing at the selection's own centre, so the common case needs
            // no pointAt() call - and the builder recolours the outline along with the text, since
            // both come from the one PonderPalette passed to .colored(...).
            TextWindowElement window = new TextWindowElement();
            window.setText(component(textKey(sceneId, textIndex++), text));
            window.setPointAt(selection.getCenter());
            addInstruction(new TextInstruction(window, duration));
            return new TextElementBuilderImpl(window, outline);
        }

        @Override
        public InputElementBuilder showControls(Vec3 sceneSpace, Pointing direction, int duration) {
            InputWindowElement element = new InputWindowElement(sceneSpace, direction);
            addInstruction(new InputWindowInstruction(element, duration));
            return new InputElementBuilderImpl(element);
        }
    }

    private static class InputElementBuilderImpl implements InputElementBuilder {

        private final InputWindowElement element;

        private InputElementBuilderImpl(InputWindowElement element) {
            this.element = element;
        }

        @Override
        public InputElementBuilder withItem(ItemStack stack) {
            element.setItem(stack);
            return this;
        }

        @Override
        public InputElementBuilder leftClick() {
            element.leftClick();
            return this;
        }

        @Override
        public InputElementBuilder rightClick() {
            element.rightClick();
            return this;
        }

        @Override
        public InputElementBuilder scroll() {
            element.scroll();
            return this;
        }

        @Override
        public InputElementBuilder whileSneaking() {
            element.setQualifier("Sneak");
            return this;
        }

        @Override
        public InputElementBuilder whileCTRL() {
            element.setQualifier("Ctrl");
            return this;
        }
    }

    private class TextElementBuilderImpl implements TextElementBuilder {

        private final TextWindowElement element;
        // Non-null only for showOutlineWithText, where colouring the text must recolour the outline too.
        private final OutlineElement linkedOutline;

        private TextElementBuilderImpl(TextWindowElement element) {
            this(element, null);
        }

        private TextElementBuilderImpl(TextWindowElement element, OutlineElement linkedOutline) {
            this.element = element;
            this.linkedOutline = linkedOutline;
        }

        @Override
        public TextElementBuilder pointAt(Vec3 scenePos) {
            element.setPointAt(scenePos);
            return this;
        }

        @Override
        public TextElementBuilder placeNearTarget() {
            element.setNearScene(true);
            return this;
        }

        @Override
        public TextElementBuilder independent(int y) {
            element.setIndependentY(y);
            return this;
        }

        @Override
        public TextElementBuilder colored(PonderPalette palette) {
            element.setPalette(palette);
            if (linkedOutline != null) {
                linkedOutline.setPalette(palette);
            }
            return this;
        }

        @Override
        public TextElementBuilder attachKeyFrame() {
            addLazyKeyframe();
            return this;
        }
    }

    protected class EffectInstructionsImpl implements EffectInstructions {

        private final RandomSource random = RandomSource.create();

        // Goes straight through the scene's level - PonderLevel routes it into the scene's particle
        // pool, see PonderLevel#addParticle. Uses DustParticleOptions (ParticleTypes.DUST, what
        // redstone dust uses) since it's vanilla's one built-in particle type taking an arbitrary
        // RGB colour, matching this method's colour-int signature.
        @Override
        public void emitSparks(Vec3 position, int color, int count) {
            addInstruction(s -> {
                Vector3f rgb = new Vector3f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
                DustParticleOptions options = new DustParticleOptions(rgb, 1F);
                for (int i = 0; i < count; i++) {
                    double mx = (random.nextFloat() - 0.5F) * 0.1;
                    double my = random.nextFloat() * 0.1;
                    double mz = (random.nextFloat() - 0.5F) * 0.1;
                    s.getLevel().addParticle(options, position.x, position.y, position.z, mx, my, mz);
                }
            });
        }
    }
}
