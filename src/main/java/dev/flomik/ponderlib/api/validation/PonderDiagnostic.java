package dev.flomik.ponderlib.api.validation;
import net.minecraft.resources.ResourceLocation;
public record PonderDiagnostic(Severity severity,String code,ResourceLocation subject,String message){public enum Severity{WARNING,ERROR}}
