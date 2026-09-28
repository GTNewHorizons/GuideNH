# 語法智能補全

GuideNH 的頁面編輯器會補全 MDX 標籤、標籤屬性、屬性值、Markdown 語法、程式碼圍籬語言與
frontmatter 鍵。這些**全部來自註冊表**，沒有任何硬編碼：你的模組可以往裡面註冊。

入口有兩條，通常兩條都會用到。

## 1. 標籤名稱來自你的標籤解析器

編輯器會列出所有已註冊 `TagCompiler` 與 `SceneElementTagCompiler` 的 `getTagNames()`。如果你的模組
已經註冊了標籤解析器，它的標籤會自動出現在補全裡，不需要額外寫任何補全程式碼：

`TagCompiler` 可透過 `GuideNhIntegrationRegistry.registerTagCompilerProvider(...)` 對所有指南註冊，
`SceneElementTagCompiler` 則透過 `registerSceneElementTagCompilerProvider(...)`；兩者亦可使用
`GuideBuilder.extension(...)` 只加在某個指南上。 provider 只在建置指南時被讀取，因此請在模組載入階段註冊。

```java
public class MyModMachineTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("MyMachine");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        // ...
    }
}
```

以原有方式註冊即可：

```java
GuideNhIntegrationRegistry.global().registerTagCompilerProvider(compilers -> compilers.add(new MyModMachineTagCompiler()));
```

或只對某個指南生效：

```java
Guide.builder(id).extension(TagCompiler.EXTENSION_POINT, new MyModMachineTagCompiler()).build();
```

解析器接受但作者從不手寫的標籤（解析器產出的 `p`、`h1`、`em` 等）不會出現在補全裡。本模組自己的這類
標籤透過 `SyntaxSink.hiddenTags(...)` 隱藏。

## 2. 用語法貢獻者（Contributor）補上其餘訊息

標籤解析器不表達"它讀哪些屬性"以及"這些屬性接受什麼值"。這正是 `SyntaxContributor` 的職責，它同時
負責宣告 Markdown 片段、程式碼圍籬語言與 frontmatter 鍵。

```java
public class MyModSyntaxContributor implements SyntaxContributor {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public void contribute(SyntaxSink sink) {
        // 属性。取值类型决定由哪个取值源来补全。
        sink.attributes(
            "MyMachine",
            AttributeSyntax.of("id", SyntaxValueKind.STRING),
            AttributeSyntax.of("machine", SyntaxValueKind.of("MYMOD_MACHINE")),  // 你自己的类型
            AttributeSyntax.of("tier", SyntaxValueKind.INT),
            AttributeSyntax.of("formed", SyntaxValueKind.BOOLEAN),
            AttributeSyntax.of("mode", SyntaxValueKind.ENUM, "input", "output", "both"));

        // 会包裹内容的标签补全为 <Name></Name>，光标落在中间。
        sink.containerTags("MyPanel");
        sink.children("MyPanel", "MyMachine", "Tooltip");

        // 补全时从你自己的注册表取值的取值源。
        sink.valueSource(new MachineIdValueSource());

        // 额外的 Markdown 片段与围栏语言。
        sink.markdown(MarkdownSnippet.block("::", "模组提示", ":: note\n", 3));
        sink.fenceLanguages("mymod-diagram");

        // frontmatter 键：固定取值，或声明取值类型。
        sink.frontmatterKeys("mymod_machine");
        sink.frontmatterKind("mymod_machine", SyntaxValueKind.of("MYMOD_MACHINE"));
    }
}
```

全域註冊，或只對單一指南註冊：

```java
GuideNhIntegrationRegistry.global().registerSyntaxContributor(new MyModSyntaxContributor());
Guide.builder(id).extension(SyntaxContributor.EXTENSION_POINT, new MyModSyntaxContributor()).build();
```

全域註冊的貢獻者先應用，因此指南自己的貢獻者可以覆蓋它們。

