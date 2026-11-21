# Syntaxisaanvulling

De gidseditor in het spel stelt tags, attributen en waarden voor terwijl u een pagina schrijft.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Schrijfreferentie

De lamp opent de schrijfreferentie. Zoek links naar onderwerpen, attributen, YAML-velden of voorbeeldcode; Ctrl+F activeert zoeken en rechtsklikken wist het veld. Een onderwerp springt naar de kop in het volledige document. Beide panelen scrollen vloeiend.

Terug en Vooruit herstellen onderwerpen en scrollposities. Houd de muis boven de markeringen rechts om koppen te lezen en klik om erheen te springen. Kopieer voorbeelden in het codeblok of het onderwerp als Markdown via de werkbalk. De lamp blijft blauw zolang de referentie open is; klik opnieuw om naar de gids terug te keren met behoud van concept, selectie en ongedaanmakingsgeschiedenis. Sluiten of Esc sluit het gidsvenster. Ontbrekende vertalingen gebruiken Engels.

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

### NEI-actielinks

Het invoegmenu en aanvulling bij `&[` bieden sjablonen voor handlers, recepten en toepassingen, plus `<NeiRecipeLink>`. De speleditor en IDE-plug-ins vullen `id`, `handlerId`, `mode` en NEI-URI-parameters aan. Handler-ID’s komen uit de geregistreerde handlers van het draaiende spel; verbind de runtime bridge van de IDE om ze op te halen. URI-aanvulling codeert waarden, vervangt de volledige huidige waarde en behoudt andere parameters. De handler-modus biedt alleen `handlerId`; bestaande parameters worden niet opnieuw aangeboden. Gewone links en codeblokken activeren geen NEI-aanvulling.

Zie [Recepten](Recipes.md#nei-receptlinks-in-lopende-tekst) voor syntaxis en voorbeelden.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
