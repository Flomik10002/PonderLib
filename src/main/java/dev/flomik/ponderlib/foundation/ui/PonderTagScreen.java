package dev.flomik.ponderlib.foundation.ui;
import dev.flomik.ponderlib.api.registration.*;
import dev.flomik.ponderlib.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
public final class PonderTagScreen extends Screen {
    private final PonderTag tag; private final Screen previous; private ComponentList list;
    public PonderTagScreen(PonderTag tag,Screen previous){super(tag.title());this.tag=tag;this.previous=previous;}
    @Override protected void init(){list=new ComponentList(minecraft,width,height,108,height-20,24);list.replace("");addRenderableWidget(list);EditBox search=new EditBox(font,width/2-100,82,200,20,Component.literal("Search components"));search.setHint(Component.literal("Search..."));search.setResponder(list::replace);addRenderableWidget(search);addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(4,4,50,20).build());}
    @Override public void onClose(){minecraft.setScreen(previous);}
    @Override public void render(GuiGraphics g,int x,int y,float pt){super.render(g,x,y,pt);if(!tag.icon().isEmpty()){g.pose().pushPose();g.pose().translate(width/2F-24,30,0);g.pose().scale(3,3,1);g.renderItem(tag.icon(),0,0);g.pose().popPose();}g.drawCenteredString(font,tag.title(),width/2,51,0xFFFFFF);g.drawCenteredString(font,tag.description(),width/2,65,0xAAAAAA);}
    @Override public boolean isPauseScreen(){return false;}
    private final class ComponentList extends ObjectSelectionList<ComponentList.Entry>{
        private ComponentList(Minecraft m,int w,int h,int y0,int y1,int ih){super(m,w,h,y0,y1,ih);}void addComponent(ResourceLocation id){addEntry(new Entry(id));}void replace(String query){clearEntries();String needle=query.toLowerCase(java.util.Locale.ROOT);for(ResourceLocation id:PonderIndex.getTags().getComponents(tag.id())){String name=BuiltInRegistries.ITEM.getOptional(id).map(item->item.getDefaultInstance().getHoverName().getString()).orElse(id.toString());if(needle.isBlank()||name.toLowerCase(java.util.Locale.ROOT).contains(needle)||id.toString().toLowerCase(java.util.Locale.ROOT).contains(needle))addComponent(id);}}
        private final class Entry extends ObjectSelectionList.Entry<Entry>{private final ResourceLocation id;private final ItemStack stack;private final Component label;private Entry(ResourceLocation id){this.id=id;stack=BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);label=stack.isEmpty()?Component.literal(id.toString()):stack.getHoverName();}@Override public Component getNarration(){return label;}@Override public void render(GuiGraphics g,int index,int top,int left,int width,int height,int mx,int my,boolean hovering,float pt){Collection<StoryBoardEntry> scenes=PonderIndex.getScenes().getScenes(id);if(hovering)g.fill(left,top,left+width,top+height,0x40FFFFFF);if(!stack.isEmpty())g.renderItem(stack,left+4,top+3);g.drawString(font,label,left+26,top+7,scenes.isEmpty()?0x777777:0xFFFFFF,false);if(!scenes.isEmpty())g.drawString(font,Integer.toString(scenes.size()),left+width-18,top+7,0xAAAAAA,false);}@Override public boolean mouseClicked(double x,double y,int b){Collection<StoryBoardEntry> scenes=PonderIndex.getScenes().getScenes(id);if(scenes.isEmpty())return false;minecraft.setScreen(PonderUI.of(scenes.iterator().next()).withPreviousScreen(PonderTagScreen.this));return true;}}
    }
}
