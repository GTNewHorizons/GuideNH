---
navigation:
  title: Zoom Small (0.8)
  position: 7790
zoom: 0.8
---

TEST GOAL / 测试目标：frontmatter zoom: 0.8 shrinks entire page content to 80%

INVARIANTS / 不变式：All text and blocks render at 80% scale relative to normal; layout remains readable; no overflow clipping.

## Comparison Text

Expected: Every element on this page is rendered at 80% of the default zoom level. Compare with zoom-large.md (1.5) to confirm proportional scaling.

Normal paragraph text at zoom 0.8. This line should appear visibly smaller than the default zoom page.

## Heading Level Two at 0.8 Zoom

Expected: H2 heading shrinks proportionally; leading and spacing scale with zoom.

More body text to fill visual space. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog.

## Heading Level Three at 0.8 Zoom

Expected: H3 heading also scales; hierarchy between H2 and H3 is preserved at reduced size.

Additional paragraph to provide enough vertical extent for screenshot comparison. This page uses zoom: 0.8 in frontmatter. All elements — headings, paragraphs, inline styles, code — are affected uniformly.

### Inline Markup

Expected: Bold, italic, code, and other inline marks scale with the zoom factor.

- **bold text** at 0.8 zoom
- *italic text* at 0.8 zoom
- `code span` at 0.8 zoom
- ~~strikethrough~~ at 0.8 zoom

### Code Block

Expected: Code block font and padding scale to 80%.

```
zoom: 0.8
navigation:
  title: Zoom Test
  position: 7790
```

The zoom factor is applied as a page-level transform. All measurements (font sizes, padding, margins, borders) are multiplied by 0.8.

## Horizontal Ruler

Expected: Ruler line weight and spacing scale with zoom.

---

Final paragraph at the bottom of the zoom-small page. The total page height at 0.8 zoom should be noticeably shorter than the equivalent content at default zoom.
