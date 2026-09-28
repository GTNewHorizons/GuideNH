# annotations


Cette documentation décrit la syntaxe et les fonctions d’exécution de GuideNH. Le code, les balises, les chemins, les identifiants et les valeurs d’attributs restent inchangés.

GuideNH scène annotations sont enfant balises dans `<GameScene>` / `<Scene>`. Elles rendent dans monde espace et peut contenir enfant markdown/balise contenu que devient Un riche tooltip.

## Règles générales

- annotations seulement fonctionnent dans Un scène
- enfant contenu devient Le tooltip corps
- annotations peut être masquées avec Le scène UI toggle
- `alwaysOnTop` dessine au-dessus de scène géométrie lorsque Pris en charge by Le annotation type
- tous scène annotations aussi acceptent facultatif `showWhenStructure`, `showWhenTier`, et `showWhenChannels` conditions lorsque Le scène utilise `<ImportStructureLib>`

## Pris en charge annotation balises

- `<BlockAnnotation>`
- `<BoxAnnotation>`
- `<LineAnnotation>`
- `<DiamondAnnotation>`
- `<TextAnnotation>`

GuideNH aussi prend en charge `<BlockAnnotationTemplate>`, qui s’applique son enfant annotations vers chaque déjà-placed correspondant bloc dans Le actuel scène.

## StructureLib conditions

lorsque Un scène contient `<ImportStructureLib>`, chaque annotation balise peut restrict son visibilité vers Un spécifique
StructureLib état:

| attribut | Meaning |
| --- | --- |
| `showWhenStructure` | bind Le annotation vers Un named `<ImportStructureLib name="...">`; omit it lorsque Le scène seulement imports un StructureLib structure |
| `showWhenTier` | tier filtre comme `2`, `1..3`, `!2`, ou `1..5,!3` |
| `showWhenChannels` | per-channel filtre comme `input:1..3, casing:!2, fluid:4` |

Règles:

- `showWhenTier` et `showWhenChannels` sont combined avec logical et
- `showWhenChannels` peut mention plusieurs channels dans un attribut
- negated-seulement clauses like `!2` mean "quelconque valeur except 2"
- Le même attributs sont aussi Pris en charge by `<PlaySound>` et `<BlockAnnotationTemplate>` enfant annotations

Exemple:

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
    Visible uniquement pour l’état StructureLib sélectionné.
  </BlockAnnotation>
</GameScene>
````

## `<BlockAnnotation>`

Met en surbrillance un volume de bloc 1x1x1 unique.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `pos` | oui | `x y z` vector |
| `color` | non | `#RRGGBB`, `#AARRGGBB`, ou `transparent` |
| `thickness` | non | ligne thickness flottant |
| `alwaysOnTop` | non | booléen expression |

Exemple:

````md
<BlockAnnotation pos="2 0 2" color="#33DDEE" alwaysOnTop={true}>
  Met le bloc contrôleur en surbrillance.
</BlockAnnotation>
````

## `<BoxAnnotation>`

Met en surbrillance une boîte arbitraire alignée sur les axes.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `min` | oui | `x y z` minimum vector |
| `max` | oui | `x y z` maximum vector |
| `color` | non | annotation couleur |
| `thickness` | non | ligne thickness flottant |
| `alwaysOnTop` | non | booléen expression |

GuideNH automatiquement swaps min/max coordonnées per axis lorsque Elles sont fourni dans reverse ordre.

Exemple:

````md
<BoxAnnotation min="0 1 0" max="1 1.6 0.6" color="#EE3333" thickness="0.04">
  Surlignage sur une demi-hauteur.
</BoxAnnotation>
````

## `<LineAnnotation>`

dessine Un ligne segment ou polyline dans monde espace.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `from` | oui, sauf `points` est Définissez | `x y z` début vector |
| `to` | oui, sauf `points` est Définissez | `x y z` fin vector |
| `points` | non | Semicolon-separated `x y z` points pour Un polyline; remplace `from` / `to` |
| `color` | non | annotation couleur |
| `thickness` | non | ligne thickness flottant |
| `alwaysOnTop` | non | booléen expression |
| `arrow` | non | `start` ou `end`; omitted signifie non arrow |
| `showPoints` | non | booléen expression; affiche chaque point as Un small cube |
| `pointColor` | non | par défaut cube couleur; omitted utilise Le ligne couleur |
| `pointSize` | non | par défaut cube taille; omitted utilise Un valeur slightly larger que `thickness` |

`LineAnnotation` peut contenir `<LinePoint>` enfants vers remplacer point marker styling. `LinePoint`
utilise `index`, facultatif `show`, facultatif `color`, et facultatif `size`. points sont zero-indexed.
Arrows peut seulement être placed on Le début ou fin of Le ligne; intermediate polyline points ne peut pas carry
arrows.

Exemple:

