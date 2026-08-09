package dev.flomik.ponderlib.foundation.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.flomik.ponderlib.api.PonderColorScheme;
import dev.flomik.ponderlib.api.registration.PonderTag;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import dev.flomik.ponderlib.foundation.PonderIndex;
import dev.flomik.ponderlib.render.PonderBoxElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Create-inspired tag-detail screen, kept separate from the scene sidebar. Its category header is
 * intentionally omitted; the associated-entry grid still mirrors Catnip's centered horizontal
 * layout, and all colours come from PonderLib's owning {@link PonderColorScheme}.
 */
public final class PonderTagScreen extends Screen {

    static final int CELL_WIDTH = 28;
    static final int CELL_HEIGHT = 28;
    static final int CELL_SPACING = 8;
    static final int MAX_ITEMS_PER_ROW = 11;
    static final int MAX_ROWS = 3;
    static final int MAIN_ITEM_OFFSET_X = 48;
    static final double MAIN_Y_MULTIPLIER = .15;
    static final int ITEMS_Y_OFFSET = 85;

    private static final double ITEM_X_MULTIPLIER = .5;
    private static final double DESCRIPTION_WIDTH_MULTIPLIER = .45;
    private static final int BACKTRACK_X = 31;
    private static final int BACKTRACK_BOTTOM = 31;
    private static final float BACK_ECHO_CHASE = .075F;

    private static final Component ASSOCIATED = Component.translatable("ponderlib.ui.associated");
    private static final Component THINK_BACK = Component.translatable("ponderlib.ui.think_back");

    private final PonderTag tag;
    private final Screen previous;
    private final PonderColorScheme colors;
    private final List<ItemEntry> items = new ArrayList<>();
    private final List<PonderButton> itemButtons = new ArrayList<>();

    private LayoutMetrics itemLayout = layoutFor(0);
    private PonderButton backTrack;
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private float backEchoPrevious;
    private float backEchoValue;
    private int uiTicks;

    public PonderTagScreen(PonderTag tag, Screen previous) {
        super(tag.title());
        this.tag = tag;
        this.previous = previous;
        this.colors = PonderIndex.colorsFor(tag.id().getNamespace());
    }

    @Override
    protected void init() {
        super.init();
        items.clear();
        itemButtons.clear();
        hoveredItem = ItemStack.EMPTY;

        initBackTrack();

        ResourceLocation mainItemId = itemId(tag.mainItem());
        for (ResourceLocation id : PonderIndex.getTags().getComponents(tag.id())) {
            ItemStack stack = BuiltInRegistries.ITEM.getOptional(id)
                .map(ItemStack::new)
                .orElseGet(() -> BuiltInRegistries.BLOCK.getOptional(id).map(ItemStack::new).orElse(null));
            if (stack != null && !id.equals(mainItemId)) {
                items.add(new ItemEntry(id, stack));
            }
        }

        itemLayout = layoutFor(items.size());
        int centerX = (int) (width * ITEM_X_MULTIPLIER);
        int centerY = itemsY(height);
        int index = 0;
        int rowY = itemLayout.areaY();

        for (int row = 0; row < itemLayout.rows(); row++) {
            int rowWidth = rowWidth(itemLayout.rowCounts()[row]);
            int rowX = -(rowWidth / 2);
            for (int column = 0; column < itemLayout.rowCounts()[row]; column++) {
                ItemEntry entry = items.get(index++);
                addItemButton(entry.id(), entry.stack(), centerX + rowX + 4, centerY + rowY + 4);
                rowX += CELL_WIDTH + CELL_SPACING;
            }
            rowY += CELL_HEIGHT + CELL_SPACING;
        }

        if (!tag.mainItem().isEmpty() && mainItemId != null) {
            addItemButton(mainItemId, tag.mainItem(),
                centerX - itemLayout.totalWidth() / 2 - MAIN_ITEM_OFFSET_X, centerY - 10);
        }
    }

    private void initBackTrack() {
        int y = height - BACKTRACK_BOTTOM - PonderButton.SIZE;
        ItemStack previousSubject = previous instanceof PonderUI ponderUI ? ponderUI.getSubject() : ItemStack.EMPTY;
        if (!previousSubject.isEmpty()) {
            backTrack = new PonderButton(BACKTRACK_X, y, previousSubject, THINK_BACK, this::onClose, () -> colors);
        } else {
            backTrack = new PonderButton(BACKTRACK_X, y, PonderButton.Icon.RETURN,
                THINK_BACK, this::onClose, () -> colors).withCreateLayout();
        }
        backTrack.withEntranceFade(0, 5, uiTicks);
        addRenderableWidget(backTrack);
    }

