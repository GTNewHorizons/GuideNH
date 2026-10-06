---
navigation:
  title: Section Color Codes (K1)
  position: 8970
---

TEST GOAL: every § color code line by line + §l/§o/§r combinations + 50 consecutive § codes + a lone § literal + Color tag comparison

INVARIANTS: no giant glyphs (K1); color switching is correct; glyph size is consistent across the page

## Full Color Code Table (§0-§9, §a-§f)

Each line below uses a different § color code applied to the same sample text. The color should match the named Minecraft color.

§0Black sample
§1Dark Blue sample
§2Dark Green sample
§3Dark Aqua sample
§4Dark Red sample
§5Dark Purple sample
§6Gold sample
§7Gray sample
§8Dark Gray sample
§9Blue sample
§aGreen sample
§bAqua sample
§cRed sample
§dLight Purple sample
§eYellow sample
§fWhite sample

Expected: Each line renders in the correct named color; colors are visually distinguishable from adjacent lines; no artifacts or missing characters.

## Formatting Code Combinations (§l, §o, §r)

§lBold text rendered with section-l
§oItalic text rendered with section-o
§l§oBold and italic combined
Normal text with §obefore reset§r and after reset
§lBold with §ocolor§r reset to normal

Expected: §l produces bold weight; §o produces italic slant; §r resets all formatting to default; combinations apply correctly; reset terminates formatting precisely at the §r boundary.

## 50 Consecutive § Codes (K1 Replication)

§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§

Expected: (K1 sentinel) No single character renders as a giant oversized glyph; all fifty § characters are uniform in size and equally spaced; the line height matches other lines on the page.

## Isolated § Literal

A single § character at the end of this sentence should render as a normal-sized glyph§. The word hello§world contains a § in the middle of a word. A paragraph with only § on its own line:

§

Expected: The isolated § renders at the same font size and scale as surrounding text; it is not a giant glyph; it does not trigger any formatting mode.

## Color Tag Comparison

Inline <Color id="red">red text via Color tag</Color> and <Color id="green">green text via Color tag</Color> and <Color id="blue">blue text via Color tag</Color>.

Direct hex: <Color color="#FFA500">orange via hex color</Color> and <Color color="#00CED1">dark turquoise via hex color</Color>.

These Color tag results should visually match the corresponding § color equivalents above.

Expected: `<Color id="red">` renders the same red as §c; `<Color id="green">` matches §a; `<Color id="blue">` matches §9; custom hex colors render correctly; Color tag does not produce a giant glyph.
