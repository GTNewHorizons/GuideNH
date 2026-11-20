# Syntax Completion

The in-game guide editor suggests tags, attributes and values while you write a page.

## Authoring reference

Click the lightbulb in the in-game editor toolbar to open the authoring reference. Search topic titles, attributes, YAML fields and example code in the left navigation panel; Ctrl+F focuses search and right-click clears it. Select a topic to jump to its heading in the complete document. Navigation and document scrollbars move smoothly.

Use Back and Forward to revisit topics and restore their scroll positions. Hover over the heading marks on the right scrollbar to see their titles, then click to jump. Copy an example with its code-block button, or copy the selected topic as Markdown with the toolbar copy button. The lightbulb stays blue while the reference is open; click it again to return to the guide with the draft, selection and undo history preserved. Close or Esc closes the guide window. Documents follow the game language, with English fallback.

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

### NEI action links

The insert menu provides handler, recipe, and usage action snippets, plus `<NeiRecipeLink>`. The `id` attribute offers item IDs; `mode` offers `handler`, `recipes`, and `usage`. Enter the exact NEI identifier for `handlerId`.

See [Recipes](Recipes.md#inline-nei-recipe-links) for syntax and examples.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
