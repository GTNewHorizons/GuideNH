# Adnotacje

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH scena Adnotacje są element podrzędny tagi wewnątrz `<GameScene>` / `<Scene>`. jeden renderować in świat przestrzeń i może zawierać element podrzędny Markdown/tag treść który staje się bogata podpowiedź.

## Ogólne zasady

- Adnotacje tylko działa wewnątrz scena
- element podrzędny treść staje się  podpowiedź body
- Adnotacje może be ukryty z  scena UI toggle
- `alwaysOnTop` rysuje nad scena geometrią gdy Obsługiwane by  adnotacja Typ
- wszystkie scena Adnotacje także akceptują opcjonalny `showWhenStructure`, `showWhenTier`, i `showWhenChannels` warunki gdy  scena używa `<ImportStructureLib>`

## Obsługiwane tagi adnotacji

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH także obsługuje `<BlockAnnotationTemplate>`, który stosuje jego element podrzędny Adnotacje do każdy już-umieszczony pasujący blok in  bieżący scena.

## Warunki StructureLib

gdy scena zawiera `<ImportStructureLib>`, każdy adnotacja tag może restrict jego widoczność do określony
StructureLib stan:

| Atrybut | Znaczenie |
| --- | --- |
| `showWhenStructure` | bind  adnotacja do named `<ImportStructureLib name="...">`; omit it gdy  scena tylko importy jeden StructureLib struktura |
| `showWhenTier` | tier filtr takie jak `2`, `1..3`, `!2`, lub `1..5,!3` |
| `showWhenChannels` | per-channel filtr takie jak `input:1..3, casing:!2, fluid:4` |

Zasady:

- `showWhenTier` i `showWhenChannels` są combined z logical i
- `showWhenChannels` może mention wiele kanały in jeden Atrybut
- negated-tylko clauses like `!2` mean "any wartość except 2"
-  ten sam atrybuty są także Obsługiwane by `<PlaySound>` i `<BlockAnnotationTemplate>` element podrzędny Adnotacje

Przykład:

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
    Widoczne tylko dla wybranego stanu StructureLib.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Wyróżnia pojedynczą objętość bloku 1x1x1.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `pos` | yes | `x y z` wektor |
| `color` | no | `#RRGGBB`, `#AARRGGBB`, lub `transparent` |
| `thickness` | no | grubość linii liczba zmiennoprzecinkowa |
| `alwaysOnTop` | no | wartość logiczna wyrażenie |

Przykład:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Wyróżnia blok kontrolera.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Wyróżnia dowolne pudełko ustawione wzdłuż osi.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `min` | yes | `x y z` minimum wektor |
| `max` | yes | `x y z` maximum wektor |
| `color` | no | adnotacja kolor |
| `thickness` | no | grubość linii liczba zmiennoprzecinkowa |
| `alwaysOnTop` | no | wartość logiczna wyrażenie |

GuideNH automatycznie swaps min/max współrzędne per oś gdy jeden są provided in reverse kolejność.

Przykład:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Podświetlenie obszaru o połowie wysokości.
</BoxAnnotation>
````

## `<LineAnnotation>`

rysuje odcinek lub polilinia in świat przestrzeń.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `from` | yes, unless `points` is Ustaw | `x y z` początek wektor |
| `to` | yes, unless `points` is Ustaw | `x y z` koniec wektor |
| `points` | no | Semicolon-separated `x y z` punkty dla polilinia; overrides `from` / `to` |
| `color` | no | adnotacja kolor |
| `thickness` | no | grubość linii liczba zmiennoprzecinkowa |
| `alwaysOnTop` | no | wartość logiczna wyrażenie |
| `arrow` | no | `start` lub `end`; pominięty oznacza no strzałka |
| `showPoints` | no | wartość logiczna wyrażenie; shows każdy punkt as small cube |
| `pointColor` | no | Domyślne cube kolor; pominięty używa  wiersz kolor |
| `pointSize` | no | Domyślne cube rozmiar; pominięty używa wartość slightly larger niż `thickness` |

