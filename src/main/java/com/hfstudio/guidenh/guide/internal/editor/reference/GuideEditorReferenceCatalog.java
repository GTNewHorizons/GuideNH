package com.hfstudio.guidenh.guide.internal.editor.reference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParser;
import com.hfstudio.guidenh.libs.mdx.MdxCommentMasker;

public class GuideEditorReferenceCatalog {

    private static final String RESOURCE_ROOT = "/assets/guidenh/editor-reference/";
    private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern FENCE = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final Pattern DOCUMENT_SUFFIX = Pattern.compile("(?i)(?:-(?:zh-CN|en-US))?(?:\\.md)?$");
    private static final Pattern SLUG_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private final List<GuideEditorReferenceSection> sections;

    public GuideEditorReferenceCatalog(List<GuideEditorReferenceSection> sections) {
        this.sections = List.copyOf(sections);
    }

    public static GuideEditorReferenceCatalog load(String language) throws IOException {
        String locale = language.toLowerCase(Locale.ROOT);
        String localePath = locale.replaceFirst("_", "/_");
        String index = readResource("index.json");
        if (index == null) {
            throw new IOException("Missing editor reference index");
        }
        List<GuideEditorReferenceSection> topics = new ArrayList<>();
        for (var entry : new JsonParser().parse(index)
            .getAsJsonObject()
            .getAsJsonArray("documents")) {
            String id = entry.getAsJsonObject()
                .get("id")
                .getAsString();
            String source = readResource(localePath + "/" + id + ".md");
            if (source == null) {
                source = readResource("en/_us/" + id + ".md");
            }
            if (source == null) {
                throw new IOException("Missing editor reference document: " + id);
            }
            topics.addAll(splitSections(id, source));
        }
        return new GuideEditorReferenceCatalog(topics);
    }

    private static @Nullable String readResource(String path) throws IOException {
        try (InputStream stream = GuideEditorReferenceCatalog.class.getResourceAsStream(RESOURCE_ROOT + path)) {
            return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public List<GuideEditorReferenceSection> sections() {
        return sections;
    }

    public List<GuideEditorReferenceSection> search(String query) {
        String normalized = query.trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return sections;
        }
        return sections.stream()
            .filter(
                section -> section.searchText()
                    .contains(normalized))
            .toList();
    }

    public @Nullable GuideEditorReferenceSection find(String documentId, @Nullable String anchor) {
        String normalizedId = normalizeDocumentId(documentId);
        GuideEditorReferenceSection first = null;
        for (var section : sections) {
            if (!section.documentId()
                .equalsIgnoreCase(normalizedId)) {
                continue;
            }
            if (first == null) {
                first = section;
            }
            if (anchor != null && section.anchors()
                .contains(anchor)) {
                return section;
            }
        }
        return first;
    }

    public static String normalizeDocumentId(String documentId) {
        String name = documentId.substring(documentId.lastIndexOf('/') + 1);
        return DOCUMENT_SUFFIX.matcher(name)
            .replaceFirst("");
    }

    public static String headingAnchor(String title) {
        String slug = SLUG_SEPARATOR.matcher(title.toLowerCase(Locale.ROOT))
            .replaceAll("-");
        return slug.replaceAll("^-|-$", "");
    }

    public static List<GuideEditorReferenceSection> splitSections(String documentId, String source) {
        List<GuideEditorReferenceSection> result = new ArrayList<>();
        StringBuilder markdown = new StringBuilder();
        List<String> anchors = new ArrayList<>();
        String documentTitle = documentId;
        String title = documentId;
        int depth = 1;
        boolean hasHeading = false;
        char fenceCharacter = 0;
        int fenceLength = 0;
        for (String line : MdxCommentMasker.mask(source)
            .split("\\R", -1)) {
            var fence = FENCE.matcher(line);
            boolean fenceLine = fence.matches();
            if (fenceLine) {
                String marker = fence.group(1);
                if (fenceCharacter == 0) {
                    if (marker.charAt(0) != '`' || !fence.group(2)
                        .contains("`")) {
                        fenceCharacter = marker.charAt(0);
                        fenceLength = marker.length();
                    }
                } else if (marker.charAt(0) == fenceCharacter && marker.length() >= fenceLength
                    && fence.group(2)
                        .isBlank()) {
                            fenceCharacter = 0;
                        }
            }
            var heading = HEADING.matcher(line);
            if (!fenceLine && fenceCharacter == 0 && heading.matches()) {
                int nextDepth = heading.group(1)
                    .length();
                String nextTitle = heading.group(2)
                    .replace("`", "");
                if (nextDepth <= 3) {
                    if (hasHeading) {
                        result.add(section(documentId, documentTitle, title, depth, markdown, anchors));
                        markdown.setLength(0);
                        anchors.clear();
                    }
                    hasHeading = true;
                    title = nextTitle;
                    depth = nextDepth;
                    if (depth == 1) {
                        documentTitle = title;
                    }
                }
                anchors.add(headingAnchor(nextTitle));
            }
            markdown.append(line)
                .append('\n');
        }
        if (hasHeading) {
            result.add(section(documentId, documentTitle, title, depth, markdown, anchors));
        }
        return List.copyOf(result);
    }

    private static GuideEditorReferenceSection section(String documentId, String documentTitle, String title, int depth,
        StringBuilder markdown, List<String> anchors) {
        String text = markdown.toString();
        return new GuideEditorReferenceSection(
            documentId,
            documentTitle,
            title,
            depth,
            text,
            (documentId + "\n" + documentTitle + "\n" + text).toLowerCase(Locale.ROOT),
            anchors);
    }
}
