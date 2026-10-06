package com.hfstudio.guidenh.guide.internal.headless;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytMermaidCanvas;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.internal.GuideBookmarkState;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchPage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchResultDocumentBuilder;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchSnippetFormatter;
import com.hfstudio.guidenh.guide.internal.settings.GuideSettingsDocumentBuilder;
import com.hfstudio.guidenh.guide.internal.settings.GuideSettingsPage;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.FontProvider;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.LayoutTreeSerializer;
import com.hfstudio.guidenh.guide.layout.RustFontMetrics;
import com.hfstudio.guidenh.guide.layout.SystemFontProvider;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Core orchestration service for "page → long screenshot PNG + optional bounds
 * JSON / debug overlay".
 *
 * <p>
 * Called inside the real Minecraft client (command or startup hook) after
 * fonts, resources, and the Guide registry are ready (post-completeInit).
 * Does <em>not</em> depend on {@code Minecraft.theWorld / thePlayer / currentScreen}.
 *
 * <p>
 * Layout is performed at {@code guiScale = 1}; the visual scale is fixed at
 * {@code 1.0} (no zoom).
 */
public final class RenderPageService {

    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd_HHmmss", Locale.ROOT);

    /** Route id for the synthetic search-results page ({@code -Dguidenh.renderpage.route}). */
    private static final String ROUTE_HOME_SEARCH = "home_search";

    /** Route id for the synthetic reader-settings page ({@code -Dguidenh.renderpage.route}). */
    private static final String ROUTE_SETTINGS = "settings";

    /**
     * Horizontal document edge padding (px per side) applied by the Rust layout
     * engine before any content: {@code CONTENT_PAD = 14.0} in
     * {@code src/rust/layout-engine/src/layout.rs}, which places content at
     * {@code x = 14} with width {@code avail_width - 2 * 14}. A 900 px headless
     * page thus lays document content out at 900 - 28 = 872 px wide
     * (bounds-verified: search-result row content x=14 w=872). The search-row
     * column width must use the same metric; clipping snippet/title/path
     * against {@code req.width()} (900) would over-budget the real 872 px
     * column by 28 px.
     */
    private static final int DOCUMENT_CONTENT_EDGE_PAD = 14;

    /** Six cycling colours for overlay borders/labels, keyed by depth % 6. */
    private static final int[] OVERLAY_FILL_COLORS = { 0x44FF0000, 0x4400FF00, 0x440000FF, 0x44FFFF00, 0x44FF00FF,
        0x4400FFFF };
    private static final int[] OVERLAY_BORDER_COLORS = { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF,
        0xFF00FFFF };

    private RenderPageService() {}

    // Data types.

    /**
     * @param guideId          host guide identifier (always required, used as resource context)
     * @param pageId           registered page id (non-null → registered-page path)
     * @param mdFile           arbitrary markdown file (non-null → raw-md path)
     * @param language         language code, e.g. "en_us" or "zh_cn"
     * @param width            layout width in document units (GUI pixels)
     * @param outDir           output directory for generated files
     * @param emitBoundsJson   if true, write a block-bounds JSON sidecar
     * @param emitDebugOverlay if true, write a debug overlay PNG
     * @param scale            render pixel-density multiplier (1-4; 1 = 1×, no scaling)
     * @param chrome           if true, append the GuideNavBar chrome pass to the
     *                         headless render. The nav
     *                         bar occupies the left {@link #navBarWidth(int)}
     *                         logical px and the document is shifted right; the
     *                         bounds JSON stays in document coordinates.
     * @param route            synthetic UI-route id (non-null → route path).
     *                         Supported values: {@code "home_search"} (search-results page)
     *                         and {@code "settings"} (reader-settings page, no query needed);
     *                         mutually exclusive with pageId/mdFile.
     * @param query            search query, required when {@code route = "home_search"}
     */
    public record RenderPageRequest(String guideId, String pageId, Path mdFile, String language, int width, Path outDir,
        boolean emitBoundsJson, boolean emitDebugOverlay, int scale, boolean chrome, String route, String query) {

        /** Legacy 9-arg construction (in-game command path); chrome defaults off. */
        public RenderPageRequest(String guideId, String pageId, Path mdFile, String language, int width, Path outDir,
            boolean emitBoundsJson, boolean emitDebugOverlay, int scale) {
            this(
                guideId,
                pageId,
                mdFile,
                language,
                width,
                outDir,
                emitBoundsJson,
                emitDebugOverlay,
                scale,
                false,
                null,
                null);
        }
    }

    /**
     * @param pngPath        path of the written PNG
     * @param boundsJsonPath path of the bounds JSON (null when not emitted)
     * @param widthPx        actual image width in pixels
     * @param heightPx       actual image height in pixels
     * @param blockCount     total number of LytBlock instances in the document
     */
    public record RenderPageResult(Path pngPath, Path boundsJsonPath, int widthPx, int heightPx, int blockCount) {}

    /**
     * Checked exception that wraps all failures inside {@link #render}.
     * The {@link Stage} indicates which phase the error occurred in.
     */
    public static final class RenderPageException extends Exception {

        public enum Stage {
            COMPILE,
            LAYOUT,
            RENDER,
            IO
        }

        private final Stage stage;

        public RenderPageException(Stage stage, String message) {
            super(message);
            this.stage = stage;
        }

        public RenderPageException(Stage stage, String message, Throwable cause) {
            super(message, cause);
            this.stage = stage;
        }

        public Stage getStage() {
            return stage;
        }
    }

    // Public API.

    /**
     * Force-initialise the Rust font engine if not already done.
     *
     * <p>
     * Equivalent to the font-initialisation portion of
     * {@code GuideScreen.ensureLayout()}: checks {@link LayoutBridge#getFontHandle()},
     * loads system CJK font data via {@link SystemFontProvider}, and calls
     * {@link LayoutBridge#init(byte[], String)} followed by
     * {@link LayoutBridge#setFontHandle(long)}.
     *
     * <p>
     * Idempotent: subsequent calls are no-ops once the font handle is non-zero.
     */
    public static void ensureFontEngineReady() {
        if (LayoutBridge.getFontHandle() == 0) {
            var fontProvider = new SystemFontProvider();
            byte[] fontData = fontProvider.getFontData("zh_CN");
            GuideDebugLog.warnAlways(
                "RenderPageService: initializing Rust font system from {} ({} bytes)",
                fontProvider.getFontPath(),
                fontData.length);
            long handle = LayoutBridge.init(fontData, "zh_CN");
            LayoutBridge.setFontHandle(handle);
            loadFallbackSymbolFont(fontProvider, handle);
        }
    }

