---
navigation:
  title: Links
  position: 8960
---

TEST GOAL / 测试目标：链接全家族——页内锚、跨页、外链、自动链接、引用式、音效链接、CommandLink、SoundLink、带 title 链接

INVARIANTS / 不变式：着色/下划线一致；点击区 bbox = 文本 bbox

## Page-Internal Anchors

Expected: Named anchor creates a jump target; the TOC-style link navigates to it; anchor name is not visible as rendered text.

<a name="section-anchors"></a>

[Jump to this anchor](#section-anchors) — click to scroll to that anchor paragraph.

<a name="midpoint">Anchor target paragraph</a> rendered inline with surrounding text.

[Jump to midpoint](#midpoint)

## Cross-Page Links

Expected: Relative markdown link resolves to another page in the same guide; click navigates.

[Open CJK Mixed page](./cjk-mixed.md)

[Open Inline Game Tags page](./inline-game-tags.md)

[Open Footnotes page](./footnotes.md)

## External Links

Expected: Underlined distinct color; opens external browser on click.

[Visit Example](https://example.com)

[GTNH Wiki](https://gtnh.huapu.moe)

## Autolinks (Bare URLs)

Expected: Bare URL rendered as clickable link with underline.

https://example.com/docs

https://github.com/GTNewHorizons/GuideNH

## Reference-Style Links

Expected: [ref][] renders the same as a standard inline link; definition line is invisible in output.

[Reference to CJK Mixed][cjk]

[Reference to Footnotes][fn]

[cjk]: ./cjk-mixed.md "CJK mixed text page"
[fn]: ./footnotes.md "Footnotes test page"

## Action Links (Sound via &[])

Expected: &[label](sound:uri) renders as clickable text that plays a sound effect; clickable bounding box matches text.

&[Play orb pickup](sound:minecraft:entity.experience_orb.pickup)

&[Click with volume](sound:minecraft:block.note.pling?volume=0.5)

## CommandLink

Expected: Clickable text that sends a chat command; close attribute closes GUI after click.

<CommandLink command="/help" />

<CommandLink command="/help" close="true" title="Help me" />

## SoundLink

Expected: Clickable text playing a sound; distinct visual style from regular hyperlinks.

<SoundLink sound="minecraft:entity.experience_orb.pickup" />

<SoundLink sound="minecraft:entity.firework.blast" volume="0.8" pitch="1.2" />

## Links with Title Tooltip

Expected: Hovering shows the title text as a tooltip overlay.

[Tooltip link to CJK](./cjk-mixed.md "This tooltip appears on hover")

[Another tooltip](./inline-game-tags.md "Go to game tags page")
