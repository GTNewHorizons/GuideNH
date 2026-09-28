# Mod-Kompatibilität


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH ships mit conditional integrations für ausgewählt mods. jede integration ist nur activated wenn seine Ziel mod ist geladen; wenn Die Ziel ist absent, Die Verwandt Tags, indices und Schlüssel bindings stay inert und Die rest von Die Leitfaden keeps working.

## StructureLib

wenn StructureLib ist geladen, GuideNH kann import multiblock previews in `<GameScene>` mit `<ImportStructureLib>`. Die client command `/exportStructure structureLib` kann auch export those previews als PNG documentation screenshots. Siehe [Structure Export](Structure-Export) für Die Vollständige command reference, StructureLib-bestimmten Optionen, und Die Verwandt `gameScene` export Modus.

## BetterQuesting

wenn [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) ist geladen, GuideNH unlocks drei features:

1. Die `quest_ids` Seite-frontmatter Schlüssel Starts indexing pages by BetterQuesting quest id.
2. zwei new Tags werden zu verfügbar: `<QuestLink>` (inline) und `<QuestCard>` (Block).
3. Die Standard "open Leitfaden" hotkey works während Beim Überfahren Eine quest in Die BetterQuesting GUI: holding Die Schlüssel looks up Die hovered quest id und navigates zu Die passend Leitfaden Seite.

### Indexing pages by quest id

Fügen Sie hinzu ein `quest_ids` Liste zu Die frontmatter von beliebig Leitfaden Seite you want associated mit eins oder mehr quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

Die Werte kann sein canonical UUID strings oder BetterQuesting compact Base64 quest ids. Malformed oder leer Einträge sind skipped mit Eine Warnung in Die log.

Compact ids sind decoded erste, dann GuideNH falls back zu canonical UUID parsing. dies stimmt überein BetterQuesting's compact quest-id format such als `AAAAAAAAAAAAAAAAAAAMug==`.

Do nicht Liste beide encodings für Die gleich quest in eins Seite's `quest_ids`; Sie normalize zu Die gleich intern UUID und would sein treated als duplicates.

wenn Eine quest id ist indexed by Eine Seite, beide `<QuestLink>` und `<QuestCard>` wird route Die Klick zu dass Leitfaden Seite stattdessen von opening Die BetterQuesting quest GUI directly.

### Linking von BetterQuesting Beschreibungs zu GuideNH pages

BetterQuesting quest Beschreibungs kann Link back in GuideNH mit ein `[guide]` Tag. Die Tag ist geparst by GuideNH nur wenn BetterQuesting ist geladen, und dann gerendert durch BetterQuesting's native hyperlink-aware Text box, so BQ word wrapping, Scrollen und Klick hit boxes stay compatible.

verwenden Die Seite id als Die Ziel:

```text
[guide]guidenh:navigation-guide[/guide]
```

Wenn Die Ziel Seite exists, Die sichtbar Link Text ist ersetzt mit dass Leitfaden Seite's Titel. Die `.md` suffix ist optional, so `guidenh:navigation-guide` und `guidenh:navigation-guide.md` Punkt zu Die gleich Seite wenn Die markdown Seite exists.

verwenden `page=` wenn you want benutzerdefiniert sichtbar Text:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

Die Link ist angezeigt in BetterQuesting's normal hyperlink Farbe mit underline styling, zeigt Eine GuideNH Tooltip auf Hover, und opens Die Ziel GuideNH Seite wenn clicked. Seite anchors sind auch unterstützt:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` und `<QuestCard>`

beide Tags accept Eine BetterQuesting quest id via `id` und decide ihre appearance von Die player's progress at kompilieren time:

| Zustand | Quelle | Darstellung |
| --- | --- | --- |
| sichtbar | quest ist unlocked but nicht completed | clickable Link, Standard style |
| Completed | `quest.isComplete(player)` gibt zurück true | clickable Link, green Farbe, trailing `✓` |
| Locked | quest exists but ist nicht unlocked, Sichtbarkeit ≠ ausgeblendet/SECRET | kursiv gray placeholder, nicht clickable |
| verborgen | locked plus Sichtbarkeit ist verborgen oder SECRET | kursiv dark-gray placeholder, nein quest details leaked |
| fehlend | quest id tut nicht auflösen zu beliebig quest in Die database | kursiv red placeholder |

