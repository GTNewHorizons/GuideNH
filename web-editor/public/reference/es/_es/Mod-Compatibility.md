# Mod Compatibility


Esta documentación describe la sintaxis y las funciones de ejecución de GuideNH. El código, las etiquetas, las rutas, los identificadores y los valores de atributos se conservan sin cambios.

GuideNH ships con conditional integrations para seleccionado mods. cada integration es solo activated cuando su destino mod es cargado; cuando El destino es absent, El related etiquetas, indices y clave bindings permanecer inert y El rest of El guíUn mantiene working.

## StructureLib

cuando StructureLib es cargado, GuideNH puede import multiblock previews en `<GameScene>` con `<ImportStructureLib>`. El client command `/exportStructure structureLib` puede también export those previews as PNG documentation screenshots. Consulta [Structure Export](Structure-Export) para El completo command reference, StructureLib-específico opciones, y El related `gameScene` export modo.

## BetterQuesting

cuando [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) es cargado, GuideNH unlocks tres features:

1. El `quest_ids` página-frontmatter clave starts indexing páginas by BetterQuesting quest id.
2. dos nuevo etiquetas become disponible: `<QuestLink>` (inline) y `<QuestCard>` (bloque).
3. El estándar "open guía" hotkey works mientras al pasar el cursor Un quest en El BetterQuesting GUI: holding El clave looks up El hovered quest id y navigates Un El coincidente guíUn página.

### Indexing páginas by quest id

Añada a `quest_ids` lista Un El frontmatter of cualquier guíUn página you want associated con uno o más quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

El valores puede ser canonical UUID strings o BetterQuesting compact Base64 quest ids. Malformed o vacío entradas son skipped con Un advertencia en El log.

Compact ids son decoded primero, entonces GuideNH falls atrás Un canonical UUID parsing. esto coincide BetterQuesting's compact quest-id format como `AAAAAAAAAAAAAAAAAAAMug==`.

Do no lista ambos encodings para El mismo quest en uno página's `quest_ids`; Estas normalize Un El mismo interno UUID y would ser treated as duplicates.

cuando Un quest id es indexed by Un página, ambos `<QuestLink>` y `<QuestCard>` será route El clic Un que guíUn página en su lugar of opening El BetterQuesting quest GUI directly.

### Linking de BetterQuesting descriptions Un GuideNH páginas

BetterQuesting quest descriptions puede enlace atrás en GuideNH con a `[guide]` etiqueta. El etiqueta es analizado by GuideNH solo cuando BetterQuesting es cargado, y entonces renderizado a través de BetterQuesting's native hyperlink-aware texto box, so BQ word wrapping, al desplazar y clic hit boxes permanecer compatible.

usar El página id as El destino:

```text
[guide]guidenh:navigation-guide[/guide]
```

Si El destino página exists, El visible enlace texto es reemplazado con que guíUn página's título. El `.md` suffix es opcional, so `guidenh:navigation-guide` y `guidenh:navigation-guide.md` punto Un El mismo página cuando El markdown página exists.

usar `page=` cuando you want personalizado visible texto:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

El enlace es mostrado en BetterQuesting's normal hyperlink color con underline styling, muestra Un GuideNH tooltip on pasar el cursor, y opens El destino GuideNH página cuando clicked. página anchors son también Compatible:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` y `<QuestCard>`

ambos etiquetas aceptan Un BetterQuesting quest id via `id` y decide sus appearance de El jugador's progress at compilar tiempo:

| estado | origen | renderizado |
| --- | --- | --- |
| visible | quest es unlocked but no completed | clickable enlace, predeterminado style |
| Completed | `quest.isComplete(player)` devuelve true | clickable enlace, green color, trailing `✓` |
| Locked | quest exists but es no unlocked, visibilidad ≠ ocultas/SECRET | italic gray placeholder, no clickable |
| ocultas | locked plus visibilidad es ocultas o SECRET | italic dark-gray placeholder, no quest details leaked |
| faltante | quest id hace no resolver Un cualquier quest en El database | italic red placeholder |

