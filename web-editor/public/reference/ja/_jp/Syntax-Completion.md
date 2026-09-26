# 構文補完

GuideNH のページエディターは、MDX タグ、属性、属性値、Markdown 構文、コードフェンス名、frontmatter キーを補完します。候補はハードコードされておらず、Mod が登録できるレジストリから取得されます。通常はタグコンパイラーと構文コントリビューターの両方を登録します。

## 1. タグ名はタグコンパイラーから提供される

エディターには、登録済みのすべての `TagCompiler` と `SceneElementTagCompiler` のタグ名が表示されます。Mod がすでにタグコンパイラーを登録している場合、追加作業なしで補完候補になります。

すべてのガイドには `GuideNhIntegrationRegistry.registerTagCompilerProvider(...)` で `TagCompiler` を、`registerSceneElementTagCompilerProvider(...)` で `SceneElementTagCompiler` を登録できます。`GuideBuilder.extension(...)` を使えば 1 つのガイドだけに追加できます。プロバイダーはガイド構築時に読み込まれるため、閲覧中ではなく Mod のロード中に登録してください。

```java
public class MyModMachineTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // ...
    }
}
```


グローバル登録:

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```


単一ガイドへの登録:

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```


コンパイラーが受け付けてもページ作者が記述しない parser 出力（`p`、`h1`、`em` など）は候補になりません。ライブラリ自身のタグは `SyntaxSink.hiddenTags(...)` で隠されています。

## 2. 構文コントリビューターが残りを定義する

タグコンパイラーだけでは、読む属性や属性値の種類は分かりません。`SyntaxContributor` が属性、Markdown スニペット、コードフェンス名、frontmatter キーを宣言します。

```java
public class MyModSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public void contribute(SyntaxSink sink) {
        // Attributes. The value kind decides which value source answers completion.
        sink.attributes(
            "MyMachine",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("machine", SyntaxValueKind.of("MYMOD_MACHINE")),  // your own kind
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "input", "output", "both"));

        // Tags that wrap content are completed as <Name></Name> with the caret inside.
        sink.containerTags("MyPanel");
        sink.children("MyPanel", "MyMachine", "Tooltip");

        // Values resolved from your own registry at completion time.
        sink.valueSource(new MachineIdValueSource());

        // Extra markdown and fence names.
        sink.markdown(MarkdownSnippet.block("::", "Mod note", ":: note\n", 3));
        sink.fenceLanguages("mymod-diagram");

        // Frontmatter keys, with fixed values or a value kind.
        sink.frontmatterKeys("mymod_machine");
        sink.frontmatterKind("mymod_machine", SyntaxValueKind.of("MYMOD_MACHINE"));
    }
}
```


グローバルまたはガイド単位で登録します。

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```


グローバル登録されたコントリビューターが先に適用されるため、ガイド固有のコントリビューターで上書きできます。Mod 内蔵の構文も通常の `BuiltinSyntaxContributor` として既定拡張に登録されています。`GuideBuilder.disableDefaultExtensions()` を呼ぶと、既定のタグコンパイラーとともに内蔵構文も無効になります。

## 3. 値の種類と値ソース

属性の `SyntaxValueKind` が補完方法と引用方法を決めます。

| 種類 | 補完内容 | 記述形式 |
| --- | --- | --- |
| `STRING` | 自由なテキスト、または属性の隣に宣言した値 | `"..."` |
| `INT`、`FLOAT` | 既知の属性名に対応する数値プリセット | そのまま |
| `BOOLEAN` | `true`、`false` | そのまま |
| `ENUM` | `AttributeSyntax.of` に渡した列挙値 | `"..."` |
| `COLOR` | `#rrggbb` とシンボル色名 | `"..."` |
| `ITEM_ID`、`BLOCK_ID` | アイテム/ブロックのレジストリ名とアイコン | `"..."` |
| `ORE_DICT`、`ENTITY_ID`、`KEY_BIND`、`COMMAND` | 対応するゲームレジストリ | `"..."` |
| `PAGE_PATH`、`FILE_PATH` | ガイドページ ID とガイドアセット内のファイル | `"..."` |
| `MOD_ID` | アイテムレジストリの namespace から得た Mod ID | `"..."` |
| `EXPRESSION`、`DOMAIN`、`FORMAT_PATTERN` | 式、区間、パターン | `"..."` |
| `VECTOR3` | 自由なテキスト | そのまま |
| `SNBT` | 自由なテキスト | `{...}` |
| `QUEST_UUID` | 自由なテキスト。現在はクエスト ID を自動検出しない | `"..."` |

組み込みの種類を再利用する場合、追加登録は不要です。属性を `ITEM_ID` と指定するだけでアイテム補完が有効になります。

独自レジストリを補完するには、独自の種類とソースを宣言します。

```java
public final class MyModValueKinds {

    public static final SyntaxValueKind MACHINE = SyntaxValueKind.of("MYMOD_MACHINE");
}
```


```java
public class MachineIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(MyModValueKinds.MACHINE);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (MyMachine machine : MyMachineRegistry.all()) {
            if (results.size() >= limit) {
                break;
            }
            if (machine.id()
                .startsWith(request.partialText())) {
                results.add(SyntaxSuggestion.withValue(machine.id(), machine.id(), machine.displayName()));
            }
        }
        return results;
    }
}
```


`request` には入力中のテキスト、外側のタグと属性、frontmatter キーが含まれるため、1 つのソースを複数の属性で共有できます。

