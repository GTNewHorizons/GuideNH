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

Met `wrap="inline"` wordt de afbeelding verticaal gecentreerd ten opzichte van de omringende tekst. De regel wordt hoger om de volledige afbeelding te bevatten. Aangrenzende tekstregels behouden hun normale hoogte; er geldt geen vaste maximale regelhoogte. Moet een zeer hoge afbeelding op een eigen regel staan, plaats de tag dan in een aparte alinea met een lege regel ervoor en erna.

`<FloatingImage>` renders Een cropped bitmap region that kan kommagetal met tekst of sit truly inline binnen a
paragraph. It ook accepts explicit `modid:path` texture ids, so it kan reference texture assets van
other mods directly.

### attributen

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `src` | yes | afbeelding pad |
| `x` | bij uitsnijden | crop begin X in source-afbeelding pixels |
| `y` | bij uitsnijden | crop begin Y in source-afbeelding pixels |
| `width` / `w` | bij uitsnijden | crop breedte in source-afbeelding pixels; exactly one spelling moet be gebruikt |
| `height` / `h` | bij uitsnijden | crop hoogte in source-afbeelding pixels; exactly one spelling moet be gebruikt |
| `scaleX` | no | horizontal weergeven multiplier, Standaard `1.0` |
| `scaleY` | no | vertical weergeven multiplier, Standaard `1.0` |
| `displayWidth` | no | final weergeven breedte in pixels; preserves De crop aspect ratio wanneer gebruikt alone |
| `displayHeight` | no | final weergeven hoogte in pixels; preserves De crop aspect ratio wanneer gebruikt alone |
| `wrap` | no | `inline` plaatst de afbeelding in de tekstregel; anders kiest `align` de zwevende zijde |
| `align` | no | `left` of `right` voor floating placement; ignored wanneer `wrap="inline"` |
| `title` | no | tooltip/title tekst |
| `alt` | no | Alternatieve tekst |
| `sound` | no | sound event played by De whole afbeelding |
| `soundSrc` | no | sound bestandspad voor De whole afbeelding |
| `trigger` | no | `click` by Standaard, of `hover` voor zweven playback |

### Notes

- `x`, `y`, `width` / `w`, en `height` / `h` are alle Vereist together wanneer cropping
- wanneer De crop attributen are alle omitted, `displayWidth` of `displayHeight` displays De full source afbeelding
- `width` en `height` bepalen de bronuitsnede. Gebruik `displayWidth` / `displayHeight` of schaalfactoren voor de weergavegrootte.
- `scaleX` en `scaleY` calculate De final weergeven grootte as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` of `displayHeight` sets De final weergeven grootte in pixels; wanneer alleen one is present, De other dimension is calculated van De crop aspect ratio
- providing both `displayWidth` en `displayHeight` staat toe intentional non-proportional stretching
- `displayWidth` / `displayHeight` kan niet be combined met `scaleX` / `scaleY`
- enkele-axis stretching is supported by setting alleen one scale differently
- `width` met `w`, of `height` met `h`, is invalid en renders Een zichtbaar error
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
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Beweeg over de afbeelding om deze tooltip te zien.
  </ImageAnnotation>
</FloatingImage>
````

Region annotatie met Een zichtbaar rand:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Dit is de tooltip voor het **gemarkeerde gebied**.
  </ImageAnnotation>
</FloatingImage>
````

meerdere regions on one afbeelding:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="0" y="0" w="16" h="32" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="16" y="0" w="16" h="32" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

afbeelding regions kan ook play sounds. gebruiken `<SoundArea>` wanneer you alleen need sound, of put `sound`
directly on `<ImageAnnotation>` wanneer De zelfde region ook has Een tooltip of rand.

````md
<FloatingImage
  displayWidth="128" displayHeight="128" src="test1.png"
  align="left"
  x="0"
  y="0"
  width="32"
  height="32"
  sound="guidenh:guide.sample_click"
>
  <SoundArea x="0" y="0" w="16" h="32" sound="guidenh:guide.sample_left" />
  <SoundArea x="16" y="0" w="16" h="32" sound="guidenh:guide.sample_hover" trigger="hover" />
  <ImageAnnotation x="2" y="2" w="10" h="10" border sound="guidenh:guide.sample_click">
    Dit gebied bevat tooltipinhoud en een klikgeluid.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers De whole afbeelding. Region sounds gebruiken cropped-afbeelding coördinaten
en obey De zelfde overlap priority as tooltips: later regions win.

## Inhoud insluiten en tekstterugloop

Bloktags op basis van `BlockTagCompiler`, zoals `<Recipe>`, `<GameScene>` en `<BlockImage>`, accepteren `wrap` en `align`.

`<FloatingImage>` heeft eigen regels: `wrap="inline"` plaatst de afbeelding in de tekstregel; anders kiest `align="left"` of `"right"` de zwevende zijde. `<ItemImage>` is een inline pictogram; gebruik `<Column>` of `<Row>` voor de uitlijning.

| Attribuut | Standaard | Betekenis |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: normale plaatsing. `square`, `tight`, `through`: links of rechts zwevend. `top-bottom`, `behind`, `front`: momenteel normale uitgelijnde plaatsing, zonder aparte tekstlagen. |
| `align` | `left` | `left`, `center` of `right` in blokcontext. Bij zwevende plaatsing kiest `right` rechts en de rest links. Centreren: `wrap="inline" align="center"`. |
| `float` | — | Ouder `left` / `right` in lopende tekst als `wrap` en `align` ontbreken. Gebruik bij voorkeur die expliciete attributen. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

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

`<BlockImage>` toont een geplaatst blok als transparant 3D-voorbeeld en ondersteunt bovenstaande blokattributen voor uitlijning en tekstomloop.

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
