# Localización


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH admite localizado guíUn páginas y localizado guíUn recursos.

## carpeta diseño

ejecución localization es carpeta-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

El repository Ejemplo recurso pack mantiene su documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

idioma folders son recognized solo en El underscored form. simple folders como `en_us/` y `zh_cn/` son no longer treated as localization roots.

## página Lookup orden

para cada requested página id, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Si El actual idioma página es faltante
3. `<page>` sin Un idioma carpeta

guíUn páginas solo fall atrás Un El guía's `defaultLanguage`. Auto-discovered recurso-pack guías todavía predeterminado que valor a `en_us`, so another idioma es no promoted en Un reserva idioma just because it exists.

## página Lang clave anula

guíUn páginas puede también reemplazar sus completo markdown origen de a `.lang` clave, but solo cuando El physical página archivo
ya exists. El archivo remains El existence gate y El reserva origen.

- GuideNH primero resolves El página archivo con El normal idioma reserva orden
- después de Un archivo tiene been found, GuideNH looks para Un página-localizado `.lang` valor para El requested idioma
- Si que clave exists y es non-vacío, su completo valor se convierte en El página markdown origen antes de parsing
- Si El clave es faltante o vacío, GuideNH falls atrás Un El resuelto archivo contenido

cuando Un non-vacío `.lang` página valor es usado, GuideNH merges faltante frontmatter campos de El resuelto physical
página antes de parsing. localizado frontmatter siempre wins para campos it explicitly defines, so translated
`navigation.title` valores permanecer localizado, mientras newer structural campos como `navigation.recommend`,
`navigation.priority`, categories, elemento enlaces, authorship metadatos, o página zoom puede ser inherited de El
reserva `.md` archivo. esto mantiene older completo-página translations de accidentally dropping home-página recommendations
cuando El base página metadatos es actualizado.

El clave format es:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Ejemplo:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

ruta separators become `.` en El clave. Literal dots dentro de Un ruta segment son escaped so Estas do no collide con
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

otro non-alphanumeric characters son escaped con El mismo `_x<hex>_` pattern.

dentro de El `.lang` valor, literal `\n` y `\r` sequences son converted Un real línea endings antes de markdown parsing,
so El traducción valor puede contener Un completo página including frontmatter, headings, lists, y MDX etiquetas.

Authoring Reglas:

- escribir El página as uno physical `.lang` línea para que clave
- usar literal `\n` dentro de El valor cuando you want Un markdown línea break
- do no insert real línea breaks en El `.lang` valor itself, because Forge reads `.lang` files línea by línea
- do no escribir `\\n` salvo you intentionally want El final markdown origen Un contener El literal characters `\n`

GuideNH hace no synthesize páginas de `.lang` alone. Un real página archivo debe todavía exist.

## clave Length

GuideNH hace no impose Un extra character limit on these página claves. On Minecraft 1.7.10 / Forge, El backing idioma
datos es effectively Un cadena-propiedad map, so El practical limits son normal memory usage y maintainability rather
que Un dedicated hard cap. Shorter página paths todavía hacer claves easier Un autor y review.

## Authoring Advice

- Establezca `defaultLanguage` deliberately cuando you want Un non-English reserva idioma para Un guía
- Añada Un shared idioma-neutral página solo cuando cross-idioma reserva es actually intended
- translate páginas primero, entonces translate recursos solo cuando texto es embedded en El recurso
- avoid idioma-específico recurso filenames cuando Un rooted shared recurso would do

## recurso Lookup orden

guíUn recursos usar Un slightly richer reserva orden:

1. `_<current language>/<path>`
2. Si El actual idioma es no El guíUn predeterminado idioma, `_<default language>/<path>`
3. `<path>`

esto makes it possible Un localize imágenes o texture-like recursos cuando needed.

## búsqueda y idioma

búsqueda documents store ambos El sin procesar Minecraft idioma y El analyzer idioma usado para Lucene. Si El actual Minecraft idioma es no mapped Un Un known analyzer, búsqueda falls atrás Un English tokenization.

## Ignore traducción Config

GuideNH hace no expose Un global "ignore translations" switch. Si you want Un guíUn Un fall atrás Un Un non-English idioma, Establezca que guía's `defaultLanguage` explicitly en code.

## Ejemplo

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Ejemplo pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Related páginas

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
