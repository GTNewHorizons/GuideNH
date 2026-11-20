# 本地化

GuideNH 支援本地化的指南頁面與本地化的指南資源。

## 目錄結構

執行時本地化基於目錄：

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

倉庫示例資源包則保留文件裡的外層 `assets/` 結構：

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

語言目錄只認以下劃線開頭的形式。像 `en_us/` 和 `zh_cn/` 這樣的普通目錄不再被當作本地化根目錄。

## 頁面查詢順序

對於每個請求的頁面 id，GuideNH 會依次嘗試：

1. `_<current language>/<page>`
2. 若當前語言頁面缺失，則嘗試 `_<default language>/<page>`
3. 不帶語言目錄的 `<page>`

指南頁面只會回退到該 guide 的 `defaultLanguage`。自動發現的資源包 guide 仍然預設把這個值設為 `en_us`，所以不會因為某個別的語言存在，就把它自動提升成兜底語言。

## 頁面 Lang Key 覆蓋

Guide 頁面還可以透過 `.lang` key 覆蓋整頁 markdown 源文字，但前提是這個頁面對應的實體 `.md` 檔案必須真實存在。
檔案本身仍然負責決定頁面是否存在，並且繼續作為回退來源。

- GuideNH 仍然先按正常的語言回退順序解析頁面檔案
- 找到某個實際檔案後，再按“請求語言”查詢該頁面對應的 `.lang` 值
- 如果該 key 存在且非空，就在解析前用它的完整值替換整頁 markdown 源文字
- 如果該 key 缺失或為空，則回退到剛剛解析到的標準頁面檔案內容

當非空 `.lang` 頁面值生效時，GuideNH 會在解析前從已解析到的實體頁面補齊缺失的 frontmatter 欄位。
本地化 frontmatter 中顯式寫出的欄位始終優先，所以翻譯後的 `navigation.title` 會保留；而
`navigation.recommend`、`navigation.priority`、分類、物品連結、作者資訊或頁面縮放等結構性欄位，
可以從回退 `.md` 檔案繼承。這樣基礎頁面新增推薦後設資料後，舊的整頁翻譯不會意外讓首頁推薦消失。

key 格式如下：

```text
guidenh.page.<namespace>.<folder>.<去掉 .md 后的页面路径>
```

例如：

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

路徑分隔符 `/` 會在 key 中變成 `.`。如果某個路徑段自身帶有字面句號，為了避免與層級分隔衝突，會進行轉義：

```text
foo.bar.md -> foo_x2e_bar
```

其他非字母數字字元也會使用同樣的 `_x<hex>_` 規則轉義。

`.lang` 值中的字面量 `\n` 與 `\r` 會在 markdown 解析前轉換成真正的換行，因此可以直接寫完整頁面內容，
包括 frontmatter、標題、列表以及 MDX 標籤。

編寫規則：

- 該 key 對應的整頁內容必須仍然寫在 `.lang` 檔案中的同一條物理行裡
- 需要 markdown 換行時，在值裡寫字面量 `\n`
- 不要直接在 `.lang` 的值裡插入真實換行，因為 Forge 讀取 `.lang` 時是按物理行分隔的
- 不要寫 `\\n`，除非你就是想讓最終 markdown 源文字里保留字面量 `\n`

GuideNH 不會僅憑 `.lang` 自動生成一個新頁面；實體頁面檔案仍然必須存在。

## 模板與語言

`templates/` 目錄下的模板頁與其他頁面遵循相同的語言查詢順序，因此同一個模板可以每種語言各有一個檔案：

```text
_en_us/templates/InfoBox.md
_zh_cn/templates/InfoBox.md
```

模板頁同樣可以像其他頁面一樣被 `.lang` 鍵整頁替換。鍵的格式是在頁面路徑後追加模板路徑：

```text
guidenh.page.<命名空间>.<目录>.templates.<名称>
```

## Key 長度

GuideNH 本身沒有再額外給這類頁面 key 施加字元上限。Minecraft 1.7.10 / Forge 這層的語言資料本質上更接近
字串屬性表，所以實際限制主要來自正常記憶體佔用和可維護性，而不是一個單獨的硬編碼長度上限。頁面路徑儘量簡潔，
仍然會更容易編寫和檢查。

## 編寫建議

- 如果你希望某個 guide 使用非英文作為回退語言，請顯式設定 `defaultLanguage`
- 只有在確實希望跨語言共享時，才新增無語言的共享頁面
- 先翻譯頁面，再在資源中確實嵌入了文字時才翻譯資源
- 若共享資源足夠通用，就不要額外引入語言特定的資原始檔名

## 資源查詢順序

指南資源使用稍微更豐富的回退順序：

1. `_<current language>/<path>`
2. 若當前語言不是指南預設語言，則嘗試 `_<default language>/<path>`
3. `<path>`

這樣在需要時，就可以對圖片或類似紋理的資源進行本地化。

## 搜尋與語言

搜尋文件會同時記錄原始 Minecraft 語言和 Lucene 實際使用的 analyzer 語言。若當前 Minecraft 語言未對映到已知 analyzer，搜尋會回退到英文分詞。

## 忽略翻譯配置

GuideNH 目前沒有提供全域性“忽略翻譯”開關。如果你希望某個 guide 回退到非英文語言，請在程式碼裡顯式設定該 guide 的 `defaultLanguage`。

## 示例

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

倉庫示例資源包中的對應路徑：

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [模板](Templates)
- [圖片與資源](Images-And-Assets)
