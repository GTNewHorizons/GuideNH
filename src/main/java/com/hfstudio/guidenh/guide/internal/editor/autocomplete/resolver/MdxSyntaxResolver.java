package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxUtils;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstResource;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MdxSyntaxResolver implements SyntaxContextResolver {

    private static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();

    @Nullable
    private String cachedText;
    @Nullable
    private MdAstRoot cachedRoot;

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty() || cursorIndex < 0 || cursorIndex > text.length()) return null;

        if (isInFrontmatter(text, cursorIndex)) {
            TextSyntaxContext frontmatter = resolveFrontmatterText(text, cursorIndex);
            if (frontmatter != null && frontmatter.shouldAutocomplete()) {
                return frontmatter;
            }
            return resolvePlainTextWord(text, cursorIndex);
        }

        MdAstRoot root = parsedRoot(text);
        if (root != null) {
            MdAstCode code = findEnclosingNode(root, cursorIndex, MdAstCode.class);
            if (code != null) {
                if (code.lang != null && !code.lang.isEmpty()) {
                    TextSyntaxContext result = resolveFenceLanguage(code, text, cursorIndex);
                    if (result != null) return result;
                }
                return resolvePlainTextWord(text, cursorIndex);
            }
            TextSyntaxContext fromAst = resolveFromAst(root, text, cursorIndex);
            if (fromAst != null) {
                return fromAst;
            }
            return resolveTextLevelTagContext(text, cursorIndex);
        }

        TextSyntaxContext fromText = resolveTextLevelTagContext(text, cursorIndex);
        if (fromText != null) {
            return fromText;
        }
        TextSyntaxContext fence = resolveFenceLanguageLine(text, cursorIndex);
        return fence != null ? fence : null;
    }

    /**
     * The tag the caret is inside, read from the text: its attribute value, its attribute name, or its name
     * while the tag is being opened.
     */
    @Nullable
    private TextSyntaxContext resolveTextLevelTagContext(String text, int cursorIndex) {
        TextSyntaxContext attribute = resolveTextLevelAttribute(text, cursorIndex);
        if (attribute != null) {
            return attribute;
        }
        return resolveTagStart(text, cursorIndex, enclosingContainerName(text, cursorIndex));
    }

    /** The name of the nearest tag that opens before the caret and has not been closed, or null at the root. */
    @Nullable
    private static String enclosingContainerName(String text, int cursorIndex) {
        Deque<String> open = new ArrayDeque<>();
        int pos = 0;
        int limit = Math.min(cursorIndex, text.length());
        while (pos < limit) {
            int lt = text.indexOf('<', pos);
            if (lt < 0 || lt >= limit) {
                break;
            }
            // Comments and declarations do not open a tag.
            if (lt + 1 < text.length() && (text.charAt(lt + 1) == '!' || text.charAt(lt + 1) == '?')) {
                pos = lt + 1;
                continue;
            }
            boolean closing = lt + 1 < text.length() && text.charAt(lt + 1) == '/';
            int nameStart = closing ? lt + 2 : lt + 1;
            int nameEnd = nameStart;
            while (nameEnd < text.length() && isTagNameChar(text.charAt(nameEnd))) {
                nameEnd++;
            }
            if (nameEnd == nameStart) {
                pos = lt + 1;
                continue;
            }
            int tagEnd = findOpeningTagEnd(text, lt);
            // A tag whose opening form is not finished is the one being typed, so it encloses nothing yet.
            boolean unfinished = tagEnd >= text.length();
            if (unfinished) {
                break;
            }
            String name = text.substring(nameStart, nameEnd);
            if (closing) {
                if (!open.isEmpty() && open.peekLast()
                    .equals(name)) {
                    open.removeLast();
                }
            } else if (!(tagEnd >= 2 && text.charAt(tagEnd - 2) == '/')) {
                // A self-closing tag encloses nothing.
                open.addLast(name);
            }
            pos = Math.max(tagEnd, lt + 1);
        }
        return open.peekLast();
    }

    @Nullable
    public MdAstRoot parsedRoot(String text) {
        if (text.equals(cachedText)) {
            return cachedRoot;
        }
        MdAstRoot root;
        try {
            root = MdAst.fromMarkdown(text, PARSE_OPTIONS);
            MdAstToMdxConverter.convert(root, Collections.emptyMap());
        } catch (RuntimeException e) {
            cachedText = text;
            cachedRoot = null;
            return null;
        }
        cachedText = text;
        cachedRoot = root;
        return root;
    }

    /** The fence language at the caret, read from the line the caret is on. */
    @Nullable
    public static TextSyntaxContext resolveFenceLanguageLine(String text, int cursorIndex) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorIndex);
        if (lineEnd < 0) lineEnd = text.length();
        if (lineStart >= lineEnd) return null;

        int markerStart = lineStart;
        while (markerStart < lineEnd && text.charAt(markerStart) == ' ') markerStart++;
        char marker = markerStart < lineEnd ? text.charAt(markerStart) : 0;
        if (marker != '`' && marker != '~') return null;
        int runEnd = markerStart;
        while (runEnd < lineEnd && text.charAt(runEnd) == marker) runEnd++;
        if (runEnd - markerStart < 3) return null;

        int langStart = skipSpaces(text, runEnd, lineEnd);
        if (cursorIndex < langStart || cursorIndex > lineEnd) return null;

        String partial = text.substring(langStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.FENCE_LANGUAGE,
            langStart,
            cursorIndex,
            new FenceLanguageContext(langStart, cursorIndex, partial));
    }

    public static boolean isInFrontmatter(String text, int cursorIndex) {
        int firstBreak = text.indexOf('\n');
        if (firstBreak < 0) return false;
        if (!text.substring(0, firstBreak)
            .trim()
            .equals("---")) {
            return false;
        }
        int pos = firstBreak + 1;
        while (pos <= text.length()) {
            int lineEnd = text.indexOf('\n', pos);
            if (lineEnd < 0) lineEnd = text.length();
            if (text.substring(pos, lineEnd)
                .trim()
                .equals("---")) {
                // The block ends here, so only a caret before this line is inside the frontmatter.
                return cursorIndex <= pos;
            }
            if (lineEnd >= text.length()) {
                return false;
            }
            pos = lineEnd + 1;
        }
        return false;
    }

    @Nullable
    public TextSyntaxContext resolveFrontmatterText(String text, int cursorIndex) {
        return resolveFrontmatter(null, text, cursorIndex);
    }

    @Nullable
    public TextSyntaxContext resolveFromAst(MdAstRoot root, String text, int cursorIndex) {
        MdAstYamlFrontmatter yaml = findEnclosingNode(root, cursorIndex, MdAstYamlFrontmatter.class);
        if (yaml != null) {
            TextSyntaxContext result = resolveFrontmatter(yaml, text, cursorIndex);
            return result != null && result.shouldAutocomplete() ? result : resolvePlainTextWord(text, cursorIndex);
        }

        MdAstCode code = findEnclosingNode(root, cursorIndex, MdAstCode.class);
        if (code != null) {
            if (code.lang != null && !code.lang.isEmpty()) {
                TextSyntaxContext result = resolveFenceLanguage(code, text, cursorIndex);
                if (result != null) return result;
            }
            return resolvePlainTextWord(text, cursorIndex);
        }

        // 2.5. Markdown link/image URL
        MdAstResource res = findEnclosingLink(root, cursorIndex);
        if (res != null) {
            TextSyntaxContext result = resolveLinkUrl(res, text, cursorIndex);
            if (result != null) return result;
        }

        // 3. MDX element
        MdxJsxElementFields element = findEnclosingMdxElement(root, cursorIndex);
        if (element != null) {
            TextSyntaxContext result = resolveMdxAttribute(element, text, cursorIndex);
            if (result != null && result.getElementType() != SyntaxElementType.WORD) {
                return result;
            }
        }

        // 4. Tag start
        TextSyntaxContext tagStart = resolveTagStart(text, cursorIndex, element != null ? element.name() : null);
        if (tagStart != null) {
            return tagStart;
        }

        return null;
    }

    @Nullable
    public TextSyntaxContext resolveFrontmatter(@Nullable MdAstYamlFrontmatter yaml, String text, int cursorIndex) {
        String line = getLineAt(text, cursorIndex);
        if (line == null) return resolvePlainTextWord(text, cursorIndex);

        // Empty line: inherit context from preceding indented parent key
        if (line.trim()
            .isEmpty()) {
            return resolveFrontmatterEmptyLine(text, cursorIndex);
        }

        // List items inherit context from their parent key, regardless of
        // whether the value contains a colon (e.g. "guidenh:guide_icon").
        String trimmed = line.trim();
        if (isYamlListMarker(trimmed)) {
            return resolveFrontmatterEmptyLine(text, cursorIndex);
        }

        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) {
            return resolveFrontmatterDraftKey(text, cursorIndex);
        }

        String key = line.substring(0, colonIdx)
            .trim();
        if (key.isEmpty() || key.startsWith("#")) {
            return resolvePlainTextWord(text, cursorIndex);
        }

        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int valueStart = colonIdx + 1;
        while (valueStart < line.length() && line.charAt(valueStart) == ' ') valueStart++;

        int valueAbsStart = lineStart + valueStart;
        int valueAbsEnd = lineStart + line.length();

        // Cursor is on the value
        if (cursorIndex >= valueAbsStart && cursorIndex <= valueAbsEnd) {
            String partialText = text.substring(valueAbsStart, cursorIndex);
            return new TextSyntaxContext(
                SyntaxElementType.WORD,
                valueAbsStart,
                valueAbsEnd,
                new FrontmatterContext(key, true, valueAbsStart, valueAbsEnd, partialText));
        }

        // Cursor is on the key
        int keyStart = lineStart + line.indexOf(key);
        int keyEnd = keyStart + key.length();
        if (cursorIndex >= keyStart && cursorIndex <= keyEnd) {
            return new TextSyntaxContext(
                SyntaxElementType.WORD,
                keyStart,
                keyEnd,
                new FrontmatterContext(key, false, keyStart, keyEnd, text.substring(keyStart, cursorIndex)));
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    public TextSyntaxContext resolveFrontmatterDraftKey(String text, int cursorIndex) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        String typed = text.substring(lineStart, cursorIndex);
        if (typed.isEmpty() || !isBareYamlKey(typed)) {
            return resolvePlainTextWord(text, cursorIndex);
        }
        return new TextSyntaxContext(
            SyntaxElementType.WORD,
            lineStart,
            cursorIndex,
            new FrontmatterContext(typed, false, lineStart, cursorIndex, typed));
    }

    public static boolean isBareYamlKey(String typed) {
        for (int i = 0; i < typed.length(); i++) {
            char c = typed.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public TextSyntaxContext resolveFrontmatterEmptyLine(String text, int cursorIndex) {
        int prevLineEnd = text.lastIndexOf('\n', cursorIndex - 1);
        if (prevLineEnd < 0) return resolvePlainTextWord(text, cursorIndex);
        int prevLineStart = text.lastIndexOf('\n', prevLineEnd - 1) + 1;
        String prevLine = text.substring(prevLineStart, prevLineEnd);
        String prevTrimmed = prevLine.trim();
        if (prevTrimmed.isEmpty() || prevTrimmed.startsWith("#")) {
            return resolvePlainTextWord(text, cursorIndex);
        }

        int prevColon = prevLine.indexOf(':');
        if (prevColon < 0) return resolvePlainTextWord(text, cursorIndex);

        String prevKey = prevLine.substring(0, prevColon)
            .trim();
        if (isYamlBlockKey(prevLine, prevColon) || prevLine.indexOf(prevKey) == 0) {
            return resolveFrontmatterInheritedValue(text, cursorIndex, prevKey);
        }

        int prevIndent = prevLine.indexOf(prevKey);
        int searchPos = prevLineStart - 1;
        while (searchPos > 0) {
            int lineEnd = searchPos;
            int lineStart = text.lastIndexOf('\n', lineEnd - 1) + 1;
            String candidate = text.substring(lineStart, lineEnd);
            int cColon = candidate.indexOf(':');
            if (cColon >= 0) {
                String cKey = candidate.substring(0, cColon)
                    .trim();
                if (!cKey.isEmpty() && candidate.indexOf(cKey) < prevIndent) {
                    return resolveFrontmatterInheritedValue(text, cursorIndex, cKey);
                }
            }
            searchPos = lineStart - 1;
        }
        return resolvePlainTextWord(text, cursorIndex);
    }

    public static boolean isYamlBlockKey(String line, int colonIndex) {
        for (int i = colonIndex + 1; i < line.length(); i++) {
            if (line.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a value context for a line that carries no key of its own, such as a list entry or an empty line under.
     */
    public TextSyntaxContext resolveFrontmatterInheritedValue(String text, int cursorIndex, String key) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorIndex);
        if (lineEnd < 0) lineEnd = text.length();
        String line = text.substring(lineStart, Math.min(cursorIndex, lineEnd));
        int valueStart = lineStart + yamlEntryContentOffset(line);
        int valueEnd = Math.max(valueStart, lineEnd);
        return new TextSyntaxContext(
            SyntaxElementType.WORD,
            valueStart,
            valueEnd,
            new FrontmatterContext(key, true, valueStart, valueEnd, text.substring(valueStart, cursorIndex)));
    }

    public static int yamlEntryContentOffset(String line) {
        int index = 0;
        while (index < line.length() && line.charAt(index) == ' ') {
            index++;
        }
        if (index < line.length()
            && (line.charAt(index) == '-' || line.charAt(index) == '+' || line.charAt(index) == '*')
            && index + 1 < line.length()
            && line.charAt(index + 1) == ' ') {
            do {
                index++;
            } while (index < line.length() && line.charAt(index) == ' ');
        }
        return index;
    }

    public static boolean isYamlListMarker(String trimmed) {
        if (trimmed.isEmpty()) return false;
        char c = trimmed.charAt(0);
        if ((c == '-' || c == '*' || c == '+') && (trimmed.length() == 1 || trimmed.charAt(1) == ' ')) return true;
        int i = 0;
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) i++;
        return i > 0 && i < trimmed.length() && (trimmed.charAt(i) == '.' || trimmed.charAt(i) == ')');
    }

    @Nullable
    public TextSyntaxContext resolveFenceLanguage(MdAstCode code, String text, int cursorIndex) {
        UnistPosition pos = code.position();
        if (pos == null || pos.start() == null) return null;

        int fenceStart = pos.start()
            .offset();
        int lineEnd = text.indexOf('\n', fenceStart);
        if (lineEnd < 0) lineEnd = text.length();

        int langStart = fenceStart + 3;
        while (langStart < lineEnd && (text.charAt(langStart) == '`' || text.charAt(langStart) == '~')) {
            langStart++;
        }
        langStart = skipSpaces(text, langStart, lineEnd);

        if (cursorIndex < langStart || cursorIndex > lineEnd) return null;

        String partial = text.substring(langStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.FENCE_LANGUAGE,
            langStart,
            cursorIndex,
            new FenceLanguageContext(langStart, cursorIndex, partial));
    }

    @Nullable
    public MdAstResource findEnclosingLink(UnistNode node, int cursorIndex) {
        MdAstLink link = findEnclosingNode(node, cursorIndex, MdAstLink.class);
        if (link != null) return link;
        return findEnclosingNode(node, cursorIndex, MdAstImage.class);
    }

    @Nullable
    public TextSyntaxContext resolveLinkUrl(MdAstResource resource, String text, int cursorIndex) {
        UnistPosition pos = ((UnistNode) resource).position();
        if (pos == null || pos.start() == null || pos.end() == null) return null;

        int nodeStart = pos.start()
            .offset();
        int nodeEnd = pos.end()
            .offset();
        int parenOpen = text.indexOf('(', nodeStart);
        if (parenOpen < 0 || parenOpen >= nodeEnd) return null;
        int parenClose = text.lastIndexOf(')', nodeEnd - 1);
        if (parenClose < parenOpen) return null;

        int urlStart = parenOpen + 1;
        int urlEnd = parenClose;
        if (cursorIndex < urlStart || cursorIndex > urlEnd) return null;

        String tagName = resource instanceof MdAstImage ? "image" : "link";
        String partial = text.substring(urlStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.ATTRIBUTE_VALUE,
            urlStart,
            urlEnd,
            new MdxValueContext(tagName, "url", urlStart, urlEnd, partial, '\0'));
    }

    @Nullable
    private static TagSpan findOpenTagAt(String text, int cursorIndex) {
        int tagStart = text.lastIndexOf('<', Math.max(0, cursorIndex - 1));
        if (tagStart < 0) {
            return null;
        }
        if (tagStart + 1 >= text.length() || text.charAt(tagStart + 1) == '/'
            || text.charAt(tagStart + 1) == '!'
            || text.charAt(tagStart + 1) == '?') {
            return null;
        }
        int nameStart = tagStart + 1;
        int nameEnd = nameStart;
        while (nameEnd < text.length() && isTagNameChar(text.charAt(nameEnd))) {
            nameEnd++;
        }
        if (nameEnd == nameStart) {
            return null;
        }
        int tagEnd = findOpeningTagEnd(text, tagStart);
        if (cursorIndex > tagEnd) {
            return null;
        }
        return new TagSpan(text.substring(nameStart, nameEnd), tagStart, tagEnd);
    }

    private record TagSpan(String name, int tagStart, int tagEnd) {}

    @Nullable
    private TextSyntaxContext resolveTextLevelAttribute(String text, int cursorIndex) {
        TagSpan tag = findOpenTagAt(text, cursorIndex);
        if (tag == null) {
            return null;
        }
        if (cursorIndex <= tag.tagStart() + 1
            + tag.name()
                .length()) {
            return null;
        }
        TextSyntaxContext value = resolveTextLevelAttributeValue(text, tag, cursorIndex);
        if (value != null) {
            return value;
        }
        return resolveAttributeNameFromTag(text, tag.name(), tag.tagStart(), tag.tagEnd(), cursorIndex);
    }

    @Nullable
    private static TextSyntaxContext resolveTextLevelAttributeValue(String text, TagSpan tag, int cursorIndex) {
        int pos = tag.tagStart() + 1
            + tag.name()
                .length();
        int tagEnd = tag.tagEnd();
        while (pos < tagEnd) {
            pos = skipSpaces(text, pos, tagEnd);
            if (pos >= tagEnd || !isAttributeNameStart(text.charAt(pos))) {
                pos++;
                continue;
            }
            int attrStart = pos;
            while (pos < tagEnd && isAttributeNameChar(text.charAt(pos))) {
                pos++;
            }
            String attrName = text.substring(attrStart, pos);
            int afterName = skipSpaces(text, pos, tagEnd);
            if (afterName >= tagEnd || text.charAt(afterName) != '=') {
                continue;
            }
            int valueStart = skipSpaces(text, afterName + 1, tagEnd);
            AttributeValueBounds bounds = valueBounds(text, valueStart, tagEnd, cursorIndex);
            int rawEnd = Math.max(bounds.rawEnd, valueStart);
            if (cursorIndex >= bounds.valueStart && cursorIndex <= Math.max(bounds.valueEnd, rawEnd)) {
                String partial = text.substring(bounds.valueStart, cursorIndex);
                return new TextSyntaxContext(
                    SyntaxElementType.ATTRIBUTE_VALUE,
                    bounds.valueStart,
                    bounds.valueEnd,
                    new MdxValueContext(
                        tag.name(),
                        attrName,
                        bounds.valueStart,
                        bounds.valueEnd,
                        partial,
                        bounds.missingTerminator));
            }
            pos = Math.max(rawEnd, pos);
        }
        return null;
    }

    @Nullable
    public TextSyntaxContext resolveTagStart(String text, int cursorIndex, @Nullable String parentTagName) {
        if (cursorIndex < 1) return null;

        char atCursor = text.charAt(cursorIndex - 1);

        // Case 1: cursor immediately after '<' — start of a new tag
        if (atCursor == '<') {
            if (cursorIndex < text.length()) {
                char next = text.charAt(cursorIndex);
                if (next == '/' || next == '!' || next == '?') {
                    return null;
                }
            }
            if (cursorIndex >= 2) {
                char prev = text.charAt(cursorIndex - 2);
                if (prev != ' ' && prev != '\n' && prev != '\r' && prev != '>' && prev != '\t') {
                    return null;
                }
            }
            return new TextSyntaxContext(
                SyntaxElementType.TAG_START,
                cursorIndex,
                cursorIndex,
                new TagStartContext(cursorIndex, cursorIndex, "", parentTagName));
        }

        // Case 2: cursor inside a partial tag name after '<' (e.g. <I|, <Item|)
        if (isTagNameChar(atCursor)) {
            int nameStart = cursorIndex - 1;
            while (nameStart > 0 && isTagNameChar(text.charAt(nameStart - 1))) {
                nameStart--;
            }
            if (nameStart == 0 || text.charAt(nameStart - 1) != '<') {
                return null;
            }
            int tagStart = nameStart - 1;
            // Closing tag: </Name — don't autocomplete
            if (nameStart > tagStart + 1 && text.charAt(tagStart + 1) == '/') {
                return null;
            }
            String partial = text.substring(nameStart, cursorIndex);
            return new TextSyntaxContext(
                SyntaxElementType.TAG_START,
                tagStart,
                cursorIndex,
                new TagStartContext(nameStart, cursorIndex, partial, parentTagName));
        }

        return null;
    }

    public static boolean isTagNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-';
    }

    @Nullable
    public MdxJsxElementFields findEnclosingMdxElement(UnistNode node, int cursorIndex) {
        UnistPosition pos = node.position();
        if (pos != null && pos.start() != null && pos.end() != null) {
            if (cursorIndex < pos.start()
                .offset() || cursorIndex
                    > pos.end()
                        .offset()) {
                return null;
            }
        }

        // Search children first so nested elements are found innermost-first.
        if (node instanceof MdAstParent) {
            for (UnistNode child : ((MdAstParent<?>) node).children()) {
                MdxJsxElementFields found = findEnclosingMdxElement(child, cursorIndex);
                if (found != null) return found;
            }
        }

        if (node instanceof MdxJsxElementFields el && !isRecovered(el)) {
            return el;
        }

        return null;
    }

    public static boolean isRecovered(MdxJsxElementFields el) {
        if (el instanceof MdxJsxFlowElement f) return f.recovered;
        if (el instanceof MdxJsxTextElement t) return t.recovered;
        return false;
    }

    @Nullable
    public TextSyntaxContext resolveMdxAttribute(MdxJsxElementFields element, String text, int cursorIndex) {
        String tagName = element.name();
        if (tagName == null) return resolvePlainTextWord(text, cursorIndex);

        for (var attrNode : element.attributes()) {
            if (!(attrNode instanceof MdxJsxAttribute attr)) continue;
            if (attr.name == null || attr.name.isEmpty()) continue;

            UnistPosition pos = attrNode.position();
            if (pos == null || pos.start() == null || pos.end() == null) continue;

            int attrStart = pos.start()
                .offset();
            int attrEnd = pos.end()
                .offset();

            if (cursorIndex < attrStart || cursorIndex > attrEnd) continue;

            TextSyntaxContext valueContext = resolveAttributeValue(
                text,
                tagName,
                attr.name,
                attrStart,
                attrEnd,
                cursorIndex);
            if (valueContext != null) return valueContext;
            break;
        }

        UnistPosition elemPos = element.position();
        if (elemPos != null && elemPos.start() != null && elemPos.end() != null) {
            int tagStart = elemPos.start()
                .offset();
            int tagEnd = findOpeningTagEnd(text, tagStart);
            if (cursorIndex > tagStart && cursorIndex < tagEnd) {
                return resolveAttributeNameFromTag(text, tagName, tagStart, tagEnd, cursorIndex);
            }
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends UnistNode> T findEnclosingNode(UnistNode node, int cursorIndex, Class<T> type) {
        UnistPosition pos = node.position();
        if (pos != null && pos.start() != null && pos.end() != null) {
            if (cursorIndex < pos.start()
                .offset() || cursorIndex
                    > pos.end()
                        .offset()) {
                return null;
            }
        }

        if (type.isInstance(node)) {
            return (T) node;
        }

        if (node instanceof MdAstParent) {
            for (UnistNode child : ((MdAstParent<?>) node).children()) {
                T found = findEnclosingNode(child, cursorIndex, type);
                if (found != null) return found;
            }
        }

        return null;
    }

    public TextSyntaxContext resolvePlainTextWord(String text, int cursorIndex) {
        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    public TextSyntaxContext resolveAttributeValue(String text, String tagName, String attrName, int attrStart,
        int attrEnd, int cursorIndex) {
        int eqIdx = indexOf(text, '=', attrStart, attrEnd);
        if (eqIdx < 0) return null;

        int valueStart = skipSpaces(text, eqIdx + 1, attrEnd);
        if (valueStart > cursorIndex) return null;
        AttributeValueBounds bounds = valueBounds(text, valueStart, attrEnd, cursorIndex);
        if (cursorIndex < bounds.valueStart || cursorIndex > bounds.valueEnd) return null;

        String partialText = text.substring(bounds.valueStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.ATTRIBUTE_VALUE,
            bounds.valueStart,
            bounds.valueEnd,
            new MdxValueContext(
                tagName,
                attrName,
                bounds.valueStart,
                bounds.valueEnd,
                partialText,
                bounds.missingTerminator));
    }

    @Nullable
    public TextSyntaxContext resolveAttributeNameFromTag(String text, String tagName, int tagStart, int tagEnd,
        int cursorIndex) {
        int scanStart = Math.max(tagStart + 1 + tagName.length(), 0);
        if (cursorIndex < scanStart || cursorIndex > tagEnd) return null;
        if (isInsideAnyAttributeValue(text, scanStart, tagEnd, cursorIndex)) return null;

        int nameStart = cursorIndex;
        while (nameStart > scanStart && isAttributeNameChar(text.charAt(nameStart - 1))) {
            nameStart--;
        }
        int nameEnd = cursorIndex;
        while (nameEnd < tagEnd && isAttributeNameChar(text.charAt(nameEnd))) {
            nameEnd++;
        }
        String partial = text.substring(nameStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.ATTRIBUTE_NAME,
            nameStart,
            nameEnd,
            new MdxAttrNameContext(tagName, nameStart, nameEnd, partial));
    }

    public static int findOpeningTagEnd(String text, int tagStart) {
        boolean inSingle = false;
        boolean inDouble = false;
        int braceDepth = 0;
        for (int i = tagStart; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((inSingle || inDouble) && ch == '\\' && i + 1 < text.length()) {
                i++;
                continue;
            }
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (inSingle || inDouble) continue;
            if (ch == '{') {
                braceDepth++;
                continue;
            }
            if (ch == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
                continue;
            }
            if (ch == '>' && braceDepth == 0) return i + 1;
        }
        return text.length();
    }

    public static boolean isInsideAnyAttributeValue(String text, int scanStart, int tagEnd, int cursorIndex) {
        int pos = scanStart;
        while (pos < tagEnd) {
            pos = skipSpaces(text, pos, tagEnd);
            if (pos >= tagEnd || !isAttributeNameStart(text.charAt(pos))) {
                pos++;
                continue;
            }
            do {
                pos++;
            } while (pos < tagEnd && isAttributeNameChar(text.charAt(pos)));
            int afterName = skipSpaces(text, pos, tagEnd);
            if (afterName >= tagEnd || text.charAt(afterName) != '=') {
                pos = afterName;
                continue;
            }
            int rawValueStart = skipSpaces(text, afterName + 1, tagEnd);
            AttributeValueBounds bounds = valueBounds(text, rawValueStart, tagEnd, cursorIndex);
            if (cursorIndex >= rawValueStart && cursorIndex <= bounds.valueEnd) {
                return true;
            }
            pos = Math.max(bounds.rawEnd, rawValueStart + 1);
        }
        return false;
    }

    public static AttributeValueBounds valueBounds(String text, int rawValueStart, int limit, int cursorIndex) {
        if (rawValueStart >= limit) {
            return new AttributeValueBounds(rawValueStart, rawValueStart, rawValueStart, '\0');
        }
        char open = text.charAt(rawValueStart);
        if (open == '"' || open == '\'' || open == '{') {
            char close = open == '{' ? '}' : open;
            int valueStart = rawValueStart + 1;
            int rawEnd = findClosingValue(text, valueStart, limit, close);
            boolean closed = rawEnd < limit && text.charAt(rawEnd) == close;
            int rawValueEnd = closed ? rawEnd + 1 : rawEnd;
            int valueEnd = closed ? rawEnd : Math.max(valueStart, Math.min(rawEnd, cursorIndex));
            return new AttributeValueBounds(valueStart, valueEnd, rawValueEnd, closed ? '\0' : close);
        }

        int rawEnd = rawValueStart;
        while (rawEnd < limit) {
            char c = text.charAt(rawEnd);
            if (Character.isWhitespace(c) || c == '>' || c == '/') {
                break;
            }
            rawEnd++;
        }
        return new AttributeValueBounds(rawValueStart, rawEnd, rawEnd, '\0');
    }

    public static int findClosingValue(String text, int start, int limit, char close) {
        for (int i = start; i < limit; i++) {
            char c = text.charAt(i);
            if (c == close || c == '>' || c == '\n' || c == '\r') {
                return i;
            }
        }
        return limit;
    }

    @Nullable
    public static String getLineAt(String text, int cursorIndex) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorIndex);
        if (lineEnd < 0) lineEnd = text.length();
        if (lineStart >= lineEnd) return null;
        return text.substring(lineStart, lineEnd);
    }

    public static int indexOf(String text, char target, int start, int end) {
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == target) return i;
        }
        return -1;
    }

    public static int skipSpaces(String text, int start, int end) {
        int pos = start;
        while (pos < end && text.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    public static boolean isAttributeNameStart(char c) {
        return Character.isLetter(c) || c == '_' || c == ':';
    }

    public static boolean isAttributeNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ':' || c == '.';
    }

    public static class AttributeValueBounds {

        public final int valueStart;
        public final int valueEnd;
        public final int rawEnd;
        public final char missingTerminator;

        public AttributeValueBounds(int valueStart, int valueEnd, int rawEnd, char missingTerminator) {
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
            this.rawEnd = rawEnd;
            this.missingTerminator = missingTerminator;
        }
    }
}
