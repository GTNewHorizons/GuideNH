# Compatibilidade com mods

Esta documentação descreve a sintaxe e os recursos de execução do GuideNH. Código, tags, caminhos, IDs e valores de atributos permanecem intactos.


GuideNH ships com conditional integrations para selecionado mods. cada integration é somente activated quando seu destino mod é carregado; quando O destino é absent, O related tags, indices e chave bindings stay inert e O rest of O guia keeps working.

## StructureLib

quando StructureLib é carregado, GuideNH pode import multiblock previews em `<GameScene>` com `<ImportStructureLib>`. O client command `/exportStructure structureLib` pode também export those previews as PNG documentation screenshots. Consulte [Structure Export](Structure-Export) para O completo command reference, StructureLib-específico opções, e O related `gameScene` export modo.

## BetterQuesting

quando [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) é carregado, GuideNH unlocks três features:

1. O `quest_ids` página-frontmatter chave starts indexing pages by BetterQuesting quest id.
2. dois new tags become disponível: `<QuestLink>` (inline) e `<QuestCard>` (bloco).
3. O padrão "open guia" hotkey works enquanto ao passar o mouse Uma quest em O BetterQuesting GUI: holding O chave looks up O hovered quest id e navigates para O correspondente página do guia.

### Indexing pages by quest id

Adicione a `quest_ids` lista para O frontmatter of qualquer página do guia you want associated com um ou mais quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

O valores pode ser canonical UUID strings ou BetterQuesting compact Base64 quest ids. Malformed ou vazio entradas são skipped com Uma aviso em O log.

Compact ids são decoded primeiro, então GuideNH falls back para canonical UUID parsing. Este corresponde BetterQuesting's compact quest-id format como `AAAAAAAAAAAAAAAAAAAMug==`.

Do não lista ambos encodings para O mesmo quest em um página's `quest_ids`; Elas normalize para O mesmo interno UUID e would ser treated as duplicates.

quando Uma quest id é indexed by Uma página, ambos `<QuestLink>` e `<QuestCard>` irá route O clicar para que página do guia em vez disso of opening O BetterQuesting quest GUI directly.

### Linking de BetterQuesting descriptions para GuideNH pages

BetterQuesting quest descriptions pode link back em GuideNH com a `[guide]` tag. O tag é analisado by GuideNH somente quando BetterQuesting é carregado, e então renderizado por BetterQuesting's native hyperlink-aware texto box, so BQ word wrapping, rolagem e clicar hit boxes stay compatible.

usar O ID da página as O destino:

```text
[guide]guidenh:navigation-guide[/guide]
```

Se O destino página exists, O visível link texto é substituído com que página do guia's título. O `.md` suffix é opcional, so `guidenh:navigation-guide` e `guidenh:navigation-guide.md` ponto para O mesmo página quando O Markdown página exists.

usar `page=` quando you want personalizado visível texto:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

O link é exibido em BetterQuesting's normal hyperlink cor com underline styling, mostra Uma GuideNH dica on passar o mouse, e opens O destino GuideNH página quando clicked. página anchors são também Compatível:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` e `<QuestCard>`

ambos tags aceitam Uma BetterQuesting quest id via `id` e decide seus appearance de O player's progress at compilar time:

| estado | origem | renderização |
| --- | --- | --- |
| visível | quest é unlocked but não completed | clickable link, Padrão style |
| Completed | `quest.isComplete(player)` retorna true | clickable link, green cor, trailing `✓` |
| Locked | quest exists but é não unlocked, visibilidade ≠ oculto/SECRET | italic gray placeholder, não clickable |
| oculto | locked plus visibilidade é oculto ou SECRET | italic dark-gray placeholder, não quest details leaked |
| ausente | quest id não faz resolver para qualquer quest em O database | italic red placeholder |

Locked but non-oculto quests são também clickable. Elas usar O mesmo Navegação destino Regras as visível e completed quests.

para visível / completed / locked quests, O clicar destino é:

