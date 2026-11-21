---
navigation:
  title: Images
  parent: index.md
  position: 145
  icon: minecraft:wool:1
categories:
  - widgets
---

# Images

The bundled `test1.png` is **32×32** pixels. Crop coordinates use source pixels. Image-region annotations use pixels within the crop; display dimensions control how large the result appears.

## Markdown Images

![Test image](test1.png)

An inline image ![inline](test1.png) can appear inside a paragraph. Use `<FloatingImage>` when you need explicit display dimensions, cropping, or image-region tooltips.

## FloatingImage

| Attribute | Default | Meaning |
| --- | --- | --- |
| `src` | required | Relative, rooted, or `modid:path` image reference |
| `x`, `y` | required when cropping | Crop origin in source pixels; non-negative integers |
| `width` / `w`, `height` / `h` | required when cropping | Positive integer crop dimensions; use exactly one spelling per dimension |
| `displayWidth`, `displayHeight` | crop size | Final positive integer display dimensions; one dimension preserves the crop aspect ratio |
| `scaleX`, `scaleY` | `1` | Positive multipliers for crop dimensions; cannot be combined with display dimensions |
| `wrap` | floating | `inline` places the image within the text line; otherwise `align` chooses the floating side |
| `align` | `left` | `left` or `right`; ignored with `wrap="inline"` |
| `title`, `alt` | none | Hover title and alternative text |
| `sound`, `soundSrc` | none | Whole-image sound event or sound-file path |
| `trigger` | `click` | `click` or `hover` |

Omit all crop attributes and provide `displayWidth` or `displayHeight` to display the entire image. When cropping, provide `x`, `y`, width, and height together.

Whole image displayed at 128×128:

<FloatingImage src="test1.png" displayWidth="128" wrap="inline" title="32x32 source at 128x128" />

A 16×16 crop from the source, stretched to 160×80:

<FloatingImage src="test1.png" x="8" y="8" width="16" height="16" displayWidth="160" displayHeight="80" wrap="inline" title="16x16 crop at 160x80" />

One display dimension preserves aspect ratio:

<FloatingImage src="test1.png" x="0" y="0" width="32" height="16" displayWidth="160" wrap="inline" title="32x16 crop at 160x80" />

Scale multipliers are an alternative to display dimensions:

Text before <FloatingImage src="minecraft:textures/gui/options_background.png" x="0" y="0" width="32" height="32" scaleX="0.75" scaleY="0.75" wrap="inline" title="24x24 display" /> text after.

### ImageAnnotation

`<ImageAnnotation>` is a child of `<FloatingImage>`. Its content is the rich tooltip. Coordinates use **cropped-image pixels** and follow resizing automatically.

| Attribute | Default | Meaning |
| --- | --- | --- |
| `x`, `y` | `0` | Region origin; omit all four coordinates to cover the whole image |
| `w`, `h` | `1` | Region width and height when any coordinate is present |
| `border` | `false` | Enable a colored border |
| `borderColor` | random opaque color | `#RRGGBB` or `#AARRGGBB` |
| `borderThickness` | `1` | Border width in display pixels |
| `sound`, `src` | none | Region sound event or sound-file path |
| `trigger` | `click` | `click` or `hover` |

Hover anywhere on the image:

<FloatingImage src="test1.png" displayWidth="128" wrap="inline">
  <ImageAnnotation>
    The annotation covers the **whole image**.
  </ImageAnnotation>
</FloatingImage>

Two regions cover the left and right halves of the 32×32 source:

<FloatingImage src="test1.png" x="0" y="0" width="32" height="32" displayWidth="160" displayHeight="80" wrap="inline">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44" borderThickness="2">
    Left half, scaled with the image.
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF" borderThickness="2">
    Right half, scaled with the image.
  </ImageAnnotation>
</FloatingImage>

### Image Sounds

The sound event ids are declared in `assets/guidenh/sounds.json`; supply their audio files under `assets/guidenh/sounds/` to hear them. Later regions take priority when regions overlap.

&[Inline sound action](sound:guidenh:guide.sample_click)

<SoundLink sound="guidenh:guide.sample_click" volume="0.8">**Play the sample sound**</SoundLink>

