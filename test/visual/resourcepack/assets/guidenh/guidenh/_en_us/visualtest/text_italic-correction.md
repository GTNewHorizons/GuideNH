---
navigation:
  title: Italic Correction Verify
  position: 8620
---

TEST GOAL: TeX-level italic correction verification (base-italic shear / italic→upright boundary correction / non-intrusion)

## Mixed

Expected: italic→upright boundary spacing correction applies.

plain *italic* plain mixed words here.

## Base Italic

Expected: whole-paragraph italic with real shear on tall glyphs.

*H I l H I l g y.*

## Upright Control

Expected: no shear reference geometry.

upright control H I l g y.

## Leading Italic

Expected: leading italic run with upright tail.

*lead italic* trail upright.
