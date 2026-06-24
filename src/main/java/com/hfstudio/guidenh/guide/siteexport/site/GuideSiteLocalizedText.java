package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.internal.util.LangUtil.normalizeLanguage;

import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

public record GuideSiteLocalizedText(String searchLabel, String searchPlaceholder, String searchEmptyTemplate,
    String languagesLabel, String fallbackBadge, String fallbackPrefix, String sharedPageLabel,
    String siteExportNoPages, String siteExportOpenGuide, String externalLinkTitle, String externalLinkMessage,
    String externalLinkOpen, String externalLinkBack) {

    private static final String SITE_EXPORT_KEY_PREFIX = "guidenh.siteexport.";
    private static final String LANGUAGE_KEY_PREFIX = SITE_EXPORT_KEY_PREFIX + "language.";

    public static GuideSiteLocalizedText resolve() {
        return new GuideSiteLocalizedText(
            translate("guidenh.siteexport.search.label"),
            translate("guidenh.siteexport.search.placeholder"),
            translate("guidenh.siteexport.search.emptyTemplate"),
            translate("guidenh.siteexport.languages.label"),
            translate("guidenh.siteexport.languages.fallbackBadge"),
            translate("guidenh.siteexport.languages.fallbackPrefix"),
            translate("guidenh.siteexport.languages.sharedPage"),
            translate("guideme.guidebook.SiteExportNoPages"),
            translate("guideme.guidebook.SiteExportOpenGuide"),
            translate("guideme.guidebook.SiteExportExternalLinkTitle"),
            translate("guideme.guidebook.SiteExportExternalLinkMessage"),
            translate("guideme.guidebook.SiteExportOpenLink"),
            translate("guideme.guidebook.SiteExportBack"));
    }

    public String fallbackTitle(@Nullable String sourceLanguageLabel) {
        String resolvedSourceLanguage = sourceLanguageLabel == null || sourceLanguageLabel.isEmpty() ? sharedPageLabel
            : sourceLanguageLabel;
        return fallbackPrefix + " " + resolvedSourceLanguage;
    }

    public String languageLabel(@Nullable String language) {
        if (language == null || language.isEmpty()) {
            return "";
        }
        return translate(LANGUAGE_KEY_PREFIX + normalizeLanguage(language));
    }

    public static String externalLinkPagePath(String language) {
        return "_site/external-link/" + normalizeLanguage(language) + ".html";
    }

    private static String translate(String key) {
        String translated = StatCollector.translateToLocal(key);
        return translated == null || translated.isEmpty() ? key : translated;
    }
}
