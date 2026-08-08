package dev.flomik.ponderlib.foundation.element;

import dev.flomik.ponderlib.api.element.PonderOverlayElement;
import dev.flomik.ponderlib.foundation.PonderScene;
import dev.flomik.ponderlib.foundation.ui.PonderUI;
import dev.flomik.ponderlib.render.PonderIconMask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A single floating icon anchored to a point in the scene, with no speech-box/tail/item of its own
 * — the lighter-weight input hint {@code OverlayInstructions#showScrollInput}/{@code
 * #showCenteredScrollInput}/{@code #showFilterSlotInput} use, as opposed to {@link
 * InputWindowElement}'s full mouse-button hint (which always needs {@code showControls}'s explicit
 * click direction to lay out its speech box against).
 */
public class InputIconElement implements PonderOverlayElement {

    /** Same wheel-and-ticks glyph {@link InputWindowElement#scroll()} draws inside its own box. */
    public static final String[] ICON_SCROLL = {
        " ##### ",
        "## # ##",
        "## # ##",
        "#######",
        "#  #  #",
        "#     #",
        " ##### ",
    };

    /** A funnel narrowing to a point — this library's "filter slot" glyph. */
    public static final String[] ICON_FILTER = {
        "#######",
        " ##### ",
        "  ###  ",
        "   #   ",
        "   #   ",
        "   #   ",
    };

    private static final int ICON_COLOR = 0xFFFFFFFF;

    private final Vec3 sceneSpace;
    private final String[] icon;
    private boolean visible;
    private float fade;

    public InputIconElement(Vec3 sceneSpace, String[] icon) {
        this.sceneSpace = sceneSpace;
        this.icon = icon;
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
        Vec2 anchor = screen.sceneToScreen(scene, sceneSpace);
        int width = PonderIconMask.width(icon);
        int height = PonderIconMask.height(icon);
        int alpha = Math.max(0x05, Math.round(0xFF * Mth.clamp(fade, 0F, 1F)));
        PonderIconMask.render(graphics, icon, (int) anchor.x - width / 2, (int) anchor.y - height / 2, 410,
            (alpha << 24) | (ICON_COLOR & 0xFFFFFF));
    }
}
