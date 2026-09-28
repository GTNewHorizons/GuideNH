# Mod Compatibility


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH ships avec conditional integrations pour sélectionné mods. chaque integration est seulement activated lorsque son cible mod est chargé; lorsque Le cible est absent, Le related balises, indices et clé bindings stay inert et Le rest of Le guide keeps working.

## StructureLib

lorsque StructureLib est chargé, GuideNH peut import multiblock previews dans `<GameScene>` avec `<ImportStructureLib>`. Le client command `/exportStructure structureLib` peut aussi export those previews as PNG documentation screenshots. Voir [Structure Export](Structure-Export) pour Le complet command reference, StructureLib-spécifique options, et Le related `gameScene` export mode.

## BetterQuesting

lorsque [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) est chargé, GuideNH unlocks trois features:

1. Le `quest_ids` page-frontmatter clé starts indexing pages by BetterQuesting quest id.
2. deux new balises become disponible: `<QuestLink>` (inline) et `<QuestCard>` (bloc).
3. Le standard "open guide" hotkey works pendant au survol Un quest dans Le BetterQuesting GUI: holding Le clé looks up Le hovered quest id et navigates vers Le correspondant guide page.

### Indexing pages by quest id

Ajoutez a `quest_ids` liste vers Le frontmatter of quelconque guide page you want associated avec un ou plus quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

Le valeurs peut être canonical UUID strings ou BetterQuesting compact Base64 quest ids. Malformed ou vide entrées sont skipped avec Un avertissement dans Le log.

Compact ids sont decoded premier, alors GuideNH falls back vers canonical UUID parsing. ceci correspond BetterQuesting's compact quest-id format comme `AAAAAAAAAAAAAAAAAAAMug==`.

Do ne liste les deux encodings pour Le même quest dans un page's `quest_ids`; Elles normalize vers Le même interne UUID et would être treated as duplicates.

lorsque Un quest id est indexed by Un page, les deux `<QuestLink>` et `<QuestCard>` sera route Le clic vers que guide page à la placer of opening Le BetterQuesting quest GUI directly.

### Linking depuis BetterQuesting descriptions vers GuideNH pages

BetterQuesting quest descriptions peut lien back dans GuideNH avec a `[guide]` balise. Le balise est analysé by GuideNH seulement lorsque BetterQuesting est chargé, et alors rendu à travers BetterQuesting's native hyperlink-aware texte box, so BQ word wrapping, défilement et clic hit boxes stay compatible.

utiliser Le page id as Le cible:

```text
[guide]guidenh:navigation-guide[/guide]
```

Si Le cible page exists, Le visible lien texte est remplacé avec que guide page's titre. Le `.md` suffix est facultatif, so `guidenh:navigation-guide` et `guidenh:navigation-guide.md` point vers Le même page lorsque Le markdown page exists.

utiliser `page=` lorsque you want personnalisé visible texte:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

Le lien est affiché dans BetterQuesting's normal hyperlink couleur avec underline styling, affiche Un GuideNH tooltip on survol, et opens Le cible GuideNH page lorsque clicked. page anchors sont aussi Pris en charge:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` et `<QuestCard>`

les deux balises acceptent Un BetterQuesting quest id via `id` et decide leurs appearance depuis Le player's progress at compilateur time:

| état | source | rendu |
| --- | --- | --- |
| visible | quest est unlocked but ne completed | clickable lien, par défaut style |
| Completed | `quest.isComplete(player)` renvoie true | clickable lien, green couleur, trailing `✓` |
| Locked | quest exists but est ne unlocked, visibilité ≠ masquées/SECRET | italic gray placeholder, ne clickable |
| masquées | locked plus visibilité est masquées ou SECRET | italic dark-gray placeholder, non quest details leaked |
| manquant | quest id fait ne résoudre vers quelconque quest dans Le database | italic red placeholder |

Locked but non-masquées quests sont aussi clickable. Elles utiliser Le même navigation cible Règles as visible et completed quests.

pour visible / completed / locked quests, Le clic cible est:

- Le indexed guide page Si Le quest id est present dans some page's `quest_ids`
- otherwise BetterQuesting's quest-book quest écran, using BetterQuesting's native parent-écran flow

Voir [Tags Reference](Tags-Reference#questlink) pour attribut tableaux et inline Exemples.

### masquées-quest handling

GuideNH jamais renders Le titre ou description of Un quest whose visibilité est `HIDDEN` ou `SECRET` pendant Le quest est encore locked pour Le player. Le placeholder texte est taken depuis Un traduction clé so packs peut localize Le wording:

| traduction clé | par défaut (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests peut encore afficher leurs description as Un survol tooltip on `<QuestLink>` et on Le clickable titre dans `<QuestCard>`, because BetterQuesting itself reveals Le description on locked quest tooltips. Définissez `show_tooltip="false"` (ou `showTooltip={false}`) vers suppress que tooltip. masquées quests do ne expose quelconque tooltip.

> [!Remarque]
> Quest état est résolu at page-compilateur time using Le local player's progress. Le compilé page est cached per guide; reopening Le guide après completing ou unlocking Un quest re-evaluates Le état.

### Open-guide hotkey integration

Le par défaut open-guide hotkey (`G`, configurable under `key.guidenh.open_guide`) gains Un second activation chemin lorsque BetterQuesting est chargé. pendant Le BetterQuesting quest-ligne GUI est open:

1. survol sur quelconque quest button dans Le BQ panel
2. Hold Le open-guide hotkey

Si quelconque enregistré guide indexes que quest id à travers `quest_ids`, GuideNH sera navigate vers Le correspondant page (ou open Le guide Si it's ne déjà open). Si non page indexes Le hovered id, Le hotkey fait nothing — it fait ne fall back vers opening Le BetterQuesting quest GUI, since BQ déjà affiche que information.

ceci chemin est independent of Le inventory élément-tooltip chemin, so au survol éléments dans votre inventory encore routes à travers Le existing élément / ore index lookups.

### Behavior lorsque BetterQuesting est absent

- `<QuestLink>` et `<QuestCard>` sont ne enregistré, so pages que utiliser eux fall back vers Le standard "inconnu balise" erreur rendu until you supprimer Le balise.
- `quest_ids` frontmatter entrées sont encore analysé et stored under `additionalProperties`, but nothing reads eux.
- Le hotkey's quest-survol branch devient Un non-op.
- BetterQuesting `[guide]...[/guide]` description liens sont jamais analysé because Le BetterQuesting texte box et related mixins sont ne chargé.

## Exporting votre balises vers Le site

Le exported site renders Le balises que ship avec GuideNH. Un mod whose balises sont compilé by son propre
`TagCompiler` registers Un renderer so son balises sont exported as well, à la placer of showing up dans Le book et
disappearing depuis Le site:

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

enregistré renderers sont asked avant Le built-dans ones, et Le premier un que renvoie markup wins, so a
renderer seulement a vers handle Le balises it declares. Returning `null` leaves Le element vers Le suivant renderer
ou vers Le built-dans export. Un renderer que throws est reported dans Le log et skipped, so un broken plugin
ne peut pas fail Le export of Un page it fait ne propre.

Le context carries Le page being exported et Le shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` et `compiler`, Le même valeurs Le built-dans renderers fonctionnent
avec. Markup you écrire est inserted dans Le page as it est, so escape texte avec
`GuideSiteGraphRenderer.esc(...)`.

ceci signifie Un guide que targets BetterQuesting peut être authored once et silently degrade dans environments where BetterQuesting est ne installed.
