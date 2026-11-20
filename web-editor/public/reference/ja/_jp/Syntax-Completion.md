# 構文補完

ゲーム内のガイドエディターは、ページの編集中にタグ、属性、値の候補を表示します。

The in-game guide editor suggests tags, attributes and values while you write a page.

## 執筆リファレンス

電球ボタンで執筆リファレンスを開きます。左側でトピック、属性、YAML フィールド、コード例を検索できます。Ctrl+F で検索欄にフォーカスし、右クリックで消去します。トピックを選ぶと文書全体の該当見出しへ移動します。両側のスクロールは滑らかです。

戻る・進むボタンでトピックとスクロール位置を復元します。右のスクロールバーの見出し線にカーソルを置くとタイトルが表示され、クリックで移動できます。コード例は各ブロックから、トピックの Markdown はツールバーからコピーできます。リファレンスを開いている間、電球は青色になります。もう一度クリックすると、下書き、選択範囲、取り消し履歴を保持してガイドに戻ります。閉じるボタンまたは Esc でガイドのウィンドウを閉じます。未翻訳の文書は英語で表示します。

## Using the candidate list

| Action | Result |
| --- | --- |
| ↑ / ↓ | Select the previous or next candidate |
| Tab / Enter | Insert the selected candidate |
| Click | Insert the selected candidate |
| Esc | Close the list |
| Mouse wheel | Scroll through candidates |
| Scrollbar | Drag the scrollbar to scroll |

## Supported completion

| While editing | Suggestions |
| --- | --- |
| `<...` | Tag names valid in the enclosing container |
| `<Tag ...` | Attribute names and their value delimiters |
| `id="..."`, `fluid="..."` | Item and fluid registry names, recipes, pages, assets and other supported values |
| Markdown | Headings, lists, tables, alerts and other Markdown constructs |
| Code fence | Supported code languages and GuideNH fence names |
| Frontmatter | YAML keys and supported values |

Tag-name completion inserts an empty tag or a matching pair with the cursor inside. Add the attributes and content you need afterward. Item and fluid value candidates show icons; fluid values use registry names such as `water` or `steam`, rather than numeric IDs.

### Quest and research IDs

ID suggestions follow the content being edited:

| Editing | ID source |
| --- | --- |
| `ResearchLink`, `ResearchCard`, `ResearchIcon` | Thaumcraft research |
| `QuestLink`, `QuestCard` | BetterQuesting quests |
| `id` in a `bindings` entry | The system named by that entry's `type` |
| `BindingLink`, `BindingCard`, `BindingIcon` | The system named by the tag's `type` |

For a binding entry, fill in `type` before choosing an `id`:

```yaml
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "infusion"}
```

The game editor reads IDs from the loaded mod. Both IDE plugins also suggest IDs already used in workspace pages; connecting to the running game adds its registered IDs. Research IDs retain their original case. To find or copy an ID, see [Thaumcraft compatibility](Mod-Compatibility#finding-and-copying-research-ids).

### NEI アクションリンク

挿入メニューと `&[` の補完では、ハンドラー、レシピ、用途のアクションテンプレートと `<NeiRecipeLink>` を選べます。ゲーム内エディターと IDE プラグインは `id`、`handlerId`、`mode`、NEI URI パラメーターを補完します。ハンドラー ID は実行中のゲームに登録されたハンドラーから取得します。IDE では runtime bridge に接続してください。URI 補完は値をエスケープし、現在の値全体を置き換えて他のパラメーターを保持します。handler モードでは `handlerId` のみを提示し、既存のパラメーターは重複して提示しません。通常のリンクやコードブロックでは NEI 補完は動作しません。

アクション構文、アイテム ID、エスケープと例については [レシピ](Recipes.md#インライン-nei-レシピリンク)を参照してください。

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
