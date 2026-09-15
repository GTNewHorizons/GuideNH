这一行写在模板里但位于 OnlyInclude 标签之外，因此包含该模板的页面永远看不到它。

<OnlyInclude>

**<Param name="label" default="片段" />** —— 只有这一部分会被转译。

</OnlyInclude>

<NoInclude>

把转译内容包在 `<OnlyInclude>` 里的模板，会把整次包含收窄到该内容，其余部分都不会到达调用页。

这个标签必须独占一行。写在句子中间时，它会像任何 MDX 标签那样把后面的文字当作自己的内容，
通常不是作者想要的效果。

```md
<Template name="OnlyPart" label="显示内容" />
```

</NoInclude>
