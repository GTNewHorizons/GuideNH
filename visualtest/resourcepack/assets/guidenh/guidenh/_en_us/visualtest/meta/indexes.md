---
navigation:
  title: Indexes (SubPages + Category)
  position: 7770
categories:
  - visualtest
  - indexes
item_ids:
  - guidenh:visualtest_fake_item
---

TEST GOAL / 测试目标：`<SubPages>` navigation child listing + `<Category name>` category member listing

INVARIANTS / 不变式：SubPages renders a link list of child pages; Category renders a listing of pages in the given category; no compile errors.

## SubPages

Expected: <SubPages /> renders an unordered list of links to child pages (indexes-sub-a.md, indexes-sub-b.md) which declare parent: indexes.md in their frontmatter. Each link title matches the child's navigation.title.

<SubPages />

## Category

Expected: `<Category name="visualtest" />` renders a listing of all pages that declare categories: [visualtest] in their frontmatter, including pages from meta/ and auxiliary pages. (Note: `<CategoryIndex>` has no compiler in the engine; `<Category>` is the real tag.)

<Category name="visualtest" />
