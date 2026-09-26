# Annotationen


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH Szene Annotationen sind untergeordnetes Element Tags innerhalb `<GameScene>` / `<Scene>`. Sie werden im Welt space und kann enthalten untergeordnetes Element markdown/Tag Inhalt dass wird zu Eine Rich-Tooltip.

## Allgemeine Regeln

- Annotationen nur funktionieren innerhalb Eine Szene
- untergeordnetes Element Inhalt wird zu Die Tooltip Inhalt
- Annotationen kann sein verborgen mit Die Szene UI toggle
- `alwaysOnTop` zeichnet über Szene Geometrie wenn unterstützt by Die Annotation Typ
- alle Szene Annotationen auch accept optional `showWhenStructure`, `showWhenTier`, und `showWhenChannels` gates wenn Die Szene verwendet `<ImportStructureLib>`

## Unterstützte Annotation-Tags

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH auch unterstützt `<BlockAnnotationTemplate>`, das auf seine untergeordnetes Element Annotationen zu jede bereits-placed passend Block in Die aktuell Szene.

## StructureLib Bedingungen

wenn Eine Szene enthält `<ImportStructureLib>`, jede Annotation Tag kann einschränken seine Sichtbarkeit zu Eine bestimmten
StructureLib Zustand:

| Attribut | Bedeutung |
| --- | --- |
| `showWhenStructure` | bind Die Annotation zu Eine named `<ImportStructureLib name="...">`; omit it wenn Die Szene nur imports eins StructureLib structure |
| `showWhenTier` | tier Filter such als `2`, `1..3`, `!2`, oder `1..5,!3` |
| `showWhenChannels` | per-channel Filter such als `input:1..3, casing:!2, fluid:4` |

Regeln:

- `showWhenTier` und `showWhenChannels` sind kombiniert mit logischen und
- `showWhenChannels` kann nennen mehrere Kanäle in eins Attribut
- negierte-nur Klauseln like `!2` mean "beliebig Wert außer 2"
- Die gleich Attribut sind auch unterstützt by `<PlaySound>` und `<BlockAnnotationTemplate>` untergeordnetes Element Annotationen

Beispiel:

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
    Nur für den ausgewählten StructureLib-Zustand sichtbar.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Hebt ein einzelnes 1x1x1-Blockvolumen hervor.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `pos` | ja | `x y z` Vektor |
| `color` | nein | `#RRGGBB`, `#AARRGGBB`, oder `transparent` |
| `thickness` | nein | Linie thickness Gleitkommazahl |
| `alwaysOnTop` | nein | Boolesch Ausdruck |

Beispiel:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Hebt den Controller-Block hervor.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Hebt eine beliebige achsenparallele Box hervor.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `min` | ja | `x y z` minimum Vektor |
| `max` | ja | `x y z` maximum Vektor |
| `color` | nein | Annotation Farbe |
| `thickness` | nein | Linie thickness Gleitkommazahl |
| `alwaysOnTop` | nein | Boolesch Ausdruck |

GuideNH automatisch swaps min/max Koordinaten per axis wenn Sie sind bereitgestellt in reverse Reihenfolge.

Beispiel:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Hervorhebung über halbe Höhe.
</BoxAnnotation>
````

## `<LineAnnotation>`

zeichnet Eine Linie segment oder polyline in Welt space.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `from` | ja, außer `points` ist Setzen Sie | `x y z` Start Vektor |
| `to` | ja, außer `points` ist Setzen Sie | `x y z` Ende Vektor |
| `points` | nein | Semicolon-separated `x y z` Punkte für Eine polyline; überschreibt `from` / `to` |
| `color` | nein | Annotation Farbe |
| `thickness` | nein | Linie thickness Gleitkommazahl |
| `alwaysOnTop` | nein | Boolesch Ausdruck |
| `arrow` | nein | `start` oder `end`; omitted bedeutet nein Pfeil |
| `showPoints` | nein | Boolesch Ausdruck; zeigt jede Punkt als Eine small cube |
| `pointColor` | nein | Standard cube Farbe; omitted verwendet Die Linie Farbe |
| `pointSize` | nein | Standard cube Größe; omitted verwendet Eine Wert slightly larger als `thickness` |

