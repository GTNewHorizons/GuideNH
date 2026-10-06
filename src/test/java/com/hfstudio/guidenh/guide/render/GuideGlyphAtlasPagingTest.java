package com.hfstudio.guidenh.guide.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless contract tests for {@link GuideGlyphAtlas} multi-page packing, LRU
 * per-page eviction and the failure set. Pure Java: no GL, no Minecraft (the
 * atlas runs in headless bookkeeping mode via {@code setHeadless(true)}).
 * <p>
 * Geometry: pages are 2048² with 1px padding. A 400x400 glyph packs 5 per row
 * (5×401 = 2005 ≤ 2048) and 5 rows (rows at y=1,402,803,1204,1605) → exactly
 * 25 glyphs per page.
 */
class GuideGlyphAtlasPagingTest {

    private static final int GLYPH = 400;
    private static final int PER_ROW = 5;
    private static final int PER_PAGE = 25;
    private static final int FULL_PAGES = GuideGlyphAtlas.MAX_PAGES;
    /** Mirrors the production page dimension (2048² RGBA). */
    private static final int PAGE_SIZE = 2048;

    private GuideGlyphAtlas atlas;

    @BeforeEach
    void setUp() {
        atlas = new GuideGlyphAtlas();
        atlas.setHeadless(true);
    }

    private static byte[] rgba(int w, int h) {
        return new byte[w * h * 4];
    }

    private GuideGlyphAtlas.GlyphSlot upload(int key) {
        return atlas.upload(key, rgba(GLYPH, GLYPH), GLYPH, GLYPH);
    }

    @Test
    void packingSpillsIntoSecondPageWhenFirstPageFills() {
        // First page holds exactly PER_PAGE glyphs; the next glyph opens page 1.
        for (int i = 0; i < PER_PAGE; i++) {
            assertEquals(0, upload(i).pageId(), "glyph " + i + " must land on page 0");
        }
        assertEquals(1, atlas.pageCount(), "page 1 not opened until page 0 is full");
        assertEquals(1, upload(PER_PAGE).pageId(), "overflow glyph must open page 1");
        assertEquals(2, atlas.pageCount());
    }

    @Test
    void lookupReturnsPageAndRefreshesRecency() {
        assertEquals(0, upload(7).pageId());
        GuideGlyphAtlas.GlyphSlot slot = atlas.lookup(7);
        assertNotNull(slot);
        assertEquals(0, slot.pageId());
        assertNotNull(slot.uv());
    }

    @Test
    void lruEvictionReusesOldestPageAfterAllFourPagesFill() {
        // Fill the full page budget (FULL_PAGES=8 pages, keys 0..199 → 25 glyphs each).
        for (int i = 0; i < PER_PAGE * FULL_PAGES; i++) {
            int page = upload(i).pageId();
            assertTrue(page < FULL_PAGES, "page " + page + " beyond budget");
        }
        assertEquals(FULL_PAGES, atlas.pageCount());

        // Refresh page 0's recency → page 1 (oldest untouched) becomes the LRU victim.
        assertNotNull(atlas.lookup(0));
        GuideGlyphAtlas.GlyphSlot overflow = upload(PER_PAGE * FULL_PAGES);
        assertNotNull(overflow, "new glyph must be packable after eviction");
        assertEquals(1, overflow.pageId(), "LRU victim (page 1) must host the overflow glyph");
        assertEquals(FULL_PAGES, atlas.pageCount(), "page budget must stay capped");

        // Glyphs on surviving pages remain resolvable.
        assertNotNull(atlas.lookup(50), "page 2 glyph survives");
        assertNotNull(atlas.lookup(75), "page 3 glyph survives");
    }