    /**
     * Best-effort fallback symbol font registration (seguisym.ttf covers the
     * callout icons ⓘ ✦ ➤ ⚠ ☢ that msyh.ttc lacks). Runs once right after
     * font init; empty data and stale native libs are skipped/ignored.
     */
    private static void loadFallbackSymbolFont(FontProvider fontProvider, long handle) {
        if (handle == 0) return;
        byte[] fallbackData = fontProvider.getFallbackFontData("zh_CN");
        if (fallbackData.length == 0) return;
        try {
            LayoutBridge.loadFallbackFont(handle, fallbackData);
        } catch (UnsatisfiedLinkError e) {
            GuideDebugLog
                .warnAlways("RenderPageService: loadFallbackFont unavailable (stale native lib?): {}", e.getMessage());
        }
    }

    /**
     * Orchestrate the full render pipeline.
     *
     * <ol>
     * <li>Ensure font engine ready</li>
     * <li>Compile the page (registered-page or raw-md path)</li>
     * <li>Layout the document at the requested width</li>
     * <li>Collect render primitives</li>
     * <li>Render to offscreen FBO (tiled if necessary)</li>
     * <li>Write PNG (with collision-safe naming)</li>
     * <li>Optionally write bounds JSON</li>
     * <li>Optionally write debug-overlay PNG</li>
     * </ol>
     *
     * <p>
     * <b>Intentional deviation from {@code GuideScreen.renderDocument}:</b>
     * This method fixes {@code visualScale = 1.0f} (in the layout context) and
     * {@code zoom = 1.0f} (in the render context) to produce a full-resolution
     * screenshot. The screenshot is defined as the geometric layout at 1.0×
     * scale; it does <em>not</em> simulate the user's current zoom or visual
     * scale. {@code GuideScreen.renderDocument} applies the user's dynamic
     * {@code currentZoom} and {@code visualScrollY} instead.
     */
    public static RenderPageResult render(RenderPageRequest req) throws RenderPageException {
        // Step 1: font engine.
        ensureFontEngineReady();

        // Step 2: resolve the host guide.
        ResourceLocation guideId = new ResourceLocation(req.guideId());
        MutableGuide guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Guide not found: " + req.guideId());
        }