`LineAnnotation` może zawierać `<LinePoint>` elementy podrzędne do nadpisywać punkt marker styling. `LinePoint`
używa `index`, opcjonalny `show`, opcjonalny `color`, i opcjonalny `size`. punkty są zero-indexed.
Arrows może tylko be umieszczony on  początek lub koniec of  wiersz; intermediate polilinia punkty nie może carry
arrows.

Przykład:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Ścieżka sygnału.
</LineAnnotation>
````

polilinia z 3D endpoint strzałka i wybrany punkt znaczniki:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Ścieżka sygnału przez zakręt.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places ekran-facing diamond marker at a świat pozycja.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `pos` | yes | `x y z` marker pozycja |
| `color` | no | tint kolor; pominięty domyślne do bright green |

Przykład:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

rysuje speech-dymek tekst etykieta over  scena. It może either follow przestrzeń świata kotwica punkt lub
pozostają stały względny do  scena środek. Unlike  other adnotacja tagi, jego element podrzędny treść is 
dymek tekst itself rather niż najechanie podpowiedź.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `pos` | no | `x y z` przestrzeń świata kotwica wektor |
| `x`, `y`, `z` | no | Alternative przestrzeń świata kotwica components gdy `pos` is pominięty |
| `text` | no | dymek tekst; element podrzędny Markdown is używany gdy pominięty |
| `textKey` | no | klucz tłumaczenia rozwiązany z zasób-pack `lang` files przed falling tył do `text` lub element podrzędny Markdown |
| `color` | no | dymek obramowanie kolor; domyślne do light grey |
| `backgroundAlpha` | no | tło opacity z `0` do `255`; domyślne do `204` |
| `maxWidth` | no | zawijanie szerokość in piksele; `0` zachowuje pojedynczy wiersz |
| `independent` | no | `true` zachowuje  dymek stały in ekran przestrzeń |
| `yOffset` | no | piksel offset z  scena środek gdy `independent={true}` |
| `connectorSide` | no | `bottom`, `top`, `left`, `right`, lub `none`; domyślne do `bottom` |
| `connectorOffset` | no | piksel offset wzdłuż  dymek krawędź; positive moves prawo dla góra/dół i down dla lewo/prawo |
| `connectorLength` | no | piksel length of  łącznik wiersz; domyślne do `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | no | opcjonalny companion wyróżniać box granice |
| `highlightColor` | no | opcjonalny wyróżniać box kolor |

świat-anchored bubbles rysować łącznik wiersz do ich kotwica. używać `connectorSide` do wybrać który
krawędź of  dymek punkty at  kotwica, `connectorOffset` do move  attachment punkt wzdłuż który
krawędź, i `connectorLength` do control  gap between  dymek i kotwica. Independent bubbles
są centered horizontally in  scena i używać `yOffset` dla vertical placement. jeden do nie rysować a
łącznik.  ten sam czas wykonania adnotacja is także używany gdy importing Animacja Ponder `text` Adnotacje.

Przykład:

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
  Wstawiaj tutaj przedmioty z **priorytetem**.
</TextAnnotation>
````

stały przestrzeń ekranu Przykład:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## bogata podpowiedź treść

adnotacja elementy podrzędne są skompilowany as normal GuideNH treść, so tooltips może zawierać:

- Markdown paragraphs i headings
- element/blok obrazy
- Receptury
- nested non-interaktywny sceny

Przykład:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

używać it gdy you want do stamp  ten sam adnotacja onto każdy pasujący blok.

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `id` | yes | blok matcher in `modid:block[:meta]` form |

Zasady:

-  template tylko sees bloki który już exist gdy it is przeanalizowany
- place it po `<Block>`, `<ImportStructure>`, lub `<ImportStructureLib>` tagi który powinien feed it
- element podrzędny Adnotacje używać lokalny współrzędne względny do każdy pasujący blok

Przykład:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Tooltip wygenerowany przez szablon
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Powiązane strony

- [GameScene](GameScene)
- [Examples](Examples)
