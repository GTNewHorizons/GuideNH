# Совместимость модов

GuideNH поставляется с условной интеграцией для выбранных модов. Каждая интеграция активируется только тогда, когда загружен целевой мод; когда цель отсутствует, связанные теги, индексы и привязки клавиш остаются инертными, а остальная часть руководства продолжает работать.

GuideNH ships with conditional integrations for selected mods. Each integration is only activated when its target mod is loaded; runtime providers and native hover integration activate only when the target mod is present. Pure content-binding identities and page indices remain available without the target mod.

## StructureLib

When StructureLib is loaded, GuideNH can import multiblock previews into `<GameScene>` with `<ImportStructureLib>`. The client command `/exportStructure structureLib` can also export those previews as PNG documentation screenshots. See [Structure Export](Structure-Export) for the full command reference, StructureLib-specific options, and the related `gameScene` export mode.

## BetterQuesting

When [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) is loaded, GuideNH unlocks three features:

1. Page bindings with `type: "betterquesting"` associate a guide with a quest. Identity indexing does not require BetterQuesting; native quest navigation does.
2. Two new tags become available: `<QuestLink>` (inline) and `<QuestCard>` (block).
3. The standard "open guide" hotkey works while hovering a quest in the BetterQuesting GUI: holding the key looks up the hovered quest id and navigates to the matching guide page.

### Indexing pages by quest id

Add a `bindings` list to the frontmatter of any guide page you want associated with one or more quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef"}
  - {type: "betterquesting", id: "AAAAAAAAAAAAAAAAAAAMug=="}
---
```

Each entry uses `type: "betterquesting"`; its `id` may be a canonical UUID or a compact BetterQuesting Base64 ID. Add `anchor` to target a section of the current guide page. Malformed or empty entries are skipped with a warning in the log.

Compact ids are decoded first, then GuideNH falls back to canonical UUID parsing. This matches BetterQuesting's compact quest-id format such as `AAAAAAAAAAAAAAAAAAAMug==`.

Do not list both encodings for the same quest in one page's `bindings`; they normalize to the same internal UUID and would be treated as duplicates.

### Linking from BetterQuesting descriptions to GuideNH pages

BetterQuesting quest descriptions can link back into GuideNH with a `[guide]` tag. The tag is parsed by GuideNH only when BetterQuesting is loaded, and then rendered through BetterQuesting's native hyperlink-aware text box, so BQ word wrapping, scrolling and click hit boxes stay compatible.

Use the page id as the target:

```text
[guide]guidenh:navigation-guide[/guide]
```

If the target page exists, the visible link text is replaced with that guide page's title. The `.md` suffix is optional, so `guidenh:navigation-guide` and `guidenh:navigation-guide.md` point to the same page when the markdown page exists.

Use `page=` when you want custom visible text:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

The link is shown in BetterQuesting's normal hyperlink color with underline styling, shows a GuideNH tooltip on hover, and opens the target GuideNH page when clicked. Page anchors are also supported:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` and `<QuestCard>`

Both tags accept a BetterQuesting quest id via `id` and decide their appearance from the player's progress when the page mounts:

| State | Source | Rendering |
| --- | --- | --- |
| Visible | quest is unlocked but not completed | clickable link, default style |
| Completed | quest is completed | clickable link, green color |
| Locked | quest exists but is not unlocked, visibility ≠ HIDDEN/SECRET | clickable quest link or card title |
| Hidden | locked plus visibility is HIDDEN or SECRET | italic dark-gray placeholder, no quest details leaked |
| Missing | quest id does not resolve to any quest in the database | italic red placeholder |

Locked but non-hidden quests are also clickable. They use the same navigation target rules as visible and completed quests.

Left click opens the quest in BetterQuesting. Right click opens its bound guide pages; when no guide is bound, right click also opens BetterQuesting. Multiple pages open the same destination selector used for items, and distinct page anchors remain separate destinations.

See [Tags Reference](Tags-Reference#questlink) for attribute tables and inline examples.

### Hidden-quest handling

GuideNH never renders the title or description of a quest whose visibility is `HIDDEN` or `SECRET` while the quest is still locked for the player. The placeholder text is taken from a translation key so packs can localize the wording:

| Translation key | Default (en_US) |
| --- | --- |
| `guidenh.binding.state.hidden` | `Hidden content` |
| `guidenh.binding.state.missing` | `Content not found` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests can still show their description as a hover tooltip on `<QuestLink>` and on the clickable title inside `<QuestCard>`, because BetterQuesting itself reveals the description on locked quest tooltips. Set `showTooltip={false}` to suppress that tooltip. Hidden quests do not expose any tooltip.

> [!NOTE]
> Reopen the guide after completing or unlocking a quest to refresh its displayed state.

### Open-guide hotkey integration

The default open-guide hotkey (`G`, configurable under `key.guidenh.open_guide`) gains a second activation path when BetterQuesting is loaded. While the BetterQuesting quest-line GUI is open:

1. Hover over any quest button in the BQ panel
2. Hold the open-guide hotkey

If any registered guide indexes that quest id through `bindings`, GuideNH will navigate to the matching page (or open the guide if it's not already open). If no page indexes the hovered id, the hotkey does nothing — it does not fall back to opening the BetterQuesting quest GUI, since BQ already shows that information.

This path is independent of the inventory item-tooltip path, so hovering items in your inventory still routes through the existing item / ore index lookups.

### Behavior when BetterQuesting is absent

- `<QuestLink>` and `<QuestCard>` are not registered, so pages that use them fall back to the standard "unknown tag" error rendering until you remove the tag.
- `bindings` remain indexed without BetterQuesting. Native quest navigation and BQ hover targets are unavailable.
- BetterQuesting description links require BetterQuesting.

For optional BetterQuesting pages, set `navigation.required_mod: betterquesting` to avoid unknown-tag errors when the mod is absent. Mod developers can find tag and site-export extensions in [Developer Integration](Developer-Integration).

## Thaumcraft and AspectRecipeIndex

### Binding research pages

Thaumcraft research binding is optional. GuideNH supplies research tags, completion-aware presentation and native navigation. Holding the open-guide key (`G` by default) on a research tree node, entry title, or ARI NEI research icon opens every matching guide destination. Without a binding, the key does nothing. Item slots retain their normal item actions.

### Finding and copying research IDs

Research ID display, discovery and copying belong to AspectRecipeIndex. `ARIConfig.showResearchId` defaults to `false` and controls only ID labels and shortcut hints. Copying works with this setting disabled. Hover a visible research tree entry, a research-page title, or an ARI NEI research widget and use NEI's configured **Copy identifier** shortcut (`copy.identifier`) with its control modifier. The clipboard receives the original ID. `showResearchKey` independently controls whether NEI shows its research widgets. Copying from a research page is limited to its title area, preserving item-copy actions in the body.

### Exporting research IDs

In NEI Options → Tools → Data Dumps, select **Thaumcraft research IDs**, choose CSV or JSON and press Dump. The complete registry is written to `dumps/thaumcraftresearch.csv` or `dumps/thaumcraftresearch.json` below the game directory. The UTF-8 catalogue includes ID, category ID/name, title, description, normal/hidden parents, siblings, flags and item/texture icons. It includes research with no NEI recipe and excludes player progress. Export remains available with `showResearchId` disabled.

### Progress and return navigation

Research binding IDs preserve case. Reopen a guide to refresh progress; resource/world changes invalidate the research catalog. Native research browsing never unlocks or purchases research. Return from the research entry to its tree, then use Escape to return to the retained guide.
