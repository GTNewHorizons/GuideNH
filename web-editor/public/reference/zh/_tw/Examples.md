[English](Examples)

# 範例

GuideNH 已经在 `wiki/resourcepack/` 中内置了一份运行时示例指南。本页将重要示例文件与它们所展示的功能对应起来。

## 核心範例頁面

| 執行階段文件 | 展示內容 |
| --- | --- |
| `.../_zh_cn/index.md` | frontmatter、item ids、配方、物品/方塊圖片、命令連結、tooltip、場景、註解、`ImportStructureLib`、`RemoveBlocks`、`BlockAnnotationTemplate`、首頁推薦範例 |
| `.../_zh_cn/markdown.md` | 普通 Markdown 功能、表格，以及 `recommend: 0` 範例 |
| `.../_en_us/rendering.md` | 區塊級渲染與佈局行為 |
| `.../_en_us/scene-blocks.md` | 靜態與互動式方塊場景範例 |
| `.../_en_us/japanese.md` | 導覽子頁面範例 |
| `.../_zh_cn/navigation-guide.md` | 导航、链接和高优先级推荐示例 |
| `wiki/resourcepack/assets/gregtech/guidenh/_zh_cn/index.md` | 第二个命名空间示例，用于展示跨指南链接和同名页面隔离 |

## 資源範例

| 執行階段文件 | 用途 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | 頁面私有圖片範例 |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | 供 `<ImportStructure>` 和 `<RemoveBlocks>` 使用的共享根结构资源 |

## 範例片段

### Frontmatter + Navigation

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### 首頁推薦

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

```yaml
navigation:
  title: Navigation & Index
  parent: index.md
  recommend: 5
```

### 相對圖片

````md
![Test Image](test1.png)
````

### 帶有命名空間的指南鏈接

````md
[同命名空间](guide.md)
[同命名空间根路径](/guide.md)
[其他命名空间](gregtech:guide.md)
[其他命名空间根路径](gregtech:/guide.md)
````

### 根路徑結構資源

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib 场景导入

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="hatch" value="1" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

子标签会设置 StructureLib 场景默认值，点击 reset view 时会恢复这些默认值。

### 導入結構後的清理

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### 場景註解 Tooltip

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### 配方過濾

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## 什麼時候該用哪個範例

- 若你在驗證解析器基礎能力，先看 `markdown.md`
- 若要一起測試混合運行時特性，包括 StructureLib tooltip、艙口高亮和清理邏輯，用 `index.md`
- 若只需要靜態方塊佈局預覽，用 `scene-blocks.md`
- 若你需要可重複使用的導入結構資源，用 `example_structure.snbt`

## 推薦學習順序

1. [快速开始](Guide-Page-Format)
2. [指南頁面格式](Guide-Page-Format)
3. [标签参考](Tags-Reference)
4. [游戏场景](GameScene)
5. [注解](Annotations)
6. [配方](Recipes)
