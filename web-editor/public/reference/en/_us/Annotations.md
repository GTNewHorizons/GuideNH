# Annotations

GuideNH scene annotations are child tags inside `<GameScene>` / `<Scene>`. They render in world space and may contain child markdown/tag content that becomes a rich tooltip.

## General Rules

- annotations only work inside a scene
- child content becomes the tooltip body
- annotations can be hidden with the scene UI toggle
- `alwaysOnTop` draws above scene geometry when supported by the annotation type
- all scene annotations also accept optional `showWhenStructure`, `showWhenTier`, and `showWhenChannels` gates when the scene uses `<ImportStructureLib>`

## Supported Annotation Tags

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<OutlineAnnotation>`
- `<TextAnnotation>`

GuideNH also supports `<BlockAnnotationTemplate>`, which applies its child annotations to every already-placed matching block in the current scene.

## StructureLib Conditions

When a scene contains `<ImportStructureLib>`, every annotation tag may restrict its visibility to a specific
StructureLib state:

| Attribute | Meaning |
| --- | --- |
| `showWhenStructure` | bind the annotation to a named `<ImportStructureLib name="...">`; omit it when the scene only imports one StructureLib structure |
| `showWhenTier` | tier filter such as `2`, `1..3`, `!2`, or `1..5,!3` |
| `showWhenChannels` | per-channel filter such as `input:1..3, casing:!2, fluid:4` |

Rules:

- `showWhenTier` and `showWhenChannels` are combined with logical AND
- `showWhenChannels` may mention multiple channels in one attribute
- negated-only clauses like `!2` mean "any value except 2"
- the same attributes are also supported by `<PlaySound>` and `<BlockAnnotationTemplate>` child annotations

Example:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Only visible for the selected StructureLib state.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Highlights a single 1x1x1 block volume.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | yes | `x y z` vector |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, or `transparent` |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |

Example:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Highlights the controller block.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Highlights an arbitrary axis-aligned box.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `min` | yes | `x y z` minimum vector |
| `max` | yes | `x y z` maximum vector |
| `color` | no | annotation color |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |

GuideNH automatically swaps min/max coordinates per axis when they are provided in reverse order.

Example:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Half-height area highlight.
</BoxAnnotation>
````

## `<LineAnnotation>`

Draws a line segment or polyline in world space.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `from` | yes, unless `points` is set | `x y z` start vector |
| `to` | yes, unless `points` is set | `x y z` end vector |
| `points` | no | Semicolon-separated `x y z` points for a polyline; overrides `from` / `to` |
| `color` | no | annotation color |
| `thickness` | no | line thickness float |
| `alwaysOnTop` | no | boolean expression |
| `arrow` | no | `start` or `end`; omitted means no arrow |
| `showPoints` | no | boolean expression; shows every point as a small cube |
| `pointColor` | no | default cube color; omitted uses the line color |
| `pointSize` | no | default cube size; omitted uses a value slightly larger than `thickness` |

`LineAnnotation` may contain `<LinePoint>` children to override point marker styling. `LinePoint`
uses `index`, optional `show`, optional `color`, and optional `size`. Points are zero-indexed.
Arrows can only be placed on the start or end of the line; intermediate polyline points cannot carry
arrows.

Example:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signal path.
</LineAnnotation>
````

Polyline with a 3D endpoint arrow and selected point markers:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Signal path through a bend.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places a screen-facing diamond marker at a world position.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | yes | `x y z` marker position |
| `color` | no | tint color; omitted defaults to bright green |

Example:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<OutlineAnnotation>`

Draws a glow-style silhouette outline around blocks and entities, like the Glowing effect. The contour follows the rendered shape of every target, stays visible through blocks and entities in front of it, and merges with touching outlines into one shape.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | no | `x y z` block to outline; defaults to `0 0 0`, the matched block inside `<BlockAnnotationTemplate>` |
| `min` / `max` | no | inclusive block corners of a region; every block inside it is outlined |
| `entity` | no | `sceneEntityId` of an `<Entity>` to outline |
| `target` | no | `blocks` (default), `entities`, or `all`: what is outlined inside the region; `entities` without a region outlines every entity |
| `mode` | no | `auto` (default), `geometry`, `cutout`, or `bounds` |
| `color` | no | outline color; omitted uses white, or the scoreboard team color for entities |
| `thickness` | no | outline width in GUI pixels, `0.25` to `8`, default `1` |
| `alwaysOnTop` | no | boolean expression; occluded parts keep full brightness instead of being dimmed |
| `pulse` | no | boolean expression; makes the outline breathe |
| `pulsePeriod` | no | breathing period in seconds, default `2` |

Silhouette modes:

