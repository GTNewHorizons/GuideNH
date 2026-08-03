package com.hfstudio.guidenh.guide.document.block;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Computes the "ink" bounding box of an item icon texture — the smallest source
 * rectangle covering every pixel whose alpha exceeds {@link #INK_ALPHA_THRESHOLD},
 * expressed in 16-unit icon space. This is the optical basis for the tight
 * advance of inline item icons: the layout cell shrinks to {@code inkWidth *
 * scale + 2 * PAD} and the icon is drawn at {@code -inkLeft * scale + PAD} so
 * the ink left edge sits exactly {@code PAD} px from the cell's left edge
 * (fixes "ItemLink icon sits too close to the item name / inconsistent gap").
 *
 * <p>Pixel reads are inherently best-effort. The primary source is the atlas
 * sprite's CPU frame data ({@link TextureAtlasSprite#getFrameTextureData(int)}),
 * which is present for animated sprites (compass/clock) but cleared for static
 * sprites after the atlas upload — in that case the source PNG is loaded from
 * the resource manager (the same image the atlas stitched, always available on
 * disk). Custom icons, the missing-texture sprite, and every other failure
 * path return {@code null} so the caller keeps its legacy 16px-cell behavior
 * with zero regression.
 *
 * <p>Results are cached statically keyed by {@code item:meta} (mirrors
 * {@link com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks}), so
 * per-frame stack swapping (e.g. {@link LytCyclingItemImage}) never pays the
 * pixel scan more than once per item meta.
 */
public final class IconMetrics {

    /** Alpha (0-255) strictly above which a source pixel counts as ink. */
    public static final int INK_ALPHA_THRESHOLD = 8;

    /** Unit size of the icon draw quad (all item icons render into a 16x16 box). */
    private static final float ICON_UNIT_SIZE = 16f;

    private static final int MAX_CACHE_SIZE = 2048;
    private static final Map<String, IconMetrics> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<>());

    /** Left edge of the ink bbox in 16-unit icon space (inclusive). */
    public final int inkLeft;
    /** Right edge of the ink bbox in 16-unit icon space (inclusive). */
    public final int inkRight;
    /** Top edge of the ink bbox in 16-unit icon space (inclusive). */
    public final int inkTop;
    /** Bottom edge of the ink bbox in 16-unit icon space (inclusive). */
    public final int inkBottom;
    /** Ink width in 16-unit icon space ({@code inkRight - inkLeft + 1}). */
    public final int width;
    /** Ink height in 16-unit icon space ({@code inkBottom - inkTop + 1}). */
    public final int height;

    private IconMetrics(int inkLeft, int inkRight, int inkTop, int inkBottom) {
        this.inkLeft = inkLeft;
        this.inkRight = inkRight;
        this.inkTop = inkTop;
        this.inkBottom = inkBottom;
        this.width = inkRight - inkLeft + 1;
        this.height = inkBottom - inkTop + 1;
    }

    /**
     * Returns the ink metrics for {@code stack}, computing and caching them on
     * first use. Returns {@code null} (without caching) on any failure path so
     * callers fall back to their legacy 16px-cell behavior.
     */
    @Nullable
    public static IconMetrics forStack(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Item item = stack.getItem();
        if (item == null) {
            return null;
        }
        int meta = stack.getItemDamage();
        String key = metaCacheKey(item, meta);
        IconMetrics cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        IconMetrics computed = compute(stack, item, key);
        if (computed != null) {
            if (CACHE.size() >= MAX_CACHE_SIZE) {
                CACHE.clear();
            }
            CACHE.put(key, computed);
        }
        return computed;
    }

    @Nullable
    private static IconMetrics compute(ItemStack stack, Item item, String key) {
        int passes;
        try {
            passes = item.requiresMultipleRenderPasses()
                ? Math.max(1, item.getRenderPasses(stack.getItemDamage()))
                : 1;
        } catch (Throwable t) {
            warnOnce("passes:" + key, t);
            passes = 1;
        }

        int minLeft = Integer.MAX_VALUE;
        int minTop = Integer.MAX_VALUE;
        int maxRight = -1;
        int maxBottom = -1;

        for (int pass = 0; pass < passes; pass++) {
            IIcon icon;
            try {
                icon = passes > 1 ? item.getIcon(stack, pass) : stack.getIconIndex();
            } catch (Throwable t) {
                warnOnce("icon:" + key + ":" + pass, t);
                continue;
            }
            // TextureAtlasSprite is the only IIcon implementation carrying
            // pixel data; custom icons and the missing-texture sprite fall back
            // to the legacy cell.
            if (!(icon instanceof TextureAtlasSprite)) {
                continue;
            }
            if ("missingno".equals(icon.getIconName())) {
                continue;
            }
            int[] ink = scanPass(key, (TextureAtlasSprite) icon, pass);
            if (ink == null) {
                continue;
            }
            if (ink[0] < minLeft) {
                minLeft = ink[0];
            }
            if (ink[1] < minTop) {
                minTop = ink[1];
            }
            if (ink[2] > maxRight) {
                maxRight = ink[2];
            }
            if (ink[3] > maxBottom) {
                maxBottom = ink[3];
            }
        }

        // No pass produced a single ink pixel (empty/fully-transparent texture)
        // — treat as unmeasurable and fall back to the legacy cell.
        if (maxRight < 0) {
            return null;
        }
        return new IconMetrics(minLeft, maxRight, minTop, maxBottom);
    }

    /**
     * Scans one render pass's icon for ink pixels.
     *
     * @return {@code {inkLeft, inkTop, inkRight, inkBottom}} in 16-unit icon
     *         space, or {@code null} when no readable pixel source yields ink.
     */
    @Nullable
    private static int[] scanPass(String key, TextureAtlasSprite sprite, int pass) {
        // Primary source: the atlas sprite's CPU frame data (task-specified).
        // getIconWidth()/getIconHeight() account for the +16px padding that
        // anisotropic filtering bakes into the pixel array, so pixel indexing
        // MUST use them.
        int w = sprite.getIconWidth();
        int h = sprite.getIconHeight();
        if (w > 0 && h > 0) {
            try {
                int[][] frames = sprite.getFrameTextureData(0);
                if (frames != null && frames.length > 0) {
                    int[] pixels = frames[0];
                    if (pixels != null && pixels.length >= w * h) {
                        return scanInkNormalized(pixels, w, h);
                    }
                }
            } catch (Throwable t) {
                // Expected for static sprites: their CPU frame data is cleared
                // after the atlas upload, so getFrameTextureData throws. This
                // is the task-required tolerated fallback path; fall through to
                // the resource-pack PNG source below instead of silently
                // skipping the item.
            }
        }
        // Fallback source: the sprite's source PNG from the resource manager —
        // the same image the atlas stitched, always present on disk.
        BufferedImage img = loadIconImage(sprite.getIconName());
        if (img == null) {
            warnOnce("png:" + key + ":" + pass, "[GuideNH] IconMetrics: no readable pixel source for {}; falling back to legacy 16px cell");
            return null;
        }
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) {
            warnOnce("png:" + key + ":" + pass, "[GuideNH] IconMetrics: no readable pixel source for {}; falling back to legacy 16px cell");
            return null;
        }
        int[] pixels = img.getRGB(0, 0, iw, ih, null, 0, iw);
        return scanInkNormalized(pixels, iw, ih);
    }

    /**
     * Scans a row-major ARGB pixel array and returns the ink bbox normalized
     * from the raw pixel grid to 16-unit icon space (item icons are drawn on a
     * 16x16 quad regardless of the source texture's pixel dimensions).
     */
    @Nullable
    private static int[] scanInkNormalized(int[] pixels, int w, int h) {
        int minLeft = Integer.MAX_VALUE;
        int minTop = Integer.MAX_VALUE;
        int maxRight = -1;
        int maxBottom = -1;
        for (int y = 0; y < h; y++) {
            int rowBase = y * w;
            for (int x = 0; x < w; x++) {
                if (((pixels[rowBase + x] >>> 24) & 0xFF) > INK_ALPHA_THRESHOLD) {
                    if (x < minLeft) {
                        minLeft = x;
                    }
                    if (x > maxRight) {
                        maxRight = x;
                    }
                    if (y < minTop) {
                        minTop = y;
                    }
                    if (y > maxBottom) {
                        maxBottom = y;
                    }
                }
            }
        }
        if (maxRight < 0) {
            return null;
        }
        return new int[] {
            Math.round(minLeft * ICON_UNIT_SIZE / w),
            Math.round(minTop * ICON_UNIT_SIZE / h),
            Math.round(maxRight * ICON_UNIT_SIZE / w),
            Math.round(maxBottom * ICON_UNIT_SIZE / h) };
    }

    /**
     * Loads the source PNG of the sprite named {@code iconName} (e.g.
     * {@code apple} or {@code crafting_table_top}) from the item or block
     * texture folders, mirroring the atlas's own path resolution.
     */
    @Nullable
    private static BufferedImage loadIconImage(@Nullable String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }
        IResourceManager rm = mc.getResourceManager();
        if (rm == null) {
            return null;
        }
        ResourceLocation base;
        try {
            base = new ResourceLocation(iconName);
        } catch (Throwable t) {
            return null;
        }
        BufferedImage img = tryLoad(rm, base, "textures/items");
        if (img == null) {
            img = tryLoad(rm, base, "textures/blocks");
        }
        return img;
    }

    @Nullable
    private static BufferedImage tryLoad(IResourceManager rm, ResourceLocation base, String folder) {
        try {
            ResourceLocation loc = new ResourceLocation(
                base.getResourceDomain(),
                folder + "/" + base.getResourcePath() + ".png");
            IResource res = rm.getResource(loc);
            if (res == null) {
                return null;
            }
            try (InputStream in = res.getInputStream()) {
                return ImageIO.read(in);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** Reuses the {@code item:meta} key convention from GuideDisplayItemStacks. */
    private static String metaCacheKey(Item item, int meta) {
        Object name = Item.itemRegistry.getNameForObject(item);
        return (name != null ? name.toString() : item.getClass().getName()) + ":" + meta;
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            GuideDebugLog.warnAlways(message, key);
        }
    }

    private static void warnOnce(String key, Throwable t) {
        if (WARNED.add(key)) {
            GuideDebugLog.warnAlways(
                "[GuideNH] IconMetrics: failed to read ink metrics for {}; falling back to legacy 16px cell",
                key,
                t);
        }
    }
}
