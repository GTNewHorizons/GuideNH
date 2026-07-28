---
navigation:
  title: Zoom Large (1.5)
  position: 7780
zoom: 1.5
---

TEST GOAL / 测试目标：frontmatter zoom: 1.5 enlarges entire page content to 150%

INVARIANTS / 不变式：All text and blocks render at 150% scale relative to normal; no overflow clipping despite larger size.

## Comparison Text

Expected: Every element on this page is rendered at 150% of the default zoom level. Compare with zoom-small.md (0.8) to confirm proportional scaling.

Normal paragraph text at zoom 1.5. This line should appear visibly larger than the default zoom page. The enlarged scale may cause more content to extend beyond the initial viewport — the page should scroll without truncation.

## Heading Level Two at 1.5 Zoom

Expected: H2 heading scales up proportionally; spacing increases with zoom.

More body text to fill visual space. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog.

## Heading Level Three at 1.5 Zoom

Expected: H3 heading also scales; hierarchy between H2 and H3 is preserved at enlarged size.

Additional paragraph to provide enough vertical extent for screenshot comparison. This page uses zoom: 1.5 in frontmatter. All elements — headings, paragraphs, inline styles, code — are affected uniformly.

### Inline Markup

Expected: Bold, italic, code, and other inline marks scale with the zoom factor.

- **bold text** at 1.5 zoom
- *italic text* at 1.5 zoom
- `code span` at 1.5 zoom
- ~~strikethrough~~ at 1.5 zoom

### Code Block

Expected: Code block font and padding scale to 150%.

```
zoom: 1.5
navigation:
  title: Zoom Test
  position: 7780
```

The zoom factor is applied as a page-level transform. All measurements (font sizes, padding, margins, borders) are multiplied by 1.5.

## Horizontal Ruler

Expected: Ruler line weight and spacing scale with zoom.

---

Final paragraph at the bottom of the zoom-large page. The total page height at 1.5 zoom should be noticeably taller than the equivalent content at default zoom.
