# Lokalizacja

Ta dokumentacja opisuje składnię i funkcje czasu wykonania GuideNH. Kod, tagi, ścieżki, identyfikatory i wartości atrybutów pozostają bez zmian.


GuideNH obsługuje zlokalizowany przewodnik strony i zlokalizowany przewodnik zasoby.

## Układ folderów

lokalizacja czasu wykonania is folder-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

 repository Przykład pakiet zasobów zachowuje jego documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

język folders są recognized tylko in  underscored form. Plain folders takie jak `en_us/` i `zh_cn/` są no longer treated as Lokalizacja roots.

## Kolejność wyszukiwania stron

Dla każdego requested ID strony, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Jeśli  bieżący język strona is missing
3. `<page>` bez folder języka

przewodnik strony tylko fall tył do  przewodnik's `defaultLanguage`. Auto-discovered zasób-pack przewodniki nadal Domyślne który wartość do `en_us`, so another język is nie promoted do język awaryjny just because it exists.

## strona Lang klucz Overrides

przewodnik strony może także zastąpić ich full źródło Markdown z a `.lang` klucz, but tylko gdy  physical strona plik
już exists.  plik remains  existence gate i  awaryjny source.

- GuideNH pierwszy resolves  strona plik z  normal język awaryjny kolejność
- po plik ma been found, GuideNH looks dla strona-zlokalizowany `.lang` wartość dla  requested język
- Jeśli który klucz exists i is non-empty, jego full wartość staje się  strona źródło Markdown przed parsing
- Jeśli  klucz is missing lub empty, GuideNH falls tył do  rozwiązany plik treść

gdy non-empty `.lang` strona wartość is używany, GuideNH merges missing frontmatter pola z  rozwiązany physical
strona przed parsing. zlokalizowany frontmatter zawsze wins dla pola it explicitly defines, so translated
`navigation.title` wartości pozostają zlokalizowany, podczas newer structural pola takie jak `navigation.recommend`,
`navigation.priority`, categories, element links, authorship metadane, lub strona powiększenie może be inherited z 
awaryjny `.md` plik. Ten zachowuje older full-strona translations z accidentally dropping home-strona recommendations
gdy  base strona metadane is zaktualizowany.

 klucz format is:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Przykład:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

ścieżka separators become `.` in  klucz. Literal dots wewnątrz a ścieżka segment są escaped so jeden do nie collide z
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

Other non-alphanumeric characters są escaped z  ten sam `_x<hex>_` pattern.

wewnątrz  `.lang` wartość, literal `\n` i `\r` sequences są converted do real wiersz endings przed Markdown parsing,
so  tłumaczenie wartość może zawierać full strona including frontmatter, headings, lists, i MDX tagi.

Authoring Zasady:

- zapisywać  strona as jeden physical `.lang` wiersz dla który klucz
- używać literal `\n` wewnątrz  wartość gdy you want Markdown wiersz break
- do nie insert real wiersz breaks do  `.lang` wartość itself, because Forge reads `.lang` files wiersz by wiersz
- do nie zapisywać `\\n` unless you intentionally want  końcowy źródło Markdown do zawierać  literal characters `\n`

GuideNH nie synthesize strony z `.lang` alone. real strona plik musi nadal exist.

## klucz Length

GuideNH nie impose extra character limit on Te strona klucze. On Minecraft 1.7.10 / Forge,  backing język
dane is effectively ciąg znaków-właściwość map, so  practical limits są normal memory usage i maintainability rather
niż dedicated hard cap. Shorter strona paths nadal make klucze easier do autor i review.

## Wskazówki dotyczące tworzenia

- Ustaw `defaultLanguage` deliberately gdy you want non-English język awaryjny dla przewodnik
- Dodaj shared język-neutral strona tylko gdy cross-język awaryjny is actually intended
- translate strony pierwszy, następnie translate zasoby tylko gdy tekst is embedded in  zasób
- avoid język-określony zasób filenames gdy rooted shared zasób would do

## Kolejność wyszukiwania zasobów

przewodnik zasoby używać slightly richer awaryjny kolejność:

1. `_<current language>/<path>`
2. Jeśli  bieżący język is nie  przewodnik język domyślny, `_<default language>/<path>`
3. `<path>`

Ten makes it possible do localize obrazy lub texture-like zasoby gdy needed.

## Wyszukiwanie i język

wyszukiwanie documents store both  raw Minecraft język i  analyzer język używany dla Lucene. Jeśli  bieżący Minecraft język is nie mapped do known analyzer, wyszukiwanie falls tył do English tokenization.

## Ignore tłumaczenie Config

GuideNH nie expose globalny "ignore translations" switch. Jeśli you want przewodnik do fall tył do non-English język, Ustaw który przewodnik's `defaultLanguage` explicitly in code.

## Przykład

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Przykład pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Powiązane strony

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
