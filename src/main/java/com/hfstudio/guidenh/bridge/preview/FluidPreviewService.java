package com.hfstudio.guidenh.bridge.preview;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraftforge.fluids.FluidContainerRegistry;

import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.client.GuideClientTasks;
import com.hfstudio.guidenh.guide.document.interaction.FluidTooltip;
import com.hfstudio.guidenh.guide.internal.fluid.FluidCatalog;
import com.hfstudio.guidenh.guide.internal.fluid.FluidIconRenderer;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStacks;
import com.hfstudio.guidenh.guide.render.GuideIconCapture;

public class FluidPreviewService implements RuntimePreviewProvider {

    private static final int CACHE_LIMIT = 256;
    private final BridgeProtocolLimits limits;
    private final Map<IconKey, String> icons = new LinkedHashMap<>(16, 0.75f, true);
    private final GuideIconCapture capture = new GuideIconCapture();
    private long resourceRevision = Long.MIN_VALUE;

    public record IconKey(FluidStackSpec fluid, long revision, int size) {}

    public FluidPreviewService(BridgeProtocolLimits limits) {
        this.limits = limits;
    }

    @Override
    public String capability() {
        return SemanticCapability.FLUIDS;
    }

    @Override
    public PreviewSearchResult search(PreviewSearchQuery query) {
        String prefix = query.getPrefix()
            .toLowerCase(Locale.ROOT);
        var entries = FluidCatalog.INSTANCE.entries()
            .stream()
            .filter(
                entry -> entry.searchText()
                    .contains(prefix))
            .map(
                entry -> new PreviewSearchEntry(
                    entry.id(),
                    entry.name(),
                    entry.id(),
                    "fluids|" + entry.id(),
                    "registry"))
            .toList();
        return PreviewSearchResult.page(capability(), entries, query.getCursor(), query.getLimit());
    }

    @Override
    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        return GuideClientTasks.call(() -> resolveOnClient(query));
    }

    private PreviewResolveResult resolveOnClient(PreviewResolveQuery query) {
        long revision = FluidCatalog.INSTANCE.resourceRevision();
        if (revision != resourceRevision) {
            icons.clear();
            capture.close();
            resourceRevision = revision;
        }
        int amount = query.getAmount() == null ? FluidContainerRegistry.BUCKET_VOLUME : query.getAmount();
        FluidStackSpec spec = new FluidStackSpec(query.getId(), amount, query.getNbt());
        var stack = FluidStacks.resolve(spec);
        int edge = "picker".equalsIgnoreCase(query.getRenderVariant()) ? 128 : 64;
        edge = Math.clamp(edge, 16, (int) Math.sqrt(limits.getMaxPreviewIconPixels()));
        IconKey key = new IconKey(spec, revision, edge);
        String png = icons.get(key);
        if (png == null) {
            int size = edge;
            BufferedImage image = capture.capture(size, () -> FluidIconRenderer.draw(stack, 0, 0, size, false));
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(image, "png", bytes);
                png = Base64.getEncoder()
                    .encodeToString(bytes.toByteArray());
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to encode fluid preview", exception);
            }
            icons.put(key, png);
            if (icons.size() > CACHE_LIMIT) icons.remove(
                icons.keySet()
                    .iterator()
                    .next());
        }
        FluidStackSpec tooltipSpec = new FluidStackSpec(spec.id(), query.getAmount(), spec.nbt());
        List<String> tooltip = new FluidTooltip(tooltipSpec, stack).getText()
            .lines()
            .limit(limits.getMaxPreviewTooltipLines())
            .toList();
        return new PreviewResolveResult(
            capability(),
            previewKey(key),
            spec.id(),
            FluidStacks.displayName(stack),
            spec.id(),
            null,
            null,
            query.getAmount(),
            spec.nbt(),
            tooltip,
            png,
            edge,
            edge);
    }

    private static String previewKey(IconKey key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(
                    key.toString()
                        .getBytes(StandardCharsets.UTF_8));
            return "fluids|" + HexFormat.of()
                .formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void close() {
        GuideClientTasks.call(() -> {
            icons.clear();
            capture.close();
            return null;
        });
    }
}
