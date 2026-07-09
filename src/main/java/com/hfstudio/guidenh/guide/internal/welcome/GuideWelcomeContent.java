package com.hfstudio.guidenh.guide.internal.welcome;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideWelcomeContent {

    private static final ResourceLocation FALLBACK_RESOURCE = new ResourceLocation(GuideNH.MODID, "welcome.txt");

    private GuideWelcomeContent() {}

    public static LoadedContent load() {
        String language = LangUtil.getCurrentLanguage();
        ResourceLocation[] candidates = new ResourceLocation[] { localizedResource(language, "lang"),
            localizedResource(language, "txt"), localizedResource(LangUtil.ENGLISH_LANGUAGE, "lang"),
            localizedResource(LangUtil.ENGLISH_LANGUAGE, "txt"), FALLBACK_RESOURCE };

        for (ResourceLocation resource : candidates) {
            String content = load(resource);
            if (content != null) {
                String normalizedLanguage = resource.getResourcePath()
                    .contains("/" + LangUtil.normalizeLanguage(language) + ".") ? language : LangUtil.ENGLISH_LANGUAGE;
                return new LoadedContent(resource.toString(), LangUtil.normalizeLanguage(normalizedLanguage), content);
            }
        }

        GuideDebugLog.warnAlways("[GuideNH] Failed to load any welcome popup content");
        return new LoadedContent(GuideNH.MODID, LangUtil.ENGLISH_LANGUAGE, "");
    }

    private static ResourceLocation localizedResource(String language, String extension) {
        return new ResourceLocation(GuideNH.MODID, "welcome/" + LangUtil.normalizeLanguage(language) + "." + extension);
    }

    private static String load(ResourceLocation resource) {
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(resource)
            .getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    @Desugar
    public record LoadedContent(String sourcePack, String language, String source) {}
}
