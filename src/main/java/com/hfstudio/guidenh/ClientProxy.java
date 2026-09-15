package com.hfstudio.guidenh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridge;
import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridgeSettings;
import com.hfstudio.guidenh.client.GuideNhClientTaskScheduler;
import com.hfstudio.guidenh.client.RegionWandRenderer;
import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.client.hotkey.CycleRegionWandModeHotkey;
import com.hfstudio.guidenh.client.hotkey.GuidePageHistoryHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHomeHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenSceneEditorHotkey;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.DefaultGuideResourcePackManager;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePackWatcher;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideOnStartup;
import com.hfstudio.guidenh.guide.internal.GuideReloadListener;
import com.hfstudio.guidenh.guide.internal.compile.CompileWorker;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.host.LytHostWorkItem;
import com.hfstudio.guidenh.guide.internal.host.scripts.BlockImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CategoryScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CommandLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CsvTableScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.FloatingImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemGridScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.KeyBindScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.MermaidScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.PlayerNameScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.QuestCardScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.QuestLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.RecipeScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SceneScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SoundLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SpecialScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.StructureScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SubPagesScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.TooltipScript;
import com.hfstudio.guidenh.guide.internal.item.RegionWandSelection;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.ElkWarmupWorkItem;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookFakeRenderEnvironment;
import com.hfstudio.guidenh.guide.internal.scheduler.DevWatchWorkItem;
import com.hfstudio.guidenh.guide.internal.scheduler.MasterScheduler;
import com.hfstudio.guidenh.guide.internal.scheduler.SearchIndexWorkItem;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookFakeWorld;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.GuideNhClientIntegrationBootstrap;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.network.Ae2NetworkRegistration;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge;
import com.hfstudio.guidenh.network.GuideNhClientBridgeHandler;
import com.hfstudio.guidenh.network.GuideNhClientBridgeMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhRegionExportClientHandler;
import com.hfstudio.guidenh.network.GuideNhRegionExportReplyMessage;
import com.hfstudio.structurelibexport.StructureExportBootstrap;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import lombok.Getter;

public class ClientProxy extends CommonProxy {

    @Getter
    private static final LytHost lytHost = new LytHost();
    private static final CompileWorker compileWorker = new CompileWorker();

    public static CompileWorker getWorker() {
        return compileWorker;
    }

