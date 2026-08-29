package com.hfstudio.guidenh.guide.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Setter;

/**
 * Glyph atlas: manages a bounded set of GL textures ({@link #MAX_PAGES} pages,
 * each a 2048² RGBA texture) containing rasterized glyphs from Rust
 * cosmic-text.
 * <p>
 * Upload strategy: each glyph is row-packed into the current packing page. When
 * the current page fills a new page is opened (up to {@link #MAX_PAGES}); once
 * all pages are in use, the least-recently-used page is evicted (GL texture
 * deleted, that page's glyph cache cleared, cursor reset) and reused.
 * <p>
 * <b>C-3 constraint:</b> capacity pressure NEVER triggers a whole-atlas clear —
 * a clear would invalidate every atlas key already emitted this frame and whole
 * paragraphs would vanish until the next layout. Eviction drops at most ONE
 * page of cached glyphs at a time; the evicted keys simply miss at draw time
 * (one glyph, once).
 * <p>
 * Structurally-unfittable glyphs (a single glyph larger than a page) are
 * recorded in a failure set: the same key is silently dropped on subsequent
 * uploads (one WARN per key), and the failure set is cleared by the first page
 * eviction so failed keys may retry after capacity is released.
 */
public class GuideGlyphAtlas {

    /**
     * Default atlas instance for measureLayout processing.
     * The render engine can override with setGlobalInstance().
     */
    @Setter
    private static GuideGlyphAtlas globalInstance = new GuideGlyphAtlas();

    public static GuideGlyphAtlas instance() {
        return globalInstance;
    }

    public GuideGlyphAtlas() {}

    private static final int ATLAS_SIZE = 2048;
    private static final int PADDING = 1;
    /**
     * Page cap: 2048² RGBA = 16 MB/page → 128 MB GPU budget at the cap.
     * Doubled from 4 to 8 (capacity stop-gap): a live render_scale=4 document
     * rasterizes glyphs at ~44 ppem, which exceeds the old 4-page capacity
     * (≈ 8,100 glyphs) and triggered silent whole-page LRU evictions mid-draw.
     */
    public static final int MAX_PAGES = 8;

    private final List<Page> pages = new ArrayList<>();
    /** Index of the current packing page, or -1 before the first page is opened. */
    private int currentPage = -1;

    /** Monotonic clock driving page {@code accessStamp} (LRU recency). */
    private long accessClock;

    /**
     * Keys that are structurally unfittable (a single glyph larger than a
     * page). While present, uploads of the same key return null silently.
     * Cleared by the first page eviction (capacity was released, failed keys
     * may retry). Evicted keys do NOT enter this set.
     */
    private final Set<Long> failureSet = new HashSet<>();

    /** Keys whose one-time WARN was already emitted (never cleared). */
    private final Set<Long> failureWarned = new HashSet<>();

    /**
     * Eviction-log aggregation window: within one window multiple page
     * evictions (capacity churn) collapse into a single WARN with aggregated
     * totals, so a page-fill storm can never flood the log one WARN per
     * eviction.
     */
    private static final long EVICTION_LOG_WINDOW_NANOS = 1_000_000_000L;
    /** Nanosecond timestamp of the last emitted eviction WARN (0 = never emitted yet). */
    private long lastEvictionWarnNanos = 0L;
    /** Glyphs dropped by evictions since the last emitted WARN. */
    private int evictionWarnGlyphCount;
    /** Pages evicted since the last emitted WARN. */
    private int evictionWarnPageCount;

    /**
     * Test hook: when true, all GL calls are skipped (packing/UV bookkeeping still runs).
     * -- SETTER --
     * Test hook for headless environments (unit tests without a GL context).
     */
    @Setter
    private boolean headless;

    /** One atlas texture plus its independent pack cursor and glyph cache. */
    private static final class Page {

        ByteBuffer atlasBuffer;
        int textureId = -1;
        int cursorX = PADDING;
        int cursorY = PADDING;
        int currentRowHeight = 0;
        long accessStamp;
        final Map<Long, GlyphUV> glyphCache = new HashMap<>();
    }

    /**
     * Upload a glyph bitmap to the atlas and return its page + UV coordinates.
     * {@code key} is the opaque bitmap key from LayoutResult (content-stable
     * across layout rebuilds); repeated uploads of the same key are no-ops.
     * Returns null when the glyph is structurally unfittable (dropped once with
     * a WARN, then silently for the same key until a page eviction clears the
     * failure set).
     */
    @Nullable
    public synchronized GlyphSlot upload(long key, byte[] rgba, int w, int h) {
        if (failureSet.contains(key)) {
            return null;
        }
        // Already packed: refresh the owning page's recency and return its slot.
        GlyphSlot existing = lookup(key);
        if (existing != null) {
            return existing;
        }
        // Structural limit: a single glyph larger than a page can never fit.
        if (w > ATLAS_SIZE - 2 * PADDING || h > ATLAS_SIZE - 2 * PADDING) {
            markFailed(key, w, h);
            return null;
        }
        int pageIndex = resolvePackingPage(w, h);
        if (pageIndex < 0) {
            // Defensive: even after opening/evicting a page the glyph did not
            // fit (should be unreachable for structurally-fittable glyphs).
            markFailed(key, w, h);
            return null;
        }
        return packInto(pageIndex, key, rgba, w, h);
    }

