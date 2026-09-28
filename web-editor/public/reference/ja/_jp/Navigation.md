# ナビゲーション

GuideNH はページの frontmatter からナビゲーションツリーを作成します。

ゲーム内サイドバーでは、展開された祖先ページが上部に固定され、表示中の子ページがその下をスクロールします。複数の祖先階層を同時に固定でき、固定行は表示中のサブツリー全体が画面外へ出たときだけ押し出されます。動作は VSCode のファイルエクスプローラーに似ています。

## ナビゲーション frontmatter

`navigation` マップで、そのページをガイドツリーに表示するかを制御します。

```yaml
navigation:
  title: Structure Preview
  parent: index.md
  position: 20
  icon: minecraft:diamond_block
```

### フィールドリファレンス

| フィールド | 説明 |
| --- | --- |
| `title` | 必須の表示タイトル |
| `keyword` | 任意の検索キーワードまたは別名 1 件 |
| `keywords` | 任意の検索キーワード/別名リスト |
| `parent` | 任意の親ページ ID。ガイドページリンクと同じ規則で解決 |
| `position` | 同じ階層の並び順ヒント |
| `recommend` | ホーム推薦の優先度。省略時は推薦パネルに表示しない |
| `priority` | 同じパスの上書きに使う読み込み優先度。既定値 `0` |
| `icon` | 任意のアイテムアイコン。`mod:item:meta:{snbt}` の末尾に対応 |
| `icons` | 切り替え表示するアイテムアイコンのリスト |
| `icon_texture` | ガイドアセットから解決するテクスチャーアイコン |
| `icon_textures` | 切り替え表示するテクスチャーアイコンのリスト |
| `required_mod` | 任意の Mod ID。Mod がないとページを非表示 |
| `required_mods` | 任意の Mod ID リスト。すべて読み込まれている必要があります |
| `excluded_mod` | 任意の Mod ID。読み込まれているとページを非表示 |
| `excluded_mods` | 任意の Mod ID リスト。いずれかが読み込まれていると非表示 |

### `navigation.position`

`navigation.position` は同じ階層のページを並べ替える任意の整数です。

- 省略時は `0`
- 大きい値ほど前に表示
- 同じ値ならタイトルをアルファベット順に比較

### 検索キーワード

`navigation.keyword` は検索別名を 1 件追加し、`navigation.keywords` は別名リストを追加します。両方を併用でき、値はトリミングされ重複が削除されます。タイトルと本文と同じ言語アナライザーと前方一致を使います。

```yaml
navigation:
  title: Molecular Assembler
  keyword: assembler
  keywords:
    - molecular assembler
    - autocrafting machine
```

## ホームページ推薦

### `navigation.recommend`

`navigation.recommend` はホームの推薦パネルで使う任意の整数です。

- フィールドが存在するページだけが表示されます。
- `0` も有効です。
- 大きい値ほど前に表示されます。
- 同じ値ならタイトル順です。
- 推薦項目は `GuidePage` 単位で、クリックするとそのページへ直接移動します。

```yaml
navigation:
  title: Steam Stage Checklist
  parent: index.md
  recommend: 0
```

## Mod 要件

`required_mod` / `required_mods` で必要な Mod を指定し、`excluded_mod` / `excluded_mods` で互換性のない Mod がある場合に隠します。条件を満たさないページはナビゲーションツリーとアイテム/カテゴリなどすべてのインデックスから除外され、検索でも見つかりません。

```yaml
navigation:
  title: Applied Energistics Integration
  parent: index.md
  required_mod: appliedenergistics2

navigation:
  title: Multi-Mod Feature
  parent: index.md
  required_mods:
    - gregtech
    - appliedenergistics2
```

両方のキーを組み合わせられます。その場合、必要な Mod がすべてあり、除外された Mod が 1 つもない場合だけ表示します。

```yaml
navigation:
  title: Client-only Integration
  parent: index.md
  required_mod: guide_api
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## 読み込み優先度

複数のリソースパックが同じページパスを提供する場合、GuideNH は frontmatter を先に読み、`navigation.priority` が最大の候補を選びます。

```yaml
navigation:
  title: Pack Override
  parent: index.md
  priority: 100