`LineAnnotation` kann enthalten `<LinePoint>` untergeordnete Elemente zu Überschreibung Punkt marker styling. `LinePoint`
uses `index`, optional `show`, optional `color`, und optional `size`. Punkte sind zero-indexed.
Arrows kann nur sein placed auf Die Start oder Ende von Die Linie; intermediate polyline Punkte kann nicht carry
arrows.

Beispiel:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Signalpfad.
</LineAnnotation>
````

Polyline mit Eine 3D endpoint Pfeil und ausgewählt Punkt markers:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Signalpfad durch eine Biegung.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places Eine Bildschirm-facing diamond marker at Eine Welt Position.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `pos` | ja | `x y z` marker Position |
| `color` | nein | tint Farbe; omitted Standards zu bright green |

Beispiel:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

zeichnet Eine speech-bubble Text Beschriftung über Die Szene. It kann entweder follow Eine Welt-space anchor Punkt oder
stay festen relativ zu Die Szene Mitte. Unlike Die andere Annotation Tags, seine untergeordnetes Element Inhalt ist Die
bubble Text itself rather als Eine Hover tooltip.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `pos` | nein | `x y z` Welt-space anchor Vektor |
| `x`, `y`, `z` | nein | Alternative Welt-space anchor components wenn `pos` ist omitted |
| `text` | nein | Bubble Text; untergeordnetes Element markdown ist verwendet wenn omitted |
| `textKey` | nein | Übersetzung Schlüssel aufgelöst von Ressource-pack `lang` files vor falling back zu `text` oder untergeordnetes Element markdown |
| `color` | nein | Bubble Rand Farbe; Standards zu light grey |
| `backgroundAlpha` | nein | Hintergrund opacity von `0` zu `255`; Standards zu `204` |
| `maxWidth` | nein | Wrap Breite in Pixel; `0` keeps Eine einzeln Linie |
| `independent` | nein | `true` keeps Die bubble festen in Bildschirm space |
| `yOffset` | nein | Pixel offset von Die Szene Mitte wenn `independent={true}` |
| `connectorSide` | nein | `bottom`, `top`, `left`, `right`, oder `none`; Standards zu `bottom` |
| `connectorOffset` | nein | Pixel offset along Die bubble edge; positive moves rechts für oben/unten und down für Links/rechts |
| `connectorLength` | nein | Pixel length von Die connector Linie; Standards zu `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | nein | optional companion hervorheben box bounds |
| `highlightColor` | nein | optional hervorheben box Farbe |

Welt-anchored bubbles zeichnen Eine connector Linie zu ihre anchor. verwenden `connectorSide` zu auswählen which
edge von Die bubble Punkte at Die anchor, `connectorOffset` zu move Die attachment Punkt along dass
edge, und `connectorLength` zu control Die gap zwischen Die bubble und anchor. Independent bubbles
sind centered horizontal in Die Szene und verwenden `yOffset` für vertikal Positionierung. Sie do nicht zeichnen ein
connector. Die gleich Laufzeit Annotation ist auch verwendet wenn importing Ponder `text` Annotationen.

Beispiel:

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
  Gegenstände mit **Priorität** hier einfügen.
</TextAnnotation>
````

festen Bildschirm-space Beispiel:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## Rich-Tooltip Inhalt

Annotation untergeordnete Elemente sind kompiliert als normal GuideNH Inhalt, so tooltips kann enthalten:

- markdown paragraphs und headings
- Element/Block Bilder
- recipes
- nested non-interactive Szenen

Beispiel:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

verwenden it wenn you want zu stamp Die gleich Annotation onto jede passend Block.

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `id` | ja | Block matcher in `modid:block[:meta]` form |

Regeln:

- Die template nur sees Blöcke dass bereits exist wenn it ist geparst
- platzieren it nach `<Block>`, `<ImportStructure>`, oder `<ImportStructureLib>` Tags dass sollte feed it
- untergeordnetes Element Annotationen verwenden lokal Koordinaten relativ zu jede passend Block

Beispiel:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Von der Vorlage erzeugter Tooltip
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Verwandte Seiten

- [GameScene](GameScene)
- [Examples](Examples)
