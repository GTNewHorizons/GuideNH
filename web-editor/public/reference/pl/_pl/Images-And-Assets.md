# Obrazy i zasoby

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH obsługuje both normal Markdown obrazy i several czas wykonania-określony visual elements.

## zasób Resolution Zasady

przewodnik zasoby rozwiązać z  ten sam Zasady używany by strona links.

| ścieżka form | Przykład | Znaczenie |
| --- | --- | --- |
| względny | `test1.png` | względny do  bieżący strona plik |
| rooted | `/assets/example_structure.snbt` | względny do  bieżący przewodnik katalog główny |
| jawny zasób id | `guidenh:textures/gui/example.png` | bezwzględny `modid:path` lookup |

## Markdown obrazy

Normal Markdown obrazy są Obsługiwane:

````md
![Example](test1.png)
````

GuideNH resolves  ścieżka i ładuje  binary zasób z  przewodnik treść katalog główny.

## `FloatingImage`

Przy `wrap="inline"` obraz jest wyśrodkowany w pionie względem otaczającego tekstu, a wysokość wiersza zwiększa się tak, aby zmieścić cały obraz. Sąsiednie wiersze tekstu zachowują zwykłą wysokość; nie ma stałego limitu wysokości wiersza. Jeśli bardzo wysoki obraz ma zajmować osobny wiersz, umieść znacznik w oddzielnym akapicie, z pustym wierszem przed nim i po nim.

`<FloatingImage>` renders cropped bitmap region który może liczba zmiennoprzecinkowa z tekst lub sit truly inline wewnątrz a
paragraph. It także accepts jawny `modid:path` texture ids, so it może reference texture zasoby z
other mods directly.

### atrybuty

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `src` | yes | obraz ścieżka |
| `x` | przy kadrowaniu | crop początek X in source-obraz piksele |
| `y` | przy kadrowaniu | crop początek Y in source-obraz piksele |
| `width` / `w` | przy kadrowaniu | crop szerokość in source-obraz piksele; exactly jeden spelling musi be używany |
| `height` / `h` | przy kadrowaniu | crop wysokość in source-obraz piksele; exactly jeden spelling musi be używany |
| `scaleX` | no | horizontal wyświetlać multiplier, Domyślne `1.0` |
| `scaleY` | no | vertical wyświetlać multiplier, Domyślne `1.0` |
| `displayWidth` | no | końcowy wyświetlać szerokość in piksele; preserves  crop aspect ratio gdy używany alone |
| `displayHeight` | no | końcowy wyświetlać wysokość in piksele; preserves  crop aspect ratio gdy używany alone |
| `wrap` | no | `inline` umieszcza obraz w wierszu; w pozostałych przypadkach `align` wybiera stronę opływania |
| `align` | no | `left` lub `right` dla floating placement; ignored gdy `wrap="inline"` |
| `title` | no | podpowiedź/tytuł tekst |
| `alt` | no | Tekst alternatywny |
| `sound` | no | sound event played by  whole obraz |
| `soundSrc` | no | sound ścieżka pliku dla  whole obraz |
| `trigger` | no | `click` by Domyślne, lub `hover` dla najechanie playback |

### Uwagi

- `x`, `y`, `width` / `w`, i `height` / `h` są wszystkie Wymagane together gdy cropping
- gdy  crop atrybuty są wszystkie pominięty, `displayWidth` lub `displayHeight` displays  full source obraz
- `width` i `height` określają wycinek źródła. Do rozmiaru wyświetlania użyj `displayWidth` / `displayHeight` lub skali.
- `scaleX` i `scaleY` obliczać  końcowy wyświetlać rozmiar as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` lub `displayHeight` sets  końcowy wyświetlać rozmiar in piksele; gdy tylko jeden is present,  other dimension is obliczony z  crop aspect ratio
- providing both `displayWidth` i `displayHeight` pozwala intentional non-proportional stretching
- `displayWidth` / `displayHeight` nie może be combined z `scaleX` / `scaleY`
- pojedynczy-oś stretching is Obsługiwane by setting tylko jeden scale differently
- `width` z `w`, lub `height` z `h`, is nieprawidłowy i renders widoczny błąd
- `src` może be względny, rooted, lub jawny `modid:path` texture id takie jak `minecraft:textures/gui/options_background.png`

### Przykład

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

`<ImageAnnotation>` is element podrzędny element of `<FloatingImage>` który attaches bogata-tekst podpowiedź (i
opcjonalny colored obramowanie) do rectangular region of  obraz. współrzędne są specified in
**cropped-obraz piksele** i są automatycznie proportionally scaled gdy  cropped obraz is resized
lub stretched.

### atrybuty

| Atrybut | Wymagane | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `x` | no | — | lewo krawędź of  region in obraz piksele |
| `y` | no | — | góra krawędź of  region in obraz piksele |
| `w` | no | — | szerokość of  region in obraz piksele |
| `h` | no | — | wysokość of  region in obraz piksele |
| `border` | no | `false` | pokazywać colored obramowanie around  region |
| `borderColor` | no | random | obramowanie kolor (`#RRGGBB` lub `#AARRGGBB`) |
| `borderThickness` | no | `1` | obramowanie thickness in wyświetlać piksele |
| `sound` | no | none | opcjonalny sound event played dla Ten region |
| `src` | no | none | opcjonalny sound ścieżka pliku; converted do sound event id |
| `trigger` | no | `click` | `click` lub `hover` |

