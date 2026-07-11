package com.hfstudio.guidenh.guide.internal.welcome;

import java.io.IOException;
import java.io.InputStream;

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
    private static final ResourceLocation FALLBACK_RESOURCE = new ResourceLocation(
        GuideNH.MODID,
        CONTENT_ROOT_FOLDER + "/" + PAGE_ID.getResourcePath());

    private GuideWelcomeContent() {}

    public static LoadedContent load() {
        String language = LangUtil.normalizeLanguage(LangUtil.getCurrentLanguage());
        LoadedContent content = load(localizedResource(language), language);
        if (content == null && !LangUtil.ENGLISH_LANGUAGE.equals(language)) {
            content = load(localizedResource(LangUtil.ENGLISH_LANGUAGE), LangUtil.ENGLISH_LANGUAGE);
        }
        if (content == null) {
            content = load(FALLBACK_RESOURCE, LangUtil.ENGLISH_LANGUAGE);
        }
        if (content != null) {
            return content;
        }

        GuideDebugLog.warnAlways("[GuideNH] Failed to load any welcome popup content");
        return new LoadedContent(GuideNH.MODID, LangUtil.ENGLISH_LANGUAGE, "");
    }

    private static LoadedContent load(ResourceLocation resource, String language) {
        byte[] bytes = readBytes(resource);
        if (bytes == null) {
            return null;
        }

        GuideLocalizedPageSourceResolver.ResolvedGuidePageSource resolved = GuideLocalizedPageSourceResolver
            .resolve(language, CONTENT_ROOT_FOLDER, PAGE_ID, bytes);
        String sourcePack = resolved.localized() ? GuideNH.MODID + ":" + CONTENT_ROOT_FOLDER + "#" + resolved.langKey()
            : resource.toString();
        return new LoadedContent(sourcePack, language, resolved.source());
    }

    private static ResourceLocation localizedResource(String language) {
        return new ResourceLocation(GuideNH.MODID, "welcome/" + language + ".md");
    }

    private static byte[] readBytes(ResourceLocation resource) {
        try (InputStream input = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(resource)
            .getInputStream()) {
            return input.readAllBytes();
        } catch (IOException ignored) {
            return null;
        }
    }

    @Desugar
    public record LoadedContent(String sourcePack, String language, String source) {}
}
