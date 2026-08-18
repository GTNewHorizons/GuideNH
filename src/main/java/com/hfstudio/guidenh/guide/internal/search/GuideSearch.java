package com.hfstudio.guidenh.guide.internal.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.minecraft.util.ResourceLocation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageIds;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageTitleResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.libs.unist.UnistNode;

import cpw.mods.fml.common.Loader;
import lombok.Getter;

/**
 * Manages the persistent Lucene index for guide search.
 */
public class GuideSearch implements AutoCloseable {

    public static final long BACKGROUND_TIME_PER_TICK = TimeUnit.MILLISECONDS.toNanos(1);
    public static final long SEARCH_TIME_PER_TICK = TimeUnit.MILLISECONDS.toNanos(8);
    private static final int INDEX_SCHEMA_VERSION = 1;
    private static final String COMMIT_SCHEMA_VERSION = "guidenh.search.schema";
    private static final String COMMIT_FINGERPRINT = "guidenh.search.fingerprint";
    private static final long PUBLISH_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(250);
    private static final long SEARCH_PRIORITY_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final int MAX_INDEX_WORKERS = 4;

    private Directory directory;
    private final Analyzer analyzer;
    private IndexWriter indexWriter;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private final List<GuideIndexingTask> pendingTasks = new ArrayList<>();
    private final ExecutorService indexingExecutor;
    private final CompletionService<PageIndexDocument> completedDocuments;
    private Instant indexingStarted;
    private int pagesIndexed;
    private int inFlightDocuments;
    private long lastPublishedNanos;
    private long searchPriorityUntilNanos;
    @Getter
    private long indexRevision;
    private long buildGeneration;
    private final Set<String> warnedAboutLanguage = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> indexedLanguages = Collections.synchronizedSet(new HashSet<>());

    public GuideSearch() {
        analyzer = new LanguageSpecificAnalyzerWrapper();
        int workerCount = Math.clamp(
            Runtime.getRuntime()
                .availableProcessors() - 1,
            1,
            MAX_INDEX_WORKERS);
        indexingExecutor = Executors.newFixedThreadPool(workerCount, runnable -> {
            Thread thread = new Thread(runnable, "GuideNH Search Indexer");
            thread.setDaemon(true);
            return thread;
        });
        completedDocuments = new ExecutorCompletionService<>(indexingExecutor);
    }

    public void index(Guide guide) {
        if (indexWriter == null) {
            indexAll();
            return;
        }
        try {
            indexWriter.deleteDocuments(
                new Term(
                    IndexSchema.FIELD_GUIDE_ID,
                    guide.getId()
                        .toString()));
        } catch (IOException e) {
            GuideDebugLog.error("[GuideNH] [GuideSearch] Failed to delete all documents before re-indexing.", e);
        }

        if (pendingTasks.isEmpty()) {
            indexingStarted = Instant.now();
            pagesIndexed = 0;
        }
        pendingTasks.removeIf(
            t -> t.guide.getId()
                .equals(guide.getId()));
        pendingTasks.add(new GuideIndexingTask(guide, new ArrayDeque<>(guide.getPages())));
    }

    public void indexAll() {
        String fingerprint = fingerprint(GuideRegistry.getAll());
        cancelPendingWork();
        indexedLanguages.clear();
        warnedAboutLanguage.clear();

        try {
            closeIndex();
            directory = openDirectory(fingerprint);
            if (DirectoryReader.indexExists(directory)) {
                indexReader = DirectoryReader.open(directory);
                if (matchesFingerprint(indexReader, fingerprint)) {
                    indexWriter = openIndexWriter(IndexWriterConfig.OpenMode.APPEND);
                    indexSearcher = new IndexSearcher(indexReader);
                    indexedLanguages.addAll(Analyzers.MINECRAFT_TO_LUCENE_LANG.values());
                    indexRevision++;
                    GuideDebugLog.info("[GuideNH] [GuideSearch] Loaded persistent search index {}", fingerprint);
                    return;
                }
                indexReader.close();
                indexReader = null;
            }

            indexWriter = openIndexWriter(IndexWriterConfig.OpenMode.CREATE);
            indexWriter.setLiveCommitData(commitData(fingerprint).entrySet());
            indexWriter.commit();
            indexReader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(indexReader);
            indexRevision++;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to reset the guide search index.", e);
        }

        for (var guide : GuideRegistry.getAll()) {
            index(guide);
        }
    }

    public void processWork() {
        processWork(BACKGROUND_TIME_PER_TICK);
    }

