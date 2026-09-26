# 注釈

GuideNH のシーン注釈は `<GameScene>` / `<Scene>` 内の子タグです。ワールド空間に描画され、子 Markdown/タグコンテンツはリッチツールチップになります。

## 一般的な規則

- 注釈はシーン内でのみ動作します。
- 子コンテンツがツールチップ本文になります。
- シーン UI の切り替えで注釈を非表示にできます。
- 注釈の種類が対応していれば、`alwaysOnTop` はシーンのジオメトリより前に描画します。
- `<ImportStructureLib>` を使うシーンでは `showWhenStructure`、`showWhenTier`、`showWhenChannels` の条件も指定できます。

## 対応する注釈タグ

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

`<BlockAnnotationTemplate>` は、現在のシーンにすでに配置された一致ブロックごとに子注釈を適用します。

## StructureLib 条件

シーンに `<ImportStructureLib>` がある場合、注釈の表示を特定の StructureLib 状態に制限できます。

| 属性 | 意味 |
| --- | --- |
| `showWhenStructure` | 名前付き `<ImportStructureLib name="...">` に紐付けます。1 つだけなら省略できます |
| `showWhenTier` | `2`、`1..3`、`!2`、`1..5,!3` などのティアフィルター |
| `showWhenChannels` | `input:1..3, casing:!2, fluid:4` などのチャンネルフィルター |

`showWhenTier` と `showWhenChannels` は論理 と で結合されます。`!2` は「2 以外」を意味し、同じ属性は `<PlaySound>` と `<BlockAnnotationTemplate>` の子注釈にも使えます。

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    選択した StructureLib 状態でのみ表示されます。
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

単一の 1×1×1 ブロック領域を強調します。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `pos` | はい | `x y z` ベクトル |
| `color` | いいえ | `#RRGGBB`、`#AARRGGBB`、または `transparent` |
| `thickness` | いいえ | 線の太さ |
| `alwaysOnTop` | いいえ | 真偽値の式 |

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  コントローラーブロックを強調表示します。
</BlockAnnotation>
````

## `<BoxAnnotation>`

軸に揃った任意のボックスを強調します。各軸で `min` と `max` が逆なら自動的に入れ替えます。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `min` | はい | 最小 `x y z` ベクトル |
| `max` | はい | 最大 `x y z` ベクトル |
| `color` | いいえ | 注釈の色 |
| `thickness` | いいえ | 線の太さ |
| `alwaysOnTop` | いいえ | 真偽値の式 |

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  高さ半分の領域を強調します。
</BoxAnnotation>
````

## `<LineAnnotation>`

ワールド空間に線分または折れ線を描画します。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `from` | `points` がない場合は必須 | 始点 `x y z` |
| `to` | `points` がない場合は必須 | 終点 `x y z` |
| `points` | いいえ | セミコロン区切りの点。`from` / `to` を上書き |
| `color` | いいえ | 注釈の色 |
| `thickness` | いいえ | 線の太さ |
| `alwaysOnTop` | いいえ | 真偽値の式 |
| `arrow` | いいえ | `start` または `end`。省略時は矢印なし |
| `showPoints` | いいえ | 各点を小さな立方体で表示 |
| `pointColor` | いいえ | 点の色。省略時は線の色 |
| `pointSize` | いいえ | 点の大きさ。省略時は線幅より少し大きい値 |

`<LinePoint>` 子要素で点の表示、色、サイズを上書きできます。`index` は 0 始まりで、矢印は線の始点または終点にだけ置けます。

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  信号経路。
</LineAnnotation>
````

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  曲がり角を通る信号経路です。
</LineAnnotation>
````

## `<DiamondAnnotation>`

ワールド位置に画面向きのダイヤモンドマーカーを置きます。`color` の既定値は明るい緑です。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `pos` | はい | マーカー位置 `x y z` |
| `color` | いいえ | 色 |

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

シーン上に吹き出し型のテキストを描画します。ワールドアンカーに追従させることも、シーン中央を基準に固定することもできます。子コンテンツはホバーツールチップではなく吹き出し本文になります。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `pos` | いいえ | ワールドアンカー `x y z` |
| `x`, `y`, `z` | いいえ | `pos` を省略したときの各成分 |
| `text` | いいえ | 本文。省略時は子 Markdown |
| `textKey` | いいえ | `lang` ファイルから解決する翻訳キー |
| `color` | いいえ | 境界線色。既定は薄い灰色 |
| `backgroundAlpha` | いいえ | 背景不透明度 `0`～`255`。既定 `204` |
| `maxWidth` | いいえ | 折り返し幅。`0` は 1 行 |
| `independent` | いいえ | `true` なら画面空間に固定 |
| `yOffset` | いいえ | 独立吹き出しの垂直オフセット |
| `connectorSide` | いいえ | `bottom`、`top`、`left`、`right`、`none` |
| `connectorOffset` | いいえ | 吹き出し端に沿う接続位置 |
| `connectorLength` | いいえ | 接続線の長さ。既定 `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | いいえ | 追加の強調ボックス境界 |
| `highlightColor` | いいえ | 強調ボックスの色 |

ワールドアンカーの吹き出しは接続線を描画します。`connectorSide`、`connectorOffset`、`connectorLength` で接続位置と長さを調整できます。独立吹き出しは中央配置され、接続線を描画しません。Ponder の `text` 注釈を読み込んだ場合も同じ実行時注釈が使われます。

````md
<TextAnnotation
  pos="1.5 2 1.5"
  textKey="guidenh.sample.scene.insert_items"
  color="#FF44AAFF"
  maxWidth={120}
  backgroundAlpha={180}
  connectorSide="right"
  connectorOffset={8}
  connectorLength={12}
>
  **優先度**を指定してアイテムをここに挿入します。
</TextAnnotation>
````

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## リッチツールチップの内容

注釈の子要素は通常の GuideNH コンテンツとしてコンパイルされます。Markdown の段落と見出し、アイテム/ブロック画像、レシピ、ネストされた非インタラクティブシーンを含められます。

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

一致するすべての既存ブロックへ同じ注釈を適用します。テンプレートは解析時点で存在するブロックだけを対象にし、対象となる `<Block>`、`<ImportStructure>`、`<ImportStructureLib>` の後に置きます。子注釈の座標は一致ブロックからの相対座標です。

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | `modid:block[:meta]` 形式のブロックマッチャー |

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      テンプレートで生成されたツールチップ
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## 関連ページ

- [GameScene](GameScene)
- [例](例s)