    @Test
    void evictedKeysMissButAreNotInFailureSet() {
        for (int i = 0; i < PER_PAGE * FULL_PAGES; i++) {
            upload(i);
        }
        assertNotNull(atlas.lookup(0));
        assertNotNull(upload(PER_PAGE * FULL_PAGES)); // triggers eviction of page 1

        assertNull(atlas.lookup(25), "glyph on the evicted page must miss");
        assertFalse(atlas.isFailed(25), "evicted keys must NOT enter the failure set");

        // Re-uploading an evicted key repacks it (contents no longer cached).
        GuideGlyphAtlas.GlyphSlot repacked = upload(25);
        assertNotNull(repacked);
        assertEquals(1, repacked.pageId(), "evicted key repacks onto the freed page");
    }

    @Test
    void structurallyOversizedGlyphFailsOnceThenStaysSilent() {
        int w = PAGE_SIZE - 1; // 2047 > 2046 → structurally unfittable
        long key = 5000L;
        assertNull(atlas.upload(key, rgba(w, 16), w, 16), "oversized glyph must be dropped");
        assertTrue(atlas.isFailed(key));
        assertEquals(0, atlas.pageCount(), "a failed upload must not allocate a page");

        // Same key again: silently blocked by the failure set (no re-WARN).
        assertNull(atlas.upload(key, rgba(w, 16), w, 16));
        assertTrue(atlas.isFailed(key));
    }

    @Test
    void evictionClearsFailureSetAllowingRetry() {
        long key = 5000L;
        int w = PAGE_SIZE - 1;
        assertNull(atlas.upload(key, rgba(w, 16), w, 16));
        assertTrue(atlas.isFailed(key));

        // Fill the atlas past the budget → first eviction clears the failure set.
        for (int i = 0; i < PER_PAGE * FULL_PAGES; i++) {
            upload(i);
        }
        assertNotNull(atlas.lookup(0));
        assertNotNull(upload(PER_PAGE * FULL_PAGES)); // eviction happens here
        assertFalse(atlas.isFailed(key), "page eviction must clear the failure set");

        // Retry after capacity was released: still structurally unfittable, but
        // now it is a fresh failure (re-recorded), not a stale-set block.
        assertNull(atlas.upload(key, rgba(w, 16), w, 16));
        assertTrue(atlas.isFailed(key));
    }

    @Test
    void clearAndDeleteAreNoopsPerPageInHeadless() {
        assertEquals(-1, atlas.getTextureId(0), "headless pages expose no GL texture");
        assertEquals(0, upload(1).pageId());
        assertNotNull(atlas.lookup(1));

        atlas.clear();
        assertNull(atlas.lookup(1), "clear() drops all cached glyphs");
        assertEquals(0, upload(2).pageId(), "packing resumes after clear");

        atlas.delete();
        assertNull(atlas.lookup(2), "delete() drops all cached glyphs");
    }

    @Test
    void singlePagePackingMatchesLegacyRowLayout() {
        // The first glyph must sit at the legacy (PADDING, PADDING) origin and
        // each row must wrap exactly like the pre-pagination single-atlas cursor.
        float pad = 1f / PAGE_SIZE;
        GuideGlyphAtlas.GlyphSlot first = upload(1);
        assertEquals(
            pad,
            first.uv()
                .u(),
            1e-6f,
            "first glyph u must be PADDING/ATLAS_SIZE");
        assertEquals(
            pad,
            first.uv()
                .v(),
            1e-6f,
            "first glyph v must be PADDING/ATLAS_SIZE");

        // Glyph PER_ROW is the first of row 2 → v advances one full glyph row.
        for (int i = 1; i < PER_ROW; i++) {
            upload(1 + i);
        }
        GuideGlyphAtlas.GlyphSlot rowTwo = upload(PER_ROW + 1);
        assertEquals(
            pad,
            rowTwo.uv()
                .u(),
            1e-6f,
            "row 2 starts at u=PADDING/ATLAS_SIZE");
        assertEquals(
            (float) (GLYPH + 1 + 1) / PAGE_SIZE,
            rowTwo.uv()
                .v(),
            1e-6f,
            "row 2 v must skip the first row + padding");
    }
}
