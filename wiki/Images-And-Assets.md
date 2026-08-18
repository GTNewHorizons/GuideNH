# Images And Assets

GuideNH supports both normal markdown images and several runtime-specific visual elements.

## Asset Resolution Rules

Guide assets resolve with the same rules used by page links.

| Path form | Example | Meaning |
| --- | --- | --- |
| relative | `test1.png` | relative to the current page file |
| rooted | `/assets/example_structure.snbt` | relative to the current guide root |
| explicit resource id | `guidenh:textures/gui/example.png` | absolute `modid:path` lookup |

## Markdown Images

Normal markdown images are supported:

````md
![Example](test1.png)
````

GuideNH resolves the path and loads the binary asset from the guide content root.

## `FloatingImage`

`<FloatingImage>` renders a cropped bitmap region that can float with text or sit truly inline inside a
paragraph. It also accepts explicit `modid:path` texture ids, so it can reference texture assets from
other mods directly.

### Attributes

| Attribute | Required | Meaning |
| --- | --- | --- |
| `src` | yes | image path |
| `x` | yes | crop start X in source-image pixels |
| `y` | yes | crop start Y in source-image pixels |
| `width` / `w` | yes | crop width in source-image pixels; exactly one spelling must be used |
| `height` / `h` | yes | crop height in source-image pixels; exactly one spelling must be used |
| `scaleX` | no | horizontal display multiplier, default `1.0` |
| `scaleY` | no | vertical display multiplier, default `1.0` |
| `displayWidth` | no | final display width in pixels; preserves the crop aspect ratio when used alone |
| `displayHeight` | no | final display height in pixels; preserves the crop aspect ratio when used alone |
| `wrap` | no | `inline` for true inline placement, otherwise use the normal wrapping modes |
| `align` | no | `left` or `right` for floating placement; ignored when `wrap="inline"` |
| `title` | no | tooltip/title text |
| `sound` | no | sound event played by the whole image |
| `soundSrc` | no | sound file path for the whole image |
| `trigger` | no | `click` by default, or `hover` for hover playback |

### Notes

- `x`, `y`, `width` / `w`, and `height` / `h` are all required together when cropping
- when the crop attributes are all omitted, `displayWidth` or `displayHeight` displays the full source image
- `width` and `height` now describe the crop rectangle, not the final display size
- `scaleX` and `scaleY` calculate the final display size as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` or `displayHeight` sets the final display size in pixels; when only one is present, the other dimension is calculated from the crop aspect ratio
- providing both `displayWidth` and `displayHeight` allows intentional non-proportional stretching
- `displayWidth` / `displayHeight` cannot be combined with `scaleX` / `scaleY`
- single-axis stretching is supported by setting only one scale differently
- `width` with `w`, or `height` with `h`, is invalid and renders a visible error
- old `FloatingImage width/height as display size` content is intentionally breaking and must be migrated manually
- `src` may be relative, rooted, or an explicit `modid:path` texture id such as `minecraft:textures/gui/options_background.png`

### Example

````md
<FloatingImage
  src="minecraft:textures/gui/options_background.png"
  x="0"
  y="0"
  width="32"
  height="32"
  displayWidth="64"
  displayHeight="64"
  wrap="inline"
  title="Example"
/>
````

## `ImageAnnotation`

`<ImageAnnotation>` is a child element of `<FloatingImage>` that attaches a rich-text tooltip (and
an optional colored border) to a rectangular region of the image. Coordinates are specified in
**cropped-image pixels** and are automatically proportionally scaled when the cropped image is resized
or stretched.

### Attributes

| Attribute | Required | Default | Meaning |
| --- | --- | --- | --- |
| `x` | no | — | left edge of the region in image pixels |
| `y` | no | — | top edge of the region in image pixels |
| `w` | no | — | width of the region in image pixels |
| `h` | no | — | height of the region in image pixels |
| `border` | no | `false` | show a colored border around the region |
| `borderColor` | no | random | border color (`#RRGGBB` or `#AARRGGBB`) |
| `borderThickness` | no | `1` | border thickness in display pixels |
| `sound` | no | none | optional sound event played for this region |
| `src` | no | none | optional sound file path; converted to a sound event id |
| `trigger` | no | `click` | `click` or `hover` |

### Notes

