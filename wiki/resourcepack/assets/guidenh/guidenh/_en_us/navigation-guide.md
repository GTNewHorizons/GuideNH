---
navigation:
  title: Navigation & Index
  keyword: navigation
  keywords:
    - guide navigation
    - content index
  parent: index.md
  position: 195
  recommend: 5
categories:
  - markdown
---

# Navigation & Content Index

GuideNH uses YAML frontmatter to declare navigation structure — no `index.md` hardcoding, no manifest files. Every page controls where it appears in the sidebar through its own frontmatter.

When nested pages are expanded in the in-game sidebar, every expanded ancestor that still owns
visible descendants stays pinned at the top. Multiple ancestor levels can stack there at once,
making deep trees easier to track while scrolling.

## Quick Example

```yaml
---
navigation:
  title: Advanced IO Bus
  parent: aae_intro/aae_intro-index.md
  icon: advanced_ae:advanced_io_bus_part
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
---
```

## Navigation Fields

### `navigation.title`

Display title in the sidebar. Required for the page to appear in navigation.

### `navigation.parent`

Page ID of the parent node. A relative path (e.g., `getting-started.md`) resolves against the page's own namespace. Omit to make this page a top-level root node.

```yaml
# Root node (no parent)
navigation:
  title: Getting Started

# Child of another page
navigation:
  title: Advanced IO Bus
  parent: aae_intro/aae_intro-index.md

# Explicit namespace, rooted path
navigation:
  title: GregTech Integration
  parent: gregtech:/index.md
```

The same rules are used by Markdown page links. `guide.md`, `./guide.md`, and `/guide.md` stay inside the current
page namespace. `gregtech:guide.md` and `gregtech:/guide.md` explicitly open the `gregtech` namespace, which means
the page is loaded from the `gregtech:guidenh` data-driven guide when this guide folder is `guidenh`.

### `navigation.keyword` and `navigation.keywords`

Use `keyword` for one search alias or `keywords` for several aliases. They can be combined; duplicate
values are removed. Search uses the same language analyzer and prefix matching as other searchable fields.

```yaml
navigation:
  title: Advanced IO Bus
  keyword: IO bus
  keywords:
    - import export bus
    - item transfer
```

### `navigation.position`

Integer sort order within siblings. Larger numbers appear first. Ties are broken by `title` alphabetically.

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  position: 10
```

### `navigation.recommend`

Optional integer used by the home page Recommended panel. The page only appears there when this field is present.
`0` is valid. Larger values appear earlier. Equal values are sorted by title alphabetically.

```yaml
navigation:
  title: Markdown Basics
  parent: index.md
  recommend: 0
```

### `navigation.priority`

Load priority for same-path page overrides across mods and resource packs. Missing priority is `0`; higher values
win. If two candidates have the same priority, the later resource pack entry wins, matching normal Minecraft
resource-pack order.

```yaml
navigation:
  title: Pack Override Page
  parent: index.md
  priority: 100
```

`DefaultGuide` uses the same native namespace-root layout. Put GuideNH pages under
`config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md`, not under an extra `assets/` directory.
These pages still follow normal resource-pack precedence, so later higher-priority resource sources can override
same-path files.

### `navigation.icon`

An item ID shown next to the page title in the sidebar.

```yaml
navigation:
  title: Recipes
  icon: minecraft:crafting_table:0:{display:{Name:"Custom Crafting Table"}}
```

### `navigation.icon_texture`

A PNG texture path (relative to the guide assets folder) used as the navigation icon.

```yaml
navigation:
  title: Home
  icon_texture: my_icon.png
```

### `navigation.icons`

A list of item IDs that cycle in the sidebar. The icon changes periodically.

```yaml
navigation:
  title: Color Demo
  icons:
    - minecraft:wool:1
    - minecraft:wool:4:{display:{Name:"Custom Green Wool"}}
    - minecraft:wool:14
```

### `navigation.icon_textures`

A list of texture paths that cycle as the navigation icon.

```yaml
navigation:
  title: Slideshow
  icon_textures:
    - frame_1.png
    - frame_2.png
```

## Content Indexing Fields

### `categories`

A list of MediaWiki-style categories. Each entry can be either `category` or `category|sort key`.

```yaml
categories:
  - markdown
  - charts|Charts Overview
