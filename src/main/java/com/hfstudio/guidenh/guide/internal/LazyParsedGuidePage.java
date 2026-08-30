package com.hfstudio.guidenh.guide.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistPoint;

/**
 * Parsed page shell that keeps frontmatter resident while deferring Markdown source and AST loading.
 */
public final class LazyParsedGuidePage extends ParsedGuidePage {

    private static final int MAX_RESIDENT_PAGES = 48;
    private static final Map<LazyParsedGuidePage, Boolean> RESIDENT_PAGES = new LinkedHashMap<>(
        MAX_RESIDENT_PAGES,
        0.75f,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<LazyParsedGuidePage, Boolean> eldest) {
            if (size() <= MAX_RESIDENT_PAGES) {
                return false;
            }
            eldest.getKey()
                .discardLoadedContent();
            return true;
        }
    };

    private final Supplier<String> sourceLoader;
    private final @Nullable String sourceFingerprint;
    private volatile String loadedSource;
    private volatile MdAstRoot loadedAst;

    public LazyParsedGuidePage(String sourcePack, ResourceLocation id, Frontmatter frontmatter, String language,
        @Nullable String parseFailureMessage, @Nullable UnistPoint parseFailureFrom,
        @Nullable UnistPoint parseFailureTo, Supplier<String> sourceLoader) {
        this(
            sourcePack,
            id,
            frontmatter,
            language,
            parseFailureMessage,
            parseFailureFrom,
            parseFailureTo,
            sourceLoader,
            null);
    }

    public LazyParsedGuidePage(String sourcePack, ResourceLocation id, Frontmatter frontmatter, String language,
        @Nullable String parseFailureMessage, @Nullable UnistPoint parseFailureFrom,
        @Nullable UnistPoint parseFailureTo, Supplier<String> sourceLoader, @Nullable String sourceFingerprint) {
        super(sourcePack, id, "", null, frontmatter, language, parseFailureMessage, parseFailureFrom, parseFailureTo);
        this.sourceLoader = Objects.requireNonNull(sourceLoader, "sourceLoader");
        this.sourceFingerprint = sourceFingerprint;
    }

    @Override
    public String getSource() {
        String source = loadedSource;
        if (source != null) {
            markResident();
            return source;
        }
        synchronized (this) {
            source = loadedSource;
            if (source == null) {
                source = sourceLoader.get();
                if (source == null) {
                    source = "";
                }
                loadedSource = source;
            }
        }
        markResident();
        return source;
    }

    @Override
    public MdAstRoot getAstRoot() {
        MdAstRoot ast = loadedAst;
        if (ast != null) {
            markResident();
            return ast;
        }
        String source = getSource();
        synchronized (this) {
            ast = loadedAst;
            if (ast == null) {
                ParsedGuidePage parsed = PageCompiler.parse(getSourcePack(), getLanguage(), getId(), source);
                ast = parsed.getAstRoot();
                loadedAst = ast;
            }
        }
        markResident();
        return ast;
    }

    @Override
    public String getSourceFingerprint() {
        return sourceFingerprint != null ? sourceFingerprint : super.getSourceFingerprint();
    }

    public static void clearResidentPages() {
        synchronized (RESIDENT_PAGES) {
            for (LazyParsedGuidePage page : RESIDENT_PAGES.keySet()) {
                page.discardLoadedContent();
            }
            RESIDENT_PAGES.clear();
        }
    }

    private void markResident() {
        synchronized (RESIDENT_PAGES) {
            RESIDENT_PAGES.put(this, Boolean.TRUE);
        }
    }

    private synchronized void discardLoadedContent() {
        loadedAst = null;
        loadedSource = null;
    }
}
