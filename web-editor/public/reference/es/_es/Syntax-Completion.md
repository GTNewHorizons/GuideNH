# Compleción de sintaxis

El editor de guías del juego sugiere etiquetas, atributos y valores mientras escribe una página.

## Usar la lista de candidatos

| Acción | Resultado |
| --- | --- |
| ↑ / ↓ | Seleccionar el candidato anterior o siguiente |
| Tab / Enter | Insertar el candidato seleccionado |
| Clic en el candidato | Insertar el candidato seleccionado |
| Esc | Cerrar la lista |
| Rueda del ratón | Desplazarse por los candidatos |
| Barra de desplazamiento | Arrastrar la barra para desplazarse |

## Compleción disponible

| Posición de edición | Sugerencias |
| --- | --- |
| `<...` | Nombres de etiquetas válidos en el contenedor actual |
| `<Tag ...` | Nombres de atributos y sus delimitadores de valores |
| `id="..."`, `fluid="..."` | Nombres de registro de objetos y fluidos, recetas, páginas, recursos y otros valores compatibles |
| Markdown | Títulos, listas, tablas, avisos y otras estructuras Markdown |
| Bloque de código | Lenguajes de código y nombres de bloques GuideNH compatibles |
| Frontmatter | Claves YAML y valores compatibles |

La compleción inserta una etiqueta vacía o un par de etiquetas con el cursor dentro. Después añada los atributos y el contenido necesarios. Los valores de objetos y fluidos muestran iconos; los fluidos usan nombres de registro como `water` o `steam`, no IDs numéricos.

## Texto multilínea y plantillas

Los candidatos muestran vistas previas breves. Los marcadores visibles `\n` y `\t` representan saltos de línea y tabulaciones; al aceptar se inserta el texto real. La inserción multilínea respeta la sangría actual y coloca el cursor donde debe continuar escribiendo.

Para insertar un ejemplo completo, abra el menú del editor con el botón derecho y elija una plantilla. Allí encontrará plantillas de escenas y gráficos. La compleción normal de nombres mantiene la etiqueta vacía.

## Referencias relacionadas

- [Formato de página](Guide-Page-Format)
- [Etiquetas y atributos](Tags-Reference)
- [Recetas](Recipes)