<FloatingImage src="test1.png" displayWidth="128" wrap="inline" sound="guidenh:guide.sample_click">
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="4" y="4" w="8" h="8" border borderColor="#FFFFCC44" sound="guidenh:guide.sample_click">
    A tooltip, border, and click sound on the same region.
  </ImageAnnotation>
</FloatingImage>

## ItemImage

| Attribute | Default | Meaning |
| --- | --- | --- |
| `id` | required | Item id, optional metadata, and optional inline SNBT |
| `ore` | none | Ore-dictionary selector; takes precedence over the supplied `id` |
| `nbt` | none | Item-stack SNBT; merges over inline SNBT |
| `scale` | `1` | Icon scale |
| `showTooltip` | `true` | Show the item tooltip |
| `noTooltip` | `false` | Legacy tooltip switch; prefer `showTooltip` |
| `showIcon` | `true` | Show the icon graphic |
| `label` | none | Item name to the `left` or `right` of the icon |
| `format` | italic name | Label pattern; `%s` inserts the item name |
| `yOffset` | `0` | Vertical offset of the inline item and label, in pixels at scale `1` |
| `labelYOffset` | `0` | Additional label-only vertical offset, in pixels at scale `1` |

Offsets scale with the icon; positive values move downward. To supply custom rich tooltip content, use `<Tooltip label="...">` with child content, as shown on [Tooltips](./tooltips.md).

### Scale and NBT

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
</Row>

<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />

<ItemImage id="minecraft:iron_ingot" ore="ingotIron" label="right" />

### Labels and Alignment

- Default offsets: <ItemImage id="minecraft:diamond" label="right" />
- Whole item moved down: <ItemImage id="minecraft:diamond" label="right" yOffset="2" />
- Label moved up: <ItemImage id="minecraft:diamond" label="right" labelYOffset="-2" />
- Name on the left: <ItemImage id="minecraft:iron_ingot" label="left" />
- Bold name: <ItemImage id="minecraft:gold_ingot" label="right" format="**%s**" />
- Strikethrough: <ItemImage id="minecraft:rotten_flesh" label="right" format="~~%s~~" />
- Underline: <ItemImage id="minecraft:emerald" label="right" format="__%s__" />
- Wavy underline: <ItemImage id="minecraft:blaze_rod" label="right" format="^^%s^^" />
- Dotted custom text: <ItemImage id="minecraft:ender_pearl" label="right" format="::Custom Label::" />
- Label only: <ItemImage id="minecraft:diamond" showIcon={false} label="right" />
- No item tooltip: <ItemImage id="minecraft:emerald" label="right" showTooltip={false} />

## BlockImage

`<BlockImage>` shows a placed block as a transparent 3D preview, with its block-name tooltip and hover outline.

| Attribute | Default | Meaning |
| --- | --- | --- |
| `id` / `ore` | one required | Block reference or ore-dictionary selector |
| `meta` | from `id` | Explicit block metadata override |
| `nbt` | none | Tile-entity SNBT, merged over inline SNBT |
| `scale` | `4` | Camera zoom multiplier |
| `width`, `height` | `128` each | Preview viewport in pixels |
| `perspective` | `isometric-north-east` | `isometric-north-east`, `isometric-north-west`, or `up` |
| `wrap` | `inline` | Block layout wrapping mode |
| `align` | `left` | `left`, `center`, or `right` in block context |
| `float` | none | Legacy `left` / `right` in flow context; prefer `wrap` and `align` |

<Row>
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

<Row>
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-east" />
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-west" />
  <BlockImage id="minecraft:chest" scale="2.5" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
</Row>

The chest uses **tile-entity** NBT; `ItemImage nbt` uses **item-stack** NBT instead. More placement examples: [Content Embedding](./content-embed.md).

## ItemLink

Text only: <ItemLink id="minecraft:chest" />

Icon on the left: <ItemLink id="minecraft:chest" showIcon="left" />

Icon on the right, tooltip disabled: <ItemLink id="minecraft:diamond" showIcon="right" showTooltip={false} />

Ore-dictionary lookup: <ItemLink ore="stickWood" />

Link to another page: <ItemLink id="minecraft:diamond" linksTo="./markdown.md#headings" />

Same-page anchor: <ItemLink id="minecraft:diamond" linksTo="#itemlink" />
