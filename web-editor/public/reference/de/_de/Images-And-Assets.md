# Bilder und Assets


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH unterstützt beide normal markdown Bilder und several Laufzeit-bestimmten visual elements.

## Ressource Resolution Regeln

Leitfaden assets auflösen mit Die gleich Regeln verwendet by Seite Links.

| Pfad form | Beispiel | Bedeutung |
| --- | --- | --- |
| relativ | `test1.png` | relativ zu Die aktuell Seite Datei |
| rooted | `/assets/example_structure.snbt` | relativ zu Die aktuell Leitfaden Wurzel |
| explizit Ressource id | `guidenh:textures/gui/example.png` | absolut `modid:path` lookup |

## Markdown Bilder

normal markdown Bilder sind unterstützt:

````md
![Example](test1.png)
````

GuideNH resolves Die Pfad und lädt Die binary Ressource von Die Leitfaden Inhalt Wurzel.

## `FloatingImage`

Mit `wrap="inline"` wird das Bild vertikal zum umgebenden Text zentriert. Die Zeile wächst so weit, dass das gesamte Bild hineinpasst. Benachbarte Textzeilen behalten ihre normale Höhe; es gibt keine feste Obergrenze für die Zeilenhöhe. Soll ein sehr hohes Bild allein in einer Zeile stehen, setzen Sie das Tag in einen eigenen Absatz mit einer Leerzeile davor und danach.

`<FloatingImage>` rendert Eine cropped bitmap region dass kann Gleitkommazahl mit Text oder sit truly inline innerhalb ein
paragraph. It auch accepts explizit `modid:path` texture ids, so it kann reference texture assets von
andere mods directly.

### Attribut

| Attribut | erforderlich | Bedeutung |
| --- | --- | --- |
| `src` | ja | Bild Pfad |
| `x` | beim Zuschneiden | crop Start X in Quelle-Bild Pixel |
| `y` | beim Zuschneiden | crop Start Y in Quelle-Bild Pixel |
| `width` / `w` | beim Zuschneiden | crop Breite in Quelle-Bild Pixel; genau eins Schreibweise muss sein verwendet |
| `height` / `h` | beim Zuschneiden | crop Höhe in Quelle-Bild Pixel; genau eins Schreibweise muss sein verwendet |
| `scaleX` | nein | horizontal anzeigen Multiplikator, Standard `1.0` |
| `scaleY` | nein | vertikal anzeigen Multiplikator, Standard `1.0` |
| `displayWidth` | nein | final anzeigen Breite in Pixel; preserves Die crop aspect ratio wenn verwendet alone |
| `displayHeight` | nein | final anzeigen Höhe in Pixel; preserves Die crop aspect ratio wenn verwendet alone |
| `wrap` | nein | `inline` platziert das Bild in der Textzeile; sonst bestimmt `align` die schwebende Seite |
| `align` | nein | `left` oder `right` für floating Positionierung; ignored wenn `wrap="inline"` |
| `title` | nein | tooltip/Titel Text |
| `alt` | nein | Alternativtext |
| `sound` | nein | sound event played by Die whole Bild |
| `soundSrc` | nein | sound Datei Pfad für Die whole Bild |
| `trigger` | nein | `click` by Standard, oder `hover` für Hover playback |

### Hinweise

- `x`, `y`, `width` / `w`, und `height` / `h` sind alle erforderlich together wenn cropping
- wenn Die crop Attribut sind alle omitted, `displayWidth` oder `displayHeight` displays Die Vollständige Quelle Bild
- `width` und `height` geben den Quellausschnitt an. Die Anzeigegröße wird mit `displayWidth` / `displayHeight` oder Skalierungsfaktoren festgelegt.
- `scaleX` und `scaleY` berechnen Die final anzeigen Größe als `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` oder `displayHeight` sets Die final anzeigen Größe in Pixel; wenn nur eins ist present, Die andere dimension ist berechnet von Die crop aspect ratio
- providing beide `displayWidth` und `displayHeight` erlaubt intentional non-proportional stretching
- `displayWidth` / `displayHeight` kann nicht sein kombiniert mit `scaleX` / `scaleY`
- einzeln-axis stretching ist unterstützt by setting nur eins scale differently
- `width` mit `w`, oder `height` mit `h`, ist ungültig und rendert Eine sichtbar Fehler
- `src` kann sein relativ, rooted, oder Eine explizit `modid:path` texture id such als `minecraft:textures/gui/options_background.png`

### Beispiel

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

`<ImageAnnotation>` ist ein untergeordnetes Element element von `<FloatingImage>` dass attaches Eine rich-Text Tooltip (und
Eine optional farbig Rand) zu Eine rectangular region von Die Bild. Koordinaten sind specified in
**cropped-Bild Pixel** und sind automatisch proportionally scaled wenn Die cropped Bild ist resized
oder stretched.

### Attribut

