# Syntaxvervollständigung

Der Leitfadeneditor im Spiel schlägt beim Schreiben einer Seite Tags, Attribute und Werte vor.

## Kandidatenliste bedienen

| Aktion | Ergebnis |
| --- | --- |
| ↑ / ↓ | Vorherigen oder nächsten Kandidaten auswählen |
| Tab / Enter | Ausgewählten Kandidaten einfügen |
| Kandidat anklicken | Ausgewählten Kandidaten einfügen |
| Esc | Liste schließen |
| Mausrad | Durch Kandidaten scrollen |
| Bildlaufleiste | Bildlaufleiste zum Scrollen ziehen |

## Unterstützte Vervollständigung

| Eingabestelle | Vorschläge |
| --- | --- |
| `<...` | Im umgebenden Container gültige Tagnamen |
| `<Tag ...` | Attributnamen mit passenden Wertbegrenzern |
| `id="..."`, `fluid="..."` | Registrierungsnamen von Gegenständen und Flüssigkeiten, Rezepte, Seiten, Ressourcen und weitere unterstützte Werte |
| Markdown | Überschriften, Listen, Tabellen, Hinweise und andere Markdown-Strukturen |
| Codeblock | Unterstützte Codesprachen und GuideNH-Blocknamen |
| Frontmatter | YAML-Schlüssel und unterstützte Werte |

Die Tagvervollständigung fügt einen leeren Tag oder ein Tagpaar mit dem Cursor dazwischen ein. Ergänzen Sie danach Attribute und Inhalt. Gegenstands- und Flüssigkeitswerte zeigen Symbole; Flüssigkeiten verwenden Registrierungsnamen wie `water` oder `steam` statt numerischer IDs.

## Mehrzeiliger Text und Vorlagen

Kandidaten zeigen kurze Vorschauen. Die sichtbaren Markierungen `\n` und `\t` stehen für Zeilenumbrüche und Tabulatoren; beim Bestätigen wird der tatsächliche Text eingefügt. Mehrzeiliger Text übernimmt die aktuelle Einrückung und setzt den Cursor an die nächste Eingabestelle.

Für ein vollständiges Beispiel öffnen Sie das Kontextmenü mit einem Rechtsklick und wählen eine Vorlage. Dort finden Sie Szenen- und Diagrammvorlagen. Die normale Tagvervollständigung lässt den Tag leer.

## Weitere Referenzen

- [Seitenformat](Guide-Page-Format)
- [Tags und Attribute](Tags-Reference)
- [Rezepte](Recipes)
