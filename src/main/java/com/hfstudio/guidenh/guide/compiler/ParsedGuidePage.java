package com.hfstudio.guidenh.guide.compiler;

import java.util.Objects;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistPoint;

import lombok.Getter;

public class ParsedGuidePage {

    @Getter
    private final String sourcePack;
    @Getter
    private final ResourceLocation id;
    @Getter
    private final String source;
    private volatile MdAstRoot astRoot;
    @Getter
    private final Frontmatter frontmatter;
    @Getter
    private final String language;
    private final @Nullable String parseFailureMessage;
    private final @Nullable UnistPoint parseFailureFrom;
    private final @Nullable UnistPoint parseFailureTo;

    @Deprecated
    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter) {
        this(sourcePack, id, source, astRoot, frontmatter, "en_us", null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language) {
        this(sourcePack, id, source, astRoot, frontmatter, language, null, null, null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language, @Nullable String parseFailureMessage) {
        this(sourcePack, id, source, astRoot, frontmatter, language, parseFailureMessage, null, null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language, @Nullable String parseFailureMessage,
        @Nullable UnistPoint parseFailureFrom, @Nullable UnistPoint parseFailureTo) {
        this.sourcePack = sourcePack;
        this.id = id;
        this.source = source;
        this.astRoot = astRoot;
        this.frontmatter = frontmatter;
        this.language = Objects.requireNonNull(language, "language");
        this.parseFailureMessage = parseFailureMessage;
        this.parseFailureFrom = parseFailureFrom;
        this.parseFailureTo = parseFailureTo;
    }

    public MdAstRoot getAstRoot() {
        MdAstRoot r = astRoot;
        if (r != null) {
            return r;
        }
        synchronized (this) {
            r = astRoot;
            if (r != null) {
                return r;
            }
            ParsedGuidePage full = PageCompiler.parse(sourcePack, language, id, source);
            astRoot = full.astRoot;
            return astRoot;
        }
    }

    public @Nullable String getParseFailureMessage() {
        return parseFailureMessage;
    }

    public @Nullable UnistPoint getParseFailureFrom() {
        return parseFailureFrom;
    }

    public @Nullable UnistPoint getParseFailureTo() {
        return parseFailureTo;
    }

    public boolean hasParseFailure() {
        return parseFailureMessage != null && !parseFailureMessage.isEmpty();
    }

    @Override
    public String toString() {
        if (id.getResourceDomain()
            .equals(sourcePack)) {
            return id.toString();
        } else {
            return id + " (from " + sourcePack + ")";
        }
    }
}