| Attribut | erforderlich | Standard | Bedeutung |
| --- | --- | --- | --- |
| `x` | nein | — | Links edge von Die region in Bild Pixel |
| `y` | nein | — | oben edge von Die region in Bild Pixel |
| `w` | nein | — | Breite von Die region in Bild Pixel |
| `h` | nein | — | Höhe von Die region in Bild Pixel |
| `border` | nein | `false` | anzeigen Eine farbig Rand around Die region |
| `borderColor` | nein | random | Rand Farbe (`#RRGGBB` oder `#AARRGGBB`) |
| `borderThickness` | nein | `1` | Rand thickness in anzeigen Pixel |
| `sound` | nein | none | optional sound event played für dies region |
| `src` | nein | none | optional sound Datei Pfad; converted zu Eine sound event id |
| `trigger` | nein | `click` | `click` oder `hover` |

### Hinweise

- omitting alle four von `x`, `y`, `w`, `h` makes Die Annotation cover Die **whole Bild**
- Wenn beliebig von Die four ist present, Die remaining omitted ones Standard zu `0` (origin) oder `1` (Größe)
- Rand ist **nicht angezeigt by Standard**; Fügen Sie hinzu `border` oder `border={true}` zu enable it
- wenn `borderColor` ist omitted und `border` ist aktiviert, Eine random fully-opaque Farbe ist verwendet
- untergeordnetes Element MDX Inhalt ist gerendert als Die Tooltip Inhalt und kann einschließen beliebig inline/Block elements
- later Annotationen (lower in Die Liste) take Hover priority über earlier ones wenn regions overlap

### Beispiel

Whole-Bild Annotation:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Bewege den Mauszeiger über das Bild, um diesen Tooltip zu sehen.
  </ImageAnnotation>
</FloatingImage>
````

Region Annotation mit Eine sichtbar Rand:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Dies ist der Tooltip für den **hervorgehobenen Bereich**.
  </ImageAnnotation>
</FloatingImage>
````

mehrere regions auf eins Bild:

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

Bild regions kann auch play sounds. verwenden `<SoundArea>` wenn you nur need sound, oder put `sound`
directly auf `<ImageAnnotation>` wenn Die gleich region auch hat Eine Tooltip oder Rand.

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
    Dieser Bereich hat Tooltip-Inhalt und einen Klickton.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers Die whole Bild. Region sounds verwenden cropped-Bild Koordinaten
und obey Die gleich overlap priority als tooltips: later regions win.

## Inhalt Embedding und Text Wrapping

Block-Tags auf Basis von `BlockTagCompiler`, darunter `<Recipe>`, `<GameScene>` und `<BlockImage>`, akzeptieren `wrap` und `align`.

`<FloatingImage>` hat eigene Regeln: `wrap="inline"` platziert das Bild in der Textzeile; sonst bestimmt `align="left"` oder `"right"` die schwebende Seite. `<ItemImage>` ist ein Inline-Symbol; die Ausrichtung erfolgt über `<Column>` oder `<Row>`.

| Attribut | Standard | Bedeutung |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: normale Platzierung. `square`, `tight`, `through`: links oder rechts schwebend. `top-bottom`, `behind`, `front`: derzeit normale ausgerichtete Platzierung, ohne separate Textebenen. |
| `align` | `left` | `left`, `center` oder `right` im Blockkontext. Bei schwebendem Umbruch schwebt `right` rechts, alles andere links. Zum Zentrieren: `wrap="inline" align="center"`. |
| `float` | — | Älteres `left` / `right` im Fließtext, wenn `wrap` und `align` fehlen. Die expliziten Attribute werden empfohlen. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## Navigation Texture Icons

Frontmatter kann verwenden `icon_texture` zu anzeigen Eine texture stattdessen von Eine Element in navigation/Suche:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

Die Datei muss decode als Eine Bild. Die Pfad ist aufgelöst like beliebig andere Leitfaden Ressource Pfad.

## Non-Bild Assets

GuideNH pages kann auch reference non-Bild Laufzeit assets, especially structure files, für Beispiel:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

diese assets sind geladen durch Die gleich Leitfaden Ressource pipeline but sind consumed by benutzerdefiniert Tags rather als gerendert directly als Bilder.

## Best Practices

- Beibehalten Seite-lokal Bilder nahe Die Seite dass verwendet sie
- Beibehalten reusable files under Die Leitfaden Wurzel `assets/` Ordner
- prefer rooted `/assets/...` paths für shared files referenced by mehrere pages
- verwenden texture icons nur für real Bild assets

## `BlockImage`

`<BlockImage>` zeigt einen platzierten Block als transparente 3D-Vorschau und unterstützt die obigen Blockattribute für Ausrichtung und Textumfluss.

Schlüssel behavior:

- transparent Hintergrund und Rand
- nein Szene buttons, nein layer slider, nein Annotation authoring surface
- Hover weiterhin zeigt Die ausgewählt Block outline und tooltip
- `scale` Änderungen Kamera Zoom und Standards zu `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, und `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT weiterhin works, but Die
  standalone `nbt` Attribut ist easier zu lesen und ist bevorzugt

Beispiel:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## Laufzeit Beispiel Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Verwandte Seiten

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
