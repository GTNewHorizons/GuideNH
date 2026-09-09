---
navigation:
  title: Images
  parent: index.md
  position: 145
  icon: minecraft:wool:1
categories:
  - widgets
  # Cycling icons example — uncomment to enable:
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4
  #   - minecraft:wool:14
  # Inline SNBT example on the icon item:
  # icon: minecraft:wool:1:{display:{Name:"Colored Wool Demo"}}
---

# Images

`<FloatingImage>`, `<ItemImage>`, and `<BlockImage>` rendering tests.

## FloatingImage

Relative path (`test1.png` in the same directory):

![Test Image](test1.png)

Inline image mixed with text: here ![inline](test1.png) is an inline image.

`<FloatingImage>` crops first and then determines the display size. `x`, `y`, `width` / `w`, and `height` / `h`
select the source rectangle on the original image. `scaleX` and `scaleY` resize that cropped result by a multiplier.
`displayWidth` and `displayHeight` specify final pixel dimensions instead. One display dimension preserves the crop
aspect ratio; both allow stretching. Display dimensions cannot be combined with `scaleX` or `scaleY`.

Cropped 64×64 region displayed at native size:

<FloatingImage src="test1.png" align="left" x="0" y="0" width="64" height="64" title="crop 64x64" />

Cropped 64×64 region stretched to 200×80:

<FloatingImage src="test1.png" align="right" x="0" y="0" width="64" height="64" displayWidth="200" displayHeight="80" title="stretch 200x80" />

Set only one final dimension to preserve the cropped image ratio:

<FloatingImage src="test1.png" align="left" x="0" y="0" width="64" height="32" displayWidth="160" title="160x80 proportional crop" />

Cross-mod texture with true inline placement:

Text before <FloatingImage src="minecraft:textures/gui/options_background.png" x="0" y="0" width="32" height="32" scaleX="0.75" scaleY="0.75" wrap="inline" title="inline crop" /> text after.

## ImageAnnotation

`<ImageAnnotation>` children attach hover tooltips (and optional colored borders) to rectangular
regions of a `<FloatingImage>`. Coordinates (`x`, `y`, `w`, `h`) are in **cropped-image pixels**; the
region is automatically scaled when the cropped image is resized or stretched. Omitting all four covers the
entire image.

Whole-image annotation (hover anywhere over the image to see the tooltip):

<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    This tooltip appears when you hover over **any part** of the image.
  </ImageAnnotation>
</FloatingImage>

Region annotation with a visible red border (x=10, y=10, w=60, h=40):

<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    Hovering the **red-bordered region** shows this tooltip.
  </ImageAnnotation>
</FloatingImage>

Multiple annotations on one image — each region shows a different tooltip:

<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>

Scaled crop with an annotation that follows the stretch:

<FloatingImage src="test1.png" align="right" x="0" y="0" width="64" height="64" scaleX="3.125" scaleY="1.25">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FFFFFF44" borderThickness="2">
    Entire cropped region after stretching.
  </ImageAnnotation>
</FloatingImage>

## Image Sounds

Click the image or the left region to play a custom sound. Hover the right region to play a hover
sound. The example declares the event ids in the resource pack's `assets/guidenh/sounds.json`;
actual `.ogg` files should be placed below `assets/guidenh/sounds/`.

&[Inline sound action](sound:guidenh:guide.sample_click)

<SoundLink sound="guidenh:guide.sample_click" volume="0.8">
  **Rich text sound link**
</SoundLink>

<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128" sound="guidenh:guide.sample_click">
  <SoundArea x="0" y="0" w="64" h="128" sound="guidenh:guide.sample_left" />
  <SoundArea x="64" y="0" w="64" h="128" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="16" y="16" w="32" h="32" border borderColor="#FFFFCC44"
    sound="guidenh:guide.sample_click">
    This annotation has both a tooltip and a click sound.
  </ImageAnnotation>
</FloatingImage>

## ItemImage Scale

<Row>
  <ItemImage id="minecraft:diamond" scale="1" />
  <ItemImage id="minecraft:diamond" scale="2" />
  <ItemImage id="minecraft:diamond" scale="3" />
  <ItemImage id="minecraft:diamond" scale="4" />
  <ItemImage id="minecraft:diamond" scale="6" />
