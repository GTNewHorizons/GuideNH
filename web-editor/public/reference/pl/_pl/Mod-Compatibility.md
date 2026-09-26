# Zgodność z modami

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH ships z conditional integrations dla wybrany mods. każdy integration is tylko activated gdy jego cel mod is załadowany; gdy  cel is absent,  related tagi, indices i klucz bindings pozostają inert i  rest of  przewodnik zachowuje working.

## StructureLib

gdy StructureLib is załadowany, GuideNH może import multiblock previews do `<GameScene>` z `<ImportStructureLib>`.  client command `/exportStructure structureLib` może także export those previews as PNG documentation screenshots. Zobacz [Structure Export](Structure-Export) dla  full command reference, StructureLib-określony opcje, i  related `gameScene` export tryb.

## BetterQuesting

gdy [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) is załadowany, GuideNH unlocks three features:

1.  `quest_ids` strona-frontmatter klucz starts indexing strony by BetterQuesting quest id.
2. dwa nowy tagi become dostępny: `<QuestLink>` (inline) i `<QuestCard>` (blok).
3.  standardowy "open przewodnik" hotkey works podczas najechanie quest in  BetterQuesting GUI: holding  klucz looks up  hovered quest id i navigates do  pasujący strona przewodnika.

### Indexing strony by quest id

Dodaj a `quest_ids` lista do  frontmatter of any strona przewodnika you want associated z jeden lub more quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

 wartości może be canonical UUID strings lub BetterQuesting compact Base64 quest ids. Malformed lub empty wpisy są skipped z ostrzeżenie in  log.

Compact ids są decoded pierwszy, następnie GuideNH falls tył do canonical UUID parsing. Ten pasuje BetterQuesting's compact quest-id format takie jak `AAAAAAAAAAAAAAAAAAAMug==`.

Do nie lista both encodings dla  ten sam quest in jeden strona's `quest_ids`; jeden normalize do  ten sam wewnętrzny UUID i would be treated as duplicates.

gdy quest id is indexed by strona, both `<QuestLink>` i `<QuestCard>` będzie route  kliknięcie do który strona przewodnika zamiast tego of opening  BetterQuesting quest GUI directly.

### Linking z BetterQuesting descriptions do GuideNH strony

BetterQuesting quest descriptions może odnośnik tył do GuideNH z a `[guide]` tag.  tag is przeanalizowany by GuideNH tylko gdy BetterQuesting is załadowany, i następnie wyrenderowany through BetterQuesting's native hyperlink-aware tekst box, so BQ word wrapping, przewijanie i kliknięcie hit boxes pozostają compatible.

używać  ID strony as  cel:

```text
[guide]guidenh:navigation-guide[/guide]
```

Jeśli  cel strona exists,  widoczny odnośnik tekst is zastąpiony z który strona przewodnika's tytuł.  `.md` suffix is opcjonalny, so `guidenh:navigation-guide` i `guidenh:navigation-guide.md` punkt do  ten sam strona gdy  Markdown strona exists.

używać `page=` gdy you want niestandardowy widoczny tekst:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

 odnośnik is wyświetlany in BetterQuesting's normal hyperlink kolor z underline styling, shows GuideNH podpowiedź on najechanie, i opens  cel GuideNH strona gdy clicked. strona anchors są także Obsługiwane:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` i `<QuestCard>`

Both tagi akceptują BetterQuesting quest id via `id` i decide ich appearance z  gracz's progress at kompilować czas:

| stan | Source | renderowanie |
| --- | --- | --- |
| widoczny | quest is unlocked but nie completed | clickable odnośnik, Domyślne style |
| Completed | `quest.isComplete(player)` zwraca true | clickable odnośnik, green kolor, trailing `✓` |
| Locked | quest exists but is nie unlocked, widoczność ≠ ukryty/SECRET | italic gray placeholder, nie clickable |
| ukryty | locked plus widoczność is ukryty lub SECRET | italic dark-gray placeholder, no quest details leaked |
| Missing | quest id nie rozwiązać do any quest in  database | italic red placeholder |