- `auto` decides per target. Closed models (cubes, glass, ice, fluids, panes, multipart and cable blocks, OBJ and TESR models) use their geometry, so see-through textures such as glass outline the whole block instead of the glass frame. Sprite-like renders (flowers, saplings, crops, torches, rails, ladders, vines, redstone) and entities use texture alpha, so the outline follows the drawn shape instead of the quads. Blocks that render nothing use their selection box.
- `geometry` always uses the rendered geometry.
- `cutout` always uses texture alpha.
- `bounds` outlines the selection boxes of blocks and the bounding boxes of entities.

Rules:

- the contour is drawn outside the silhouette only; the target itself is not tinted
- parts hidden behind other blocks or entities stay visible and are dimmed like other occluded annotations; `alwaysOnTop` keeps them at full brightness
- invisible entities still get an outline
- touching or overlapping outlines merge into one contour, also across different annotations
- translucent colors and the soft edge are blended with the scene, so the edge never turns black
- child content becomes the tooltip; hovering any outlined block or entity shows it
- inside `<BlockAnnotationTemplate>`, `pos` is relative to each matched block and the outlines of all matches merge into one shape
- exported sites draw the same outline with WebGL

Example:

````md
<GameScene zoom={3} interactive={true}>
  <Block id="minecraft:glass" />
  <Block id="minecraft:red_flower" x="2" />
  <Entity id="minecraft:sheep" sceneEntityId="sheep" x="1.5" z="2.5" />
  <OutlineAnnotation pos="0 0 0" color="#55FFFF">
    Glass is outlined as a full cube.
  </OutlineAnnotation>
  <OutlineAnnotation pos="2 0 0" color="#FFFF55" thickness="1.5" />
  <OutlineAnnotation entity="sheep" color="#FFAA00" alwaysOnTop={true} pulse={true} />
</GameScene>
````

Outline every matching block of a structure:

````md
<BlockAnnotationTemplate id="minecraft:glass">
  <OutlineAnnotation color="#AAFFFF" />
</BlockAnnotationTemplate>
````

## `<TextAnnotation>`

Draws a speech-bubble text label over the scene. It can either follow a world-space anchor point or
stay fixed relative to the scene center. Unlike the other annotation tags, its child content is the
bubble text itself rather than a hover tooltip.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `pos` | no | `x y z` world-space anchor vector |
| `x`, `y`, `z` | no | Alternative world-space anchor components when `pos` is omitted |
| `text` | no | Bubble text; child markdown is used when omitted |
| `textKey` | no | Translation key resolved from resource-pack `lang` files before falling back to `text` or child markdown |
| `color` | no | Bubble border color; defaults to light grey |
| `backgroundAlpha` | no | Background opacity from `0` to `255`; defaults to `204` |
| `maxWidth` | no | Wrap width in pixels; `0` keeps a single line |
| `independent` | no | `true` keeps the bubble fixed in screen space |
| `yOffset` | no | Pixel offset from the scene center when `independent={true}` |
| `connectorSide` | no | `bottom`, `top`, `left`, `right`, or `none`; defaults to `bottom` |
| `connectorOffset` | no | Pixel offset along the bubble edge; positive moves right for top/bottom and down for left/right |
| `connectorLength` | no | Pixel length of the connector line; defaults to `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | no | Optional companion highlight box bounds |
| `highlightColor` | no | Optional highlight box color |

World-anchored bubbles draw a connector line to their anchor. Use `connectorSide` to choose which
edge of the bubble points at the anchor, `connectorOffset` to move the attachment point along that
edge, and `connectorLength` to control the gap between the bubble and anchor. Independent bubbles
are centered horizontally in the scene and use `yOffset` for vertical placement. They do not draw a
connector. The same runtime annotation is also used when importing Ponder `text` annotations.

Example:

````md
<TextAnnotation
  pos="1.5 2 1.5"
  textKey="guidenh.sample.scene.insert_items"
  color="#FF44AAFF"
  maxWidth={120}
  backgroundAlpha={180}
  connectorSide="right"
  connectorOffset={8}
  connectorLength={12}
>
  Insert items here with **priority**.
</TextAnnotation>
````

Fixed screen-space example:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## Rich Tooltip Content

Annotation children are compiled as normal GuideNH content, so tooltips may contain:

- markdown paragraphs and headings
- item/block images
- recipes
- nested non-interactive scenes

Example:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

Use it when you want to stamp the same annotation onto every matching block.

| Attribute | Required | Meaning |
| --- | --- | --- |
| `id` | yes | block matcher in `modid:block[:meta]` form |

Rules:

- the template only sees blocks that already exist when it is parsed
- place it after `<Block>`, `<ImportStructure>`, or `<ImportStructureLib>` tags that should feed it
- child annotations use local coordinates relative to each matched block

Example:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Template-generated tooltip
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Related Pages

- [GameScene](GameScene)
- [Examples](Examples)