本模組自帶的內建語法就是一個普通貢獻者（`BuiltinSyntaxContributor`），以預設擴充的方式註冊。因此
`GuideBuilder.disableDefaultExtensions()` 會連同預設標籤解析器一起把內建語法關掉。

## 3. 取值類型與取值來源

屬性的 `SyntaxValueKind` 決定編輯器如何補全、以及如何書寫它的值：

| 類型 | 補全來源 | 書寫形式 |
| --- | --- | --- |
| `STRING` | 自由文字（或屬性旁邊聲明的固定候選） | `"..."` |
| `INT`、`FLOAT` | 常見屬性名的數值預設 | 裸值 |
| `BOOLEAN` | `true`、`false` | 裸值 |
| `ENUM` | 傳給 `AttributeSyntax.of` 的枚舉常數，或聲明的固定候選 | `"..."` |
| `COLOR` | `#rrggbb` 以及符號顏色名 | `"..."` |
| `ITEM_ID`、`BLOCK_ID` | 物品/方塊註冊名，附圖標 | `"..."` |
| `ORE_DICT`、`ENTITY_ID`、`KEY_BIND`、`COMMAND` | 對應的遊戲註冊表 | `"..."` |
| `PAGE_PATH`、`FILE_PATH` | 指南頁 id、指南資源目錄中的文件 | `"..."` |
| `MOD_ID` | 取自物品註冊表命名空間的模組 id | `"..."` |
| `EXPRESSION`、`DOMAIN`、`FORMAT_PATTERN` | 常用表達式、區間與格式模板 | `"..."` |
| `VECTOR3` | 自由文本 | 裸值 |
| `SNBT` | 自由文本 | `{...}` |
| `QUEST_UUID` | 自由文字；任務 id 目前無法列舉 | `"..."` |

重複使用內建類型不需要註冊任何東西：把屬性標成 `ITEM_ID`，它就有物品補全。

要從你自己的註冊表補全，就聲明一個類型和一個取值來源。類型只是一個標識，按你的習慣聲明即可：

```java
public final class MyModValueKinds {

    public static final SyntaxValueKind MACHINE = SyntaxValueKind.of("MYMOD_MACHINE");
}
```

```java
public class MachineIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(MyModValueKinds.MACHINE);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (MyMachine machine : MyMachineRegistry.all()) {
            if (results.size() >= limit) {
                break;
            }
            if (machine.id()
                .startsWith(request.partialText())) {
                results.add(SyntaxSuggestion.withValue(machine.id(), machine.id(), machine.displayName()));
            }
        }
        return results;
    }
}
```

`request` 攜帶已輸入文字、外層標籤與屬性名稱、以及 frontmatter 鍵，因此一個取值來源可以服務多個屬性。

`SyntaxValueKind` 依值比較：兩處獨立宣告同一個型別會路由到同一批取值來源，所以型別不需要集中定義，
也不需要依賴本模組提供的任何枚舉類別。

## 4. 即時數據

需要讀取目前指南或文件內容的取值來源實作 `SyntaxEnvironmentAware`。 `prepare` 在每次補全 tick 中、任何
請求應答前呼叫一次：

```java
public class MyMachineValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    @Override
    public void prepare(SyntaxEnvironment environment) {
        // environment.guide()、environment.pagePaths()、environment.documentText()
    }
}
```

`prepare` 要寫得夠輕：它每個編輯器 tick 都會執行。把派生清單緩存起來，只在輸入真正變化時才重建——
內建的 `PagePathValueSource` 與 `AnchorValueSource` 就是這麼做的。

## 5. 插入模板

補全標籤名預設寫入 `<Name />`，或寫入成對標籤並把遊標放在容器內部。如果某個標籤必須帶上屬性或內容才
有意義，就宣告它該補全成什麼文字：

```java
sink.insertTemplates(
    InsertTemplate.caretAfter("MyMachine", "<MyMachine id=\"\" tier=\"1\" />", "id=\""),
    InsertTemplate.caretAfter("MyBlock", "<MyBlock id=\"\">\n  \n</MyBlock>", "\n  "));
```