## 4. 実行中の環境データ

開いているガイドやドキュメントからデータを必要とするソースは `SyntaxEnvironmentAware` を実装します。`prepare` は各補完ティックでリクエスト処理前に 1 回呼ばれます。

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide(), environment.pagePaths(), environment.documentText()
    }
}
```


`prepare` は毎回実行されるため軽量に保ちます。入力が変化した場合だけ派生リストを再構築し、通常はキャッシュしてください。

## 5. 挿入テンプレート

タグ名を補完すると `<Name />`、またはカーソルを内部に置いた対になるタグが挿入されます。属性や本文が必要なタグは、便利な完全形をテンプレートとして宣言できます。

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```


`caretAfter` は指定マーカーの最初の出現直後にカーソルを置きます。`InsertTemplate.of(tag, text)` は末尾にカーソルを置きます。テンプレートは入力済みのタグ名を置き換え、作者はそのまま完全形の編集を続けられます。

## 6. 独自構文のスロット

ブロック内のディレクティブ、コードフェンス内の小言語、自分でコンパイルするフィールドなどは `SyntaxSlot` を登録します。

```java
public class MyModSlot implements SyntaxSlot {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model) {
        // Own the value of one attribute of your own tag, while its quote is still open.
        String attribute = "ports=\"";
        int attributeStart = text.lastIndexOf(attribute, cursorIndex);
        if (attributeStart < 0) {
            return null;
        }
        int from = attributeStart + attribute.length();
        int closingQuote = text.indexOf('"', from);
        if (closingQuote >= 0 && cursorIndex > closingQuote) {
            return null;
        }
        return new SyntaxSlotMatch(
            from,
            cursorIndex,
            text.substring(from, cursorIndex),
            MyPortRegistry.suggestions(),
            suggestion -> SyntaxReplacement.cursorAtEnd(suggestion.value()));
    }
}
```


```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```


スロットの `match` は置換範囲、入力済みテキスト、候補を決めます。カーソルが独自構文の外にある場合は `null` を返してください。ドキュメントに実際に存在するテキストを返す必要があり、正規化した文字列を返すと再検証でコミットが破棄されます。`SyntaxValueWriter` は置換後の文字列、カーソル位置、選択範囲を決めます。`selection` はダブルクリック時に選択する範囲を任意で指定します。

スロットはエディター自身のリゾルバーより先に確認され、候補が空でも最初に一致したスロットがカーソルを所有します。

## 7. エディターが標準で補完するもの

- 外側のコンテナに対応した MDX タグ。コンテナタグは `<Name></Name>` になります。
- 値の種類に応じた属性名（`name=""`、`name={}`、`name={true}`）。
- レシピ、アイテム/ブロック ID、Mod ID などの属性値。
- 見出し、リスト、タスクリスト、引用、アラート、表、コードブロック、区切り線、数式ブロック、強調、リンク、画像の Markdown 構文。
- コードブロック言語レジストリと GuideNH 固有フェンスから得たフェンス名。
- YAML frontmatter のキーと、値の種類を宣言したキーの候補値。
- タグを実用的な完全形で挿入するテンプレート。

候補は完全な前方一致、namespace 後の識別子一致、名前の部分一致の順で順位付けされます。

## 8. 拡張できる範囲

このページの機能はレジストリであり、登録済みプラグインはガイド構築またはページコンパイル時に読み込まれます。Mod のロード中に登録してください。

Mod 自身のテンプレート例には `<BlockStat item="" count="1" />`、`<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />`、最初の系列を含むチャートがあります。

| 目的 | 使用する API |
| --- | --- |
| タグ、シーン要素、属性、値の種類を追加 | `TagCompiler`、`SceneElementTagCompiler`、`SyntaxContributor`、`SyntaxValueSource` |
| 独自の補完スロットと writer を追加 | `SyntaxSlot` |
| タグを完全形で補完 | `sink.insertTemplates(...)` |
| Markdown スニペット、フェンス、frontmatter を追加 | `SyntaxContributor` |
| 独自フェンスの本文を解釈 | `CodeFenceRenderer` |
| シンボル色名を追加 | `SymbolicColorResolver` または `registerSymbolicColorResolver` |
| ページインデックスを追加 | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| タグをサイトへ出力 | `GuideSiteTagRenderer` |
| シーンエディターのボタンを追加 | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| サイトへリソースを追加 | `ExportableResourceProvider` |
| 独自構文のエラーを読者へ報告 | `LytErrorSink.appendError(...)` |
| ガイドエディターへボタン/メニューを追加 | `GuideNhIntegrationRegistry.registerEditorAction(...)` |

組み込みツールバー自体は固定です。新しい action は追加できますが、既存 action の動作変更はできません。診断はコンパイル時に `LytErrorSink` へ報告され、入力中専用の警告フックはありません。サイト出力は登録済みレンダラーを先に呼び出します。独自のシーン注釈は本内で描画できますが、サイトビューアーへ自動シリアライズされるのは `SceneAnnotation` 形状だけです。

プラグインの規則:

- `SyntaxValueKind` には `"MYMOD_MACHINE"` のような Mod namespace 付き ID を使います。
- `SyntaxSlot.match` からドキュメントが実際に保持する文字列を返します。
- 例外を投げたプラグインは namespace とともにログへ記録され、そのプラグインだけがスキップされます。