    public void processWork(long budgetNanos) {
        if (indexWriter == null || !hasPendingWork()) {
            return;
        }

        long start = System.nanoTime();
        schedulePendingPages();
        boolean wroteDocuments = false;
        while (!isTimeElapsed(start, budgetNanos)) {
            Future<PageIndexDocument> completed = completedDocuments.poll();
            if (completed == null) {
                break;
            }
            PageIndexDocument pageDocument = receiveCompletedDocument(completed);
            if (pageDocument == null || pageDocument.generation() != buildGeneration) {
                continue;
            }
            inFlightDocuments--;
            writeDocument(pageDocument);
            wroteDocuments = true;
            schedulePendingPages();
        }

        boolean finished = !hasPendingWork();
        if (wroteDocuments && (finished || isPublishDue())) {
            publishIndex();
        }
        if (finished) {
            GuideDebugLog.info(
                "[GuideNH] [GuideSearch] Indexing of {} pages finished in {}",
                pagesIndexed,
                Duration.between(indexingStarted, Instant.now()));
        }
    }

    private boolean isTimeElapsed(long start, long budgetNanos) {
        return System.nanoTime() - start >= budgetNanos;
    }

    public boolean hasPendingWork() {
        return !pendingTasks.isEmpty() || inFlightDocuments > 0;
    }

    public boolean isSearchPriorityActive() {
        return System.nanoTime() < searchPriorityUntilNanos;
    }

    private void schedulePendingPages() {
        int maximumInFlight = MAX_INDEX_WORKERS * 2;
        while (inFlightDocuments < maximumInFlight && !pendingTasks.isEmpty()) {
            GuideIndexingTask task = pendingTasks.getFirst();
            var page = task.pendingPages()
                .pollFirst();
            if (page == null) {
                pendingTasks.removeFirst();
                continue;
            }
            long generation = buildGeneration;
            completedDocuments.submit(() -> createPageIndexDocument(task.guide(), page, generation));
            inFlightDocuments++;
        }
    }

    @Nullable
    private PageIndexDocument receiveCompletedDocument(Future<PageIndexDocument> completed) {
        try {
            return completed.get();
        } catch (Exception exception) {
            GuideDebugLog.error("[GuideNH] [GuideSearch] Failed to prepare a search document", exception);
            return null;
        }
    }

    @NonNull
    private PageIndexDocument createPageIndexDocument(Guide guide, ParsedGuidePage page, long generation) {
        try {
            Document document = createPageDocument(guide, page);
            return new PageIndexDocument(guide, page, document, generation);
        } catch (Throwable throwable) {
            GuideDebugLog
                .error("[GuideNH] [GuideSearch] Failed to prepare index document {}{}", guide, page, throwable);
            return new PageIndexDocument(guide, page, null, generation);
        }
    }

    private void writeDocument(PageIndexDocument pageDocument) {
        try {
            Document document = pageDocument.document();
            if (document == null) {
                return;
            }
            indexWriter.addDocument(document);
            String searchLanguage = document.get(IndexSchema.FIELD_SEARCH_LANG);
            if (searchLanguage != null) {
                indexedLanguages.add(searchLanguage);
            }
        } catch (IOException exception) {
            GuideDebugLog.error(
                "[GuideNH] [GuideSearch] Failed to index document {}{}",
                pageDocument.guide(),
                pageDocument.page(),
                exception);
        } finally {
            pagesIndexed++;
        }
    }

    private boolean isPublishDue() {
        return System.nanoTime() - lastPublishedNanos >= PUBLISH_INTERVAL_NANOS;
    }

    private void publishIndex() {
        try {
            indexWriter.commit();
            refreshIndexReader();
            lastPublishedNanos = System.nanoTime();
            indexRevision++;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to publish the guide search index.", exception);
        }
    }

    private void refreshIndexReader() throws IOException {
        var newReader = DirectoryReader.open(directory);
        var oldReader = indexReader;
        indexReader = newReader;
        indexSearcher = new IndexSearcher(newReader);
        if (oldReader != null) {
            oldReader.close();
        }
    }

    public List<SearchResult> searchGuide(String queryText, @Nullable Guide onlyFromGuide) {
        if (queryText.isEmpty()) {
            return List.of();
        }
        searchPriorityUntilNanos = System.nanoTime() + SEARCH_PRIORITY_NANOS;
        if (indexSearcher == null) {
            return List.of();
        }

        var searchLanguage = getLuceneLanguageFromMinecraft(LangUtil.getCurrentLanguage());
        var indexSearcher = this.indexSearcher;

        Query query;
        try {
            query = GuideQueryParser.parse(queryText, analyzer, indexedLanguages);
        } catch (Exception e) {
            GuideDebugLog.debug("[GuideNH] [GuideSearch] Failed to parse search query: '{}'", queryText, e);
            return List.of();
        }

        // Add an exact guide filter without changing the parsed query.
        if (onlyFromGuide != null) {
            query = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST)
                .add(
                    new TermQuery(
                        new Term(
                            IndexSchema.FIELD_GUIDE_ID,
                            onlyFromGuide.getId()
                                .toString())),
                    BooleanClause.Occur.FILTER)
                .build();
        }

