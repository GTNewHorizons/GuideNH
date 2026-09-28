# Lokalisierung


Diese Dokumentation beschreibt die GuideNH-Syntax und Laufzeitfunktionen. Code, Tags, Pfade, IDs und Attributwerte bleiben unverändert.

GuideNH unterstützt lokalisiert Leitfaden pages und lokalisiert Leitfaden assets.

## Ordner Layout

Laufzeit localization ist Ordner-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

Die repository Beispiel Ressource pack keeps seine documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

Sprache folders sind recognized nur in Die underscored form. einfach folders such als `en_us/` und `zh_cn/` sind nein longer treated als localization roots.

## Seite Lookup Reihenfolge

für jede requested Seite id, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Wenn Die aktuell Sprache Seite ist fehlend
3. `<page>` ohne Eine Sprache Ordner

Leitfaden pages nur fall back zu Die Leitfaden's `defaultLanguage`. Auto-discovered Ressource-pack Guides weiterhin Standard dass Wert zu `en_us`, so another Sprache ist nicht promoted in Eine Fallback Sprache just becaVerwenden Sie it exists.

## Seite Lang Schlüssel überschreibt

Leitfaden pages kann auch ersetzen ihre Vollständige markdown Quelle von ein `.lang` Schlüssel, but nur wenn Die physical Seite Datei
bereits exists. Die Datei remains Die existence gate und Die Fallback Quelle.

- GuideNH erste resolves Die Seite Datei mit Die normal Sprache Fallback Reihenfolge
- nach Eine Datei hat been found, GuideNH looks für Eine Seite-lokalisiert `.lang` Wert für Die requested Sprache
- Wenn dass Schlüssel exists und ist non-leer, seine Vollständige Wert wird zu Die Seite markdown Quelle vor parsing
- Wenn Die Schlüssel ist fehlend oder leer, GuideNH falls back zu Die aufgelöst Datei Inhalt

wenn Eine non-leer `.lang` Seite Wert ist verwendet, GuideNH merges fehlend frontmatter Felder von Die aufgelöst physical
Seite vor parsing. lokalisiert frontmatter immer wins für Felder it explicitly defines, so translated
`navigation.title` Werte stay lokalisiert, während newer structural Felder such als `navigation.recommend`,
`navigation.priority`, categories, Element Links, authorship metadata, oder Seite Zoom kann sein inherited von Die
Fallback `.md` Datei. dies keeps older Vollständige-Seite translations von accidentally dropping home-Seite recommendations
wenn Die base Seite metadata ist aktualisiert.

Die Schlüssel format ist:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Beispiel:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

Pfad separators werden zu `.` in Die Schlüssel. Literal dots innerhalb Eine Pfad segment sind escaped so Sie do nicht collide mit
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

andere non-alphanumeric characters sind escaped mit Die gleich `_x<hex>_` pattern.

innerhalb Die `.lang` Wert, literal `\n` und `\r` sequences sind converted zu real Linie endings vor markdown parsing,
so Die Übersetzung Wert kann enthalten Eine Vollständige Seite including frontmatter, headings, lists, und MDX Tags.

Authoring Regeln:

- schreiben Die Seite als eins physical `.lang` Linie für dass Schlüssel
- verwenden literal `\n` innerhalb Die Wert wenn you want Eine markdown Linie break
- do nicht insert real Linie breaks in Die `.lang` Wert itself, becaVerwenden Sie Forge reads `.lang` files Linie by Linie
- do nicht schreiben `\\n` außer you intentionally want Die final markdown Quelle zu enthalten Die literal characters `\n`

GuideNH tut nicht synthesize pages von `.lang` alone. Eine real Seite Datei muss weiterhin exist.

## Schlüssel Length

GuideNH tut nicht impose Eine extra character limit auf diese Seite Schlüssel. auf Minecraft 1.7.10 / Forge, Die backing Sprache
Daten ist effectively Eine Zeichenfolge-Eigenschaft map, so Die practical limits sind normal memory usage und maintainability rather
als Eine dedicated hard cap. Shorter Seite paths weiterhin make Schlüssel easier zu author und review.

## Hinweise zur Erstellung

- Setzen Sie `defaultLanguage` deliberately wenn you want Eine non-English Fallback Sprache für Eine Leitfaden
- Fügen Sie hinzu Eine shared Sprache-neutral Seite nur wenn cross-Sprache Fallback ist actually intended
- translate pages erste, dann translate assets nur wenn Text ist embedded in Die Asset
- avoid Sprache-bestimmten Ressource filenames wenn Eine rooted shared Ressource would do

## Reihenfolge der Ressourcensuche

Leitfaden assets verwenden Eine slightly richer Fallback Reihenfolge:

1. `_<current language>/<path>`
2. Wenn Die aktuell Sprache ist nicht Die Leitfaden Standard Sprache, `_<default language>/<path>`
3. `<path>`

dies makes it possible zu localize Bilder oder texture-like assets wenn needed.

## Suche und Sprache

Suche documents store beide Die roh Minecraft Sprache und Die analyzer Sprache verwendet für Lucene. Wenn Die aktuell Minecraft Sprache ist nicht mapped zu Eine known analyzer, Suche falls back zu English tokenization.

## Ignore Übersetzung Config

GuideNH tut nicht expose Eine globale "ignore translations" switch. Wenn you want Eine Leitfaden zu fall back zu Eine non-English Sprache, Setzen Sie dass Leitfaden's `defaultLanguage` explicitly in code.

## Beispiel

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Beispiel pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Verwandte Seiten

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