### Uwagi

- omitting wszystkie four of `x`, `y`, `w`, `h` makes  adnotacja cover  **whole obraz**
- Jeśli any of  four is present,  remaining pominięty ones Domyślne do `0` (origin) lub `1` (rozmiar)
- obramowanie is **nie wyświetlany by Domyślne**; Dodaj `border` lub `border={true}` do enable it
- gdy `borderColor` is pominięty i `border` is włączony, random fully-opaque kolor is używany
- element podrzędny MDX treść is wyrenderowany as  podpowiedź body i może uwzględniać any inline/blok elements
- later Adnotacje (lower in  lista) take najechanie priority over earlier ones gdy regions overlap

### Przykład

Whole-obraz adnotacja:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Najedź na obraz, aby zobaczyć tę podpowiedź.
  </ImageAnnotation>
</FloatingImage>
````

Region adnotacja z widoczny obramowanie:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    To podpowiedź **wyróżnionego obszaru**.
  </ImageAnnotation>
</FloatingImage>
````

wiele regions on jeden obraz:

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

obraz regions może także play sounds. używać `<SoundArea>` gdy you tylko need sound, lub put `sound`
directly on `<ImageAnnotation>` gdy  ten sam region także ma podpowiedź lub obramowanie.

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
    Ten obszar ma treść podpowiedzi i dźwięk kliknięcia.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers  whole obraz. Region sounds używać cropped-obraz współrzędne
i obey  ten sam overlap priority as tooltips: later regions win.

## Osadzanie treści i zawijanie tekstu

Tagi oparte na `BlockTagCompiler`, w tym `<Recipe>`, `<GameScene>` i `<BlockImage>`, przyjmują `wrap` oraz `align`.

`<FloatingImage>` ma własne reguły: `wrap="inline"` umieszcza obraz w wierszu, a w pozostałych przypadkach `align="left"` lub `"right"` wybiera stronę opływania. `<ItemImage>` jest ikoną w tekście; do wyrównania użyj `<Column>` lub `<Row>`.

| Atrybut | Domyślnie | Znaczenie |
| --- | --- | --- |
| `wrap` | `inline` | `inline`: zwykłe umieszczenie. `square`, `tight`, `through`: opływanie z lewej lub prawej. `top-bottom`, `behind`, `front`: obecnie zwykłe wyrównane umieszczenie, bez osobnych warstw tekstu. |
| `align` | `left` | `left`, `center` lub `right` w kontekście bloku. Przy opływaniu `right` wybiera prawą stronę, inne wartości lewą. Wyśrodkowanie: `wrap="inline" align="center"`. |
| `float` | — | Starsze `left` / `right` w tekście, gdy brak `wrap` i `align`. Zalecane są jawne atrybuty. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

## Nawigacja Texture Icons

frontmatter może używać `icon_texture` do pokazywać texture zamiast tego of element in Nawigacja/wyszukiwanie:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

 plik musi decode as obraz.  ścieżka is rozwiązany like any other przewodnik zasób ścieżka.

## Non-obraz zasoby

GuideNH strony może także reference non-obraz czas wykonania zasoby, especially struktura files, dla Przykład:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

Te zasoby są załadowany through  ten sam przewodnik zasób pipeline but są consumed by niestandardowy tagi rather niż wyrenderowany directly as obrazy.

## Best Practices

- Zachowaj strona-lokalny obrazy near  strona który używa ich
- Zachowaj reusable files under  przewodnik katalog główny `assets/` folder
- prefer rooted `/assets/...` paths dla shared files referenced by wiele strony
- używać texture icons tylko dla real obraz zasoby

## `BlockImage`

`<BlockImage>` pokazuje postawiony blok jako przezroczysty podgląd 3D i obsługuje powyższe atrybuty wyrównania oraz opływania.

klucz zachowanie:

- przezroczysty tło i obramowanie
- no scena buttons, no layer slider, no adnotacja authoring surface
- najechanie nadal shows  wybrany blok outline i podpowiedź
- `scale` zmiany kamera powiększenie i domyślne do `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, i `up`
- `nbt` supplies blok-byt SNBT; inline `id="mod:block:meta:{...}"` SNBT nadal works, but 
  standalone `nbt` Atrybut is easier do czytać i is preferowany

Przykład:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## czas wykonania Przykład Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Powiązane strony

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
