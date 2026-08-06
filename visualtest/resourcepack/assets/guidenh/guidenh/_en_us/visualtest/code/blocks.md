---
navigation:
  title: Code Block Variants
  position: 8700
---

TEST GOAL / 测试目标：代码块全变体——多语言 + 长行 + 空行 + 单行 + 特殊字符 + width/height 固定视口 + 缩进代码块

INVARIANTS / 不变式：长行策略符合规格；背景框包裹全行；固定视口出现滚动条而非页面溢出

## Multi-Language: XML

Expected: XML syntax highlighting; angle brackets and tag names colored correctly.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>demo</artifactId>
  <version>1.0.0</version>
</project>
```

## Multi-Language: Java

Expected: Java syntax highlighting; keywords, strings, and annotations colored.

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.sendMessage("Welcome to the server!");
    player.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));
}
```

## Multi-Language: JSON

Expected: JSON syntax highlighting; keys, strings, numbers, and braces colored.

```json
{
  "name": "GuideNH",
  "version": "1.0.0",
  "mcVersion": "1.7.10",
  "dependencies": {
    "GTNH": ">=2.0.0",
    "CodeChickenLib": "1.1.3"
  }
}
```

## Multi-Language: Python

Expected: Python syntax highlighting; def, print, and string literals colored.

```python
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)

for i in range(10):
    print(f"fib({i}) = {fibonacci(i)}")
```

## No Language (Plain Text)

Expected: No syntax highlighting applied; rendered as plain monospace text without language label.

```text
This code block has no explicit language fence.
The engine should render it as plain text.
No highlighting tokens are expected.
```

## 80+ Column Long Line

Expected: Long single line does not overflow the page right margin; engine applies horizontal scroll or line-wrapping per its configured long-line strategy; the background box wraps around the entire line.

```
ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;':\",./<>?`~ ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
```

## Empty Lines in Code Block

Expected: Multiple consecutive empty lines preserved within the code block; line numbering (if present) remains consistent; no collapsed blank lines.

```java
public class EmptyLines {

    public void methodA() {
        System.out.println("line 1");
    }


    public void methodB() {
        System.out.println("line 5 — two blank lines above");
    }

}
```

## Single-Line Code Block

Expected: A code block with exactly one line of content renders with single-line height; toolbar with language label visible; no extraneous vertical padding.

```json
{ "key": "value", "enabled": true }
```

## Special Characters &lt;&gt;&amp;&quot;&sect;

Expected: Angle brackets, ampersand, double quotes, and section sign render literally as code content; no HTML/XML entity interpretation inside the fence.

```xml
<tag attr="value" &special §char>
  <nested>content with "quotes" & amps §ect</nested>
</tag>
```

## Fixed Viewport width=180 height=80 (Overflow Sentinel)

Expected: Code block renders with a fixed body viewport (180px wide, 80px tall); content exceeding the viewport shows a vertical scrollbar; the page itself does not grow in height; this is the overflow scroll container sentinel.

```java width=180 height=80
public class ScrollTest {
    public static void main(String[] args) {
        System.out.println("line 1");
        System.out.println("line 2");
        System.out.println("line 3");
        System.out.println("line 4");
        System.out.println("line 5");
        System.out.println("line 6");
        System.out.println("line 7");
        System.out.println("line 8");
        System.out.println("line 9");
        System.out.println("line 10");
    }
}
```

## Indented Code Block (4 spaces)

Expected: Leading four-space indentation causes the content to be rendered as a code block (no language label, no toolbar, plain monospace).

    print("indented code block — no toolbar/language label")
    for i in range(5):
        print(f"line {i}")
    print("done")
