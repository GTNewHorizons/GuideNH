---
navigation:
  title: CJK Tables
  position: 8780
---

TEST GOAL / 测试目标：CJK 表头/单元格/混合、CJK 长串

INVARIANTS / 不变式：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐

## CJK Headers and Cells

Expected: Chinese headers render correctly; cell content with mixed CJK and ASCII wraps properly within column boundaries.

| 材料 | 数量 | 备注 |
| --- | ---- | ---- |
| 圆石 | 64 | 基础建筑材料 |
| 萤石粉 | 16 | 光源材料 |
| 钻石 | 3 | 稀有宝石 |

## Mixed CJK and English Content

Expected: CJK + English mixed strings in one cell wrap without overflow; baseline alignment is consistent across mixed-script cells.

| 物品 | 说明 |
| --- | ---- |
| 神秘锭 | Thaumium ingot from Thaumcraft 4 |
| 红石 | Redstone dust used in GTNH circuits |
| 铂 | Iridium neutron reflector plate |

## Long CJK String Without Spaces

Expected: Long CJK-only string without spaces wraps at character boundaries; no overflow beyond right column edge.

| 类别 | 长文本 |
| --- | ------ |
| 说明 | 这是一段没有空格的中文长文本测试折行行为确保不溢出列边界 |
| 备注 | 格雷科技社区版GTNH整合包包含大量魔改内容 请参考NEI 查询配方 |
