# Afbeeldingen en assets

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH ondersteunt both normal Markdown afbeeldingen en several runtime-specific visual elements.

## asset Resolution Regels

gids assets oplossen met De zelfde Regels gebruikt by pagina links.

| pad form | Example | Betekenis |
| --- | --- | --- |
| relative | `test1.png` | relative naar De huidige pagina bestand |
| rooted | `/assets/example_structure.snbt` | relative naar De huidige gids root |
| explicit resource id | `guidenh:textures/gui/example.png` | absolute `modid:path` lookup |

## Markdown afbeeldingen

Normal Markdown afbeeldingen are supported:

````md
![Example](test1.png)
````

GuideNH resolves De pad en laadt De binary asset van De gids inhoud root.

## `FloatingImage`

`<FloatingImage>` renders Een cropped bitmap region that kan kommagetal met tekst of sit truly inline binnen a
paragraph. It ook accepts explicit `modid:path` texture ids, so it kan reference texture assets van
other mods directly.

### attributen

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `src` | yes | afbeelding pad |
| `x` | yes | crop begin X in source-afbeelding pixels |
| `y` | yes | crop begin Y in source-afbeelding pixels |
| `width` / `w` | yes | crop breedte in source-afbeelding pixels; exactly one spelling moet be gebruikt |
| `height` / `h` | yes | crop hoogte in source-afbeelding pixels; exactly one spelling moet be gebruikt |
| `scaleX` | no | horizontal weergeven multiplier, Standaard `1.0` |
| `scaleY` | no | vertical weergeven multiplier, Standaard `1.0` |
| `displayWidth` | no | final weergeven breedte in pixels; preserves De crop aspect ratio wanneer gebruikt alone |
| `displayHeight` | no | final weergeven hoogte in pixels; preserves De crop aspect ratio wanneer gebruikt alone |
| `wrap` | no | `inline` voor true inline placement, otherwise gebruiken De normal wrapping modes |
| `align` | no | `left` of `right` voor floating placement; ignored wanneer `wrap="inline"` |
| `title` | no | tooltip/title tekst |
| `sound` | no | sound event played by De whole afbeelding |
| `soundSrc` | no | sound bestandspad voor De whole afbeelding |
| `trigger` | no | `click` by Standaard, of `hover` voor zweven playback |

### Notes

- `x`, `y`, `width` / `w`, en `height` / `h` are alle Vereist together wanneer cropping
- wanneer De crop attributen are alle omitted, `displayWidth` of `displayHeight` displays De full source afbeelding
- `width` en `height` now describe De crop rectangle, niet De final weergeven grootte
- `scaleX` en `scaleY` calculate De final weergeven grootte as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` of `displayHeight` sets De final weergeven grootte in pixels; wanneer alleen one is present, De other dimension is calculated van De crop aspect ratio
- providing both `displayWidth` en `displayHeight` staat toe intentional non-proportional stretching
- `displayWidth` / `displayHeight` kan niet be combined met `scaleX` / `scaleY`
- enkele-axis stretching is supported by setting alleen one scale differently
- `width` met `w`, of `height` met `h`, is invalid en renders Een zichtbaar error
- old `FloatingImage width/height as display size` inhoud is intentionally breaking en moet be migrated manually
- `src` kan be relative, rooted, of Een explicit `modid:path` texture id such as `minecraft:textures/gui/options_background.png`

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

`<ImageAnnotation>` is Een kind element of `<FloatingImage>` that attaches Een rich-tekst tooltip (en
Een optioneel colored rand) naar Een rectangular region of De afbeelding. coördinaten are specified in
**cropped-afbeelding pixels** en are automatisch proportionally scaled wanneer De cropped afbeelding is resized
of stretched.

### attributen

| Attribuut | Vereist | Standaard | Betekenis |
| --- | --- | --- | --- |
| `x` | no | — | left edge of De region in afbeelding pixels |
| `y` | no | — | top edge of De region in afbeelding pixels |
| `w` | no | — | breedte of De region in afbeelding pixels |
| `h` | no | — | hoogte of De region in afbeelding pixels |
| `border` | no | `false` | tonen Een colored rand around De region |
| `borderColor` | no | random | rand kleur (`#RRGGBB` of `#AARRGGBB`) |
| `borderThickness` | no | `1` | rand thickness in weergeven pixels |
| `sound` | no | none | optioneel sound event played voor Deze region |
| `src` | no | none | optioneel sound bestandspad; converted naar Een sound event id |
| `trigger` | no | `click` | `click` of `hover` |

### Notes