    private final GuideNhRuntimeBridge runtimeBridge = new GuideNhRuntimeBridge();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuideNhClientTaskScheduler.initialize();
        GuidebookLevel.setPreviewWorldFactory(GuidebookFakeWorld::new);
        GuidebookLevel.setDefaultBuildHeightProvider(() -> {
            var world = Minecraft.getMinecraft().theWorld;
            // TODO: Read minimum/maximum build heights from the CubicChunks API when it is an explicit dependency.
            int maxHeight = world != null ? world.getHeight() : GuidebookLevel.DEFAULT_MAX_BUILD_HEIGHT_EXCLUSIVE;
            return new GuidebookLevel.BuildHeightBounds(GuidebookLevel.DEFAULT_MIN_BUILD_HEIGHT, maxHeight);
        });
        GuideNhClientIntegrationBootstrap.preInitClient();
        GuideME.initClientProxy();
        GuideNhNetwork.channel()
            .registerMessage(GuideNhClientBridgeHandler.class, GuideNhClientBridgeMessage.class, 2, Side.CLIENT);
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhRegionExportClientHandler.class,
                GuideNhRegionExportReplyMessage.class,
                8,
                Side.CLIENT);
        Ae2NetworkRegistration.registerClientMessages();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        RegionWandSelection.reloadBindings();
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new GuideReloadListener());
        DefaultGuideResourcePackManager.init();
        ClientCommandHandler.instance.registerCommand(new GuideNhClientCommand());
        StructureExportBootstrap.registerClientCommands();
        GuideNhClientBridgeController.init();
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiBridge.init();
        }
        OpenGuideHomeHotkey.init();
        OpenGuideHotkey.init();
        GuidePageHistoryHotkey.init();
        OpenSceneEditorHotkey.init();

        CycleRegionWandModeHotkey.init();
        MinecraftForge.EVENT_BUS.register(new RegionWandRenderer());
        MasterScheduler.init();
        MasterScheduler.getInstance()
            .submit(new LytHostWorkItem(lytHost));
        MasterScheduler.getInstance()
            .submit(new SearchIndexWorkItem());
        MasterScheduler.getInstance()
            .submit(new ElkWarmupWorkItem());

        // Phase 3: LytScript registrations
        lytHost.registerScript("CommandLink", new CommandLinkScript());
        lytHost.registerScript("Img", new ImageScript());
        lytHost.registerScript("FloatingImage", new FloatingImageScript());
        lytHost.registerScript("PlayerName", new PlayerNameScript());
        lytHost.registerScript("KeyBind", new KeyBindScript());
        lytHost.registerScript("SoundLink", new SoundLinkScript());
        lytHost.registerScript("Structure", new StructureScript());
        lytHost.registerScript("SubPages", new SubPagesScript());
        lytHost.registerScript("Tooltip", new TooltipScript());
        lytHost.registerScript("ItemGrid", new ItemGridScript());
        lytHost.registerScript("ItemImage", new ItemImageScript());
        lytHost.registerScript("ItemLink", new ItemLinkScript());
        lytHost.registerScript("Category", new CategoryScript());
        lytHost.registerScript("Special", new SpecialScript());
        lytHost.registerScript("BlockImage", new BlockImageScript());
        lytHost.registerScript("CsvTable", new CsvTableScript());
        lytHost.registerScript("Mermaid", new MermaidScript());
        lytHost.registerScript("QuestLink", new QuestLinkScript());
        lytHost.registerScript("QuestCard", new QuestCardScript());
        // Phase 3: SceneScript handles Scene and GameScene
        SceneScript sceneScript = new SceneScript();
        lytHost.registerScript("Scene", sceneScript);
        lytHost.registerScript("GameScene", sceneScript);
        // Phase 3: RecipeScript handles Recipe, Usage, RecipeFor, RecipeUsage, RecipesFor, RecipesUsage
        RecipeScript recipeScript = new RecipeScript();
        lytHost.registerScript("Recipe", recipeScript);
        lytHost.registerScript("Usage", recipeScript);
        lytHost.registerScript("RecipeFor", recipeScript);
        lytHost.registerScript("RecipeUsage", recipeScript);
        lytHost.registerScript("RecipesFor", recipeScript);
        lytHost.registerScript("RecipesUsage", recipeScript);

        MinecraftForge.EVENT_BUS.register(this);
        if (ModConfig.runtimeBridge.enabled) {
            GuideDebugLog.infoAlways(
                "GuideNH runtime bridge configuration loaded. hostConfigured={}, port={}, tokenConfigured={}",
                ModConfig.runtimeBridge.host != null && !ModConfig.runtimeBridge.host.trim()
                    .isEmpty(),
                ModConfig.runtimeBridge.port,
                ModConfig.runtimeBridge.token != null && !ModConfig.runtimeBridge.token.isEmpty());
            runtimeBridge.start(
                new GuideNhRuntimeBridgeSettings(
                    ModConfig.runtimeBridge.enabled,
                    ModConfig.runtimeBridge.host,
                    ModConfig.runtimeBridge.port,
                    ModConfig.runtimeBridge.token,
                    ModConfig.runtimeBridge.maxMessageBytes,
                    ModConfig.runtimeBridge.maxPageSize,
                    ModConfig.runtimeBridge.maxSubscriptions,
                    ModConfig.runtimeBridge.maxConnections,
                    ModConfig.runtimeBridge.maxDeltaEntries));
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
        GuideDevelopmentResourcePackWatcher.init();
        GuideReloadListener.markBootComplete();
        MasterScheduler.getInstance()
            .submit(new DevWatchWorkItem());
        GuideOnStartup.init();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clearClientRuntimeState("server disconnect");
        runtimeBridge.stop();
        GuideME.closeSearch();
        lytHost.getNavigation()
            .clear();
    }

    @SubscribeEvent
    public void onClientWorldUnload(WorldEvent.Unload event) {
        if (!event.world.isRemote) {
            return;
        }
        clearClientRuntimeState("world unload");
    }

    /**
     * WorldClient is reachable from tile entities, entities, RenderBlocks, and the preview
     * player. Clear all GuideNH-owned links at both lifecycle boundaries; the operation is
     * intentionally idempotent because Forge can emit unload and disconnect in either order.
     */
    private static void clearClientRuntimeState(String reason) {
        if (!GuideNhClientTaskScheduler.isOnClientThread()) {
            GuideNhClientTaskScheduler.execute(() -> clearClientRuntimeState(reason));
            return;
        }
        GuidebookLevel.releaseAllRuntimeWorlds();
        GuidebookLevelRenderer.getInstance()
            .clearClientRuntimeCaches();
        GuidebookFakeRenderEnvironment.clearClientRuntimeCaches();
        getWorker().clearPendingRuntimeReleases();
        GuideDebugLog.debug("[GuideNH] Cleared client preview runtime state ({})", reason);
    }
}
