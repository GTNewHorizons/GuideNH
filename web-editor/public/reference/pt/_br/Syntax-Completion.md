# Conclusão de sintaxe

O editor de guias no jogo sugere tags, atributos e valores enquanto você escreve uma página.

The in-game guide editor suggests tags, attributes and values while you write a page.

## Referência de escrita

A lâmpada abre a referência de escrita. Pesquise tópicos, atributos, campos YAML ou exemplos à esquerda; Ctrl+F foca a pesquisa e o botão direito limpa o campo. Selecionar um tópico salta para o título no documento completo. Os dois painéis rolam suavemente.

Voltar e Avançar restauram tópicos e posições. Passe o cursor sobre as marcas da barra direita para ver os títulos e clique para saltar. Copie exemplos pelo bloco de código ou o tópico em Markdown pela barra. A lâmpada permanece azul enquanto a referência está aberta; clique novamente para voltar ao guia preservando rascunho, seleção e histórico. Fechar ou Esc fecha a janela do guia. Traduções ausentes usam inglês.

## Using the candidate list

| Action | Result |
| --- | --- |
| ↑ / ↓ | Select the previous or next candidate |
| Tab / Enter | Insert the selected candidate |
| Click | Insert the selected candidate |
| Esc | Close the list |
| Mouse wheel | Scroll through candidates |
| Scrollbar | Drag the scrollbar to scroll |

## Supported completion

| While editing | Suggestions |
| --- | --- |
| `<...` | Tag names valid in the enclosing container |
| `<Tag ...` | Attribute names and their value delimiters |
| `id="..."`, `fluid="..."` | Item and fluid registry names, recipes, pages, assets and other supported values |
| Markdown | Headings, lists, tables, alerts and other Markdown constructs |
| Code fence | Supported code languages and GuideNH fence names |
| Frontmatter | YAML keys and supported values |

Tag-name completion inserts an empty tag or a matching pair with the cursor inside. Add the attributes and content you need afterward. Item and fluid value candidates show icons; fluid values use registry names such as `water` or `steam`, rather than numeric IDs.

### Quest and research IDs

ID suggestions follow the content being edited:

| Editing | ID source |
| --- | --- |
| `ResearchLink`, `ResearchCard`, `ResearchIcon` | Thaumcraft research |
| `QuestLink`, `QuestCard` | BetterQuesting quests |
| `id` in a `bindings` entry | The system named by that entry's `type` |
| `BindingLink`, `BindingCard`, `BindingIcon` | The system named by the tag's `type` |

For a binding entry, fill in `type` before choosing an `id`:

```yaml
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "infusion"}
```

The game editor reads IDs from the loaded mod. Both IDE plugins also suggest IDs already used in workspace pages; connecting to the running game adds its registered IDs. Research IDs retain their original case. To find or copy an ID, see [Thaumcraft compatibility](Mod-Compatibility#finding-and-copying-research-ids).

### Links de ação do NEI

O menu de inserção oferece trechos de ação para todas as receitas de um manipulador, receitas de itens e usos, além de `<NeiRecipeLink>`. O atributo `id` sugere IDs de itens; `mode` oferece `handler`, `recipes` e `usage`. Insira o identificador exato do NEI em `handlerId`.

Consulte [Receitas](Recipes.md#links-de-receitas-do-nei-no-texto) para a sintaxe e os exemplos.

## Multiline text and templates

Candidate rows use short previews. The visible `\n` and `\t` markers stand for line breaks and tabs; accepting a candidate inserts the actual text. Multiline insertion follows the current indentation and places the cursor where typing should continue.

For a complete example, open the editor's right-click menu and choose a template. Scene and chart templates are available there. Normal tag-name completion keeps the tag empty.

## Related references

- [Page format](Guide-Page-Format)
- [Tags and attributes](Tags-Reference)
- [Recipes](Recipes)