        GuideDebugLog.debug("[GuideNH] [GuideSearch] Running GuideME search query: {}", query);

        TopDocs topDocs;
        try {
            topDocs = indexSearcher.search(query, 25);
        } catch (IOException e) {
            GuideDebugLog.error("[GuideNH] [GuideSearch] Failed to search for '{}'", queryText, e);
            return List.of();
        }

        var result = new ArrayList<SearchResult>(topDocs.scoreDocs.length);
        var highlighter = new Highlighter(new QueryScorer(query));
        try {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                var document = indexSearcher.doc(scoreDoc.doc);
                var guideId = new ResourceLocation(document.get(IndexSchema.FIELD_GUIDE_ID));
                var pageId = new ResourceLocation(document.get(IndexSchema.FIELD_PAGE_ID));

                var guide = GuideRegistry.getById(guideId);
                if (guide == null) {
                    GuideDebugLog.warn(
                        "[GuideNH] [GuideSearch] Search index produced guide id {} which couldn't be found.",
                        guideId);
                    continue;
                }

                var page = guide.getParsedPage(pageId);
                if (page == null) {
                    GuideDebugLog.warn(
                        "[GuideNH] [GuideSearch] Search index produced page {} in guide {}, which couldn't be found.",
                        pageId,
                        guideId);
                    continue;
                }

                String bestFragment = "";
                try {
                    bestFragment = highlighter.getBestFragment(
                        analyzer,
                        IndexSchema.getTextField(searchLanguage),
                        document.get(IndexSchema.FIELD_TEXT));
                    if (bestFragment == null) {
                        bestFragment = "";
                    }
                } catch (InvalidTokenOffsetsException e) {
                    GuideDebugLog.error("[GuideNH] [GuideSearch] Cannot determine text to highlight for result", e);
                }

                var pageTitle = document.get(IndexSchema.FIELD_TITLE);
                result.add(
                    new SearchResult(
                        guideId,
                        pageId,
                        pageTitle,
                        GuideSearchSnippetFormatter.format(bestFragment),
                        scoreDoc.score));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        result.sort((left, right) -> {
            int leftPriority = searchPriority(left.pageId());
            int rightPriority = searchPriority(right.pageId());
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
            int scoreCompare = Float.compare(right.score(), left.score());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return left.pageTitle()
                .compareToIgnoreCase(right.pageTitle());
        });
        return result;
    }

    private int searchPriority(ResourceLocation pageId) {
        if (MediaWikiPageIds.isCategoryPage(pageId)) {
            return 2;
        }
        if (MediaWikiPageIds.isSpecialPage(pageId)) {
            return 3;
        }
        return 1;
    }

    @Nullable
    private Document createPageDocument(Guide guide, ParsedGuidePage page) {
        if (MediaWikiPageIds.isSpecialPage(page.getId())) {
            return null;
        }
        var pageText = getSearchableText(guide, page);
        var pageTitle = getPageTitle(guide, page);

        var searchLang = getLuceneLanguageFromMinecraft(page.getLanguage());

        var doc = new Document();
        doc.add(
            new StringField(
                IndexSchema.FIELD_GUIDE_ID,
                guide.getId()
                    .toString(),
                Field.Store.YES));
        doc.add(
            new StoredField(
                IndexSchema.FIELD_PAGE_ID,
                page.getId()
                    .toString()));
        doc.add(new StoredField(IndexSchema.FIELD_LANG, page.getLanguage()));
        doc.add(new StoredField(IndexSchema.FIELD_SEARCH_LANG, searchLang));

        // Keep the original strings for result display and Lucene highlighter output.
        doc.add(new StoredField(IndexSchema.FIELD_TITLE, pageTitle));
        doc.add(new StoredField(IndexSchema.FIELD_TEXT, pageText));

        doc.add(new TextField(IndexSchema.getTitleField(searchLang), pageTitle, Field.Store.NO));
        doc.add(new TextField(IndexSchema.getTextField(searchLang), pageText, Field.Store.NO));
        return doc;
    }

    private String getLuceneLanguageFromMinecraft(String language) {
        var luceneLang = Analyzers.MINECRAFT_TO_LUCENE_LANG.get(language);
        if (luceneLang == null) {
            if (warnedAboutLanguage.add(language)) {
                GuideDebugLog.warn(
                    "[GuideNH] [GuideSearch] Minecraft language '{}' is unknown, so search falls back to english.",
                    language);
            }
            return Analyzers.LANG_ENGLISH;
        }
        return luceneLang;
    }

    public static String getPageTitle(Guide guide, ParsedGuidePage page) {
        return MediaWikiPageTitleResolver.resolvePageTitle(guide, page);
    }