`caretAfter` 把遊標放在給定標記第一次出現的位置之後，因此模板不需要手數字符數就能說明接下來在哪裡繼續
輸入。 `InsertTemplate.of(tag, text)` 則把遊標放在文字末尾。模板會替換已輸入標籤名，作者確認後就直接落
在所寫好的形式裡。

本模組本身也為"光有標籤名不夠用"的標籤聲明了模板，例如 `<BlockStat item="" count="1" />`、
`<InputAnnotation pos="0.5 1.5 0.5" inputType="lmb" />`，以及帶有第一個資料系列的圖表。

## 6. 槽位：你自己的文法

以上都是本模組已經解析的語法。若要補全你自己的語法－區塊內的指令、程式碼圍欄裡的迷你語言、你自己編譯的字
段－註冊一個 `SyntaxSlot`：

```java
public class MyModSlot implements SyntaxSlot {

    @Override
    public String namespace() {
        return "mymod";
    }

    @Override
    public SyntaxSlotMatch match(String text, int cursorIndex, GuideSyntaxModel model) {
        // 只接管你自己标签里某个属性的取值，且引号尚未闭合。
        String attribute = "ports=\"";
        int attributeStart = text.lastIndexOf(attribute, cursorIndex);
        if (attributeStart < 0) {
            return null;
        }
        int from = attributeStart + attribute.length();
        int closingQuote = text.indexOf('"', from);
        if (closingQuote >= 0 && cursorIndex > closingQuote) {
            return null;
        }
        return new SyntaxSlotMatch(
            from,
            cursorIndex,
            text.substring(from, cursorIndex),
            MyPortRegistry.suggestions(),
            suggestion -> SyntaxReplacement.cursorAtEnd(suggestion.value()));
    }
}
```

註冊方式與貢獻者完全一致：

```java
GuideNhIntegrationRegistry.global().registerSyntaxSlot(new MyModSlot());
Guide.builder(id).extension(SyntaxSlot.EXTENSION_POINT, new MyModSlot()).build();
```

槽位能控制的內容：

- `match` 決定"確認候選項後替換哪一段範圍"、"這段裡已經輸入了什麼"、"編輯器提供哪些取值"。遊標不在你的
語法裡時回傳 `null`。你報告為"已輸入"的文字必須是文檔裡該範圍真實的內容：編輯器寫入前會再次核對，若報
的是規範化後的形式，這次確認會被丟棄。
- match 裡的 `SyntaxValueWriter` 決定確認後寫入什麼：替換範圍的文字、遊標落點、以及選取範圍。留空
（`null`）表示用取值本身取代該範圍。 writer 拋異常或不回傳內容時會在日誌中報告，編輯器不會寫入任何內容。
- `selection` 可選宣告雙擊你的語法時選取哪一段。

插槽會在編輯器本身的解析器之前被詢問；第一個命中的槽位即擁有遊標，即使它當下沒有可提供的取值－因此本模
組不會在別的模組宣告的文字亂猜。

## 7. 編輯器目前已覆寫的語法

- MDX 標籤，以外層容器收窄；會包裹內容的標籤補全為 `<Name></Name>`。
- 屬性名，依取值類型展開為 `name=""`、`name={}` 或 `name={true}`。
- 屬性取值，依取值類型路由，包括配方、物品/方塊 id 與模組 id。
- Markdown 語法：標題、清單、任務清單、引用、提示區塊、表格、程式碼區塊、分隔線、公式區塊，以及行內的強調、
行內程式碼、連結與圖片。每個片段都會把遊標放在接下來要繼續輸入的位置。
- 代碼圍欄語言，來自代碼塊語言註冊表加上 GuideNH 特有的幾種圍欄。
- YAML frontmatter 鍵，以及宣告了取值類型的鍵的值。
- 插入範本：光有標籤名稱不夠用的標籤會一次補全成完整形式。

候選項會排序，彈窗預設選取最近的一個：先精確前綴，再匹配命名空間之後的標識，最後才是名稱中任意位置
匹配。

