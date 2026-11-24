package com.hfstudio.guidenh.client.command;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.client.GuideNhClientTaskScheduler;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureImportService;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureData;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureFileStore;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore;
import com.hfstudio.guidenh.network.GuideNhCustomPayloadLimits;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhRegionExportReplyMessage;
import com.hfstudio.guidenh.network.GuideNhRegionExportRequestMessage;
import com.hfstudio.guidenh.network.GuideNhStructureRequestMessage;
import com.hfstudio.guidenh.network.GuideNhStructureRequestSender;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientBridgeController {

    public static final GuideNhClientBridgeController INSTANCE = new GuideNhClientBridgeController();

    private final SceneEditorStructureImportService structureImportService;
    private final GuideStructureFileStore structureFileStore;
    private final AtomicInteger nextRegionExportRequestId;
    private final Map<Integer, CompletableFuture<String>> pendingRegionExports;
    private final Map<Integer, RegionReplyAssembler> pendingRegionExportChunks;
    private final Set<String> rememberedSceneLabels;

    private CompletableFuture<SceneEditorStructureImportService.ImportResult> pendingImport;
    private PendingImportRequest pendingImportRequest;
    private GuiYesNo pendingImportConfirmation;

    private GuideNhClientBridgeController() {
        this.structureImportService = new SceneEditorStructureImportService(SceneEditorStructureCache.createDefault());
        this.structureFileStore = GuideStructureFileStore.createDefault();
        this.nextRegionExportRequestId = new AtomicInteger(1);
        this.pendingRegionExports = new HashMap<>();
        this.pendingRegionExportChunks = new HashMap<>();
        this.rememberedSceneLabels = new HashSet<>();
    }

    public static GuideNhClientBridgeController getInstance() {
        return INSTANCE;
    }

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public boolean isServerStructureCommandsAvailable() {
        return GuideNhStructureRuntime.isServerStructureCommandsAvailable();
    }

    public Path exportStructureToFile(String prefix, String structureText) throws Exception {
        return structureFileStore.saveExport(prefix, structureText);
    }

    public void beginImportStructure(int x, int y, int z, String filePath) {
        if (!isServerStructureCommandsAvailable()) {
            sendClient(GuidebookText.CommandStructureServerRequired);
            return;
        }
        if (pendingImport != null) {
            sendClient(GuidebookText.CommandStructureImportPending);
            return;
        }
        NetHandlerPlayClient connection = Minecraft.getMinecraft()
            .getNetHandler();
        if (connection == null) {
            sendClient(GuidebookText.CommandStructureServerRequired);
            return;
        }
        try {
            if (filePath == null || filePath.replace('\\', '/')
                .startsWith("//")) {
                throw new IllegalArgumentException("Network structure paths are not allowed");
            }
            pendingImport = structureImportService.importFromPathAsync(Paths.get(filePath));
            pendingImportRequest = new PendingImportRequest(x, y, z, connection);
        } catch (Exception e) {
            sendClient(GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
    }

    public void placeAllStructures(int x, int y, int z) {
        if (!isServerStructureCommandsAvailable()) {
            sendClient(GuidebookText.CommandStructureServerRequired);
            return;
        }
        syncAllClientStructuresToServer();
        GuideNhStructureRequestSender.sendPlaceAll(GuideNhNetwork.channel(), x, y, z);
    }

    public void rememberScene(String label, String structureText) {
        try {
            GuideNhStructureRuntime.getClientMemoryStore()
                .remember(label, structureText);
        } catch (Exception e) {
            // Silently ignore parse failures for auto-registered scenes
        }
    }

    public void rememberScene(String label, GuideStructureData structureData) {
        if (structureData == null || label == null || label.isEmpty()) {
            return;
        }
        if (!rememberedSceneLabels.add(label)) {
            return;
        }
        GuideNhStructureRuntime.getClientMemoryStore()
            .remember(label, structureData);
    }

    public boolean hasRememberedScene(String label) {
        return label != null && rememberedSceneLabels.contains(label);
    }

    public void onServerHello() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(true);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
    }

    public void onServerDisconnected() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(false);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
        clearPendingImport();
        rememberedSceneLabels.clear();
        for (CompletableFuture<String> future : pendingRegionExports.values()) {
            future.complete(null);
        }
        pendingRegionExports.clear();
        pendingRegionExportChunks.clear();
    }

    public CompletableFuture<String> requestRegionExport(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
        boolean includeEntities) {
        if (!isServerStructureCommandsAvailable()) {
            return CompletableFuture.completedFuture(null);
        }
        int requestId = nextRegionExportRequestId.getAndIncrement();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRegionExports.put(requestId, future);
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        int dimensionId = player != null && player.worldObj != null && player.worldObj.provider != null
            ? player.worldObj.provider.dimensionId
            : 0;
        GuideNhNetwork.channel()
            .sendToServer(
                new GuideNhRegionExportRequestMessage(
                    requestId,
                    dimensionId,
                    x,
                    y,
                    z,
                    sizeX,
                    sizeY,
                    sizeZ,
                    includeEntities));
        return future;
    }

    public void handleRegionExportReply(GuideNhRegionExportReplyMessage message) {
        CompletableFuture<String> future = pendingRegionExports.get(message.getRequestId());
        if (future == null) {
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_ERROR) {
            pendingRegionExports.remove(message.getRequestId());
            pendingRegionExportChunks.remove(message.getRequestId());
            future.complete(null);
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_COMPLETE) {
            pendingRegionExports.remove(message.getRequestId());
            pendingRegionExportChunks.remove(message.getRequestId());
            future.complete(message.getPayloadText());
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_CHUNK) {
            try {
                RegionReplyAssembler assembler = pendingRegionExportChunks.computeIfAbsent(
                    message.getRequestId(),
                    ignored -> new RegionReplyAssembler(message.getChunkCount()));
                String completed = assembler.accept(message);
                if (completed == null) {
                    return;
                }
                pendingRegionExports.remove(message.getRequestId());
                pendingRegionExportChunks.remove(message.getRequestId());
                future.complete(completed);
            } catch (IllegalArgumentException e) {
                pendingRegionExports.remove(message.getRequestId());
                pendingRegionExportChunks.remove(message.getRequestId());
                future.complete(null);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (GuideNhStructureRuntime.isClientStructureSyncNeeded()) {
            GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
            syncAllClientStructuresToServer();
        }
        if (pendingImport == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (pendingImportRequest == null || pendingImportRequest.connection() != minecraft.getNetHandler()) {
            clearPendingImport();
            return;
        }
        if (pendingImportConfirmation != null) {
            if (minecraft.currentScreen != pendingImportConfirmation) {
                clearPendingImport();
                sendClient(GuidebookText.CommandStructureImportCanceled);
            }
            return;
        }
        if (!pendingImport.isDone()) {
            return;
        }

        CompletableFuture<SceneEditorStructureImportService.ImportResult> future = pendingImport;
        PendingImportRequest request = pendingImportRequest;

        try {
            SceneEditorStructureImportService.ImportResult result = future.join();
            if (result == null) {
                clearPendingImport();
                sendClient(GuidebookText.CommandStructureImportCanceled);
                return;
            }
            GuiScreen previousScreen = minecraft.currentScreen;
            pendingImportConfirmation = new GuiYesNo(
                (confirmed, id) -> completeImport(confirmed, future, request, result, previousScreen),
                GuidebookText.CommandStructureUploadConfirm.text(),
                result.getDisplayPath(),
                0);
            minecraft.displayGuiScreen(pendingImportConfirmation);
        } catch (CompletionException e) {
            clearPendingImport();
            sendClient(
                GuidebookText.CommandStructureImportFailure,
                getErrorMessage(e.getCause() != null ? e.getCause() : e));
        } catch (Exception e) {
            clearPendingImport();
            sendClient(GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
    }

    private void completeImport(boolean confirmed,
        CompletableFuture<SceneEditorStructureImportService.ImportResult> future, PendingImportRequest request,
        SceneEditorStructureImportService.ImportResult result, GuiScreen previousScreen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (pendingImport != future || pendingImportRequest != request
            || minecraft.currentScreen != pendingImportConfirmation) {
            return;
        }
        boolean sameConnection = request.connection() == minecraft.getNetHandler();
        clearPendingImport();
        minecraft.displayGuiScreen(sameConnection ? previousScreen : null);
        if (!confirmed || !sameConnection || !isServerStructureCommandsAvailable()) {
            sendClient(GuidebookText.CommandStructureImportCanceled);
            return;
        }
        try {
            var entry = GuideNhStructureRuntime.getClientMemoryStore()
                .remember(result.getDisplayPath(), result.getStructureText());
            GuideNhStructureRequestSender.sendImportAndPlace(
                GuideNhNetwork.channel(),
                request.x(),
                request.y(),
                request.z(),
                entry.getStructureText());
        } catch (Exception e) {
            sendClient(GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
    }

    private void clearPendingImport() {
        if (pendingImport != null) {
            pendingImport.cancel(false);
        }
        pendingImport = null;
        pendingImportRequest = null;
        pendingImportConfirmation = null;
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        GuideNhClientTaskScheduler.execute(this::onServerDisconnected);
    }

    private void syncAllClientStructuresToServer() {
        var entries = GuideNhStructureRuntime.getClientMemoryStore()
            .snapshotEntries();
        if (entries.isEmpty()) {
            return;
        }
        for (var entry : entries) {
            syncEntryToServerIfAvailable(entry);
        }
    }

    private void syncEntryToServerIfAvailable(GuideStructureMemoryStore.Entry entry) {
        if (!isServerStructureCommandsAvailable()) {
            return;
        }
        GuideNhStructureRequestSender.sendCache(GuideNhNetwork.channel(), entry.getStructureText());
    }

    private void sendClient(GuidebookText key, Object... args) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
        }
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    public record PendingImportRequest(int x, int y, int z, NetHandlerPlayClient connection) {}

    public static class RegionReplyAssembler {

        private final byte[][] chunks;
        private int received;
        private int totalBytes;

        private RegionReplyAssembler(int chunkCount) {
            if (chunkCount <= 0 || chunkCount > GuideNhStructureRequestMessage.MAX_CHUNKS_PER_STRUCTURE) {
                throw new IllegalArgumentException("Invalid region export chunk count: " + chunkCount);
            }
            this.chunks = new byte[chunkCount][];
        }

        private synchronized String accept(GuideNhRegionExportReplyMessage message) {
            int index = message.getChunkIndex();
            if (message.getChunkCount() != chunks.length || index < 0 || index >= chunks.length) {
                throw new IllegalArgumentException("Inconsistent region export chunks");
            }
            byte[] bytes = message.getPayloadBytes();
            if (bytes == null || bytes.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET) {
                throw new IllegalArgumentException("Invalid region export chunk size");
            }
            if (chunks[index] == null) {
                if (bytes.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_TRANSFER_BYTES - totalBytes) {
                    throw new IllegalArgumentException("Region export exceeds the transfer limit");
                }
                chunks[index] = bytes;
                received++;
                totalBytes += bytes.length;
            }
            if (received != chunks.length) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    return null;
                }
                out.write(chunk, 0, chunk.length);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
