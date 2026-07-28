---
navigation:
  title: Scroll Containers
  position: 7680
---

TEST GOAL / 测试目标：固定视口代码块 `width`/`height` 内嵌超长内容，容器内滚动而非页面级溢出

INVARIANTS / 不变式：容器内滚动而非页面级溢出

## Vertical Scroll — Fixed Height Viewport

Expected: Code block with `width=180 height=80` renders a fixed-height viewport; 15 lines of content exceed the viewport height; a vertical scrollbar appears; the page itself does not grow vertically to accommodate the overflowing lines.

```java width=180 height=80
public class VerticalScroll {
    public static void main(String[] args) {
        System.out.println("Line 01 of 15");
        System.out.println("Line 02 of 15");
        System.out.println("Line 03 of 15");
        System.out.println("Line 04 of 15");
        System.out.println("Line 05 of 15");
        System.out.println("Line 06 of 15");
        System.out.println("Line 07 of 15");
        System.out.println("Line 08 of 15");
        System.out.println("Line 09 of 15");
        System.out.println("Line 10 of 15");
        System.out.println("Line 11 of 15");
        System.out.println("Line 12 of 15");
        System.out.println("Line 13 of 15");
        System.out.println("Line 14 of 15");
        System.out.println("Line 15 of 15");
    }
}
```

## Horizontal Scroll — Fixed Width Viewport

Expected: Code block with `width=120 height=120` constrains the viewport width; long lines exceeding 120px trigger a horizontal scrollbar; the page itself does not grow in width; content is revealed by scrolling horizontally.

```python width=120 height=120
def long_function_signature(param_one, param_two, param_three, param_four, param_five):
    result = param_one + param_two + param_three + param_four + param_five
    print(f"Computed result from all parameters is: {result}")
    return result

very_long_variable_name_for_configuration_settings = {"setting_alpha": "value_alpha", "setting_beta": "value_beta", "setting_gamma": "value_gamma"}
```

## Both Axes — Narrow Tall Viewport

Expected: Code block with `width=150 height=60` constrains both dimensions; content exceeds viewport in both axes; both horizontal and vertical scrollbars appear; the page height and width remain at their natural values unaffected by the code content overflow.

```java width=150 height=60
public class BothAxesScroll {
    public static void process(String input, String config, String format, String target) {
        System.out.println("Processing input=" + input + " config=" + config);
        System.out.println("Format=" + format + " Target=" + target);
        System.out.println("Step 1: Validate input parameters");
        System.out.println("Step 2: Apply configuration profile");
        System.out.println("Step 3: Transform source data");
        System.out.println("Step 4: Generate output artifact");
        System.out.println("Step 5: Run cleanup routines");
        System.out.println("Done.");
    }
}
