---
navigation:
  title: Details
  position: 8280
---

TEST GOAL: details open/closed, embedded tables/code/images, several in a row

INVARIANTS: content is visible when open and only the summary row shows when closed; embedded blocks render normally inside the content area; consecutive details blocks do not interfere with each other

## Details Open

Expected: Details is open by default. The body content (text and image) is visible below the summary.

<details open>
<summary>Open Details (click to toggle)</summary>

This is the body of an open details block.

![](../assets/red-64.png)
</details>

## Details Closed

Expected: Details is closed by default. Only the summary line is visible; the body content is hidden.

<details>
<summary>Closed Details (click to expand)</summary>

This body is hidden until the user clicks the summary.

![](../assets/checker-128.png)
</details>

## Details with Embedded Table and Code

Expected: Open details containing a markdown table and a code block. Both render correctly inside the details body.

<details open>
<summary>Details with Table and Code</summary>

| Item | Value |
|------|-------|
| Diamond | 64 |
| Gold | 32 |

```java
System.out.println("Hello from details");
```
</details>

## Details with Embedded Image and Scene

Expected: Open details containing an image and a GameScene. Both render within the details content area.

<details open width="260">
<summary>Details with Image and Scene</summary>

![](../assets/checker-128.png)

<GameScene width="120" height="80" zoom="5">
<Block id="minecraft:diamond_block" />
</GameScene>
</details>

## Multiple Consecutive Details

Expected: Three details blocks stacked vertically. Each toggles independently; the open state of one does not affect the others.

<details open>
<summary>First Details (open)</summary>

Content of the first details block.
</details>

<details>
<summary>Second Details (closed)</summary>

Content of the second details block.
</details>

<details open>
<summary>Third Details (open)</summary>

Content of the third details block.
</details>