    public static String getSearchableText(Guide guide, ParsedGuidePage page) {
        var searchableText = new StringBuilder();

        var sink = new IndexingSink() {

            @Override
            public void appendText(UnistNode parent, String text) {
                searchableText.append(text);
            }

            @Override
            public void appendBreak() {
                searchableText.append('\n');
            }
        };
        new PageIndexer(guide, guide.getExtensions(), page.getId()).index(page.getAstRoot(), sink);
        return searchableText.toString();
    }

    private Directory openDirectory(String fingerprint) throws IOException {
        Path indexDirectory = Loader.instance()
            .getConfigDir()
            .toPath()
            .resolve("guidenh")
            .resolve("search-index")
            .resolve("v" + INDEX_SCHEMA_VERSION)
            .resolve(fingerprint);
        Files.createDirectories(indexDirectory);
        return FSDirectory.open(indexDirectory);
    }

    private IndexWriter openIndexWriter(IndexWriterConfig.OpenMode openMode) throws IOException {
        ClassLoader previousClassLoader = Thread.currentThread()
            .getContextClassLoader();
        Thread.currentThread()
            .setContextClassLoader(GuideSearch.class.getClassLoader());
        try {
            return new IndexWriter(directory, new IndexWriterConfig(analyzer).setOpenMode(openMode));
        } finally {
            Thread.currentThread()
                .setContextClassLoader(previousClassLoader);
        }
    }

    private boolean matchesFingerprint(IndexReader reader, String fingerprint) throws IOException {
        if (!(reader instanceof DirectoryReader directoryReader)) {
            return false;
        }
        Map<String, String> commitData = directoryReader.getIndexCommit()
            .getUserData();
        return Integer.toString(INDEX_SCHEMA_VERSION)
            .equals(commitData.get(COMMIT_SCHEMA_VERSION)) && fingerprint.equals(commitData.get(COMMIT_FINGERPRINT));
    }

    private Map<String, String> commitData(String fingerprint) {
        return Map.of(COMMIT_SCHEMA_VERSION, Integer.toString(INDEX_SCHEMA_VERSION), COMMIT_FINGERPRINT, fingerprint);
    }

    private String fingerprint(Iterable<? extends Guide> guides) {
        MessageDigest digest = createFingerprintDigest();
        updateFingerprint(digest, "schema=" + INDEX_SCHEMA_VERSION);
        var sortedGuides = new ArrayList<Guide>();
        for (Guide guide : guides) {
            sortedGuides.add(guide);
        }
        sortedGuides.sort(
            Comparator.comparing(
                guide -> guide.getId()
                    .toString()));
        for (Guide guide : sortedGuides) {
            updateFingerprint(
                digest,
                guide.getId()
                    .toString());
            var pages = new ArrayList<>(guide.getPages());
            pages.sort(
                Comparator.comparing(
                    page -> page.getId()
                        .toString()));
            for (var page : pages) {
                updateFingerprint(
                    digest,
                    page.getId()
                        .toString());
                updateFingerprint(digest, page.getLanguage());
                updateFingerprint(digest, page.getSource());
            }
        }
        return HexFormat.of()
            .formatHex(digest.digest());
    }

    private MessageDigest createFingerprintDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void updateFingerprint(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private void cancelPendingWork() {
        buildGeneration++;
        pendingTasks.clear();
        inFlightDocuments = 0;
    }

    private void closeIndex() throws IOException {
        IOException failure = null;
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException exception) {
                failure = exception;
            } finally {
                indexWriter = null;
            }
        }
        if (indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException exception) {
                if (failure != null) {
                    failure.addSuppressed(exception);
                } else {
                    failure = exception;
                }
            } finally {
                indexReader = null;
                indexSearcher = null;
            }
        }
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException exception) {
                if (failure != null) {
                    failure.addSuppressed(exception);
                } else {
                    failure = exception;
                }
            } finally {
                directory = null;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void close() throws IOException {
        cancelPendingWork();
        indexingExecutor.shutdownNow();
        closeIndex();
    }

    @Desugar
    public record GuideIndexingTask(Guide guide, Deque<ParsedGuidePage> pendingPages) {}

    @Desugar
    public record PageIndexDocument(Guide guide, ParsedGuidePage page, Document document, long generation) {}

    @Desugar
    public record SearchResult(ResourceLocation guideId, ResourceLocation pageId, String pageTitle, LytFlowContent text,
        float score) {

        public SearchResult {
            Objects.requireNonNull(guideId, "guideId");
            Objects.requireNonNull(pageId, "pageId");
            Objects.requireNonNull(pageTitle, "pageTitle");
            Objects.requireNonNull(text, "text");
        }
    }
}
