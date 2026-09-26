package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.internal.util.LangUtil.normalizeLanguage;

import java.util.Locale;

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
        String normalized = normalizeLanguage(language).replace('-', '_');
        String translated = translate(LANGUAGE_KEY_PREFIX + normalized);
        if (!translated.equals(LANGUAGE_KEY_PREFIX + normalized)) {
            return translated;
        }
        return displayLanguageLabel(normalized);
    }

    public static String externalLinkPagePath(String language) {
        return "_site/external-link/" + normalizeLanguage(language) + ".html";
    }

    private static String translate(String key) {
        String translated = StatCollector.translateToLocal(key);
        return translated == null || translated.isEmpty() ? key : translated;
    }

    private static String displayLanguageLabel(String language) {
        String languageTag = language.replace('_', '-');
        Locale locale = Locale.forLanguageTag(languageTag);
        String displayName = locale.getDisplayName(Locale.ENGLISH);
        if (!displayName.isEmpty() && !displayName.equals(languageTag)) {
            return displayName;
        }

        int separator = language.indexOf('_');
        if (separator > 0 && separator + 1 < language.length()) {
            String languagePart = language.substring(0, separator);
            String regionPart = language.substring(separator + 1).toUpperCase(Locale.ROOT);
            return languagePart + " (" + regionPart + ")";
        }
        return language;
    }
}
