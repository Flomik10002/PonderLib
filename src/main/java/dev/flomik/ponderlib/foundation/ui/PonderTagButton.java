package dev.flomik.ponderlib.foundation.ui;

import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;

final class PonderTagButton extends AbstractWidget {
    private final PonderTag tag;
    private final Runnable callback;

    PonderTagButton(int x, int y, PonderTag tag, Runnable callback) {
        super(x, y, 112, 30, tag.title());
        this.tag = tag;
        this.callback = callback;
        if (!tag.description().getString().isBlank()) setTooltip(Tooltip.create(tag.description()));
    }

    @Override public void onClick(double mouseX, double mouseY) { callback.run(); }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int border = isHovered() ? 0xFFE4C869 : 0xFF665A3A;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xD0101010);
        graphics.renderOutline(getX(), getY(), width, height, border);
        if (!tag.icon().isEmpty()) graphics.renderItem(tag.icon(), getX() + 6, getY() + 7);
        graphics.drawString(Minecraft.getInstance().font, tag.title(), getX() + 27, getY() + 11, 0xFFFFFF, false);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
