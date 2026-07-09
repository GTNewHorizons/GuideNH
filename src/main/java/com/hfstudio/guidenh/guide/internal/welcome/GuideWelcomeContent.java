package com.hfstudio.guidenh.guide.internal.welcome;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.localization.GuideLocalizedPageSourceResolver;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideWelcomeContent {

    private static final String CONTENT_ROOT_FOLDER = "welcome";
    private static final ResourceLocation PAGE_ID = new ResourceLocation(GuideNH.MODID, "welcome.md");

    private GuideWelcomeContent() {}

    public static LoadedContent load() {
        String language = LangUtil.normalizeLanguage(LangUtil.getCurrentLanguage());
        LoadedContent content = load(language);
        if (content != null) {
            return content;
        }

        if (!LangUtil.ENGLISH_LANGUAGE.equals(language)) {
            content = load(LangUtil.ENGLISH_LANGUAGE);
            if (content != null) {
                return content;
            }
        }

        GuideDebugLog.warnAlways("[GuideNH] Failed to load any welcome popup content");
        return new LoadedContent(GuideNH.MODID, LangUtil.ENGLISH_LANGUAGE, "");
    }

    private static LoadedContent load(String language) {
        ResourceLocation resource = localizedResource(language);
        String content = load(resource);
        if (content == null) {
            return null;
        }
        GuideLocalizedPageSourceResolver.ResolvedGuidePageSource resolved = GuideLocalizedPageSourceResolver
            .resolve(language, CONTENT_ROOT_FOLDER, PAGE_ID, content.getBytes(StandardCharsets.UTF_8));
        String sourcePack = resolved.localized() ? GuideNH.MODID + ":" + CONTENT_ROOT_FOLDER + "#" + resolved.langKey()
            : resource.toString();
        return new LoadedContent(sourcePack, LangUtil.normalizeLanguage(language), resolved.source());
    }

    private static ResourceLocation localizedResource(String language) {
        return new ResourceLocation(GuideNH.MODID, "welcome/" + LangUtil.normalizeLanguage(language) + ".md");
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
