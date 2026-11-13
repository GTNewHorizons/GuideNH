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

Avec `wrap="inline"`, l’image est centrée verticalement par rapport au texte environnant et la ligne s’agrandit pour contenir l’image entière. Les lignes de texte voisines conservent leur hauteur habituelle ; aucune hauteur maximale fixe n’est imposée. Pour qu’une image très haute occupe une ligne à elle seule, placez la balise dans un paragraphe distinct, précédé et suivi d’une ligne vide.

`<FloatingImage>` renders Un cropped bitmap region que peut flottant avec texte ou sit truly inline dans a
paragraph. It aussi accepts explicite `modid:path` texture ids, so it peut reference texture assets depuis
autre mods directly.

### attributs

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `src` | oui | image chemin |
| `x` | lors du recadrage | crop début X dans source-image pixels |
| `y` | lors du recadrage | crop début Y dans source-image pixels |
| `width` / `w` | lors du recadrage | crop largeur dans source-image pixels; exactement un forme doit être utilisé |
| `height` / `h` | lors du recadrage | crop hauteur dans source-image pixels; exactement un forme doit être utilisé |
| `scaleX` | non | horizontal afficher multiplier, par défaut `1.0` |
| `scaleY` | non | vertical afficher multiplier, par défaut `1.0` |
| `displayWidth` | non | final afficher largeur dans pixels; preserves Le crop aspect ratio lorsque utilisé alone |
| `displayHeight` | non | final afficher hauteur dans pixels; preserves Le crop aspect ratio lorsque utilisé alone |
| `wrap` | non | `inline` place l’image sur la ligne ; sinon, `align` choisit le côté flottant |
| `align` | non | `left` ou `right` pour floating placement; ignored lorsque `wrap="inline"` |
| `title` | non | tooltip/titre texte |
| `alt` | non | Texte alternatif |
| `sound` | non | sound event played by Le whole image |
| `soundSrc` | non | sound fichier chemin pour Le whole image |
| `trigger` | non | `click` by par défaut, ou `hover` pour survol playback |

### Remarques

- `x`, `y`, `width` / `w`, et `height` / `h` sont tous obligatoire together lorsque cropping
- lorsque Le crop attributs sont tous omitted, `displayWidth` ou `displayHeight` affiche Le complet source image
- `width` et `height` définissent le recadrage source. Utilisez `displayWidth` / `displayHeight` ou les facteurs d’échelle pour la taille affichée.
- `scaleX` et `scaleY` calculer Le final afficher taille as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` ou `displayHeight` sets Le final afficher taille dans pixels; lorsque seulement un est present, Le autre dimension est calculé depuis Le crop aspect ratio
- providing les deux `displayWidth` et `displayHeight` permet intentional non-proportional stretching
- `displayWidth` / `displayHeight` ne peut pas être combined avec `scaleX` / `scaleY`
- unique-axis stretching est Pris en charge by setting seulement un scale differently
- `width` avec `w`, ou `height` avec `h`, est invalide et renders Un visible erreur
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
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation>
    Survolez l’image pour afficher cette infobulle.
  </ImageAnnotation>
</FloatingImage>
````

Region annotation avec Un visible bordure:

````md
<FloatingImage displayWidth="128" displayHeight="128" src="test1.png" align="left" x="0" y="0" width="32" height="32">
  <ImageAnnotation x="2" y="2" w="15" h="10" border borderColor="#FFFF4444" borderThickness="2">
    Voici l’infobulle de la **zone surlignée**.
  </ImageAnnotation>
</FloatingImage>
````

plusieurs regions on un image:

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

image regions peut aussi play sounds. utiliser `<SoundArea>` lorsque you seulement need sound, ou put `sound`
directly on `<ImageAnnotation>` lorsque Le même region aussi a Un tooltip ou bordure.

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
    Cette zone contient une infobulle et un son de clic.
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers Le whole image. Region sounds utiliser cropped-image coordonnées
et obey Le même overlap priority as tooltips: later regions win.

## contenu Embedding et texte Wrapping

Les balises basées sur `BlockTagCompiler`, notamment `<Recipe>`, `<GameScene>` et `<BlockImage>`, acceptent `wrap` et `align`.

`<FloatingImage>` suit ses propres règles : `wrap="inline"` place l’image sur la ligne ; sinon, `align="left"` ou `"right"` choisit le côté flottant. `<ItemImage>` est une icône en ligne ; utilisez `<Column>` ou `<Row>` pour l’aligner.

| Attribut | Valeur par défaut | Signification |
| --- | --- | --- |
| `wrap` | `inline` | `inline` : placement normal. `square`, `tight`, `through` : flottant à gauche ou à droite. `top-bottom`, `behind`, `front` : actuellement, placement normal aligné sans couches de texte distinctes. |
| `align` | `left` | `left`, `center` ou `right` en contexte bloc. En mode flottant, `right` choisit la droite et les autres valeurs la gauche. Pour centrer : `wrap="inline" align="center"`. |
| `float` | — | Ancien `left` / `right` dans le texte lorsque `wrap` et `align` sont absents. Préférez ces attributs explicites. |

```mdx
<BlockImage id="minecraft:stone" wrap="square" align="right" />

<BlockImage id="minecraft:diamond_block" wrap="inline" align="center" />

<Column align="center">
  <ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
</Column>
```

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

`<BlockImage>` affiche un bloc placé en aperçu 3D transparent et accepte les attributs de bloc ci-dessus pour l’alignement et l’habillage du texte.

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