```

On the parent page, use `<Category name="markdown" rows="3" />` to auto-generate a list of all pages in that category.
Each category also gets an auto-generated hidden searchable page such as `Category:markdown`.
GuideNH also provides hidden special pages such as `Special:SpecialPages`, `Special:AllPages`, and `Special:Categories`.
Use `<Special name="SpecialPages" rows="3" />`, `<Special name="AllPages" rows="3" />`, or `<Special name="Categories" rows="3" />` to embed them directly.
All `Special:*` pages stay out of normal guide search, while `Category:*` pages remain searchable.

### `item_id` and `item_ids`

Links items to this page for G-key lookup and right-click navigation. Use `item_id` for one NEI-style
expression or `item_ids` for a list of expressions. Append `#anchor` to jump to a specific heading.

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```

`minecraft:lava` performs a partial registry-id match. `<minecraft:wool:14>` strictly matches an item
and metadata value. The compatible `ae2:white_paint_ball:*` form strictly matches every metadata value. Whitespace
combines conditions, `,` combines rules in one term, `!` excludes a match, and `|` separates alternatives.
`minecraft:potion 0-16,20-36,!28` combines two metadata ranges and excludes `28`. Lookups try exact and wildcard
mappings before evaluating expressions.

### External content bindings

`bindings` associates this guide page with content from another system. Write one mapping per line: `type` selects the system, `id` identifies its content, and optional `anchor` targets a section of **this guide page**. Omitting `anchor` opens the page top.

One content ID can refer to several pages or anchors. Navigation uses the same destination selector as item bindings. Duplicate page/anchor entries merge, while distinct anchors remain separate choices.

#### BetterQuesting quests

Use type `betterquesting` and a canonical UUID or compact Base64 quest ID. Replace these examples with actual IDs from your pack:

```yaml
bindings:
  - {type: "betterquesting", id: "01234567-89ab-cdef-0123-456789abcdef", anchor: "quest"}
  - {type: "betterquesting", id: "AAAAAAAAAAAAAAAAAAAMug=="}
```

```mdx
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage quest" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" title="Stage guide" />
```

Left click on a link or card title opens the BQ quest. Right click opens bound guides, falling back to BQ when none exist. Hold the open-guide key (`G` by default) over a BQ quest button to choose its guides; without a binding, the key does nothing. `QuestLink` and `QuestCard` require BetterQuesting.

#### Thaumcraft research

Use type `thaumcraft` and a registered ID such as `INFUSION` or `NITOR`, preserving case:

```yaml
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "infusion"}
  - {type: "thaumcraft", id: "INFUSION", anchor: "apparatus"}
  - {type: "thaumcraft", id: "NITOR"}
```

For page-top bindings, `research_id: "INFUSION"` binds one ID and `research_ids` accepts a string list. Use `bindings` for anchors.

```mdx
<ResearchLink id="INFUSION" text="Infusion guide" />
<ResearchLink id="INFUSION" text="Open the Thaumonomicon" target="native" />
<ResearchCard id="INFUSION" showParents={true} />
<ResearchIcon id="NITOR" />
```

Research links and card titles prefer bound guides by default. With `target="native"`, left click opens native research. Right click opens guides, falling back to the native screen when none exist. Icons supply tooltips and open-guide hotkey targets without click navigation. [Research & Bindings](research.md) demonstrates page-top, infusion and apparatus destinations.

ARI supplies research ID display and copying. Hover a research tree entry, research-page title or NEI research widget and use NEI's Copy identifier shortcut with its Ctrl modifier. `showResearchId` only controls labels; copying works while it is disabled. NEI Options → Tools → Data Dumps exports the complete research ID catalogue.

#### Other content types

Once an integration registers another type, use the same `bindings` syntax and select it with `type` on `BindingLink`, `BindingCard` or `BindingIcon`:

```mdx
<BindingLink type="your_provider" id="chapter:stage_2" />
```

All three fields follow YAML string rules. In double quotes, escape a double quote as `\"` and a backslash as `\\`; in single quotes, write `''` for one single quote. Quoted commas, colons and `#` remain part of the value. Put every content type bound by a page in the same `bindings` list.

### `navigation.required_mod`, `navigation.required_mods`, `navigation.excluded_mod`, and `navigation.excluded_mods`

Required fields keep a page visible only when their mod ids are loaded. Excluded fields hide a page when
the single excluded mod, or any mod in the excluded list, is loaded. All required mods must be loaded and
none of the excluded mods may be loaded.

```yaml
navigation:
  title: AE2 Integration
  required_mod: appliedenergistics2

navigation:
  title: Multi-Mod Integration
  required_mods:
    - appliedenergistics2
    - gregtech

navigation:
  title: Compatibility-specific Integration
  excluded_mod: legacy_addon

navigation:
  title: Incompatible Integration
  excluded_mods:
    - incompatible_addon
    - legacy_addon
```

## Optional Meta Fields

| Field | Description |
|-------|-------------|
| `title` | Page heading override (shown in the top-left of the guide screen) |
| `author` | Author attribution shown in the page footer |
| `date` | Creation date shown in the page footer |
| `updated` | Last-modified date shown in the page footer |

## Complete Example

```yaml
---
title: Advanced IO Bus
navigation:
  title: Advanced IO Bus
  parent: getting-started.md
  position: 10
  icon: advanced_ae:advanced_io_bus_part
  required_mod: advanced_ae
categories:
  - advanced items
item_ids:
  - advanced_ae:advanced_io_bus_part
  - advanced_ae:advanced_io_bus_part:1
author: pedroksl
date: 2025-01-01
---

# Advanced IO Bus

The Advanced IO Bus provides faster item transfer...
```


## Research bindings

[Research & Bindings](research.md) demonstrates research tags, generic YAML bindings, native navigation and multiple page/anchor destinations. Research ID display and copying are provided by ARI; copying does not require `showResearchId`.
