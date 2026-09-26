# Localisation


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH prend en charge localisé guide pages et localisé guide assets.

## dossier mise en page

exécution localization est dossier-based:

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

Le repository Exemple ressource pack keeps son documented outer `assets/` layer:

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

langue folders sont recognized seulement dans Le underscored form. simple folders comme `en_us/` et `zh_cn/` sont non longer treated as localization roots.

## page Lookup ordre

pour chaque requested page id, GuideNH tries:

1. `_<current language>/<page>`
2. `_<default language>/<page>` Si Le actuel langue page est manquant
3. `<page>` sans Un langue dossier

guide pages seulement fall back vers Le guide's `defaultLanguage`. Auto-discovered ressource-pack guides encore par défaut que valeur vers `en_us`, so another langue est ne promoted dans Un repli langue just because it exists.

## page Lang clé remplace

guide pages peut aussi remplacer leurs complet markdown source depuis a `.lang` clé, but seulement lorsque Le physical page fichier
déjà exists. Le fichier remains Le existence gate et Le repli source.

- GuideNH premier resolves Le page fichier avec Le normal langue repli ordre
- après Un fichier a been found, GuideNH looks pour Un page-localisé `.lang` valeur pour Le requested langue
- Si que clé exists et est non-vide, son complet valeur devient Le page markdown source avant parsing
- Si Le clé est manquant ou vide, GuideNH falls back vers Le résolu fichier contenu

lorsque Un non-vide `.lang` page valeur est utilisé, GuideNH merges manquant frontmatter champs depuis Le résolu physical
page avant parsing. localisé frontmatter toujours wins pour champs it explicitly defines, so translated
`navigation.title` valeurs stay localisé, pendant newer structural champs comme `navigation.recommend`,
`navigation.priority`, categories, élément liens, authorship metadata, ou page zoom peut être inherited depuis Le
repli `.md` fichier. ceci keeps older complet-page translations depuis accidentally dropping home-page recommendations
lorsque Le base page metadata est mis à jour.

Le clé format est:

```text
guidenh.page.<namespace>.<folder>.<page path without .md>
```

Exemple:

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

chemin separators become `.` dans Le clé. Literal dots dans Un chemin segment sont escaped so Elles do ne collide avec
segment separators:

```text
foo.bar.md -> foo_x2e_bar
```

autre non-alphanumeric characters sont escaped avec Le même `_x<hex>_` pattern.

dans Le `.lang` valeur, literal `\n` et `\r` sequences sont converted vers real ligne endings avant markdown parsing,
so Le traduction valeur peut contenir Un complet page including frontmatter, headings, lists, et MDX balises.

Authoring Règles:

- écrire Le page as un physical `.lang` ligne pour que clé
- utiliser literal `\n` dans Le valeur lorsque you want Un markdown ligne break
- do ne insert real ligne breaks dans Le `.lang` valeur itself, because Forge reads `.lang` files ligne by ligne
- do ne écrire `\\n` sauf you intentionally want Le final markdown source vers contenir Le literal characters `\n`

GuideNH fait ne synthesize pages depuis `.lang` alone. Un real page fichier doit encore exist.

## clé Length

GuideNH fait ne impose Un extra character limit on these page clés. On Minecraft 1.7.10 / Forge, Le backing langue
données est effectively Un chaîne-propriété map, so Le practical limits sont normal memory usage et maintainability rather
que Un dedicated hard cap. Shorter page paths encore faire clés easier vers author et review.

## Authoring Advice

- Définissez `defaultLanguage` deliberately lorsque you want Un non-English repli langue pour Un guide
- Ajoutez Un shared langue-neutral page seulement lorsque cross-langue repli est actually intended
- translate pages premier, alors translate assets seulement lorsque texte est embedded dans Le ressource
- avoid langue-spécifique ressource filenames lorsque Un rooted shared ressource would do

## ressource Lookup ordre

guide assets utiliser Un slightly richer repli ordre:

1. `_<current language>/<path>`
2. Si Le actuel langue est ne Le guide par défaut langue, `_<default language>/<path>`
3. `<path>`

ceci makes it possible vers localize images ou texture-like assets lorsque needed.

## recherche et langue

recherche documents store les deux Le brut Minecraft langue et Le analyzer langue utilisé pour Lucene. Si Le actuel Minecraft langue est ne mapped vers Un known analyzer, recherche falls back vers English tokenization.

## Ignore traduction Config

GuideNH fait ne expose Un global "ignore translations" switch. Si you want Un guide vers fall back vers Un non-English langue, Définissez que guide's `defaultLanguage` explicitly dans code.

## Exemple

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

Repository Exemple pack equivalent:

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Images And Assets](Images-And-Assets)
