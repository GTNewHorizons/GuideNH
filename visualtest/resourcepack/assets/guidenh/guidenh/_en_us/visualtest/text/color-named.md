---
navigation:
  title: Color Named Colours
  position: 8975
---

TEST GOAL / 测试目标：`<Color id="...">` 具名色（16 个 Minecraft 命名色）渲染

INVARIANTS / 不变式：全部具名色 id 解析成功（无红色错误文本）；颜色互相区分；段落正常排版

Syntax reference: `ColorTagCompiler` + `SymbolicColorResolver` — the id resolves through `SymbolicColor` (case-insensitive); the supported named set is the 16 Minecraft colours: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white.

## All Sixteen Named Colours

Here it should: render each named colour via `<Color id="...">` — every id resolves to its Minecraft colour without an error.

<Color id="black">black sample</Color> | <Color id="dark_blue">dark_blue sample</Color> | <Color id="dark_green">dark_green sample</Color>

<Color id="dark_aqua">dark_aqua sample</Color> | <Color id="dark_red">dark_red sample</Color> | <Color id="dark_purple">dark_purple sample</Color>

<Color id="gold">gold sample</Color> | <Color id="gray">gray sample</Color> | <Color id="dark_gray">dark_gray sample</Color>

<Color id="blue">blue sample</Color> | <Color id="green">green sample</Color> | <Color id="aqua">aqua sample</Color>

<Color id="red">red sample</Color> | <Color id="light_purple">light_purple sample</Color> | <Color id="yellow">yellow sample</Color>

<Color id="white">white sample</Color>

## Mixed Colour Paragraph

Here it should: render multiple named colours in a single flowing paragraph — colour switching mid-sentence works and does not leak into the following text.

The <Color id="red">red</Color> apple, the <Color id="blue">blue</Color> sky and the <Color id="green">green</Color> grass all sit in one paragraph, and <Color id="gold">gold</Color> follows.

## Named vs Hex Comparison

Here it should: render the named form next to its hex equivalent — both should match visually. The hex form must be 6-digit (`#RRGGBB`); the engine rejects 3-digit shorthands as malformed color values.

<Color id="red">red</Color> vs <Color color="#FF5555">#FF5555</Color> | <Color id="green">green</Color> vs <Color color="#55FF55">#55FF55</Color> | <Color id="blue">blue</Color> vs <Color color="#5555FF">#5555FF</Color>