- omitting all four of `x`, `y`, `w`, `h` makes the annotation cover the **whole image**
- if any of the four is present, the remaining omitted ones default to `0` (origin) or `1` (size)
- border is **not shown by default**; add `border` or `border={true}` to enable it
- when `borderColor` is omitted and `border` is enabled, a random fully-opaque color is used
- child MDX content is rendered as the tooltip body and may include any inline/block elements
- later annotations (lower in the list) take hover priority over earlier ones when regions overlap

### Example

Whole-image annotation:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    Hover anywhere on the image to see this tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Region annotation with a visible border:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    This is the **highlighted region** tooltip.
  </ImageAnnotation>
</FloatingImage>
````

Multiple regions on one image:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

Image regions can also play sounds. Use `<SoundArea>` when you only need sound, or put `sound`
directly on `<ImageAnnotation>` when the same region also has a tooltip or border.

````md
<FloatingImage
  src="test1.png"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  sound="guidenh:image.click"
>
  <SoundArea x="0" y="0" w="64" h="64" sound="guidenh:image.left" />
  <SoundArea x="64" y="0" w="64" h="64" sound="guidenh:image.right" trigger="hover" />
  <ImageAnnotation x="10" y="10" w="40" h="40" border sound="guidenh:image.note">
    This region has both tooltip content and a click sound.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers the whole image. Region sounds use cropped-image coordinates
and obey the same overlap priority as tooltips: later regions win.

## Content Embedding and Text Wrapping

All block-level tags — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
and any other tag backed by `BlockTagCompiler` — support two optional layout attributes that
provide Word-style content embedding.

| Attribute | Values | Default | Meaning |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | Text-wrapping mode |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Wrap modes

| Mode | Word equivalent | Block-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | In line with text | Default stack (嵌入型) | Sits on the text line |
| `square` | Square | Document-level float; text wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | Same as `square` (紧密型) | Same as `square` |
| `through` | Through | Same as `square` (穿越型) | Same as `square` |
| `top-bottom` | Top and Bottom | Full-width slot; `align` repositions horizontally (上下型) | Line-inline with breaks |
| `behind` | Behind text | Aligned inline slot; renders behind text (衬于文字下方) | Sits on the line |
| `front` | In front of text | Aligned inline slot; renders in front of text (浮于文字上方) | Sits on the line |

### Alignment with floating wrap

For `wrap=square/tight/through`:
- `align=left` (default) — block floats to the **left**; text fills the right side.
- `align=right` — block floats to the **right**; text fills the left side.
- `align=center` — block is centred without floating (no text wrapping).

### Examples

Left-floating image using the new `wrap` attribute:

````md
<FloatingImage
  src="test1.png"
  wrap="square"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  scaleX="0.5"
  scaleY="0.5"
/>

Paragraph text that flows to the right of the image...
````

Right-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Text that flows to the left of the recipe box...
````

Centred item image (no text wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

Right-aligned item image:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

Item NBT can be supplied separately from the item id. Inline SNBT in `id` is still supported;
when both forms are present, the standalone `nbt` attribute is merged last.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **Note** — `wrap="inline"` now gives `<FloatingImage>` true inline placement inside flow text.
> In inline mode, `align` is ignored instead of producing an error.

## Navigation Texture Icons

Frontmatter can use `icon_texture` to show a texture instead of an item in navigation/search:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

The file must decode as an image. The path is resolved like any other guide asset path.

## Non-Image Assets

GuideNH pages may also reference non-image runtime assets, especially structure files, for example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

These assets are loaded through the same guide asset pipeline but are consumed by custom tags rather than rendered directly as images.

## Best Practices

- keep page-local images near the page that uses them
- keep reusable files under the guide root `assets/` folder
- prefer rooted `/assets/...` paths for shared files referenced by multiple pages
- use texture icons only for real image assets

## `BlockImage`

`<BlockImage>` uses the same block-level embedding rules as `<FloatingImage>`, but the visual
content is a transparent 3D single-block preview instead of a bitmap. It is best suited for
showing how a placed block looks in-world while still fitting inline with normal guide prose.

Key behavior:

- transparent background and border
- no scene buttons, no layer slider, no annotation authoring surface
- hover still shows the selected block outline and tooltip
- `scale` changes camera zoom
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, and `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT still works, but the
  standalone `nbt` attribute is easier to read and is preferred

Example:

````md
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## Runtime Example Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
