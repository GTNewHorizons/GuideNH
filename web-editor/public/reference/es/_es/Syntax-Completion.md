# Compleción de sintaxis

El editor de guías del juego sugiere etiquetas, atributos y valores mientras escribe una página.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Referencia de escritura

La bombilla abre la referencia de escritura. Busca temas, atributos, campos YAML o ejemplos a la izquierda; Ctrl+F enfoca la búsqueda y el clic derecho la borra. Elegir un tema salta a su título en el documento completo. Ambos paneles se desplazan suavemente.

Atrás y Adelante restauran temas y posiciones. Pasa el ratón por las marcas de la barra derecha para ver los títulos y haz clic para saltar. Copia ejemplos desde el bloque de código o el tema como Markdown desde la barra. La bombilla permanece azul mientras la referencia está abierta; haz clic de nuevo para volver a la guía conservando borrador, selección e historial. Cerrar o Esc cierra la ventana de la guía. Las traducciones ausentes usan inglés.

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

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## NEI action links

The insert menu provides handler, recipe, and usage action snippets, plus `<NeiRecipeLink>`. The `id` attribute offers item IDs; `mode` offers `handler`, `recipes`, and `usage`. Enter the exact NEI identifier for `handlerId`.

See [Recipes](Recipes.md#inline-nei-recipe-links) for syntax and examples.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
