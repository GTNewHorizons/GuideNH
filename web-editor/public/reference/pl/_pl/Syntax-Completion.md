# Uzupełnianie składni

Edytor przewodnika w grze podpowiada tagi, atrybuty i wartości podczas pisania strony.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Dokumentacja tworzenia poradników

Kliknij żarówkę na pasku edytora w grze, aby otworzyć dokumentację wspólną z Web Editor. Wyszukiwanie obejmuje tematy, atrybuty tagów, pola YAML i przykłady kodu. Wybierz temat po lewej i czytaj opis po prawej; oba panele przewijają się niezależnie. ↑ / ↓ wybiera tematy, Ctrl+F aktywuje wyszukiwanie, a Page Up / Page Down przewija opis. Prawy przycisk myszy czyści wyszukiwanie.

Linki przechodzą do tematów i nagłówków; przycisk wstecz wraca do poprzedniego tematu. Kopiuj przykłady przyciskiem w bloku kodu lub cały temat jako Markdown przyciskiem u góry. Żarówka pozostaje niebieska, gdy dokumentacja jest otwarta; kliknij ponownie, aby wrócić do przewodnika, zachowując szkic, zaznaczenie i historię cofania. Zamknij lub Esc zamyka okno przewodnika. Dokumentacja używa języka gry, a przy braku tłumaczenia wyświetla angielski.

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

### Odnośniki akcji NEI

Menu wstawiania udostępnia fragmenty akcji dla wszystkich receptur modułu obsługi, receptur przedmiotów i zastosowań oraz `<NeiRecipeLink>`. Atrybut `id` podpowiada identyfikatory przedmiotów; `mode` oferuje `handler`, `recipes` i `usage`. W `handlerId` wpisz dokładny identyfikator NEI.

Zobacz [Receptury](Recipes.md#odnośniki-do-receptur-nei-w-tekście), aby poznać składnię i przykłady.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
