package com.hfstudio.guidenh.guide.internal.headless;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Headless screenshot render driver activated by {@code -Dguidenh.headlessRender=true}.
 *
 * <p>State machine (driven by {@link TickEvent.ClientTickEvent}):
 * <ol>
 *   <li>{@code IDLE} — wait for main menu, then close screen, launch integrated server</li>
 *   <li>{@code LOADING_WORLD} — poll until {@code theWorld / thePlayer / netHandler} are non-null</li>
 *   <li>{@code WORLD_STABLE} — wait 20 ticks, then call {@link RenderPageService#render}</li>
 *   <li>{@code DONE} — guard against re-entry</li>
 * </ol>
 *
 * <p>Watchdog: 360 second timeout → {@code exitJava(2)}.
 *
 * <p>All errors → {@code exitJava(1)}.  Never swallow exceptions.
 *
 * @see RenderPageService
 */
public class GuideNhHeadlessRenderDriver {

    // ---- config --------------------------------------------------------------

    /** Immutable snapshot of JVM property configuration. */
    public record HeadlessRenderConfig(
        String guideId,
        @Nullable String pageId,
        @Nullable Path mdFile,
        int width,
        Path outDir,
        String language,
        boolean emitBoundsJson,
        boolean emitDebugOverlay,
        String worldName
    ) {}

    // ---- state machine -------------------------------------------------------

    private enum State { IDLE, LOADING_WORLD, WORLD_STABLE, DONE }

    private State state = State.IDLE;
    private final HeadlessRenderConfig config;
    private final long watchdogStartNanos;
    private int stableTickCount = 0;
    private boolean executed = false;

    // ---- construction --------------------------------------------------------

    public GuideNhHeadlessRenderDriver(HeadlessRenderConfig config) {
        this.config = config;
        this.watchdogStartNanos = System.nanoTime();
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

        boolean hasPage = pageId != null && !pageId.isEmpty();
        boolean hasMd = mdProp != null && !mdProp.isEmpty();
        if (!hasPage && !hasMd) {
            logError("Either -Dguidenh.renderpage.page=<id> or -Dguidenh.renderpage.md=<path> must be specified");
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
            outDir = mc.mcDataDir.toPath().resolve("screenshots");
        }

        String lang = System.getProperty("guidenh.renderpage.lang", "en_us");
        boolean bounds = Boolean.parseBoolean(System.getProperty("guidenh.renderpage.bounds", "false"));
        boolean overlay = Boolean.parseBoolean(System.getProperty("guidenh.renderpage.overlay", "false"));
        String worldName = System.getProperty("guidenh.renderpage.world", "screenshot-world");

        return new HeadlessRenderConfig(
            guideId,
            hasPage ? pageId : null,
            hasMd ? Paths.get(mdProp) : null,
            width,
            outDir,
            lang,
            bounds,
            overlay,
            worldName
        );
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

        // Watchdog: 360 seconds absolute timeout
        if (System.nanoTime() - watchdogStartNanos > 360_000_000_000L) {
            GuideDebugLog.error("[GuideNH] [HeadlessRender] headless render timeout (360s exceeded)");
            FMLCommonHandler.instance().exitJava(2, false);
            return;
        }

        if (executed) {
            return;
        }

        try {
            tick();
        } catch (Throwable t) {
            GuideDebugLog.error("[GuideNH] [HeadlessRender] Unhandled exception in state machine", t);
            System.err.println("[GuideNH] [HeadlessRender] Unhandled exception: " + t.getMessage());
            FMLCommonHandler.instance().exitJava(1, false);
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
            default -> {}
        }
    }

    // ---- state handlers ------------------------------------------------------

    private void handleIdle(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            return;
        }

        state = State.LOADING_WORLD;
        GuideDebugLog.infoAlways(
            "[GuideNH] [HeadlessRender] Starting integrated server (world: {})",
            config.worldName());

        mc.displayGuiScreen(null);
        WorldSettings settings = new WorldSettings(
            0L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
        mc.launchIntegratedServer(config.worldName(), config.worldName(), settings);
    }

    private void handleLoadingWorld(Minecraft mc) {
        if (mc.theWorld != null && mc.thePlayer != null && mc.getNetHandler() != null) {
            state = State.WORLD_STABLE;
            stableTickCount = 0;
            GuideDebugLog.infoAlways(
                "[GuideNH] [HeadlessRender] World loaded, waiting 20 ticks for stability");
        }
    }

    private void handleWorldStable(Minecraft mc) {
        stableTickCount++;
        if (stableTickCount < 20) {
            return;
        }

        // Prevent any re-entry
        executed = true;
        state = State.DONE;

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
                config.emitDebugOverlay()
            );
            RenderPageService.RenderPageResult result = RenderPageService.render(req);

            String message = "[GuideNH] [HeadlessRender] Screenshot written: " + result.pngPath()
                + " (" + result.widthPx() + "x" + result.heightPx() + ")";
            GuideDebugLog.infoAlways(message);
            System.out.println(message);

            FMLCommonHandler.instance().exitJava(0, false);
        } catch (RenderPageService.RenderPageException e) {
            GuideDebugLog.error(
                "[GuideNH] [HeadlessRender] Render failed at stage {}: {}",
                e.getStage(), e.getMessage());
            System.err.println(
                "[GuideNH] [HeadlessRender] Render failed at stage " + e.getStage()
                    + ": " + e.getMessage());
            FMLCommonHandler.instance().exitJava(1, false);
        } catch (Throwable t) {
            GuideDebugLog.error(
                "[GuideNH] [HeadlessRender] Unhandled exception during render", t);
            System.err.println(
                "[GuideNH] [HeadlessRender] Unhandled exception: " + t.getMessage());
            FMLCommonHandler.instance().exitJava(1, false);
        }
    }
}
