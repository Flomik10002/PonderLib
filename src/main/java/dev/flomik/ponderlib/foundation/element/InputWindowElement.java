package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.api.Pointing;
import dev.flomik.ponderlib.api.element.PonderOverlayElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.ui.PonderUI;
import dev.flomik.ponderlib.render.PonderIconMask;
import dev.flomik.ponderlib.render.PonderSpeechBox;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An input hint anchored to a point in the scene — "right-click here", optionally with the item to
 * use: a speech box whose tail points at the spot, holding a mouse-input icon, an optional item,
 * and an optional modifier-key qualifier.
 * <p>
 * The mouse icons are plain code-drawn pixel masks (see {@link PonderIconMask}, shared with the
 * scene screen's buttons) rather than image assets, so this ships no art files at all. The item, by
 * contrast, needs no asset of ours either — {@code GuiGraphics#renderItem} draws whatever the
 * caller passes.
 */
public class InputWindowElement implements PonderOverlayElement {

    /** A mouse body with the left button filled. */
    private static final String[] ICON_LEFT_CLICK = {
        " ##### ",
        "###  ##",
        "###  ##",
        "#######",
        "#     #",
        "#     #",
        " ##### ",
    };
    /** The same body with the right button filled instead. */
    private static final String[] ICON_RIGHT_CLICK = {
        " ##### ",
        "##  ###",
        "##  ###",
        "#######",
        "#     #",
        "#     #",
        " ##### ",
    };
    /** Body with the wheel marked, plus up/down ticks. */
    private static final String[] ICON_SCROLL = {
        " ##### ",
        "## # ##",
        "## # ##",
        "#######",
        "#  #  #",
        "#     #",
        " ##### ",
    };

    private static final int ICON_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND = 0xFF000000;
    private static final int BORDER_TOP = 0x40FFEEDD;
    private static final int BORDER_BOT = 0x20FFEEDD;
    private static final int PADDING = 4;

    private final Vec3 sceneSpace;
    private final Pointing pointing;
    @Nullable
    private String[] icon;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    private Component qualifier;
    private boolean visible;
    private float fade;

    public InputWindowElement(Vec3 sceneSpace, Pointing pointing) {
        this.sceneSpace = sceneSpace;
        this.pointing = pointing;
    }

    public void leftClick() {
        icon = ICON_LEFT_CLICK;
    }

    public void rightClick() {
        icon = ICON_RIGHT_CLICK;
    }

    public void scroll() {
        icon = ICON_SCROLL;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void setQualifier(String text) {
        this.qualifier = Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    public void setFade(float fade) {
        this.fade = fade;
    }

    public float getFade() {
        return fade;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(PonderScene scene, GuiGraphics graphics, float partialTick) {
        if (!visible || fade < 1 / 16F || !(Minecraft.getInstance().screen instanceof PonderUI screen)) {
            return;
        }
        Font font = screen.getFont();
        Vec2 anchor = screen.sceneToScreen(scene, sceneSpace);

        int iconWidth = icon == null ? 0 : PonderIconMask.width(icon);
        int iconHeight = icon == null ? 0 : PonderIconMask.height(icon);
        int itemWidth = item.isEmpty() ? 0 : 16;
        int qualifierWidth = qualifier == null ? 0 : font.width(qualifier) + 2;

        int contentWidth = iconWidth + (itemWidth > 0 ? itemWidth + 2 : 0) + qualifierWidth;
        int contentHeight = Math.max(Math.max(iconHeight, itemWidth), qualifier == null ? 0 : 9);
        int boxWidth = contentWidth + PADDING * 2;
        int boxHeight = contentHeight + PADDING * 2;

        // The tail points at the anchor, so the box sits on the opposite side of it.
        int boxX = switch (pointing) {
            case LEFT -> (int) anchor.x + 10;
            case RIGHT -> (int) anchor.x - 10 - boxWidth;
            default -> (int) anchor.x - boxWidth / 2;
        };
        int boxY = switch (pointing) {
            case UP -> (int) anchor.y + 10;
            case DOWN -> (int) anchor.y - 10 - boxHeight;
            default -> (int) anchor.y - boxHeight / 2;
        };

        PonderSpeechBox.render(graphics, boxX, boxY, boxWidth, boxHeight, pointing,
            400, BACKGROUND, BORDER_TOP, BORDER_BOT, fade);

        int alpha = Math.max(0x05, Math.round(0xFF * Mth.clamp(fade, 0F, 1F)));
        int cursorX = boxX + PADDING;
        int centreY = boxY + boxHeight / 2;

        if (icon != null) {
            PonderIconMask.render(graphics, icon, cursorX, centreY - iconHeight / 2, 410,
                (alpha << 24) | (ICON_COLOR & 0xFFFFFF));
            cursorX += iconWidth + 2;
        }
        if (!item.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 410);
            graphics.renderItem(item, cursorX, centreY - 8);
            graphics.pose().popPose();
            cursorX += itemWidth + 2;
        }
        if (qualifier != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 410);
            graphics.drawString(font, qualifier, cursorX, centreY - 4, (alpha << 24) | 0xBBBBBB, false);
            graphics.pose().popPose();
        }
    }
}
