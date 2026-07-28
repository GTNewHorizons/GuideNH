---
navigation:
  title: Tasks Lists
  position: 8880
---

TEST GOAL / 测试目标：任务列表混合完成/未完成、嵌套、富文本标签

INVARIANTS / 不变式：复选框与文本对齐；状态样式区分

## Mixed Checked and Unchecked

Expected: Completed `- [x]` and pending `- [ ]` items render with visually distinct checkbox states.

- [x] Completed task A
- [ ] Pending task B
- [x] Completed task C
- [ ] Pending task D
- [ ] Pending task E

## Nested Task Lists

Expected: Task items nest under both checked and unchecked parents; indentation preserves hierarchy.

- [x] Parent completed
  - [ ] Child pending A
  - [x] Child completed B
- [ ] Parent pending
  - [ ] Sub-task one
  - [ ] Sub-task two
    - [x] Deep nested completed
    - [ ] Deep nested pending

## Rich Text Labels

Expected: Task labels support inline markdown formatting and inline game tags; rendering does not break checkbox alignment.

- [x] **Bold completed** with *italic* suffix
- [ ] `code inline` inside task label
- [x] Task with [link](https://example.com) embedded
- [ ] ~~Strikethrough~~ ++underline++ ^^wave^^ mixed
- [x] <ItemImage id="minecraft:diamond" scale={0.5} /> icon in label
