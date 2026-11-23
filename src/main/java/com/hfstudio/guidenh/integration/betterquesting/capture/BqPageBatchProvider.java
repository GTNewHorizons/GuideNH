package com.hfstudio.guidenh.integration.betterquesting.capture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.integration.betterquesting.capture.BqCaptureCanvasTree.Node;
import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqCanvas;
import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqQuest;
import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqQuestLines;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.api2.utils.Tuple2;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;

public class BqPageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "betterquesting_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof GuiHome
            || context.capture()
                .screen() instanceof GuiQuestLines
            || context.capture()
                .screen() instanceof GuiQuest;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        GuiScreenCanvas screen = (GuiScreenCanvas) context.capture()
            .screen();
        String reason = Minecraft.getMinecraft().thePlayer == null ? "A player is required to browse quests"
            : ((AccessorCaptureBqCanvas) screen).guidenh$getCapturePopup() != null
                ? "Close the BQ popup before page navigation"
                : "";
        if (!reason.isEmpty() || !(screen instanceof GuiQuestLines lines)) return List.of(
            new CaptureTargetDescriptor(
                "bq:chapters",
                id(),
                CaptureBatchOptions.PAGES,
                I18n.format("guidenh.capture.bq.chapters"),
                reason.isEmpty() ? "ready" : "unavailable",
                reason,
                Map.of("kind", "index")));
        return pages(lines, options);
    }

    private static List<CaptureTargetDescriptor> pages(GuiQuestLines screen, CaptureBatchOptions options) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        UUID playerId = QuestingAPI.getQuestingUUID(player);
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        Set<UUID> quests = new HashSet<>();
        for (Tuple2<Map.Entry<UUID, IQuestLine>, Integer> entry : ((AccessorCaptureBqQuestLines) screen)
            .guidenh$getCaptureChapters()) {
            boolean available = (entry.getSecond() & 4) == 0;
            if (!available && BQ_Settings.hideLockedQuestLines) continue;
            UUID chapter = entry.getFirst()
                .getKey();
            targets.add(
                new CaptureTargetDescriptor(
                    "bq:chapter:" + chapter,
                    "betterquesting_pages",
                    CaptureBatchOptions.PAGES,
                    QuestTranslation.translateQuestLineName(entry.getFirst()),
                    available ? "ready" : "unavailable",
                    available ? "" : "This chapter is visible but its navigation is locked",
                    Map.of("kind", "chapter", "chapter", chapter.toString())));
            if (!available) continue;
            for (UUID questId : entry.getFirst()
                .getValue()
                .keySet()) {
                IQuest quest = QuestDatabase.INSTANCE.get(questId);
                if (!QuestCache.isQuestShown(quest, playerId, player)
                    || !options.includeVariants() && !quests.add(questId)) continue;
                targets.add(
                    new CaptureTargetDescriptor(
                        "bq:quest:" + (options.includeVariants() ? chapter + "/" : "") + questId,
                        "betterquesting_pages",
                        CaptureBatchOptions.PAGES,
                        QuestTranslation.translateQuestName(questId, quest),
                        "ready",
                        "",
                        Map.of("kind", "quest", "chapter", chapter.toString(), "quest", questId.toString())));
            }
        }
        return List.copyOf(targets);
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new PageNavigator(
            (GuiScreenCanvas) context.capture()
                .screen(),
            options);
    }

    public static class PageNavigator implements CaptureBatchNavigator {

        private final GuiScreenCanvas origin;
        private final GuiQuestLines reader;
        private final CaptureBatchOptions options;
        private final BqCaptureState original;
        private GuiScreenCanvas active;
        private BqCanvasState pageState;
        private CaptureTargetDescriptor selected;
        private String page = "";
        private String canvasPath;
        private int x;
        private int y;
        private long nodesFrame = Long.MIN_VALUE;
        private Map<String, Node> frameNodes = Map.of();
        private boolean restored;

        public PageNavigator(GuiScreenCanvas origin, CaptureBatchOptions options) {
            this.origin = origin;
            this.options = options;
            original = new BqCaptureState(origin);
            reader = origin instanceof GuiQuestLines lines ? lines : new GuiQuestLines(origin);
            reader.setPreviousScreen(((AccessorCaptureBqCanvas) origin).guidenh$getCapturePreviousScreen());
        }

        private void show(GuiScreenCanvas screen) {
            active = screen;
            if (Minecraft.getMinecraft().currentScreen != screen) Minecraft.getMinecraft()
                .displayGuiScreen(screen);
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            String kind = target.data()
                .get("kind");
            String key = kind + ":"
                + target.data()
                    .getOrDefault("chapter", "")
                + ":"
                + target.data()
                    .getOrDefault("quest", "");
            if (!key.equals(page)) {
                show(reader);
                if (!kind.equals("index")) {
                    UUID chapterId = UUID.fromString(
                        target.data()
                            .get("chapter"));
                    IQuestLine chapter = QuestLineDatabase.INSTANCE.get(chapterId);
                    boolean visible = ((AccessorCaptureBqQuestLines) reader).guidenh$getCaptureChapters()
                        .stream()
                        .anyMatch(
                            entry -> entry.getFirst()
                                .getKey()
                                .equals(chapterId) && (entry.getSecond() & 4) == 0);
                    if (chapter == null || !visible)
                        throw new IllegalStateException("The selected BQ chapter is no longer accessible");
                    ((AccessorCaptureBqQuestLines) reader).guidenh$openCaptureChapter(Map.entry(chapterId, chapter));
                    if (kind.equals("quest")) {
                        UUID questId = UUID.fromString(
                            target.data()
                                .get("quest"));
                        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                        if (!QuestCache.isQuestShown(
                            QuestDatabase.INSTANCE.get(questId),
                            QuestingAPI.getQuestingUUID(player),
                            player)) throw new IllegalStateException("The selected BQ quest is no longer visible");
                        original.touchQuest(questId);
                        show(new GuiQuest(reader, questId));
                    }
                }
                page = key;
                pageState = null;
            }
            selected = target;
            canvasPath = target.data()
                .get("canvas");
            nodesFrame = Long.MIN_VALUE;
            if (canvasPath != null) {
                pageState.restore(active);
                Node node = BqCaptureCanvasTree
                    .collect(active, new CaptureRect(0, 0, active.width, active.height), false)
                    .get(canvasPath);
                if (node == null || !(node.panel() instanceof CanvasScrolling canvas))
                    throw new IllegalStateException("The selected BQ scroll canvas disappeared");
                x = Integer.parseInt(
                    target.data()
                        .get("x"));
                y = Integer.parseInt(
                    target.data()
                        .get("y"));
                canvas.setZoom(
                    Float.parseFloat(
                        target.data()
                            .get("zoom")));
                canvas.setScrollX(x);
                canvas.setScrollY(y);
                canvas.updatePanelScroll();
            }
        }

        private Map<String, Node> nodes(CaptureBatchContext context) {
            if (nodesFrame != context.capture()
                .frameId()) {
                frameNodes = BqCaptureCanvasTree.collect(
                    active,
                    context.capture()
                        .viewport()
                        .bounds(),
                    false);
                nodesFrame = context.capture()
                    .frameId();
            }
            return frameNodes;
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == active
                && !active.getChildren()
                    .isEmpty();
            if (canvasPath != null) {
                Node node = nodes(context).get(canvasPath);
                ready &= node != null && !node.visibleBounds()
                    .isEmpty()
                    && node.panel() instanceof CanvasScrolling canvas
                    && canvas.getScrollX() == x
                    && canvas.getScrollY() == y;
            } else if (selected.data()
                .get("kind")
                .equals("quest")) {
                    ready &= active instanceof GuiQuest && ((AccessorCaptureBqQuest) active).guidenh$getCaptureQuestId()
                        .equals(
                            UUID.fromString(
                                selected.data()
                                    .get("quest")));
                }
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for BQ page layout and scroll content");
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            return List.of(
                canvasPath == null ? context.capture()
                    .viewport()
                    .bounds()
                    : nodes(context).get(canvasPath)
                        .visibleBounds());
        }

        @Override
        public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
            if (canvasPath == null) return null;
            Node node = nodes(context).get(canvasPath);
            CanvasScrolling canvas = (CanvasScrolling) node.panel();
            BqScrollExtent extent = BqScrollExtent.from(canvas);
            double zoom = canvas.getZoom();
            return new CaptureScrollGeometry(
                page + ":" + canvasPath,
                node.visibleBounds(),
                (canvas.getScrollX() - extent.x()) * zoom * node.scaleX() + node.visibleBounds()
                    .x()
                    - node.bounds()
                        .x(),
                (canvas.getScrollY() - extent.y()) * zoom * node.scaleY() + node.visibleBounds()
                    .y()
                    - node.bounds()
                        .y(),
                extent.width() * zoom * node.scaleX(),
                extent.height() * zoom * node.scaleY());
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
            CaptureBatchContext context) {
            if (canvasPath != null) return List.of();
            pageState = new BqCanvasState(active);
            List<CaptureTargetDescriptor> targets = new ArrayList<>();
            for (Map.Entry<String, Node> entry : nodes(context).entrySet()) {
                Node node = entry.getValue();
                if (!(node.panel() instanceof CanvasScrolling canvas) || node.visibleBounds()
                    .isEmpty()) continue;
                BqScrollExtent extent = BqScrollExtent.from(canvas);
                IGuiRect rectangle = canvas.getTransform();
                int stepX = Math.max(1, (int) Math.floor(rectangle.getWidth() / canvas.getZoom()));
                int stepY = Math.max(1, (int) Math.floor(rectangle.getHeight() / canvas.getZoom()));
                String role = role(canvas);
                for (int top : offsets(extent.minimumY(), extent.maximumY() - extent.minimumY(), stepY)) {
                    for (int left : offsets(extent.minimumX(), extent.maximumX() - extent.minimumX(), stepX)) {
                        Map<String, String> data = new LinkedHashMap<>(target.data());
                        data.put("canvas", entry.getKey());
                        data.put("x", Integer.toString(left));
                        data.put("y", Integer.toString(top));
                        data.put("zoom", Float.toString(canvas.getZoom()));
                        data.put("role", role);
                        targets.add(
                            new CaptureTargetDescriptor(
                                target.id() + "/canvas:" + entry.getKey() + "/" + left + ":" + top,
                                target.providerId(),
                                target.scope(),
                                target.label() + " · "
                                    + I18n.format("guidenh.capture.bq." + role)
                                    + " ["
                                    + left
                                    + ", "
                                    + top
                                    + "]",
                                "ready",
                                "",
                                data));
                    }
                }
            }
            if (target.data()
                .get("kind")
                .equals("index")) targets.addAll(pages(reader, options));
            return List.copyOf(targets);
        }

        private String role(CanvasScrolling canvas) {
            if (active instanceof GuiQuest) {
                AccessorCaptureBqQuest quest = (AccessorCaptureBqQuest) active;
                if (quest.guidenh$getCaptureTasks() == canvas) return "tasks";
                if (quest.guidenh$getCaptureRewards() == canvas) return "rewards";
                if (quest.guidenh$getCaptureDescription() == canvas) return "description";
            } else if (active instanceof GuiQuestLines) {
                AccessorCaptureBqQuestLines lines = (AccessorCaptureBqQuestLines) active;
                if (lines.guidenh$getCaptureQuestCanvas() == canvas) return "map";
                if (lines.guidenh$getCaptureChapterList() == canvas) return "chapters";
                if (lines.guidenh$getCaptureDescription() == canvas) return "description";
            }
            return "content";
        }

        private static List<Integer> offsets(int minimum, int span, int step) {
            List<Integer> offsets = new ArrayList<>();
            offsets.add(minimum);
            for (int offset = step; offset < span; offset += step) offsets.add(minimum + offset);
            if (span > 0) offsets.add(minimum + span);
            return offsets;
        }

        @Override
        public void restore() {
            original.restoreGlobals();
            show(origin);
            original.restoreCanvas(origin);
            restored = true;
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == origin && original.restored(origin);
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the original BQ view");
        }

        @Override
        public void close() {
            if (restored) original.restoreGlobals();
            frameNodes = Map.of();
        }
    }
}
