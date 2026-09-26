# 例

GuideNH には `wiki/resourcepack/` に実行時ガイドのサンプルが含まれています。このページでは、主なサンプルファイルと、それぞれが示す機能を対応付けます。

## 基本サンプルページ

| 実行時ファイル | 確認できる内容 |
| --- | --- |
| `.../_en_us/index.md` | frontmatter、アイテム ID、レシピ、アイテム/ブロック画像、コマンドリンク、ツールチップ、シーン、注釈、`ImportStructureLib`、`RemoveBlocks`、`BlockAnnotationTemplate`、ホーム推薦 |
| `.../_en_us/markdown.md` | 通常の Markdown、表、`recommend: 0` の例 |
| `.../_en_us/rendering.md` | ブロックレベルの描画とレイアウト動作 |
| `.../_en_us/scene-blocks.md` | 静的およびインタラクティブなブロックシーン |
| `.../_en_us/japanese.md` | ナビゲーションの子ページ |
| `.../_en_us/navigation-guide.md` | ナビゲーション、リンク、高優先度推薦 |
| `wiki/resourcepack/assets/gregtech/guidenh/_en_us/index.md` | ガイド間リンクと同名ページ分離を確認する第 2 namespace の例 |

## アセットの例

| 実行時ファイル | 用途 |
| --- | --- |
| `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png` | ページローカル画像の例 |
| `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt` | `<ImportStructure>` と `<RemoveBlocks>` に使うルート共有構造アセット |

## サンプル断片

### Frontmatter とナビゲーション

```yaml
item_ids:
  - minecraft:book
navigation:
  title: Root
  icon_texture: test1.png
  recommend: 3
```

### ホーム推薦

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

### 相対画像

````md
![Test Image](test1.png)
````

### Namespace 付きガイドリンク

````md
[同じ namespace](guide.md)
[ルートから同じ namespace](/guide.md)
[別 namespace](gregtech:guide.md)
[ルートから別 namespace](gregtech:/guide.md)
````

### ルート構造アセット

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

### StructureLib シーンの読み込み

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

子タグはシーンの StructureLib 既定値を設定します。シーンビューをリセットすると、これらの値も復元されます。

### 読み込んだ構造の整理

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <RemoveBlocks id="minecraft:glowstone" />
</GameScene>
````

### シーン注釈ツールチップ

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

### レシピフィルター

````md
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

## どの例を使うか

- パーサーの基本を確認する場合は `markdown.md` から始めます。
- StructureLib のツールチップ、ハッチ強調、整理動作を含む複数の実行時機能を同時に確認する場合は `index.md` を使います。
- 静的なブロック配置だけを確認する場合は `scene-blocks.md` を使います。
- 再利用可能な構造アセットを読み込む場合は `example_structure.snbt` を使います。

## 推奨する学習順

1. [はじめに](Guide-Page-Format)
2. [ガイドページ形式](Guide-Page-Format)
3. [タグリファレンス](Tags-Reference)
4. [GameScene](GameScene)
5. [注釈](Annotations)
6. [レシピ](Recipes)
