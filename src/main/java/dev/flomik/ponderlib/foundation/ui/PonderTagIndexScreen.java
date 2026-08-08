package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.foundation.PonderIndex;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Every tag carried by any registered scene, on the same flat vanilla {@link ObjectSelectionList}
 * technique {@link PonderIndexScreen} already uses. Tags are grouped by mod namespace under a
 * "Categories of &lt;ModName&gt;" heading per group, via a plain, non-clickable header row inserted
 * whenever the namespace changes in {@code PonderSceneRegistry#getAllTags()}'s already-sorted
 * list, rather than building a separate grouping map for it. No mod-name lookup (the raw namespace
 * id is shown instead - no mod-name registry to query here) and no per-tag icon/title/description
 * (the tag model is a bare {@code Set<ResourceLocation>}, nothing to show beyond the id itself).
 */
public class PonderTagIndexScreen extends Screen {

    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM_MARGIN = 32;
    private static final int ENTRY_HEIGHT = 20;

    public PonderTagIndexScreen() {
        super(Component.literal("Ponder Tags"));
    }

    @Override
    protected void init() {
        TagList list = new TagList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN, ENTRY_HEIGHT);
        String currentNamespace = null;
        for (PonderTag tag : PonderIndex.getTags().getAll()) {
            if (!tag.isIndexed()) continue;
            if (!tag.id().getNamespace().equals(currentNamespace)) {
                currentNamespace = tag.id().getNamespace();
                list.addHeaderEntry(currentNamespace);
            }
            list.addTagEntry(tag);
        }
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.literal("All scenes"),
                button -> minecraft.setScreen(new PonderIndexScreen()))
            .bounds(4, 4, 90, 20)
            .build());
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

    private static final class TagList extends ObjectSelectionList<TagList.TagRow> {

        private TagList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
        }

        void addHeaderEntry(String namespace) {
            addEntry(new TagRow(namespace));
        }

        void addTagEntry(PonderTag tag) {
            addEntry(new TagRow(tag));
        }

        private final class TagRow extends ObjectSelectionList.Entry<TagRow> {

            private final PonderTag tag;
            private final Component label;

            private TagRow(String namespaceHeader) {
                this.tag = null;
                this.label = Component.literal(namespaceHeader).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
            }

            private TagRow(PonderTag tag) {
                this.tag = tag;
                this.label = tag.title();
            }

            @Override
            public Component getNarration() {
                return label;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                int mouseX, int mouseY, boolean hovering, float partialTick) {
                if (tag != null && hovering) {
                    graphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
                }
                graphics.drawString(minecraft.font, label, left + 4, top + (height - 8) / 2, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (tag == null) {
                    // Header row - not a real entry, just a section label; nothing to open.
                    return false;
                }
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.setScreen(new PonderTagScreen(tag, minecraft.screen));
                return true;
            }
        }
    }
}
