# Lokalisatie

Deze documentatie beschrijft de GuideNH-syntaxis en runtimefuncties. Code, tags, paden, ID's en attribuutwaarden blijven ongewijzigd.


GuideNH ondersteunt gelokaliseerd gids pages en gelokaliseerd gids assets.

## Mapindeling

runtime-lokalisatie is map-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

De repository example resourcepack keeps its documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

taal folders are recognized alleen in De underscored form. Plain folders such as `en_us/` en `zh_cn/` are no longer treated as Lokalisatie roots.

## Volgorde voor pagina-opzoeking

Voor elke requested pagina-ID, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Als De huidige taal pagina is missing
3. `<page>` zonder Een taalmap

gids pages alleen fall back naar De gids's `defaultLanguage`. Auto-discovered resource-pack gidsen still Standaard that waarde naar `en_us`, so another taal is niet promoted into Een fallbacktaal just because it exists.

## pagina Lang Key Overrides

gids pages kan ook vervangen their full Markdown-bron van a `.lang` key, but alleen wanneer De physical pagina bestand
al exists. De bestand remains De existence gate en De fallback source.

- GuideNH first resolves De pagina bestand met De normal taal fallback order
- na Een bestand has been found, GuideNH looks voor Een pagina-gelokaliseerd `.lang` waarde voor De requested taal
- Als that key exists en is non-empty, its full waarde becomes De pagina Markdown-bron voor parsing
- Als De key is missing of empty, GuideNH falls back naar De opgelost bestand inhoud

wanneer Een non-empty `.lang` pagina waarde is gebruikt, GuideNH merges missing frontmatter fields van De opgelost physical
pagina voor parsing. gelokaliseerd frontmatter always wins voor fields it explicitly defines, so translated
`navigation.title` waarden stay gelokaliseerd, terwijl newer structural fields such as `navigation.recommend`,
`navigation.priority`, categories, item links, authorship metadata, of pagina zoomen kan be inherited van De
fallback `.md` bestand. Deze keeps older full-pagina translations van accidentally dropping home-pagina recommendations
wanneer De base pagina metadata is bijgewerkt.

De key format is:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Example:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

pad separators become `.` in De key. Literal dots binnen Een pad segment are escaped so they do niet collide met
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

Other non-alphanumeric characters are escaped met De zelfde `_x<hex>_` pattern.

binnen De `.lang` waarde, literal `\n` en `\r` sequences are converted naar real regel endings voor Markdown parsing,
so De vertaling waarde kan bevatten Een full pagina including frontmatter, headings, lists, en MDX tags.

Authoring Regels:

- write De pagina as one physical `.lang` regel voor that key
- gebruiken literal `\n` binnen De waarde wanneer you want Een Markdown regel break
- do niet insert real regel breaks into De `.lang` waarde itself, because Forge reads `.lang` files regel by regel
- do niet write `\\n` unless you intentionally want De final Markdown-bron naar bevatten De literal characters `\n`

GuideNH doet niet synthesize pages van `.lang` alone. Een real pagina bestand moet still exist.

## Key Length

GuideNH doet niet impose Een extra character limit on Deze pagina keys. On Minecraft 1.7.10 / Forge, De backing taal
data is effectively Een tekenreeks-eigenschap map, so De practical limits are normal memory usage en maintainability rather
than Een dedicated hard cap. Shorter pagina paths still make keys easier naar author en review.

## Advies voor auteurs

- Stel in `defaultLanguage` deliberately wanneer you want Een non-English fallbacktaal voor Een gids
- Voeg toe Een shared taal-neutral pagina alleen wanneer cross-taal fallback is actually intended
- translate pages first, then translate assets alleen wanneer tekst is embedded in De asset
- avoid taal-specific asset filenames wanneer Een rooted shared asset would do

## Volgorde voor asset-opzoeking

gids assets gebruiken Een slightly richer fallback order:

1. `_<current language>/<path>`
2. Als De huidige taal is niet De gids standaardtaal, `_<default language>/<path>`
3. `<path>`

Deze makes it possible naar localize afbeeldingen of texture-like assets wanneer needed.

## Zoeken en taal

zoeken documents store both De raw Minecraft taal en De analyzer taal gebruikt voor Lucene. Als De huidige Minecraft taal is niet mapped naar Een known analyzer, zoeken falls back naar English tokenization.

## Ignore vertaling Config

GuideNH doet niet expose Een globale "ignore translations" switch. Als you want Een gids naar fall back naar Een non-English taal, Stel in that gids's `defaultLanguage` explicitly in code.

## Example

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository example pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Gerelateerde pagina's

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
