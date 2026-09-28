# Annotaties

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH scène Annotaties are kind tags binnen `<GameScene>` / `<Scene>`. They renderen in wereld space en kan bevatten kind Markdown/tag inhoud that becomes Een rich tooltip.

## Algemene regels

- Annotaties alleen work binnen Een scène
- kind inhoud becomes De tooltip body
- Annotaties kan be verborgen met De scène UI toggle
- `alwaysOnTop` draws above scène geometry wanneer supported by De annotatie Type
- alle scène Annotaties ook accept optioneel `showWhenStructure`, `showWhenTier`, en `showWhenChannels` gates wanneer De scène uses `<ImportStructureLib>`

## Ondersteunde annotatietags

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH ook ondersteunt `<BlockAnnotationTemplate>`, which applies its kind Annotaties naar iedere al-placed matching blok in De huidige scène.

## StructureLib-voorwaarden

wanneer Een scène bevat `<ImportStructureLib>`, iedere annotatie tag kan restrict its zichtbaarheid naar Een specific
StructureLib status:

| Attribuut | Betekenis |
| --- | --- |
| `showWhenStructure` | bind De annotatie naar Een named `<ImportStructureLib name="...">`; omit it wanneer De scène alleen imports one StructureLib structure |
| `showWhenTier` | tier filter such as `2`, `1..3`, `!2`, of `1..5,!3` |
| `showWhenChannels` | per-channel filter such as `input:1..3, casing:!2, fluid:4` |

Regels:

- `showWhenTier` en `showWhenChannels` are combined met logical en
- `showWhenChannels` kan mention meerdere channels in one Attribuut
- negated-alleen clauses like `!2` mean "any waarde except 2"
- De zelfde attributen are ook supported by `<PlaySound>` en `<BlockAnnotationTemplate>` kind Annotaties

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
    Alleen zichtbaar voor de geselecteerde StructureLib-status.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Markeert één blokvolume van 1x1x1.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `pos` | yes | `x y z` vector |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, of `transparent` |
| `thickness` | no | lijndikte kommagetal |
| `alwaysOnTop` | no | boolean expressie |

Example:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Markeert het controllerblok.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Markeert een willekeurige assenparallelle doos.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `min` | yes | `x y z` minimum vector |
| `max` | yes | `x y z` maximum vector |
| `color` | no | annotatie kleur |
| `thickness` | no | lijndikte kommagetal |
| `alwaysOnTop` | no | boolean expressie |

GuideNH verwisselt automatisch min/max coördinaten per axis wanneer they are provided in reverse order.

Example:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Markering over halve hoogte.
</BoxAnnotation>
````

## `<LineAnnotation>`

Draws Een lijnsegment of polylijn in wereld space.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `from` | yes, unless `points` is Stel in | `x y z` begin vector |
| `to` | yes, unless `points` is Stel in | `x y z` einde vector |
| `points` | no | Semicolon-separated `x y z` punten voor Een polylijn; overrides `from` / `to` |
| `color` | no | annotatie kleur |
| `thickness` | no | lijndikte kommagetal |
| `alwaysOnTop` | no | boolean expressie |
| `arrow` | no | `start` of `end`; omitted betekent no pijl |
| `showPoints` | no | boolean expressie; shows iedere punt as Een small cube |
| `pointColor` | no | Standaard cube kleur; omitted uses De regel kleur |
| `pointSize` | no | Standaard cube grootte; omitted uses Een waarde slightly larger than `thickness` |

`LineAnnotation` kan bevatten `<LinePoint>` kinderen naar override punt marker styling. `LinePoint`
uses `index`, optioneel `show`, optioneel `color`, en optioneel `size`. punten are zero-indexed.
Arrows kan alleen be placed on De begin of einde of De regel; intermediate polylijn punten kan niet carry
arrows.

Example:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signaalpad.
</LineAnnotation>
````

polylijn met Een 3D endpoint pijl en geselecteerd punt markers:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Signaalpad door een bocht.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places Een scherm-facing diamond marker at Een wereld positie.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `pos` | yes | `x y z` marker positie |
| `color` | no | tint kleur; omitted defaults naar bright green |

Example:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

Draws Een speech-bubble tekst label over De scène. It kan either follow Een wereldruimte anchor punt of
stay vast relative naar De scène center. Unlike De other annotatie tags, its kind inhoud is De
bubble tekst itself rather than Een zweven tooltip.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `pos` | no | `x y z` wereldruimte anchor vector |
| `x`, `y`, `z` | no | Alternative wereldruimte anchor components wanneer `pos` wordt weggelaten |
| `text` | no | Bubble tekst; kind Markdown is gebruikt wanneer omitted |
| `textKey` | no | vertaalsleutel opgelost van resource-pack `lang` files voor falling back naar `text` of kind Markdown |
| `color` | no | Bubble rand kleur; defaults naar light grey |
| `backgroundAlpha` | no | achtergrond opacity van `0` naar `255`; defaults naar `204` |
| `maxWidth` | no | Wrap breedte in pixels; `0` keeps Een enkele regel |
| `independent` | no | `true` keeps De bubble vast in scherm space |
| `yOffset` | no | Pixel offset van De scène center wanneer `independent={true}` |
| `connectorSide` | no | `bottom`, `top`, `left`, `right`, of `none`; defaults naar `bottom` |
| `connectorOffset` | no | Pixel offset along De bubble edge; positive moves right voor top/bottom en down voor left/right |
| `connectorLength` | no | Pixel length of De connector regel; defaults naar `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | no | optioneel companion markeren box bounds |
| `highlightColor` | no | optioneel markeren box kleur |

wereld-anchored bubbles tekenen Een connector regel naar their anchor. gebruiken `connectorSide` naar choose which
edge of De bubble punten at De anchor, `connectorOffset` naar move De attachment punt along that
edge, en `connectorLength` naar control De gap between De bubble en anchor. Independent bubbles
are centered horizontally in De scène en gebruiken `yOffset` voor vertical placement. They do niet tekenen a
connector. De zelfde runtime annotatie is ook gebruikt wanneer importing Ponder-animatie `text` Annotaties.

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
  Plaats hier items met **prioriteit**.
</TextAnnotation>
````

vast schermruimte example:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## Rich tooltip inhoud

annotatie kinderen are compiled as normal GuideNH inhoud, so tooltips kan bevatten:

- Markdown paragraphs en headings
- item/blok afbeeldingen
- Recepten
- nested non-interactief scènes

Example:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

gebruiken it wanneer you want naar stamp De zelfde annotatie onto iedere matching blok.

| Attribuut | Vereist | Betekenis |
| --- | --- | --- |
| `id` | yes | blok matcher in `modid:block[:meta]` form |

Regels:

- De template alleen sees blokken that al exist wanneer it is parsed
- place it na `<Block>`, `<ImportStructure>`, of `<ImportStructureLib>` tags that zou moeten feed it
- kind Annotaties gebruiken lokale coördinaten relative naar elke matched blok

Example:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Tooltip uit de sjabloon
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Gerelateerde pagina's

- [GameScene](GameScene)
- [Examples](Examples)
