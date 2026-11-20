---
navigation:
  title: Tooltip
  parent: index.md
  position: 140
categories:
  - widgets
---

# Tooltip

物品悬停提示、链接标题与 `<Tooltip>` 富文本容器示例。

## 悬停基础测试

将鼠标移到物品图标或方块 3D 预览上时，会浮现对应 tooltip：

<Row>
  <ItemImage id="minecraft:diamond" />
  <BlockImage id="minecraft:furnace" />
  <RecipeFor id="minecraft:stick" />
</Row>

悬停方块时显示白色描边+方块名：

<GameScene width="160" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="1" />
</GameScene>

## ItemStack tooltip 开关

- 默认（显示 tooltip）：<ItemImage id="minecraft:diamond_sword" />
- 禁用（`noTooltip`）：<ItemImage id="minecraft:diamond_sword" noTooltip={true} />
- ItemLink：<ItemLink id="minecraft:diamond_axe" />
- 文字链接：[跳到 Markdown 页](./markdown.md)

---

**Markdown 混合内联格式**：**粗体** / *斜体* / ~~删除线~~ / `代码` / [链接](./japanese.md)

## Tooltip 内容渲染测试

### 文本 tooltip

<Row>
  <a title="这是一段纯文本 Tooltip。">悬停查看文字提示</a>
  <a title="第一行内容较长，提示框会按可用宽度自动换行；第二段说明继续展示完整文字。">悬停查看自动换行</a>
  <a title="这段文字较长，用来检查提示框是否会根据可用空间换行并在屏幕边缘调整位置。">悬停检查屏幕边缘</a>
</Row>

### ItemStack tooltip

<Row>
  <ItemImage id="minecraft:bread" />
  <ItemImage id="minecraft:cooked_porkchop" />
  <ItemImage id="minecraft:diamond_sword:0:{ench:[{id:16s,lvl:5s},{id:21s,lvl:3s}]}" />
  <ItemImage id="minecraft:potion:0:{CustomPotionEffects:[{Id:1b,Amplifier:1b,Duration:9600}]}" />
</Row>

### 物品图 tooltip

<Tooltip label="悬停查看绿宝石图标">
  <ItemImage id="minecraft:emerald" scale="3" />
</Tooltip>

### 配方 tooltip

<Tooltip label="悬停查看合成台配方">
  <RecipeFor id="minecraft:crafting_table" />
</Tooltip>

### 3D 预览内 tooltip

<GameScene width="192" height="160" zoom={5} interactive={true}>
  <Block id="minecraft:chest" />
  <Block id="minecraft:furnace" x="1" />
</GameScene>

## 富内容 Tooltip（`<Tooltip>`）

`<Tooltip label="...">` 的 `label` 是悬停触发文字，子节点是提示框里的富文本内容。

### 纯 Markdown 富文本

<Tooltip label="悬停查看 Markdown 富文本">
  ## Markdown 标题

  **粗体** / *斜体* / ~~删除线~~ / `代码`

  - 列表项 1
  - 列表项 2：<ItemImage id="minecraft:diamond" />

  > 这是一段引用。
</Tooltip>

### 嵌入 ItemImage / BlockImage

<Tooltip label="悬停查看物品和方块">
  <ItemImage id="minecraft:diamond" scale="2" /> 钻石

  <BlockImage id="minecraft:diamond_block" scale="3" /> 钻石块
</Tooltip>

### 嵌入配方

<Tooltip label="悬停查看熔炉配方">
  <RecipeFor id="minecraft:iron_ingot" handlerId="smelting" />
</Tooltip>

### 嵌入 3D 预览

<Tooltip label="悬停查看 3D 预览">
  <GameScene width="160" height="128" zoom={5} interactive={false}>
    <Block id="minecraft:chest" />
    <Block id="minecraft:furnace" x="1" />
  </GameScene>
</Tooltip>

### 混合富文本

<Tooltip label="悬停查看混合内容">
  <RecipeFor id="minecraft:wooden_door" />

  ---

  <GameScene width="160" height="128" zoom={5} interactive={false}>
    <Block id="minecraft:wooden_door" />
    <Block id="minecraft:wooden_door" y="1" />
  </GameScene>

  <ItemImage id="minecraft:wooden_door" /> 木门：两格高，可被玩家手动或红石信号开关。
</Tooltip>
