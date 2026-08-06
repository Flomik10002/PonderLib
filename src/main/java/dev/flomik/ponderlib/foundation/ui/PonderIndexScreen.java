package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * A minimal scene browser - open every registered {@link StoryBoardEntry} and pick one to play,
 * instead of only being able to reach a scene through its item's tooltip. A single flat, scrollable
 * list built on vanilla's {@link ObjectSelectionList} - no icons, no search box, no pagination/grid
 * layout (a plain scrollable list has no need for it). Optionally filtered to one tag (see
 * {@link #PonderIndexScreen(ResourceLocation)}), reachable from {@link PonderTagIndexScreen}'s
 * per-tag entries.
 */
public class PonderIndexScreen extends Screen {

    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM_MARGIN = 32;
    private static final int ENTRY_HEIGHT = 20;

    @Nullable
    private final ResourceLocation tagFilter;

    public PonderIndexScreen() {
        this(null);
    }

    public PonderIndexScreen(@Nullable ResourceLocation tagFilter) {
        super(tagFilter == null ? Component.literal("Ponder Index")
            : Component.literal("Ponder Index — #" + tagFilter.getPath()));
        this.tagFilter = tagFilter;
    }

    @Override
    protected void init() {
        SceneList list = new SceneList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN, ENTRY_HEIGHT);
        for (StoryBoardEntry entry : PonderIndex.getScenes().getAllEntries()) {
            if (tagFilter == null || entry.getTags().contains(tagFilter)) {
                list.addSceneEntry(entry);
            }
        }
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.literal("Tags"),
                button -> minecraft.setScreen(new PonderTagIndexScreen()))
            .bounds(4, 4, 50, 20)
            .build());
        if (tagFilter != null) {
            addRenderableWidget(Button.builder(Component.literal("All scenes"),
                    button -> minecraft.setScreen(new PonderIndexScreen()))
                .bounds(width - 94, 4, 90, 20)
                .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class SceneList extends ObjectSelectionList<SceneList.SceneEntry> {

        private SceneList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
        }

        void addSceneEntry(StoryBoardEntry entry) {
            addEntry(new SceneEntry(entry));
        }

        private final class SceneEntry extends ObjectSelectionList.Entry<SceneEntry> {

            private final StoryBoardEntry storyBoardEntry;
            private final Component label;

            private SceneEntry(StoryBoardEntry storyBoardEntry) {
                this.storyBoardEntry = storyBoardEntry;
                this.label = itemLabel(storyBoardEntry);
            }

            private static Component itemLabel(StoryBoardEntry entry) {
                return BuiltInRegistries.ITEM.getOptional(entry.getComponent())
                    .map(item -> item.getDefaultInstance().getHoverName())
                    .orElseGet(() -> Component.literal(entry.getComponent().toString()));
            }

            @Override
            public Component getNarration() {
                return label;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                int mouseX, int mouseY, boolean hovering, float partialTick) {
                if (hovering) {
                    graphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
                }
                graphics.drawString(minecraft.font, label, left + 4, top + (height - 8) / 2, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                Minecraft.getInstance().setScreen(PonderUI.of(storyBoardEntry));
                return true;
            }
        }
    }
}
