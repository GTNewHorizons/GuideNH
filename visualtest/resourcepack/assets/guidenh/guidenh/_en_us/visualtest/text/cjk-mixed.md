---
navigation:
  title: CJK Mixed Text
  position: 8950
---

TEST GOAL / 测试目标：中英混排、全角标点、CJK 无空格长串换行、40+ 字符英文长词、CJK+行内代码/链接混排

INVARIANTS / 不变式：任意断行不溢出；混排基线一致

## Chinese-English Mixed

Expected: Both CJK glyphs and Latin characters are on the same baseline; line spacing is consistent.

本指南涵盖了许多 Minecraft 的 GTNH 模组包内容，包括 Processing Array 和 Advanced Miner II 等高级机器。

Please read the 合成表 carefully before crafting the 工业电路板。

这个句子混合English和中文，目的是测试baseline alignment和line spacing。

## Full-Width Punctuation

Expected: Full-width punctuation （，。！？【】「」） does not break the line or cause overflow.

指南的「第一章」介绍了基础合成；「第二章」则深入讲解了【多区块结构】的搭建方法。

注意：全角逗号，句号。以及感叹号！问号？都应该正常显示。

## Long CJK String without Spaces

Expected: Long consecutive CJK text wraps at glyph boundaries; no overflow beyond right margin.

这是一个没有空格的长中文串测试用来验证引擎的断行行为是否正确因为中文没有空格所以引擎需要根据字符边界自动换行不能溢出到右边距外面同时也不能在错误的位置断行

## Long English Word (40+ Characters)

Expected: Long uninterrupted Latin text breaks or overflows gracefully; no horizontal scrollbar at page level.

Supercalifragilisticexpialidocious is an English word that contains 34 characters, and antidisestablishmentarianism contains 28 characters.

Pneumonoultramicroscopicsilicovolcanoconiosis is a 45-character lung disease — such long words should not break layout.

## CJK with Inline Code and Links

Expected: CJK text flowing inline with `code spans` and [markdown links](./inline-game-tags.md) without breaking the baseline or causing misalignment.

请打开 `config/guidenh/DefaultGuide` 目录并阅读 [Links 页面](./links.md) 的说明。

代码片段如 `ItemStack stack = new ItemStack(Items.apple)` 应当与中文同行，不撑高行盒。

打开 [Footnotes 页](./footnotes.md)查看脚注功能；同时注意 `@Mod(modid = "GuideNH")` 的显示效果。
