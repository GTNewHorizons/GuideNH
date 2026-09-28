# images et Assets


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH prend en charge les deux normal markdown images et several exécution-spécifique visual elements.

## ressource Resolution Règles

guide assets résoudre avec Le même Règles utilisé by page liens.

| chemin form | Exemple | Meaning |
| --- | --- | --- |
| relatif | `test1.png` | relatif vers Le actuel page fichier |
| rooted | `/assets/example_structure.snbt` | relatif vers Le actuel guide racine |
| explicite ressource id | `guidenh:textures/gui/example.png` | absolu `modid:path` lookup |

## Markdown images

normal markdown images sont Pris en charge:

````md
![Example](test1.png)
````

GuideNH resolves Le chemin et charge Le binary ressource depuis Le guide contenu racine.

## `FloatingImage`

`<FloatingImage>` renders Un cropped bitmap region que peut flottant avec texte ou sit truly inline dans a
paragraph. It aussi accepts explicite `modid:path` texture ids, so it peut reference texture assets depuis
autre mods directly.

### attributs

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `src` | oui | image chemin |
| `x` | oui | crop début X dans source-image pixels |
| `y` | oui | crop début Y dans source-image pixels |
| `width` / `w` | oui | crop largeur dans source-image pixels; exactement un forme doit être utilisé |
| `height` / `h` | oui | crop hauteur dans source-image pixels; exactement un forme doit être utilisé |
| `scaleX` | non | horizontal afficher multiplier, par défaut `1.0` |
| `scaleY` | non | vertical afficher multiplier, par défaut `1.0` |
| `displayWidth` | non | final afficher largeur dans pixels; preserves Le crop aspect ratio lorsque utilisé alone |
| `displayHeight` | non | final afficher hauteur dans pixels; preserves Le crop aspect ratio lorsque utilisé alone |
| `wrap` | non | `inline` pour true inline placement, otherwise utiliser Le normal wrapping modes |
| `align` | non | `left` ou `right` pour floating placement; ignored lorsque `wrap="inline"` |
| `title` | non | tooltip/titre texte |
| `sound` | non | sound event played by Le whole image |
| `soundSrc` | non | sound fichier chemin pour Le whole image |
| `trigger` | non | `click` by par défaut, ou `hover` pour survol playback |

### Remarques

- `x`, `y`, `width` / `w`, et `height` / `h` sont tous obligatoire together lorsque cropping
- lorsque Le crop attributs sont tous omitted, `displayWidth` ou `displayHeight` affiche Le complet source image
- `width` et `height` now describe Le crop rectangle, ne Le final afficher taille
- `scaleX` et `scaleY` calculer Le final afficher taille as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` ou `displayHeight` sets Le final afficher taille dans pixels; lorsque seulement un est present, Le autre dimension est calculé depuis Le crop aspect ratio
- providing les deux `displayWidth` et `displayHeight` permet intentional non-proportional stretching
- `displayWidth` / `displayHeight` ne peut pas être combined avec `scaleX` / `scaleY`
- unique-axis stretching est Pris en charge by setting seulement un scale differently
- `width` avec `w`, ou `height` avec `h`, est invalide et renders Un visible erreur
- old `FloatingImage width/height as display size` contenu est intentionally breaking et doit être migrated manuellement
- `src` peut être relatif, rooted, ou Un explicite `modid:path` texture id comme `minecraft:textures/gui/options_background.png`

### Exemple

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

`<ImageAnnotation>` est un enfant element of `<FloatingImage>` que attaches Un riche-texte tooltip (et
Un facultatif colored bordure) vers Un rectangular region of Le image. coordonnées sont specified dans
**cropped-image pixels** et sont automatiquement proportionally scaled lorsque Le cropped image est resized
ou stretched.

### attributs

| attribut | obligatoire | par défaut | Meaning |
| --- | --- | --- | --- |
| `x` | non | — | gauche edge of Le region dans image pixels |
| `y` | non | — | haut edge of Le region dans image pixels |
| `w` | non | — | largeur of Le region dans image pixels |
| `h` | non | — | hauteur of Le region dans image pixels |
| `border` | non | `false` | afficher Un colored bordure around Le region |
| `borderColor` | non | random | bordure couleur (`#RRGGBB` ou `#AARRGGBB`) |
| `borderThickness` | non | `1` | bordure thickness dans afficher pixels |
| `sound` | non | none | facultatif sound event played pour ceci region |
| `src` | non | none | facultatif sound fichier chemin; converted vers Un sound event id |
| `trigger` | non | `click` | `click` ou `hover` |

### Remarques

