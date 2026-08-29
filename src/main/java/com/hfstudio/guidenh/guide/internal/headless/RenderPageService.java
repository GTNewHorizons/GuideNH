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
import net.minecraft.util.ResourceLocation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytMermaidCanvas;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.internal.GuideBookmarkState;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;
import com.hfstudio.guidenh.guide.layout.FontProvider;
import com.hfstudio.guidenh.guide.layout.JavaFontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutBridge;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.LayoutTreeSerializer;
import com.hfstudio.guidenh.guide.layout.SystemFontProvider;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Getter;

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

    /** Six cycling colours for overlay borders/labels, keyed by depth % 6. */
    private static final int[] OVERLAY_FILL_COLORS = { 0x44FF0000, 0x4400FF00, 0x440000FF, 0x44FFFF00, 0x44FF00FF,
        0x4400FFFF };
    private static final int[] OVERLAY_BORDER_COLORS = { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF,
        0xFF00FFFF };

    private RenderPageService() {}

    // ---- data types ---------------------------------------------------------

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
     *                         headless render (S2 verification channel). The nav
     *                         bar occupies the left {@link #navBarWidth(int)}
     *                         logical px and the document is shifted right; the
     *                         bounds JSON stays in document coordinates.
     */
    public record RenderPageRequest(String guideId, String pageId, Path mdFile, String language, int width, Path outDir,
        boolean emitBoundsJson, boolean emitDebugOverlay, int scale, boolean chrome) {

        /** Legacy 9-arg construction (in-game command path) — chrome defaults off. */
        public RenderPageRequest(String guideId, String pageId, Path mdFile, String language, int width, Path outDir,
            boolean emitBoundsJson, boolean emitDebugOverlay, int scale) {
            this(guideId, pageId, mdFile, language, width, outDir, emitBoundsJson, emitDebugOverlay, scale, false);
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
    @Getter
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

    }

    // ---- public API ---------------------------------------------------------

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
     * Idempotent — subsequent calls are no-ops once the font handle is non-zero.
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
        // ---- 1. Font engine -------------------------------------------------
        ensureFontEngineReady();

        // ---- 2. Resolve host guide ------------------------------------------
        ResourceLocation guideId = new ResourceLocation(req.guideId());
        MutableGuide guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Guide not found: " + req.guideId());
        }

        // ---- 3. Compile -----------------------------------------------------
        GuidePage compiledPage;
        try {
            if (req.pageId() != null && !req.pageId()
                .isEmpty()) {
                compiledPage = compileRegisteredPage(guide, req);
            } else if (req.mdFile() != null) {
                compiledPage = compileMdFile(guide, req);
            } else {
                throw new RenderPageException(
                    RenderPageException.Stage.COMPILE,
                    "Either pageId or mdFile must be provided");
            }
        } catch (RenderPageException e) {
            throw e;
        } catch (Exception e) {
            throw new RenderPageException(RenderPageException.Stage.COMPILE, "Compilation failed", e);
        }

        LytDocument document = compiledPage.document();

        // ---- 3a. Mount document (dispatch MOUNT events for SceneScript etc.) ---
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

        // ---- 4+5. Layout + primitive collection ------------------------------
        // Optional headless guiscale injection (-Dguidenh.renderpage.guiscale=
        // <1|2|3|4>): temporarily set gameSettings.guiScale so that
        // DisplayScale.scaleFactor() — whose cache key includes guiScale —
        // drives the Rust glyph rasterization scale like the live client's
        // auto render_scale. Without this, headless renders always run at
        // scaleFactor=1 (11 ppem glyphs) and can never cover the capacity
        // surface that breaks at render_scale=4. The property is read once,
        // the value is applied before layout and restored afterwards. Absent
        // property = strict no-op (layout stays byte-identical).
        int contentHeight;
        List<GuideRenderPrimitive> primitives;
        VanillaRenderContext renderCtx;
        Minecraft mc = Minecraft.getMinecraft();
        int guiscaleInjection = parseGuiscaleInjection();
        int previousGuiScale = 0;
        if (guiscaleInjection > 0 && mc != null) {
            previousGuiScale = mc.gameSettings.guiScale;
            mc.gameSettings.guiScale = guiscaleInjection;
            GuideDebugLog.infoAlways(
                "RenderPageService: guiscale injection active: {} (layout render_scale simulation; "
                    + "restored after layout/collect)",
                guiscaleInjection);
        }
        try {
            try {
                var layoutCtx = new LayoutContext(new JavaFontMetrics()).withVisualScale(1.0f);
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
                renderCtx = new VanillaRenderContext(LightDarkMode.LIGHT_MODE, fullViewport, contentHeight);
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
                // non-"true" value is a strict no-op — the primitive list stays
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
            }
        }

        // ---- 6. Render ------------------------------------------------------
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

        // ---- 7. Write PNG ---------------------------------------------------
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

        // ---- 8. Bounds JSON (optional) --------------------------------------
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

        // ---- 9. Debug overlay (optional) ------------------------------------
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

        // ---- 10. Unmount document from LytHost (avoid document leak on static host) ---
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

    // ---- compilation helpers ------------------------------------------------

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

    // ---- chrome pass helpers (S2 nav bar overlay) --------------------------

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
     * pass. Headless-only — never touches the live GuideScreen's nav bar.
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
        VanillaRenderContext navCtx = new VanillaRenderContext(
            LightDarkMode.DARK_MODE,
            new LytRect(0, 0, navW, contentHeight),
            contentHeight);
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

    // ---- toolbar page-title injection (drawPageTitle equivalent) ------------

    /**
     * Headless toolbar page-title injection, mirroring the navscroll / mermaid
     * injection pattern above. Reads {@code -Dguidenh.renderpage.title=true}
     * (absent or any other value = strict no-op: the collected primitive list
     * stays byte-identical) and, when enabled, overlays the toolbar page-title
     * glyph run onto the document render via the same layout the live screen
     * uses in {@code GuideScreen.drawPageTitle}: ordinary toolbar title placed
     * from the toolbar band's left edge (panelX = 0, panelY = 0, panelW = page
     * width, narrow-reading inset 0). This gives the toolbar title a headless
     * verification channel — X.7's lesson was that the title band previously
     * could only be judged by live eyesight.
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
        var layoutCtx = new LayoutContext(new JavaFontMetrics());
        titlePara.layout(layoutCtx, 0, 0, availableW);
        int titleH = titlePara.getBounds()
            .height();
        int titleY = Math.max(0, (GuideScreen.TOOLBAR_H - titleH) / 2) + panelY + 2;

        LytRect titleScreenVp = new LytRect(titleX, titleY, availableW, Math.max(titleH, GuideScreen.TOOLBAR_H));
        var titleCtx = new VanillaRenderContext(
            LightDarkMode.LIGHT_MODE,
            titleScreenVp,
            titleY + titleScreenVp.height());
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
     * Parse {@code -Dguidenh.renderpage.guiscale} (1-4) into an int injection
     * value, mirroring the navscroll injection pattern. Absent, empty or
     * invalid values return 0 (= no injection, layout stays byte-identical);
     * invalid values are reported once with a WARN.
     */
    private static int parseGuiscaleInjection() {
        String prop = System.getProperty("guidenh.renderpage.guiscale");
        if (prop == null || prop.isEmpty()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(prop);
            if (v >= 1 && v <= 4) {
                return v;
            }
            GuideDebugLog.warnAlways(
                "RenderPageService: ignoring invalid -Dguidenh.renderpage.guiscale={} (expected 1-4)",
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

    // ---- file naming --------------------------------------------------------

    private static String buildBaseName(RenderPageRequest req) {
        String name;
        if (req.pageId() != null && !req.pageId()
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

    // ---- bounds JSON --------------------------------------------------------

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

    // ---- debug overlay ------------------------------------------------------

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

    // ---- block counting -----------------------------------------------------

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