    private void addItemButton(ResourceLocation id, ItemStack stack, int x, int y) {
        Component label = stack.isEmpty() ? Component.literal(id.toString()) : stack.getHoverName();
        Collection<StoryBoardEntry> scenes = PonderIndex.getScenes().getScenes(id);
        Runnable callback = scenes.isEmpty() ? () -> { } : () -> openPonder(scenes);

        PonderButton button = new PonderButton(x, y, stack, label, callback, () -> colors);
        if (scenes.isEmpty()) {
            // Missing entries remain hoverable and show their full ItemStack tooltip. Only the
            // border stops animating; its actual colours stay PonderLib's own.
            button.withBorderColors(colors.frameBorderTop(), colors.frameBorderBottom());
        }
        itemButtons.add(addRenderableWidget(button));
    }

    private void openPonder(Collection<StoryBoardEntry> scenes) {
        StoryBoardEntry selected = scenes.iterator().next();
        for (StoryBoardEntry candidate : scenes) {
            if (candidate.getTags().contains(tag.id())) {
                selected = candidate;
                break;
            }
        }
        minecraft.setScreen(PonderUI.of(selected).withPreviousScreen(this));
    }

    private static ResourceLocation itemId(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        uiTicks++;
        backEchoPrevious = backEchoValue;
        backEchoValue = Math.max(0F, backEchoValue - BACK_ECHO_CHASE);
        for (var child : children()) {
            if (child instanceof PonderButton button) {
                button.tick();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderWindow(graphics, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderItemTooltip(graphics, mouseX, mouseY);
    }

    private void renderWindow(GuiGraphics graphics, float partialTick) {
        renderBackTrackEcho(graphics, partialTick);
        renderBackTrackLabel(graphics);
        renderAssociatedEntries(graphics);
        renderDescription(graphics);
    }

    private void renderBackTrackEcho(GuiGraphics graphics, float partialTick) {
        if (backTrack == null) {
            return;
        }

        float animated = Mth.lerp(partialTick, backEchoPrevious, backEchoValue);
        int y = height - 51;
        PonderStreak.backEcho(graphics, backTrack.getX(), y, PonderStreak.BACK_ECHO_Z, PonderButton.SIZE,
            1F - animated, colors.frameBorderTop(), colors.frameBorderBottom());
    }

    private void renderAssociatedEntries(GuiGraphics graphics) {
        if (items.isEmpty()) {
            return;
        }

        int centerX = (int) (width * ITEM_X_MULTIPLIER);
        int centerY = itemsY(height);
        int titleWidth = font.width(ASSOCIATED);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);

        new PonderBoxElement()
            .withBackground(colors.buttonBackground())
            .gradientBorder(colors.frameBorderTop(), colors.frameBorderBottom())
            .at(-titleWidth / 2F - 5, itemLayout.areaY() - 21, 100)
            .withBounds(titleWidth + 10, 10)
            .render(graphics);

        poseStack.pushPose();
        poseStack.translate(0, 0, 200);
        graphics.drawCenteredString(font, ASSOCIATED, 0, itemLayout.areaY() - 20, colors.buttonIconLit());
        poseStack.popPose();

        int streakLength = itemLayout.totalWidth() / 2 + 75;
        int streakBreadth = itemLayout.totalHeight() + 10;
        PonderStreak.render(graphics, 0, 0, 0, streakBreadth, streakLength, colors.buttonBackground());
        PonderStreak.render(graphics, 180, 0, 0, streakBreadth, streakLength, colors.buttonBackground());
        poseStack.popPose();
    }

    private void renderDescription(GuiGraphics graphics) {
        int descriptionWidth = descriptionWidth(width);
        int x = (width - descriptionWidth) / 2;
        int y = descriptionY(height, itemLayout.totalHeight());
        String description = tag.description().getString();
        int descriptionHeight = font.wordWrapHeight(description, descriptionWidth);

        new PonderBoxElement()
            .withBackground(colors.buttonBackground())
            .gradientBorder(colors.frameBorderTop(), colors.frameBorderBottom())
            .at(x - 3, y - 3, 90)
            .withBounds(descriptionWidth + 6, descriptionHeight + 6)
            .render(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        drawSplitString(graphics, font, description, x, y, descriptionWidth, colors.buttonIconLit());
        graphics.pose().popPose();
    }

    private void renderBackTrackLabel(GuiGraphics graphics) {
        if (backTrack == null || (!backTrack.isHovered() && !backTrack.isFocused())) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        graphics.drawString(font, THINK_BACK, 41 - font.width(THINK_BACK) / 2,
            height - 16, colors.buttonIconDim(), false);
        graphics.pose().popPose();
        if (Mth.equal(backEchoValue, 0F)) {
            // Jump both samples so the next echo pass starts without an interpolation seam.
            backEchoPrevious = 1F;
            backEchoValue = 1F;
        }
    }

    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        hoveredItem = ItemStack.EMPTY;
        for (PonderButton button : itemButtons) {
            if (button.isMouseOver(mouseX, mouseY) && button.getItem() != null) {
                hoveredItem = button.getItem();
            }
        }
        if (hoveredItem.isEmpty()) {
            return;
        }

        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.renderTooltip(font, hoveredItem, mouseX, mouseY);
        graphics.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    private static void drawSplitString(GuiGraphics graphics, Font font, String text,
                                        int x, int y, int width, int color) {
        for (String line : cutString(font, text, width)) {
            int lineX = font.isBidirectional()
                ? x + width - font.width(font.bidirectionalShaping(line))
                : x;
            graphics.drawString(font, line, lineX, y, color, false);
            y += 9;
        }
    }

    private static List<String> cutString(Font font, String text, int maxWidthPerLine) {
        List<String> words = new LinkedList<>();
        BreakIterator iterator = BreakIterator.getLineInstance(
            Minecraft.getInstance().getLanguageManager().getJavaLocale());
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            words.add(text.substring(start, end));
        }

        List<String> lines = new LinkedList<>();
        StringBuilder currentLine = new StringBuilder();
        int lineWidth = 0;
        for (String word : words) {
            int wordWidth = font.width(word);
            if (lineWidth + wordWidth > maxWidthPerLine) {
                if (lineWidth > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    lineWidth = 0;
                } else {
                    lines.add(word);
                    continue;
                }
            }
            currentLine.append(word);
            lineWidth += wordWidth;
        }
        if (lineWidth > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    static int rowCount(int itemCount) {
        return Mth.clamp((int) Math.ceil(itemCount / (double) MAX_ITEMS_PER_ROW), 1, MAX_ROWS);
    }

    static LayoutMetrics layoutFor(int itemCount) {
        int rows = rowCount(itemCount);
        int[] rowCounts = new int[rows];
        int itemsPerRow = itemCount / rows;
        int remainder = itemCount - itemsPerRow * rows;
        for (int row = 0; row < rows; row++) {
            rowCounts[row] = itemsPerRow + (remainder-- > 0 ? 1 : 0);
        }
        int totalWidth = rowWidth(rowCounts[0]);
        int totalHeight = rows * CELL_HEIGHT + (rows > 1 ? (rows - 1) * CELL_SPACING : 0);
        return new LayoutMetrics(rows, rowCounts, totalWidth, totalHeight,
            -totalWidth / 2, -totalHeight / 2);
    }

    private static int rowWidth(int count) {
        return count * CELL_WIDTH + (count - 1) * CELL_SPACING;
    }

    static int itemsY(int screenHeight) {
        return (int) (MAIN_Y_MULTIPLIER * screenHeight + ITEMS_Y_OFFSET);
    }

    static int descriptionWidth(int screenWidth) {
        return (int) (screenWidth * DESCRIPTION_WIDTH_MULTIPLIER);
    }

    static int descriptionY(int screenHeight, int itemAreaHeight) {
        return itemsY(screenHeight) - 10 + Math.max(itemAreaHeight, 48);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    record LayoutMetrics(int rows, int[] rowCounts, int totalWidth, int totalHeight, int areaX, int areaY) {
    }

    private record ItemEntry(ResourceLocation id, ItemStack stack) {
    }

    PonderTag getTag() {
        return tag;
    }
}
