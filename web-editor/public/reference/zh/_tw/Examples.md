# 示例

GuideNH 在 `wiki/resourcepack/` 中內建了一份執行時示例指南。本頁將重要示例檔案與它們所展示的功能對應起來。

## 核心示例頁面

下表檔案位於 `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/`。

| 檔案 | 用途 |
| --- | --- |
| `index.md` | 分類入口 |
| `markdown.md` | Markdown 語法 |
| `markdown-table.md` | 表格與合併單元格 |
| `rendering.md` | 塊級佈局 |
| `images.md` | 圖片與紋理裁剪 |
| `tooltips.md` | 懸停說明 |
| `recipes.md` | 物品/流體配方及篩選 |
| `charts.md` | 圖表、參考線及流體圖示 |
| `function-graph.md` | 函式繪圖 |
| `content-embed.md` | 內容嵌入與文字環繞 |
| `ponder.md` | 動畫時間線 |
| `scene-blocks.md` | 方塊與材料清單 |
| `scene-import.md` | 結構匯入與方塊編輯 |
| `scene-entities.md` | 實體預覽 |
| `scene-camera.md` | 相機控制 |
| `scene-annotations.md` | 場景註解 |
| `scene-particles.md` | 粒子與天氣 |
| `navigation-guide.md` | 導航、索引繫結與圖示 |
| `page-lang-key.md` | 整頁翻譯 |
| `templates-demo.md` | 模板與包含控制 |
| `debug.md` | 佈局與互動檢查 |
| `japanese.md` | 語言與導航示例 |

模板定義在 `templates/` 子目錄。`wiki/resourcepack/assets/gregtech/guidenh/` 演示另一個名稱空間的頁面與跨指南連結。

## 資源示例

| 執行時檔案 | 用途 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | 頁面私有圖片示例 |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | 供 `<ImportStructure>` 和 `<RemoveBlocks>` 使用的共享根結構資源 |

## 示例片段

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

### 帶名稱空間的指南連結

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

### StructureLib 場景匯入

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

子標籤會設定 StructureLib 場景預設值，點選 reset view 時會恢復這些預設值。

### 匯入結構後的清理

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### 場景註解 Tooltip

````md
<GameScene>
  <DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
    ### Activated Beacon
    <RecipeFor id="minecraft:furnace" />
  </DiamondAnnotation>
</GameScene>
````

### 配方過濾

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### 數量格式化

使用 `<Amount>` 表示數量。`unit` 可省略，預設只顯示數字；`fluid` 遵循 GTNHLib 的 `mB`/`L` 偏好。

````md
<Amount value="250" />
<Amount value="250" unit="fluid" />/s
<Amount value="1200000" format="compact" /> 个物品
````

## 什麼時候該用哪個示例

- 若你在驗證解析器基礎能力，先看 `markdown.md`
- 若你要一起測試混合執行時特性，包括 StructureLib tooltip、艙口高亮和清理邏輯，用 `index.md`
- 若你只需要靜態方塊佈局預覽，用 `structure.md`
- 若你需要可複用的匯入結構資源，用 `example_structure.snbt`
- 若要測試模板轉譯、條件判斷與包含控制，用 `templates-demo.md` 配合 `templates/` 目錄。參見[模板](Templates)

## 推薦學習順序

1. [快速開始](Getting-Started)
2. [指南頁面格式](Guide-Page-Format)
3. [標籤參考](Tags-Reference)
4. [遊戲場景](GameScene)
5. [註解](Annotations)
6. [配方](Recipes)