    /**
     * Locate or create a page that can host a glyph of size {@code w}x{@code h}.
     * Opens new pages up to {@link #MAX_PAGES}; once the page budget is
     * exhausted the least-recently-used page is evicted and reused. Never clears
     * the whole atlas (C-3).
     */
    private int resolvePackingPage(int w, int h) {
        for (int attempt = 0; attempt <= MAX_PAGES; attempt++) {
            Page page = currentPage >= 0 ? pages.get(currentPage) : null;
            if (page != null && packFits(page, w, h)) {
                return currentPage;
            }
            if (pages.size() < MAX_PAGES) {
                currentPage = openNewPage();
                continue;
            }
            int victim = findOldestPage();
            if (victim < 0) {
                return -1;
            }
            evictPage(victim);
            currentPage = victim;
            // A page's capacity was released: previously-failed keys may retry.
            failureSet.clear();
        }
        return -1;
    }

    /** Look up a glyph bitmap's page + UV coordinates in the atlas. */
    public @Nullable GlyphSlot lookup(long key) {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            GlyphUV uv = page.glyphCache.get(key);
            if (uv != null) {
                // Refresh page recency: the page that just served a glyph is
                // hot and must not be the first LRU victim.
                page.accessStamp = ++accessClock;
                return new GlyphSlot(i, uv);
            }
        }
        return null;
    }

    /** Get the GL texture name for the given page (created on first use). */
    public int getTextureId(int pageId) {
        if (pageId < 0 || pageId >= pages.size()) {
            return -1;
        }
        Page page = pages.get(pageId);
        ensureTexture(page);
        return page.textureId;
    }

    /** Test hook: number of currently allocated pages. */
    public int pageCount() {
        return pages.size();
    }

    /** Test hook: whether {@code key} is currently in the failure set. */
    public boolean isFailed(long key) {
        return failureSet.contains(key);
    }

    /** Clear the atlas: per-page caches cleared and pixels zeroed (caller-initiated). */
    public void clear() {
        for (Page page : pages) {
            page.glyphCache.clear();
            for (int i = 0; i < page.atlasBuffer.capacity(); i++) {
                page.atlasBuffer.put(i, (byte) 0);
            }
            page.cursorX = PADDING;
            page.cursorY = PADDING;
            page.currentRowHeight = 0;
            if (page.textureId >= 0 && !headless) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, page.textureId);
                GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    ATLAS_SIZE,
                    ATLAS_SIZE,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    page.atlasBuffer);
            }
        }
    }

    /** Delete every page's GL texture and drop their caches. */
    public void delete() {
        for (Page page : pages) {
            if (page.textureId >= 0) {
                GL11.glDeleteTextures(page.textureId);
                page.textureId = -1;
            }
            page.glyphCache.clear();
        }
    }

    // ---- packing internals -----------------------------------------------------

    private int openNewPage() {
        Page page = new Page();
        page.atlasBuffer = GLAllocation.createDirectByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        page.accessStamp = ++accessClock;
        pages.add(page);
        return pages.size() - 1;
    }

    /**
     * Whether {@code w}x{@code h} fits on the page's current row (wrapping to a
     * new row first, mirroring the pre-pagination single-atlas cursor advance).
     */
    private boolean packFits(Page page, int w, int h) {
        if (page.cursorX + w + PADDING > ATLAS_SIZE) {
            page.cursorX = PADDING;
            page.cursorY += page.currentRowHeight + PADDING;
            page.currentRowHeight = 0;
        }
        return page.cursorY + h + PADDING <= ATLAS_SIZE;
    }

    private int findOldestPage() {
        int oldest = -1;
        long oldestStamp = Long.MAX_VALUE;
        for (int i = 0; i < pages.size(); i++) {
            long stamp = pages.get(i).accessStamp;
            if (stamp < oldestStamp) {
                oldestStamp = stamp;
                oldest = i;
            }
        }
        return oldest;
    }

    /**
     * Evict one page: delete its GL texture, drop its glyph cache, reset the
     * pack cursor and zero the pixel buffer (fresh allocation) so stale pixels
     * never re-enter the re-created texture. Leaves a throttled WARN trace
     * (page / evicted glyph count / reason / pages after eviction).
     */
    private void evictPage(int index) {
        Page page = pages.get(index);
        if (!headless && page.textureId >= 0) {
            GL11.glDeleteTextures(page.textureId);
        }
        page.textureId = -1;
        int evictedGlyphs = page.glyphCache.size();
        page.glyphCache.clear();
        page.atlasBuffer = GLAllocation.createDirectByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
        page.cursorX = PADDING;
        page.cursorY = PADDING;
        page.currentRowHeight = 0;
        page.accessStamp = ++accessClock;
        recordEviction(index, evictedGlyphs);
    }

    /**
     * Leave a WARN trace when a page is evicted. The reason is always LRU —
     * {@link #resolvePackingPage} evicts the least-recently-used page once the
     * page budget is exhausted. Throttled: evictions inside the same
     * {@link #EVICTION_LOG_WINDOW_NANOS} window aggregate into one WARN (this
     * is a capacity condition, not a per-glyph event), so capacity churn
     * cannot flood the log.
     */
    private void recordEviction(int pageIndex, int evictedGlyphs) {
        evictionWarnGlyphCount += evictedGlyphs;
        evictionWarnPageCount++;
        long now = System.nanoTime();
        if (now - lastEvictionWarnNanos < EVICTION_LOG_WINDOW_NANOS) {
            return; // throttle: absorbed into the next window's aggregated WARN
        }
        GuideDebugLog.warnAlways(
            "[GuideNH] glyph atlas: page evicted (LRU), last victim page={}, {} glyph(s) dropped "
                + "across {} eviction(s), pages after eviction={} (MAX_PAGES={})",
            pageIndex,
            evictionWarnGlyphCount,
            evictionWarnPageCount,
            pages.size(),
            MAX_PAGES);
        evictionWarnGlyphCount = 0;
        evictionWarnPageCount = 0;
        lastEvictionWarnNanos = now;
    }

    private GlyphSlot packInto(int pageIndex, long key, byte[] rgba, int w, int h) {
        Page page = pages.get(pageIndex);
        // Row-wrap (idempotent: resolvePackingPage already advanced the cursor).
        if (page.cursorX + w + PADDING > ATLAS_SIZE) {
            page.cursorX = PADDING;
            page.cursorY += page.currentRowHeight + PADDING;
            page.currentRowHeight = 0;
        }

        int u = page.cursorX;
        int v = page.cursorY;

        // Write glyph pixels into the page buffer
        for (int gy = 0; gy < h; gy++) {
            for (int gx = 0; gx < w; gx++) {
                int srcIdx = (gy * w + gx) * 4;
                int dstIdx = ((page.cursorY + gy) * ATLAS_SIZE + (page.cursorX + gx)) * 4;
                page.atlasBuffer.put(dstIdx, rgba[srcIdx]);
                page.atlasBuffer.put(dstIdx + 1, rgba[srcIdx + 1]);
                page.atlasBuffer.put(dstIdx + 2, rgba[srcIdx + 2]);
                page.atlasBuffer.put(dstIdx + 3, rgba[srcIdx + 3]);
            }
        }

        if (h > page.currentRowHeight) page.currentRowHeight = h;
        page.cursorX += w + PADDING;

        if (!headless) {
            // Upload to the page's GL texture
            ensureTexture(page);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, page.textureId);
            // Set UNPACK_ROW_LENGTH so glTexSubImage2D reads rows matching the atlas stride,
            // not the sub-image width. Position the buffer to the start of the glyph's row.
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, ATLAS_SIZE);
            page.atlasBuffer.position((v * ATLAS_SIZE + u) * 4);
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                u,
                v,
                w,
                h,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                page.atlasBuffer);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            page.atlasBuffer.position(0);
        }

        float texSize = ATLAS_SIZE;
        GlyphUV uv = new GlyphUV(u / texSize, v / texSize, (u + w) / texSize, (v + h) / texSize);
        page.glyphCache.put(key, uv);
        page.accessStamp = ++accessClock;
        return new GlyphSlot(pageIndex, uv);
    }

    private void markFailed(long key, int w, int h) {
        failureSet.add(key);
        if (failureWarned.add(key)) {
            // One-shot WARN per key (mirrors Rust CLAMP_REPORTED): a structural
            // drop is a hard, un-recoverable property of the glyph, not a
            // capacity condition — flood-logging it per frame is noise.
            GuideDebugLog.warnAlways(
                "[GuideNH] glyph atlas: oversized glyph dropped, key={} ({}x{}) exceeds page {}x{}",
                key,
                w,
                h,
                ATLAS_SIZE,
                ATLAS_SIZE);
        }
    }

    private void ensureTexture(Page page) {
        if (page.textureId >= 0 || headless) return;
        page.textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, page.textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            ATLAS_SIZE,
            ATLAS_SIZE,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null);
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            ATLAS_SIZE,
            ATLAS_SIZE,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            page.atlasBuffer);
    }

    /** Page index + UV coordinates of a packed glyph. */
    public record GlyphSlot(int pageId, GlyphUV uv) {}

    public record GlyphUV(float u, float v, float u2, float v2) {}
}