- omitting tous four of `x`, `y`, `w`, `h` makes Le annotation cover Le **whole image**
- Si quelconque of Le four est present, Le remaining omitted ones par défaut vers `0` (origin) ou `1` (taille)
- bordure est **ne affiché by par défaut**; Ajoutez `border` ou `border={true}` vers enable it
- lorsque `borderColor` est omitted et `border` est activé, Un random fully-opaque couleur est utilisé
- enfant MDX contenu est rendu as Le tooltip corps et peut inclure quelconque inline/bloc elements
- later annotations (lower dans Le liste) take survol priority sur earlier ones lorsque regions overlap

### Exemple

Whole-image annotation:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    Survolez l’image pour afficher cette infobulle.
  </ImageAnnotation>
</FloatingImage>
````

Region annotation avec Un visible bordure:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    Voici l’infobulle de la **zone surlignée**.
  </ImageAnnotation>
</FloatingImage>
````

plusieurs regions on un image:

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

image regions peut aussi play sounds. utiliser `<SoundArea>` lorsque you seulement need sound, ou put `sound`
directly on `<ImageAnnotation>` lorsque Le même region aussi a Un tooltip ou bordure.

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
    Cette zone contient une infobulle et un son de clic.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers Le whole image. Region sounds utiliser cropped-image coordonnées
et obey Le même overlap priority as tooltips: later regions win.

## contenu Embedding et texte Wrapping

tous bloc-level balises — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
et quelconque autre balise backed by `BlockTagCompiler` — prise en charge deux facultatif mise en page attributs que
provide Word-style contenu embedding.

| attribut | valeurs | par défaut | Meaning |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | texte-wrapping mode |
| `align` | `left` · `center` · `right` | `left` | Horizontal alignment |

### Wrap modes

| Mode | Word equivalent | bloc-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | dans ligne avec texte | par défaut stack (嵌入型) | Sits on Le texte ligne |
| `square` | Square | Document-level flottant; texte wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | même as `square` (紧密型) | même as `square` |
| `through` | à travers | même as `square` (穿越型) | même as `square` |
| `top-bottom` | haut et bas | complet-largeur slot; `align` repositions horizontally (上下型) | ligne-inline avec breaks |
| `behind` | Behind texte | Aligned inline slot; renders behind texte (衬于文字下方) | Sits on Le ligne |
| `front` | dans front of texte | Aligned inline slot; renders dans front of texte (浮于文字上方) | Sits on Le ligne |

### Alignment avec floating wrap

pour `wrap=square/tight/through`:
- `align=left` (par défaut) — bloc floats vers Le **gauche**; texte fills Le droite côté.
- `align=right` — bloc floats vers Le **droite**; texte fills Le gauche côté.
- `align=center` — bloc est centred sans floating (non texte wrapping).

### Exemples

gauche-floating image using Le new `wrap` attribut:

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

Texte de paragraphe qui s’écoule à droite de l’image…
````

droite-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

Texte qui s’écoule à gauche de la boîte de recette…
````

Centred élément image (non texte wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

droite-aligned élément image:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

élément NBT peut être supplied separately depuis Le élément id. Inline SNBT dans `id` est encore Pris en charge;
lorsque les deux forms sont present, Le standalone `nbt` attribut est merged dernier.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **Remarque** — `wrap="inline"` now gives `<FloatingImage>` true inline placement dans flow texte.
> dans inline mode, `align` est ignored à la placer of producing Un erreur.

## Navigation Texture Icons

Frontmatter peut utiliser `icon_texture` vers afficher Un texture à la placer of an élément dans navigation/recherche:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

Le fichier doit decode as Un image. Le chemin est résolu like quelconque autre guide ressource chemin.

## Non-image Assets

GuideNH pages peut aussi reference non-image exécution assets, especially structure files, pour Exemple:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

These assets sont chargé à travers Le même guide ressource pipeline but sont consumed by personnalisé balises rather que rendu directly as images.

## Best Practices

- Conservez page-local images près Le page que utilise eux
- Conservez reusable files under Le guide racine `assets/` dossier
- prefer rooted `/assets/...` paths pour shared files referenced by plusieurs pages
- utiliser texture icons seulement pour real image assets

## `BlockImage`

`<BlockImage>` utilise Le même bloc-level embedding Règles as `<FloatingImage>`, but Le visual
contenu est un transparent 3D unique-bloc aperçu à la placer of Un bitmap. It est best suited pour
showing how Un placed bloc looks dans-monde pendant encore fitting inline avec normal guide prose.

clé behavior:

- transparent arrière-plan et bordure
- non scène buttons, non layer slider, non annotation authoring surface
- survol encore affiche Le sélectionné bloc outline et tooltip
- `scale` modifications caméra zoom et defaults vers `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, et `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT encore works, but Le
  standalone `nbt` attribut est easier vers lire et est préféré

Exemple:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## exécution Exemple Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## Related Pages

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
