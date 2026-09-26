# Modcompatibiliteit

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH ships met conditional integrations voor geselecteerd mods. elke integration is alleen activated wanneer its target mod is loaded; wanneer De target is absent, De related tags, indices en key bindings stay inert en De rest of De gids keeps working.

## StructureLib

wanneer StructureLib is loaded, GuideNH kan import multiblock previews into `<GameScene>` met `<ImportStructureLib>`. De client command `/exportStructure structureLib` kan ook export those previews as PNG documentation screenshots. See [Structure Export](Structure-Export) voor De full command reference, StructureLib-specific opties, en De related `gameScene` export modus.

## BetterQuesting

wanneer [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) is loaded, GuideNH unlocks three features:

1. De `quest_ids` pagina-frontmatter key starts indexing pages by BetterQuesting quest id.
2. Two new tags become available: `<QuestLink>` (inline) en `<QuestCard>` (blok).
3. De standard "open gids" hotkey works terwijl hovering Een quest in De BetterQuesting GUI: holding De key looks up De hovered quest id en navigates naar De matching gids­pagina.

### Indexing pages by quest id

Voeg toe a `quest_ids` lijst naar De frontmatter of any gids­pagina you want associated met one of more quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

De waarden kan be canonical UUID strings of BetterQuesting compact Base64 quest ids. Malformed of empty entries are skipped met Een warning in De log.

Compact ids are decoded first, then GuideNH falls back naar canonical UUID parsing. Deze matches BetterQuesting's compact quest-id format such as `AAAAAAAAAAAAAAAAAAAMug==`.

Do niet lijst both encodings voor De zelfde quest in one pagina's `quest_ids`; they normalize naar De zelfde internal UUID en would be treated as duplicates.

wanneer Een quest id is indexed by Een pagina, both `<QuestLink>` en `<QuestCard>` zal route De klikken naar that gids­pagina instead of opening De BetterQuesting quest GUI directly.

### Linking van BetterQuesting descriptions naar GuideNH pages

BetterQuesting quest descriptions kan link back into GuideNH met a `[guide]` tag. De tag is parsed by GuideNH alleen wanneer BetterQuesting is loaded, en then gerenderd through BetterQuesting's native hyperlink-aware tekst box, so BQ word wrapping, scrolling en klikken hit boxes stay compatible.

gebruiken De pagina-ID as De target:

```text
[guide]guidenh:navigation-guide[/guide]
```

Als De target pagina exists, De zichtbaar link tekst is vervangen met that gids­pagina's title. De `.md` suffix is optioneel, so `guidenh:navigation-guide` en `guidenh:navigation-guide.md` punt naar De zelfde pagina wanneer De Markdown pagina exists.

gebruiken `page=` wanneer you want aangepaste zichtbaar tekst:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

De link is getoond in BetterQuesting's normal hyperlink kleur met underline styling, shows Een GuideNH tooltip on zweven, en opens De target GuideNH pagina wanneer clicked. pagina anchors are ook supported:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` en `<QuestCard>`

Both tags accept Een BetterQuesting quest id via `id` en decide their appearance van De player's progress at compile time:

| status | Source | Rendering |
| --- | --- | --- |
| zichtbaar | quest is unlocked but niet completed | clickable link, Standaard style |
| Completed | `quest.isComplete(player)` geeft terug true | clickable link, green kleur, trailing `✓` |
| Locked | quest exists but is niet unlocked, zichtbaarheid ≠ verborgen/SECRET | italic gray placeholder, niet clickable |
| verborgen | locked plus zichtbaarheid is verborgen of SECRET | italic dark-gray placeholder, no quest details leaked |
| Missing | quest id doet niet oplossen naar any quest in De database | italic red placeholder |

Locked but non-verborgen quests are ook clickable. They gebruiken De zelfde Navigatie target Regels as zichtbaar en completed quests.

voor zichtbaar / completed / locked quests, De klikken target is:

- De indexed gids­pagina Als De quest id is present in some pagina's `quest_ids`
- otherwise BetterQuesting's quest-book quest scherm, using BetterQuesting's native bovenliggend-scherm flow

See [Tags Reference](Tags-Reference#questlink) voor Attribuut tabellen en inline Voorbeelden.

### verborgen-quest handling

GuideNH never renders De title of description of Een quest whose zichtbaarheid is `HIDDEN` of `SECRET` terwijl De quest is still locked voor De player. De placeholder tekst is taken van Een vertaalsleutel so packs kan localize De wording:

| vertaalsleutel | Standaard (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests kan still tonen their description as Een zweven tooltip on `<QuestLink>` en on De clickable title binnen `<QuestCard>`, because BetterQuesting itself reveals De description on locked quest tooltips. Stel in `show_tooltip="false"` (of `showTooltip={false}`) naar suppress that tooltip. verborgen quests do niet expose any tooltip.

> [!Opmerking]
> Quest status is opgelost at pagina-compile time using De lokale player's progress. De compiled pagina is cached per gids; reopening De gids na completing of unlocking Een quest re-evaluates De status.

### Open-gids hotkey integration

De Standaard open-gids hotkey (`G`, configurable under `key.guidenh.open_guide`) gains Een second activation pad wanneer BetterQuesting is loaded. terwijl De BetterQuesting quest-regel GUI is open:

1. zweven over any quest button in De BQ panel
2. Hold De open-gids hotkey

Als any registered gids indexes that quest id through `quest_ids`, GuideNH zal navigate naar De matching pagina (of open De gids Als it's niet al open). Als no pagina indexes De hovered id, De hotkey does nothing — it doet niet fall back naar opening De BetterQuesting quest GUI, since BQ al shows that information.

Deze pad is independent of De inventory item-tooltip pad, so hovering items in your inventory still routes through De existing item / ore index lookups.

### Behavior wanneer BetterQuesting is absent

- `<QuestLink>` en `<QuestCard>` are niet registered, so pages that gebruiken them fall back naar De standard "onbekend tag" error rendering until you verwijderen De tag.
- `quest_ids` frontmatter entries are still parsed en stored under `additionalProperties`, but nothing reads them.
- De hotkey's quest-zweven branch becomes Een no-op.
- BetterQuesting `[guide]...[/guide]` description links are never parsed because De BetterQuesting tekst box en related mixins are niet loaded.

## Exporting your tags naar De site

De exported site renders De tags that ship met GuideNH. Een mod whose tags are compiled by its own
`TagCompiler` registers Een renderer so its tags are exported as well, instead of showing up in De book en
disappearing van De site:

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

Registered renderers are asked voor De built-in ones, en De first one that geeft terug markup wins, so a
renderer alleen has naar handle De tags it declares. Returning `null` leaves De element naar De next renderer
of naar De built-in export. Een renderer that throws is reported in De log en skipped, so one broken plugin
kan niet fail De export of Een pagina it doet niet own.

De context carries De pagina being exported en De shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` en `compiler`, De zelfde waarden De built-in renderers work
met. Markup you write is inserted into De pagina as it is, so escape tekst met
`GuideSiteGraphRenderer.esc(...)`.

Deze betekent Een gids that targets BetterQuesting kan be authored once en silently degrade in environments where BetterQuesting is niet installed.
