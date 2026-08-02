---
navigation:
  title: Callouts & Blockquotes
  position: 8260
---

TEST GOAL / 测试目标：五种 GitHub alert（NOTE/TIP/IMPORTANT/WARNING/CAUTION）+ 裸关键字命中 + 自定义 quote 三种图标（TEXT/ITEM/PNG）+ 朴素引用块 + 引用块内嵌套列表与段落混合

INVARIANTS / 不变式：alert 渲染 3px 类型色左边框 + 标题行（i18n 标题 + 类型色符号图标）；自定义 quote 以 color= 上色 3px 左边框、header 仅在存在 title/icon 时渲染；朴素引用块灰底 + 2px 边框且无标题行；`> NOTE:` 裸关键字也命中 Note alert；`[!NOTE]` 独立成行且后接空行时字面标记进入 alert 正文（已知边界）

## Plain Blockquote with Multiple Paragraphs

Here it should: render a plain blockquote — grey background, 2px grey left border, no title row, and the two paragraphs stacked inside the same grey quote box.

> First paragraph of a plain blockquote. It carries the main content of the quote.
>
> Second paragraph of the same blockquote, separated by a `>`-marked blank line. Both paragraphs share the grey quote box.

## GitHub Alerts

### Note Alert

Here it should: render a Note alert box — 3px blue (#638ef1) left border, a bold title row with the blue ⓘ symbol followed by the i18n Note title, and the body text below the title row.

> [!NOTE]
> Note alert with the blue accent line and icon.

### Tip Alert

Here it should: render a Tip alert box — 3px green (#61b75d) left border, a bold title row with the green ✦ symbol followed by the i18n Tip title.

> [!TIP]
> Tip alert with a green accent line and icon.

### Important Alert

Here it should: render an Important alert box — 3px purple (#8755dd) left border, a bold title row with the purple ➤ symbol followed by the i18n Important title.

> [!IMPORTANT]
> Important alert with the purple accent line and icon.

### Warning Alert

Here it should: render a Warning alert box — 3px gold (#c79d3e) left border, a bold title row with the gold ⚠ symbol followed by the i18n Warning title.

> [!WARNING]
> Warning alert with the gold accent line and icon.

### Caution Alert

Here it should: render a Caution alert box — 3px red (#e46150) left border, a bold title row with the red ☢ symbol followed by the i18n Caution title.

> [!CAUTION]
> Caution alert with the red accent line and icon.

## Custom Quote: Text Icon

Here it should: render a custom quote box — 3px #638ef1 left border, a header row containing the plain-text icon `i` followed by the bold title "Custom Quote", and the body below the header.

> {: title="Custom Quote" color="#638ef1" icon="i" }
> Custom title, accent colour and text icon.

## Custom Quote: Item Icon

Here it should: render a custom quote box — 3px #61b75d left border, a header row containing a small emerald item icon (`minecraft:emerald`) followed by the bold title "Item Quote".

> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> ItemStack icon in the quote header.

## Custom Quote: PNG Icon

Here it should: render a custom quote box — 3px #c79d3e left border, a header row containing the 8×8 red-64.png image icon followed by the bold title "PNG Quote".

> {: title="PNG Quote" color="#c79d3e" iconPng="../assets/red-64.png" }
> PNG icon loaded from guide assets.

## Bare Keyword Trap

Here it should: match the Note alert even without brackets — the bare keyword `> NOTE:` (no `[!]` wrappers) still renders the blue Note alert box with title row, and the body retains the full line text including the `NOTE:` prefix.

> NOTE: A bare keyword without brackets hits the Note alert.

## Alert Marker Alone with Blank Line

Here it should: record a known boundary — when `[!NOTE]` sits alone on its line and a blank line separates the body, the engine still builds a Note alert box (title row present), but the literal `[!NOTE]` marker text lands in the box body because the directive prefix is only stripped when body text follows it in the same paragraph. The text after the blank line renders as a separate plain blockquote.

> [!NOTE]

> Body text after a blank line — the literal `[!NOTE]` marker stays in the alert body (known boundary, documented expected behaviour).

## Blockquote with Nested List and Paragraph Mix

Here it should: render a single plain blockquote containing a nested unordered list between two paragraphs — list markers and indentation stay inside the grey quote box, and the closing paragraph still sits in the same box.

> Opening paragraph of a blockquote that mixes paragraphs and a list.
>
> * First item of a nested list.
> * Second item of a nested list.
>
> Closing paragraph after the list, still inside the same blockquote.
