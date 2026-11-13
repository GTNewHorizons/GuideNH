package com.hfstudio.guidenh.guide.internal;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResourcePack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.FrontmatterPageMeta;

import cpw.mods.fml.common.FMLContainerHolder;
import cpw.mods.fml.common.Loader;

public class GuidePageMetadataFormatter {

    private GuidePageMetadataFormatter() {}

    public static String format(FontRenderer fr, int maxW, String sourcePack,
        @Nullable IResourcePack sourceResourcePack, @Nullable FrontmatterPageMeta meta) {

        String sourceDisplay = getSourceDisplayName(sourcePack, sourceResourcePack);
        List<String> authors = meta != null ? meta.authors() : List.of();
        String dateVal = meta != null ? meta.date() : null;
        String updatedVal = meta != null ? meta.updated() : null;
        String authorsStr = buildAuthorsString(authors);

        // Width calculations use raw (non-italic) text so getStringWidth() is accurate
        String rawAuthorPart = authorsStr.isEmpty() ? "" : GuidebookText.PageMetaAuthor.text(authorsStr);
        String rawDatePart = dateVal != null ? GuidebookText.PageMetaDate.text(dateVal) : "";
        String rawUpdatedPart = updatedVal != null ? GuidebookText.PageMetaUpdated.text(updatedVal) : "";
        String rawFull = GuidebookText.PageMetaContentFrom.text(sourceDisplay) + rawAuthorPart
            + rawDatePart
            + rawUpdatedPart;

        if (fr.getStringWidth(rawFull) <= maxW) {
            return formatBottomBar(sourceDisplay, authorsStr, dateVal, updatedVal);
        }

        int prefixW = fr.getStringWidth(GuidebookText.PageMetaContentFrom.text(""));
        int ellipsisW = fr.getStringWidth("...");
        int rawSuffixW = fr.getStringWidth(rawAuthorPart + rawDatePart + rawUpdatedPart);
        int availableForSource = maxW - prefixW - rawSuffixW - ellipsisW;
        if (availableForSource > 0) {
            String truncSrc = truncateStringToWidth(fr, sourceDisplay, availableForSource);
            String attempt = GuidebookText.PageMetaContentFrom.text(truncSrc + "...") + rawAuthorPart
                + rawDatePart
                + rawUpdatedPart;
            if (fr.getStringWidth(attempt) <= maxW) {
                return formatBottomBar(truncSrc + "...", authorsStr, dateVal, updatedVal);
            }
        }

        if (!authors.isEmpty()) {
            String firstAuthor = authors.getFirst();
            int authorPrefixW = fr.getStringWidth(GuidebookText.PageMetaAuthor.text(""));
            int availableForAuthor = maxW - prefixW
                - ellipsisW
                - authorPrefixW
                - ellipsisW
                - fr.getStringWidth(rawDatePart + rawUpdatedPart);
            String truncSrc2 = truncateStringToWidth(
                fr,
                sourceDisplay,
                availableForAuthor > 0 ? availableForAuthor : fr.getStringWidth(sourceDisplay));
            String rawSingleAuthorPart = GuidebookText.PageMetaAuthor.text(firstAuthor);
            String attempt2 = GuidebookText.PageMetaContentFrom.text(truncSrc2 + "...") + rawSingleAuthorPart
                + rawDatePart
                + rawUpdatedPart;
            if (fr.getStringWidth(attempt2) <= maxW) {
                return formatBottomBar(truncSrc2 + "...", firstAuthor, dateVal, updatedVal);
            }
        }

        return truncateStringToWidth(fr, rawFull, maxW);
    }

    /**
     * Builds the bottom-bar string with §o italic formatting around each placeholder value.
     * §r resets to white; §7 restores dark gray (#AAAAAA) to match the draw color.
     */
    private static String formatBottomBar(String sourceDisplay, String authorsStr, @Nullable String dateVal,
        @Nullable String updatedVal) {
        String authorPart = authorsStr.isEmpty() ? "" : GuidebookText.PageMetaAuthor.text("§o" + authorsStr + "§r§7");
        String datePart = dateVal != null ? GuidebookText.PageMetaDate.text("§o" + dateVal + "§r§7") : "";
        String updatedPart = updatedVal != null ? GuidebookText.PageMetaUpdated.text("§o" + updatedVal + "§r§7") : "";
        return GuidebookText.PageMetaContentFrom.text("§o" + sourceDisplay + "§r§7") + authorPart
            + datePart
            + updatedPart;
    }

    private static String buildAuthorsString(List<String> authors) {
        if (authors.isEmpty()) return "";
        if (authors.size() == 1) return authors.get(0);
        if (authors.size() == 2) return authors.get(0) + ", " + authors.get(1);
        return authors.get(0) + ", " + authors.get(1) + "...";
    }

    private static String truncateStringToWidth(FontRenderer fr, String text, int maxW) {
        if (fr.getStringWidth(text) <= maxW) return text;
        int accWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            int cw = fr.getCharWidth(text.charAt(i));
            if (accWidth + cw > maxW) {
                return text.substring(0, i);
            }
            accWidth += cw;
        }
        return text;
    }

    private static String getSourceDisplayName(String sourcePack, @Nullable IResourcePack sourceResourcePack) {
        if (sourceResourcePack instanceof FMLContainerHolder holder) {
            var container = holder.getFMLContainer();
            if (container != null) {
                String modName = container.getName();
                if (modName != null && !modName.isBlank()) {
                    return modName;
                }
                String modId = container.getModId();
                if (modId != null && !modId.isBlank()) {
                    return modId;
                }
            }
        }
        if (sourceResourcePack != null) {
            return formatResourcePackName(sourceResourcePack.getPackName());
        }

        int colon = sourcePack.indexOf(':');
        String namespace = colon >= 0 ? sourcePack.substring(colon + 1) : sourcePack;

        // Pages constructed outside a resource-pack load keep their existing mod-name fallback.
        try {
            var mod = Loader.instance()
                .getIndexedModList()
                .get(namespace);
            if (mod != null) {
                return mod.getName();
            }
        } catch (Throwable ignored) {}
        return namespace;
    }

    private static String formatResourcePackName(String packName) {
        if (packName.length() > 4 && packName.regionMatches(true, packName.length() - 4, ".zip", 0, 4)) {
            return packName.substring(0, packName.length() - 4);
        }
        return packName;
    }
}
