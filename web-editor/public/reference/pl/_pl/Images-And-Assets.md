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

`<FloatingImage>` renders cropped bitmap region który może liczba zmiennoprzecinkowa z tekst lub sit truly inline wewnątrz a
paragraph. It także accepts jawny `modid:path` texture ids, so it może reference texture zasoby z
other mods directly.

### atrybuty

| Atrybut | Wymagane | Znaczenie |
| --- | --- | --- |
| `src` | yes | obraz ścieżka |
| `x` | yes | crop początek X in source-obraz piksele |
| `y` | yes | crop początek Y in source-obraz piksele |
| `width` / `w` | yes | crop szerokość in source-obraz piksele; exactly jeden spelling musi be używany |
| `height` / `h` | yes | crop wysokość in source-obraz piksele; exactly jeden spelling musi be używany |
| `scaleX` | no | horizontal wyświetlać multiplier, Domyślne `1.0` |
| `scaleY` | no | vertical wyświetlać multiplier, Domyślne `1.0` |
| `displayWidth` | no | końcowy wyświetlać szerokość in piksele; preserves  crop aspect ratio gdy używany alone |
| `displayHeight` | no | końcowy wyświetlać wysokość in piksele; preserves  crop aspect ratio gdy używany alone |
| `wrap` | no | `inline` dla true inline placement, otherwise używać  normal wrapping modes |
| `align` | no | `left` lub `right` dla floating placement; ignored gdy `wrap="inline"` |
| `title` | no | podpowiedź/tytuł tekst |
| `sound` | no | sound event played by  whole obraz |
| `soundSrc` | no | sound ścieżka pliku dla  whole obraz |
| `trigger` | no | `click` by Domyślne, lub `hover` dla najechanie playback |

### Uwagi

- `x`, `y`, `width` / `w`, i `height` / `h` są wszystkie Wymagane together gdy cropping
- gdy  crop atrybuty są wszystkie pominięty, `displayWidth` lub `displayHeight` displays  full source obraz
- `width` i `height` now describe  crop rectangle, nie  końcowy wyświetlać rozmiar
- `scaleX` i `scaleY` obliczać  końcowy wyświetlać rozmiar as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` lub `displayHeight` sets  końcowy wyświetlać rozmiar in piksele; gdy tylko jeden is present,  other dimension is obliczony z  crop aspect ratio
- providing both `displayWidth` i `displayHeight` pozwala intentional non-proportional stretching
- `displayWidth` / `displayHeight` nie może be combined z `scaleX` / `scaleY`
- pojedynczy-oś stretching is Obsługiwane by setting tylko jeden scale differently
- `width` z `w`, lub `height` z `h`, is nieprawidłowy i renders widoczny błąd
- old `FloatingImage width/height as display size` treść is intentionally breaking i musi be migrated ręcznie
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
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    Najedź na obraz, aby zobaczyć tę podpowiedź.
  </ImageAnnotation>
</FloatingImage>
````

Region adnotacja z widoczny obramowanie:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    To podpowiedź **wyróżnionego obszaru**.
  </ImageAnnotation>
</FloatingImage>
````

wiele regions on jeden obraz:

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

obraz regions może także play sounds. używać `<SoundArea>` gdy you tylko need sound, lub put `sound`
directly on `<ImageAnnotation>` gdy  ten sam region także ma podpowiedź lub obramowanie.

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
    Ten obszar ma treść podpowiedzi i dźwięk kliknięcia.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers  whole obraz. Region sounds używać cropped-obraz współrzędne
i obey  ten sam overlap priority as tooltips: later regions win.

## Osadzanie treści i zawijanie tekstu

wszystkie blok-level tagi — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
i any other tag backed by `BlockTagCompiler` — obsługa dwa opcjonalny układ atrybuty który
zapewniać Word-style treść embedding.

| Atrybut | wartości | Domyślne | Znaczenie |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | tekst-wrapping tryb |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Tryby zawijania

| tryb | Word equivalent | blok-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | In wiersz z tekst | Domyślne stos (嵌入型) | Sits on  tekst wiersz |
| `square` | Square | Document-level liczba zmiennoprzecinkowa; tekst wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | ten sam as `square` (紧密型) | ten sam as `square` |
| `through` | Through | ten sam as `square` (穿越型) | ten sam as `square` |
| `top-bottom` | góra i dół | Full-szerokość slot; `align` repositions horizontally (上下型) | wiersz-inline z breaks |
| `behind` | Behind tekst | Aligned inline slot; renders behind tekst (衬于文字下方) | Sits on  wiersz |
| `front` | In przód of tekst | Aligned inline slot; renders in przód of tekst (浮于文字上方) | Sits on  wiersz |

### Alignment z floating zawijanie

dla `wrap=square/tight/through`:
- `align=left` (Domyślne) — blok floats do  **lewo**; tekst fills  prawo strona.
- `align=right` — blok floats do  **prawo**; tekst fills  lewo strona.
- `align=center` — blok is centred bez floating (no tekst wrapping).

### Przykłady

lewo-floating obraz używając  nowy `wrap` Atrybut:

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

Tekst akapitu płynący po prawej stronie obrazu…
````

prawo-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Tekst płynący po lewej stronie pola receptury…
````

Centred element obraz (no tekst wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

prawo-aligned element obraz:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

element NBT może be supplied separately z  element id. Inline SNBT in `id` is nadal Obsługiwane;
gdy both forms są present,  standalone `nbt` Atrybut is merged ostatni.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **Uwaga** — `wrap="inline"` now gives `<FloatingImage>` true inline placement wewnątrz flow tekst.
> In inline tryb, `align` is ignored zamiast tego of producing błąd.

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

`<BlockImage>` używa  ten sam blok-level embedding Zasady as `<FloatingImage>`, but  visual
treść is przezroczysty 3D pojedynczy-blok podgląd zamiast tego of bitmap. It is best suited dla
showing how umieszczony blok looks in-świat podczas nadal fitting inline z normal przewodnik prose.

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
