# ガイドページ形式

GuideNH の実行時ページは Markdown ファイルです。標準 Markdown のブロック/インライン構文、YAML frontmatter、GFM 表、取り消し線、`==text==` のマーク強調、`++text++`/`^^text^^`/`::text::` の下線拡張、MDX コメント（`{/* ... */}`）、MDX 形式のカスタムタグを解析します。

## 対応する Markdown

見出し、段落、太字/斜体/取り消し線/コード、リンク、画像、URL とメールの自動リンク、参照リンク、順序付き/順序なしリスト、GFM タスクリスト、引用、GitHub アラート、水平線、フェンス/インデントコードブロック、GFM 表、脚注、小文字 HTML タグ、MDX コメントを使用できます。

実際の例は `wiki/resourcepack/assets/guidenh/guidenh/_en_us/markdown.md` を参照してください。

## マーク強調

インライン強調には `==text==` を使います。色を指定する場合は `<mark color="#8A6A00">text</mark>` を使います。既定の背景は白い文字が読みやすい暗い金色です。

## コードブロック

`java`、`lua`、`scala`、`csv`、`mermaid` などの明示的フェンス、言語省略時の自動推測、言語ラベル、ゲーム内ビューアーのコピー、軽量シンタックスハイライトに対応します。

````md
```lua
ローカル 値 = 42
print(値)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````


インデントコードブロックも使用できます。

````md
    print("indented code")
````


フェンスが `mermaid` の mindmap なら通常のコードではなくインタラクティブなマインドマップになります。`csv` のフェンスは実行時表として描画されます。言語を省略した CSV 風テキストはコードブロックのままです。

````md
```csv widths=120,80
名前,値
iron,42
gold,17
```
````


ヘッダー指定と幅リストも使えます。

````md
```csv widths="120,80" header=false
名前,値
iron,42
gold,17
```
````


通常の段落では GFM 形式の URL、`www.` ホスト、メールアドレスの自動リンクも使えます。

````md
Visit https://example.com/docs, www.example.org, or guide@example.com
````


## Mermaid マインドマップ

実行時 Mermaid 対応の中心は `mindmap` です。`mermaid` フェンス、自動判定、`<Mermaid>`、外部 `src`、リッチ Markdown ラベル、`<NodeContent>`、ドラッグ移動、`layout: tidy-tree`、標準ノード形状、`::icon(...)` と `:::class` メタデータを使用できます。
外部図は `<Mermaid src="./diagram.mmd" />` の形式で読み込めます。

````md
```mermaid
mindmap
  ルート((GuideNH))
    実行時
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      ドラッグ へ pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]
    export["Site export"]

<NodeContent id="runtime">
ランタイムノードではテキスト、リンク、ブロックを組み合わせられます。

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_en_us/test1.png)
</NodeContent>
</Mermaid>
````


まだ対応していない Mermaid 図は通常の Mermaid ラベル付きコードブロックへフォールバックします。

## CSV 表のインポート

明示タグで CSV ファイルを読み込めます。

````md
<CsvTable src="./markdown-table.csv" />
````


`src` は現在のページから、実行時アセットやシーンの `src` と同じ規則で解決されます。幅ヒントも指定できます。

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````


フェンス内に CSV を直接記述することもできます。

````md
```csv
名前,値
iron,42
gold,17
```
````


## Markdown 表の幅ヒント

表の直後に実行時属性行を置くと、GFM 表に表示列幅を指定できます。

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````


表自体は標準 Markdown のまま、GuideNH が実行時だけ列幅を適用します。

## タスクリスト、アラート、脚注

`- [ ]`/`- [x]`、`[!NOTE]` などの GitHub アラート、脚注の参照と定義に対応します。

````md
- [x] done item
- [ ] todo item

> [!NOTE]
> alert body

Footnote ref[^one]

[^one]: tooltip text
````


脚注参照はツールチップ風のインラインマーカーになり、ページ下部にコンパクトな脚注一覧が追加されます。

## リスト幅のカスタマイズ

標準 Markdown リストに幅指定はないため、GuideNH のコンテナで制限します。

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````


## 参照リンクと画像

CommonMark の参照定義を使用できます。

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````


## 小文字 HTML の実行時タグ

`<a>`、`<br>`、`<kbd>`、`<sub>`、`<sup>`、`<details>` のサブセットを直接処理します。その他の生 HTML はブラウザー HTML ではなくリテラルテキストとして扱います。

````md
Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./subpage.md" title="Open subpage">Open subpage</a><br clear="all" />

