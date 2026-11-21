# Complétion de syntaxe

L’éditeur de guides du jeu propose des balises, des attributs et des valeurs pendant la rédaction d’une page.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Référence de rédaction

L’ampoule ouvre la référence de rédaction. Recherchez sujets, attributs, champs YAML ou exemples à gauche ; Ctrl+F active la recherche, le clic droit la vide. Un sujet mène à son titre dans le document complet. Les deux panneaux défilent progressivement.

Précédent et Suivant restaurent les sujets et leur position. Survolez les repères de la barre droite pour lire les titres, puis cliquez pour y accéder. Copiez un exemple depuis son bloc ou le sujet en Markdown depuis la barre. L’ampoule reste bleue lorsque la référence est ouverte ; cliquez à nouveau dessus pour revenir au guide en conservant brouillon, sélection et historique. Fermer ou Échap ferme la fenêtre du guide. Les traductions manquantes utilisent l’anglais.

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

### Liens d’action NEI

Le menu d’insertion et la complétion de `&[` proposent des modèles de gestionnaires, de recettes et d’utilisations, ainsi que `<NeiRecipeLink>`. L’éditeur du jeu et les extensions IDE complètent `id`, `handlerId`, `mode` et les paramètres URI de NEI. Les identifiants proviennent des gestionnaires enregistrés dans le jeu en cours ; connectez le runtime bridge de l’IDE pour les obtenir. La complétion URI échappe les valeurs, remplace toute la valeur courante et conserve les autres paramètres. Le mode handler propose uniquement `handlerId` ; les paramètres présents ne sont pas proposés à nouveau. Les liens ordinaires et les blocs de code ne déclenchent pas la complétion NEI.

Consultez [Recettes](Recipes.md#liens-de-recettes-nei-dans-le-texte) pour la syntaxe et les exemples.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
