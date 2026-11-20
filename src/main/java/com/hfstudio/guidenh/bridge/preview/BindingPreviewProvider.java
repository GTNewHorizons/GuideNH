package com.hfstudio.guidenh.bridge.preview;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.client.GuideClientTasks;
import com.hfstudio.guidenh.guide.internal.binding.BindingQueries;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public class BindingPreviewProvider implements RuntimePreviewProvider {

    private final ItemPreviewService itemPreviews;
    private final BridgeProtocolLimits limits;

    public BindingPreviewProvider(ItemPreviewService itemPreviews, BridgeProtocolLimits limits) {
        this.itemPreviews = itemPreviews;
        this.limits = limits;
    }

    @Override
    public String capability() {
        return SemanticCapability.BINDINGS;
    }

    @Override
    public PreviewSearchResult search(PreviewSearchQuery query) {
        int offset = 0;
        try {
            offset = Math.max(0, Integer.parseInt(query.getCursor()));
        } catch (NumberFormatException ignored) {}
        var result = BindingQueries.search(
            query.getFilters()
                .get("type"),
            query.getPrefix(),
            offset,
            query.getLimit());
        var entries = result.entries()
            .stream()
            .map(
                display -> new PreviewSearchEntry(
                    display.key()
                        .id(),
                    BindingRuntime.title(display),
                    display.key()
                        .type(),
                    display.key()
                        .type() + "|"
                        + display.key()
                            .id(),
                    "binding"))
            .toList();
        int end = offset + entries.size();
        return new PreviewSearchResult(
            capability(),
            (int) Math.max(1, result.revision() & Integer.MAX_VALUE),
            entries,
            end < result.total() ? Integer.toString(end) : null);
    }

    @Override
    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        var key = BindingKey.parse(
            query.getFilters()
                .getOrDefault("type", ""),
            query.getId());
        return GuideClientTasks.call(() -> {
            var display = BindingRuntime.describe(key, BindingRuntime.view(true, true));
            String png = "";
            int width = 0, height = 0;
            var icon = display.icon();
            if (icon != null && icon.itemId() != null) {
                var item = itemPreviews.resolve(
                    new PreviewResolveQuery(
                        "items",
                        icon.itemId() + ":" + icon.meta(),
                        1,
                        icon.nbt(),
                        "inline",
                        Map.of()));
                png = item.getIconPngBase64();
                width = item.getPixelWidth();
                height = item.getPixelHeight();
            } else if (icon != null && icon.texture() != null) {
                try (var resource = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(icon.texture())
                    .getInputStream()) {
                    int maximum = Math.max(24, (limits.getMaxPreviewResolveBytes() - 4096) * 3 / 4);
                    byte[] bytes = resource.readNBytes(maximum + 1);
                    if (bytes.length >= 24 && bytes.length <= maximum
                        && ByteBuffer.wrap(bytes)
                            .getLong() == 0x89504e470d0a1a0aL) {
                        var header = ByteBuffer.wrap(bytes);
                        width = header.getInt(16);
                        height = header.getInt(20);
                        if (width > 0 && height > 0 && (long) width * height <= limits.getMaxPreviewIconPixels())
                            png = Base64.getEncoder()
                                .encodeToString(bytes);
                        else {
                            width = 0;
                            height = 0;
                        }
                    }
                } catch (IOException ignored) {}
            }
            return new BindingPreviewResult(display, png, width, height);
        });
    }
}
