---
navigation:
  title: Error Page (Parse Failure)
  position: 7800
---

TEST GOAL: error text should render in red (the createErrorFlowContent / buildErrorPage paths)

INVARIANTS: the attribute-error text, the red of the Color tag and the red of §4 are all the same color; error text is not the gray body-text color.

Color reference A (Color tag): <Color id="red">THIS_SHOULD_BE_RED_VIA_COLOR_TAG</Color>

Color reference B (section code): §4THIS_SHOULD_BE_RED_VIA_SECTION_CODE§r

Attribute error below (should also be red, was gray):

<ItemImage id={} />
