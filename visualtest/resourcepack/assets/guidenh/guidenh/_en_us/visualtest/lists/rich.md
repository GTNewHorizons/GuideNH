---
navigation:
  title: Lists Rich
  position: 8890
---

TEST GOAL / 测试目标：富内容列表项：多段落、代码块、链接、展示公式、小表格、图片

INVARIANTS / 不变式：续行左对齐；嵌入块不破坏编号

## Multi-Paragraph Items

Expected: Each item contains two or more paragraphs; continuation text aligns with the item text, not the marker.

1. First paragraph of item one.

   Continuation paragraph indented under the item text. Wraps to show left alignment.
2. First paragraph of item two.

   Second continuation. Still aligned under the text start, not under the marker.

## Code Block in Item

Expected: Fenced code block inside a list item is indented and does not break numbering.

1. Item with embedded Java code block:

   ```java
   public class Sample {
       public static void main(String[] args) {
           System.out.println("list item code");
       }
   }
   ```
2. Next item resumes numbering correctly after the code block.
3. Third item confirms numbering continuity.

## Link in Item

Expected: Links render with correct styling inside list items; clickable area matches text.

- Visit the [GuideNH project](https://example.com/guidenh) for details.
- Internal page: open [index page](../index.md) reference.
- Styled link: **[Bold external link](https://example.com)**.

## Display Formula in Item

Expected: Display formula centered within a list item does not overflow or break numbering.

- Inline formula $E=mc^2$ appears within the sentence.
- Display formula on its own line:

  $$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

  Text continues normally after the display formula.
- Final item confirms list integrity after the formula block.

## Small Table in Item

Expected: Markdown table inside a list item renders with correct column widths and alignment.

- Comparison table embedded in item:

  | Material | Value |
  |----------|-------|
  | Iron     |    42 |
  | Gold     |    17 |
  | Diamond  |     9 |

  Trailing text after the table stays within the item.

## Image in Item

Expected: Image inside a list item aligns correctly; inline and block images do not overflow.

- Red icon inline: ![Red](../assets/red-64.png) appears beside text.
- Block image below text:

  ![Red Square](../assets/red-64.png)

  Caption follows the image on a new line.
