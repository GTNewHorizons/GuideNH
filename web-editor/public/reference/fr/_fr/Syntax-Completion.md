# Complétion de syntaxe

L’éditeur de guides du jeu propose des balises, des attributs et des valeurs pendant la rédaction d’une page.

## Utiliser la liste des candidats

| Action | Résultat |
| --- | --- |
| ↑ / ↓ | Sélectionner le candidat précédent ou suivant |
| Tab / Enter | Insérer le candidat sélectionné |
| Clic sur le candidat | Insérer le candidat sélectionné |
| Esc | Fermer la liste |
| Molette | Faire défiler les candidats |
| Barre de défilement | Faire glisser la barre de défilement |

## Complétions disponibles

| Emplacement de saisie | Suggestions |
| --- | --- |
| `<...` | Noms de balises valides dans le conteneur courant |
| `<Tag ...` | Noms d’attributs et délimiteurs de leurs valeurs |
| `id="..."`, `fluid="..."` | Noms de registre des objets et fluides, recettes, pages, ressources et autres valeurs prises en charge |
| Markdown | Titres, listes, tableaux, avertissements et autres structures Markdown |
| Bloc de code | Langages de code et noms de blocs GuideNH pris en charge |
| Frontmatter | Clés YAML et valeurs prises en charge |

La complétion insère une balise vide ou une paire de balises avec le curseur à l’intérieur. Ajoutez ensuite les attributs et le contenu nécessaires. Les valeurs d’objets et de fluides affichent des icônes ; les fluides utilisent des noms de registre comme `water` ou `steam`, pas des identifiants numériques.

## Texte multiligne et modèles

Les candidats affichent des aperçus courts. Les marqueurs visibles `\n` et `\t` représentent les sauts de ligne et tabulations ; la validation insère le texte réel. L’insertion multiligne reprend l’indentation courante et place le curseur à l’endroit où poursuivre la saisie.

Pour un exemple complet, ouvrez le menu de l’éditeur avec un clic droit et choisissez un modèle. Des modèles de scènes et de graphiques y sont proposés. La complétion normale des noms conserve une balise vide.

## Références associées

- [Format de page](Guide-Page-Format)
- [Balises et attributs](Tags-Reference)
- [Recettes](Recipes)
