package com.hfstudio.guidenh.guide.internal.headless;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialDataIndex;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Headless screenshot render driver activated by {@code -Dguidenh.headlessRender=true}.
 *
 * <p>
 * State machine (driven by {@link TickEvent.ClientTickEvent}):
 * <ol>
 * <li>{@code IDLE} — wait for main menu, then close screen, launch integrated server</li>
 * <li>{@code LOADING_WORLD} — poll until {@code theWorld / thePlayer / netHandler} are non-null</li>
 * <li>{@code WORLD_STABLE} — wait 20 ticks, then start rendering</li>
 * <li>{@code RENDERING} — render page(s): single page or batch loop
 * <ul>
 * <li>Single-page mode ({@code --page} / {@code --md}): render + {@code exitJava(0|1)}</li>
 * <li>Batch mode ({@code --allPages} / {@code --list}): loop all pages, then summary + {@code exitJava(0|1)}</li>
 * </ul>
 * </li>
 * <li>{@code DONE} — guard against re-entry</li>
 * </ol>
 *
 * <p>
 * <b>Batch mode properties</b> (mutually exclusive with {@code --page} / {@code --md}):
 * <ul>
 * <li>{@code -Dguidenh.renderpage.allPages=true} — render every page of the guide in sorted order</li>
 * <li>{@code -Dguidenh.renderpage.list=&lt;txt-path&gt;} — one pageId per line; empty lines and
 * {@code #}-prefixed lines are skipped</li>
 * </ul>
 *
 * <p>
 * Batch summary is printed to stdout and log: {@code total / ok / failed} plus failed-page list.
 * Exit code: {@code 0} when all succeeded, {@code 1} on any failure.
 *
 * <p>
 * <b>Watchdog:</b> default 360 s timeout for single-page mode; for batch mode the timeout is
 * {@code 360 + 120 × pageCount} seconds, computed once when the page list is known.
 * <b>Limitation:</b> rendering executes synchronously on the client tick thread. If a single page
 * render hangs, the watchdog cannot interrupt it — it can only fire before or after that page
 * completes.
 *
 * <p>
 * All errors → {@code exitJava(1)}. Never swallow exceptions.
 *
 * @see RenderPageService
 */
public class GuideNhHeadlessRenderDriver {

    // ---- config --------------------------------------------------------------

    /**
     * Max time (ms) to block before the first headless page render for the MediaWiki
     * special-data index warmup (scheduled asynchronously by MutableGuide) to complete.
     * Bounded so a stuck warmup worker can never hang the headless render forever;
     * on timeout we log a warning and continue.
     */
    private static final long MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS = 30_000L;

    /** Poll interval (ms) while waiting for the MediaWiki warmup worker. */
    private static final long MEDIA_WIKI_WARMUP_POLL_MILLIS = 25L;

    /** Immutable snapshot of JVM property configuration. */
    public record HeadlessRenderConfig(String guideId, @Nullable String pageId, @Nullable Path mdFile, boolean allPages,
        @Nullable Path listPath, int width, Path outDir, String language, boolean emitBoundsJson,
        boolean emitDebugOverlay, String worldName, int scale, boolean chrome) {}

    // ---- state machine -------------------------------------------------------

    private enum State {
        IDLE,
        LOADING_WORLD,
        WORLD_STABLE,
        RENDERING,
        DONE
    }

    private State state = State.IDLE;
    private final HeadlessRenderConfig config;
    private long watchdogDeadlineNanos;
    private int stableTickCount = 0;
    private boolean renderPending = false;

    // ---- batch state ---------------------------------------------------------

    /** Ordered list of page IDs to render (batch mode). */
    private List<String> batchPageIds = Collections.emptyList();
    private int pageIndex = 0;
    private int okCount = 0;
    private int failCount = 0;
    private final List<String> failedPageIds = new ArrayList<>();

    // ---- construction --------------------------------------------------------

    public GuideNhHeadlessRenderDriver(HeadlessRenderConfig config) {
        this.config = config;
        this.watchdogDeadlineNanos = System.nanoTime() + 360_000_000_000L;
        if (config.chrome()) {
            GuideDebugLog
                .infoAlways("[GuideNH] [HeadlessRender] chrome pass enabled: nav bar will be appended to page renders");
        }
    }

    // ---- property parsing ----------------------------------------------------

    /**
     * Parse all relevant JVM properties into a {@link HeadlessRenderConfig}.
     *
     * @return parsed config, or {@code null} if any required property is missing or invalid
     */
    @Nullable
    public static HeadlessRenderConfig parseConfig() {
        String guideId = System.getProperty("guidenh.renderpage.guide");
        if (guideId == null || guideId.isEmpty()) {
            logError("Missing required property: -Dguidenh.renderpage.guide=<id>");
            return null;
        }

        String pageId = System.getProperty("guidenh.renderpage.page");
        String mdProp = System.getProperty("guidenh.renderpage.md");
        String allPagesProp = System.getProperty("guidenh.renderpage.allPages");
        String listProp = System.getProperty("guidenh.renderpage.list");

        boolean hasPage = pageId != null && !pageId.isEmpty();
        boolean hasMd = mdProp != null && !mdProp.isEmpty();
        boolean allPages = "true".equalsIgnoreCase(allPagesProp);
        boolean hasList = listProp != null && !listProp.isEmpty();

        // Mutually exclusive groups: single-page (page/md) vs batch (allPages/list)
        int singleModes = (hasPage ? 1 : 0) + (hasMd ? 1 : 0);
        int batchModes = (allPages ? 1 : 0) + (hasList ? 1 : 0);

        if (singleModes > 0 && batchModes > 0) {
            logError(
                "Batch mode (-Dguidenh.renderpage.allPages / --list) and single-page mode "
                    + "(-Dguidenh.renderpage.page / --md) are mutually exclusive");
            return null;
        }
        if (batchModes > 1) {
            logError(
                "Only one batch mode allowed: -Dguidenh.renderpage.allPages OR "
                    + "-Dguidenh.renderpage.list, not both");
            return null;
        }
        if (batchModes == 0 && singleModes == 0) {
            logError(
                "Specify single-page mode (-Dguidenh.renderpage.page=<id> or "
                    + "-Dguidenh.renderpage.md=<path>) or batch mode "
                    + "(-Dguidenh.renderpage.allPages=true or -Dguidenh.renderpage.list=<path>)");
            return null;
        }

        int width;
        try {
            width = Integer.parseInt(System.getProperty("guidenh.renderpage.width", "900"));
        } catch (NumberFormatException e) {
            logError("Invalid width value: " + System.getProperty("guidenh.renderpage.width"));
            return null;
        }
        if (width < 100 || width > 4096) {
            logError("Width must be between 100 and 4096, got: " + width);
            return null;
        }

        String outProp = System.getProperty("guidenh.renderpage.out");
        Path outDir;
        if (outProp != null && !outProp.isEmpty()) {
            outDir = Paths.get(outProp);
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.mcDataDir == null) {
                logError("Cannot determine default output directory: Minecraft instance not available");
                return null;
            }
            outDir = mc.mcDataDir.toPath()
                .resolve("screenshots");
        }

        String lang = System.getProperty("guidenh.renderpage.lang", "en_us");
        boolean bounds = Boolean.parseBoolean(System.getProperty("guidenh.renderpage.bounds", "false"));
        boolean overlay = Boolean.parseBoolean(System.getProperty("guidenh.renderpage.overlay", "false"));
        String worldName = System.getProperty("guidenh.renderpage.world", "screenshot-world");

        int scale;
        try {
            scale = Integer.parseInt(System.getProperty("guidenh.renderpage.scale", "1"));
        } catch (NumberFormatException e) {
            logError("Invalid scale value: " + System.getProperty("guidenh.renderpage.scale"));
            return null;
        }
        if (scale < 1 || scale > 4) {
            logError("Scale must be between 1 and 4, got: " + scale);
            return null;
        }

        boolean chrome = Boolean.parseBoolean(System.getProperty("guidenh.renderpage.chrome", "false"));

        Path mdPath = null;
        Path listPath = null;
        try {
            mdPath = hasMd ? Paths.get(mdProp) : null;
            listPath = hasList ? Paths.get(listProp) : null;
        } catch (InvalidPathException e) {
            // NOTE: --md / --list expect FILE PATHS (list = file with one pageId per line),
            // not page ids. Page ids contain ':' which is an illegal Windows path char and
            // previously blew up here as an unlogged InvalidPathException that FML's state
            // event dispatch swallowed silently, hanging the client at the main menu.
            logError(
                "Invalid file path for -Dguidenh.renderpage.md / --list: " + e.getMessage()
                    + " (note: --list expects a path to a file containing one pageId per line)");
            return null;
        }

        return new HeadlessRenderConfig(
            guideId,
            hasPage ? pageId : null,
            mdPath,
            allPages,
            listPath,
            width,
            outDir,
            lang,
            bounds,
            overlay,
            worldName,
            scale,
            chrome);
    }

    private static void logError(String message) {
        GuideDebugLog.error("[GuideNH] [HeadlessRender] {}", message);
        System.err.println("[GuideNH] [HeadlessRender] " + message);
    }

    // ---- tick handler --------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Watchdog: default 360s; batch mode recalculates when page list is known
        if (System.nanoTime() > watchdogDeadlineNanos) {
            GuideDebugLog.error("[GuideNH] [HeadlessRender] headless render timeout exceeded");
            FMLCommonHandler.instance()
                .exitJava(2, false);
            return;
        }

        if (state == State.DONE) {
            return;
        }

        try {
            tick();
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] [HeadlessRender] Unhandled exception in state machine", t);
            System.err.println("[GuideNH] [HeadlessRender] Unhandled exception: " + t.getMessage());
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }

    private void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        switch (state) {
            case IDLE -> handleIdle(mc);
            case LOADING_WORLD -> handleLoadingWorld(mc);
            case WORLD_STABLE -> handleWorldStable(mc);
            case RENDERING -> {
                // Rendering is performed in onRenderTick; ClientTick only handles watchdog.
            }
            default -> {}
        }
    }

    // ---- state handlers ------------------------------------------------------

    private void handleIdle(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            return;
        }

        state = State.LOADING_WORLD;
        GuideDebugLog
            .infoAlways("[GuideNH] [HeadlessRender] Starting integrated server (world: {})", config.worldName());

        mc.displayGuiScreen(null);
        WorldSettings settings = new WorldSettings(0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
        mc.launchIntegratedServer(config.worldName(), config.worldName(), settings);
    }

    private void handleLoadingWorld(Minecraft mc) {
        if (mc.theWorld != null && mc.thePlayer != null && mc.getNetHandler() != null) {
            state = State.WORLD_STABLE;
            stableTickCount = 0;
            GuideDebugLog.infoAlways("[GuideNH] [HeadlessRender] World loaded, waiting 20 ticks for stability");
        }
    }

    private void handleWorldStable(Minecraft mc) {
        stableTickCount++;
        if (stableTickCount < 20) {
            return;
        }

        // Both batch and single-page modes must not render before the async MediaWiki
        // special-data warmup is ready: Special pages compile against the guide's
        // MediaWikiListContext, and a not-yet-warmed guide serves an empty fallback
        // (MediaWikiSpecialDataIndex.empty()), which renders as an empty 96 px page.
        awaitMediaWikiWarmup();

        if (config.allPages() || config.listPath() != null) {
            // ---- batch mode: prepare, then let handleRendering loop -----------
            List<String> ids;
            try {
                ids = collectBatchPageIds();
            } catch (Exception e) {
                logError("Failed to collect batch page IDs: " + e.getMessage());
                FMLCommonHandler.instance()
                    .exitJava(1, false);
                return;
            }

            if (ids.isEmpty()) {
                logError("No pages to render in batch mode");
                FMLCommonHandler.instance()
                    .exitJava(1, false);
                return;
            }

            // One-time watchdog recalculation based on page count
            watchdogDeadlineNanos = System.nanoTime() + 360_000_000_000L + (long) ids.size() * 120_000_000_000L;

            batchPageIds = ids;
            pageIndex = 0;
            okCount = 0;
            failCount = 0;
            failedPageIds.clear();

            state = State.RENDERING;
            renderPending = true;
            GuideDebugLog.infoAlways(
                "[GuideNH] [HeadlessRender] Batch render start: {} pages (watchdog {}s), deferred to RenderTickEvent",
                ids.size(),
                360L + ids.size() * 120L);
            System.out.println(
                "[GuideNH] [HeadlessRender] Batch render start: " + ids.size() + " pages, deferred to RenderTickEvent");

        } else {
            // ---- single-page mode: defer render to RenderTickEvent ------------
            state = State.RENDERING;
            renderPending = true;
            GuideDebugLog.infoAlways("[GuideNH] [HeadlessRender] Single-page render deferred to RenderTickEvent");
        }
    }

    // ---- MediaWiki warmup gate ------------------------------------------------

    /**
     * Block until the target guide's MediaWiki special-data index warmup has completed,
     * or until {@link #MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS} elapses (timeout → warning log
     * and continue; the affected Special pages may render as empty fallbacks).
     *
     * <p>
     * In {@link MutableGuide}, the async warmup is <em>triggered</em> by the first
     * {@link MutableGuide#getMediaWikiListContext()} call; until it finishes the guide
     * serves a fallback context whose {@link MediaWikiSpecialDataIndex} is the empty
     * singleton. Therefore the first call here both schedules the warmup and inspects
     * it, and subsequent polls detect completion once the real index replaces the empty
     * fallback. When the guide type has no async warmup (or it is already complete)
     * this returns immediately.
     */
    private void awaitMediaWikiWarmup() {
        ResourceLocation guideId = new ResourceLocation(config.guideId());
        MutableGuide guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            return;
        }
        MediaWikiListContext context = guide.getMediaWikiListContext();
        if (context == null || context.specialDataIndex() != MediaWikiSpecialDataIndex.empty()) {
            return; // no async warmup pending, or already complete
        }
        GuideDebugLog.infoAlways(
            "[GuideNH] [HeadlessRender] Waiting for MediaWiki special-data warmup of guide {} (up to {} ms)",
            guideId,
            MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS);

        long deadlineNanos = System.nanoTime() + MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS * 1_000_000L;
        boolean interrupted = false;
        while (System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(MEDIA_WIKI_WARMUP_POLL_MILLIS);
            } catch (InterruptedException e) {
                interrupted = true;
                GuideDebugLog.warnAlways(
                    "[GuideNH] [HeadlessRender] Interrupted while waiting for MediaWiki special-data warmup: {}",
                    e.getMessage());
                break;
            }
            context = guide.getMediaWikiListContext();
            if (context != null && context.specialDataIndex() != MediaWikiSpecialDataIndex.empty()) {
                long waitedMillis = (MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS * 1_000_000L
                    - (deadlineNanos - System.nanoTime())) / 1_000_000L;
                GuideDebugLog.infoAlways(
                    "[GuideNH] [HeadlessRender] MediaWiki special-data warmup complete for guide {} "
                        + "(waited {} ms of {} ms budget)",
                    guideId,
                    waitedMillis,
                    MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS);
                break;
            }
        }
        if (interrupted) {
            Thread.currentThread()
                .interrupt();
            return;
        }
        if (context == null || context.specialDataIndex() == MediaWikiSpecialDataIndex.empty()) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [HeadlessRender] MediaWiki special-data warmup for guide {} not complete within {} ms; "
                    + "proceeding — Special pages may render empty fallbacks",
                guideId,
                MEDIA_WIKI_WARMUP_TIMEOUT_MILLIS);
        }
    }

    // ---- batch rendering -----------------------------------------------------

    /**
     * Collect page IDs for batch mode either from the guide ({@code allPages}) or from the list
     * file ({@code listPath}).
     */
    private List<String> collectBatchPageIds() {
        if (config.allPages()) {
            ResourceLocation guideId = new ResourceLocation(config.guideId());
            MutableGuide guide = GuideRegistry.getById(guideId);
            if (guide == null) {
                throw new IllegalStateException("Guide not found: " + config.guideId());
            }
            Collection<ParsedGuidePage> pages = guide.getPages();
            return pages.stream()
                .map(
                    p -> p.getId()
                        .toString())
                .sorted()
                .collect(Collectors.toList());
        } else {
            return readPageIdList(config.listPath());
        }
    }

    /**
     * Read a page-ID list file: one pageId per line; empty lines and lines starting with
     * {@code #} are skipped. Non-empty lines that do not start with {@code #} are treated as
     * page IDs without further validation — invalid IDs will fail during render and be
     * reported there.
     */
    private List<String> readPageIdList(Path listPath) {
        List<String> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(listPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                result.add(trimmed);
            }
        } catch (IOException e) {
            logError("Failed to read page list file: " + listPath + " (" + e.getMessage() + ")");
        }
        GuideDebugLog
            .infoAlways("[GuideNH] [HeadlessRender] Read {} page IDs from list file: {}", result.size(), listPath);
        return result;
    }

    /**
     * Render one page per invocation. Called from {@link #onRenderTick(TickEvent.RenderTickEvent)}
     * when {@code state == RENDERING} in batch mode.
     */
    private void handleRendering(Minecraft mc) {
        if (pageIndex >= batchPageIds.size()) {
            finishBatch();
            return;
        }

        String pageIdStr = batchPageIds.get(pageIndex);
        GuideDebugLog.infoAlways(
            "[GuideNH] [HeadlessRender] Rendering page [{}/{}]: {}",
            pageIndex + 1,
            batchPageIds.size(),
            pageIdStr);
        System.out.println(
            "[GuideNH] [HeadlessRender] Rendering page [" + (pageIndex + 1)
                + "/"
                + batchPageIds.size()
                + "]: "
                + pageIdStr);

        try {
            RenderPageService.ensureFontEngineReady();
            var req = new RenderPageService.RenderPageRequest(
                config.guideId(),
                pageIdStr,
                null, // no mdFile in batch mode
                config.language(),
                config.width(),
                config.outDir(),
                config.emitBoundsJson(),
                config.emitDebugOverlay(),
                config.scale(),
                config.chrome());
            RenderPageService.RenderPageResult result = RenderPageService.render(req);

            String okMsg = "[GuideNH] [HeadlessRender] Page OK [" + (pageIndex + 1)
                + "/"
                + batchPageIds.size()
                + "]: "
                + pageIdStr
                + " -> "
                + result.pngPath()
                + " ("
                + result.widthPx()
                + "x"
                + result.heightPx()
                + ")";
            GuideDebugLog.infoAlways(okMsg);
            System.out.println(okMsg);
            okCount++;
        } catch (RenderPageService.RenderPageException e) {
            String failMsg = "[GuideNH] [HeadlessRender] Page FAILED [" + (pageIndex + 1)
                + "/"
                + batchPageIds.size()
                + "]: "
                + pageIdStr
                + " at stage "
                + e.getStage()
                + ": "
                + e.getMessage();
            GuideDebugLog.error(failMsg);
            System.err.println(failMsg);
            failCount++;
            failedPageIds.add(pageIdStr);
        } catch (Throwable t) {
            String failMsg = "[GuideNH] [HeadlessRender] Page FAILED [" + (pageIndex + 1)
                + "/"
                + batchPageIds.size()
                + "]: "
                + pageIdStr
                + " with exception: "
                + t.getMessage();
            GuideDebugLog.error(failMsg, t);
            System.err.println(failMsg);
            failCount++;
            failedPageIds.add(pageIdStr);
        }

        pageIndex++;

        if (pageIndex >= batchPageIds.size()) {
            finishBatch();
        }
    }

    /**
     * Print batch summary to stdout and log, then exit with the appropriate code.
     */
    private void finishBatch() {
        state = State.DONE;

        int total = okCount + failCount;
        String summary = "[GuideNH] [HeadlessRender] Batch complete: total=" + total
            + " ok="
            + okCount
            + " failed="
            + failCount;
        GuideDebugLog.infoAlways(summary);
        System.out.println(summary);

        if (!failedPageIds.isEmpty()) {
            String failedSummary = "[GuideNH] [HeadlessRender] Failed pages (" + failCount
                + "): "
                + String.join(", ", failedPageIds);
            GuideDebugLog.error(failedSummary);
            System.err.println(failedSummary);
        }

        FMLCommonHandler.instance()
            .exitJava(failCount > 0 ? 1 : 0, false);
    }

    // ---- render-tick handler (frame rendering cycle) -------------------------

    /**
     * Execute deferred rendering inside the frame rendering cycle (RenderTickEvent.END).
     *
     * <p>
     * Angelica's Tessellator mixins route draws through VBO/VAO paths whose internal state
     * is tied to the frame rendering pass. Running {@link RenderPageService#render} inside
     * {@link TickEvent.ClientTickEvent} (outside frame) caused silent zero-fragment output.
     * This handler shifts execution into the frame cycle to validate that hypothesis.
     *
     * <p>
     * Both single-page and batch modes are handled here. Batch mode renders all remaining
     * pages in one go (equivalent to the original per-tick loop, just inside the frame render
     * cycle instead of client tick).
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!renderPending) {
            return;
        }
        renderPending = false;

        if (state != State.RENDERING) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        if (config.allPages() || config.listPath() != null) {
            // Batch mode: render all remaining pages in this render frame
            while (pageIndex < batchPageIds.size()) {
                handleRendering(mc);
            }
            // finishBatch is called by handleRendering when all pages are done (JVM exits)
        } else {
            // Single-page mode
            renderSinglePage();
        }
    }

    /**
     * Execute a single-page render and exit the JVM with the appropriate code.
     *
     * <p>
     * Extracted from the old {@code handleWorldStable} single-page path.
     */
    private void renderSinglePage() {
        GuideDebugLog.infoAlways("[GuideNH] [HeadlessRender] Rendering page...");
        try {
            RenderPageService.ensureFontEngineReady();
            var req = new RenderPageService.RenderPageRequest(
                config.guideId(),
                config.pageId(),
                config.mdFile(),
                config.language(),
                config.width(),
                config.outDir(),
                config.emitBoundsJson(),
                config.emitDebugOverlay(),
                config.scale(),
                config.chrome());
            RenderPageService.RenderPageResult result = RenderPageService.render(req);

            String message = "[GuideNH] [HeadlessRender] Screenshot written: " + result
                .pngPath() + " (" + result.widthPx() + "x" + result.heightPx() + ")";
            GuideDebugLog.infoAlways(message);
            System.out.println(message);

            FMLCommonHandler.instance()
                .exitJava(0, false);
        } catch (RenderPageService.RenderPageException e) {
            GuideDebugLog
                .error("[GuideNH] [HeadlessRender] Render failed at stage {}: {}", e.getStage(), e.getMessage());
            System.err
                .println("[GuideNH] [HeadlessRender] Render failed at stage " + e.getStage() + ": " + e.getMessage());
            FMLCommonHandler.instance()
                .exitJava(1, false);
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] [HeadlessRender] Unhandled exception during render", t);
            System.err.println("[GuideNH] [HeadlessRender] Unhandled exception: " + t.getMessage());
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }
}
