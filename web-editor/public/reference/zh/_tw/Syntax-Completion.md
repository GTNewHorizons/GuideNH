# 語法智慧補全

在遊戲內指南編輯器中編寫頁面時，編輯器會提供標籤、屬性和取值候選。

## 編寫參考

點選遊戲內編輯器工具列的燈泡開啟編寫參考。左側導覽可搜尋主題標題、標籤屬性、YAML 欄位及範例程式碼；Ctrl+F 聚焦搜尋，右鍵清空。選取主題會在完整文件中跳至對應標題。導覽與正文捲軸皆平滑移動。

前進、後退按鈕可重新造訪主題並還原捲動位置。游標停留在右側捲軸的標題橫線上可查看標題，點選可跳轉。程式碼區塊按鈕複製範例，工具列複製按鈕複製所選主題的 Markdown。參考開啟時燈泡保持藍色，再次點選燈泡返回指南頁，保留草稿、選取範圍及復原歷史。關閉按鈕或 Esc 關閉指南視窗。文件使用遊戲語言，缺少翻譯時使用英文。

## 使用候選列表

| 操作 | 結果 |
| --- | --- |
| ↑ / ↓ | 選擇上一項或下一項 |
| Tab / Enter | 插入選中的候選 |
| 單擊候選 | 插入選中的候選 |
| Esc | 關閉列表 |
| 滑鼠滾輪 | 滾動候選列表 |
| 滑條 | 拖動滑條滾動 |

## 支援的補全

| 編輯位置 | 候選內容 |
| --- | --- |
| `<...` | 當前容器內可用的標籤名 |
| `<Tag ...` | 屬性名及對應的取值定界符 |
| `id="..."`, `fluid="..."` | 物品和流體註冊名、配方、頁面、資源及其他支援的取值 |
| Markdown | 標題、列表、表格、提示塊等 Markdown 結構 |
| 程式碼圍欄 | 支援的程式碼語言和 GuideNH 圍欄名 |
| Frontmatter | YAML 欄位及支援的取值 |

補全標簽名會插入空標籤或成對標籤，成對標籤的游標位於內部，再按需填寫屬性和內容。物品與流體的取值候選左側帶有圖示；流體使用 `water`、`steam` 等註冊名，不使用數字 ID。

### 任務與研究 ID

ID 候選由當前內容決定：

| 編寫位置 | ID 來源 |
| --- | --- |
| `ResearchLink`、`ResearchCard`、`ResearchIcon` | Thaumcraft 研究 |
| `QuestLink`、`QuestCard` | BetterQuesting 任務 |
| `bindings` 條目中的 `id` | 同一條目 `type` 指定的系統 |
| `BindingLink`、`BindingCard`、`BindingIcon` | 標籤 `type` 指定的系統 |

填寫繫結時，先寫 `type`，再選擇 `id`：

```yaml
bindings:
  - {type: "thaumcraft", id: "INFUSION", anchor: "infusion"}
```

遊戲內編輯器讀取已載入模組的 ID。兩種 IDE 外掛離線時也會建議工作區頁面已使用的 ID；連線執行中的遊戲後，可以查詢遊戲註冊的 ID。研究 ID 保留原始大小寫。查詢與複製方法見 [Thaumcraft 相容](Mod-Compatibility#檢視與複製研究-id)。

## 多行文字與模板

候選行使用簡短預覽。可見的 `\n`、`\t` 表示換行和製表符，確認候選後會插入實際文字。多行插入沿用當前位置的縮排，並將游標放在繼續填寫的位置。

需要完整示例時，在編輯器右鍵選單中選擇模板，場景和圖表模板均在此提供。普通標籤名補全保留空標籤。

## 相關參考

- [頁面格式](Guide-Page-Format)
- [標籤與屬性](Tags-Reference)
- [配方](Recipes)

## NEI action links

The insert menu provides handler, recipe, and usage action snippets, plus `<NeiRecipeLink>`. The `id` attribute offers item IDs; `mode` offers `handler`, `recipes`, and `usage`. Enter the exact NEI identifier for `handlerId`.

See [Recipes](Recipes.md#inline-nei-recipe-links) for syntax and examples.