Locked but non-ukryty quests są także clickable. jeden używać  ten sam Nawigacja cel Zasady as widoczny i completed quests.

dla widoczny / completed / locked quests,  kliknięcie cel is:

-  indexed strona przewodnika Jeśli  quest id is present in some strona's `quest_ids`
- otherwise BetterQuesting's quest-book quest ekran, używając BetterQuesting's native nadrzędny-ekran flow

Zobacz [Tags Reference](Tags-Reference#questlink) dla Atrybut tabele i inline Przykłady.

### ukryty-quest handling

GuideNH nigdy renders  tytuł lub opis of quest whose widoczność is `HIDDEN` lub `SECRET` podczas  quest is nadal locked dla  gracz.  placeholder tekst is taken z klucz tłumaczenia so packs może localize  wording:

| klucz tłumaczenia | Domyślne (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests może nadal pokazywać ich opis as najechanie podpowiedź on `<QuestLink>` i on  clickable tytuł wewnątrz `<QuestCard>`, because BetterQuesting itself reveals  opis on locked quest tooltips. Ustaw `show_tooltip="false"` (lub `showTooltip={false}`) do suppress który podpowiedź. ukryty quests do nie expose any podpowiedź.

> [!Uwaga]
> Quest stan is rozwiązany at strona-kompilować czas używając  lokalny gracz's progress.  skompilowany strona is cached per przewodnik; reopening  przewodnik po completing lub unlocking quest re-evaluates  stan.

### Open-przewodnik hotkey integration

 Domyślne open-przewodnik hotkey (`G`, configurable under `key.guidenh.open_guide`) gains second activation ścieżka gdy BetterQuesting is załadowany. podczas  BetterQuesting quest-wiersz GUI is open:

1. najechanie over any quest button in  BQ panel
2. Hold  open-przewodnik hotkey

Jeśli any zarejestrowany przewodnik indexes który quest id through `quest_ids`, GuideNH będzie navigate do  pasujący strona (lub open  przewodnik Jeśli it's nie już open). Jeśli no strona indexes  hovered id,  hotkey does nothing — it nie fall tył do opening  BetterQuesting quest GUI, since BQ już shows który information.

Ten ścieżka is independent of  inventory element-podpowiedź ścieżka, so najechanie items in your inventory nadal routes through  existing element / ore index lookups.

### zachowanie gdy BetterQuesting is absent

- `<QuestLink>` i `<QuestCard>` są nie zarejestrowany, so strony który używać ich fall tył do  standardowy "nieznany tag" błąd renderowanie until you usuwać  tag.
- `quest_ids` frontmatter wpisy są nadal przeanalizowany i stored under `additionalProperties`, but nothing reads ich.
-  hotkey's quest-najechanie branch staje się no-op.
- BetterQuesting `[guide]...[/guide]` opis links są nigdy przeanalizowany because  BetterQuesting tekst box i related mixins są nie załadowany.

## Exporting your tagi do  site

 exported site renders  tagi który ship z GuideNH. mod whose tagi są skompilowany by jego własny
`TagCompiler` registers renderer so jego tagi są exported as well, zamiast tego of showing up in  book i
disappearing z  site:

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

zarejestrowany renderers są asked przed  built-in ones, i  pierwszy jeden który zwraca markup wins, so a
renderer tylko ma do handle  tagi it declares. Returning `null` leaves  element do  następny renderer
lub do  built-in export. renderer który throws is reported in  log i skipped, so jeden broken plugin
nie może fail  export of strona it nie własny.

 context carries  strona being exported i  shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` i `compiler`,  ten sam wartości  built-in renderers działa
z. Markup you zapisywać is inserted do  strona as it is, so escape tekst z
`GuideSiteGraphRenderer.esc(...)`.

Ten oznacza przewodnik który targets BetterQuesting może be authored once i silently degrade in environments where BetterQuesting is nie installed.