</Row>

## ItemImage NBT

`ItemImage` accepts an optional `nbt` attribute for item stack data. Inline SNBT in `id` remains
supported; when both forms are present, the standalone `nbt` attribute is merged last.

<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />

<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>

### Inline Icon vs. Text Baseline

Inline `<ItemImage>` icons are nudged upward by ~4 pixels (scaled by `scale`) so their visual center lines up with the surrounding text baseline. The label text receives a separate, smaller default nudge (-2 px). Both can be overridden independently.

- Default offset (-4px icon, -3px label): this line mixes <ItemImage id="minecraft:diamond" label="right" /> diamond, <ItemImage id="minecraft:apple" label="right" /> apple and <ItemImage id="minecraft:iron_ingot" label="right" /> iron ingot.
- Disabled icon offset (`yOffset="0"`, label unchanged): <ItemImage id="minecraft:diamond" yOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" label="right" /> apple.
- Disabled label offset (`labelYOffset="0"`, icon unchanged): <ItemImage id="minecraft:diamond" labelYOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" labelYOffset="0" label="right" /> apple.
- Both offsets zeroed (`yOffset="0" labelYOffset="0"`): <ItemImage id="minecraft:diamond" yOffset="0" labelYOffset="0" label="right" /> diamond, <ItemImage id="minecraft:apple" yOffset="0" labelYOffset="0" label="right" /> apple.

> Values are pixels at `scale=1` and are multiplied by the current scale at render time.

## ItemImage Label

Label to the right (default italic name):

<ItemImage id="minecraft:diamond" label="right" />

Label to the left:

<ItemImage id="minecraft:iron_ingot" label="left" />

Bold format with `%s` placeholder:

<ItemImage id="minecraft:gold_ingot" label="right" format="**%s**" />

Strikethrough format:

<ItemImage id="minecraft:rotten_flesh" label="right" format="~~%s~~" />

Underline (using `__`):

<ItemImage id="minecraft:emerald" label="right" format="__%s__" />

Wavy underline:

<ItemImage id="minecraft:blaze_rod" label="right" format="^^%s^^" />

Dotted underline:

<ItemImage id="minecraft:ender_pearl" label="right" format="::Custom Label::" />

Icon hidden, label only:

<ItemImage id="minecraft:diamond" showIcon="false" label="right" />

Icon shown, no tooltip:

<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />

## BlockImage Scale

`BlockImage` now renders a transparent 3D placed-block preview rather than an item-form icon.
When `scale` is omitted, it defaults to `4`.

<Row>
  <BlockImage id="minecraft:stone" />
  <BlockImage id="minecraft:stone" scale="1" />
  <BlockImage id="minecraft:stone" scale="2" />
  <BlockImage id="minecraft:stone" scale="3" />
  <BlockImage id="minecraft:stone" scale="4" />
  <BlockImage id="minecraft:stone" scale="6" />
</Row>

## BlockImage Perspective And Tile NBT

<Row>
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-east" />
  <BlockImage id="minecraft:furnace" scale="2.5" perspective="isometric-north-west" />
  <BlockImage id="minecraft:chest" scale="2.5" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
</Row>

## BlockImage Row Samples

<Row>
  <BlockImage id="minecraft:log" scale="4" />
  <BlockImage id="minecraft:log2" scale="4" />
  <BlockImage id="minecraft:planks" scale="4" />
  <BlockImage id="minecraft:cobblestone" scale="4" />
  <BlockImage id="minecraft:stonebrick" scale="4" />
  <BlockImage id="minecraft:mossy_cobblestone" scale="4" />
</Row>

<ItemImage id="minecraft:compass" />

## ItemLink

Basic link (text only, tooltip enabled):

<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />

Icon to the left of the link text:

<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />

Icon to the right, tooltip suppressed:

<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />

Ore-dictionary lookup:

<ItemLink ore="stickWood" />

Explicit link target with anchor:

<ItemLink id="minecraft:diamond" linksTo="./markdown.md#headings" />

Same-page anchor link:

<ItemLink id="minecraft:diamond" linksTo="#itemlink" />