Locked but non-ocultas quests son también clickable. Estas usar El mismo navigation destino Reglas as visible y completed quests.

para visible / completed / locked quests, El clic destino es:

- El indexed guíUn página Si El quest id es present en some página's `quest_ids`
- otherwise BetterQuesting's quest-book quest pantalla, usando BetterQuesting's native padre-pantalla flow

Consulta [Tags Reference](Tags-Reference#questlink) para atributo tablas y inline Ejemplos.

### ocultas-quest handling

GuideNH nunca renderiza El título o descripción of Un quest whose visibilidad es `HIDDEN` o `SECRET` mientras El quest es todavía locked para El jugador. El placeholder texto es taken de Un traducción clave so packs puede localize El wording:

| traducción clave | predeterminado (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests puede todavía mostrar sus descripción as Un pasar el cursor tooltip on `<QuestLink>` y on El clickable título dentro de `<QuestCard>`, because BetterQuesting itself reveals El descripción on locked quest tooltips. Establezca `show_tooltip="false"` (o `showTooltip={false}`) Un suppress que tooltip. ocultas quests do no expose cualquier tooltip.

> [!Nota]
> Quest estado es resuelto at página-compilar tiempo usando El local jugador's progress. El compilado página es cached per guía; reopening El guíUn después de completing o unlocking Un quest re-evaluates El estado.

### Open-guíUn hotkey integration

El predeterminado open-guíUn hotkey (`G`, configurable under `key.guidenh.open_guide`) gains Un second activation ruta cuando BetterQuesting es cargado. mientras El BetterQuesting quest-línea GUI es open:

1. pasar el cursor sobre cualquier quest button en El BQ panel
2. Hold El open-guíUn hotkey

Si cualquier registrado guíUn indexes que quest id a través de `quest_ids`, GuideNH será navigate Un El coincidente página (o open El guíUn Si it's no ya open). Si no página indexes El hovered id, El hotkey hace nothing — it hace no fall atrás Un opening El BetterQuesting quest GUI, since BQ ya muestra que information.

esto ruta es independent of El inventory elemento-tooltip ruta, so al pasar el cursor elementos en su inventory todavía routes a través de El existing elemento / ore index lookups.

### comportamiento cuando BetterQuesting es absent

- `<QuestLink>` y `<QuestCard>` son no registrado, so páginas que usar ellos fall atrás Un El estándar "desconocido etiqueta" error renderizado until you eliminar El etiqueta.
- `quest_ids` frontmatter entradas son todavía analizado y stored under `additionalProperties`, but nothing reads ellos.
- El hotkey's quest-pasar el cursor branch se convierte en Un no-op.
- BetterQuesting `[guide]...[/guide]` descripción enlaces son nunca analizado because El BetterQuesting texto box y related mixins son no cargado.

## Exporting su etiquetas Un El site

El exported site renderiza El etiquetas que ship con GuideNH. Un mod whose etiquetas son compilado by su propio
`TagCompiler` registers Un renderer so su etiquetas son exported as well, en su lugar of showing up en El book y
disappearing de El site:

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

registrado renderers son asked antes de El built-en ones, y El primero uno que devuelve markup wins, so a
renderer solo tiene Un handle El etiquetas it declares. Returning `null` leaves El element Un El siguiente renderer
o Un El built-en export. Un renderer que throws es reported en El log y skipped, so uno broken plugin
no puede fail El export of Un página it hace no propio.

El context carries El página being exported y El shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` y `compiler`, El mismo valores El built-en renderers funcionan
con. Markup you escribir es inserted en El página as it es, so escape texto con
`GuideSiteGraphRenderer.esc(...)`.

esto significa Un guíUn que targets BetterQuesting puede ser authored once y silently degrade en environments where BetterQuesting es no installed.
