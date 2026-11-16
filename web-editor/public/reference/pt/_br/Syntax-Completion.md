# Conclusão de sintaxe

O editor de guias no jogo sugere tags, atributos e valores enquanto você escreve uma página.

## Usar a lista de candidatos

| Ação | Resultado |
| --- | --- |
| ↑ / ↓ | Selecionar o candidato anterior ou seguinte |
| Tab / Enter | Inserir o candidato selecionado |
| Clique no candidato | Inserir o candidato selecionado |
| Esc | Fechar a lista |
| Roda do mouse | Rolar pelos candidatos |
| Barra de rolagem | Arrastar a barra para rolar |

## Conclusão disponível

| Local de edição | Sugestões |
| --- | --- |
| `<...` | Nomes de tags válidos no contêiner atual |
| `<Tag ...` | Nomes de atributos e delimitadores de seus valores |
| `id="..."`, `fluid="..."` | Nomes de registro de itens e fluidos, receitas, páginas, recursos e outros valores compatíveis |
| Markdown | Títulos, listas, tabelas, avisos e outras estruturas Markdown |
| Bloco de código | Linguagens de código e nomes de blocos GuideNH compatíveis |
| Frontmatter | Chaves YAML e valores compatíveis |

A conclusão insere uma tag vazia ou um par de tags com o cursor dentro. Depois acrescente os atributos e o conteúdo necessários. Os valores de itens e fluidos mostram ícones; fluidos usam nomes de registro como `water` ou `steam`, e não IDs numéricos.

## Texto multilinha e modelos

Os candidatos mostram prévias curtas. Os marcadores visíveis `\n` e `\t` representam quebras de linha e tabulações; a confirmação insere o texto real. A inserção multilinha segue a indentação atual e posiciona o cursor onde você deve continuar escrevendo.

Para inserir um exemplo completo, abra o menu do editor com o botão direito e escolha um modelo. Há modelos de cenas e gráficos nesse menu. A conclusão normal de nomes mantém a tag vazia.

## Referências relacionadas

- [Formato de página](Guide-Page-Format)
- [Tags e atributos](Tags-Reference)
- [Receitas](Recipes)
