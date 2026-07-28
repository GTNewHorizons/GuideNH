---
navigation:
  title: Frontmatter Variants
  position: 7800
  icon: guidenh:visualtest_fake_item
  icon_texture: guidenh:textures/gui/visualtest_fake.png
  icons:
    - guidenh:visualtest_fake_item
    - guidenh:visualtest_fake_item_alt
  icon_textures:
    - guidenh:textures/gui/visualtest_fake.png
    - guidenh:textures/gui/visualtest_fake_b.png
categories:
  - visualtest
  - frontmatter
item_ids:
  - guidenh:visualtest_fake_item
  - guidenh:visualtest_fake_item_alt
author: Visual Test Suite
date: 2026-07-28
---

TEST GOAL / 测试目标：frontmatter navigation.icon / icon_texture / icons / icon_textures variants + categories + item_ids + author / date

INVARIANTS / 不变式：Sidebar-only fields (icon / icons / icon_textures) must not crash loading even when sidebar is hidden; body renders normally.

## Navigation Icon (Single)

Expected: navigation.icon set to guidenh:visualtest_fake_item — sidebar icon uses this item id. Loads without error.

Body paragraph for the icon page. No sidebar-dependent content here.

## Navigation Icon Texture (Single)

Expected: navigation.icon_texture set to guidenh:textures/gui/visualtest_fake.png — sidebar icon uses this texture path. Loads without error.

## Navigation Icons List

Expected: navigation.icons with two entries — sidebar cycles through both item icons on each page open. Loads without error.

## Navigation Icon Textures List

Expected: navigation.icon_textures with two entries — sidebar cycles through both texture icons. Loads without error.

## Categories Frontmatter

Expected: categories: [visualtest, frontmatter] — page appears in CategoryIndex for both categories.

## Item IDs Frontmatter

Expected: item_ids: [guidenh:visualtest_fake_item, guidenh:visualtest_fake_item_alt] — these fictional bindings do not resolve to any real item. Loads without error.

## Author & Date

Expected: author: Visual Test Suite, date: 2026-07-28 — metadata displays in the page bottom bar if the engine renders it.