Locked but non-ausgeblendet quests sind auch clickable. Sie verwenden Die gleich navigation Ziel Regeln als sichtbar und completed quests.

für sichtbar / completed / locked quests, Die Klick Ziel ist:

- Die indexed Leitfaden Seite Wenn Die quest id ist present in some Seite's `quest_ids`
- otherwise BetterQuesting's quest-book quest Bildschirm, using BetterQuesting's native übergeordnet-Bildschirm flow

Siehe [Tags Reference](Tags-Reference#questlink) für Attribut Tabellen und inline Beispiele.

### ausgeblendet-quest handling

GuideNH nie rendert Die Titel oder Beschreibung von Eine quest whose Sichtbarkeit ist `HIDDEN` oder `SECRET` während Die quest ist weiterhin locked für Die player. Die placeholder Text ist taken von ein Übersetzung Schlüssel so packs kann localize Die wording:

| Übersetzung Schlüssel | Standard (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests kann weiterhin anzeigen ihre Beschreibung als Eine Hover Tooltip auf `<QuestLink>` und auf Die clickable Titel innerhalb `<QuestCard>`, becaVerwenden Sie BetterQuesting itself reveals Die Beschreibung auf locked quest tooltips. Setzen Sie `show_tooltip="false"` (oder `showTooltip={false}`) zu suppress dass tooltip. verborgen quests do nicht expose beliebig tooltip.

> [!Hinweis]
> Quest Zustand ist aufgelöst at Seite-kompilieren time using Die lokal player's progress. Die kompiliert Seite ist zwischengespeichert per Leitfaden; reopening Die Leitfaden nach completing oder unlocking Eine quest re-evaluates Die Zustand.

### Open-Leitfaden hotkey integration

Die Standard open-Leitfaden hotkey (`G`, configurable under `key.guidenh.open_guide`) gains Eine second activation Pfad wenn BetterQuesting ist geladen. während Die BetterQuesting quest-Linie GUI ist open:

1. Hover über beliebig quest button in Die BQ panel
2. Hold Die open-Leitfaden hotkey

Wenn beliebig registriert Leitfaden indexes dass quest id durch `quest_ids`, GuideNH wird navigate zu Die passend Seite (oder open Die Leitfaden Wenn it's nicht bereits open). Wenn nein Seite indexes Die hovered id, Die hotkey tut nothing — it tut nicht fall back zu opening Die BetterQuesting quest GUI, since BQ bereits zeigt dass information.

dies Pfad ist independent von Die inventory Element-tooltip Pfad, so Beim Überfahren Elemente in Ihre inventory weiterhin routes durch Die existing Element / ore index lookups.

### Behavior wenn BetterQuesting ist absent

- `<QuestLink>` und `<QuestCard>` sind nicht registriert, so pages dass verwenden sie fall back zu Die Standard "unbekannt Tag" Fehler Darstellung until you entfernen Die Tag.
- `quest_ids` frontmatter Einträge sind weiterhin geparst und stored under `additionalProperties`, but nothing reads sie.
- Die hotkey's quest-Hover branch wird zu Eine nein-op.
- BetterQuesting `[guide]...[/guide]` Beschreibung Links sind nie geparst becaVerwenden Sie Die BetterQuesting Text box und Verwandt mixins sind nicht geladen.

## Exporting Ihre Tags zu Die site

Die exported site rendert Die Tags dass ship mit GuideNH. Eine mod whose Tags sind kompiliert by seine eigene
`TagCompiler` registers Eine renderer so seine Tags sind exported als well, stattdessen von showing up in Die book und
disappearing von Die site:

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

registriert renderers sind asked vor Die built-in ones, und Die erste eins dass gibt zurück markup wins, so ein
renderer nur hat zu handle Die Tags it declares. Returning `null` leaves Die element zu Die nächste renderer
oder zu Die built-in export. Eine renderer dass throws ist reported in Die log und skipped, so eins broken plugin
kann nicht fail Die export von Eine Seite it tut nicht eigene.

Die context carries Die Seite being exported und Die shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` und `compiler`, Die gleich Werte Die built-in renderers funktionieren
mit. Markup you schreiben ist inserted in Die Seite als it ist, so escape Text mit
`GuideSiteGraphRenderer.esc(...)`.

dies bedeutet Eine Leitfaden dass targets BetterQuesting kann sein authored once und silently degrade in environments where BetterQuesting ist nicht installed.
