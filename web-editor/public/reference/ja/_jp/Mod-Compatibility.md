# Mod 互換性

GuideNH には、選択した Mod 用の条件付き連携があります。対象 Mod が読み込まれている場合だけ連携が有効になり、対象がない場合は関連タグ、インデックス、キー割り当ては何もせず、残りのガイドは通常どおり動作します。

## StructureLib

StructureLib が読み込まれている場合、`<ImportStructureLib>` でマルチブロックのプレビューを `<GameScene>` に取り込めます。クライアントコマンド `/exportStructure structureLib` では、プレビューを PNG のドキュメント画像として書き出せます。詳しい構文とオプションは [構造のエクスポート](Structure-Export) を参照してください。

## BetterQuesting

[BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) が読み込まれると、次の機能が有効になります。

1. ページ frontmatter の `quest_ids` で BetterQuesting のクエスト ID をページに登録します。
2. `<QuestLink>`（インライン）と `<QuestCard>`（ブロック）が使えるようになります。
3. BetterQuesting GUI でクエストにカーソルを合わせ、標準の「ガイドを開く」キーを押すと対応ページへ移動できます。

### クエスト ID でページをインデックスする

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

値には正規 UUID または BetterQuesting の短縮 Base64 ID を指定できます。壊れた値や空の値はログに警告を出して無視されます。短縮 ID を先にデコードし、失敗した場合は正規 UUID として解析します。同じクエストを両方の形式で登録しないでください。内部 UUID が同じになり重複扱いになります。

ページにクエスト ID を登録すると、`<QuestLink>` と `<QuestCard>` はそのページをクリック先にします。

### BetterQuesting の説明から GuideNH へリンクする

クエスト説明では `[guide]` タグを使えます。このタグは BetterQuesting が存在する場合だけ解析されます。

```text
[guide]guidenh:navigation-guide[/guide]
```

ページが存在すれば表示文字列はページタイトルになります。`.md` は省略できます。

```text
[guide page=guidenh:navigation-guide]ナビゲーションガイドを開く[/guide]
```

```text
[guide page=guidenh:navigation-guide#navigation-fields]ナビゲーション項目[/guide]
```

リンクは通常のハイパーリンク色と下線で表示され、ホバー時に GuideNH のツールチップを表示し、クリックで対象ページを開きます。

### `<QuestLink>` と `<QuestCard>`

両タグは `id` 属性でクエスト ID を受け取り、コンパイル時のプレイヤーの進行状況に応じて描画します。

| 状態 | 条件 | 描画 |
| --- | --- | --- |
| 表示 | 解放済みで未完了 | クリック可能な通常リンク |
| 完了 | `quest.isComplete(player)` が true | 緑色リンクと末尾の `✓` |
| ロック | 未解放だが 非表示/SECRET ではない | 斜体の灰色プレースホルダー |
| 非表示 | ロック中で 非表示 または SECRET | 詳細を漏らさない濃い灰色プレースホルダー |
| 欠落 | ID がデータベースにない | 斜体の赤色プレースホルダー |

非表示でないロック済みクエストはクリックできます。登録ページがなければ BetterQuesting のクエスト画面を開きます。

### 非表示クエスト

ロック中の 非表示/SECRET クエストのタイトルや説明は表示しません。プレースホルダーは次の翻訳キーから取得します。

| 翻訳キー | 既定値 (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

ロック済みで公開されているクエストは、`<QuestLink>` または `<QuestCard>` 内のタイトルに説明ツールチップを表示できます。`show_tooltip="false"` または `showTooltip={false}` で非表示にできます。

> [!注記]
> クエスト状態はページのコンパイル時に現在のプレイヤーの進行状況から解決されます。ページはガイドごとにキャッシュされるため、解放後はガイドを開き直すと再評価されます。

### ガイドを開くキー

BetterQuesting が読み込まれていると、既定のキー `G`（`key.guidenh.open_guide` で変更可能）でクエストから対応ページを開けます。

1. BetterQuesting のクエスト画面でクエストボタンにカーソルを合わせます。
2. ガイドを開くキーを押し続けます。

対応ページがインデックスされていなければ何も行いません。インベントリのアイテムツールチップ経路は独立して動作します。

### BetterQuesting がない場合

- `<QuestLink>` と `<QuestCard>` は登録されず、通常の 不明-タグ エラーになります。
- `quest_ids` は `additionalProperties` に保存されますが、読み取られません。
- クエストホバー用のキー処理は何もしません。
- `[guide]...[/guide]` の説明リンクは解析されません。

## タグをサイトへエクスポートする

エクスポートサイトは GuideNH 標準タグを描画します。独自 `TagCompiler` のタグは `GuideSiteTagRenderer` を登録すると描画できます。

```java
public class MyModSiteTagRenderer implements GuideSiteTagRenderer {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public String render(GuideSiteTagRenderContext context, MdxJsxElementFields element) {
        String id = element.getAttributeString("id", "");
        return "<div class=\"mymod-machine\">" + GuideSiteGraphRenderer.esc(id) + "</div>";
    }
}
```

```java
GuideNhIntegrationRegistry.global().registerSiteTagRenderer(new MyModSiteTagRenderer());
Guide.builder(id).extension(GuideSiteTagRenderer.EXTENSION_POINT, new MyModSiteTagRenderer()).build();
```

登録済みレンダラーは組み込みレンダラーより先に呼ばれ、最初にマークアップを返したものが採用されます。担当外では `null` を返してください。例外はログに記録され、そのレンダラーだけがスキップされます。

コンテキストには `defaultNamespace`、`currentPageId`、`templates`、`sceneResolver`、`compiler` とエクスポート対象ページが含まれます。出力するテキストは `GuideSiteGraphRenderer.esc(...)` でエスケープしてください。