```

省略時は `0`、値は `2147483647` までの符号付き Java 整数です。大きい値が勝ち、同値なら後から処理されたリソースパックが勝ちます。優先度は同じページパスと言語/フォールバック層の候補間だけで使われます。

## アイコンのソース

GuideNH は次の順でアイコンを選びます。

1. 1 件以上設定された `icon_textures`
2. 正常に読み込めた `icon_texture`
3. 解決できるアイテムが 1 件以上ある `icons`
4. 存在する `icon`
5. 使用できるものがなければアイコンなし

テクスチャーアイコンは実行時アセットから読み込むため、`test1.png` のようなページ相対ファイルも使えます。

## 親ノードとルートノード

- `parent` を省略するとルートノードになります。
- `parent: index.md` などを設定すると子ノードになります。
- 親ページは同じガイドツリーに存在する必要があります。

`navigation.parent` は Markdown ページリンクと同じ namespace 規則を使います。`parent: index.md` と `parent: ./index.md` は現在の namespace 内、`parent: /index.md` は namespace ルート、`parent: gregtech:index.md` は明示的な別 namespace です。データ駆動ガイドは namespace ごとに分離され、同名ページへ誤ってフォールバックしません。

## カテゴリページ

frontmatter で 1 つ以上のカテゴリに参加できます。

```yaml
categories:
  - basics
  - machines|Arc Furnace
```

各項目はカテゴリ名、または `category|sort key` です。`<Category name="machines" rows="3" />` で一覧を取得でき、`Category:machines` のような非表示検索ページも自動作成されます。`Special:AllPages` と `Special:Categories` も自動作成されます。

## アイテムインデックスページ

`item_id`（単一値）または `item_ids`（リスト）でアイテムからページへの対応を登録できます。

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:compass
  - minecraft:wool:*
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:iron_ore#crafting
```

これらは `<ItemLink>` から使われます。`minecraft:potion 16384-16462,!16386` は `16386` を除く範囲、`minecraft:potion 0-16,20-36,!28` は 2 範囲から `28` を除く式です。`ae2:white_paint_ball:*` は全メタデータに一致します。任意の `#anchor` を末尾に付けると、クリック時に見出しへスクロールします。

検索順序:

1. アイテムとメタの完全一致
2. ワイルドカードメタのフォールバック
3. 一致するアイテム式

## 見出しアンカーリンク

Markdown リンクと `<a>` タグで見出しアンカーへ移動できます。見出しを小文字にして空白をハイフンへ変換します。

```md
[Jump to Installation](#installation)
[Jump to Crafting Recipe](#crafting-recipe)
```

```md
[See Getting Started](./Guide-Page-Format#installation)
[Another guide](other-guide.md#usage)
```

```md
[Absolute link](guidenh:other-guide.md#usage)
[Any namespace](mymods:crafting/iron.md#smelting)
```

`namespace:path` 形式はその ID のページを解決し、相対 `../` を使わずに済みます。MDX では `<a name="...">` で名前付きアンカーも置けます。

```md
<a name="custom-anchor" />

...content...

[Jump here](#custom-anchor)
```

## `<SubPages>`

`<SubPages>` はナビゲーションの子ページへのリンクを描画します。

| 属性 | 型 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `id` | ページ ID または空文字列 | 現在のページ | 子を一覧するページ |
| `alphabetical` | 真偽値の式 | `false` | ナビゲーション順ではなくタイトル順 |

````md
<SubPages />
<SubPages id="index.md" />
<SubPages id="" alphabetical={true} />
````

`id=""` はルートノードを一覧します。

## `<Category>`

`<Category>` は指定カテゴリのすべてのページへのリンクを描画します。

| 属性 | 型 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `name` | 文字列 | なし | 描画するカテゴリ名 |
| `rows` | 正の整数 | `3` | MediaWiki 形式の表示列数 |

````md
<Category name="machines" rows="3" />
````

カテゴリがない場合はインラインエラーを描画します。検索用の非表示ページは `Category:machines` です。

## `<Special>`

`<Special>` は組み込みの MediaWiki 形式特殊ページ一覧を描画します。

| 属性 | 型 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `name` | 文字列 | なし | `AllPages` または `Categories` |
| `rows` | 正の整数 | `3` | 表示列数 |

````md
<Special name="AllPages" rows="4" />
<Special name="Categories" rows="3" />
````

同じ内容は `Special:AllPages` と `Special:Categories` の非表示検索ページでも利用できます。

## 検索結果のタイトル

検索タイトルは次の順で決まります。

1. `navigation.title`
2. 最初のレベル 1 見出し（`# Heading`）
3. 生のページ ID

## 関連ページ

- [ガイドページ形式](Guide-Page-Format)
- [検索](Search)
- [タグリファレンス](Tags-Reference)
