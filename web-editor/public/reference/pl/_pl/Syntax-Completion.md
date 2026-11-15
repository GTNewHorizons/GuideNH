# Uzupełnianie składni

Edytor przewodnika w grze podpowiada tagi, atrybuty i wartości podczas pisania strony.

## Obsługa listy kandydatów

| Działanie | Wynik |
| --- | --- |
| ↑ / ↓ | Wybrać poprzedniego lub następnego kandydata |
| Tab / Enter | Wstawić wybranego kandydata |
| Kliknięcie kandydata | Wstawić wybranego kandydata |
| Esc | Zamknąć listę |
| Kółko myszy | Przewijać kandydatów |
| Pasek przewijania | Przeciągnąć pasek przewijania |

## Dostępne uzupełnianie

| Miejsce edycji | Podpowiedzi |
| --- | --- |
| `<...` | Nazwy tagów dozwolonych w bieżącym kontenerze |
| `<Tag ...` | Nazwy atrybutów i ograniczniki ich wartości |
| `id="..."`, `fluid="..."` | Nazwy rejestrowe przedmiotów i płynów, przepisy, strony, zasoby i inne obsługiwane wartości |
| Markdown | Nagłówki, listy, tabele, ostrzeżenia i inne struktury Markdown |
| Blok kodu | Obsługiwane języki kodu i nazwy bloków GuideNH |
| Frontmatter | Klucze YAML i obsługiwane wartości |

Uzupełnianie nazwy wstawia pusty tag lub parę tagów z kursorem wewnątrz. Potem dodaj potrzebne atrybuty i treść. Wartości przedmiotów i płynów mają ikony; płyny używają nazw rejestrowych, takich jak `water` lub `steam`, zamiast liczbowych identyfikatorów.

## Tekst wielowierszowy i szablony

Kandydaci pokazują krótkie podglądy. Widoczne znaczniki `\n` i `\t` oznaczają nowe linie i tabulatory; zatwierdzenie wstawia rzeczywisty tekst. Wstawianie wielu linii zachowuje bieżące wcięcie i ustawia kursor tam, gdzie należy kontynuować pisanie.

Aby wstawić pełny przykład, otwórz menu edytora prawym przyciskiem myszy i wybierz szablon. Dostępne są tam szablony scen i wykresów. Zwykłe uzupełnianie nazw pozostawia tag pusty.

## Powiązane materiały

- [Format strony](Guide-Page-Format)
- [Tagi i atrybuty](Tags-Reference)
- [Przepisy](Recipes)