<details open>
<summary>More</summary>

Body text
</details>
````


## MDX コメント

MDX コメントは Markdown コンパイル前に無視されます。

````md
表示されるテキスト。{/* 非表示のインラインコメント */}

{/*
multiline comment
*/}

More visible text.
````


## frontmatter

最初の YAML frontmatter ブロックから既知のキーを読み取ります。

| キー | 型 | 意味 |
| --- | --- | --- |
| `navigation` | map | ページをナビゲーションツリーへ追加 |
| `categories` | 文字列リスト | MediaWiki 形式カテゴリへ追加 |
| `item_id` / `item_ids` | アイテムフィルター式 | `<ItemLink>` から発見可能にする |
| `ore_ids` | 鉱石辞書名リスト | 鉱石辞書アイテムから発見可能にする |
| `quest_ids` | BetterQuesting ID リスト | `<QuestLink>`/`<QuestCard>` とキー操作に対応 |
| `author` / `authors` | 文字列またはリスト | 下部バーに作者を表示 |
| `date` / `updated` | 文字列または日付 | 作成日/更新日を表示 |
| `zoom` | 正の浮動小数点数 | ページごとのコンテンツ倍率 |
| その他 | YAML 値 | `additionalProperties` に保持 |

### `navigation`

`title` は必須です。`keyword`、`keywords`、`parent`、`position`、`priority`、`icon`、`icons`、`icon_texture`、`icon_textures`、`required_mod`、`required_mods`、`excluded_mod`、`excluded_mods` を指定できます。

```yaml
item_id: minecraft:potion 16384-16462,!16386
item_ids:
  - ae2:white_paint_ball:*
  - "<minecraft:wool:14>"
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book:0:{display:{Name:"My Custom Book"}}
  # Use meta/damage to select a specific subtype:
  # icon: minecraft:wool:1       (orange wool, colon form)
  # Cycling icons list — cycles one per second:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4:{display:{Name:"Custom Green Wool"}}
  #   - minecraft:wool:14

  icon_texture: test1.png
  # Cycling textures:
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples|Examples Overview
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: ExampleAuthor
date: 2024-01-15
updated: 2024-06-01
```


カテゴリは通常名または `category|sort key` として受け付けます。BetterQuesting の ID は正規 UUID または短縮 Base64 形式を受け付け、同じクエストの両方を併記しないでください。作者、日付のいずれかが存在すると画面下部に情報バーが表示されます。

```yaml
authors:
  - Alice
  - Bob
  - Charlie   # only Alice and Bob are shown, "..." appended
```

```yaml
authors:
  - name: Alice
  - name: Bob
```


### `zoom`

`zoom` は単一ページのコンテンツ倍率です。`1.0` は標準、`1.5` は 150%、`0.75` は 75% です。ModConfig のグローバル `contentZoom` と乗算され、幅を再計算して折り返しとブロック形状を維持します。

```yaml
zoom: 1.5
navigation:
  title: My Dense Page
```


## リンクの解決

ページリンクは現在の namespace 内で相対解決され、`/` は namespace ルート、`modid:path` は明示 namespace、`#anchor` は見出しアンカーです。アセットリンクも同じ規則で解決されます。

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```


namespace が異なる同名ページへは誤ってフォールバックせず、存在しない場合はリンクエラーを表示します。

## アイテム参照構文

`icon`、`icons`、アイテム ID を受け取るタグは次の形式を使います。

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```


メタを省略すると `0`、最初の `{` 以降は SNBT として解析されます。ワイルドカードメタと SNBT を組み合わせられます。

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```


### アイテムインデックス式

`item_id` は 1 式、`item_ids` は同じ式の YAML リストです。空白は項、`|` は選択肢、`,` は規則、`!` は除外を表します。完全一致、メタワイルドカード、式の順に検索されます。

undefined

## エラー処理

ページを解析できない場合もガイドをクラッシュさせず、エラーページを生成します。不正なタグ、ID、属性はインラインエラーとして表示します。

## 関連ページ

- [ナビゲーション](Navigation)
- [画像とアセット](Images-And-Assets)
- [タグリファレンス](Tags-Reference)

## 追加の構文例

ページ内リンクには `<a>` を使い、ナビゲーション分類には `<Category>` と `<Special>` を使います。
Mermaid の `<Mermaid>` コンテナは複数の `<NodeContent>` を持てます。アイテム参照には既存の
`<ItemLink>` 構文を使用します。