- omitting alle four of `x`, `y`, `w`, `h` makes De annotatie cover De **whole afbeelding**
- Als any of De four is present, De remaining omitted ones Standaard naar `0` (origin) of `1` (grootte)
- rand is **niet getoond by Standaard**; Voeg toe `border` of `border={true}` naar enable it
- wanneer `borderColor` wordt weggelaten en `border` is ingeschakeld, Een random fully-opaque kleur is gebruikt
- kind MDX inhoud is gerenderd as De tooltip body en kan include any inline/blok elements
- later Annotaties (lower in De lijst) take zweven priority over earlier ones wanneer regions overlap

### Example

Whole-afbeelding annotatie:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    Beweeg over de afbeelding om deze tooltip te zien.
  </ImageAnnotation>
</FloatingImage>
````

Region annotatie met Een zichtbaar rand:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    Dit is de tooltip voor het **gemarkeerde gebied**.
  </ImageAnnotation>
</FloatingImage>
````

meerdere regions on one afbeelding:

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

afbeelding regions kan ook play sounds. gebruiken `<SoundArea>` wanneer you alleen need sound, of put `sound`
directly on `<ImageAnnotation>` wanneer De zelfde region ook has Een tooltip of rand.

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
    Dit gebied bevat tooltipinhoud en een klikgeluid.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers De whole afbeelding. Region sounds gebruiken cropped-afbeelding coördinaten
en obey De zelfde overlap priority as tooltips: later regions win.

## Inhoud insluiten en tekstterugloop

alle blok-level tags — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
en any other tag backed by `BlockTagCompiler` — ondersteuning two optioneel layout attributen that
bieden Word-style inhoud embedding.

| Attribuut | waarden | Standaard | Betekenis |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | tekst-wrapping modus |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Terugloopmodi

| modus | Word equivalent | blok-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | In regel met tekst | Standaard stack (嵌入型) | Sits on De tekst regel |
| `square` | Square | Document-level kommagetal; tekst wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | zelfde as `square` (紧密型) | zelfde as `square` |
| `through` | Through | zelfde as `square` (穿越型) | zelfde as `square` |
| `top-bottom` | Top en Bottom | Full-breedte slot; `align` repositions horizontally (上下型) | regel-inline met breaks |
| `behind` | Behind tekst | Aligned inline slot; renders behind tekst (衬于文字下方) | Sits on De regel |
| `front` | In front of tekst | Aligned inline slot; renders in front of tekst (浮于文字上方) | Sits on De regel |

### Alignment met floating wrap

voor `wrap=square/tight/through`:
- `align=left` (Standaard) — blok floats naar De **left**; tekst fills De right side.
- `align=right` — blok floats naar De **right**; tekst fills De left side.
- `align=center` — blok is centred zonder floating (no tekst wrapping).

### Voorbeelden

Left-floating afbeelding using De new `wrap` Attribuut:

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

Paragraaftekst die rechts van de afbeelding loopt…
````

Right-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Tekst die links van het receptvak loopt…
````

Centred item afbeelding (no tekst wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

Right-aligned item afbeelding:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

item NBT kan be supplied separately van De item id. Inline SNBT in `id` is still supported;
wanneer both forms are present, De standalone `nbt` Attribuut is merged last.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **Opmerking** — `wrap="inline"` now gives `<FloatingImage>` true inline placement binnen flow tekst.
> In inline modus, `align` is ignored instead of producing Een error.

## Navigatie Texture Icons

frontmatter kan gebruiken `icon_texture` naar tonen Een texture instead of Een item in Navigatie/zoeken:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

De bestand moet decode as Een afbeelding. De pad is opgelost like any other gids asset pad.

## Non-afbeelding Assets

GuideNH pages kan ook reference non-afbeelding runtime assets, especially structure files, voor example:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

Deze assets are loaded through De zelfde gids asset pipeline but are consumed by aangepaste tags rather than gerenderd directly as afbeeldingen.

## Best Practices

- Behoud pagina-lokale afbeeldingen near De pagina that uses them
- Behoud reusable files under De gids root `assets/` map
- prefer rooted `/assets/...` paths voor shared files referenced by meerdere pages
- gebruiken texture icons alleen voor real afbeelding assets

## `BlockImage`

`<BlockImage>` uses De zelfde blok-level embedding Regels as `<FloatingImage>`, but De visual
inhoud is Een transparent 3D enkele-blok voorbeeldweergave instead of Een bitmap. It is best suited voor
showing how Een placed blok looks in-wereld terwijl still fitting inline met normal gids prose.

Key behavior:

- transparent achtergrond en rand
- no scène buttons, no layer slider, no annotatie authoring surface
- zweven still shows De geselecteerd blok outline en tooltip
- `scale` wijzigingen camera zoomen en defaults naar `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, en `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT still works, but De
  standalone `nbt` Attribuut is easier naar read en is preferred

Example:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## runtime Example Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Gerelateerde pagina's

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
