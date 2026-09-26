# ローカライズ

GuideNH は、ローカライズされたガイドページとガイド用アセットに対応しています。

## フォルダー構成

実行時のローカライズはフォルダー単位で行われます。

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

リポジトリのサンプルリソースパックでは、ドキュメントに記載されている外側の `assets/` 階層を維持します。

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

言語フォルダーとして認識されるのは、先頭にアンダースコアがある形式だけです。`en_us/` や `zh_cn/` のような通常のフォルダーは、ローカライズのルートとして扱われません。

## ページの検索順序

要求されたページ ID ごとに、GuideNH は次の順で検索します。

1. `_<current language>/<page>`
2. 現在の言語のページがない場合は `_<default language>/<page>`
3. 言語フォルダーを持たない `<page>`

ガイドページがフォールバックするのは、そのガイドの `defaultLanguage` だけです。自動検出されたリソースパックガイドの既定値も `en_us` のままなので、別の言語が存在するだけでフォールバック言語に昇格することはありません。

## ページ Lang キーによる上書き

ガイドページは `.lang` キーから完全な Markdown ソースを取得して置き換えることもできます。ただし、対応する物理 `.md` ファイルがすでに存在している場合に限ります。ファイルはページの存在判定とフォールバック元を兼ねます。

- GuideNH はまず通常の言語フォールバック順でページファイルを解決します。
- ファイルが見つかった後、要求された言語のページ用 `.lang` 値を探します。
- キーが存在し、値が空でなければ、解析前にその値全体をページの Markdown ソースとして使用します。
- キーがない、または空の場合は、解決されたファイルの内容にフォールバックします。

空でない `.lang` ページ値を使う場合、GuideNH は解析前に、解決された物理ページから不足している frontmatter フィールドを補います。ローカライズ側で明示した frontmatter は常に優先されるため、翻訳した `navigation.title` はそのまま保持されます。一方、`navigation.recommend`、`navigation.priority`、カテゴリ、アイテムリンク、著者メタデータ、ページのズームなど、新しい構造用フィールドはフォールバック元の `.md` から継承できます。これにより、基準ページにホームページ推薦のメタデータが追加されても、古い完全翻訳が推薦情報を誤って失うことはありません。

キーの形式は次のとおりです。

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

例:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

パス区切りはキー内で `.` になります。パスの各セグメントにリテラルのピリオドが含まれる場合、セグメント区切りと衝突しないようにエスケープされます。

```text
foo.bar.md -> foo_x2e_bar
```

英数字以外の文字も、同じ `_x<hex>_` 形式でエスケープされます。

`.lang` 値に含まれるリテラルの `\n` と `\r` は、Markdown の解析前に実際の改行へ変換されます。そのため、frontmatter、見出し、リスト、MDX タグを含む完全なページを値に記述できます。

作成時の規則:

- キーは物理 `.lang` ファイル内の 1 行として記述します。
- 値の中で Markdown の改行を表すにはリテラルの `\n` を使います。
- Forge は `.lang` ファイルを行単位で読むため、値の中に実際の改行を挿入しないでください。
- 最終的な Markdown ソースに文字 `\n` をそのまま含めたい場合を除き、`\\n` は記述しないでください。

GuideNH は `.lang` エントリだけからページを生成しません。実際のページファイルが必ず存在している必要があります。

## キーの長さ

GuideNH はページキーに追加の文字数制限を設けていません。Minecraft 1.7.10 / Forge では、基盤となる言語データは実質的に文字列プロパティのマップです。したがって実用上の制限は、専用の上限ではなく通常のメモリ使用量と保守性です。短いページパスのほうがキーを作成・確認しやすくなります。

## 作成時のヒント

- ガイドのフォールバック言語を英語以外にしたい場合は、`defaultLanguage` を明示的に設定します。
- 言語に依存しない共有ページは、異なる言語へのフォールバックを意図している場合だけ作成します。
- まずページを翻訳し、アセット内にテキストを埋め込む場合にだけアセットも翻訳します。
- ルートから共有できるアセットで足りる場合は、言語固有のアセットファイル名を避けます。

## アセットの検索順序

ガイドアセットのフォールバック順は、ページより少し豊富です。

1. `_<current language>/<path>`
2. 現在の言語がガイドの既定言語でない場合は `_<default language>/<path>`
3. `<path>`

これにより、必要に応じて画像やテクスチャー風アセットもローカライズできます。

## 検索と言語

検索ドキュメントには、Minecraft の生の言語コードと Lucene で使う解析言語の両方が保存されます。現在の Minecraft 言語が既知のアナライザーに割り当てられていない場合、検索は英語のトークン化にフォールバックします。

## 翻訳無視設定

GuideNH は全体に適用される「翻訳を無視する」スイッチを提供していません。ガイドを英語以外の言語へフォールバックさせたい場合は、そのガイドの `defaultLanguage` をコードで明示的に設定してください。

## 例

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

リポジトリのサンプルパックでの対応例:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## 関連ページ

- [ガイドページ形式](Guide-Page-Format)
- [画像とアセット](Images-And-Assets)
