[English](Localization)

# 本地化

GuideNH 支援在地化的指南頁面與在地化的指南資源。

## 目錄結構

運行時本地化基於目錄：

```text
config/guidenh/DefaultGuide/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

倉庫範例資源包則保留文件裡的外層 `assets/` 結構：

```text
wiki/resourcepack/assets/<modid>/guidenh/
|-- _en_us/
|   `-- index.md
`-- _zh_cn/
    `-- index.md
```

語言目錄只認以下劃線開頭的形式。像 `en_us/` 和 `zh_cn/` 這樣的普通目錄不再被當作本地化根目錄。

## 頁面尋找順序

對於每個請求的頁面 id，GuideNH 會依序嘗試：

1. `_<current language>/<page>`
2. 若目前語言頁面缺失，則嘗試 `_<default language>/<page>`
3. 不含語言目錄的 `<page>`

其中 `<page>` 代表目前請求的頁面路徑。

指南頁只會回退到該 guide 的 `defaultLanguage`。自動發現的資源包 guide 仍然預設把這個值設為 `en_us`，所以不會因為某個別的語言存在，就把它自動提升成兜底語言。

## 頁面 Lang Key 覆蓋

Guide 頁面也可以透過 `.lang` key 覆蓋整頁 markdown 源文本，但前提是這個頁面對應的實體 `.md` 文件必須真實存在。
檔案本身仍然負責決定頁面是否存在，並且繼續作為回退來源。

- GuideNH 仍然先以正常的語言回退順序解析頁面文件
- 找到某個實際檔案後，再按「請求語言」找出該頁面對應的 `.lang` 值
- 如果該 key 存在且非空，就在解析前用它的完整值替換整頁 markdown 源文本
- 如果該 key 缺失或為空，則回退到剛剛解析到的標準頁面檔案內容

當非空 `.lang` 頁值生效時，GuideNH 會在解析前從已解析到的實體頁面補齊缺失的 frontmatter 欄位。
在地化 frontmatter 中明確寫出的欄位總是優先，所以翻譯後的 `navigation.title` 會保留；而
`navigation.recommend`、`navigation.priority`、分類、物品連結、作者資訊或頁面縮放等結構性字段，
可以從回退 `.md` 檔案繼承。這樣基礎頁面新增推薦元資料後，舊的整頁翻譯不會意外讓首頁推薦消失。

key 格式如下：

```text
guidenh.page.<namespace>.<folder>.<去掉 .md 后的页面路径>
```

例如：

```text
assets/guidenh/guidenh/_en_us/charts.md
-> guidenh.page.guidenh.guidenh.charts
```

路徑分隔符號 `/` 會在 key 變成 `.`。如果某個路徑段本身帶有字面句號，為了避免與層級分隔衝突，會進行轉義：

```text
foo.bar.md -> foo_x2e_bar
```

其他非字母數字字元也會使用相同的 `_x<hex>_` 規則轉義。

`.lang` 值中的字面量 `\n` 與 `\r` 會在 markdown 解析前轉換成真正的換行，因此可以直接寫完整頁內容，
包括 frontmatter、標題、列表以及 MDX 標籤。

編寫規則：

- 該 key 對應的整頁內容必須仍寫在 `.lang` 檔案中的同一實體行裡
- 需要 markdown 換行時，在值內寫字面量 `\n`
- 不要直接在 `.lang` 的值插入真實換行，因為 Forge 讀取 `.lang` 時是按物理行分隔的
- 不要寫 `\\n`，除非你就是想讓最終 markdown 原始文字保留字面量 `\n`

GuideNH 不會僅憑 `.lang` 自動產生一個新頁面；實體頁面檔案仍然必須存在。

## Key 長度

GuideNH 本身沒有再額外給這類頁面 key 施加字元上限。 Minecraft 1.7.10 / Forge 這層的語言資料本質上更接近
字串屬性表，所以實際限制主要來自正常記憶體佔用和可維護性，而不是一個單獨的硬編碼長度上限。頁面路徑盡量簡潔，
仍然會更容易編寫和檢查。

## 編寫建議

- 如果你希望某個 guide 使用非英文作為回退語言，請明確設定 `defaultLanguage`
- 只有在確實希望跨語言共享時，才添加無語言的共享頁面
- 先翻譯頁面，再在資源中確實嵌入了文字時才翻譯資源
- 若共享資源夠通用，就不要額外引入語言特定的資源檔名

## 資源查找順序

指南資源使用稍微更豐富的回退順序：

1. `_<current language>/<path>`
2. 若目前語言不是指南預設語言，則嘗試 `_<default language>/<path>`
3. `<path>`

這樣在需要時，就可以對圖片或類似紋理的資源進行在地化。

## 搜尋與語言

搜尋文件會同時記錄原始 Minecraft 語言和 Lucene 實際使用的 analyzer 語言。若目前 Minecraft 語言未對應到已知 analyzer，搜尋會回退到英文分詞。

## 忽略翻譯配置

GuideNH 目前沒有提供全域「忽略翻譯」開關。如果你希望某個 guide 回退到非英文語言，請在程式碼裡明確設定該 guide 的 `defaultLanguage`。

## 範例

```text
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/index.md
config/guidenh/DefaultGuide/guidenh/guidenh/_en_us/test1.png
config/guidenh/DefaultGuide/guidenh/guidenh/_zh_cn/test1.png
```

倉庫範例資源包中的對應路徑：

```text
wiki/resourcepack/assets/guidenh/guidenh/_en_us/index.md
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/index.md
wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png
wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png
```

## 相關頁面

- [指南頁面格式](Guide-Page-Format)
- [圖片與資源](Images-And-Assets)