````md
<LineAnnotation from="0.5 1.2 0.5" to="2.5 1.2 2.5" color="#FFD24C" thickness="0.08">
  Chemin du signal.
</LineAnnotation>
````

Polyline avec Un 3D endpoint arrow et sélectionné point markers:

````md
<LineAnnotation
  points="0.5 1.2 0.5; 1.5 1.7 0.5; 2.5 1.2 2.5"
  color="#FFD24C"
  thickness="0.08"
  arrow="end"
>
  <LinePoint index="0" show color="#66CCFF" />
  <LinePoint index="1" show color="#FF8844" size="0.12" />
  Chemin du signal avec un coude.
</LineAnnotation>
````

## `<DiamondAnnotation>`

Places Un écran-facing diamond marker at Un monde position.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `pos` | oui | `x y z` marker position |
| `color` | non | tint couleur; omitted defaults vers bright green |

Exemple:

````md
<DiamondAnnotation pos="0.5 2.2 0.5" color="#FFD24C">
  ### Activated Beacon
  Hover for rich content.
</DiamondAnnotation>
````

## `<TextAnnotation>`

dessine Un speech-bubble texte étiquette sur Le scène. It peut l’un ou l’autre follow Un monde-espace anchor point ou
stay fixe relatif vers Le scène centre. Unlike Le autre annotation balises, son enfant contenu est Le
bubble texte itself rather que Un survol tooltip.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `pos` | non | `x y z` monde-espace anchor vector |
| `x`, `y`, `z` | non | Alternative monde-espace anchor components lorsque `pos` est omitted |
| `text` | non | Bubble texte; enfant markdown est utilisé lorsque omitted |
| `textKey` | non | traduction clé résolu depuis ressource-pack `lang` files avant falling back vers `text` ou enfant markdown |
| `color` | non | Bubble bordure couleur; defaults vers light grey |
| `backgroundAlpha` | non | arrière-plan opacity depuis `0` vers `255`; defaults vers `204` |
| `maxWidth` | non | Wrap largeur dans pixels; `0` keeps Un unique ligne |
| `independent` | non | `true` keeps Le bubble fixe dans écran espace |
| `yOffset` | non | Pixel offset depuis Le scène centre lorsque `independent={true}` |
| `connectorSide` | non | `bottom`, `top`, `left`, `right`, ou `none`; defaults vers `bottom` |
| `connectorOffset` | non | Pixel offset along Le bubble edge; positive moves droite pour haut/bas et down pour gauche/droite |
| `connectorLength` | non | Pixel length of Le connector ligne; defaults vers `6` |
| `hlMinX/Y/Z`, `hlMaxX/Y/Z` | non | facultatif companion surligner box bounds |
| `highlightColor` | non | facultatif surligner box couleur |

monde-anchored bubbles dessiner Un connector ligne vers leurs anchor. utiliser `connectorSide` vers choisir qui
edge of Le bubble points at Le anchor, `connectorOffset` vers move Le attachment point along que
edge, et `connectorLength` vers control Le gap entre Le bubble et anchor. Independent bubbles
sont centered horizontally dans Le scène et utiliser `yOffset` pour vertical placement. Elles do ne dessiner a
connector. Le même exécution annotation est aussi utilisé lorsque importing Ponder `text` annotations.

Exemple:

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
  Insérez les objets ici avec une **priorité**.
</TextAnnotation>
````

fixe écran-espace Exemple:

````md
<TextAnnotation independent={true} yOffset={40} color="#FFFFCC00" backgroundAlpha={140}>
  Independent status text
</TextAnnotation>
````

## riche Tooltip contenu

annotation enfants sont compilé as normal GuideNH contenu, so tooltips peut contenir:

- markdown paragraphs et headings
- élément/bloc images
- recipes
- nested non-interactive scènes

Exemple:

````md
<DiamondAnnotation pos="0.5 1.5 0.5">
  **Machine Core**
  <RecipeFor id="minecraft:furnace" />
</DiamondAnnotation>
````

## `<BlockAnnotationTemplate>`

utiliser it lorsque you want vers stamp Le même annotation onto chaque correspondant bloc.

| attribut | obligatoire | Meaning |
| --- | --- | --- |
| `id` | oui | bloc matcher dans `modid:block[:meta]` form |

Règles:

- Le template seulement sees blocs que déjà exist lorsque it est analysé
- placer it après `<Block>`, `<ImportStructure>`, ou `<ImportStructureLib>` balises que devrait feed it
- enfant annotations utiliser local coordonnées relatif vers chaque correspondant bloc

Exemple:

````md
<GameScene zoom={2}>
  <ImportStructure src="/assets/example_structure.snbt" />
  <BlockAnnotationTemplate id="minecraft:log">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Infobulle générée par le modèle
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>
````

## Related Pages

- [GameScene](GameScene)
- [Examples](Examples)