## 8. 可擴展的範圍，以及目前還不行的地方

本頁所有內容都是登錄；註冊進去的外掛程式只在建置指南或編譯頁面時被讀取，因此請在模組載入階段註冊。

| 你想做的事 | 用什麼 |
| --- | --- |
| 新增標籤、場景元素、屬性、取值類型及其取值 | `TagCompiler`、`SceneElementTagCompiler`、`SyntaxContributor`、`SyntaxValueSource` |
| 補全你自己的語法（自己的插槽、自己的寫入方式、雙擊範圍） | `SyntaxSlot` |
| 讓某個標籤一次補全成完整形式 | `sink.insertTemplates(...)`（同時出現在編輯器插入選單裡） |
| 新增 Markdown 片段、圍籬名稱、frontmatter 鍵與取值 | `SyntaxContributor` |
| 決定你自己圍籬的內容如何解析 | `CodeFenceRenderer`（圍籬名同時以 `sink.fenceLanguages(...)` 宣告；重寫 `renderSiteFence` 設站點也顯示） |
| 新增符號顏色名 | `SymbolicColorResolver`（單一指南）或 `registerSymbolicColorResolver`（全域） |
| 新增頁面索引 | `GuideBuilder.index(...)` / `GuideBuilderIntegrationHook` |
| 把自己的標籤匯出到站點 | `GuideSiteTagRenderer`（見 [模組相容](Mod-Compatibility)） |
| 為場景編輯器加工具列按鈕或選單項目 | `SceneEditorToolbarRegistry` / `SceneEditorMenuRegistry` |
| 讓匯出站點多一份資源 | 在你的節點上實作 `ExportableResourceProvider` |
| 把自己的文法問題回報給讀者 | `LytErrorSink.appendError(compiler, text, element)`，即標籤編譯器所拿到的 `parent` |
| 為指南編輯器加按鈕或選單項 | `GuideNhIntegrationRegistry.registerEditorAction(GuideEditorActionContribution.insert(...))` 或 `.wrap(...)`，或實作 `GuideEditorActionContribution.Provider` 只加到某個指南 |

刻意保持封閉的部分，以及代價：

- 指南編輯器的內建工具列是固定集合。貢獻的**動作**（請參閱 `GuideEditorActionContribution`）會在內建項目之後獲得自己的工具列按鈕與選單項，因此無法做的是改動某個內建動作，而不是新增一個。
- 診斷在編譯期間透過 `LytErrorSink` 回報：錯誤區塊會被追加進文檔，因此書內與匯出站點都會顯示。編輯器裡沒有單獨的「邊打字邊告警」鉤子。
- 網站匯出用自己的內建鏈渲染自帶標籤。註冊的渲染器會先被詢問（對每個 MDX 元素、以及你自己的圍欄名），所以你的標籤與圍欄都能導出；但內置鏈本身還不是一組註冊式渲染器。
- 你自己的場景註解能在書內渲染，但不會被序列化進導出網站的檢視器。
- 你自己註冊為 `SceneElementTagCompiler` 的場景元素能在書內渲染，但只有它的元素是 `SceneAnnotation` 時才進得了站點：導出器只收集這一種形態。其他形態（例如自行繪製幾何體的元素）會被排除在導出之外，而場景其餘部分照常導出。要讓這類元素導出，需要一個能承載它的註解，或為產出該標籤的網站渲染器提供標記。

插件需要注意的約定：

- `SyntaxValueKind` 的 id 請帶你的模組命名空間（如 `"MYMOD_MACHINE"`）。取值來源只按 id 路由；是否需要引號屬於宣告該屬性的那一方。
- `SyntaxSlot.match` 請回傳文件裡真實存在的那段文字：編輯器寫入前會重新核對，規範化後的形式會讓這次確認被丟棄。
- 插件拋異常會被記錄（有命名空間）並且跳過，因此失敗只影響你自己的貢獻，不會拖垮編輯器。

