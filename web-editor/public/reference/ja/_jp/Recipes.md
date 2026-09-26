# レシピ

GuideNH は、ガイドページ内にクラフトレシピと NEI が提供するレシピを直接描画できます。

## 対応タグ

- `<Recipe>`
- `<Usage>`
- `<RecipeFor>`
- `<RecipeUsage>`
- `<RecipesFor>`
- `<RecipesUsage>`

これらのタグはすべて同じコンパイラーと属性セットを共有します。

## タグの意味

| タグ | 動作 |
| --- | --- |
| `<Recipe>` | 対象アイテムの単一レシピを描画します |
| `<Usage>` | 対象アイテムを材料として使う単一レシピを描画します |
| `<RecipeFor>` | 単一レシピ形式。記述しやすい名前です |
| `<RecipeUsage>` | 単一レシピ形式。記述しやすい名前です |
| `<RecipesFor>` | 条件に一致する複数のレシピを描画します |
| `<RecipesUsage>` | 条件に一致する複数の使用レシピを描画します |

複数のレシピが存在する場合、単一レシピ形式では、フィルターでさらに絞らない限り 1 件だけ描画されます。

## 共通属性

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | 対象アイテムの参照 |
| `fallbackText` | いいえ | 利用できるレシピがない場合に表示するテキスト |
| `handlerName` | いいえ | ハンドラー名に対する大文字小文字を区別しない部分一致フィルター |
| `handlerId` | いいえ | オーバーレイまたはハンドラー ID の完全一致フィルター（大文字小文字を区別しない） |
| `handlerBlacklist` | いいえ | 結果から除外するハンドラーのカンマ区切りリスト |
| `handlerWhitelist` | いいえ | ブラックリストに入っていても残すハンドラーのカンマ区切りリスト |
| `handlerOrder` | いいえ | ハンドラーをフィルターした後の 0 始まりの位置 |
| `input` | いいえ | 材料フィルター式 |
| `output` | いいえ | 結果フィルター式 |
| `limit` | いいえ | 描画するレシピ数の正の整数上限 |

## アイテム ID の構文

`id`、`input`、`output` 属性はすべて GuideNH 拡張アイテム参照形式を使います。

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

メタ値のワイルドカード:

- `*`
- `32767`
- `ANY` などの大文字トークン

## フィルター式の構文

`input` と `output` のフィルターでは次を使えます。

- `,` は OR
- `&` は と
- `!` は しない

例:

```text
minecraft:planks:*
minecraft:stick&minecraft:redstone
!minecraft:planks:0
minecraft:planks:*,minecraft:log:*
```

NBT は 3 つのアイテム参照属性すべてと組み合わせられます。SNBT の複合体やリスト内にあるカンマとアンパサンドは参照の一部として保持され、トップレベルの区切り文字だけがフィルターを分割します。

```md
<RecipeFor id='minecraft:written_book:0:{title:TestBook,author:GuideNH}' />
<RecipesFor
  id='minecraft:stone:3:{display:{Name:"Special Stone"}}'
  input='minecraft:chest:0:{Items:[{id:"minecraft:diamond",Count:1b}]}'
  output='minecraft:diamond:0:{display:{Name:"Reward"}}'
/>
```

## 描画順序

GuideNH は次の順でレシピを試します。

1. NEI ネイティブハンドラーの描画
2. NEI スロットデータのフォールバック
3. 内蔵バニラクラフトのフォールバック

一致するものがない場合:

- `fallbackText` があれば、それを使用します。
- それ以外では、記述エラーをインライン表示します。

## 例

### 単一レシピ

````md
<RecipeFor id="minecraft:crafting_table" />
````

### 複数レシピ

````md
<RecipesFor id="minecraft:torch" />
````

### ハンドラーのフィルター

````md
<RecipesFor id="minecraft:iron_pickaxe" handlerId="repair" />
<RecipesFor id="minecraft:fire_charge" handlerName="shapeless" />
````

### ハンドラーのブラックリストとホワイトリスト

`handlerBlacklist` は結果からハンドラーを除外し、`handlerWhitelist` は除外されたハンドラーを戻します。どちらも **カンマ区切りリスト**を受け取り、ハンドラー ID、オーバーレイ ID、クラス名のいずれかに対して大文字小文字を区別しない部分一致を行います。

ブラックリストに名前があるハンドラーは除外されます。ただし、タグが明示的に要求した場合は除外されません。`handlerId`、`handlerWhitelist`、小文字化したクラス名の一致はいずれもハンドラーを残します。

木材はクラフト材料と燃料の両方なので、不要なハンドラーを列挙できます。

````md
<RecipesFor id="minecraft:planks" handlerBlacklist="fuel,repair" />
````

クラス名にも部分文字列が使われるため、Mod ID を 1 つ指定すると、その Mod が登録したすべてのハンドラーを対象にできます。

````md
<RecipesFor id="minecraft:chest" handlerBlacklist="gregtech,ic2,railcraft" limit="6" />
````

ページ単位のホワイトリストでブラックリストを戻すこともできます。

````md
<Recipe id="minecraft:chest" handlerWhitelist="GTNEIMultiblockHandler,StructureCompatNEIHandler" fallbackText="Multiblock preview unavailable." />
````

ホワイトリスト単独では結果を絞りません。ブラックリストで除外されるハンドラーを救済するだけなので、1 件に絞る場合は `handlerId`、`handlerName`、`handlerOrder` を使い、描画数の上限には `limit` を使います。

設定ファイル `config/guidenh/guidenh.cfg` の `recipeHandlerBlacklist` はすべてのページに適用され、既定では次を含みます。

- `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
- `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`

ページ側のリストは設定済みリストに追加されます。そのため、ページでさらに隠すことはできますが、既定の除外を減らすことはできません。

### 入力・出力フィルター

````md
<RecipesFor id="minecraft:stick" input="minecraft:planks:*" limit="3" />
<RecipesFor id="minecraft:stick" output="minecraft:torch" limit="2" />
<RecipesFor id="minecraft:redstone_torch" input="minecraft:stick&minecraft:redstone" limit="1" />
````

### フォールバックテキスト

````md
<Recipe id="missingrecipe" fallbackText="This recipe is disabled." />
````

## 推奨事項

- オプション Mod の連携には `fallbackText` を指定します。
- 使用したい NEI ハンドラーが正確に分かっている場合は `handlerId` を使います。
- 1 つのブロックが多くのハンドラーから生成される場合は `handlerBlacklist` で表示対象を限定します。
- 次の 2 つのハンドラーは `config/guidenh/guidenh.cfg` で既定非表示です。表示するには `handlerId` または `handlerWhitelist` で指定します。
  - `blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler`
  - `blockrenderer6343.integration.structurelib.StructureCompatNEIHandler`
- タグが多くのレシピに展開される可能性がある場合は `limit` を使います。
- 複雑なフィルター式は、保守しやすいようにタグの近くのコメントへ残します。

## 実行時の例

ハンドラーフィルター、ワイルドカード、NBT 付きアイテム ID の詳しいレシピ例は `wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md` を参照してください。