        // Step 3: compile.
        GuidePage compiledPage;
        try {
            if (req.route() != null) {
                if (ROUTE_SETTINGS.equals(req.route())) {
                    compiledPage = compileSettingsRoute(guide, req);
                } else if (ROUTE_HOME_SEARCH.equals(req.route())) {
                    compiledPage = compileSearchRoute(guide, req);
                } else {
                    throw new RenderPageException(
                        RenderPageException.Stage.COMPILE,
                        "Unsupported route: " + req
                            .route() + " (supported: " + ROUTE_HOME_SEARCH + ", " + ROUTE_SETTINGS + ")");
                }
            } else if (req.pageId() != null && !req.pageId()
                .isEmpty()) {
                    compiledPage = compileRegisteredPage(guide, req);
                } else if (req.mdFile() != null) {
                    compiledPage = compileMdFile(guide, req);
                } else {
                    throw new RenderPageException(
                        RenderPageException.Stage.COMPILE,
                        "Either pageId, mdFile or route must be provided");
                }
        } catch (RenderPageException e) {
            throw e;
        } catch (Exception e) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Compilation failed", e);
        }

        LytDocument document = compiledPage.document();

        // Step 3a: mount the document (dispatch MOUNT events for SceneScript etc.).
        String mountPageId = compiledPage.id()
            .toString();
        LytHost lytHost = ClientProxy.getLytHost();
        try {
            lytHost.setCurrentPageId(mountPageId);
            lytHost.setCurrentPageCollection(guide);
            lytHost.mountDocument(document);

            // Drive async scripts (SceneScript: doInit → doAwaitSnbt → doBuild)
            // to convergence using the host's step mechanism.
            long deadline = System.nanoTime() + 10_000_000_000L; // 10 seconds
            while (lytHost.hasWork() && System.nanoTime() < deadline) {
                lytHost.step(deadline);
            }
            if (lytHost.hasWork()) {
                GuideDebugLog.warnAlways(
                    "RenderPageService: page {} mount timed out after 10s, {} tasks still pending",
                    mountPageId,
                    lytHost.pendingTaskCount());
            }
        } catch (Exception e) {
            throw new RenderPageException(RenderPageException.Stage.LAYOUT, "Mount failed for page " + mountPageId, e);
        }

        // Steps 4 and 5: layout and primitive collection.
        // Headless guiscale injection (-Dguidenh.renderpage.guiscale=0|1|2|3|4,
        // default 4 when absent): temporarily set gameSettings.guiScale so that
        // DisplayScale.scaleFactor() (whose cache key includes guiScale)
        // drives the Rust glyph rasterization scale like the live client's
        // auto render_scale. The headless render baseline is guiscale=4, so an
        // absent property injects 4 by default; without injection, headless
        // renders run at scaleFactor=1 (11 ppem glyphs) and can never cover
        // the capacity surface that breaks at render_scale=4. Explicit 0
        // disables the injection (layout stays byte-identical). The property
        // is read once, the value is applied before layout and restored
        // afterwards.
        int contentHeight;
        List<GuideRenderPrimitive> primitives;
        VanillaRenderContext renderCtx;
        Minecraft mc = Minecraft.getMinecraft();
        int guiscaleInjection = parseGuiscaleInjection();
        int previousGuiScale = 0;
        if (guiscaleInjection > 0 && mc != null) {
            previousGuiScale = mc.gameSettings.guiScale;
            mc.gameSettings.guiScale = guiscaleInjection;
            // DisplayScale override: bypasses the ScaledResolution geometric
            // cap (sf = min(guiScale, floor(dw/320), floor(dh/240))) that
            // otherwise clamps effective scaleFactor to 2 on the 854×480
            // headless window even with guiScale=4 injected. Cleared in the
            // finally block below.
            DisplayScale.setInjectedScaleFactor(guiscaleInjection);
            GuideDebugLog.infoAlways(
                "RenderPageService: guiscale injection active: {} (layout render_scale simulation; "
                    + "restored after layout/collect)",
                guiscaleInjection);
        }
        try {
            try {
                var layoutCtx = new LayoutContext(new RustFontMetrics()).withVisualScale(1.0f);
                document.updateLayout(layoutCtx, req.width());
                contentHeight = document.getContentHeight();
                if (contentHeight <= 0) {
                    throw new RenderPageException(
                        RenderPageException.Stage.LAYOUT,
                        "Document content height must be positive, got: " + contentHeight);
                }
            } catch (Exception e) {
                throw new RenderPageException(RenderPageException.Stage.LAYOUT, "Layout failed", e);
            }

            try {
                var fullViewport = new LytRect(0, 0, req.width(), contentHeight);
                renderCtx = new VanillaRenderContext(fullViewport, contentHeight);
                renderCtx.setDocumentOrigin(0, 0);
                renderCtx.setScrollOffsetY(0);
                renderCtx.setPreciseScrollOffsetY(0);
                renderCtx.setZoom(1.0f);
                renderCtx.setScreenViewport(fullViewport);

                var pc = new PrimitiveCollector(fullViewport, renderCtx);
                applyMermaidInjection(document);
                pc.collectFrom(document);
                primitives = pc.result();
                // Headless toolbar-title injection (-Dguidenh.renderpage.title=true):
                // overlay the page-title glyph run collected through the drawPageTitle
                // equivalent path on top of the document primitives. Absent or any
                // non-"true" value is a strict no-op; the primitive list stays
                // byte-identical, so the default render output is unchanged.
                if (isPageTitleInjectionEnabled()) {
                    List<GuideRenderPrimitive> titlePrims = collectToolbarTitlePrimitives(req, guide, compiledPage);
                    if (!titlePrims.isEmpty()) {
                        List<GuideRenderPrimitive> merged = new ArrayList<>(primitives.size() + titlePrims.size());
                        merged.addAll(primitives);
                        merged.addAll(titlePrims);
                        primitives = merged;
                    }
                }
            } catch (Exception e) {
                throw new RenderPageException(RenderPageException.Stage.RENDER, "Primitive collection failed", e);
            }
        } finally {
            if (guiscaleInjection > 0 && mc != null) {
                mc.gameSettings.guiScale = previousGuiScale;
                // Clear the DisplayScale override exactly where the guiscale
                // injection window ends (same condition as the injection).
                DisplayScale.setInjectedScaleFactor(0);
            }
        }

        // Step 6: render.
        int scale = req.scale();
        int renderedWidth = req.width();
        BufferedImage image;
        try {
            if (req.chrome()) {
                // Chrome pass: the document renders byte-identically to the
                // chrome=false path (own renderAll call), and the GuideNavBar
                // renders in a second offscreen pass at the left. The two are
                // composited side by side (nav left, document right), mirroring
                // the real GuideScreen layout (nav sidebar + content area).
                BufferedImage docImage = DocumentOffscreenFramebuffer
                    .renderAll(primitives, renderCtx, req.width(), contentHeight, 0x121216, scale);
                List<GuideRenderPrimitive> navPrims = new ArrayList<>();
                VanillaRenderContext navCtx = collectNavBarPrimitives(
                    req,
                    guide,
                    compiledPage,
                    contentHeight,
                    navPrims);
                int navW = navBarWidth(req.width());
                BufferedImage navImage = DocumentOffscreenFramebuffer
                    .renderAll(navPrims, navCtx, navW, contentHeight, 0x121216, scale);
                image = composeChrome(docImage, navImage, navW * scale, 0x121216);
                renderedWidth = req.width() + navW;
                GuideDebugLog.infoAlways(
                    "RenderPageService: chrome pass composed {} nav primitives into {}x{} output "
                        + "(nav width {} logical px, scale {})",
                    navPrims.size(),
                    image.getWidth(),
                    image.getHeight(),
                    navW,
                    scale);
            } else {
                image = DocumentOffscreenFramebuffer
                    .renderAll(primitives, renderCtx, req.width(), contentHeight, 0x121216, scale);
            }
        } catch (Exception e) {
            throw new RenderPageException(RenderPageException.Stage.RENDER, "Offscreen rendering failed", e);
        } finally {
            // Reset the document origin as required by DocumentOffscreenFramebuffer's contract
            renderCtx.setDocumentOrigin(0, 0);
        }

        // Step 7: write the PNG.
        Path pngPath;
        try {
            Files.createDirectories(req.outDir());
            String baseName = buildBaseName(req);
            pngPath = resolveTargetPath(req.outDir(), baseName, "png");
            ImageIO.write(image, "png", pngPath.toFile());
            GuideDebugLog
                .infoAlways("RenderPageService: wrote PNG {} ({}x{})", pngPath, image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new RenderPageException(RenderPageException.Stage.IO, "Failed to write PNG", e);
        }

        // Step 8: bounds JSON (optional).
        Path boundsJsonPath = null;
        if (req.emitBoundsJson()) {
            try {
                boundsJsonPath = resolveTargetPath(req.outDir(), buildBaseName(req), "json");
                writeBoundsJson(document, boundsJsonPath);
                GuideDebugLog.infoAlways("RenderPageService: wrote bounds JSON {}", boundsJsonPath);
            } catch (IOException e) {
                throw new RenderPageException(RenderPageException.Stage.IO, "Failed to write bounds JSON", e);
            }
        }

        // Step 9: debug overlay (optional).
        if (req.emitDebugOverlay()) {
            try {
                Path overlayPath = req.outDir()
                    .resolve(buildBaseName(req) + "_overlay.png");
                drawDebugOverlay(image, document, overlayPath, scale);
                GuideDebugLog.infoAlways("RenderPageService: wrote overlay PNG {}", overlayPath);
            } catch (IOException e) {
                throw new RenderPageException(RenderPageException.Stage.IO, "Failed to write overlay PNG", e);
            }
        }

        // Step 10: unmount the document from LytHost so the static host does not leak it.
        try {
            // mountDocument(null) detaches the current doc (setLive(false)) and clears
            // the task queue. LytHost has no explicit unmount/release method beyond this.
            lytHost.mountDocument(null);
        } catch (Exception e) {
            GuideDebugLog.warnAlways("RenderPageService: cleanup unmount failed for page {}", mountPageId, e);
        }

        int blockCount = countBlocks(document);
        return new RenderPageResult(pngPath, boundsJsonPath, renderedWidth * scale, contentHeight * scale, blockCount);
    }

    // Compilation helpers.

    private static GuidePage compileRegisteredPage(MutableGuide guide, RenderPageRequest req)
        throws RenderPageException {
        ResourceLocation pageId = new ResourceLocation(req.pageId());
        ParsedGuidePage parsed = guide.getParsedPage(pageId);
        if (parsed == null) {
            throw new RenderPageException(
                RenderPageException.Stage.COMPILE,
                buildPageNotFoundMessage(guide, req.pageId()));
        }
        try {
            return PageCompiler.compile(guide, guide.getExtensions(), parsed);
        } catch (Exception e) {
            throw new RenderPageException(
                RenderPageException.Stage.COMPILE,
                "Failed to compile registered page " + req.pageId(),
                e);
        }
    }

    private static GuidePage compileMdFile(MutableGuide guide, RenderPageRequest req) throws RenderPageException {
        Path mdFile = req.mdFile();
        if (!Files.isRegularFile(mdFile)) {
            throw new RenderPageException(
                RenderPageException.Stage.COMPILE,
                "mdFile does not exist or is not a regular file: " + mdFile);
        }
        String content;
        try {
            content = Files.readString(mdFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Failed to read mdFile: " + mdFile, e);
        }

        String fileName = mdFile.getFileName()
            .toString();
        if (fileName.endsWith(".md")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        }
        // Replace characters invalid in ResourceLocation path
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        ResourceLocation syntheticId = new ResourceLocation(guide.getDefaultNamespace(), safeName);

        String sourcePack = guide.getDefaultNamespace();
        try {
            ParsedGuidePage parsed = PageCompiler.parse(sourcePack, req.language(), syntheticId, content);
            return PageCompiler.compile(guide, guide.getExtensions(), parsed);
        } catch (Exception e) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Failed to compile mdFile " + mdFile, e);
        }
    }

    // Route compilation helpers (synthetic UI routes).

    /**
     * Compile the {@code settings} route: build the reader-settings document with
     * {@link GuideSettingsDocumentBuilder#buildDocument()}, the exact same single
     * builder the live screen uses ({@code GuideScreen.rebuildSettingsDocument} /
     * {@code GuideScreen} line 2405), so the headless render shares one source of
     * truth with the live page. Unlike the search route (which approximates its
     * rows headlessly), the settings route adds no headless-side reconstruction.
     * The page is wrapped with the synthetic id {@link GuideSettingsPage#PAGE_ID}
     * ({@code guidenh:settings}) so the mount / chrome / naming pipeline can reuse
     * the registered-page path unchanged. No query parameter is needed.
     */
    private static GuidePage compileSettingsRoute(MutableGuide guide, RenderPageRequest req) {
        LytDocument document = GuideSettingsDocumentBuilder.buildDocument();
        return new GuidePage(guide.getDefaultNamespace(), GuideSettingsPage.PAGE_ID, document);
    }

    /**
     * Compile the {@code home_search} route: run a real guide search
     * ({@link GuideME#getSearch()} → {@link GuideSearch#searchGuide(String, Guide)}),
     * map every hit into a {@link GuideSearchResultDocumentBuilder.SearchPageResult}
     * and build the search-results document exactly like the live screen
     * ({@code GuideScreen.buildSearchDocument}). The page is wrapped with the
     * synthetic id {@link GuideSearchPage#PAGE_ID} ({@code guidenh:search}) so the
     * mount / chrome / naming pipeline can reuse the registered-page path unchanged.
     *
     * <p>
     * Headless side builds the {@code SearchPageResult} rows itself instead of
     * reaching into {@code GuideScreen}'s private helpers. Title/path clipping is
     * approximated from {@code req.width()} using {@link FontRenderer} string
     * metrics; the snippet is clipped to at most two {@code textColumnWidth} lines
     * plus an ellipsis using {@link GuideText#measureWidth} (the same Rust metric
     * the headless render uses), mirroring {@code GuideScreen.clipSnippetForWidth}.
     */
    private static GuidePage compileSearchRoute(MutableGuide guide, RenderPageRequest req) {
        String query = req.query() == null ? "" : req.query();
        var results = new ArrayList<GuideSearchResultDocumentBuilder.SearchPageResult>();
        String normalizedQuery = GuideSearchPage.normalizeQuery(query);
        try {
            for (var result : GuideME.getSearch()
                .searchGuide(normalizedQuery, null)) {
                results.add(buildSearchRouteResult(req, result));
            }
        } catch (Throwable t) {
            // Mirrors GuideScreen.buildSearchDocument: a failed search degrades to
            // the empty state instead of failing the whole render.
            GuideDebugLog.warnAlways("Search failed", t);
        }
        LytDocument document = GuideSearchResultDocumentBuilder
            .buildDocument(query, results, GuidebookText.SearchNoQuery.text(), GuidebookText.SearchNoResults.text());
        return new GuidePage(guide.getDefaultNamespace(), GuideSearchPage.PAGE_ID, document);
    }

    /**
     * Map one {@link GuideSearch.SearchResult} into a
     * {@link GuideSearchResultDocumentBuilder.SearchPageResult}, mirroring the
     * live screen's row structure but approximating the text-column widths
     * from {@code req.width()} with the Rust document-edge inset deducted:
     * the headless document is laid out at the full page width with content
     * starting 14 px in on each side (the layout engine's {@code CONTENT_PAD}),
     * so a 900 px page renders row content at 872 px, instead of the live panel
     * minus nav reservations.
     */
    private static GuideSearchResultDocumentBuilder.SearchPageResult buildSearchRouteResult(RenderPageRequest req,
        GuideSearch.SearchResult result) {
        GuidePageIcon icon = null;
        var navNode = GuideRegistry.getMergedNavigationTree()
            .getNodeById(result.pageId());
        if (navNode != null) {
            icon = navNode.icon();
        }
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc != null ? mc.fontRenderer : null;
        int iconGap = icon != null ? GuideScreen.SEARCH_RESULT_ICON_AND_GAP : 0;
        // Column budget mirrors the rendered row-content width: the Rust layout
        // insets the whole document by DOCUMENT_CONTENT_EDGE_PAD per side
        // (the layout engine's CONTENT_PAD), so a 900 px page renders content at 872 px;
        // an icon row further deducts SEARCH_RESULT_ICON_AND_GAP on the same
        // basis as GuideScreen.getSearchTextColumnWidth.
        int textColumnWidth = Math.max(80, req.width() - DOCUMENT_CONTENT_EDGE_PAD * 2 - iconGap);
        int pathWidth = Math.max(192, textColumnWidth / 3);
        int titleWidth = Math.max(60, textColumnWidth - pathWidth - GuideScreen.SEARCH_RESULT_TITLE_GAP);

        String rawTitle = result.pageTitle()
            .isEmpty()
                ? result.pageId()
                    .toString()
                : result.pageTitle();
        String title = clipRightForWidth(font, rawTitle, titleWidth);
        String pagePath = clipSearchPath(font, resolveSearchPath(result.pageId()), pathWidth);
        LytFlowContent snippet = clipSnippetForWidth(result.text(), textColumnWidth);
        return new GuideSearchResultDocumentBuilder.SearchPageResult(
            result.guideId(),
            PageAnchor.page(result.pageId()),
            icon,
            title,
            pagePath,
            snippet);
    }

    /**
     * Breadcrumb path for a search result, mirroring
     * {@code GuideScreen.resolveSearchResultPath} against the merged navigation
     * tree; falls back to the resource path's parent segment.
     */
    private static String resolveSearchPath(ResourceLocation pageId) {
        var path = GuideRegistry.getMergedNavigationTree()
            .getPathTo(pageId);
        if (path.size() > 1) {
            var breadcrumb = new StringBuilder();
            for (int i = 0; i < path.size() - 1; i++) {
                var node = path.get(i);
                if (node.title() == null || node.title()
                    .isEmpty()) {
                    continue;
                }
                if (!breadcrumb.isEmpty()) {
                    breadcrumb.append(" / ");
                }
                breadcrumb.append(node.title());
            }
            if (!breadcrumb.isEmpty()) {
                return breadcrumb.toString();
            }
        }
        String resourcePath = pageId.getResourcePath();
        int slashIndex = resourcePath.lastIndexOf('/');
        return slashIndex > 0 ? resourcePath.substring(0, slashIndex) : "";
    }

    /**
     * Font-metric right-clip with ellipsis, mirroring
     * {@code GuideScreen.clipRightForWidth} using the vanilla {@link FontRenderer}.
     * {@code null} font (no client instance) passes the value through un-clipped.
     */
    private static String clipRightForWidth(@Nullable FontRenderer font, String value, int maxWidth) {
        if (font == null || value == null
            || value.isEmpty()
            || maxWidth <= 0
            || font.getStringWidth(value) <= maxWidth) {
            return value;
        }
        String unclippedValue = value.endsWith(GuideScreen.ASCII_ELLIPSIS)
            && value.length() > GuideScreen.ASCII_ELLIPSIS.length()
                ? value.substring(0, value.length() - GuideScreen.ASCII_ELLIPSIS.length())
                : value;
        int ellipsisWidth = font.getStringWidth(GuideScreen.ASCII_ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return font.trimStringToWidth(GuideScreen.ASCII_ELLIPSIS, maxWidth);
        }
        return font.trimStringToWidth(unclippedValue, maxWidth - ellipsisWidth) + GuideScreen.ASCII_ELLIPSIS;
    }

    /**
     * Character-count right-clip with ellipsis, mirroring
     * {@code GuideScreen.clipRightForChars}.
     */
    private static String clipRightForChars(String value, int maxChars) {
        if (value == null || value.isEmpty() || maxChars <= 0 || value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= GuideScreen.ASCII_ELLIPSIS.length()) {
            return GuideScreen.ASCII_ELLIPSIS.substring(0, maxChars);
        }
        return value.substring(0, maxChars - GuideScreen.ASCII_ELLIPSIS.length()) + GuideScreen.ASCII_ELLIPSIS;
    }

    /**
     * Path clip (char cap then width), mirroring {@code GuideScreen.clipSearchPath}.
     */
    private static String clipSearchPath(@Nullable FontRenderer font, String value, int pathWidth) {
        return clipRightForWidth(font, clipRightForChars(value, GuideScreen.SEARCH_PATH_MAX_CHARS), pathWidth);
    }

    /**
     * Snippet clip mirroring {@code GuideScreen.clipSnippetForWidth} with the
     * headless measurement basis, made line-aware for the Rust renderer's
     * hard-break semantics.
     *
     * <p>
     * <b>Why line-splitting is mandatory:</b> {@link GuideText#measureWidth}
     * returns the <em>widest single line</em> of the shaped text (the Rust
     * {@code shape_text_cmd} width uses a max_x semantic; see
     * {@code src/rust/layout-engine/src/parley_text.rs}). A snippet containing {@code \n} (the Lucene highlighter
     * extracts multi-line fragments; a stress/mixed page snippet carries 4
     * newlines) would therefore pass a whole-text fits check
     * ({@link GuideText#clipToWidth} compares {@code measureWidth(text)}, the
     * widest single line, against the budget) and be passed through un-clipped,
     * then blow the row height up in layout where every {@code \n} is a hard
     * break.
     * The snippet is therefore split on {@code \n} first, each line measured
     * independently, and each newline itself consumes a line slot of the
     * two-line budget.
     *
     * <p>
     * <b>Budget:</b> the snippet is clipped to at most two rendered lines
     * (total budget 2; the first line is measured against the full
     * {@code lineWidth}, the second line (where the ellipsis lands) against
     * {@code lineWidth - ellipsisWidth} so the ellipsis itself fits inside the
     * second line without spilling a third). Width is measured with
     * {@link GuideText#measureWidth}, the same Rust metric the headless render
     * uses, against {@link DefaultStyles#BASE_STYLE} (fontScale 1, non-bold,
     * matching the snippet paragraph's inherited body style). The character
     * budget passed to
     * {@link GuideSearchSnippetFormatter#clipToVisibleCharsWithEllipsis} is the
     * codepoint sum of the kept characters (line 1 + newline + line 2 prefix),
     * mirroring the real path's budget structure; the formatter reserves the
     * ellipsis inside that budget.
     *
     * <p>
     * Fallbacks mirror the real path: a non-positive {@code lineWidth}
     * degrades to {@code clipToVisibleChars(snippet, 0)}; an empty snippet is
     * passed through untouched; a snippet that fits entirely within the
     * two-line budget is passed through un-clipped (no ellipsis appended).
     * When {@link GuideText#isAvailable()} is false no measurement is possible;
     * the snippet must NOT silently pass through as if it fit, so a WARN is
     * logged and the snippet is clipped to empty rather than allowed to
     * overflow.
     */
    private static LytFlowContent clipSnippetForWidth(LytFlowContent snippet, int lineWidth) {
        if (lineWidth <= 0) {
            return GuideSearchSnippetFormatter.clipToVisibleChars(snippet, 0);
        }

        String plainText = GuideSearchSnippetFormatter.toPlainText(snippet);
        if (plainText.isEmpty()) {
            return snippet;
        }

        if (!GuideText.isAvailable()) {
            GuideDebugLog.warnAlways(
                "clipSnippetForWidth: GuideText metrics unavailable, clipping snippet to empty rather than overflowing");
            return GuideSearchSnippetFormatter.clipToVisibleChars(snippet, 0);
        }

        int ellipsisWidth = GuideText.measureWidth(GuideSearchSnippetFormatter.ELLIPSIS, DefaultStyles.BASE_STYLE);
        int lineWithEllipsisWidth = Math.max(0, lineWidth - ellipsisWidth);

        // Hard breaks consume line slots: split first, measure each line
        // independently against the same Rust metric the renderer uses.
        String[] lines = plainText.split("\n", -1);

        String firstClipped = GuideText
            .clipToWidth(lines[0], lineWidth, DefaultStyles.BASE_STYLE, GuideText.ClipSuffix.NONE);
        if (lines.length == 1) {
            // Single line: passes the whole first line → it truly fits.
            if (firstClipped.length() >= lines[0].length()) {
                return snippet;
            }
            // Single overflowing line: clip to one line + ellipsis (the
            // ellipsis width is deducted from the line budget). The budget is
            // the kept codepoint count; the formatter reserves its own
            // 3-char ellipsis inside it, so the result is strictly narrower
            // than the measured prefix and the snippet can never overflow.
            String headClipped = GuideText
                .clipToWidth(lines[0], lineWithEllipsisWidth, DefaultStyles.BASE_STYLE, GuideText.ClipSuffix.NONE);
            return GuideSearchSnippetFormatter
                .clipToVisibleCharsWithEllipsis(snippet, headClipped.codePointCount(0, headClipped.length()));
        }

        // Multi-line snippet: the first line overflowing already exhausts the
        // line-1 slot: keep only the first line's prefix plus the ellipsis.
        if (firstClipped.length() < lines[0].length()) {
            String headClipped = GuideText
                .clipToWidth(lines[0], lineWithEllipsisWidth, DefaultStyles.BASE_STYLE, GuideText.ClipSuffix.NONE);
            return GuideSearchSnippetFormatter
                .clipToVisibleCharsWithEllipsis(snippet, headClipped.codePointCount(0, headClipped.length()));
        }

        // Line 1 fits whole; line 2 (ellipsis target) is measured with the
        // ellipsis width deducted so "line-2 prefix + ..." stays inside the
        // column and never wraps to a third line.
        String secondClipped = GuideText
            .clipToWidth(lines[1], lineWithEllipsisWidth, DefaultStyles.BASE_STYLE, GuideText.ClipSuffix.NONE);
        boolean secondFitsWhole = secondClipped.length() >= lines[1].length();
        if (lines.length == 2 && secondFitsWhole) {
            return snippet;
        }
        // More than two lines, or line 2 overflowing: clip to two lines. The
        // budget keeps line 1 + the hard break + line 2's prefix (codepoints);
        // the formatter subtracts its 3-char ellipsis and appends "...".
        int budget = lines[0].codePointCount(0, lines[0].length()) + 1
            + secondClipped.codePointCount(0, secondClipped.length());
        return GuideSearchSnippetFormatter.clipToVisibleCharsWithEllipsis(snippet, budget);
    }

    /**
     * Build a descriptive "page not found" message including the list of available page keys.
     */
    private static String buildPageNotFoundMessage(MutableGuide guide, String pageId) {
        try {
            var pages = guide.getPages();
            String keyList = pages.stream()
                .limit(30)
                .map(
                    p -> p.getId()
                        .toString())
                .collect(Collectors.joining(", ", "[", "]"));
            return "Page not found: " + pageId + ". Available pages (" + pages.size() + " total): " + keyList;
        } catch (IllegalStateException e) {
            // pages collection is not loaded yet
            return "Page not found: " + pageId + " (pages not loaded yet)";
        }
    }

    // Chrome pass helpers for the nav bar overlay.

    /**
     * Nav bar open width for the headless chrome pass, mirroring
     * {@code GuideScreen.resolveNavigationOpenWidth} under the full-width
     * assumption (panelX = 0, panelW = page width): 18 % of the page width,
     * floored at {@link GuideNavBar#MIN_DYNAMIC_OPEN_WIDTH} and capped by the
     * panel minus padding. For the default 900 px page width this yields
     * {@code max(110, 162) = 162} logical px.
     */
    private static int navBarWidth(int pageWidth) {
        int requested = Math
            .max(GuideNavBar.MIN_DYNAMIC_OPEN_WIDTH, pageWidth * GuideNavBar.OPEN_WIDTH_SCREEN_PERCENT / 100);
        int maxWidth = Math.max(GuideNavBar.WIDTH_CLOSED, pageWidth - 16 - 40);
        return Math.min(requested, maxWidth);
    }

    /**
     * Build a fresh GuideNavBar for the current guide, drive it to the same
     * state the live screen would reach (open/pinned, current page's ancestors
     * expanded) and collect its render primitives via
     * {@link GuideNavBar#collectPrimitives}. The nav bar spans the full
     * document height at x = 0; the returned context backs the second offscreen
     * pass. Headless-only; never touches the live GuideScreen's nav bar.
     */
    private static VanillaRenderContext collectNavBarPrimitives(RenderPageRequest req, MutableGuide guide,
        GuidePage compiledPage, int contentHeight, List<GuideRenderPrimitive> target) {
        int navW = navBarWidth(req.width());
        GuideNavBar navBar = new GuideNavBar();
        navBar.setBounds(0, 0, contentHeight);
        navBar.setOpenWidth(navW);
        GuideBookmarkState bookmarkState = GuideBookmarkState.getSharedInstance();
        NavigationTree tree = guide.getNavigationTree();
        navBar.activateGuide(
            guide.getId(),
            GuideNavBarState.defaultState(),
            tree,
            bookmarkState,
            compiledPage.id(),
            Collections.emptySet());
        navBar.setPinned(true);
        navBar.update(-1, -1, tree, bookmarkState);
        // Headless scroll injection: -Dguidenh.renderpage.navscroll=<px> renders
        // this frame at the given scroll offset so the chrome pass can reproduce
        // and regression-test the sticky/scroll overlap. Default 0 (absent)
        // keeps the existing behaviour byte-identical.
        String navScrollProp = System.getProperty("guidenh.renderpage.navscroll");
        if (navScrollProp != null && !navScrollProp.isEmpty()) {
            try {
                int navScroll = Integer.parseInt(navScrollProp);
                navBar.setScrollY(navScroll);
                GuideDebugLog.infoAlways(
                    "RenderPageService: nav bar scroll injected: {} px (guidenh.renderpage.navscroll)",
                    navScroll);
            } catch (NumberFormatException e) {
                GuideDebugLog
                    .warnAlways("RenderPageService: ignoring invalid -Dguidenh.renderpage.navscroll={}", navScrollProp);
            }
        }
        VanillaRenderContext navCtx = new VanillaRenderContext(new LytRect(0, 0, navW, contentHeight), contentHeight);
        var navCollector = new PrimitiveCollector(new LytRect(0, 0, navW, contentHeight), navCtx);
        navBar.collectPrimitives(guide.getId(), compiledPage.id(), guide, bookmarkState, false, navCollector);
        target.addAll(navCollector.result());
        return navCtx;
    }

    /**
     * Headless mermaid canvas injection, mirroring the navscroll injection
     * pattern above. Reads {@code -Dguidenh.renderpage.mermaidzoom} (double,
     * 0 = no zoom injection) and {@code -Dguidenh.renderpage.mermaidoffset}
     * ({@code "x,y"}, {@code "0,0"} = no offset injection) and applies them to
     * every {@link LytMermaidCanvas} instance in the document before primitive
     * collection, so the {@code HEADLESS} render branch can be verified for the
     * zoom / drag paths without a live client. Absent or zero values are a
     * strict no-op: the canvases keep their historical fit-to-view + centre
     * behaviour byte-identical.
     */
    private static void applyMermaidInjection(LytDocument document) {
        String zoomProp = System.getProperty("guidenh.renderpage.mermaidzoom");
        String offsetProp = System.getProperty("guidenh.renderpage.mermaidoffset");
        boolean zoomAbsent = zoomProp == null || zoomProp.isEmpty();
        boolean offsetAbsent = offsetProp == null || offsetProp.isEmpty();
        if (zoomAbsent && offsetAbsent) {
            return;
        }
        float zoom = 0f;
        if (!zoomAbsent) {
            try {
                zoom = (float) Double.parseDouble(zoomProp);
            } catch (NumberFormatException e) {
                GuideDebugLog
                    .warnAlways("RenderPageService: ignoring invalid -Dguidenh.renderpage.mermaidzoom={}", zoomProp);
                return;
            }
        }
        int offsetX = 0;
        int offsetY = 0;
        if (!offsetAbsent) {
            String[] parts = offsetProp.split(",", -1);
            if (parts.length != 2) {
                GuideDebugLog.warnAlways(
                    "RenderPageService: ignoring invalid -Dguidenh.renderpage.mermaidoffset={} (expected x,y)",
                    offsetProp);
                return;
            }
            try {
                offsetX = Integer.parseInt(parts[0].trim());
                offsetY = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                GuideDebugLog.warnAlways(
                    "RenderPageService: ignoring invalid -Dguidenh.renderpage.mermaidoffset={}",
                    offsetProp);
                return;
            }
        }
        if (zoom <= 0f && offsetX == 0 && offsetY == 0) {
            return;
        }
        GuideDebugLog
            .infoAlways("RenderPageService: mermaid canvas injection zoom={} offset=({},{})", zoom, offsetX, offsetY);
        applyMermaidInjectionRecursive(document, zoom, offsetX, offsetY);
    }

    private static void applyMermaidInjectionRecursive(LytNode node, float zoom, int offsetX, int offsetY) {
        if (node instanceof LytMermaidCanvas<?>canvas) {
            canvas.setHeadlessInjection(zoom, offsetX, offsetY);
        }
        for (var child : node.getChildren()) {
            applyMermaidInjectionRecursive(child, zoom, offsetX, offsetY);
        }
    }

    // Toolbar page-title injection (equivalent of the live drawPageTitle pass).

    /**
     * Headless toolbar page-title injection, mirroring the navscroll / mermaid
     * injection pattern above. Reads {@code -Dguidenh.renderpage.title=true}
     * (absent or any other value = strict no-op: the collected primitive list
     * stays byte-identical) and, when enabled, overlays the toolbar page-title
     * glyph run onto the document render via the same layout the live screen
     * uses in {@code GuideScreen.drawPageTitle}: ordinary toolbar title placed
     * from the toolbar band's left edge (panelX = 0, panelY = 0, panelW = page
     * width, narrow-reading inset 0). This gives the toolbar title a headless
     * verification channel: previously the title band could only be judged by
     * live eyesight.
     */
    private static boolean isPageTitleInjectionEnabled() {
        return Boolean.parseBoolean(System.getProperty("guidenh.renderpage.title"));
    }

    /**
     * Build the toolbar title paragraph the same way the live screen does
     * ({@code GuideScreen.refreshCurrentPageTitle}'s document-title branch):
     * the page's extracted H1 heading flow content when present, otherwise the
     * navigation-tree node title, otherwise the page id. The paragraph is
     * styled with {@link GuideScreen#TOOLBAR_TITLE_STYLE}.
     */
    private static LytParagraph buildToolbarPageTitle(MutableGuide guide, GuidePage page) {
        LytParagraph title = new LytParagraph();
        title.setStyle(GuideScreen.TOOLBAR_TITLE_STYLE);
        LytHeading extracted = page.titleHeading();
        if (extracted != null) {
            for (LytFlowContent flowContent : extracted.getContent()) {
                title.append(flowContent);
            }
        } else {
            String resolvedTitle = null;
            try {
                var node = guide.getNavigationTree()
                    .getNodeById(page.id());
                if (node != null) {
                    resolvedTitle = node.title();
                }
            } catch (Throwable ignored) {}
            if (resolvedTitle == null || resolvedTitle.isEmpty()) {
                resolvedTitle = page.id()
                    .toString();
            }
            title.appendText(resolvedTitle);
        }
        return title;
    }

    /**
     * Collect the toolbar title paragraph as render primitives positioned at
     * its live-screen slot. Mirrors {@code GuideScreen.drawPageTitle}: same
     * ordinary-toolbar titleX (toolbar band left edge + padding) / titleY
     * formulas, same available-width reserve for the toolbar icon row, same
     * title-screen viewport for culling. The primitives are emitted under
     * {@code pushTransform(titleX, titleY, 1.0f)} so the glyphs land at the
     * toolbar-band position in the final output.
     *
     * @return collected primitives; empty when the page carries no title text
     */
    private static List<GuideRenderPrimitive> collectToolbarTitlePrimitives(RenderPageRequest req, MutableGuide guide,
        GuidePage page) {
        LytParagraph titlePara = buildToolbarPageTitle(guide, page);
        if (titlePara.isEmpty()) {
            return List.of();
        }

        int panelX = 0;
        int panelY = 0;
        int panelW = req.width();
        // Ordinary toolbar title (mirrors GuideScreen.drawPageTitle after the
        // toolbar-title semantic change): placed from the toolbar band's left
        // edge, no navbar/content-column avoidance; the reserved right-side
        // icon area is kept.
        int reservedRight = (16 + GuideScreen.TOOLBAR_GAP) * 5 + GuideScreen.PANEL_PADDING + 4;
        int availableW = Math.max(20, panelW - GuideScreen.PANEL_PADDING - reservedRight);
        int titleX = panelX + GuideScreen.PANEL_PADDING;

        // Single-pass layout at (0, 0): position is applied via the GL
        // translate (pushTransform), matching GuideScreen.drawPageTitle.
        var layoutCtx = new LayoutContext(new RustFontMetrics());
        titlePara.layout(layoutCtx, 0, 0, availableW);
        int titleH = titlePara.getBounds()
            .height();
        int titleY = Math.max(0, (GuideScreen.TOOLBAR_H - titleH) / 2) + panelY + 2;

        LytRect titleScreenVp = new LytRect(titleX, titleY, availableW, Math.max(titleH, GuideScreen.TOOLBAR_H));
        var titleCtx = new VanillaRenderContext(titleScreenVp, titleY + titleScreenVp.height());
        var pc = new PrimitiveCollector(titleScreenVp, titleCtx);
        pc.pushTransform(titleX, titleY, 1.0f);
        pc.collectFrom(titlePara);
        pc.popTransform();
        GuideDebugLog.infoAlways(
            "RenderPageService: toolbar page-title injected: '{}' at ({},{}) h={} availableW={} "
                + "(guidenh.renderpage.title)",
            titlePara.getTextContent(),
            titleX,
            titleY,
            titleH,
            availableW);
        return pc.result();
    }

    /**
     * Parse {@code -Dguidenh.renderpage.guiscale} into an int injection value,
     * mirroring the navscroll injection pattern. Three-state semantics:
     * <ul>
     * <li>absent or empty property → default 4 (the headless render baseline
     * is guiscale=4, matching the live client's render_scale=4 capacity
     * surface);</li>
     * <li>explicit 0 → no injection (layout stays byte-identical to the
     * scaleFactor=1 path);</li>
     * <li>explicit 1-4 → that value, injected verbatim.</li>
     * </ul>
     * Invalid (non-numeric or out-of-range) values are reported once with a
     * WARN and disable the injection (return 0).
     */
    private static int parseGuiscaleInjection() {
        String prop = System.getProperty("guidenh.renderpage.guiscale");
        if (prop == null || prop.isEmpty()) {
            // Absent property = unset → baseline default 4 (headless render
            // baseline is guiscale=4; see the injection comment at the call
            // site).
            return 4;
        }
        try {
            int v = Integer.parseInt(prop);
            if (v >= 1 && v <= 4) {
                return v;
            }
            if (v == 0) {
                // Explicit 0 = disable injection (layout stays byte-identical).
                return 0;
            }
            GuideDebugLog.warnAlways(
                "RenderPageService: ignoring invalid -Dguidenh.renderpage.guiscale={} (expected 1-4; 0 = disable)",
                prop);
        } catch (NumberFormatException e) {
            GuideDebugLog.warnAlways("RenderPageService: ignoring invalid -Dguidenh.renderpage.guiscale={}", prop);
        }
        return 0;
    }

    /**
     * Composite the two offscreen passes side by side: nav image at x = 0,
     * document image shifted right by {@code navWidthPx} (scale-scaled nav
     * width). The background fills the remaining band gap if the nav image is
     * shorter than the document image.
     */
    private static BufferedImage composeChrome(BufferedImage docImage, BufferedImage navImage, int navWidthPx,
        int backgroundRgb) {
        int w = docImage.getWidth() + navWidthPx;
        int h = Math.max(docImage.getHeight(), navImage.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(new Color(backgroundRgb));
            g.fillRect(0, 0, w, h);
            g.drawImage(navImage, 0, 0, null);
            g.drawImage(docImage, navWidthPx, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    // File naming.

    private static String buildBaseName(RenderPageRequest req) {
        String name;
        if (req.route() != null && ROUTE_HOME_SEARCH.equals(req.route())) {
            // Route renders have no page id / md file: derive the stem from the route
            // name plus a file-name-safe form of the query (e.g. search_expected_...).
            name = "search_" + sanitizeFileName(req.query());
        } else if (req.route() != null && ROUTE_SETTINGS.equals(req.route())) {
            // Settings route has no query: the stem is just the route name
            // (settings_<ts>.png / .json), mirroring the search-route naming pattern.
            name = "settings";
        } else if (req.pageId() != null && !req.pageId()
            .isEmpty()) {
                // Use the path segment after the colon (namespace:path)
                String pageId = req.pageId();
                int colon = pageId.indexOf(':');
                if (colon >= 0) {
                    name = pageId.substring(colon + 1);
                } else {
                    name = pageId;
                }
                // Replace path separators with underscores
                name = name.replace('/', '_')
                    .replace(':', '_');
            } else {
                name = req.mdFile()
                    .getFileName()
                    .toString();
                if (name.endsWith(".md")) {
                    name = name.substring(0, name.length() - 3);
                }
            }
        return name + "_"
            + LocalDateTime.now()
                .format(FILE_NAME_FORMAT);
    }

    /**
     * File-name-safe form of the search query: trim and replace every character
     * outside {@code [a-zA-Z0-9._-]} with {@code _}. Falls back to {@code empty}
     * so a blank query still yields a usable file stem.
     */
    private static String sanitizeFileName(@Nullable String value) {
        if (value == null || value.trim()
            .isEmpty()) {
            return "empty";
        }
        return value.trim()
            .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Resolve a non-colliding file path. Appends {@code _2}, {@code _3} … when
     * the candidate already exists, matching the pattern used by
     * {@code SceneEditorScreenshotExportService.resolveTargetPath}.
     */
    private static Path resolveTargetPath(Path dir, String baseName, String extension) throws IOException {
        Path candidate = dir.resolve(baseName + "." + extension);
        int collisionIndex = 2;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(baseName + "_" + collisionIndex + "." + extension);
            collisionIndex++;
        }
        return candidate;
    }

    // Bounds JSON.

    private static void writeBoundsJson(LytDocument document, Path target) throws IOException {
        var arr = new JsonArray();
        walkBlocksForJson(document, 0, arr);
        String json = new GsonBuilder().setPrettyPrinting()
            .create()
            .toJson(arr);
        Files.writeString(target, json, StandardCharsets.UTF_8);
    }

    private static void walkBlocksForJson(LytNode node, int depth, JsonArray target) {
        if (node instanceof LytBlock block && !LayoutTreeSerializer.shouldSkipInBoundsDump(node)) {
            LytRect bounds = block.getBounds();
            if (bounds != null) {
                var obj = new JsonObject();
                obj.addProperty("i", target.size());
                obj.addProperty(
                    "cls",
                    block.getClass()
                        .getSimpleName());
                obj.addProperty("x", bounds.x());
                obj.addProperty("y", bounds.y());
                obj.addProperty("w", bounds.width());
                obj.addProperty("h", bounds.height());
                obj.addProperty("depth", depth);
                target.add(obj);
            }
        }
        for (var child : node.getChildren()) {
            walkBlocksForJson(child, depth + 1, target);
        }
    }

    // Debug overlay.

    private static void drawDebugOverlay(BufferedImage source, LytDocument document, Path target, int scale)
        throws IOException {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage overlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int[] counter = { 0 };
            drawOverlayBlocks(g, document, 0, counter, scale);
        } finally {
            g.dispose();
        }

        // Composite the overlay onto a copy of the source image
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D rg = result.createGraphics();
        try {
            rg.drawImage(source, 0, 0, null);
            rg.drawImage(overlay, 0, 0, null);
        } finally {
            rg.dispose();
        }
        ImageIO.write(result, "png", target.toFile());
    }

    /**
     * Recursively walk the block tree and draw semi-transparent fills, borders,
     * and block-index labels. The colour cycles through six colours based on
     * nesting depth.
     *
     * @param counter single-element array carrying the global block index
     */
    private static void drawOverlayBlocks(Graphics2D g, LytNode node, int depth, int[] counter, int scale) {
        if (node instanceof LytBlock block) {
            LytRect bounds = block.getBounds();
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                int idx = counter[0]++;
                int ci = depth % OVERLAY_FILL_COLORS.length;
                int bx = bounds.x() * scale;
                int by = bounds.y() * scale;
                int bw = bounds.width() * scale;
                int bh = bounds.height() * scale;

                // Semi-transparent fill
                g.setColor(new Color(OVERLAY_FILL_COLORS[ci], true));
                g.fillRect(bx, by, bw, bh);

                // Solid border
                g.setColor(new Color(OVERLAY_BORDER_COLORS[ci]));
                g.drawRect(bx, by, bw, bh);

                // Block index label near the top-left corner
                g.setColor(new Color(OVERLAY_BORDER_COLORS[ci]));
                g.drawString(String.valueOf(idx), bx + 2 * scale, by + 12 * scale);
            }
        }
        for (var child : node.getChildren()) {
            drawOverlayBlocks(g, child, depth + 1, counter, scale);
        }
    }

    // Block counting.

    private static int countBlocks(LytNode node) {
        int count = 0;
        if (node instanceof LytBlock) {
            count = 1;
        }
        for (var child : node.getChildren()) {
            count += countBlocks(child);
        }
        return count;
    }
}
