package dev.flomik.ponderlib.foundation.ui;
import dev.flomik.ponderlib.api.registration.PonderTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
final class PonderTagButton extends AbstractWidget {
    private final PonderTag tag; private final Runnable callback;
    PonderTagButton(int x,int y,PonderTag tag,Runnable callback){super(x,y,112,30,tag.title());this.tag=tag;this.callback=callback;if(!tag.description().getString().isBlank())setTooltip(Tooltip.create(tag.description()));}
    @Override public void onClick(double x,double y){callback.run();}
    @Override protected void renderWidget(GuiGraphics g,int x,int y,float pt){int border=isHovered()?0xFFE4C869:0xFF665A3A;g.fill(getX(),getY(),getX()+width,getY()+height,0xD0101010);g.renderOutline(getX(),getY(),width,height,border);if(!tag.icon().isEmpty())g.renderItem(tag.icon(),getX()+6,getY()+7);g.drawString(Minecraft.getInstance().font,tag.title(),getX()+27,getY()+11,0xFFFFFF,false);}
    @Override protected void updateWidgetNarration(NarrationElementOutput out){defaultButtonNarrationText(out);}
}
