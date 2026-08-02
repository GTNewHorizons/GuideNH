---
navigation:
  title: Special Index (MediaWikiSpecialGeneratedBlock)
  position: 7760
categories:
  - visualtest
  - indexes
item_ids:
  - guidenh:visualtest_fake_item
---

TEST GOAL / 测试目标：`<Special>` tag family — MediaWikiSpecialGeneratedBlock F7a pixel-font-migrated rendering: Special catalog, category grid, page grid, empty-state fallback.

INVARIANTS / 不变式：SpecialPages renders the grouped special-page catalog (2 columns, group headers, clickable link rows, top/bottom 1px borders); all Special blocks render through the primitives/GuideText path (no 8×8 MC pixel font); no compile errors.

## Special Pages Index (grouped catalog)

Expected: `<Special name="SpecialPages" />` renders the special-page catalog grouped by group (maintenance / lists / media / developer / translation / other), two columns, each entry a clickable title link; top/bottom borders visible. Static-catalog data, always non-empty — this is the F7a Special Index block.

<Special name="SpecialPages" />

## Categories (grid)

Expected: `<Special name="Categories" rows="2" />` renders the category list (GRID kind, 2 columns), each row: category title + member-count subtitle, clickable to the category page. Derived from the local CategoryIndex — non-empty in the visualtest corpus.

<Special name="Categories" rows="2" />

## All Pages (grid, index-backed; empty-state fallback allowed)

Expected: `<Special name="AllPages" rows="3" />` renders every normal page of the guide (3 columns) with page-id subtitles. Depends on the MediaWiki data index; if the async index is not yet warmed at mount time the block renders the empty-state text instead — which itself verifies the EMPTY_STYLE path of the migrated block.

<Special name="AllPages" rows="3" />
