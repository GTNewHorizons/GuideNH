# Syntaxisaanvulling

De gidseditor in het spel stelt tags, attributen en waarden voor terwijl u een pagina schrijft.

## De kandidatenlijst gebruiken

| Actie | Resultaat |
| --- | --- |
| ↑ / ↓ | De vorige of volgende kandidaat selecteren |
| Tab / Enter | De geselecteerde kandidaat invoegen |
| Kandidaat aanklikken | De geselecteerde kandidaat invoegen |
| Esc | De lijst sluiten |
| Muiswiel | Door kandidaten scrollen |
| Schuifbalk | De schuifbalk slepen om te scrollen |

## Ondersteunde aanvulling

| Invoerpositie | Suggesties |
| --- | --- |
| `<...` | Tagnamen die geldig zijn in de huidige container |
| `<Tag ...` | Attribuutnamen en hun waardebegrenzers |
| `id="..."`, `fluid="..."` | Registratienamen van voorwerpen en vloeistoffen, recepten, pagina’s, bestanden en andere ondersteunde waarden |
| Markdown | Koppen, lijsten, tabellen, meldingen en andere Markdown-structuren |
| Codeblok | Ondersteunde codetalen en GuideNH-bloknamen |
| Frontmatter | YAML-sleutels en ondersteunde waarden |

Tagaanvulling voegt een lege tag of een tagpaar met de cursor ertussen in. Voeg daarna de benodigde attributen en inhoud toe. Voorwerpen en vloeistoffen hebben pictogrammen bij hun waarden; vloeistoffen gebruiken registratienamen zoals `water` of `steam` in plaats van numerieke ID’s.

## Meerregelige tekst en sjablonen

Kandidaten tonen korte voorbeelden. De zichtbare markeringen `\n` en `\t` staan voor regeleinden en tabs; bevestigen voegt de echte tekst in. Meerregelige invoer neemt de huidige inspringing over en plaatst de cursor waar u verder kunt typen.

Open voor een volledig voorbeeld het rechtermuisknopmenu van de editor en kies een sjabloon. Daar vindt u scène- en grafieksjablonen. Normale tagaanvulling houdt de tag leeg.

## Gerelateerde naslag

- [Paginaformaat](Guide-Page-Format)
- [Tags en attributen](Tags-Reference)
- [Recepten](Recipes)
