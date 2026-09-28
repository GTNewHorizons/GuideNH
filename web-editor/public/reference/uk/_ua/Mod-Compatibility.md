# Сумісність із модами

Ця документація описує синтаксис і функції виконання GuideNH. Код, теги, шляхи, ідентифікатори та значення атрибутів збережено без змін.


GuideNH ships з conditional integrations для вибраний mods. кожен integration is лише activated коли його ціль mod is завантажений; коли  ціль is absent,  related теги, indices і ключ bindings залишаються inert і  rest of  посібник зберігає working.

## StructureLib

коли StructureLib is завантажений, GuideNH може import multiblock previews до `<GameScene>` з `<ImportStructureLib>`.  client command `/exportStructure structureLib` може також export those previews as PNG documentation screenshots. Дивіться [Structure Export](Structure-Export) для  full command reference, StructureLib-певний параметри, і  related `gameScene` export режим.

## BetterQuesting

коли [BetterQuesting](https://github.com/GTNewHorizons/BetterQuesting) is завантажений, GuideNH unlocks three features:

1.  `quest_ids` сторінка-frontmatter ключ starts indexing сторінки by BetterQuesting quest id.
2. два новий теги become доступний: `<QuestLink>` (inline) і `<QuestCard>` (блок).
3.  стандартний "open посібник" hotkey works поки наведення quest in  BetterQuesting GUI: holding  ключ looks up  hovered quest id і navigates до  відповідний сторінка посібника.

### Indexing сторінки by quest id

Додайте a `quest_ids` список до  frontmatter of any сторінка посібника you want associated з один або more quests:

```yaml
---
navigation:
  title: Stage 2 — Steam Age
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
---
```

 значення може be canonical UUID strings або BetterQuesting compact Base64 quest ids. Malformed або empty записи є skipped з попередження in  log.

Compact ids є decoded перший, потім GuideNH falls назад до canonical UUID parsing. Цей відповідає BetterQuesting's compact quest-id format наприклад `AAAAAAAAAAAAAAAAAAAMug==`.

Do не список both encodings для  той самий quest in один сторінка's `quest_ids`; Вони normalize до  той самий внутрішній UUID і would be treated as duplicates.

коли quest id is indexed by a сторінка, both `<QuestLink>` і `<QuestCard>` буде route  натискання до який сторінка посібника замість цього of opening  BetterQuesting quest GUI directly.

### Linking з BetterQuesting descriptions до GuideNH сторінки

BetterQuesting quest descriptions може посилання назад до GuideNH з a `[guide]` тег.  тег is проаналізований by GuideNH лише коли BetterQuesting is завантажений, і потім відтворений through BetterQuesting's native hyperlink-aware текст box, so BQ word wrapping, прокручування і натискання hit boxes залишаються compatible.

використовувати  ідентифікатор сторінки as  ціль:

```text
[guide]guidenh:navigation-guide[/guide]
```

Якщо  ціль сторінка exists,  видимий посилання текст is замінений з який сторінка посібника's заголовок.  `.md` suffix is необов’язковий, so `guidenh:navigation-guide` і `guidenh:navigation-guide.md` точка до  той самий сторінка коли  Markdown сторінка exists.

використовувати `page=` коли you want власний видимий текст:

```text
[guide page=guidenh:navigation-guide]Open the navigation guide[/guide]
```

 посилання is показано in BetterQuesting's normal hyperlink колір з underline styling, shows GuideNH підказка on наведення, і opens  ціль GuideNH сторінка коли clicked. сторінка anchors є також Підтримувані:

```text
[guide page=guidenh:navigation-guide#navigation-fields]Navigation fields[/guide]
```

### `<QuestLink>` і `<QuestCard>`

Both теги приймають BetterQuesting quest id via `id` і decide їхні appearance з  гравець's progress at компілювати час:

| стан | Source | відтворення |
| --- | --- | --- |
| видимий | quest is unlocked but не completed | clickable посилання, Типове style |
| Completed | `quest.isComplete(player)` повертає true | clickable посилання, green колір, trailing `✓` |
| Locked | quest exists but is не unlocked, видимість ≠ прихований/SECRET | italic gray placeholder, не clickable |
| прихований | locked plus видимість is прихований або SECRET | italic dark-gray placeholder, no quest details leaked |
| Missing | quest id не розв’язати до any quest in  database | italic red placeholder |

Locked but non-прихований quests є також clickable. Вони використовувати  той самий Навігація ціль Правила as видимий і completed quests.

для видимий / completed / locked quests,  натискання ціль is:

-  indexed сторінка посібника Якщо  quest id is present in some сторінка's `quest_ids`
- otherwise BetterQuesting's quest-book quest екран, використовуючи BetterQuesting's native батьківський-екран flow

Дивіться [Tags Reference](Tags-Reference#questlink) для Атрибут таблиці і inline Приклади.

### прихований-quest handling

GuideNH ніколи renders  заголовок або опис of quest whose видимість is `HIDDEN` або `SECRET` поки  quest is досі locked для  гравець.  placeholder текст is taken з a ключ перекладу so packs може localize  wording:

| ключ перекладу | Типове (en_US) |
| --- | --- |
| `guidenh.compat.bq.locked` | `Locked Quest` |
| `guidenh.compat.bq.hidden` | `Hidden Quest` |
| `guidenh.compat.bq.missing` | `Unknown Quest` |
| `guidenh.compat.bq.open_in_guide` | `Open in Guide` |

Locked quests може досі показувати їхні опис as a наведення підказка on `<QuestLink>` і on  clickable заголовок усередині `<QuestCard>`, because BetterQuesting itself reveals  опис on locked quest tooltips. Установіть `show_tooltip="false"` (або `showTooltip={false}`) до suppress який підказка. прихований quests do не expose any підказка.

> [!Примітка]
> Quest стан is розв’язаний at сторінка-компілювати час використовуючи  локальний гравець's progress.  скомпільований сторінка is cached per посібник; reopening  посібник після completing або unlocking quest re-evaluates  стан.

### Open-посібник hotkey integration

 Типове open-посібник hotkey (`G`, configurable under `key.guidenh.open_guide`) gains second activation шлях коли BetterQuesting is завантажений. поки  BetterQuesting quest-рядок GUI is open:

1. наведення over any quest button in  BQ panel
2. Hold  open-посібник hotkey

Якщо any зареєстрований посібник indexes який quest id through `quest_ids`, GuideNH буде navigate до  відповідний сторінка (або open  посібник Якщо it's не вже open). Якщо no сторінка indexes  hovered id,  hotkey does nothing — it не fall назад до opening  BetterQuesting quest GUI, since BQ вже shows який information.

Цей шлях is independent of  inventory предмет-підказка шлях, so наведення items in your inventory досі routes through  existing предмет / ore index lookups.

### поведінка коли BetterQuesting is absent

- `<QuestLink>` і `<QuestCard>` є не зареєстрований, so сторінки який використовувати їх fall назад до  стандартний "невідомий тег" помилка відтворення until you видалити  тег.
- `quest_ids` frontmatter записи є досі проаналізований і stored under `additionalProperties`, but nothing reads їх.
-  hotkey's quest-наведення branch стає no-op.
- BetterQuesting `[guide]...[/guide]` опис links є ніколи проаналізований because  BetterQuesting текст box і related mixins є не завантажений.

## Exporting your теги до  site

 exported site renders  теги який ship з GuideNH. mod whose теги є скомпільований by його власний
`TagCompiler` registers renderer so його теги є exported as well, замість цього of showing up in  book і
disappearing з  site:

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

зареєстрований renderers є asked до  built-in ones, і  перший один який повертає markup wins, so a
renderer лише має до handle  теги it declares. Returning `null` leaves  element до  наступний renderer
або до  built-in export. renderer який throws is reported in  log і skipped, so один broken plugin
не може fail  export of a сторінка it не власний.

 context carries  сторінка being exported і  shared export services: `defaultNamespace`,
`currentPageId`, `templates`, `sceneResolver` і `compiler`,  той самий значення  built-in renderers працюють
з. Markup you записувати is inserted до  сторінка as it is, so escape текст з
`GuideSiteGraphRenderer.esc(...)`.

Цей означає a посібник який targets BetterQuesting може be authored once і silently degrade in environments where BetterQuesting is не installed.
