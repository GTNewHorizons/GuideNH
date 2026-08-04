---
navigation:
  title: QuestLink Tag
  position: 9080
---

TEST GOAL / 测试目标：`<QuestLink>` 内联标签（id / text / show_tooltip 属性）编译与渲染

INVARIANTS / 不变式：QuestLink 渲染为内联文本且不触发编译错误；段落正常换行

Syntax reference: `QuestLinkCompiler` — `<QuestLink id="<uuid>" [text="<override>"] [show_tooltip="false"]/>` compiles into an inline BetterQuesting quest link. The fixture screenshot world has an empty BetterQuesting quest database, so the runtime state for these quest ids is MISSING — `QuestLinkScript` renders MISSING links as red italic text (feature rendering for a missing quest, not a compiler error).

## QuestLink With Text Override

Here it should: render an inline quest link for a quest UUID with a custom `text` label — no compile-time error, text flows inline.

Follow <QuestLink id="0f1e2d3c-4b5a-6789-abcd-0e1f2a3b4c5d" text="the furnace quest" /> to learn about furnaces.

## QuestLink Without Text Override

Here it should: render a quest link that falls back to the localized quest-name label (missing-quest bracket label in this empty-database world).

<QuestLink id="a1b2c3d4-e5f6-4a5b-8c7d-9e8f7a6b5c4d" />

## QuestLink Without Tooltip

Here it should: render a quest link with `show_tooltip="false"` suppressing the hover tooltip while keeping the inline text.

Quest progress: <QuestLink id="12345678-90ab-cdef-1234-567890abcdef" text="start the machine" show_tooltip="false" />.
