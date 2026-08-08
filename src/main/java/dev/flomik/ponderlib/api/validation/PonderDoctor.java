package dev.flomik.ponderlib.api.validation;
import dev.flomik.ponderlib.api.registration.*;
import dev.flomik.ponderlib.foundation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import static dev.flomik.ponderlib.api.validation.PonderDiagnostic.Severity.*;
public final class PonderDoctor {
    private static final int MAX_RUNTIME_TICKS=20*60*30; private PonderDoctor(){}
    public static List<PonderDiagnostic> validateAll(){List<PonderDiagnostic> out=new ArrayList<>();for(PonderTag tag:PonderIndex.getTags().getAll())validateTag(tag,out);for(StoryBoardEntry e:PonderIndex.getScenes().getAllEntries())validateScene(e,out);return List.copyOf(out);}
    private static void validateTag(PonderTag tag,List<PonderDiagnostic> out){if(tag.title().getString().isBlank())out.add(d(ERROR,"tag.empty_title",tag.id(),"Tag title is empty"));if(tag.description().getString().isBlank())out.add(d(WARNING,"tag.empty_description",tag.id(),"Tag description is empty"));if(tag.icon().isEmpty())out.add(d(WARNING,"tag.empty_icon",tag.id(),"Tag icon is empty"));List<ResourceLocation> cs=PonderIndex.getTags().getComponents(tag.id());if(cs.isEmpty())out.add(d(WARNING,"tag.empty",tag.id(),"Tag has no components"));for(ResourceLocation c:cs)if(!PonderIndex.getScenes().doScenesExistForId(c))out.add(d(WARNING,"tag.component_without_scenes",c,"Component belongs to "+tag.id()+" but has no Ponder scenes"));}
    private static void validateScene(StoryBoardEntry e,List<PonderDiagnostic> out){ResourceLocation id=e.getSchematicLocation();if(Minecraft.getInstance().getResourceManager().getResource(id).isEmpty()){out.add(d(ERROR,"scene.missing_schematic",id,"Schematic resource does not exist"));return;}try{PonderScene s=PonderScene.compile(e);if(s.getTitle().getString().isBlank())out.add(d(WARNING,"scene.empty_title",id,"Scene title is empty"));int declared=s.getTotalTime(),measured=s.measureRuntimeTicks(MAX_RUNTIME_TICKS);if(measured<0)out.add(d(ERROR,"scene.never_finishes",id,"Scene did not finish within "+MAX_RUNTIME_TICKS+" ticks"));else if(measured!=declared)out.add(d(ERROR,"scene.bad_duration",id,"Timeline reports "+declared+" ticks but playback needs "+measured));for(int i=0;i<s.getKeyframeCount();i++){int time=s.getKeyframeTime(i);if(time<0||time>declared)out.add(d(ERROR,"scene.keyframe_outside_timeline",id,"Keyframe "+i+" is at "+time+" but duration is "+declared));}}catch(RuntimeException ex){out.add(d(ERROR,"scene.compile_failed",id,ex.getClass().getSimpleName()+": "+ex.getMessage()));}}
    private static PonderDiagnostic d(PonderDiagnostic.Severity s,String c,ResourceLocation id,String m){return new PonderDiagnostic(s,c,id,m);}
}