- O indexed página do guia Se O quest id é present em some página's `quest_ids`
- otherwise BetterQuesting's quest-book quest tela, using BetterQuesting's native pai-tela flow

Consulte [Tags Reference](Tags-Reference#questlink) para Atributo tabelas e inline Exemplos.

### oculto-quest handling

GuideNH nunca renders O título ou descrição of Uma quest whose visibilidade é `HIDDEN` ou `SECRET` enquanto O quest é ainda locked para O player. O placeholder texto é taken de Uma chave de tradução so packs pode localize O wording:

| chave de tradução | Padrão (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests pode ainda mostrar seus descrição as Uma passar o mouse dica on `<QuestLink>` e on O clickable título dentro de `<QuestCard>`, because BetterQuesting itself reveals O descrição on locked quest tooltips. Defina `show_tooltip="false"` (ou `showTooltip={false}`) para suppress que dica. oculto quests do não expose qualquer dica.

> [!Observação]
> Quest estado é resolvido at página-compilar time using O local player's progress. O compilado página é cached per guia; reopening O guia depois de completing ou unlocking Uma quest re-evaluates O estado.

### Open-guia hotkey integration

O Padrão open-guia hotkey (`G`, configurable under `key.guidenh.open_guide`) gains Uma second activation caminho quando BetterQuesting é carregado. enquanto O BetterQuesting quest-linha GUI é open:

1. passar o mouse sobre qualquer quest button em O BQ panel
2. Hold O open-guia hotkey

Se qualquer registrado guia indexes que quest id por `quest_ids`, GuideNH irá navigate para O correspondente página (ou open O guia Se it's não já open). Se não página indexes O hovered id, O hotkey faz nothing — it não faz fall back para opening O BetterQuesting quest GUI, since BQ já mostra que information.

Este caminho é independent of O inventory item-dica caminho, so ao passar o mouse itens em seu inventory ainda routes por O existing item / ore index lookups.

### Behavior quando BetterQuesting é absent

- `<QuestLink>` e `<QuestCard>` são não registrado, so pages que usar eles fall back para O padrão "desconhecido tag" erro renderização until you remover O tag.
- `quest_ids` frontmatter entradas são ainda analisado e stored under `additionalProperties`, but nothing reads eles.
- O hotkey's quest-passar o mouse branch torna-se Uma não-op.
- BetterQuesting `[guide]...[/guide]` descrição links são nunca analisado because O BetterQuesting texto box e related mixins são não carregado.

## Exporting seu tags para O site

O exported site renders O tags que ship com GuideNH. Uma mod whose tags são compilado by seu próprio
`TagCompiler` registers Uma renderer so seu tags são exported as well, em vez disso of showing up em O book e
disappearing de O site:

```java
public class MyModSiteTagRenderer implements GuideSiteTagRenderer {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public String render(GuideSiteTagRenderContext context, MdxJsxElementFields element) {
        String id = element.getAttributeString("id", "");
        return "<div class=\"mymod-machine\">" + GuideSiteGraphRenderer.esc(id) + "</div>";
    }
}
```

```java
GuideNhIntegrationRegistry.global().registerSiteTagRenderer(new MyModSiteTagRenderer());
Guide.builder(id).extension(GuideSiteTagRenderer.EXTENSION_POINT, new MyModSiteTagRenderer()).build();
```

registrado renderers são asked antes de O built-em ones, e O primeiro um que retorna markup wins, so a
renderer somente tem para handle O tags it declares. Returning `null` leaves O element para O próximo renderer
ou para O built-em export. Uma renderer que throws é reported em O log e skipped, so um broken plugin
não pode fail O export of Uma página it não faz próprio.

O context carries O página being exported e O shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` e `compiler`, O mesmo valores O built-em renderers funcionam
com. Markup you escrever é inserted em O página as it é, so escape texto com
`GuideSiteGraphRenderer.esc(...)`.

Este significa Uma guia que targets BetterQuesting pode ser authored once e silently degrade em environments where BetterQuesting é não installed.
