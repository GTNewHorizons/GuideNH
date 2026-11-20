# Доповнення синтаксису

Редактор посібника у грі пропонує теги, атрибути та значення під час написання сторінки.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Довідник автора

Натисніть лампочку на панелі редактора у грі, щоб відкрити спільний довідник із Web Editor. Пошук охоплює теми, атрибути тегів, поля YAML і приклади коду. Виберіть тему ліворуч і читайте опис праворуч; панелі прокручуються незалежно. ↑ / ↓ вибирає теми, Ctrl+F активує пошук, Page Up / Page Down прокручує опис. Права кнопка миші очищає пошук.

Посилання переходять до тем і заголовків; кнопка назад повертає попередню тему. Копіюйте приклади кнопкою в блоці коду або всю тему в Markdown верхньою кнопкою. Лампочка залишається синьою, поки довідник відкритий; натисніть її ще раз, щоб повернутися до посібника, зберігши чернетку, виділення та історію скасування. Кнопка закриття або Esc закриває вікно посібника. Документація використовує мову гри; відсутні переклади замінюються англійською.

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

### Посилання дій NEI

Меню вставлення пропонує шаблони дій для всіх рецептів обробника, рецептів предмета й застосувань, а також `<NeiRecipeLink>`. Для атрибута `id` пропонуються ідентифікатори предметів, для `mode` — `handler`, `recipes` та `usage`. Введіть точний ідентифікатор NEI у `handlerId`.

Див. [Рецепти](Recipes.md#посилання-на-рецепти-nei-в-тексті): синтаксис і приклади.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
